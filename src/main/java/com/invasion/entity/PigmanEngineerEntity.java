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
import net.minecraft.world.entity.npc.villager.Villager;
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
    private boolean buildingTower;
    private int towerBuildCooldown;
    private BlockPos towerBuildPosition;





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
        goalSelector.addGoal(1, new MobMeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(2, new GoToNexusGoal(this));
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
        if (buildingTower && isTowerBuildInterrupted()) {
            returnToTowerBuildPosition();
        } else {
            terrainModifier.onUpdate();
        }
        towerBuildCooldown = Math.max(0, towerBuildCooldown - 1);

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

    }

    @Override
    public boolean hurtServer(
            ServerLevel serverLevel, DamageSource source, float damage) {
        boolean damaged = super.hurtServer(serverLevel, source, damage);
        if (damaged && buildingTower) {
            if (source.getEntity() instanceof LivingEntity attacker
                    && attacker.isAlive()) {
                setTarget(attacker);
            }
        }
        return damaged;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot.isArmor()
                && isEquippableInSlot(stack, slot)
                && canReplaceCurrentItem(stack, getItemBySlot(slot), slot);
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

    public void cancelStalledTerrainTask(Notifiable.Status status) {
        terrainModifier.cancelTask(status);
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
        towerBuildPosition = basePos;
        boolean accepted = terrainModifier.requestTask(
                entries,
                status -> {
                    buildingTower = false;
                    towerBuildPosition = null;
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
            towerBuildPosition = null;
        }
        return accepted;
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
            navigation.returnToTowerBuild(towerBuildPosition);
        }
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

        // Phase 3: build the platform centre first so the exit ladder has
        // support. Placing the ladder immediately afterwards guarantees that
        // the climbable column reaches through the platform before the
        // remaining deck blocks are filled in.
        BlockPos platformCenter = towerBase.above(3);
        BlockPos ladderOpening = ladderBase.above(3);
        if (level().getBlockState(platformCenter).canBeReplaced()) {
            entries.add(new ModifyBlockEntry(
                    platformCenter, planks, TOWER_PLANK_BUILD_TIME));
        }
        if (!level().getBlockState(ladderOpening).is(Blocks.LADDER)) {
            entries.add(new ModifyBlockEntry(
                    ladderOpening, ladder, TOWER_LADDER_BUILD_TIME));
        }

        // Phase 4: complete the 3x3 platform footprint. The ladder cell stays
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
