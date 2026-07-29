package com.invasion.entity;

import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.ModifyBlockEntry;
import com.invasion.entity.ai.builder.TerrainDigger;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.BuilderIMMobNavigation;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.item.InvItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PigmanEngineerEntity extends IMMobEntity implements Miner {
    private static final int BRIDGE_PLANK_BUILD_TIME = 45;
    private static final int TOWER_PLANK_BUILD_TIME = 45;
    private static final int TOWER_LADDER_BUILD_TIME = 25;

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
        Direction bridgeDirection = null;
        var path = getNavigation().getPath();
        if (path != null && !path.isDone()) {
            BlockPos pathPos = path.getNextNodePos();
            int dx = pathPos.getX() - mobPos.getX();
            int dz = pathPos.getZ() - mobPos.getZ();
            if ((dx != 0 || dz != 0)
                    && Math.abs(pathPos.getY() - mobPos.getY()) <= 1) {
                Direction direction =
                        Direction.getApproximateNearest(dx, 0, dz);
                if (direction.getAxis().isHorizontal()) {
                    bridgeDirection = direction;
                }
            }
        }

        if (bridgeDirection == null) {
            BlockPos nexusPos = getNexus().getOrigin();
            double dx = nexusPos.getX() - mobPos.getX();
            double dz = nexusPos.getZ() - mobPos.getZ();
            Direction direction = Direction.getApproximateNearest(dx, 0, dz);
            if (!direction.getAxis().isHorizontal()) {
                return;
            }
            bridgeDirection = direction;
        }

        // Always begin with the adjacent block. The modern path can skip one
        // node over a gap; building only below that node leaves an unusable hole.
        BlockPos nextPos = mobPos.relative(bridgeDirection);
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
            Direction direction = bridgeDirection;
            terrainModifier.submitJob(
                    nextPos,
                    Notifiable.NONE,
                    pos -> terrainBuilder.askBuildBridgeLine(
                            pos, direction, 3));
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
        Direction orientation = Direction.getNearest(dx, 0.0D, dz);
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
            BlockState aboveState = world.getBlockState(above);
            if (aboveState.isAir()
                    || aboveState.is(Blocks.OAK_PLANKS)
                    || aboveState.is(Blocks.COBBLESTONE)) {
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
    public void customServerAiStep() {
        super.customServerAiStep();
        terrainModifier.onUpdate();
        towerBuildCooldown = Math.max(0, towerBuildCooldown - 1);

        if (!level().isClientSide) {
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
    public void travel(Vec3 movementInput) {
        if (!this.level().isClientSide && terrainModifier.isBusy() && currentBuildTarget != null) {

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
        if (!level().isClientSide && terrainModifier.isBusy()) {
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
            return beginBridgeAction(pos, asker);
        }

        return false;
    }

    private boolean beginBridgeAction(BlockPos feetPos, Notifiable asker) {
        var movement = getDeltaMovement();
        setXxa(0);
        setZza(0);
        setSpeed(0);
        setDeltaMovement(0, movement.y, 0);

        BlockState feetState = level().getBlockState(feetPos);
        BlockPos placementPos = feetState.getFluidState().isEmpty()
                ? feetPos.below()
                : feetPos;

        List<ModifyBlockEntry> entries = new ArrayList<>(3);
        BlockPos currentFeetPos = blockPosition();
        int stepX = Integer.signum(feetPos.getX() - currentFeetPos.getX());
        int stepZ = Integer.signum(feetPos.getZ() - currentFeetPos.getZ());

        if (stepX != 0 && stepZ != 0) {
            // Fill both sides of the corner before the diagonal destination.
            // This leaves no unsupported diagonal gap between bridge blocks.
            addBridgePlank(entries, placementPos.offset(-stepX, 0, 0));
            addBridgePlank(entries, placementPos.offset(0, 0, -stepZ));
        }
        addBridgePlank(entries, placementPos);

        return terrainModifier.requestTask(entries, asker, null);
    }

    private void addBridgePlank(List<ModifyBlockEntry> entries, BlockPos pos) {
        BlockState replacedState = level().getBlockState(pos);
        // Air and every fluid state are valid, including flowing water/lava.
        if (replacedState.isAir() || !replacedState.getFluidState().isEmpty()) {
            entries.add(new ModifyBlockEntry(
                    pos,
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    BRIDGE_PLANK_BUILD_TIME
            ));
        }
    }

    public boolean tryStartTowerBuild() {
        if (buildingTower || towerBuildCooldown > 0 || !hasNexus()) {
            return false;
        }

        BlockPos nexusPos = getNexus().getOrigin();
        BlockPos basePos = blockPosition();
        int deltaX = nexusPos.getX() - basePos.getX();
        int deltaZ = nexusPos.getZ() - basePos.getZ();
        Direction towardNexus;
        if (Math.abs(deltaX) >= Math.abs(deltaZ) && deltaX != 0) {
            towardNexus = deltaX > 0 ? Direction.EAST : Direction.WEST;
        } else if (deltaZ != 0) {
            towardNexus = deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            towardNexus = getDirection();
        }

        Direction ladderFacing = towardNexus.getOpposite();
        BlockPos towerBase = basePos.relative(towardNexus);
        if (!canBuildTowerAt(basePos, towerBase)) {
            towerBuildCooldown = 40;
            return false;
        }

        List<ModifyBlockEntry> entries = createTowerPlan(
                basePos, towerBase, ladderFacing);
        if (entries.isEmpty()) {
            towerBuildCooldown = 40;
            return false;
        }

        stopHorizontalMovementForTower();
        buildingTower = true;
        boolean accepted = terrainModifier.requestTask(
                entries,
                status -> {
                    buildingTower = false;
                    towerBuildCooldown = status == Notifiable.Status.SUCCESS
                            ? 20
                            : 80;
                    if (status == Notifiable.Status.SUCCESS
                            && getNavigation()
                                    instanceof BuilderIMMobNavigation navigation) {
                        navigation.resumeAfterTowerBuild();
                    }
                },
                null);
        if (!accepted) {
            buildingTower = false;
        }
        return accepted;
    }

    private boolean canBuildTowerAt(BlockPos ladderBase, BlockPos towerBase) {
        for (int height = 0; height < 3; height++) {
            BlockPos supportPos = towerBase.above(height);
            BlockState support = level().getBlockState(supportPos);
            if (!support.isCollisionShapeFullBlock(level(), supportPos)
                    && !support.canBeReplaced()) {
                return false;
            }

            BlockState ladderSpace =
                    level().getBlockState(ladderBase.above(height));
            if (!ladderSpace.is(Blocks.LADDER)
                    && !ladderSpace.canBeReplaced()) {
                return false;
            }
        }

        BlockPos platformCenter = towerBase.above(3);
        BlockPos ladderOpening = ladderBase.above(3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                if (platformPos.equals(ladderOpening)) {
                    continue;
                }
                BlockState platformState = level().getBlockState(platformPos);
                if (!platformState.isCollisionShapeFullBlock(
                                level(), platformPos)
                        && !platformState.canBeReplaced()) {
                    return false;
                }
            }
        }
        BlockState exitSpace = level().getBlockState(ladderOpening);
        if (!exitSpace.is(Blocks.LADDER) && !exitSpace.canBeReplaced()) {
            return false;
        }
        return true;
    }

    private List<ModifyBlockEntry> createTowerPlan(
            BlockPos ladderBase,
            BlockPos towerBase,
            Direction ladderFacing) {
        List<ModifyBlockEntry> entries = new ArrayList<>(15);
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, ladderFacing);

        // Phase 1: three solid support blocks, accepting existing full blocks.
        for (int height = 0; height < 3; height++) {
            BlockPos supportPos = towerBase.above(height);
            if (!level().getBlockState(supportPos)
                    .isCollisionShapeFullBlock(level(), supportPos)) {
                entries.add(new ModifyBlockEntry(
                        supportPos, planks, TOWER_PLANK_BUILD_TIME));
            }
        }

        // Phase 2: ladders on the side of the column facing the engineer.
        for (int height = 0; height < 3; height++) {
            BlockPos ladderPos = ladderBase.above(height);
            if (!level().getBlockState(ladderPos).is(Blocks.LADDER)) {
                entries.add(new ModifyBlockEntry(
                        ladderPos, ladder, TOWER_LADDER_BUILD_TIME));
            }
        }

        // Phase 3: a 3x3 platform footprint above the column. The ladder cell
        // remains open as the only way through the deck.
        BlockPos platformCenter = towerBase.above(3);
        BlockPos ladderOpening = ladderBase.above(3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                if (!platformPos.equals(ladderOpening)
                        && level().getBlockState(platformPos).canBeReplaced()) {
                    entries.add(new ModifyBlockEntry(
                            platformPos, planks, TOWER_PLANK_BUILD_TIME));
                }
            }
        }

        // The exit ladder is placed last because it is supported by the new
        // platform centre block.
        if (!level().getBlockState(ladderOpening).is(Blocks.LADDER)) {
            entries.add(new ModifyBlockEntry(
                    ladderOpening, ladder, TOWER_LADDER_BUILD_TIME));
        }
        return entries;
    }

    private void stopHorizontalMovementForTower() {
        var movement = getDeltaMovement();
        setXxa(0);
        setZza(0);
        setSpeed(0);
        setDeltaMovement(0, Math.min(movement.y, 0), 0);
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
    }

    public boolean isBuildingTower() {
        return buildingTower;
    }





    @Override
    public void onPathSet() {
        if (!buildingTower) {
            terrainModifier.cancelTask();
        }
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
        // Building support is intentionally disabled while the engineer's
        // construction system is rebuilt from a blank state.
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
