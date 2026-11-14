package com.agrievolution.preview;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.client.GardenWorkScreen;
import com.agrievolution.client.SeedVaultScreen;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.client.analyzer.SeedAnalyzerScreen;
import com.agrievolution.client.breeding.SeedBreedingStationScreen;
import com.agrievolution.client.greenhouse.*;
import com.agrievolution.client.journal.JournalScreen;
import com.agrievolution.client.laboratory.BotanicalGeneticsLaboratoryScreen;
import com.agrievolution.client.soil.*;
import com.agrievolution.content.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Uses disposable client menu data and never opens a world or sends inventory actions. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class GardenGuiPreview {
    private static int ticks, index=-1;
    private static final String[] STICK_MAP_NAMES = {"greenhouse-stick-map", "greenhouse-stick-map-tooltip", "greenhouse-weed-map", "greenhouse-weed-map-tooltip"};
    private static final String[] NATIVE_BREEDING_NAMES = {"native-breeding-compost", "native-breeding-progress", "native-breeding-mismatch"};
    private static final String[] VERTICAL_NAMES = {"vertical-greenhouse-menu", "vertical-greenhouse-model",
            "vertical-greenhouse-fixed-pair", "garden-order-board", "preserving-station", "garden-work-models"};
    private static final String[] PROGRESSION_NAMES = {"expanded-orders", "garden-commissions", "seed-vault", "garden-progression-models"};
    private static final String[] NAMES={"analyzer","breeding","laboratory","greenhouse","harvest","seed_bank","storage","soil_research","soil_sampler","journal","journal_compact","seed_bag_layout","building_journal","building_journal_compact","greenhouse_0","greenhouse_1","greenhouse_2","greenhouse_3","greenhouse_4","greenhouse_5","greenhouse_6","greenhouse_7","breeding_planner","breeding_planner_compact"};
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(Boolean.getBoolean("agri.cropPreview")) return;
        if(Boolean.getBoolean("agri.stickPreview")||Boolean.getBoolean("agri.lunchControlsPreview")||Boolean.getBoolean("agri.selectivePlantingPreview")||Boolean.getBoolean("agri.plannerPreview")||Boolean.getBoolean("agri.journalIndexPreview")||Boolean.getBoolean("agri.machinePreview")||Boolean.getBoolean("agri.irrigationPreview")||Boolean.getBoolean("agri.grandmasPreview")) return;
        boolean stickMapOnly = Boolean.getBoolean("agri.greenhouseStickPreview");
        boolean nativeBreeding = Boolean.getBoolean("agri.nativeBreedingPreview");
        boolean vertical = Boolean.getBoolean("agri.verticalGreenhousePreview");
        boolean progression = Boolean.getBoolean("agri.gardenProgressionPreview");
        String[] names = progression ? PROGRESSION_NAMES : vertical ? VERTICAL_NAMES : nativeBreeding ? NATIVE_BREEDING_NAMES : stickMapOnly ? STICK_MAP_NAMES : NAMES;
        var mc=Minecraft.getInstance();
        if(index<0 && (mc.screen == null || mc.getOverlay() != null)) return;
        if(++ticks<50) return;
        ticks=0;
        if(index>=0 && index<names.length) Screenshot.grab(mc.gameDirectory,names[index]+".png",mc.getMainRenderTarget(),
                message->System.out.println("GUI_PREVIEW_CAPTURE: "+message.getString()));
        index++;
        if(index>=names.length) { mc.stop(); return; }
        mc.options.guiScale().set(2); mc.resizeDisplay();
        mc.setScreen(new Wrapper(progression ? 37 + index : vertical ? 31 + index : nativeBreeding ? 28 + index : stickMapOnly ? 24 + index : index));
    }
    private static final class Wrapper extends Screen {
        private final int which;
        private Screen inner;
        Wrapper(int which) { super(Component.literal("Garden GUI visual QA")); this.which=which; }
        @Override protected void init() {
            var inv=new Inventory(null); var name=Component.literal("Agri Evolution");
            inner=switch(which) {
                case 0 -> new SeedAnalyzerScreen(ItemStack.EMPTY,-1);
                case 1 -> new SeedBreedingStationScreen(new SeedBreedingStationMenu(1,inv),inv,name);
                case 2 -> new BotanicalGeneticsLaboratoryScreen(new BotanicalGeneticsLaboratoryMenu(1,inv),inv,name);
                case 3 -> new BotanicalGreenhouseScreen(new BotanicalGreenhouseMenu(1,inv),inv,name);
                case 4 -> new BotanicalGreenhouseHarvestScreen(new BotanicalGreenhouseHarvestMenu(1,inv),inv,name);
                case 5 -> new BotanicalGreenhouseSeedBankScreen(new BotanicalGreenhouseSeedBankMenu(1,inv),inv,name);
                case 6 -> new BotanicalGreenhouseStorageScreen(new BotanicalGreenhouseStorageMenu(1,inv),inv,name);
                case 7 -> new SoilResearchTableScreen(new SoilResearchTableMenu(1,inv),inv,name);
                case 8 -> new SoilSamplerScreen(ItemStack.EMPTY);
                case 9,10 -> new JournalScreen(ItemStack.EMPTY);
                case 22,23 -> new com.agrievolution.client.journal.BreedingPlannerScreen(new TitleScreen(),com.agrievolution.client.journal.JournalSnapshot.from(ItemStack.EMPTY));
                case 12,13 -> new JournalScreen(new ItemStack(AEItems.JOURNAL.get()));
                case 28,29,30 -> nativeBreeding(inv, name);
                case 31,33 -> verticalGreenhouse(inv);
                case 32 -> new VerticalModel();
                case 34,35 -> gardenWork(inv, which == 35);
                case 36 -> new GardenWorkModels();
                case 37,38 -> gardenWork(inv, false);
                case 39 -> seedVault(inv);
                case 40 -> new GardenWorkModels(true);
                default -> which>=14?greenhouse(inv,name):null;
            };
            if(inner!=null) inner.init(minecraft,which==10||which==13||which==23?400:width,which==10||which==13||which==23?240:height);
            if(which>=14&&which<22)inner.mouseClicked((width-410)/2+Math.round((37+(which-14)*57)*410f/482f),(height-206)/2+Math.round(54*206f/228f),0);
            if (which >= 24 && which <= 27) inner.mouseClicked((width-410)/2 + Math.round(151*410f/482f), (height-206)/2 + Math.round(54*206f/228f), 0);
            if (which == 26 || which == 27) inner.mouseClicked((width-410)/2 + Math.round(429*410f/482f), (height-206)/2 + Math.round(89*206f/228f), 0);
            if (which == 39) {
                int x = (width - 320) / 2 + SeedVaultMenu.FIRST_X;
                int y = (height - 214) / 2 + SeedVaultMenu.FIRST_Y;
                inner.mouseClicked(x + 8, y + 8, 2);
                inner.mouseClicked(x + 26, y + 8, 2);
            }
        }
        private Screen seedVault(Inventory inventory) {
            var menu = new SeedVaultMenu(1, inventory);
            var first = new ItemStack(AEItems.CRIMSON_CLOVER_SEED.get(), 9);
            var second = new ItemStack(AEItems.AZURE_FLAX_SEED.get(), 7);
            GenomeStorage.putOnSeed(first, new CropGenome(90, 45, 67));
            GenomeStorage.putOnSeed(second, new CropGenome(55, 82, 64));
            com.agrievolution.genetics.RareTraitStorage.putOnSeed(first, com.agrievolution.genetics.RareTrait.VIGOROUS);
            menu.getSlot(0).set(first);
            menu.getSlot(1).set(second);
            menu.getSlot(2).set(new ItemStack(Items.BEETROOT_SEEDS, 32));
            menu.getSlot(11).set(new ItemStack(AEItems.TOMATO_SEED.get(), 5));
            menu.setData(0, 1);
            return new SeedVaultScreen(menu, inventory, AEBlocks.SEED_VAULT.get().getName());
        }
        private Screen gardenWork(Inventory inventory, boolean preserving) {
            var menu = new GardenWorkMenu(1, inventory);
            menu.setData(0, preserving ? 1 : 0);
            menu.setData(1, 2);
            menu.setData(2, 1);
            if (which == 38) {
                menu.setData(4, GardenWork.ROWS);
                menu.setData(5, 1);
            }
            var items = preserving
                    ? new ItemStack[]{new ItemStack(AEItems.STRAWBERRY.get(), 8), new ItemStack(AEItems.BLUEBERRY.get(), 9),
                        new ItemStack(Items.SUGAR, 4), new ItemStack(Items.GLASS_BOTTLE, 4)}
                    : new ItemStack[]{new ItemStack(AEItems.TOMATO.get(), 6), new ItemStack(AEItems.LETTUCE.get(), 6),
                        new ItemStack(AEItems.CUCUMBER.get(), 4)};
            for (int i = 0; i < items.length; i++) inventory.setItem(9 + i, items[i]);
            var title = preserving ? AEBlocks.PRESERVING_STATION.get().getName() : AEBlocks.GARDEN_ORDER_BOARD.get().getName();
            return new GardenWorkScreen(menu, inventory, title);
        }
        private Screen verticalGreenhouse(Inventory inv) {
            var menu = new VerticalGreenhouseMenu(1, inv);
            var seeds = new net.minecraft.world.item.Item[]{AEItems.WILD_SEED.get(), AEItems.AZURE_FLAX_SEED.get(), Items.BEETROOT_SEEDS,
                    Items.BEETROOT_SEEDS, Items.PITCHER_POD, Items.PUMPKIN_SEEDS};
            for (int i = 0; i < seeds.length; i++) {
                menu.getSlot(i).set(new ItemStack(seeds[i]));
                menu.setData(VerticalGreenhouseBlockEntity.PROGRESS_START + i, 12 + i * 13);
            }
            menu.getSlot(VerticalGreenhouseBlockEntity.HARVEST).set(new ItemStack(Items.WHEAT, 5));
            menu.getSlot(VerticalGreenhouseBlockEntity.HARVEST + 1).set(new ItemStack(Items.BEETROOT, 4));
            menu.getSlot(VerticalGreenhouseBlockEntity.OFFSPRING).set(new ItemStack(Items.BEETROOT_SEEDS, 3));
            menu.setData(0, 12250);
            menu.setData(1, 3);
            menu.setData(2, VerticalGreenhouseBlockEntity.Status.RUNNING.ordinal());
            menu.setData(3, VerticalGreenhouseBlockEntity.Status.RUNNING.ordinal());
            menu.setData(4, 57);
            menu.setData(5, 1);
            menu.setData(6, 2);
            menu.setData(VerticalGreenhouseBlockEntity.WATER_STATUS, VerticalGreenhouseBlockEntity.WaterStatus.READY.ordinal());
            menu.setData(VerticalGreenhouseBlockEntity.ROUTE_START, VerticalGreenhouseBlockEntity.OutputRoute.BOTH.ordinal());
            if (which == 33) {
                menu.setData(VerticalGreenhouseBlockEntity.PAIR_MODE, 1);
                menu.setData(VerticalGreenhouseBlockEntity.SELECTED_A, 3);
                menu.setData(VerticalGreenhouseBlockEntity.SELECTED_B, 4);
                menu.setData(5, 3);
                menu.setData(6, 4);
                menu.setData(VerticalGreenhouseBlockEntity.WATER_INTAKE, VerticalGreenhouseBlockEntity.WaterIntake.KEEP_FULL.ordinal());
                menu.setData(VerticalGreenhouseBlockEntity.WATER_STATUS, VerticalGreenhouseBlockEntity.WaterStatus.BLOCKED_VALVE.ordinal());
                menu.setData(VerticalGreenhouseBlockEntity.ROUTE_START + 2, VerticalGreenhouseBlockEntity.OutputRoute.SEEDS.ordinal());
                menu.setData(VerticalGreenhouseBlockEntity.BATCH_TARGET, 12);
                menu.setData(VerticalGreenhouseBlockEntity.BATCH_COUNT, 3);
            }
            return new VerticalGreenhouseScreen(menu, inv, Component.translatable("block.agri_evolution.vertical_greenhouse"));
        }
        private Screen nativeBreeding(Inventory inv, Component name) {
            var menu = new SeedBreedingStationMenu(1, inv);
            menu.getSlot(0).set(new ItemStack(Items.PITCHER_POD));
            menu.getSlot(1).set(new ItemStack(which == 30 ? Items.WHEAT_SEEDS : Items.PITCHER_POD));
            if (which == 29) {
                menu.getSlot(2).set(new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 4));
                menu.setData(0, 56);
                menu.setData(1, 140);
            }
            return new SeedBreedingStationScreen(menu, inv, name);
        }
        private Screen greenhouse(Inventory inv,Component name) {
            var menu=new BotanicalGreenhouseMenu(1,inv);
            for(int i=0;i<51;i++)menu.setData(i,1);
            menu.setData(0,3200);menu.setData(1,4000);menu.setData(4,25);menu.setData(5,25);
            menu.setData(6,20);menu.setData(7,15);menu.setData(8,5);menu.setData(10,15);
            menu.setData(12,23);menu.setData(13,60);menu.setData(16,500);menu.setData(17,35);
            menu.setData(28,128);menu.setData(29,123);menu.setData(30,5);
            int map = BotanicalGreenhouseBlockEntity.STICK_MAP_START;
            int[] weedData = new int[GreenhouseWeedStatus.DATA_COUNT];
            for (int plot = 0; plot < GreenhouseStickStatus.PLOT_COUNT; plot++) {
                int sample = switch (plot) {
                    case 0, 1, 5, 10 -> GreenhouseStickStatus.encode(1, 21000);
                    case 3, 4, 8, 9 -> GreenhouseStickStatus.encode(2, 96000);
                    case 12, 13, 17 -> GreenhouseStickStatus.OBSIDIAN;
                    case 20 -> GreenhouseStickStatus.encode(1, 800);
                    case 23 -> GreenhouseStickStatus.encode(2, 960);
                    case 14 -> GreenhouseStickStatus.UNAVAILABLE;
                    case 16, 21, 22 -> GreenhouseStickStatus.UNSUPPORTED;
                    default -> GreenhouseStickStatus.NO_CROP;
                };
                menu.setData(map + plot, sample);
                GreenhouseWeedStatus weeds = switch (plot) {
                    case 3, 10, 20 -> GreenhouseWeedStatus.WEEDY;
                    case 14 -> GreenhouseWeedStatus.UNAVAILABLE;
                    default -> sample == GreenhouseStickStatus.NO_CROP ? GreenhouseWeedStatus.NO_CROP : GreenhouseWeedStatus.CLEAR;
                };
                weedData[GreenhouseWeedStatus.dataIndex(plot)] |= weeds.bits(plot);
            }
            for (int i = 0; i < weedData.length; i++) menu.setData(BotanicalGreenhouseBlockEntity.WEED_MAP_START + i, weedData[i]);
            return new BotanicalGreenhouseScreen(menu,inv,name);
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial) {
            if (which == 25 || which == 27) {
                inner.render(g, (width-410)/2 + Math.round(367*410f/482f), (height-206)/2 + Math.round(184*206f/228f), partial);
                return;
            }
            if(inner!=null) { inner.render(g,-100,-100,partial); return; }
            // Bag construction needs a live player; preview its exact existing slot layout only.
            super.render(g,mx,my,partial);
            int x=(width-268)/2,y=(height-150)/2;
            GardenGuiTheme.panel(g,x-2,y-2,272,154,0xFFE0D1AE);
            g.drawString(font,"Seed Bag",x+8,y+6,0xFF302518,false);
            g.drawString(font,"Inventory",x+8,y+56,0xFF302518,false);
            com.agrievolution.client.SeedBagScreen.actionButton("Deposit seeds",x+178,y+28,()->{}).render(g,-100,-100,partial);
            com.agrievolution.client.SeedBagScreen.actionButton("Sort species",x+178,y+50,()->{}).render(g,-100,-100,partial);
            g.drawWordWrap(font,Component.literal("Different genetics stay separate. Full bag? Extras remain in your inventory."),x+181,y+76,78,0x302518);
            for(int col=0;col<9;col++) {
                GardenGuiTheme.slot(g,x+8+18*col,y+28);
                for(int row=0;row<3;row++) GardenGuiTheme.slot(g,x+8+18*col,y+68+18*row);
                GardenGuiTheme.slot(g,x+8+18*col,y+126);
            }
        }
    }

    private static final class GardenWorkModels extends Screen {
        private final boolean progression;
        GardenWorkModels() { this(false); }
        GardenWorkModels(boolean progression) {
            super(Component.literal("Garden workstations"));
            this.progression = progression;
        }

        @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
            gui.fill(0, 0, width, height, 0xFF362B24);
            if (progression) {
                drawBlock(gui, AEBlocks.COMPOST_BIN.get().defaultBlockState().setValue(CompostBinBlock.FILL, CompostBinBlock.READY), width / 4);
                drawBlock(gui, AEBlocks.SEED_VAULT.get().defaultBlockState(), width * 3 / 4);
                return;
            }
            drawBlock(gui, AEBlocks.GARDEN_ORDER_BOARD.get(), width / 4);
            drawBlock(gui, AEBlocks.PRESERVING_STATION.get(), width * 3 / 4);
            var jars = new ItemStack[]{new ItemStack(AEItems.BERRY_JAM.get()),
                    new ItemStack(AEItems.DILL_PICKLES.get()), new ItemStack(AEItems.GARDEN_SEASONING.get())};
            for (int i = 0; i < jars.length; i++) {
                var pose = gui.pose();
                pose.pushPose();
                pose.translate(width / 2 + (i - 1) * 62 - 20, height - 65, 0);
                pose.scale(2.5f, 2.5f, 1);
                gui.renderItem(jars[i], 0, 0);
                pose.popPose();
            }
        }

        private void drawBlock(GuiGraphics gui, net.minecraft.world.level.block.Block block, int x) {
            drawBlock(gui, block.defaultBlockState(), x);
        }

        private void drawBlock(GuiGraphics gui, net.minecraft.world.level.block.state.BlockState state, int x) {
            gui.drawCenteredString(font, state.getBlock().getName(), x, 22, 0xFFF1DFB8);
            gui.flush();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var pose = gui.pose();
            pose.pushPose();
            pose.translate(x, height - 95, 150);
            pose.scale(105, -105, 105);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(23));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(146));
            pose.translate(-0.5, 0, -0.5);
            var buffers = minecraft.renderBuffers().bufferSource();
            minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            pose.popPose();
            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        }
    }

    private static final class VerticalModel extends Screen {
        VerticalModel() { super(Component.translatable("block.agri_evolution.vertical_greenhouse")); }

        @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
            gui.fill(0, 0, width, height, 0xFF362B24);
            gui.drawCenteredString(font, title, width / 2, 16, 0xFFF1DFB8);
            int front = width / 3, rear = width * 2 / 3;
            gui.drawCenteredString(font, "Front", front, height - 17, 0xFFF1DFB8);
            gui.drawCenteredString(font, "Rear", rear, height - 17, 0xFFF1DFB8);
            gui.flush();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            drawColumn(gui, front, 146);
            drawColumn(gui, rear, -34);
            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        }

        private void drawColumn(GuiGraphics gui, int x, float yaw) {
            var pose = gui.pose();
            pose.pushPose();
            pose.translate(x, height - 45, 150);
            pose.scale(65, -65, 65);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(16));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int section = 0; section < 3; section++) {
                pose.pushPose();
                pose.translate(-0.5, section, -0.5);
                var state = AEBlocks.VERTICAL_GREENHOUSE.get().defaultBlockState().setValue(VerticalGreenhouseBlock.SECTION, section);
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
