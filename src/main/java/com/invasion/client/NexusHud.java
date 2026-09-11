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
	private static final int PINK = 0xFFFF55FF;
    private static final String SEPARATOR = "  ";

    private static NexusHudPayload state = NexusHudPayload.hidden();
    private static CachedHud cachedHud;

    private NexusHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.BOSS_OVERLAY,
                InvasionMod.id("nexus_status"),
                NexusHud::render);
    }

    public static void update(NexusHudPayload payload) {
        if (!payload.equals(state)) {
            cachedHud = null;
        }
        state = payload;
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (!state.active()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        if (cachedHud == null || cachedHud.font() != font) {
            cachedHud = CachedHud.create(state, font);
        }
        CachedHud hud = cachedHud;
        int x = (graphics.guiWidth() - hud.totalWidth()) / 2;

        graphics.text(font, hud.wave(), x, 8, BLUE, true);
		int mobsX = x + hud.waveWidth() + hud.separatorWidth();
		if (hud.showPhase()) {
			graphics.text(font, hud.phase(), mobsX, 8, PINK, true);
			mobsX += hud.phaseWidth() + hud.separatorWidth();
		}
        graphics.text(font, hud.mobs(), mobsX, 8, GREEN, true);
        graphics.text(font, hud.nexus(), mobsX + hud.mobsWidth() + hud.separatorWidth(), 8, RED, true);
    }

    private record CachedHud(Font font, Component wave, Component phase,
            Component mobs, Component nexus, boolean showPhase, int waveWidth,
            int phaseWidth, int mobsWidth, int separatorWidth, int totalWidth) {
        private static CachedHud create(NexusHudPayload state, Font font) {
            Component wave = Component.literal(
                    (state.continuous() ? "Attack " : "Wave ") + state.wave());
            boolean showPhase = state.phaseCount() > 1;
            Component phase = showPhase
                    ? Component.literal("Phase " + state.phase() + "/" + state.phaseCount())
                    : Component.empty();
            Component mobs = Component.literal(
                    state.defeatedMobs() + "/" + state.totalMobs() + " mobs");
            Component nexus = Component.literal(state.nexusHealthPercent() + "% Nexus");
            int waveWidth = font.width(wave);
            int phaseWidth = showPhase ? font.width(phase) : 0;
            int mobsWidth = font.width(mobs);
            int separatorWidth = font.width(SEPARATOR);
            int totalWidth = waveWidth + (showPhase ? separatorWidth + phaseWidth : 0)
                    + separatorWidth + mobsWidth + separatorWidth + font.width(nexus);
            return new CachedHud(font, wave, phase, mobs, nexus, showPhase,
                    waveWidth, phaseWidth, mobsWidth, separatorWidth, totalWidth);
        }
    }
}
