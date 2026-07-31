package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.network.NexusHudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public final class NexusHud {
    private static final int BLUE = 0xFF5555FF;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final String SEPARATOR = "  ";

    private static NexusHudPayload state = NexusHudPayload.hidden();

    private NexusHud() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(),
                "nexus_status",
                NexusHud::render);
    }

    public static void update(NexusHudPayload payload) {
        state = payload;
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
            GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!state.active()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component wave = Component.literal((state.continuous() ? "Attack " : "Wave ") + state.wave());
        Component mobs = Component.literal(state.defeatedMobs() + "/" + state.totalMobs() + " mobs");
        Component nexus = Component.literal(state.nexusHealthPercent() + "% Nexus");
        int waveWidth = font.width(wave);
        int mobsWidth = font.width(mobs);
        int separatorWidth = font.width(SEPARATOR);
        int totalWidth = waveWidth + separatorWidth + mobsWidth + separatorWidth + font.width(nexus);
        int x = (graphics.guiWidth() - totalWidth) / 2;

        graphics.drawString(font, wave, x, 8, BLUE, true);
        int mobsX = x + waveWidth + separatorWidth;
        graphics.drawString(font, mobs, mobsX, 8, GREEN, true);
        graphics.drawString(font, nexus, mobsX + mobsWidth + separatorWidth, 8, RED, true);
    }
}
