package com.invasion.entity;

import com.invasion.entity.ai.ClimbableMoveControl;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.entity.pathfinding.IMNavigation;
import com.invasion.entity.pathfinding.Navigation;
import com.invasion.entity.pathfinding.PathCreator;
import com.invasion.nexus.IHasNexus;
import com.invasion.particle.InvParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Deprecated
public abstract class EntityIMLiving extends Monster implements NexusEntity, Stunnable {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    private int stunTimer;

    protected int flammability = 2;

    public EntityIMLiving(EntityType<? extends EntityIMLiving> type, Level world) {
        super(type, world);
        moveControl = new ClimbableMoveControl(this);
        resetHealth();
    }

    @Override
    public PathfinderMob asEntity() {
        return this;
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new IMMobNavigation(this, createIMNavigation().getActor());
    }

    @Deprecated
    protected Navigation createIMNavigation() {
        return new IMNavigation(this, new PathCreator(700, 50));
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public void aiStep() {
        if (isStunned()) {
            if (!level().isClientSide() && tickCount % 10 == 0) {
                ((ServerLevel)level()).sendParticles(InvParticles.DAZE, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
            }
            stunTimer--;
        }
        super.aiStep();
        if (getBurnsInDay()
                && !isInWaterOrRain()
                && level().isBrightOutside()
                && level().canSeeSky(blockPosition())) {
            sunlightDamageTick();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            damage *= flammability;
        }

        return super.hurtServer(serverLevel, source, damage);
    }

    @Override
    public boolean stun(int ticks) {
        stunTimer = Math.max(stunTimer, ticks);
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        return true;
    }

    @Override
    public boolean isStunned() {
        return stunTimer > 0;
    }

    @Override
    public boolean hasLineOfSight(Entity entity) {
        float distance = distanceTo(entity);
        return distance <= getSenseRange() || (super.hasLineOfSight(entity) && distance <= getAggroRange());
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        return super.checkSpawnObstruction(world) && (hasNexus() || getLightLevelBelow8()) && level().loadedAndEntityCanStandOn(blockPosition().below(), this);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return hasNexus() ? 0 : super.getWalkTargetValue(pos, world);
    }

    @Override
    public final boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus();
    }

    @Override
    public final boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    protected void sunlightDamageTick() {
        igniteForSeconds(8);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("stunTimer", stunTimer);
        nexus.writeNbt(compound);
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        stunTimer = compound.getIntOr("stunTimer", 0);
        nexus.readNbt(compound);
    }
}
