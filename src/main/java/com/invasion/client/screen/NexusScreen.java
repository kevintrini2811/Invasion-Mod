package com.invasion.client.screen;

import com.invasion.InvasionMod;
import com.invasion.block.container.NexusScreenHandler;
import com.invasion.nexus.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;

public class NexusScreen extends AbstractContainerScreen<NexusScreenHandler> {
    private static final Identifier BACKGROUND = InvasionMod.id("textures/gui/nexus.png");

    public NexusScreen(NexusScreenHandler container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        context.text(font, "Nexus - Level " + menu.getLevel(), 46, 6, 0x404040, false);
        context.text(font, menu.getKills() + " mobs killed", 96, 60, 0x404040, false);
        context.text(font, "R: " + menu.getSpawnRadius(), 142, 72, 0x404040, false);

        if (menu.getMode() == Mode.STARTED || menu.getMode() == Mode.WAITING) {
            context.text(font, "Activated!", 13, 62, 4210752, false);
            context.text(font, "Wave " + menu.getCurrentWave(), 55, 37, 0x404040, false);
        } else if (menu.getMode() == Mode.CONTINUOUS) {
            context.text(font, "Power:", 56, 31, 4210752, false);
            context.text(font, "" + menu.getPowerLevel(), 61, 44, 0x404040, false);
        }

        if (menu.isActivating() && menu.getMode() == Mode.STOPPED) {
            context.text(font, "Activating...", 13, 62, 0x404040, false);
            if (menu.getMode() != Mode.STABLE) {
                context.text(font, "Are you sure?", 8, 72, 0x404040, false);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int j = (width - imageWidth) / 2;
        int k = (height - imageHeight) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, j, k,
                0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int progress = menu.getGenerationProgressScaled(26);
        context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                j + 126, k + 28 + 26 - progress,
                185.0F, 26.0F - progress, 9, progress, 256, 256);

        progress = menu.getCookProgressScaled(18);
        context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                j + 31, k + 51,
                204.0F, 0.0F, progress, 2, 256, 256);

        if (menu.getMode() == Mode.STARTED || menu.getMode() == Mode.WAITING) {
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                    j + 19, k + 29,
                    176.0F, 0.0F, 9, 31, 256, 256);
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                    j + 19, k + 19,
                    194.0F, 0.0F, 9, 9, 256, 256);
        } else if (menu.getMode() == Mode.CONTINUOUS) {
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                    j + 19, k + 29,
                    176.0F, 31.0F, 9, 31, 256, 256);
        }

        if ((menu.getMode() == Mode.STOPPED || menu.getMode() == Mode.CONTINUOUS)
                && menu.isActivating()) {
            progress = menu.getActivationProgressScaled(31);
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                    j + 19, k + 29 + 31 - progress,
                    176.0F, 31.0F - progress, 9, progress, 256, 256);
        } else if (menu.getMode() == Mode.STABLE && menu.isActivating()) {
            progress = menu.getActivationProgressScaled(31);
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                    j + 19, k + 29 + 31 - progress,
                    176.0F, 62.0F - progress, 9, progress, 256, 256);
        }
    }
}
