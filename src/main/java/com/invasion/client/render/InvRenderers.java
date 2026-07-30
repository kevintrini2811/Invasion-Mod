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
import com.invasion.client.render.entity.ThrowerEntityRenderer;
import com.invasion.client.render.entity.TntEntityRenderer;
import com.invasion.client.render.entity.TrapEntityRenderer;
import com.invasion.entity.InvEntities;
import com.invasion.item.InvItems;
import com.invasion.particle.InvParticles;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public interface InvRenderers {
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(InvParticles.DAZE, DazeParticle::factory);
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InvEntities.ZOMBIE, AbstractIMZombieEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_PIGMAN, ZombiePigmanEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SKELETON, IMSkeletonEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SPIDER, SpiderRenderer::new);
        event.registerEntityRenderer(InvEntities.JUMPING_SPIDER, context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.JUMPER));
        event.registerEntityRenderer(InvEntities.QUEEN_SPIDER, context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.MOTHER));
        event.registerEntityRenderer(InvEntities.PIGMAN_ENGINEER, PigmanEngineerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.IMP, ImpEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.THROWER, ThrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER, BurrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BOULDER, BoulderEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.TNT, TntEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.WOLF, IMWolfEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.TRAP, TrapEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BOLT, ElectricityBoltEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SFX, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPAWN_PROXY, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPIDER_EGG, SpiderEggEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.CREEPER, IMCreeperEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BIRD, VultureEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.VULTURE, RenderGiantBird::new);

        ItemProperties.register(InvItems.SEARING_BOW, ResourceLocation.withDefaultNamespace("pull"), (stack, world, entity, seed) -> {
            return entity == null || entity.getUseItem() != stack ? 0.0F : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20F;
        });
        ItemProperties.register(InvItems.SEARING_BOW, ResourceLocation.withDefaultNamespace("pulling"),
            (stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0
        );
    }
}
