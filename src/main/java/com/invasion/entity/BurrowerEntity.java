package com.invasion.entity;

import java.util.Arrays;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

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


public class BurrowerEntity extends IMMobEntity implements Miner {
    public static final int NUMBER_OF_SEGMENTS = 16;
    private TerrainModifier terrainModifier = new TerrainModifier(this, 2);
    private TerrainDigger terrainDigger = new TerrainDigger(this, terrainModifier, 1);

    private final PosRotate3D[] segments3D = new PosRotate3D[NUMBER_OF_SEGMENTS];
    private final PosRotate3D[] segments3DLastTick = new PosRotate3D[NUMBER_OF_SEGMENTS];

    protected final Vector3f rot = new Vector3f();
    protected final Vector3f prevRot = new Vector3f();

    public BurrowerEntity(EntityType<BurrowerEntity> type, Level world) {
        super(type, world);
        Arrays.fill(segments3D, PosRotate3D.ZERO);
        Arrays.fill(segments3DLastTick, PosRotate3D.ZERO);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FOLLOW_RANGE, 32)
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
        return terrainDigger.askClearPosition(path.getNextNodePos(), notifee, 1);
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
        }
    }

    public void setHeadRotation(PosRotate3D pos) {
        prevRot.set(rot);
        rot.set(pos.rotation());
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
