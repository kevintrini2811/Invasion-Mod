package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.KillEntityGoal;
import com.invasion.entity.ai.goal.NoNexusPathGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ImpEnitty extends IMMobEntity {
    public ImpEnitty(EntityType<ImpEnitty> type, Level world) {
        super(type, world);
        getNavigatorNew().getActor().setCanClimb(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new KillEntityGoal<>(this, Player.class, 40));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new ProvideSupportGoal(this, 4, true));
        goalSelector.addGoal(4, new KillEntityGoal<>(this, Mob.class, 40));
        goalSelector.addGoal(5, new GoToNexusGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(5, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NoNexusPathGoal(this, new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F)));
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity entity) {
        if (super.doHurtTarget(serverLevel, entity)) {
            entity.igniteForSeconds(3);
            return true;
        }
        return false;
    }
}
