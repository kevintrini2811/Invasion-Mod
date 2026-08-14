package com.invasion.compat;

import fuzs.mutantmonsters.client.renderer.entity.MutantCreeperRenderer;
import fuzs.mutantmonsters.client.renderer.entity.CreeperMinionRenderer;
import fuzs.mutantmonsters.client.renderer.entity.MutantEndermanRenderer;
import fuzs.mutantmonsters.client.renderer.entity.MutantSkeletonRenderer;
import fuzs.mutantmonsters.client.renderer.entity.MutantZombieRenderer;
import fuzs.mutantmonsters.client.renderer.entity.SpiderPigRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class MutantMonstersRenderers {
    private MutantMonstersRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                MutantMonstersEntities.CREEPER_MINION,
                CreeperMinionRenderer::new);
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
