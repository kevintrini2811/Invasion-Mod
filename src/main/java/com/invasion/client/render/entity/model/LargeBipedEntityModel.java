package com.invasion.client.render.entity.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Enlarged version of a normal biped.
 * Has a slightly bigger head and torso and wider-set limbs.
 *
 * @param <T> The entity type
 */
public class LargeBipedEntityModel<T extends LivingEntity> extends HumanoidModel<T> {
  public LargeBipedEntityModel(ModelPart root) {
    super(root);
  }

  public static LayerDefinition getTexturedModelData(CubeDeformation dilation, float pivotOffsetY) {
      return LayerDefinition.create(createMesh(dilation, pivotOffsetY), 64, 64);
  }

  public static MeshDefinition createMesh(CubeDeformation dilation, float pivotOffsetY) {
      MeshDefinition data = new MeshDefinition();
      PartDefinition root = data.getRoot();
      root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7, -3.5F, 7, 7, 7, dilation), PartPose.offset(0, 0 + pivotOffsetY, 0));
      root.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -7, -3.5F, 7, 7, 7, dilation.extend(0.5F)), PartPose.offset(0, 0 + pivotOffsetY, 0));

      root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create().texOffs(16, 15).addBox(-5, 0, -3, 10, 12, 5, dilation), PartPose.offset(0, 0 + pivotOffsetY, 0));

      root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create().texOffs(46, 15).addBox(-3, -2, -2, 4, 12, 4, dilation), PartPose.offset(-6, 2 + pivotOffsetY, 0));
      root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create().texOffs(46, 15).mirror().addBox(-1, -2, -2, 4, 12, 4, dilation), PartPose.offset(6, 2 + pivotOffsetY, 0));

      root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, dilation), PartPose.offset(-2F, 12 + pivotOffsetY, 0));
      root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4, dilation), PartPose.offset(2F, 12 + pivotOffsetY, 0));
      return data;
  }
}