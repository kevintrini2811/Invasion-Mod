package com.invasion.entity;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.TerrainDigger;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.BurrowerNavigation;
import com.invasion.entity.pathfinding.Navigation;
import com.invasion.entity.pathfinding.PathNavigateAdapter;
import com.invasion.entity.pathfinding.PathCreator;
import com.invasion.util.math.PosRotate3D;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class BurrowerEntity extends IMMobEntity implements Miner {
    public static final int NUMBER_OF_SEGMENTS = 16;

    private static final EntityDataAccessor<Vector3fc> HEAD_ROTATION =
            SynchedEntityData.defineId(BurrowerEntity.class, EntityDataSerializers.VECTOR3);
    private TerrainModifier terrainModifier = new TerrainModifier(this, 2);
    private TerrainDigger terrainDigger = new TerrainDigger(this, terrainModifier, 1);

    private final PosRotate3D[] segments3D = new PosRotate3D[NUMBER_OF_SEGMENTS];
    private final PosRotate3D[] segments3DLastTick = new PosRotate3D[NUMBER_OF_SEGMENTS];
    private final Deque<Vec3> clientMovementHistory = new ArrayDeque<>();
    private BurrowerTailEntity tailHitbox;

    protected final Vector3f rot = new Vector3f();
    protected final Vector3f prevRot = new Vector3f();

    public BurrowerEntity(EntityType<BurrowerEntity> type, Level world) {
        super(type, world);
        Arrays.fill(segments3D, PosRotate3D.ZERO);
        Arrays.fill(segments3DLastTick, PosRotate3D.ZERO);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEAD_ROTATION, new Vector3f());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.GRAVITY, 0)
                .add(Attributes.STEP_HEIGHT, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AttackNexusGoal<>(this));
        goalSelector.addGoal(2, new GoToNexusGoal(this));
        goalSelector.addGoal(3, new MobMeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1,
                new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2,
                new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        Navigation burrowerNavigation = createIMNavigation();
        return new PathNavigateAdapter(this, world, burrowerNavigation);
    }

    @Override
    protected Navigation createIMNavigation() {
        return new BurrowerNavigation(this, new PathCreator(800, 400), 16, -4);
    }

    @Override
    public boolean onPathBlocked(Path path, Notifiable notifee) {
        return tryClearPosition(path.getNextNodePos(), notifee);
    }

    public boolean tryClearPosition(BlockPos pos, Notifiable notifee) {
        return terrainDigger.askClearPosition(pos, notifee, 1);
    }

    @Override
    public BlockPos[] getBlockRemovalOrder(BlockPos pos) {
        return new BlockPos[] { pos };
    }

    public Vector3f getRotation() {
        return rot;
    }

    public Vector3f getPrevRotation() {
        return prevRot;
    }

    public PosRotate3D[] getSegments3D() {
        return segments3D;
    }

    public PosRotate3D[] getSegments3DLastTick() {
        return this.segments3DLastTick;
    }

    public void setSegment(int index, PosRotate3D pos) {
        if (index < segments3D.length) {
            segments3DLastTick[index] = segments3D[index];
            segments3D[index] = pos;
            if (!level().isClientSide() && index == NUMBER_OF_SEGMENTS - 1) {
                updateTailHitbox(pos.position());
            }
        }
    }

    public void setHeadRotation(PosRotate3D pos) {
        prevRot.set(rot);
        rot.set(pos.rotation());
        entityData.set(HEAD_ROTATION, new Vector3f(pos.rotation()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (!level().isClientSide()) {
            return;
        }

        if (HEAD_ROTATION.equals(data)) {
            prevRot.set(rot);
            rot.set(entityData.get(HEAD_ROTATION));
            return;
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            updateClientSegments();
        }
    }

    private void updateClientSegments() {
        clientMovementHistory.addLast(position());
        while (clientMovementHistory.size() > 256) {
            clientMovementHistory.removeFirst();
        }
        Vec3[] history = clientMovementHistory.toArray(Vec3[]::new);
        Vec3[] sampledPoints = new Vec3[NUMBER_OF_SEGMENTS + 1];
        for (int i = 0; i < sampledPoints.length; i++) {
            sampledPoints[i] = sampleClientHistory(history, (i + 1) * 0.20D);
        }

        for (int i = 0; i < NUMBER_OF_SEGMENTS; i++) {
            segments3DLastTick[i] = segments3D[i];
            Vec3 pointAhead = i == 0 ? position() : sampledPoints[i - 1];
            Vec3 direction = pointAhead.subtract(sampledPoints[i + 1]);
            double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            float oldYaw = segments3D[i].rotation().y();
            float targetYaw = horizontal > 1.0E-4D
                    ? (float) -Math.atan2(direction.z, direction.x)
                    : oldYaw;
            float oldPitch = segments3D[i].rotation().z();
            float targetPitch = (float) Math.atan2(direction.y, horizontal);
            Vector3f rotation = new Vector3f(
                    0.0F,
                    Mth.rotLerpRad(0.35F, oldYaw, targetYaw),
                    Mth.rotLerpRad(0.35F, oldPitch, targetPitch));
            segments3D[i] = new PosRotate3D(sampledPoints[i], rotation);
        }
    }

    private Vec3 sampleClientHistory(Vec3[] history, double targetDistance) {
        double distance = 0.0D;
        for (int i = history.length - 1; i > 0; i--) {
            double step = history[i].distanceTo(history[i - 1]);
            if (step < 1.0E-6D) {
                continue;
            }
            if (distance + step >= targetDistance) {
                return history[i].lerp(history[i - 1],
                        (targetDistance - distance) / step);
            }
            distance += step;
        }
        return history.length == 0 ? position() : history[0];
    }

    private void updateTailHitbox(Vec3 position) {
        if (tailHitbox == null || tailHitbox.isRemoved()) {
            tailHitbox = new BurrowerTailEntity(InvEntities.BURROWER_TAIL, level());
            tailHitbox.setParent(this);
            tailHitbox.setPos(position);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.addFreshEntity(tailHitbox);
            }
        } else {
            tailHitbox.setPos(position);
        }
    }

    @Override
    public void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        terrainModifier.onUpdate();
    }

    @Override
    public String getLegacyName() {
        return "EntityIMBurrower#u-u-u";
    }
}
