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
import net.minecraft.world.level.pathfinder.Path;

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

    private final TerrainModifier terrainModifier = new TerrainModifier(this, 2.8F);
    private final TerrainDigger terrainDigger = new TerrainDigger(this, terrainModifier, 1.0F);
    private final TerrainBuilder terrainBuilder = new TerrainBuilder(this, 1);

    private float supportThisTick;





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
            return terrainModifier.submitJob(pos, asker, terrainBuilder::askBuildBridge);
        }

        if (action.getType() == PathAction.Type.SCAFFOLD) {
            return terrainModifier.submitJob(pos, asker, terrainBuilder::askBuildScaffoldLayer);
        }

        if (action.getType() == PathAction.Type.TOWER) {
            Direction dir = action.getOrientation();
            if (dir == null || !dir.getAxis().isHorizontal()) {
                dir = getDirection();
            }
            final Direction towerDir = dir;
            // The block plan is anchored on the engineer's current feet so it
            // can be completed without walking between placements.
            BlockPos planBase = blockPosition().above();
            return terrainModifier.submitJob(planBase, asker, p ->
                    terrainBuilder.askBuildLadderTower(p, towerDir, 3)
            );
        }



        if (action.getType() == PathAction.Type.LADDER) {
            return terrainModifier.submitJob(pos, asker, p -> {
                Direction direction = action.getOrientation();
                if (direction == null) {
                    direction = getDirection();
                }

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
