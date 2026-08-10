package com.invasion.client.render;

import com.invasion.client.render.entity.VultureEntityRenderer;
import com.invasion.client.render.entity.ElectricityBoltEntityRenderer;
import com.invasion.client.render.entity.BoulderEntityRenderer;
import com.invasion.client.render.entity.BurrowerEntityRenderer;
import com.invasion.client.render.entity.SpiderEggEntityRenderer;
import com.invasion.client.render.entity.RenderGiantBird;
import com.invasion.client.render.entity.IMCreeperEntityRenderer;
import com.invasion.client.render.entity.IMSkeletonEntityRenderer;
import com.invasion.client.render.entity.IMSpiderEntityRenderer;
import com.invasion.client.render.entity.IMWolfEntityRenderer;
import com.invasion.client.particle.DazeParticle;
import com.invasion.client.render.entity.AbstractIMZombieEntityRenderer;
import com.invasion.client.render.entity.ZombiePigmanEntityRenderer;
import com.invasion.client.render.entity.ImpEntityRenderer;
import com.invasion.client.render.entity.PigmanEngineerEntityRenderer;
import com.invasion.client.render.entity.ZombieBuilderEntityRenderer;
import com.invasion.client.render.entity.ThrowerEntityRenderer;
import com.invasion.client.render.entity.TntEntityRenderer;
import com.invasion.client.render.entity.TrapEntityRenderer;
import com.invasion.entity.InvEntities;
import com.invasion.item.InvItems;
import com.invasion.particle.InvParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.SpiderEntityRenderer;
import net.minecraft.util.Identifier;

public interface InvRenderers {
    static void bootstrap() {
        ParticleFactoryRegistry.getInstance().register(InvParticles.DAZE, DazeParticle::factory);

        EntityRendererRegistry.register(InvEntities.ZOMBIE, AbstractIMZombieEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.ZOMBIE_PIGMAN, ZombiePigmanEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.SKELETON, IMSkeletonEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.SPIDER, SpiderEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.JUMPING_SPIDER, context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.JUMPER));
        EntityRendererRegistry.register(InvEntities.QUEEN_SPIDER, context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.MOTHER));
        EntityRendererRegistry.register(InvEntities.PIGMAN_ENGINEER, PigmanEngineerEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.ZOMBIE_BUILDER, ZombieBuilderEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.IMP, ImpEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.THROWER, ThrowerEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.BURROWER, BurrowerEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.BOULDER, BoulderEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.TNT, TntEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.WOLF, IMWolfEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.TRAP, TrapEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.BOLT, ElectricityBoltEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.SFX, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.SPAWN_PROXY, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.SPIDER_EGG, SpiderEggEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.CREEPER, IMCreeperEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.BIRD, VultureEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.VULTURE, RenderGiantBird::new);

        ModelPredicateProviderRegistry.register(InvItems.SEARING_BOW, Identifier.ofVanilla("pull"), (stack, world, entity, seed) -> {
            return entity == null || entity.getActiveItem() != stack ? 0.0F : (stack.getMaxUseTime(entity) - entity.getItemUseTimeLeft()) / 20F;
        });
        ModelPredicateProviderRegistry.register(InvItems.SEARING_BOW, Identifier.ofVanilla("pulling"),
            (stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1 : 0
        );
    }
}
