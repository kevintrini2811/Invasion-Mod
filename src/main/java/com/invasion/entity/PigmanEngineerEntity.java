package com.invasion.entity;

import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.BuilderIMMobNavigation;
import com.invasion.entity.pathfinding.PathingUtil;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.item.InvItems;
import com.invasion.nexus.Nexus;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import com.invasion.nexus.NexusAccess;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

public class PigmanEngineerEntity extends IMMobEntity implements Miner {
    private final TerrainModifier terrainModifier = new TerrainModifier(this, 4.5F);
    private final TerrainBuilder terrainBuilder = new TerrainBuilder(this, 1);

    private float supportThisTick;
    @org.jetbrains.annotations.Nullable
    private BlockPos currentBuildTarget;

    private void tryEmergencyTowerBuild() {
        if (terrainModifier.isBusy()) {
            return;
        }

        NexusAccess nexus = getNexus();
        if (nexus == null) {
            return;
        }

        World world = getWorld();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.getBlockPos();

        // Vertikaler Abstand zum Nexus
        int dy = nexusPos.getY() - mobPos.getY();
        if (Math.abs(dy) <= 2) {
            // fast gleiche Höhe -> nichts bauen
            return;
        }

        // Horizontaler Abstand (2D) zum Nexus
        double dx = (nexusPos.getX() + 0.5D) - this.getX();
        double dz = (nexusPos.getZ() + 0.5D) - this.getZ();
        double horizDistSq = dx * dx + dz * dz;

        if (horizDistSq > 4.0D * 4.0D) {
            // Zu weit weg -> erst näher laufen
            return;
        }

        // Richtung zum Nexus (nur horizontal)
        Direction orientation = Direction.getFacing(dx, 0.0D, dz);
        if (!orientation.getAxis().isHorizontal()) {
            orientation = this.getHorizontalFacing();
        }

        // Basis-Position: Block auf dem wir stehen / vor uns
        BlockPos basePos = mobPos;
        BlockState baseState = world.getBlockState(basePos);
        if (!PathingUtil.isAirOrReplaceable(baseState)) {
            basePos = basePos.offset(orientation);
        }

        // Nexus über uns -> Turm nach oben
        if (dy > 0) {
            BlockPos above = basePos.up();
            if (world.getBlockState(above).isAir()) {
                // Über uns ist Luft -> noch nicht direkt unter der Decke
                return;
            }

            int layers = Math.min(32, dy + 1);
            final Direction towerDir = orientation;
            final int towerLayers = Math.max(4, layers);

            this.currentBuildTarget = basePos;

            terrainModifier.submitJob(basePos, Notifiable.NONE, p ->
                    terrainBuilder.askBuildLadderTower(p, towerDir, towerLayers)
            );
        } else {
            // Nexus unter uns -> Schacht nach unten
            BlockPos below = basePos.down();
            if (world.getBlockState(below).isAir()) {
                // Unter uns ist Luft -> kein solider Boden zum Reinarbeiten
                return;
            }

            int depth = Math.min(32, -dy + 1);
            final Direction shaftDir = orientation;
            final int shaftDepth = Math.max(4, depth);

            this.currentBuildTarget = basePos;

            terrainModifier.submitJob(basePos, Notifiable.NONE, p ->
                    terrainBuilder.askBuildLadderShaftDown(p, shaftDir, shaftDepth)
            );
        }
    }





