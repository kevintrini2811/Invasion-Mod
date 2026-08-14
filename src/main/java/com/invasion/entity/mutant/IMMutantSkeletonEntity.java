package com.invasion.entity.mutant;

import java.util.Optional;
import com.invasion.entity.SkeletonArrowEntity;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import fuzs.mutantmonsters.common.world.entity.animation.AnimatedEntity;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantSkeleton;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class IMMutantSkeletonEntity extends MutantSkeleton implements IMMutantMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    public IMMutantSkeletonEntity(EntityType<? extends MutantSkeleton> type, Level level) { super(type, level); }
    @Override protected void registerGoals() { super.registerGoals(); addNexusGoals(); }
    @Override public IHasNexus.Handle getNexusHandle() { return nexus; }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); nexus.writeNbt(out); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); nexus.readNbt(in); }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof Combatant<?>) && super.canAttack(target); }
    @Override public boolean removeWhenFarAway(double distance) { return !hasNexus() && super.removeWhenFarAway(distance); }

    @Override
    public double getNexusAttackRangeSqr() {
        return 1024.0D;
    }

    @Override
    public int performNexusAttack(ServerLevel level, NexusAccess nexus) {
        if (isAnimationPlaying()) {
            return 5;
        }
        getLookControl().setLookAt(Vec3.atCenterOf(nexus.getOrigin()));
        AnimatedEntity.sendAnimationPacket(this, SHOOT_ANIMATION);
        return SHOOT_ANIMATION.duration();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        NexusAccess boundNexus = getNexus();
        if (!(level() instanceof ServerLevel) || boundNexus == null
                || getAnimation() != SHOOT_ANIMATION
                || getAnimationTick() != 26) {
            return;
        }

        Vec3 target = Vec3.atCenterOf(boundNexus.getOrigin());
        SkeletonArrowEntity arrow = new SkeletonArrowEntity(level(), this,
                getMainHandItem());
        double dX = target.x - getX();
        double dY = target.y - arrow.getY();
        double dZ = target.z - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        arrow.shoot(dX, dY + horizontalDistance * 0.2D, dZ, 2.4F, 1.0F);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }
}
