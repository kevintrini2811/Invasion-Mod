package com.invasion.compat;

import java.util.EnumSet;
import org.jetbrains.annotations.Nullable;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** IM Blaze Nexus flight and fireball logic adapted for Wildfire. */
public final class WildfireNexusGoal extends Goal {
    private final Monster mob;
    private final Combatant<?> combatant;
    private int cooldown;
    @Nullable private Vec3 crossing;

    public WildfireNexusGoal(Monster mob, Combatant<?> combatant) {
        this.mob = mob;
        this.combatant = combatant;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        NexusAccess nexus = combatant.getNexus();
        return nexus != null && nexus.isActive()
                && mob.getTarget() == null;
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void stop() { crossing = null; }

    @Override public void tick() {
        NexusAccess nexus = combatant.getNexus();
        if (nexus == null) return;
        Vec3 target = Vec3.atCenterOf(nexus.getOrigin());
        mob.getLookControl().setLookAt(target.x, target.y, target.z);
        if (mob.distanceToSqr(target) > 64.0D) fly(target, nexus.getOrigin());
        if (cooldown-- > 0) return;
        Vec3 shot = shotTarget(target, nexus.getOrigin());
        if (shot == null || mob.distanceToSqr(shot) > 1024.0D) return;
        Vec3 direction = shot.subtract(
                mob.getX(), mob.getY(0.5D), mob.getZ()).normalize();
        SmallFireball fireball = new SmallFireball(
                mob.level(), mob, direction.x, direction.y, direction.z);
        fireball.setPos(mob.getX(), mob.getY(0.5D) + 0.5D, mob.getZ());
        mob.level().addFreshEntity(fireball);
        mob.level().levelEvent(null, 1018, mob.blockPosition(), 0);
        cooldown = 60;
    }

    private void fly(Vec3 target, BlockPos nexusPos) {
        Vec3 wanted = flightTarget(target, nexusPos);
        mob.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, 1.0D);
        Vec3 velocity = mob.getDeltaMovement().add(
                wanted.subtract(mob.position()).normalize().scale(0.025D));
        if (wanted.y > mob.getY() + 1.0D) velocity = new Vec3(velocity.x,
                velocity.y + (0.3D - velocity.y) * 0.3D, velocity.z);
        double speed = velocity.horizontalDistance();
        if (speed > 0.35D) velocity = new Vec3(
                velocity.x * 0.35D / speed, velocity.y,
                velocity.z * 0.35D / speed);
        mob.setDeltaMovement(velocity);
        mob.move(MoverType.SELF, velocity);
    }

    private Vec3 flightTarget(Vec3 target, BlockPos nexusPos) {
        if (crossing != null) {
            if (mob.distanceToSqr(crossing.x, mob.getY(), crossing.z) > 2.25D
                    || mob.getY() < crossing.y - 1.5D) return crossing;
            crossing = null;
        }
        BlockHitResult hit = clip(new Vec3(target.x, mob.getEyeY(), target.z));
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getBlockPos().equals(nexusPos)) return target.add(0, 2, 0);
        BlockPos wall = hit.getBlockPos();
        for (int y = Math.max(wall.getY() + 1, mob.blockPosition().getY());
                y < mob.level().getMaxBuildHeight() - 1; y++) {
            BlockPos lower = new BlockPos(wall.getX(), y, wall.getZ());
            if (mob.level().getBlockState(lower).getCollisionShape(
                            mob.level(), lower).isEmpty()
                    && mob.level().getBlockState(lower.above()).getCollisionShape(
                            mob.level(), lower.above()).isEmpty()) {
                Vec3 across = target.subtract(Vec3.atCenterOf(wall))
                        .multiply(1, 0, 1);
                if (across.lengthSqr() > 0) across = across.normalize().scale(4);
                return crossing = new Vec3(wall.getX() + .5 + across.x,
                        y + 1, wall.getZ() + .5 + across.z);
            }
        }
        return target.add(0, 2, 0);
    }

    @Nullable private Vec3 shotTarget(Vec3 target, BlockPos nexusPos) {
        BlockHitResult hit = clip(target);
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getBlockPos().equals(nexusPos)) return target;
        return mob.level().getBlockState(hit.getBlockPos()).ignitedByLava()
                ? Vec3.atCenterOf(hit.getBlockPos()) : null;
    }

    private BlockHitResult clip(Vec3 target) {
        return mob.level().clip(new ClipContext(mob.getEyePosition(), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
    }
}