    public PigmanEngineerEntity(EntityType<PigmanEngineerEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new BuilderIMMobNavigation(this);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(0, new MineBlockGoal(this));
        goalSelector.add(1, new AttackNexusGoal<>(this));
        goalSelector.add(2, new GoToNexusGoal(this));
        goalSelector.add(3, new MobMeleeAttackGoal(this, 1, false));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 7));
        goalSelector.add(9, new LookAtEntityGoal(this, IMCreeperEntity.class, 12));
        goalSelector.add(9, new LookAroundGoal(this));

        targetSelector.add(1, new CustomRangeActiveTargetGoal<>(this, VillagerEntity.class, 3, true));
        targetSelector.add(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PlayerEntity.class, 3, true), this::hasNexus));
        targetSelector.add(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PlayerEntity.class, this::getSenseRange, false), () -> !hasNexus()));
        targetSelector.add(2, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PlayerEntity.class, this::getAggroRange, true), () -> !hasNexus()));
        targetSelector.add(3, new RevengeGoal(this));
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        Item heldItem = switch (getRandom().nextInt(3)) {
            case 0 -> Items.LADDER;
            case 1 -> Items.IRON_PICKAXE;
            default -> InvItems.ENGY_HAMMER;
        };
        equipStack(EquipmentSlot.MAINHAND, heldItem.getDefaultStack());
    }

    @Override
    public float getMaxSelfDamage() {
        return 0;
    }

    @Override
    public float getSelfDamage() {
        return 0;
    }

    @Override
    public void mobTick() {
        super.mobTick();
        terrainModifier.onUpdate();

        if (!getWorld().isClient) {
            // Wenn gerade kein anderer Baujob läuft:
            if (!terrainModifier.isBusy()) {
                // Notfall-Turm direkt unter dem Nexus ausprobieren
                tryEmergencyTowerBuild();
            }

            // Wenn nach onUpdate + evtl. Emergency-Build nichts mehr zu tun ist,
            // Build-Target zurücksetzen
            if (!terrainModifier.isBusy()) {
                currentBuildTarget = null;
            }
        }
    }




    @Override
    public void tickMovement() {
        super.tickMovement();
        terrainBuilder.setBuildRate(1 + supportThisTick * 0.33F);
        supportThisTick = 0;
    }


    @Override
    public void travel(Vec3d movementInput) {
        if (!this.getWorld().isClient && terrainModifier.isBusy() && currentBuildTarget != null) {

            double maxReach = 4.5D;           // wie im TerrainModifier
            double maxReachSq = maxReach * maxReach;

            double distSq = this.getEyePos().squaredDistanceTo(
                    Vec3d.ofCenter(currentBuildTarget)
            );

            // Erst wenn er WIRKLICH in Reichweite ist, einfrieren
            if (distSq <= maxReachSq) {
                super.travel(Vec3d.ZERO);
                return;
            }
        }

        super.travel(movementInput);
    }




    @Override
    public void baseTick() {
        super.baseTick();
        updateAnimation();
    }

    protected void updateAnimation() {
        if (!getWorld().isClient && terrainModifier.isBusy()) {
            swingHand(Hand.MAIN_HAND);
            PathAction currentAction = getNavigatorNew().getCurrentWorkingAction();
            if (currentAction == PathAction.NONE) {
                equipStack(EquipmentSlot.MAINHAND, Items.IRON_PICKAXE.getDefaultStack());
            } else {
                equipStack(EquipmentSlot.MAINHAND, InvItems.ENGY_HAMMER.getDefaultStack());
            }
        }
    }


    @Override
    public boolean handlePathAction(BlockPos pos, PathAction action, Notifiable asker) {
        if (action.getType() == PathAction.Type.BRIDGE) {
            // NEU: Build-Target merken
            this.currentBuildTarget = pos;
            return terrainModifier.submitJob(pos, asker, terrainBuilder::askBuildBridge);
        }

        if (action.getType() == PathAction.Type.SCAFFOLD) {
            this.currentBuildTarget = pos;
            return terrainModifier.submitJob(pos, asker, terrainBuilder::askBuildScaffoldLayer);
        }

        if (action.getType() == PathAction.Type.TOWER) {
            Direction dir = action.getOrientation();
            if (dir == null || !dir.getAxis().isHorizontal()) {
                dir = getHorizontalFacing();
            }

            int startY = pos.getY();
            int targetY = startY;
            NexusAccess nexus = getNexus();
            if (nexus != null) {
                targetY = nexus.getOrigin().getY();
            }

            int diff = targetY - startY;

            // Sicherstellen, dass wir überhaupt etwas tun
            if (diff == 0) {
                diff = 4; // z.B. kleine Standardhöhe nach oben, kannst du anpassen
            }

            final Direction towerDir = dir;

            if (diff > 0) {
                // Nexus liegt höher -> normalen Leiter-Tower nach oben bauen
                int layersUp = Math.min(diff, 32);
                this.currentBuildTarget = pos;

                return terrainModifier.submitJob(pos, asker, p ->
                        terrainBuilder.askBuildLadderTower(p, towerDir, layersUp)
                );
            } else {
                // Nexus liegt tiefer -> Schacht nach unten bauen
                int depthDown = Math.min(-diff, 32); // positive Tiefe
                this.currentBuildTarget = pos;

                return terrainModifier.submitJob(pos, asker, p ->
                        terrainBuilder.askBuildLadderShaftDown(p, towerDir, depthDown)
                );
            }
        }



        if (action.getType() == PathAction.Type.LADDER) {
            return terrainModifier.submitJob(pos, asker, p -> {
                Direction direction = action.getOrientation();
                if (direction == null) {
                    direction = getHorizontalFacing();
                }

                // NEU:
                this.currentBuildTarget = p;

                return terrainBuilder.askBuildLadder(p, direction);
            });
        }

        return true;
    }





    @Override
    public void onPathSet() {
        terrainModifier.cancelTask();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH;
    }

    public void supportForTick(MobEntity entity, float amount) {
        supportThisTick += amount;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Deprecated
    @Override
    public String getLegacyName() {
        return "IMPigManEngineer-T1";
    }
}