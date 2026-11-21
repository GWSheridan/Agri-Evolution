package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import com.agrievolution.farming.SoilResearch;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID)
public final class FarmLedgerService {
    public static final int KNOWN=1, SOIL=2, PLANTED=4, MATURE=8, WEEDS=16, DRY=32, LOW_FOOD=64, SUPPORT=128;
    public static final int SURVEY_BUDGET = 128;
    static final class Session {
        UUID selected = FarmLedgerData.NONE;
        BlockPos corner;
        String dimension = "";
        ItemStack markingStack;
        boolean marking, preview;
        long until, requestTick = -1;
        int layout, page, requests;
    }
    private static Session session(ServerPlayer p) { return FarmLedgerData.get(p.serverLevel()).sessions.computeIfAbsent(p.getUUID(), id -> new Session()); }
    public static boolean holding(Player p) { return p.isAlive() && !p.isSpectator() && (p.getMainHandItem().getItem() instanceof FarmLedgerItem || p.getOffhandItem().getItem() instanceof FarmLedgerItem); }
    private static String dimension(ServerPlayer p) { return p.level().dimension().location().toString(); }
    public static boolean nearby(ServerPlayer p, FarmLedgerData.Field f) {
        if (!f.dimension.equals(dimension(p))) return false;
        double x = Math.clamp(p.getX(), f.min.getX(), f.min.getX()+f.width);
        double z = Math.clamp(p.getZ(), f.min.getZ(), f.min.getZ()+f.depth);
        return p.distanceToSqr(x, f.min.getY()+1, z) <= 64*64;
    }

    public static void request(ServerPlayer p, FarmLedgerPayload.Request request) {
        if (!holding(p)) return;
        var data = FarmLedgerData.get(p.serverLevel()); var s = session(p); long now = p.serverLevel().getGameTime();
        if (s.requestTick != now) { s.requestTick = now; s.requests = 0; }
        if (++s.requests > 6) return;
        boolean menu = p.containerMenu instanceof FarmLedgerMenu && p.containerMenu.containerId == request.menu() && p.containerMenu.stillValid(p);
        if (!menu && !(request.action().equals("watch") && s.preview && s.selected.equals(request.field()))) return;
        var f = data.fields.get(request.field()); var input = request.data(); String message = "";
        if (request.action().equals("list")) { s.page = Math.max(0, Math.min(100000, input.getInt("Page"))); }
        else if (request.action().equals("mark")) {
            s.marking = true; s.corner = null; s.dimension = dimension(p);
            s.markingStack = p.getMainHandItem().getItem() instanceof FarmLedgerItem ? p.getMainHandItem() : p.getOffhandItem();
            p.closeContainer(); p.displayClientMessage(Component.literal("Mark two farmland corners at the same height. Sneak-use cancels."), true); return;
        } else if (f == null || !f.canView(p.getUUID())) {
            s.selected = FarmLedgerData.NONE; s.until = 0; message = "This field is no longer available.";
        } else {
            s.selected = f.id; s.layout = Math.clamp(input.getInt("Layout"), 0, 3);
            s.dimension = dimension(p);
            if (menu && input.contains("Preview")) s.preview = input.getBoolean("Preview");
            if (request.action().equals("select") || request.action().equals("watch")) s.until = now + 60;
            else if (request.action().equals("stop")) s.until = 0;
            else if (!data.current(f, p.getUUID(), request.revision(), false)) message = "Field changed or access was revoked. Your draft is kept; reload before saving.";
            else switch (request.action()) {
                case "save" -> {
                    var seeds = FarmLedgerData.readStrings(input.getCompound("Plan"), f.size());
                    if (seeds == null || !data.saveLayout(f, p.getUUID(), request.revision(), s.layout, input.getString("Name"), seeds)) message = "Invalid layout.";
                    else message = "Layout saved.";
                }
                case "copy" -> {
                    var source = input.hasUUID("Source") ? data.fields.get(input.getUUID("Source")) : null;
                    if (!data.copyLayout(f, p.getUUID(), request.revision(), s.layout, source, input.getInt("SourceLayout"))) message = "Copy needs an accessible field with the same dimensions.";
                    else message = "Layout copied.";
                }
                case "rename" -> {
                    if (f.owner.equals(p.getUUID()) && FarmLedgerData.validName(input.getString("Name"))) { f.name = input.getString("Name").strip(); data.changed(f); }
                    else message = "Only the owner can rename this field (1–32 characters).";
                }
                case "delete" -> {
                    if (f.owner.equals(p.getUUID())) { data.fields.remove(f.id); data.setDirty(); s.selected = FarmLedgerData.NONE; s.until = 0; }
                    else message = "Only the owner can delete a field.";
                }
                case "invite", "revoke" -> {
                    UUID guest = null;
                    if(input.getString("Player").length()>36) { message="Use an online player name or UUID."; break; }
                    try { guest = UUID.fromString(input.getString("Player")); } catch (IllegalArgumentException ignored) {
                        var online = p.server.getPlayerList().getPlayerByName(input.getString("Player")); if (online != null) guest = online.getUUID();
                    }
                    if (!f.owner.equals(p.getUUID()) || guest == null) message = "Use an online player name or a player UUID. Only the owner manages access.";
                    else if (request.action().equals("revoke")) { if (f.invited.remove(guest) != null) data.changed(f); }
                    else if (!data.invite(f, p.getUUID(), request.revision(), guest, input.getBoolean("Edit"))) message = "Cannot invite this player (eight invitations maximum).";
                }
                case "unlink" -> { int index = input.getInt("Index"); if (index >= 0 && index < f.machines.size()) { f.machines.remove(index); if(index<f.machineTypes.size())f.machineTypes.remove(index);f.machineStatus.clear(); data.changed(f); } }
                default -> message = "Unknown ledger action.";
            }
        }
        send(p, s, message);
    }

