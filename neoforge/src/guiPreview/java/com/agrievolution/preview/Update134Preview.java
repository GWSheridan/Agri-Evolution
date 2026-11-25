package com.agrievolution.preview;

import com.agrievolution.client.*;
import com.agrievolution.client.greenhouse.*;
import com.agrievolution.client.journal.JournalScreen;
import com.agrievolution.content.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Disposable menus at two logical resolutions; this harness never opens a save. */
@EventBusSubscriber(modid = "agri_evolution", value = Dist.CLIENT)
public final class Update134Preview {
    private static int ticks, index = -1;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("agri.update134Preview")) return;
        var mc = Minecraft.getInstance();
        if (mc.screen == null || mc.getOverlay() != null || ++ticks < 32) return;
        ticks = 0;
        if (index >= 0) Screenshot.grab(mc.gameDirectory, "update134-" + index + ".png", mc.getMainRenderTarget(),
                message -> System.out.println("UPDATE134_CAPTURE: " + message.getString()));
        if (++index >= 19) { mc.stop(); return; }
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        mc.setScreen(new Preview(index));
    }

    private static final class Preview extends Screen {
        private final int index;
        private Screen inner;
        private int viewportWidth, viewportHeight;
        Preview(int index) { super(Component.literal("1.3.4 visual checks")); this.index = index; }
        @Override protected void init() {
            var inventory = new Inventory(null);
            var title = Component.literal("Agri Evolution");
            int kind = index < 16 ? index % 8 : index == 16 ? 0 : index == 17 ? 3 : 4;
            inner = switch (kind) {
                case 0 -> new JournalScreen(new ItemStack(AEItems.BREEDING_JOURNAL.get()));
                case 1 -> new JournalScreen(new ItemStack(AEItems.JOURNAL.get()));
                case 2 -> new BotanicalGreenhouseScreen(new BotanicalGreenhouseMenu(1, inventory), inventory, title);
                case 3 -> new VerticalGreenhouseScreen(new VerticalGreenhouseMenu(1, inventory), inventory, title);
                case 4 -> new BatchCookingScreen(new BatchCookingMenu(1, inventory), inventory, title);
                case 5 -> new IrrigationControllerScreen(controller(inventory), inventory, title);
                case 6 -> new SeedVaultScreen(new SeedVaultMenu(1, inventory), inventory, title);
                default -> new BotanicalGreenhouseSeedBankScreen(new BotanicalGreenhouseSeedBankMenu(1, inventory), inventory, title);
            };
            viewportWidth = index < 8 || index >= 16 ? 320 : width;
            viewportHeight = index < 8 || index >= 16 ? 240 : height;
            inner.init(minecraft, viewportWidth, viewportHeight);
            if (index >= 16) for (int step = 0; step < 12; step++) {
                inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);
                inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN, 0, org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);
            }
        }
        @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
            gui.fill(0, 0, width, height, 0xff171d19);
            gui.enableScissor(0, 0, viewportWidth, viewportHeight);
            inner.render(gui, -100, -100, delta);
            gui.disableScissor();
            if (viewportWidth < width) gui.drawString(font, viewportWidth + " x " + viewportHeight + " logical pixels", viewportWidth + 6, 8, 0xffeeeecc);
        }
        @Override public boolean isPauseScreen() { return false; }

        private IrrigationControllerMenu controller(Inventory inventory) {
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                for (int i = 0; i < IrrigationControllerBlockEntity.ZONES; i++) buffer.writeUtf("North orchard and vegetable beds " + i);
                return new IrrigationControllerMenu(1, inventory, buffer);
            } finally { buffer.release(); }
        }
    }
}
