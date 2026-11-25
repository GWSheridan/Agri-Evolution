package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned plans. Items never carry farm ownership or inventory contents. */
public final class FarmLedgerData extends SavedData {
    public static final int MAX_FIELDS = 16, MAX_SIDE = 32, MAX_INVITES = 8, MAX_LINKS = 8;
    public static final UUID NONE = new UUID(0, 0);
    private static final Factory<FarmLedgerData> FACTORY = new Factory<>(FarmLedgerData::new, FarmLedgerData::load, null);
    final Map<UUID, Field> fields = new LinkedHashMap<>();
    final Map<UUID, FarmLedgerService.Session> sessions = new HashMap<>();
    int surveyTurn;

    public static FarmLedgerData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "agri_farm_ledger");
    }

    public static final class Layout {
        public String name;
        public String[] seeds;
        Layout(String name, int size) { this.name = name; seeds = new String[size]; Arrays.fill(seeds, ""); }
        CompoundTag save() { var t = strings(seeds); t.putString("Name", name); return t; }
    }

    public static final class Field {
        public final UUID id, owner;
        public final String dimension;
        public final BlockPos min;
        public final int width, depth;
        public String name;
        public int revision;
        public final Map<UUID, Boolean> invited = new LinkedHashMap<>();
        public final Layout[] layouts = new Layout[4];
        public final List<BlockPos> machines = new ArrayList<>();
        public final List<String> machineTypes = new ArrayList<>();
        public final List<String> machineStatus = new ArrayList<>();
        public final String[] actual;
        public final int[] flags;
        public final long[] observed;
        public int cursor;
        public long surveyedAt;

        Field(UUID id, UUID owner, String dimension, BlockPos min, int width, int depth, String name) {
            this.id = id; this.owner = owner; this.dimension = dimension; this.min = min.immutable();
            this.width = width; this.depth = depth; this.name = name;
            for (int i = 0; i < 4; i++) layouts[i] = new Layout("Layout " + (i + 1), size());
            actual = new String[size()]; Arrays.fill(actual, ""); flags = new int[size()]; observed = new long[size()];
        }
        public int size() { return width * depth; }
        public BlockPos soil(int cell) { return min.offset(cell % width, 0, cell / width); }
        public boolean canView(UUID player) { return owner.equals(player) || invited.containsKey(player); }
        public boolean canEdit(UUID player) { return owner.equals(player) || Boolean.TRUE.equals(invited.get(player)); }
        public CompoundTag summary() {
            var t = new CompoundTag(); t.putUUID("Id", id); t.putUUID("Owner", owner);
            t.putString("Dimension", dimension); t.putLong("Min", min.asLong()); t.putInt("Width", width); t.putInt("Depth", depth);
            t.putString("Name", name); t.putInt("Revision", revision); return t;
        }
        public CompoundTag save() {
            var t = summary(); var plans = new ListTag(); for (var layout : layouts) plans.add(layout.save()); t.put("Layouts", plans);
            var guests = new ListTag(); invited.forEach((id, edit) -> { var g = new CompoundTag(); g.putUUID("Id", id); g.putBoolean("Edit", edit); guests.add(g); });
            t.put("Invited", guests); t.putLongArray("Machines", machines.stream().mapToLong(BlockPos::asLong).toArray());
            var types=new ListTag();machineTypes.forEach(s->types.add(StringTag.valueOf(s)));t.put("MachineTypes",types);
            var statuses = new ListTag(); machineStatus.forEach(s -> statuses.add(StringTag.valueOf(s))); t.put("MachineStatus", statuses);
            t.put("Actual", strings(actual)); t.putIntArray("Flags", flags); t.putLongArray("Observed", observed); t.putLong("SurveyedAt", surveyedAt); return t;
        }
    }

    public Field create(UUID owner, String dimension, BlockPos first, BlockPos second) {
        if (fields.values().stream().filter(f -> f.owner.equals(owner)).count() >= MAX_FIELDS
                || !validBounds(first, second) || ResourceLocation.tryParse(dimension) == null) return null;
        var min = new BlockPos(Math.min(first.getX(), second.getX()), first.getY(), Math.min(first.getZ(), second.getZ()));
        var field = new Field(UUID.randomUUID(), owner, dimension, min,
                Math.abs(first.getX() - second.getX()) + 1, Math.abs(first.getZ() - second.getZ()) + 1, "New field");
        fields.put(field.id, field); setDirty(); return field;
    }

    public static boolean validBounds(BlockPos a, BlockPos b) {
        return a.getY() == b.getY() && Math.abs((long)a.getX() - b.getX()) < MAX_SIDE
                && Math.abs((long)a.getZ() - b.getZ()) < MAX_SIDE;
    }
    public static boolean validName(String value) {
        return !value.isBlank() && value.length() <= 32 && value.codePoints().noneMatch(c -> Character.isISOControl(c) || c == 167);
    }
    public static boolean validSeedId(String value) {
        return value.isEmpty() || value.length() <= 128 && ResourceLocation.tryParse(value) != null;
    }

    public boolean saveLayout(Field f, UUID actor, int revision, int slot, String name, String[] seeds) {
        if (!current(f, actor, revision, false) || slot < 0 || slot >= 4 || !validName(name)
                || seeds.length != f.size() || Arrays.stream(seeds).anyMatch(s -> s == null || !validSeedId(s))) return false;
        f.layouts[slot].name = name.strip(); f.layouts[slot].seeds = seeds.clone(); changed(f); return true;
    }
    public boolean copyLayout(Field destination, UUID actor, int revision, int slot, Field source, int sourceSlot) {
        return source != null && source.canView(actor) && destination != null && destination.width == source.width
                && destination.depth == source.depth && sourceSlot >= 0 && sourceSlot < 4
                && saveLayout(destination, actor, revision, slot, source.layouts[sourceSlot].name, source.layouts[sourceSlot].seeds);
    }
    boolean current(Field f, UUID actor, int revision, boolean ownerOnly) {
        return f != null && fields.get(f.id) == f && f.revision == revision
                && (ownerOnly ? f.owner.equals(actor) : f.canEdit(actor));
    }
    public boolean invite(Field f, UUID actor, int revision, UUID guest, boolean edit) {
        if (!current(f, actor, revision, true) || guest.equals(NONE) || guest.equals(f.owner)
                || !f.invited.containsKey(guest) && f.invited.size() >= MAX_INVITES) return false;
        f.invited.put(guest, edit); changed(f); return true;
    }
    void changed(Field f) { f.revision++; setDirty(); }

    /** Palette encoding keeps a 32x32 layout compact even with long registry names. */
    public static CompoundTag strings(String[] values) {
        var palette = new LinkedHashMap<String, Integer>(); var names = new ListTag(); int[] indices = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (!palette.containsKey(value)) { palette.put(value, palette.size()); names.add(StringTag.valueOf(value)); }
            indices[i] = palette.get(value);
        }
        var t = new CompoundTag(); t.put("Palette", names); t.putIntArray("Cells", indices); return t;
    }
    public static String[] readStrings(CompoundTag t, int size) {
        var palette = t.getList("Palette", Tag.TAG_STRING); var cells = t.getIntArray("Cells");
        if (size < 1 || size > 1024 || cells.length != size || palette.isEmpty() || palette.size() > 1024) return null;
        String[] values = new String[size];
        for (int i = 0; i < size; i++) {
            if (cells[i] < 0 || cells[i] >= palette.size()) return null;
            values[i] = palette.getString(cells[i]); if (!validSeedId(values[i])) return null;
        }
        return values;
    }

    public static Field readField(CompoundTag t) {
        if (!t.hasUUID("Id") || !t.hasUUID("Owner") || !validName(t.getString("Name"))) return null;
        int w = t.getInt("Width"), d = t.getInt("Depth"); String dim = t.getString("Dimension");
        if (w < 1 || d < 1 || w > 32 || d > 32 || dim.length() > 128 || ResourceLocation.tryParse(dim) == null) return null;
        var f = new Field(t.getUUID("Id"), t.getUUID("Owner"), dim, BlockPos.of(t.getLong("Min")), w, d, t.getString("Name"));
        f.revision = t.getInt("Revision"); var layouts = t.getList("Layouts", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(4, layouts.size()); i++) {
            var plan = layouts.getCompound(i); var cells = readStrings(plan, f.size());
            if (cells != null && validName(plan.getString("Name"))) { f.layouts[i].name = plan.getString("Name"); f.layouts[i].seeds = cells; }
        }
        var guests = t.getList("Invited", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_INVITES, guests.size()); i++) { var g = guests.getCompound(i); if (g.hasUUID("Id")) f.invited.put(g.getUUID("Id"), g.getBoolean("Edit")); }
        var machines = t.getLongArray("Machines"); for (int i = 0; i < Math.min(MAX_LINKS, machines.length); i++) f.machines.add(BlockPos.of(machines[i]));
        var types=t.getList("MachineTypes",Tag.TAG_STRING);for(int i=0;i<f.machines.size();i++)f.machineTypes.add(i<types.size()&&validSeedId(types.getString(i))?types.getString(i):"");
        var statuses = t.getList("MachineStatus", Tag.TAG_STRING); for (int i = 0; i < Math.min(MAX_LINKS, statuses.size()); i++) f.machineStatus.add(statuses.getString(i).substring(0, Math.min(256, statuses.getString(i).length())));
        var actual = readStrings(t.getCompound("Actual"), f.size()); if (actual != null) System.arraycopy(actual, 0, f.actual, 0, f.size());
        var flags = t.getIntArray("Flags"); if (flags.length == f.size()) System.arraycopy(flags, 0, f.flags, 0, f.size());
        var times = t.getLongArray("Observed"); if (times.length == f.size()) System.arraycopy(times, 0, f.observed, 0, f.size());
        f.surveyedAt = t.getLong("SurveyedAt"); return f;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 1); var list = new ListTag(); fields.values().forEach(f -> list.add(f.save())); tag.put("Fields", list); return tag;
    }
    public static FarmLedgerData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new FarmLedgerData();
        if (tag.getInt("Version") != 1) throw new IllegalStateException("Unsupported Farm Ledger save version: " + tag.getInt("Version"));
        var records = tag.getList("Fields", Tag.TAG_COMPOUND); var counts = new HashMap<UUID, Integer>();
        for (int i = 0; i < records.size(); i++) {
            var f = readField(records.getCompound(i));
            if (f != null && counts.getOrDefault(f.owner, 0) < MAX_FIELDS && !data.fields.containsKey(f.id)) {
                data.fields.put(f.id, f); counts.merge(f.owner, 1, Integer::sum);
            }
        }
        return data;
    }
}
