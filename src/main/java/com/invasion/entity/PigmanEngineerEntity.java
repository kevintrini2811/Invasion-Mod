package com.invasion.entity;

import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainDigger;
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
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

public class PigmanEngineerEntity extends IMMobEntity implements Miner {
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (getRandom().nextBoolean()) {
            spawnAtLocation(level, Items.LEATHER);
        } else {
            spawnAtLocation(level, isOnFire() ? Items.COOKED_PORKCHOP : Items.PORKCHOP);
        }
    }

    private final TerrainModifier terrainModifier = new TerrainModifier(this, 4.5F);
    private final TerrainDigger terrainDigger = new TerrainDigger(this, terrainModifier, 1.0F);
    private final TerrainBuilder terrainBuilder = new TerrainBuilder(this, 1);

    private float supportThisTick;
    @org.jetbrains.annotations.Nullable
    private BlockPos currentBuildTarget;

    /**
     * Vanilla's modern ground navigator rejects a node over a gap before our
     * actionable node can always be considered. Detect the next ledge while
     * attacking a nexus so the engineer can still start the original bridge
     * building action.
     */
    private void tryBuildBridgeAhead() {
        if (terrainModifier.isBusy() || !hasNexus() || getAIGoal() != HasAiGoals.Goal.BREAK_NEXUS) {
            return;
        }

        BlockPos mobPos = blockPosition();
        BlockPos nextPos = null;
        var path = getNavigation().getPath();
        if (path != null && !path.isDone()) {
            BlockPos pathPos = path.getNextNodePos();
            int horizontalDistance = Math.abs(pathPos.getX() - mobPos.getX())
                    + Math.abs(pathPos.getZ() - mobPos.getZ());
            if (horizontalDistance > 0 && horizontalDistance <= 2
                    && Math.abs(pathPos.getY() - mobPos.getY()) <= 1) {
                nextPos = new BlockPos(pathPos.getX(), mobPos.getY(), pathPos.getZ());
            }
        }

        if (nextPos == null) {
            BlockPos nexusPos = getNexus().getOrigin();
            double dx = nexusPos.getX() - mobPos.getX();
            double dz = nexusPos.getZ() - mobPos.getZ();
            Direction direction = Direction.getApproximateNearest(dx, 0, dz);
            if (!direction.getAxis().isHorizontal()) {
                return;
            }
            nextPos = mobPos.relative(direction);
        }

        Level world = level();
        BlockPos currentFloor = mobPos.below();
        BlockPos bridgeFloor = nextPos.below();
        boolean standingAtLedge = world.getBlockState(currentFloor)
                .isCollisionShapeFullBlock(world, currentFloor);
        BlockState stateAtNextPos = world.getBlockState(nextPos);
        boolean spaceIsClear = (PathingUtil.isAirOrReplaceable(stateAtNextPos)
                || !stateAtNextPos.getFluidState().isEmpty())
                && PathingUtil.isAirOrReplaceable(world.getBlockState(nextPos.above()));
        boolean floorIsMissing = !world.getBlockState(bridgeFloor)
                .isCollisionShapeFullBlock(world, bridgeFloor);

        if (standingAtLedge && spaceIsClear && floorIsMissing) {
            currentBuildTarget = nextPos;
            terrainModifier.submitJob(nextPos, Notifiable.NONE, terrainBuilder::askBuildBridge);
        }
    }

    private void tryEmergencyTowerBuild() {
        if (terrainModifier.isBusy()) {
            return;
        }

        NexusAccess nexus = getNexus();
        if (nexus == null) {
            return;
        }

        Level world = level();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.blockPosition();

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
        Direction orientation = Direction.getApproximateNearest(dx, 0.0D, dz);
        if (!orientation.getAxis().isHorizontal()) {
            orientation = this.getDirection();
        }

        // Basis-Position: Block auf dem wir stehen / vor uns
        BlockPos basePos = mobPos;
        BlockState baseState = world.getBlockState(basePos);
        if (!PathingUtil.isAirOrReplaceable(baseState)) {
            basePos = basePos.relative(orientation);
        }

        // Nexus über uns -> Turm nach oben
        if (dy > 0) {
            BlockPos above = basePos.above();
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
            BlockPos below = basePos.below();
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





    public PigmanEngineerEntity(EntityType<PigmanEngineerEntity> type, Level world) {
        super(type, world);
        getNavigatorNew().setCanDestroyBlocks(true);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean onPathBlocked(Path path, Notifiable notifee) {
        if (path.isDone()) {
            return false;
        }
        return terrainDigger.askClearPosition(path.getNextNodePos(), notifee, 1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23F)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
                .add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new BuilderIMMobNavigation(this);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new AttackNexusGoal<>(this));
        goalSelector.addGoal(2, new GoToNexusGoal(this));
        goalSelector.addGoal(3, new MobMeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 7));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(this, Villager.class, 3, true));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, 3, true), this::hasNexus));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false), () -> !hasNexus()));
        targetSelector.addGoal(2, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true), () -> !hasNexus()));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        Item heldItem = switch (getRandom().nextInt(3)) {
            case 0 -> Items.LADDER;
            case 1 -> Items.IRON_PICKAXE;
            default -> InvItems.ENGY_HAMMER;
        };
        setItemSlot(EquipmentSlot.MAINHAND, heldItem.getDefaultInstance());
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
    public void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        terrainModifier.onUpdate();

        if (tickCount % 5 == 0) {
            for (ItemEntity item : serverLevel.getEntitiesOfClass(
                    ItemEntity.class,
                    getBoundingBox().inflate(1.25D),
                    candidate -> !candidate.hasPickUpDelay()
                            && wantsToPickUp(
                                    serverLevel, candidate.getItem()))) {
                pickUpItem(serverLevel, item);
            }
        }

        if (!level().isClientSide()) {
            // Wenn gerade kein anderer Baujob läuft:
            if (!terrainModifier.isBusy()) {
                tryBuildBridgeAhead();
            }
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
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot.isArmor()
                && isEquippableInSlot(stack, slot)
                && canReplaceCurrentItem(stack, getItemBySlot(slot), slot);
    }




    @Override
    public void aiStep() {
        super.aiStep();
        terrainBuilder.setBuildRate(1 + supportThisTick * 0.33F);
        supportThisTick = 0;
    }


    @Override
    public void travel(Vec3 movementInput) {
        if (!this.level().isClientSide() && terrainModifier.isBusy() && currentBuildTarget != null) {

            double maxReach = 4.5D;           // wie im TerrainModifier
            double maxReachSq = maxReach * maxReach;

            double distSq = this.getEyePosition().distanceToSqr(
                    Vec3.atCenterOf(currentBuildTarget)
            );

            // Erst wenn er WIRKLICH in Reichweite ist, einfrieren
            if (distSq <= maxReachSq) {
                super.travel(Vec3.ZERO);
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
        if (!level().isClientSide() && terrainModifier.isBusy()) {
            swing(InteractionHand.MAIN_HAND);
            PathAction currentAction = getNavigatorNew().getCurrentWorkingAction();
            if (currentAction == PathAction.NONE) {
                setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_PICKAXE.getDefaultInstance());
            } else {
                setItemSlot(EquipmentSlot.MAINHAND, InvItems.ENGY_HAMMER.getDefaultInstance());
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
                dir = getDirection();
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
                    direction = getDirection();
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
        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
    }

    public void supportForTick(Mob entity, float amount) {
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
