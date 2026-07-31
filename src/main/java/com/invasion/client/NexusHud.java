package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.network.NexusHudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class NexusHud {
    private static final int BLUE = 0xFF5555FF;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final String SEPARATOR = "  ";

    private static NexusHudPayload state = NexusHudPayload.hidden();

    private NexusHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.BOSS_OVERLAY,
                InvasionMod.id("nexus_status"),
                NexusHud::render);
    }

    public static void update(NexusHudPayload payload) {
        state = payload;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
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

        graphics.text(font, wave, x, 8, BLUE, true);
        int mobsX = x + waveWidth + separatorWidth;
        graphics.text(font, mobs, mobsX, 8, GREEN, true);
        graphics.text(font, nexus, mobsX + mobsWidth + separatorWidth, 8, RED, true);
    }
}