    static void send(ServerPlayer p, Session s, String message) {
        if (p.connection == null || !p.connection.hasChannel(FarmLedgerPayload.Response.TYPE)) return;
        var data = FarmLedgerData.get(p.serverLevel()); var out = new CompoundTag(); var list = new ListTag();
        var visible = data.fields.values().stream().filter(f -> f.canView(p.getUUID())).toList();
        s.page = Math.min(s.page, Math.max(0, (visible.size()-1)/32));
        visible.stream().skip((long)s.page*32).limit(32).forEach(f -> list.add(f.summary())); out.put("Fields", list);
        out.putInt("Page", s.page); out.putInt("Pages", Math.max(1, (visible.size()+31)/32)); out.putString("Message", message);
        var selected = data.fields.get(s.selected);
        if (selected != null && selected.canView(p.getUUID())) {
            out.put("Selected", selected.save()); out.putBoolean("Nearby", nearby(p, selected));
            out.putBoolean("Watching", s.until > p.serverLevel().getGameTime());
        } else { s.selected = FarmLedgerData.NONE; s.until = 0; }
        var counts = new CompoundTag(); carriedSeeds(p).forEach(counts::putInt); out.put("Carried", counts);
        PacketDistributor.sendToPlayer(p, new FarmLedgerPayload.Response(out));
    }

