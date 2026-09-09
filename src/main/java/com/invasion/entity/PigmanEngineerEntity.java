package com.invasion.entity;

import com.invasion.Notifiable;
import com.invasion.block.BlockMetadata;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PigmanEngineerEntity extends IMMobEntity implements Miner {
    private static final int BRIDGE_PLANK_BUILD_TIME = 45;
    private static final int TOWER_PLANK_BUILD_TIME = 45;
    private static final int TOWER_LADDER_BUILD_TIME = 25;
    private static final int TOWER_INTERRUPTION_TIMEOUT = 20 * 10;

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (!dropsEngineerBonusLoot()) {
            return;
        }
        if (getRandom().nextBoolean()) {
            spawnAtLocation(Items.LEATHER);
        } else {
            spawnAtLocation(isOnFire() ? Items.COOKED_PORKCHOP : Items.PORKCHOP);
        }
    }

    protected boolean dropsEngineerBonusLoot() {
        return true;
    }

    // Reaches the far upper corner of a 3x3 tower platform while the
    // engineer remains at the ladder base during construction.
    private final TerrainModifier terrainModifier = new TerrainModifier(this, 6.0F);
    private final TerrainDigger terrainDigger = new TerrainDigger(this, terrainModifier, 1.0F);
    private boolean buildingTower;
    private int towerBuildCooldown;
    private int towerInterruptedTicks;
    private BlockPos towerBuildPosition;
    private BlockPos towerPlatformCenter;





    public PigmanEngineerEntity(EntityType<? extends PigmanEngineerEntity> type, Level world) {
        super(type, world);
        getNavigatorNew().setCanDestroyBlocks(true);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean onPathBlocked(Path path, Notifiable notifee) {
        if (path.isDone()) {
            return false;
        }
        return terrainDigger.askClearPosition(
                path.getNextNodePos(), notifee,
                1.0F / getDiggingSpeedMultiplier());
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
        goalSelector.addGoal(1, new MobMeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(2, new GoToNexusGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 7));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, 3, true), this::hasNexus));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false), () -> !hasNexus()));
        targetSelector.addGoal(2, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true), () -> !hasNexus()));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        Item heldItem = switch (getRandom().nextInt(3)) {
            case 0 -> Items.LADDER;
            case 1 -> getMiningTool();
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
        ServerLevel serverLevel = (ServerLevel) level();
        if (buildingTower && isTowerBuildInterrupted()) {
            towerInterruptedTicks++;
            if (towerInterruptedTicks >= TOWER_INTERRUPTION_TIMEOUT) {
                cancelStalledTowerBuild();
            } else {
                returnToTowerBuildPosition();
            }
        } else {
            towerInterruptedTicks = 0;
            terrainModifier.onUpdate();
        }
        towerBuildCooldown = Math.max(0, towerBuildCooldown - 1);

        if (ItemSearchScheduler.shouldSearch(this)) {
            for (ItemEntity item : serverLevel.getEntitiesOfClass(
                    ItemEntity.class,
                    getBoundingBox().inflate(1.25D),
                    candidate -> !candidate.hasPickUpDelay()
                            && wantsToPickUp(candidate.getItem()))) {
                pickUpItem(item);
            }
        }

    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        boolean damaged = super.hurt(source, damage);
        if (damaged && buildingTower) {
            if (source.getEntity() instanceof LivingEntity attacker
                    && attacker.isAlive()) {
                setTarget(attacker);
            }
        }
        return damaged;
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot.isArmor()
                && stack.canEquip(slot, this)
                && canReplaceCurrentItem(stack, getItemBySlot(slot));
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateAnimation();
    }

    protected void updateAnimation() {
        if (!level().isClientSide()
                && terrainModifier.isBusy()
                && (!buildingTower || !isTowerBuildInterrupted())) {
            swing(InteractionHand.MAIN_HAND);
            PathAction currentAction = getNavigatorNew().getCurrentWorkingAction();
            if (currentAction == PathAction.NONE) {
                setItemSlot(EquipmentSlot.MAINHAND, getMiningTool().getDefaultInstance());
            } else {
                setItemSlot(EquipmentSlot.MAINHAND, InvItems.ENGY_HAMMER.getDefaultInstance());
            }
        }
    }

    protected Item getMiningTool() {
        return Items.IRON_PICKAXE;
    }


    @Override
    public boolean handlePathAction(BlockPos pos, PathAction action, Notifiable asker) {
        if (action.getType() == PathAction.Type.BRIDGE) {
            if (!com.invasion.compat.ConfiguredModMobs.allowsBridging(getType(), true)) {
                return false;
            }
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
                    getBuildingBlock(),
                    BRIDGE_PLANK_BUILD_TIME
            ));
        }
    }

    public void cancelStalledTerrainTask(Notifiable.Status status) {
        terrainModifier.cancelTask(status);
    }

    /**
     * Leaves a local pathfinding dead spot without walking into unsupported
     * terrain. Called only after repeated attempts produced no path at all.
     */
    public boolean tryEscapeMissingPath() {
        if (!hasNexus() || buildingTower || terrainModifier.isBusy()) {
            return false;
        }

        BlockPos current = blockPosition();
        BlockPos nexus = getNexus().getOrigin();
        List<BlockPos> candidates = new ArrayList<>();
        for (int radius = 1; radius <= 3; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) {
                        continue;
                    }
                    for (int yOffset : new int[] {0, 1, -1, 2}) {
                        candidates.add(current.offset(xOffset, yOffset, zOffset));
                    }
                }
            }
        }
        candidates.sort(Comparator
                .comparingDouble((BlockPos pos) -> pos.distSqr(nexus))
                .thenComparingInt(pos -> pos.distManhattan(current)));

        for (BlockPos target : candidates) {
                BlockPos floor = target.below();
                if (!level().getBlockState(floor)
                                .isCollisionShapeFullBlock(level(), floor)
                        || !level().noCollision(
                                this,
                                getBoundingBox().move(
                                        target.getX() + 0.5D - getX(),
                                        target.getY() - getY(),
                                        target.getZ() + 0.5D - getZ()))) {
                    continue;
                }

            setPos(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            setDeltaMovement(0, 0, 0);
            fallDistance = 0;
            getNavigation().stop();
            return true;
        }

        return false;
    }

    /**
     * Builds one supported escape step when an engineer is isolated on a
     * platform and therefore cannot take the safe walking recovery above.
     */
    public boolean tryBridgeMissingPath(Notifiable asker) {
        if (!com.invasion.compat.ConfiguredModMobs.allowsBridging(getType(), true)
                || !hasNexus() || buildingTower || terrainModifier.isBusy()) {
            return false;
        }

        BlockPos current = blockPosition();
        for (Direction direction : directionsTowardNexus(current)) {
            BlockPos target = current.relative(direction);
            BlockPos floor = target.below();
            BlockState floorState = level().getBlockState(floor);
            if ((!floorState.isAir()
                            && floorState.getFluidState().isEmpty())
                    || !level().noCollision(
                            this,
                            getBoundingBox().move(
                                    target.getX() + 0.5D - getX(),
                                    target.getY() - getY(),
                                    target.getZ() + 0.5D - getZ()))) {
                continue;
            }

            if (beginBridgeAction(target, asker)) {
                return true;
            }
        }
        return false;
    }

    private List<Direction> directionsTowardNexus(BlockPos current) {
        BlockPos nexus = getNexus().getOrigin();
        List<Direction> directions = new ArrayList<>(
                Direction.Plane.HORIZONTAL.stream().toList());
        directions.sort(Comparator.comparingInt(direction ->
                -direction.getStepX() * (nexus.getX() - current.getX())
                - direction.getStepZ() * (nexus.getZ() - current.getZ())));
        return directions;
    }

    public boolean tryStartTowerBuild() {
        if (!com.invasion.compat.ConfiguredModMobs.allowsEngineerTower(getType(), true)
                || buildingTower || towerBuildCooldown > 0 || !hasNexus()
                || !isStandingOnSolidGround()) {
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
        towerInterruptedTicks = 0;
        towerBuildPosition = basePos;
        towerPlatformCenter = towerBase.above(3);
        boolean accepted = terrainModifier.requestTask(
                entries,
                this::verifyTowerClearanceAfterBuild,
                this::onTowerBlockChanged);
        if (!accepted) {
            buildingTower = false;
            towerInterruptedTicks = 0;
            towerBuildPosition = null;
            towerPlatformCenter = null;
        }
        return accepted;
    }

    private void verifyTowerClearanceAfterBuild(Notifiable.Status status) {
        if (status == Notifiable.Status.SUCCESS
                && towerPlatformCenter != null) {
            List<ModifyBlockEntry> clearance = createTowerClearancePlan(
                    towerPlatformCenter);
            if (!clearance.isEmpty()
                    && terrainModifier.requestTask(
                            clearance,
                            this::verifyTowerClearanceAfterBuild,
                            this::onTowerBlockChanged)) {
                return;
            }
        }
        finishTowerBuild(status);
    }

    private void finishTowerBuild(Notifiable.Status status) {
        buildingTower = false;
        towerInterruptedTicks = 0;
        towerBuildPosition = null;
        towerPlatformCenter = null;
        towerBuildCooldown = status == Notifiable.Status.SUCCESS ? 20 : 80;
        if (status == Notifiable.Status.SUCCESS
                && getNavigation()
                        instanceof BuilderIMMobNavigation navigation) {
            navigation.resumeAfterTowerBuild();
        }
    }

    private void onTowerBlockChanged(Notifiable.Status status) {
        ModifyBlockEntry entry = terrainModifier.getLastBlockModified();
        if (status == Notifiable.Status.SUCCESS
                && entry != null
                && entry.newBlock().isAir()) {
            onBlockRemoved(entry.pos(), entry.getOldBlock());
        }
    }

    private boolean isStandingOnSolidGround() {
        if (!onGround() || onClimbable()
                || isInWater() || isInLava()
                || !level().getBlockState(blockPosition())
                        .getFluidState().isEmpty()) {
            return false;
        }

        BlockPos floorPos = blockPosition().below();
        return level().getBlockState(floorPos)
                .isCollisionShapeFullBlock(level(), floorPos);
    }

    private boolean isTowerBuildInterrupted() {
        return getTarget() != null || !isAtTowerBuildPosition();
    }

    private boolean isAtTowerBuildPosition() {
        if (towerBuildPosition == null) {
            return true;
        }
        double targetX = towerBuildPosition.getX() + 0.5D;
        double targetZ = towerBuildPosition.getZ() + 0.5D;
        double deltaX = getX() - targetX;
        double deltaZ = getZ() - targetZ;
        return deltaX * deltaX + deltaZ * deltaZ < 0.16D
                && Math.abs(getY() - towerBuildPosition.getY()) < 0.75D;
    }

    private void returnToTowerBuildPosition() {
        if (getTarget() != null || towerBuildPosition == null
                || isAtTowerBuildPosition()) {
            return;
        }
        if (getNavigation() instanceof BuilderIMMobNavigation navigation) {
            if (navigation.isDone() || towerInterruptedTicks % 20 == 1) {
                navigation.returnToTowerBuild(towerBuildPosition);
            }
        }
    }

    private void cancelStalledTowerBuild() {
        terrainModifier.cancelTask(Notifiable.Status.OUT_OF_RANGE);
        getNavigation().stop();
        towerBuildCooldown = 80;
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

        // The engineer must be able to stand anywhere on the completed deck.
        // Reject a tower site only when one of the two clearance layers
        // contains a block that the engineer cannot remove.
        for (int clearanceHeight = 1; clearanceHeight <= 2;
                clearanceHeight++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos clearancePos = platformCenter.offset(
                            x, clearanceHeight, z);
                    BlockState clearanceState =
                            level().getBlockState(clearancePos);
                    if (!clearanceState.isAir()
                            && BlockMetadata.isIndestructible(clearanceState)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private List<ModifyBlockEntry> createTowerPlan(
            BlockPos ladderBase,
            BlockPos towerBase,
            Direction ladderFacing) {
        List<ModifyBlockEntry> entries = new ArrayList<>(33);
        BlockState planks = getBuildingBlock();
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, ladderFacing);

        BlockPos platformCenter = towerBase.above(3);

        // Phase 1: clear two full blocks of headroom before placing any
        // ladders, so the engineer cannot climb into an unfinished exit.
        entries.addAll(createTowerClearancePlan(platformCenter));

        // Phase 2: three solid support blocks, accepting existing full blocks.
        for (int height = 0; height < 3; height++) {
            BlockPos supportPos = towerBase.above(height);
            if (!level().getBlockState(supportPos)
                    .isCollisionShapeFullBlock(level(), supportPos)) {
                entries.add(new ModifyBlockEntry(
                        supportPos, planks, TOWER_PLANK_BUILD_TIME));
            }
        }

        // Phase 3: ladders on the side of the column facing the engineer.
        for (int height = 0; height < 3; height++) {
            BlockPos ladderPos = ladderBase.above(height);
            if (!level().getBlockState(ladderPos).is(Blocks.LADDER)) {
                entries.add(new ModifyBlockEntry(
                        ladderPos, ladder, TOWER_LADDER_BUILD_TIME));
            }
        }

        // Phase 4: build the platform centre first so the exit ladder has
        // support. Placing the ladder immediately afterwards guarantees that
        // the climbable column reaches through the platform before the
        // remaining deck blocks are filled in.
        BlockPos ladderOpening = ladderBase.above(3);
        if (level().getBlockState(platformCenter).canBeReplaced()) {
            entries.add(new ModifyBlockEntry(
                    platformCenter, planks, TOWER_PLANK_BUILD_TIME));
        }
        if (!level().getBlockState(ladderOpening).is(Blocks.LADDER)) {
            entries.add(new ModifyBlockEntry(
                    ladderOpening, ladder, TOWER_LADDER_BUILD_TIME));
        }

        // Phase 5: complete the 3x3 platform footprint. The ladder cell stays
        // open as the only way through the deck.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                if (!platformPos.equals(platformCenter)
                        && !platformPos.equals(ladderOpening)
                        && level().getBlockState(platformPos).canBeReplaced()) {
                    entries.add(new ModifyBlockEntry(
                            platformPos, planks, TOWER_PLANK_BUILD_TIME));
                }
            }
        }
        return entries;
    }

    private List<ModifyBlockEntry> createTowerClearancePlan(
            BlockPos platformCenter) {
        List<ModifyBlockEntry> entries = new ArrayList<>(18);
        for (int clearanceHeight = 1; clearanceHeight <= 2;
                clearanceHeight++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos clearancePos = platformCenter.offset(
                            x, clearanceHeight, z);
                    if (!level().getBlockState(clearancePos).isAir()) {
                        entries.add(ModifyBlockEntry.ofDeletion(
                                clearancePos,
                                (int) getBlockRemovalCost(clearancePos)));
                    }
                }
            }
        }
        return entries;
    }

    /** The solid block used for bridges, tower supports and platforms. */
    protected BlockState getBuildingBlock() {
        return com.invasion.compat.ConfiguredModMobs
                .buildingBlock(getType(), Blocks.OAK_PLANKS).defaultBlockState();
    }

    private void stopHorizontalMovementForTower() {
        getNavigation().stop();
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
