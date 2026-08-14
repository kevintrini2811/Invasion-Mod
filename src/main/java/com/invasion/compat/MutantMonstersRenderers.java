package com.invasion.compat;

import fuzs.mutantmonsters.common.client.renderer.entity.MutantCreeperRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.CreeperMinionRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantEndermanRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantSkeletonRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantZombieRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.SpiderPigRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class MutantMonstersRenderers {
    private MutantMonstersRenderers() {
    }

    public static void bootstrap() {
        EntityRendererRegistry.register(
                MutantMonstersEntities.CREEPER_MINION,
                CreeperMinionRenderer::new);
        EntityRendererRegistry.register(
                MutantMonstersEntities.MUTANT_ZOMBIE,
                MutantZombieRenderer::new);
        EntityRendererRegistry.register(
                MutantMonstersEntities.MUTANT_CREEPER,
                MutantCreeperRenderer::new);
        EntityRendererRegistry.register(
                MutantMonstersEntities.MUTANT_SKELETON,
                MutantSkeletonRenderer::new);
        EntityRendererRegistry.register(
                MutantMonstersEntities.MUTANT_ENDERMAN,
                MutantEndermanRenderer::new);
        EntityRendererRegistry.register(
                MutantMonstersEntities.SPIDER_PIG,
                SpiderPigRenderer::new);
    }
}