    public static Map<String, Integer> carriedSeeds(Player p) {
        var counts = new HashMap<String, Integer>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var stack = p.getInventory().getItem(i); count(stack, counts);
            if (stack.getItem() instanceof SeedBagItem) stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().limit(SeedBagItem.SLOTS).forEach(s -> count(s, counts));
        }
        return counts;
    }
    private static void count(ItemStack stack, Map<String, Integer> counts) {
        if (SeedBreedingParents.accepts(stack)) counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Math.max(0, stack.getCount()), Integer::sum);
    }

    public static void useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer p)) return;
        var s = session(p); if (p.isShiftKeyDown()) { cancel(s); p.displayClientMessage(Component.literal("Field marking cancelled."), true); return; }
        var level = p.serverLevel(); var pos = ctx.getClickedPos(); if (!level.hasChunkAt(pos) || !holding(p)) return;
        var held = ctx.getItemInHand(); var before = held.copy(); var state = level.getBlockState(pos); var entity = level.getBlockEntity(pos);
        if (!WorkstationAccess.mayUse(level, pos, p, ctx.getClickedFace(), held)
                || !WorkstationAccess.mayCompleteItemUse(level, p, ctx.getHand(), held, before)
                || !level.hasChunkAt(pos) || level.getBlockState(pos) != state || level.getBlockEntity(pos) != entity) return;
        if (s.marking) {
            if (!dimension(p).equals(s.dimension) || held != s.markingStack) { cancel(s); return; }
            if (!(state.getBlock() instanceof FarmBlock)) { p.displayClientMessage(Component.literal("Choose a farmland corner."), true); return; }
            if (s.corner == null) { s.corner = pos.immutable(); p.displayClientMessage(Component.literal("First corner marked. Choose the opposite corner."), true); }
            else {
                if (!level.hasChunkAt(s.corner) || !(level.getBlockState(s.corner).getBlock() instanceof FarmBlock)) { cancel(s); return; }
                var field = FarmLedgerData.get(level).create(p.getUUID(), dimension(p), s.corner, pos);
                if (field == null) { p.displayClientMessage(Component.literal("Use one height, at most 32×32 blocks and 16 owned fields."), true); return; }
                cancel(s); s.selected = field.id; FarmLedgerItem.open(p); send(p, s, "Field created.");
            }
            return;
        }
        var data = FarmLedgerData.get(level); var f = data.fields.get(s.selected);
        BlockPos machine = machinePosition(level, pos, state);
        if (machine == null) { FarmLedgerItem.open(p); return; }
        if (f == null || !f.canEdit(p.getUUID()) || !f.dimension.equals(dimension(p))) { p.displayClientMessage(Component.literal("Select an editable field in this dimension first."), true); return; }
        int revision = f.revision; var coreEntity = level.hasChunkAt(machine) ? level.getBlockEntity(machine) : null;
        if (!level.hasChunkAt(machine) || coreEntity == null || !WorkstationAccess.mayUse(level, machine, p, ctx.getClickedFace(), held)
                || !WorkstationAccess.mayCompleteItemUse(level, p, ctx.getHand(), held, before)
                || level.getBlockState(pos) != state || level.getBlockEntity(pos) != entity
                || !data.current(f, p.getUUID(), revision, false) || level.getBlockEntity(machine) != coreEntity) return;
        if (f.machines.contains(machine)) { p.displayClientMessage(Component.literal("Machine already linked."), true); return; }
        if (f.machines.size() >= 8) { p.displayClientMessage(Component.literal("Eight machines are already linked."), true); return; }
        f.machines.add(machine);f.machineTypes.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(coreEntity.getType()).toString());f.machineStatus.clear();data.changed(f);p.displayClientMessage(Component.literal("Machine linked to " + f.name + "."), true);
    }
    private static BlockPos machinePosition(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof WaterReservoirBlock) return WaterReservoirBlock.core(pos, state);
        if (state.getBlock() instanceof VerticalGreenhouseBlock) { var c = VerticalGreenhouseBlock.controller(level, pos, state); return c == null ? null : c.getBlockPos(); }
        return level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity || level.getBlockEntity(pos) instanceof IrrigationControllerBlockEntity ? pos.immutable() : null;
    }
    static void cancel(Session s) { s.corner = null; s.marking = false; s.markingStack = null; }
    public static void cancelMarking(ServerPlayer p) { cancel(session(p)); }
    public static void close(ServerPlayer p) { var s=session(p); if(!s.preview) s.until=0; }

    @SubscribeEvent public static void playerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var data = FarmLedgerData.get(p.serverLevel()); var s = data.sessions.get(p.getUUID()); if (s == null) return;
        if (!holding(p) || !s.dimension.isEmpty() && !dimension(p).equals(s.dimension)) { cancel(s); s.until = 0; s.dimension = dimension(p); }
        if (s.marking && p.getMainHandItem() != s.markingStack && p.getOffhandItem() != s.markingStack) cancel(s);
        if (p.tickCount % 20 == 0 && (p.containerMenu instanceof FarmLedgerMenu || s.until > p.serverLevel().getGameTime()) && holding(p)) send(p, s, "");
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) FarmLedgerData.get(p.serverLevel()).sessions.remove(p.getUUID());
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post e) {
        if (e.getLevel() instanceof ServerLevel level) survey(level);
    }
    public static int survey(ServerLevel level) {
        var data = FarmLedgerData.get(level); var active = new LinkedHashSet<FarmLedgerData.Field>();
        for (var entry : data.sessions.entrySet()) {
            var p = level.getServer().getPlayerList().getPlayer(entry.getKey()); var s = entry.getValue(); var f = data.fields.get(s.selected);
            if (p != null && p.level() == level && holding(p) && s.until > level.getGameTime() && f != null && f.canView(p.getUUID()) && nearby(p, f)) active.add(f);
        }
        return surveyFields(level, active);
    }
    static int surveyFields(ServerLevel level, Collection<FarmLedgerData.Field> active) {
        if (active.isEmpty()) return 0;
        var data=FarmLedgerData.get(level);var fields = new ArrayList<>(active); int used = 0;
        int target=Math.min(SURVEY_BUDGET,fields.stream().mapToInt(FarmLedgerData.Field::size).sum());
        var quota=new HashMap<FarmLedgerData.Field,Integer>();
        while (used < target) {
            var f = fields.get(Math.floorMod(data.surveyTurn++, fields.size()));
            if(quota.getOrDefault(f,0)>=f.size())continue;
            quota.merge(f,1,Integer::sum);
            sample(level, f, f.cursor++); used++;
            if (f.cursor == f.size()) { f.cursor = 0; f.surveyedAt = System.currentTimeMillis(); data.setDirty(); }
            if (used >= target) break;
        }
        if (level.getGameTime() % 20 == 0) for (var f : fields) { machineStatus(level, f); data.setDirty(); }
        return used;
    }
    public static void sample(ServerLevel level, FarmLedgerData.Field f, int i) {
        var soil = f.soil(i); var pos = soil.above();
        if (!level.hasChunkAt(soil) || !level.hasChunkAt(pos)) { f.flags[i] = 0; f.actual[i] = ""; f.observed[i] = 0; return; }
        var state = level.getBlockState(pos); var ground = level.getBlockState(soil); int flags = KNOWN;
        if (ground.getBlock() instanceof FarmBlock) flags |= SOIL;
        if (!state.isAir()) flags |= PLANTED;
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) || state.getBlock() instanceof AttachedStemBlock) flags |= MATURE;
        var age = CropWeeds.ageProperty(state);
        if (age != null && state.getValue(age).equals(Collections.max(age.getPossibleValues()))) flags |= MATURE;
        if (CropWeeds.hasWeeds(level, pos)) flags |= WEEDS;
        if (ground.hasProperty(FarmBlock.MOISTURE) && ground.getValue(FarmBlock.MOISTURE) <= 2) flags |= DRY;
        if (SoilCareData.rotationBonus(level,pos)>0) flags |= 256;
        if (SoilCareData.mulched(level,soil)) flags |= 512;
        if (level.getBlockEntity(pos) instanceof AECropBlockEntity plant) {
            if (plant.soilNutrition() < 35) flags |= LOW_FOOD;
            int tier = CropSupportItem.tier(state);
            if (tier == 0 || tier < 3 && plant.supportTicks() <= CropSupportItem.WEAR_WARNING_TICKS) flags |= SUPPORT;
        }
        // Survival checks can inspect neighbouring blocks. Never call them at an unloaded boundary.
        boolean neighbours = level.hasChunksAt(pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
        if (!neighbours) flags |= 1 << 20;
        for (int l = 0; l < 4; l++) {
            var block = plantedBlock(f.layouts[l].seeds[i]);
            if (block != null && neighbours && !block.defaultBlockState().canSurvive(level, pos)) flags |= 1 << (16+l);
        }
        f.flags[i] = flags; f.actual[i] = state.isAir() ? "" : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); f.observed[i] = System.currentTimeMillis();
    }
    public static Block plantedBlock(String id) {
        var key = ResourceLocation.tryParse(id); if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return null;
        var item = BuiltInRegistries.ITEM.get(key);
        if (item instanceof SpeciesSeedItem seed) return seed.cropBlock();
        return item instanceof BlockItem plant && SeedBreedingParents.accepts(new ItemStack(item)) ? plant.getBlock() : null;
    }
    public static boolean matches(String seed, String actual) {
        var plant = plantedBlock(seed); var key = ResourceLocation.tryParse(actual); if (plant == null || key == null) return false;
        var block = BuiltInRegistries.BLOCK.get(key);
        return plant == block || plant == Blocks.MELON_STEM && block == Blocks.ATTACHED_MELON_STEM || plant == Blocks.PUMPKIN_STEM && block == Blocks.ATTACHED_PUMPKIN_STEM;
    }
    public static String cellStatus(FarmLedgerData.Field f, int layout, int i) {
        String seed = f.layouts[layout].seeds[i]; if (seed.isEmpty()) return "Unassigned";
        if ((f.flags[i] & KNOWN) == 0 || plantedBlock(seed) == null) return "Unknown";
        if ((f.flags[i] & SOIL) == 0 || (f.flags[i] & 1 << (16+layout)) != 0) return "Unsuitable soil";
        if (matches(seed, f.actual[i])) return "Correct";
        if ((f.flags[i] & 1 << 20) != 0) return "Unknown";
        if (f.actual[i].isEmpty()) return "Missing";
        return "Wrong plant";
    }
    static void machineStatus(ServerLevel level, FarmLedgerData.Field f) {
        f.machineStatus.clear();
        for (int index=0;index<f.machines.size();index++) {
            var pos=f.machines.get(index);
            String status = "Unloaded";
            if (level.hasChunkAt(pos)) {
                var be = level.getBlockEntity(pos);
                String expected=index<f.machineTypes.size()?f.machineTypes.get(index):"";
                if(be!=null&&!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString().equals(expected))be=null;
                if (be instanceof WaterReservoirBlockEntity tank) status = "Reservoir: " + tank.water() + " mB";
                else if (be instanceof VerticalGreenhouseBlockEntity house) status = "Vertical Greenhouse: " + house.water() + " mB";
                else if (be instanceof BotanicalGreenhouseBlockEntity house) status = "Botanical Greenhouse: " + house.data.get(0) + " mB, " + (house.data.get(2)==1?"formed":"incomplete");
                else if (be instanceof IrrigationControllerBlockEntity controller) status = "Controller: " + (controller.enabled()?"enabled":"paused") + ", active zone " + (controller.active()+1) + ", reserve " + controller.reserveWater() + " mB";
                else status = "Missing machine";
            }
            f.machineStatus.add(pos.toShortString() + " — " + status);
        }
    }
    private FarmLedgerService() { }
}
