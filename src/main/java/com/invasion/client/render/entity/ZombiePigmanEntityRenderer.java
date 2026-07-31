package com.invasion.client.render.entity;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.invasion.InvasionMod;
import com.invasion.entity.AbstractIMZombieEntity;

public class ZombiePigmanEntityRenderer extends AbstractIMZombieEntityRenderer {
    static final List<ResourceLocation> TEXTURES = Stream.of(
            "textures/entity/zombie_pigman/zombie_pigman.png",
            "textures/entity/zombie_pigman/zombie_pigman.png",
            "textures/entity/zombie_pigman/zombie_pigman_t3.png"
    ).map(InvasionMod::id).toList();

	public ZombiePigmanEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

    @Override
    protected boolean isBrute(AbstractIMZombieEntity entity) {
        return entity.getTier() == 3;
    }


    @Override
    protected List<ResourceLocation> getTextures() {
        return TEXTURES;
    }
}