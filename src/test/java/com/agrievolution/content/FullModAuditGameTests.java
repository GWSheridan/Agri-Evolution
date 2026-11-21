package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Regression cases found during the 1.3.40 audit. */
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class FullModAuditGameTests {
    private static Runnable seedHook, foodHook;
    private static int seedQueries;
    private static final ResourceLocation SEED = ResourceLocation.fromNamespaceAndPath("agri_evolution", "test_audit_seed");
    private static final ResourceLocation FOOD = ResourceLocation.fromNamespaceAndPath("agri_evolution", "test_audit_food");
    @EventBusSubscriber(modid="agri_evolution", bus=EventBusSubscriber.Bus.MOD)
    public static final class AuditRegistry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.ITEM, SEED, () -> new BlockItem(Blocks.WHEAT, new Item.Properties()) {
                @Override public Block getBlock() { seedQueries++; if(seedHook != null) { var hook=seedHook; seedHook=null; hook.run(); } return super.getBlock(); }
            });
            event.register(Registries.ITEM, FOOD, () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(.3f).build())) {
                @Override public FoodProperties getFoodProperties(ItemStack stack, LivingEntity player) { if(foodHook != null) { var hook=foodHook; foodHook=null; hook.run(); } return stack.get(DataComponents.FOOD); }
            });
        }
    }
    private static final class AuditPlayer extends Player {
        Runnable permission;
        AuditPlayer(GameTestHelper h, BlockPos pos) { super(h.getLevel(), pos, 0, new GameProfile(UUID.randomUUID(), "audit")); setPos(pos.getCenter()); getAbilities().mayBuild=true; }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) { if(permission!=null){var hook=permission;permission=null;hook.run();} return true; }
    }
    private static final class LunchPlayer extends net.minecraft.server.level.ServerPlayer {
        boolean spectator;
        LunchPlayer(GameTestHelper h) {
            super(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"lunch-use"),
                    net.minecraft.server.level.ClientInformation.createDefault());
        }
        @Override public boolean isSpectator() { return spectator; }
        @Override public void displayClientMessage(Component message, boolean actionBar) {}
        @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider,
                java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extraData) {
            containerMenu = provider.createMenu(1,getInventory(),this);
            return java.util.OptionalInt.of(1);
        }
        @Override public void closeContainer() { containerMenu.removed(this); containerMenu = inventoryMenu; }
    }
    private static ItemStack seed(int count) { return new ItemStack(BuiltInRegistries.ITEM.get(SEED), count); }
    private static void sourceSwap(GameTestHelper h, boolean vault) {
        var pos=h.absolutePos(new BlockPos(4,210,4));
        h.getLevel().setBlock(pos, (vault?AEBlocks.SEED_VAULT.get():AEBlocks.SEED_BREEDING_STATION.get()).defaultBlockState(),2);
        var player=new AuditPlayer(h,pos); var original=seed(8); player.getInventory().setItem(9,original);
        AbstractContainerMenu menu=vault?new SeedVaultMenu(1,player.getInventory(),(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos))
                :new SeedBreedingStationMenu(1,player.getInventory(),(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos));
        seedHook=()->player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));
        try { menu.quickMoveStack(player,vault?27:5); } finally { seedHook=null; }
        h.assertTrue(player.getInventory().getItem(9).is(Items.DIAMOND) && player.getInventory().getItem(9).getCount()==7
                && menu.getSlot(0).getItem().isEmpty(), "A modded seed query must not destroy a replacement inventory stack or deposit detached seeds"); h.succeed();
    }
    @GameTest(template="empty") public static void auditVaultDepositSourceSwap(GameTestHelper h){sourceSwap(h,true);}
    @GameTest(template="empty") public static void auditBreedingStationDepositSourceSwap(GameTestHelper h){sourceSwap(h,false);}

    private static void cursorSwap(GameTestHelper h, boolean lunch) {
        var player=h.makeMockPlayer(GameType.SURVIVAL); var bag=new ItemStack(lunch?AEItems.LUNCH_BOX.get():AEItems.SEED_BAG.get()); player.getInventory().setItem(0,bag);
        AbstractContainerMenu menu=lunch?new LunchBoxMenu(1,player.getInventory(),0):new SeedBagMenu(1,player.getInventory(),0);
        var original=lunch?new ItemStack(BuiltInRegistries.ITEM.get(FOOD),8):seed(8); menu.setCarried(original);
        Runnable hook=()->menu.setCarried(new ItemStack(Items.DIAMOND,7)); if(lunch)foodHook=hook;else seedHook=hook;
        try { menu.clicked(0,0,ClickType.PICKUP,player); } finally { foodHook=null;seedHook=null; }
        h.assertTrue(menu.getCarried().is(Items.DIAMOND)&&menu.getCarried().getCount()==7&&menu.getSlot(0).getItem().isEmpty(),
                "Changing the cursor during item inspection must preserve the new cursor instead of charging or storing the old stack");h.succeed();
    }
    @GameTest(template="empty") public static void auditSeedBagCursorReplacement(GameTestHelper h){cursorSwap(h,false);}
    @GameTest(template="empty") public static void auditLunchBoxCursorReplacement(GameTestHelper h){cursorSwap(h,true);}

    private static void oversized(GameTestHelper h, boolean lunch) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var contents=net.minecraft.core.NonNullList.withSize(10,ItemStack.EMPTY);
        contents.set(0,new ItemStack(lunch?Items.BREAD:Items.WHEAT_SEEDS,2));contents.set(9,new ItemStack(lunch?Items.APPLE:Items.BEETROOT_SEEDS,7));
        var bag=new ItemStack(lunch?AEItems.LUNCH_BOX.get():AEItems.SEED_BAG.get());bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));player.getInventory().setItem(0,bag);
        AbstractContainerMenu menu=lunch?new LunchBoxMenu(1,player.getInventory(),0):new SeedBagMenu(1,player.getInventory(),0);menu.removed(player);
        h.assertTrue(bag.get(DataComponents.CONTAINER).nonEmptyStream().mapToInt(ItemStack::getCount).sum()==9,
                "Opening and closing an oversized saved container must not silently delete the extra stored stack");h.succeed();
    }
    @GameTest(template="empty") public static void auditOversizedSeedBagContents(GameTestHelper h){oversized(h,false);}
    @GameTest(template="empty") public static void auditOversizedLunchBoxContents(GameTestHelper h){oversized(h,true);}

    private static void invalidLunchUse(GameTestHelper h, int kind) {
        var player=new LunchPlayer(h);player.setShiftKeyDown(true);
        var held=new ItemStack(kind==0?Items.DIAMOND:AEItems.LUNCH_BOX.get());player.setItemInHand(InteractionHand.MAIN_HAND,held);
        if(kind==1)player.spectator=true;if(kind==2)player.setHealth(0);
        var before=held.copy();AEItems.LUNCH_BOX.get().use(kind==3?h.getLevel().getServer().getLevel(Level.NETHER):h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(ItemStack.matches(before,held),"Invalid lunch-box use must not write pause settings into the held item");h.succeed();
    }
    @GameTest(template="empty") public static void auditLunchUseWrongHeldItem(GameTestHelper h){invalidLunchUse(h,0);}
    @GameTest(template="empty") public static void auditLunchUseSpectator(GameTestHelper h){invalidLunchUse(h,1);}
    @GameTest(template="empty") public static void auditLunchUseDeadPlayer(GameTestHelper h){invalidLunchUse(h,2);}
    @GameTest(template="empty") public static void auditLunchUseWrongWorld(GameTestHelper h){invalidLunchUse(h,3);}

    private static void spanner(GameTestHelper h,int kind,boolean mode) {
        var pos=h.absolutePos(new BlockPos(4,210,4));Block block=kind==0?AEBlocks.IRRIGATION_PIPE.get():kind==1?AEBlocks.IRRIGATION_SPRINKLER.get():AEBlocks.IRRIGATION_CONTROLLER.get();
        h.getLevel().setBlock(pos,block.defaultBlockState(),2);var before=h.getLevel().getBlockState(pos);var player=new AuditPlayer(h,pos);player.setShiftKeyDown(kind==2);
        var tool=new ItemStack(AEItems.SPANNER.get());player.setItemInHand(InteractionHand.MAIN_HAND,tool);var beforeTool=tool.copy();
        player.permission=()->{if(mode)player.setShiftKeyDown(!player.isShiftKeyDown());else player.getAbilities().mayBuild=false;};
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.EAST,pos,false);
        if(kind==2) AEItems.SPANNER.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        else SpannerItem.onRightClick(new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
        boolean unchanged=h.getLevel().getBlockState(pos)==before&&ItemStack.matches(beforeTool,tool);
        if(kind==1)unchanged&=!((IrrigationSprinklerBlockEntity)h.getLevel().getBlockEntity(pos)).smartWatering();
        h.assertTrue(unchanged,"Spanner actions must recheck build access and the original mode after permissions");h.succeed();
    }
    @GameTest(template="empty") public static void auditSpannerRevokedPipeAccess(GameTestHelper h){spanner(h,0,false);}
    @GameTest(template="empty") public static void auditSpannerRevokedSprinklerAccess(GameTestHelper h){spanner(h,1,false);}
    @GameTest(template="empty") public static void auditSpannerRevokedControllerAccess(GameTestHelper h){spanner(h,2,false);}
    @GameTest(template="empty") public static void auditSpannerChangedSprinklerMode(GameTestHelper h){spanner(h,1,true);}
    @GameTest(template="empty") public static void auditSpannerChangedControllerMode(GameTestHelper h){spanner(h,2,true);}

    @GameTest(template="empty") public static void auditRecoveredUpgradeSeesInstalledWear(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        h.getLevel().setBlock(pos,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,1),2);
        ((AECropBlockEntity)h.getLevel().getBlockEntity(pos)).setSupportTicks(1234);
        var player=new AuditPlayer(h,pos);var iron=new ItemStack(AEItems.IRON_CROP_SUPPORT.get(),2);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,iron,t->t.putInt("SupportTicks",50000));player.setItemInHand(InteractionHand.MAIN_HAND,iron);
        var observed=new ItemStack[1];Consumer<BlockEvent.NeighborNotifyEvent> listener=event->{if(observed[0]==null&&event.getPos().equals(pos)&&CropSupportItem.tier(h.getLevel().getBlockState(pos))==2)observed[0]=((AECropBlockEntity)h.getLevel().getBlockEntity(pos)).recoverSupport();};
        NeoForge.EVENT_BUS.addListener(listener);try{AEItems.IRON_CROP_SUPPORT.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));}finally{NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(observed[0]!=null&&CropSupportItem.remaining(observed[0])==50000,"Upgrade notifications must expose the new support's life, not the previous tier's countdown");h.succeed();
    }
    @GameTest(template="empty") public static void auditLunchPreviewRejectsPlayerlessClicks(GameTestHelper h) {
        var menu=new LunchBoxMenu(1,new Inventory(null),-1);menu.getSlot(0).set(new ItemStack(Items.BREAD,3));menu.clicked(0,0,ClickType.PICKUP,null);
        h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(0).getItem().getCount()==3,"Playerless lunch preview clicks should return safely");h.succeed();
    }
    @GameTest(template="empty") public static void auditViewportReservesFooterAtInitialization(GameTestHelper h) {
        var viewport=new com.agrievolution.client.NativeGuiViewport();viewport.init(430,240,410,220);
        h.assertTrue(!viewport.oversized()||viewport.y()<=4,"An oversized screen must initialize inside the same footer-reserved bounds used for scrolling");h.succeed();
    }

    private static void changedSources(GameTestHelper h, int kind) {
        for (boolean vault : new boolean[]{true,false}) {
            var pos = h.absolutePos(new BlockPos(vault ? 4 : 6,210,4));
            h.getLevel().setBlock(pos,(vault ? AEBlocks.SEED_VAULT.get() : AEBlocks.SEED_BREEDING_STATION.get()).defaultBlockState(),2);
            var player = new AuditPlayer(h,pos);
            var source = seed(8);
            source.set(DataComponents.CUSTOM_NAME,Component.literal("Saved seed line"));
            player.getInventory().setItem(9,source);
            AbstractContainerMenu menu = vault ? new SeedVaultMenu(1,player.getInventory(),(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos))
                    : new SeedBreedingStationMenu(1,player.getInventory(),(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos));
            int first = vault ? 27 : 5;
            var expected = new ItemStack[1];
            seedHook = () -> {
                if (kind == 0) source.grow(3);
                else if (kind == 1) source.set(DataComponents.CUSTOM_NAME,Component.literal("New seed line"));
                else if (kind == 2) menu.removed(player);
                else h.assertTrue(menu.quickMoveStack(player,first).isEmpty(),"Nested seed transfers are rejected");
                expected[0] = source.copy();
            };
            int queries = seedQueries;
            ItemStack result;
            try { result = menu.quickMoveStack(player,first); } finally { seedHook = null; }
            if (kind < 3) h.assertTrue(result.isEmpty() && menu.getSlot(0).getItem().isEmpty() && ItemStack.matches(expected[0],source),
                    "Changed seed inputs and closed menus cancel the old deposit");
            else h.assertTrue(!result.isEmpty() && menu.getSlot(0).getItem().getCount() == 8 && player.getInventory().getItem(9).isEmpty()
                    && seedQueries - queries == 1 && menu.getSlot(0).getItem().getHoverName().getString().equals("Saved seed line"),
                    "Only the original transfer runs, using one seed query and retaining its components");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void changedDepositCountIsPreserved(GameTestHelper h){changedSources(h,0);}
    @GameTest(template="empty") public static void changedDepositComponentsArePreserved(GameTestHelper h){changedSources(h,1);}
    @GameTest(template="empty") public static void closingDuringDepositCancelsTransfer(GameTestHelper h){changedSources(h,2);}
    @GameTest(template="empty") public static void depositQueriesOnceAndRejectsNestedTransfers(GameTestHelper h){changedSources(h,3);}

    @GameTest(template="empty") public static void machineCursorReplacementCancelsInsertion(GameTestHelper h) {
        for (boolean vault : new boolean[]{true,false}) {
            var pos = h.absolutePos(new BlockPos(vault ? 4 : 6,210,4));
            h.getLevel().setBlock(pos,(vault ? AEBlocks.SEED_VAULT.get() : AEBlocks.SEED_BREEDING_STATION.get()).defaultBlockState(),2);
            var player = new AuditPlayer(h,pos);
            AbstractContainerMenu menu = vault ? new SeedVaultMenu(1,player.getInventory(),(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos))
                    : new SeedBreedingStationMenu(1,player.getInventory(),(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos));
            menu.setCarried(seed(8));
            seedHook = () -> menu.setCarried(new ItemStack(Items.DIAMOND,7));
            try { menu.clicked(0,0,ClickType.PICKUP,player); } finally { seedHook = null; }
            h.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 7 && menu.getSlot(0).getItem().isEmpty(),
                    "Machine clicks use the same cursor checks as portable storage");
        }
        h.succeed();
    }

    private static void portableClicks(GameTestHelper h, int kind) {
        for (boolean lunch : new boolean[]{false,true}) {
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var bag = new ItemStack(lunch ? AEItems.LUNCH_BOX.get() : AEItems.SEED_BAG.get());
            player.getInventory().setItem(0,bag);
            AbstractContainerMenu menu = lunch ? new LunchBoxMenu(1,player.getInventory(),0) : new SeedBagMenu(1,player.getInventory(),0);
            var input = lunch ? new ItemStack(BuiltInRegistries.ITEM.get(FOOD),8) : seed(8);
            input.set(DataComponents.CUSTOM_NAME,Component.literal("Packed supply"));
            var original = input.copy();
            if (kind == 1 || kind == 4) player.getInventory().setItem(1,input); else menu.setCarried(input);
            Runnable callback = () -> {
                if (kind == 0) input.set(DataComponents.CUSTOM_NAME,Component.literal("Changed supply"));
                else if (kind == 1) player.getInventory().setItem(1,new ItemStack(Items.DIAMOND,7));
                else if (kind == 2) menu.getSlot(0).set(new ItemStack(Items.DIAMOND,7));
            };
            if (lunch) foodHook = callback; else seedHook = callback;
            try { menu.clicked(0,kind == 1 || kind == 3 || kind == 4 ? 1 : 0,
                    kind == 1 || kind == 4 ? ClickType.SWAP : ClickType.PICKUP,player); }
            finally { seedHook = null; foodHook = null; }
            if (kind == 0) h.assertTrue(menu.getCarried() == input && input.getCount() == 8 && menu.getSlot(0).getItem().isEmpty(),
                    "A changed cursor component is preserved");
            else if (kind == 1) h.assertTrue(player.getInventory().getItem(1).is(Items.DIAMOND) && player.getInventory().getItem(1).getCount() == 7
                    && menu.getSlot(0).getItem().isEmpty(),"A changed hotbar source is preserved");
            else if (kind == 2) h.assertTrue(menu.getSlot(0).getItem().is(Items.DIAMOND) && menu.getCarried() == input && input.getCount() == 8,
                    "A newer destination survives the pending insertion");
            else {
                int count = kind == 3 ? 1 : 8;
                h.assertTrue(ItemStack.matches(original.copyWithCount(count),menu.getSlot(0).getItem())
                        && bag.get(DataComponents.CONTAINER).nonEmptyStream().mapToInt(ItemStack::getCount).sum() == count,
                        "Ordinary right-click and hotbar insertion preserve and save the supplied item data");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void changedCursorComponentsArePreserved(GameTestHelper h){portableClicks(h,0);}
    @GameTest(template="empty") public static void changedHotbarSourcesArePreserved(GameTestHelper h){portableClicks(h,1);}
    @GameTest(template="empty") public static void changedInsertionDestinationsArePreserved(GameTestHelper h){portableClicks(h,2);}
    @GameTest(template="empty") public static void ordinaryPortableRightClickSavesComponents(GameTestHelper h){portableClicks(h,3);}
    @GameTest(template="empty") public static void ordinaryPortableHotbarInsertionSavesComponents(GameTestHelper h){portableClicks(h,4);}

    @GameTest(template="empty") public static void overflowSurvivesEditingAndReopening(GameTestHelper h) {
        for (boolean lunch : new boolean[]{false,true}) {
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var saved = net.minecraft.core.NonNullList.withSize(256,ItemStack.EMPTY);
            var extra = new ItemStack(lunch ? Items.APPLE : Items.BEETROOT_SEEDS,7);
            extra.set(DataComponents.CUSTOM_NAME,Component.literal("Imported supplies"));
            saved.set(255,extra);
            var bag = new ItemStack(lunch ? AEItems.LUNCH_BOX.get() : AEItems.SEED_BAG.get());
            bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(saved));
            player.getInventory().setItem(0,bag);
            for (int iteration = 0; iteration < 2; iteration++) {
                AbstractContainerMenu menu = lunch ? new LunchBoxMenu(1,player.getInventory(),0) : new SeedBagMenu(1,player.getInventory(),0);
                menu.getSlot(0).set(new ItemStack(lunch ? Items.BREAD : Items.WHEAT_SEEDS,iteration + 2));
                menu.removed(player);
                var contents = bag.get(DataComponents.CONTAINER);
                h.assertTrue(contents.getSlots() == 256 && ItemStack.matches(contents.getStackInSlot(255),extra)
                        && contents.getStackInSlot(0).getCount() == iteration + 2,"Edits and reopen preserve overflow slots and their components");
            }
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void normalLunchUseTogglesAndOpensBothHands(GameTestHelper h) {
        var player = new LunchPlayer(h);
        for (var hand : InteractionHand.values()) {
            var box = new ItemStack(AEItems.LUNCH_BOX.get());
            player.setItemInHand(hand,box);
            player.setShiftKeyDown(true);
            h.assertTrue(AEItems.LUNCH_BOX.get().use(h.getLevel(),player,hand).getResult().consumesAction()
                    && !LunchBoxItem.enabled(box),"Normal crouch use toggles the actual held box");
            player.setShiftKeyDown(false);
            AEItems.LUNCH_BOX.get().use(h.getLevel(),player,hand);
            h.assertTrue(player.containerMenu instanceof LunchBoxMenu && player.containerMenu.stillValid(player),"Both hands open a valid Lunch Box menu");
            player.closeContainer();
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void portableDragDistributionRetainsSupplies(GameTestHelper h) {
        for (boolean lunch : new boolean[]{false,true}) {
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().setItem(0,new ItemStack(lunch ? AEItems.LUNCH_BOX.get() : AEItems.SEED_BAG.get()));
            AbstractContainerMenu menu = lunch ? new LunchBoxMenu(1,player.getInventory(),0) : new SeedBagMenu(1,player.getInventory(),0);
            menu.setCarried(lunch ? new ItemStack(BuiltInRegistries.ITEM.get(FOOD),8) : seed(8));
            menu.clicked(-999,0,ClickType.QUICK_CRAFT,player);
            menu.clicked(0,1,ClickType.QUICK_CRAFT,player);
            menu.clicked(1,1,ClickType.QUICK_CRAFT,player);
            menu.clicked(-999,2,ClickType.QUICK_CRAFT,player);
            h.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == 4 && menu.getSlot(1).getItem().getCount() == 4,
                    "Dragging splits supplies between the selected slots without losing items");
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void notificationRecoveryRetainsNewSupportWear(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4,210,4));
        h.getLevel().setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        h.getLevel().setBlock(pos,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,1),2);
        ((AECropBlockEntity)h.getLevel().getBlockEntity(pos)).setSupportTicks(1234);
        var installer = new AuditPlayer(h,pos);
        var remover = new AuditPlayer(h,pos);
        remover.setShiftKeyDown(true);
        remover.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AEItems.SPANNER.get()));
        var iron = new ItemStack(AEItems.IRON_CROP_SUPPORT.get(),2);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,iron,t->t.putInt("SupportTicks",50000));
        installer.setItemInHand(InteractionHand.MAIN_HAND,iron);
        boolean[] removed = {false};
        Consumer<BlockEvent.NeighborNotifyEvent> callback = event -> {
            if (!removed[0] && event.getPos().equals(pos) && CropSupportItem.tier(h.getLevel().getBlockState(pos)) == 2) {
                removed[0] = true;
                AEItems.SPANNER.get().useOn(new UseOnContext(remover,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
            }
        };
        NeoForge.EVENT_BUS.addListener(callback);
        try { AEItems.IRON_CROP_SUPPORT.get().useOn(new UseOnContext(installer,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false))); }
        finally { NeoForge.EVENT_BUS.unregister(callback); }
        var returned = remover.getInventory().items.stream().filter(s->s.is(AEItems.IRON_CROP_SUPPORT.get())).toList();
        h.assertTrue(removed[0] && CropSupportItem.tier(h.getLevel().getBlockState(pos)) == 0 && iron.getCount() == 1
                && returned.stream().mapToInt(ItemStack::getCount).sum() == 1 && CropSupportItem.remaining(returned.getFirst()) == 50000
                && installer.getInventory().items.stream().anyMatch(s->s.is(AEItems.CROP_SUPPORT.get()) && CropSupportItem.remaining(s) == 1234),
                "Immediate recovery sees the new wear, charges one upgrade and returns each support once");
        h.succeed();
    }

    @GameTest(template="empty") public static void viewportScrollKeepsReservedFooterBounds(GameTestHelper h) {
        var viewport = new com.agrievolution.client.NativeGuiViewport();
        viewport.init(430,240,410,220);
        h.assertTrue(viewport.y() == 4,"Oversized content starts at its upper scrolling bound");
        viewport.scroll(0,-50,false);
        h.assertTrue(viewport.y() == 0,"Bottom scrolling keeps the 16-pixel footer and four-pixel margin");
        viewport.scroll(0,50,false);
        h.assertTrue(viewport.y() == 4,"Upward scrolling returns to the original position");
        viewport.init(430,300,410,220);
        h.assertTrue(!viewport.oversized() && viewport.y() == 32 && !viewport.scroll(0,-1,false),"Fitting screens center inside the same usable area");
        h.succeed();
    }
}
