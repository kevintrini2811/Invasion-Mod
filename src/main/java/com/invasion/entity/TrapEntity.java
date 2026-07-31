package com.invasion.entity;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvSounds;
import com.invasion.item.InvItems;

public class TrapEntity extends Entity {
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.STRING);

    private int timeTriggered;

    public TrapEntity(EntityType<TrapEntity> type, Level world) {
        super(type, world);
    }

    public TrapEntity(EntityType<TrapEntity> type, Level world, double x, double y, double z) {
        this(type, world, x, y, z, Type.EMPTY);
    }

    public TrapEntity(EntityType<TrapEntity> type, Level world, double x, double y, double z, Type trapType) {
        this(type, world);
        setTrapType(trapType);
        moveTo(x, y, z, 0, 0);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TYPE, Type.EMPTY.getSerializedName());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 20 == 0 && getTrapType() == Type.RIFT) {
                doRiftParticles();
            }
            return;
        }

        if (!isValidPlacement()) {
            spawnAtLocation(InvItems.EMPTY_TRAP);
            discard();
            return;
        }

        if (getTrapType() != Type.EMPTY) {
            if (tickCount < 60) {
                if (tickCount % 10 == 0) {
                    playSound(InvSounds.ENTITY_TRAP_COUNTDOWN, 0.1F, 1);
                }
                return;
            } else if (tickCount == 60) {
                playSound(InvSounds.ENTITY_TRAP_READY, 0.1F, 1);
            } else {
                List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.1), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                if (targets.size() > 0) {
                    if (timeTriggered <= 0) {
                        timeTriggered = tickCount + 250;
                    }

                    if (tickCount > timeTriggered) {
                        if (targets.stream().anyMatch(this::trapEffect)) {
                            setTrapType(Type.EMPTY);
                        }
                    } else {
                        int timeRemaining = timeTriggered - tickCount;
                        if (tickCount % Math.max(timeRemaining / 10, 1) == 0) {
                            playSound(InvSounds.ENTITY_TRAP_READY, 0.1F, 1);
                        }
                    }
                } else {
                    if (timeTriggered > 0) {
                        if (tickCount > timeTriggered) {
                            trapEffect(null);
                            setTrapType(Type.EMPTY);
                        } else {
                            int timeRemaining = timeTriggered - tickCount;
                            if (tickCount % Math.max(timeRemaining / 10, 1) == 0) {
                                playSound(InvSounds.ENTITY_TRAP_READY, 0.1F, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean trapEffect(@Nullable LivingEntity triggerEntity) {
        if (getTrapType() == Type.RIFT) {
            if (triggerEntity != null) {
                triggerEntity.hurt(damageSources().magic(), triggerEntity instanceof Player ? 12 : 38);
            }

            for (Entity entity : level().getEntities(this, getBoundingBox().inflate(2, 1, 2))) {
                if (entity instanceof TrapEntity) {
                    continue;
                }
                entity.hurt(damageSources().magic(), 8);
                if (entity instanceof Stunnable l) {
                    l.stun(60);
                }
            }
            playSound(SoundEvents.ITEM_BREAK, 1.5F, random.nextFloat() * 0.25F + 0.55F);
        } else if (getTrapType() == Type.FIRE) {
            playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.5F, 1.15F / (random.nextFloat() * 0.3F + 1));
            doFireball(1.1F, 8);
        }

        return true;
    }

    public void playerTouch(Player player) {
        if (tickCount > 30 && getTrapType() == Type.EMPTY) {
            ItemStack drop = InvItems.EMPTY_TRAP.getDefaultInstance();
            if (!player.addItem(drop)) {
                player.drop(drop, false);
            }
            playSound(SoundEvents.ITEM_PICKUP, 1, 1);
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = super.interact(player, hand);
        if (result != InteractionResult.PASS || getTrapType() == Type.EMPTY) {
            return result;
        }
        ItemStack curItem = player.getItemInHand(hand);
        if (curItem.is(InvItems.MATERIAL_PROBE)) {
            ItemStack drop = getTrapType().getDroppedStack();
            if (!player.addItem(drop)) {
                player.drop(drop, false);
            }
            playSound(SoundEvents.ITEM_PICKUP, 1, 1);
            discard();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public Type getTrapType() {
        return Type.of(entityData.get(TYPE));
    }

    public void setTrapType(Type type) {
        entityData.set(TYPE, type.getSerializedName());
    }

    public boolean isValidPlacement() {
        BlockPos below = blockPosition().below();
        BlockState supportingState = level().getBlockState(below);
        return supportingState.entityCanStandOn(level(), below, this);
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }


    private void doFireball(float size, int initialDamage) {
        int sz = Mth.ceil(size);
        for (BlockPos pos : BlockPos.withinManhattan(blockPosition(), sz, sz, sz)) {
            if (!pos.equals(blockPosition())) {
                BlockState state = level().getBlockState(pos);
                if (state.isAir() || state.ignitedByLava()) {
                    level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
        }

        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(size))) {
            entity.setSecondsOnFire(8);
            entity.hurt(damageSources().onFire(), initialDamage);
        }
    }

    private void doRiftParticles() {
        for (int i = 0; i < 300; i++) {
            double x = random.triangle(0, 3);
            double z = random.triangle(0, 3);
            level().addParticle(ParticleTypes.PORTAL, getX() + x, getY() + 2, getZ() + z, -x / 3F, -2, -z / 3F);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setTrapType(Type.of(compound.getString("type")));
        timeTriggered = compound.getInt("time_triggered");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("type", getTrapType().getSerializedName());
        compound.putInt("time_triggered", timeTriggered);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    public enum Type implements StringRepresentable {
        EMPTY(() -> InvItems.EMPTY_TRAP),
        RIFT(() -> InvItems.RIFT_TRAP),
        FIRE(() -> InvItems.FLAME_TRAP);

        @SuppressWarnings("deprecation")
        public static final EnumCodec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @SuppressWarnings("deprecation")
        public static Type of(String id) {
            return CODEC.byName(id, EMPTY);
        }

        private final String name = name().toLowerCase(Locale.ROOT);
        private final ItemLike item;

        Type(ItemLike item) {
            this.item = item;
        }

        public ItemStack getDroppedStack() {
            return item.asItem().getDefaultInstance();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

}
