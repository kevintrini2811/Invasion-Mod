package com.invasion.compat;

import fuzs.mutantmonsters.common.client.renderer.entity.MutantCreeperRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantEndermanRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantSkeletonRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.MutantZombieRenderer;
import fuzs.mutantmonsters.common.client.renderer.entity.SpiderPigRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class MutantMonstersRenderers {
    private MutantMonstersRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                MutantMonstersEntities.MUTANT_ZOMBIE,
                MutantZombieRenderer::new);
        event.registerEntityRenderer(
                MutantMonstersEntities.MUTANT_CREEPER,
                MutantCreeperRenderer::new);
        event.registerEntityRenderer(
                MutantMonstersEntities.MUTANT_SKELETON,
                MutantSkeletonRenderer::new);
        event.registerEntityRenderer(
                MutantMonstersEntities.MUTANT_ENDERMAN,
                MutantEndermanRenderer::new);
        event.registerEntityRenderer(
                MutantMonstersEntities.SPIDER_PIG,
                SpiderPigRenderer::new);
    }
}
