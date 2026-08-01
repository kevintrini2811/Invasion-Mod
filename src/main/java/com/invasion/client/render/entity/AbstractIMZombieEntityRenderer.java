package com.invasion.client.render.entity;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.LargeBipedEntityModel;
import com.invasion.entity.AbstractIMZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Replicates the rendering code from ZombieBaseEntityRenderer and ZombieEntityRenderer
 * and adds model swapping for the big and normal modes.
 *
 * @see net.minecraft.client.renderer.entity.ZombieRenderer
 * @see net.minecraft.client.renderer.entity.AbstractZombieRenderer
 */
public class AbstractIMZombieEntityRenderer extends HumanoidMobRenderer<AbstractIMZombieEntity, HumanoidModel<AbstractIMZombieEntity>> {
    static final List<ResourceLocation> TEXTURES = Stream.of(
            "textures/entity/zombie/old_zombie_t1.png",
            "textures/entity/zombie/zombie_t1.png",
            "textures/entity/zombie/zombie_t2.png",
            "textures/entity/zombie_pigman/zombie_pigman_t3.png",
            "textures/entity/zombie/zombie_t2a.png",
            "textures/entity/zombie/zombie_tar.png",
            "textures/entity/zombie/zombie_t3.png"
    ).map(InvasionMod::id).toList();

    protected final HumanoidModel<AbstractIMZombieEntity> normalModel;
    protected final LargeBipedEntityModel<AbstractIMZombieEntity> bigModel;

    public AbstractIMZombieEntityRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, ModelLayers.ZOMBIE);
    }

    protected AbstractIMZombieEntityRenderer(
            EntityRendererProvider.Context ctx,
            net.minecraft.client.model.geom.ModelLayerLocation bodyLayer) {
        super(ctx, new ZombieEntityModel(ctx.bakeLayer(bodyLayer)), 0.5F);
        normalModel = model;
        bigModel = new BigZombieEntityModel(LargeBipedEntityModel.getTexturedModelData(CubeDeformation.NONE, 0).bakeRoot());

        boolean zombieVillager = bodyLayer.equals(ModelLayers.ZOMBIE_VILLAGER);
        HumanoidModel<AbstractIMZombieEntity> innerArmor = zombieVillager
                ? new ZombieEntityModel(ctx.bakeLayer(
                        ModelLayers.ZOMBIE_VILLAGER_INNER_ARMOR))
                : new ZombieEntityModel(ctx.bakeLayer(
                        ModelLayers.ZOMBIE_INNER_ARMOR));
        HumanoidModel<AbstractIMZombieEntity> outerArmor = zombieVillager
                ? new ZombieEntityModel(ctx.bakeLayer(
                        ModelLayers.ZOMBIE_VILLAGER_OUTER_ARMOR))
                : new ZombieEntityModel(ctx.bakeLayer(
                        ModelLayers.ZOMBIE_OUTER_ARMOR));
        addLayer(new ArmorFeature(this,
                innerArmor,
                outerArmor,
                ctx.getModelManager(),
                false
        ));
    }

    @Override
    public void render(AbstractIMZombieEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertices, int light) {
        this.model = usesBruteModel(entity) ? bigModel : normalModel;
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    private boolean usesBruteModel(AbstractIMZombieEntity entity) {
        return isBrute(entity) && !entity.isBaby();
    }

    protected boolean isBrute(AbstractIMZombieEntity entity) {
        return entity.getTextureId() == 3 || entity.getTextureId() == 6;
    }

    @Override
    protected void scale(AbstractIMZombieEntity entity, PoseStack matrices, float amount) {
        float scale = entity.scaleAmount();
        matrices.scale(scale, (2 + scale) / 3F, scale);
    }

    protected List<ResourceLocation> getTextures() {
        return TEXTURES;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractIMZombieEntity entity) {
        int id = entity.getTextureId();
        List<ResourceLocation> textures = getTextures();
        return textures.get(id < 0 || id >= textures.size() ? 0 : id);
    }

    private final class ArmorFeature extends HumanoidArmorLayer<AbstractIMZombieEntity, HumanoidModel<AbstractIMZombieEntity>, HumanoidModel<AbstractIMZombieEntity>> {
        private final boolean isBig;

        public ArmorFeature(
                RenderLayerParent<AbstractIMZombieEntity, HumanoidModel<AbstractIMZombieEntity>> context,
                HumanoidModel<AbstractIMZombieEntity> innerModel,
                HumanoidModel<AbstractIMZombieEntity> outerModel,
                ModelManager bakery,
                boolean isBig) {
            super(context, innerModel, outerModel, bakery);
            this.isBig = isBig;
        }

        @Override
        public void render(PoseStack matrices, MultiBufferSource vertices, int light, AbstractIMZombieEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            // Vanilla armor UVs are authored for the regular humanoid mesh.
            // The old enlarged armor mesh stretched every armor texture across
            // the brute torso. Keep the vanilla mesh and let copied brute limb
            // poses place it correctly instead.
            if (!isBig) {
                super.render(matrices, vertices, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
            }
        }
    }

    private static final class ZombieEntityModel extends HumanoidModel<AbstractIMZombieEntity> {
        public ZombieEntityModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AbstractIMZombieEntity hostileEntity, float f, float g, float h, float i, float j) {
            super.setupAnim(hostileEntity, f, g, h, i, j);
            AnimationUtils.animateZombieArms(leftArm, rightArm, isAttacking(hostileEntity), attackTime, h);
        }

        public boolean isAttacking(AbstractIMZombieEntity entity) {
            return entity.isAggressive();
        }
    }

    private static final class BigZombieEntityModel extends LargeBipedEntityModel<AbstractIMZombieEntity> {
        public BigZombieEntityModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AbstractIMZombieEntity hostileEntity, float f, float g, float h, float i, float j) {
            super.setupAnim(hostileEntity, f, g, h, i, j);
            AnimationUtils.animateZombieArms(leftArm, rightArm, isAttacking(hostileEntity), attackTime, h);
        }

        public boolean isAttacking(AbstractIMZombieEntity entity) {
            return entity.isAggressive();
        }
    }
}
