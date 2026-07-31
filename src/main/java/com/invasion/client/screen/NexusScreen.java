package com.invasion.client.screen;

import com.invasion.InvasionMod;
import com.invasion.block.container.NexusScreenHandler;
import com.invasion.nexus.Mode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NexusScreen extends AbstractContainerScreen<NexusScreenHandler> {
    private static final ResourceLocation BACKGROUND = InvasionMod.id("textures/gui/nexus.png");

    public NexusScreen(NexusScreenHandler container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        context.drawString(font, "Nexus - Level " + menu.getLevel(), 46, 6, 0x404040, false);
        context.drawString(font, menu.getKills() + " mobs killed", 96, 60, 0x404040, false);
        context.drawString(font, "R: " + menu.getSpawnRadius(), 142, 72, 0x404040, false);
        if (menu.getMode() == Mode.STARTED || menu.getMode() == Mode.WAITING) {
            context.drawString(font, "Activated!", 13, 62, 4210752, false);
            context.drawString(font, "Wave " + menu.getCurrentWave(), 55, 37, 0x404040, false);
        } else if (menu.getMode() == Mode.CONTINUOUS) {
            context.drawString(font, "Power:", 56, 31, 4210752, false);
            context.drawString(font, Integer.toString(menu.getPowerLevel()), 61, 44, 0x404040, false);
        }
        if (menu.isActivating() && menu.getMode() == Mode.STOPPED) {
            context.drawString(font, "Activating...", 13, 62, 0x404040, false);
            context.drawString(font, "Are you sure?", 8, 72, 0x404040, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        context.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);
        int progress = menu.getGenerationProgressScaled(26);
        context.blit(BACKGROUND, x + 126, y + 54 - progress, 185, 26 - progress, 9, progress);
        context.blit(BACKGROUND, x + 31, y + 51, 204, 0, menu.getCookProgressScaled(18), 2);
        if (menu.getMode() == Mode.STARTED || menu.getMode() == Mode.WAITING) {
            context.blit(BACKGROUND, x + 19, y + 29, 176, 0, 9, 31);
            context.blit(BACKGROUND, x + 19, y + 19, 194, 0, 9, 9);
        } else if (menu.getMode() == Mode.CONTINUOUS) {
            context.blit(BACKGROUND, x + 19, y + 29, 176, 31, 9, 31);
        }
        if ((menu.getMode() == Mode.STOPPED || menu.getMode() == Mode.CONTINUOUS) && menu.isActivating()) {
            progress = menu.getActivationProgressScaled(31);
            context.blit(BACKGROUND, x + 19, y + 60 - progress, 176, 31 - progress, 9, progress);
        } else if (menu.getMode() == Mode.STABLE && menu.isActivating()) {
            progress = menu.getActivationProgressScaled(31);
            context.blit(BACKGROUND, x + 19, y + 60 - progress, 176, 62 - progress, 9, progress);
        }
    }
}
