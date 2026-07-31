package com.invasion.compat.jei;

import java.util.List;

import com.invasion.InvasionMod;
import com.invasion.item.InvItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class InvasionJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = InvasionMod.id("jei_plugin");
    private static final RecipeType<RiftFluxGeneration> RIFT_FLUX_GENERATION =
            RecipeType.create("invmod", "rift_flux_generation", RiftFluxGeneration.class);
    private static final RiftFluxGeneration RIFT_FLUX_RECIPE = new RiftFluxGeneration();

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RiftFluxCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RIFT_FLUX_GENERATION, List.of(RIFT_FLUX_RECIPE));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(InvItems.NEXUS_CORE, RIFT_FLUX_GENERATION);
    }

    private static final class RiftFluxGeneration {
    }

    private static final class RiftFluxCategory implements IRecipeCategory<RiftFluxGeneration> {
        private final IDrawable icon;
        private final IDrawableStatic arrow;

        private RiftFluxCategory(IGuiHelper guiHelper) {
            icon = guiHelper.createDrawableItemLike(InvItems.NEXUS_CORE);
            arrow = guiHelper.getRecipeArrow();
        }

        @Override
        public RecipeType<RiftFluxGeneration> getRecipeType() {
            return RIFT_FLUX_GENERATION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("invmod.jei.rift_flux_generation");
        }

        @Override
        public int getWidth() {
            return 82;
        }

        @Override
        public int getHeight() {
            return 36;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RiftFluxGeneration recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 10)
                    .setStandardSlotBackground()
                    .addItemLike(InvItems.NEXUS_CORE);
            builder.addOutputSlot(61, 10)
                    .setOutputSlotBackground()
                    .addItemLike(InvItems.RIFT_FLUX);
        }

        @Override
        public void draw(RiftFluxGeneration recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                double mouseX, double mouseY) {
            arrow.draw(graphics, 27, 10);
        }
    }
}
