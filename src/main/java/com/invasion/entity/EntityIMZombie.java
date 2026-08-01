package com.invasion.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.util.math.PosUtils;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.SprintGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import com.invasion.entity.ai.goal.StoopGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.NoNexusPathGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.nexus.NexusAccess;


public class EntityIMZombie extends AbstractIMZombieEntity {
    private static final float TERRAIN_REACH = 3.0F;
    private static final double TERRAIN_REACH_SQR = TERRAIN_REACH * TERRAIN_REACH;
    private static final EntityDataAccessor<Boolean> BABY =
            SynchedEntityData.defineId(
                    EntityIMZombie.class, EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.world.entity.ai.attributes.AttributeModifier
            BABY_SPEED_BONUS = AttributeUtil.addPercentage(
                    InvasionMod.id("baby_zombie_speed"), 50);

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (getRandom().nextFloat() < 0.35F) {
            spawnAtLocation(level, Items.ROTTEN_FLESH);
        }
        if (getTier() == 1 && getFlavour() == 1 && getRandom().nextFloat() < 0.2F) {
            spawnAtLocation(level, Items.WOODEN_SWORD);
        } else if (getTier() == 2 && getFlavour() == 0 && getRandom().nextFloat() < 0.25F) {
            spawnAtLocation(level, Items.IRON_CHESTPLATE);
        } else if (getTier() == 2 && getFlavour() == 1 && getRandom().nextFloat() < 0.25F) {
            spawnAtLocation(level, Items.IRON_SWORD);
        }
    }

    static final int OLD_ZOMBIE = 0;
    static final int ZOMBIE = 1;
    static final int ZOMBIE_T2 = 2;
    static final int ZOMBIE_PIGMAN = 3;
    static final int ZOMBIE_T2A = 4;
    static final int TAR = 5;
    static final int BRUTE = 6;

    // TODO: Account for self-harm when breaking blocks
    // selfDamage = points of damage dealt per block broken
    // masSelfDamage = max damage loss before the mob can no longer mine blocks. If health is less than (maxHealth - maxSelfDamage) it stops.
    protected int selfDamage = 2;
    protected int maxSelfDamage = 6;
    // Zombies may only modify terrain within their actual interaction range.
    // This also prevents an entire cobblestone ramp from being placed remotely.
    private final TerrainModifier terrainModifier = new TerrainModifier(this, TERRAIN_REACH);
    private final TerrainBuilder terrainBuilder = new TerrainBuilder(this, 1.0F);
    private static AttributeSupplier.Builder createBaseAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.19F)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    public static AttributeSupplier.Builder createTierT1V0Attributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    public static AttributeSupplier.Builder createTierT1V1Attributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    public static AttributeSupplier.Builder createTierT2V0Attributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    public static AttributeSupplier.Builder createTierT2V1Attributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 10.0);
    }

    /**
     * Tar Zombie
     */
    public static AttributeSupplier.Builder createTierT2V2ttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 5.0);
    }
    /**
     * Zombie Pigman
     */
    public static AttributeSupplier.Builder createTierT2V3ttributes() {
        return createBaseAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25F)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }
    /**
     * Zombie Brute
     */
    public static AttributeSupplier.Builder createTierT3V0Attributes() {
        return createBaseAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.17F)
                .add(Attributes.ATTACK_DAMAGE, 18.0);
    }

    public EntityIMZombie(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world, 2F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BABY, false);
    }

    @Override
    public boolean isBaby() {
        return entityData.get(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        entityData.set(BABY, baby);
        if (!level().isClientSide()) {
            AttributeUtil.toggleAttribute(
                    this,
                    Attributes.MOVEMENT_SPEED,
                    BABY_SPEED_BONUS,
                    baby);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == BABY) {
            refreshDimensions();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsBaby", isBaby());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBaby(input.getBooleanOr("IsBaby", false)
                || input.getBooleanOr("isBaby", false));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new ProvideSupportGoal(this, 4, true));
        goalSelector.addGoal(3, new PredicatedGoal(new SprintGoal<>(this), () -> getTier() == 3));
        goalSelector.addGoal(4, new PredicatedGoal(new StoopGoal(this), () -> getTier() == 3));
        goalSelector.addGoal(5, new GoToNexusGoal(this));
        addWeaponCombatGoals(1.3F);
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false), () -> getTier() != 3));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F), () -> getTier() != 3 && NoNexusPathGoal.isLostPathToNexus(this)));
        targetSelector.addGoal(4, new CustomRangeActiveTargetGoal<>(this, IronGolem.class, this::getAggroRange, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()
                && getType() == InvEntities.ZOMBIE
                && level().getBlockState(blockPosition()).is(Blocks.SOUL_FIRE)) {
            EntityIMSpeedyZombie.convertFrom(this);
            return;
        }
        if (!level().isClientSide() && isTar() && isOnFire()) {
            // Tar keeps burning until it actually enters water. Refreshing a
            // short duration here prevents the normal fire timer from
            // expiring while still allowing water to clear it first.
            igniteForTicks(20);
            spreadTarFire();
        }
    }

    @Override
    public void clearFire() {
        if (!isTar() || isInWater()) {
            super.clearFire();
        }
    }
    @Override
    public void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);

        terrainModifier.onUpdate();

        if (!level().isClientSide()) {
            if (!terrainModifier.isBusy()) {
                // erst schräg nach oben versuchen
                if (!tryDigUpToNexus()) {
                    // sonst ggf. nach unten
                    tryDigDownToNexus();
                }
            }
        }
    }






    // Darf dieser Zombie-Typ überhaupt "buddeln"?
    // Darf dieser konkrete Zombie-Typ nach unten buddeln?
    private boolean canDigDown() {
        int tier = getTier();
        int flavour = getFlavour();

        // Nur die Varianten, die sowieso Blöcke zerstören können:
        if (tier == 1 && flavour == 0) return true; // T1, Var 0
        if (tier == 2 && flavour == 0) return true; // T2, Var 0
        if (isTar()) return true;
        if (isPigman()) return true;
        if (isBrute()) return true;

        return false;
    }

    @Override
    protected boolean canDestroyBlocksByDefault() {
        return canDigDown();
    }

    @Override
    public float getSelfDamage() {
        return selfDamage;
    }

    @Override
    public float getMaxSelfDamage() {
        return maxSelfDamage;
    }

    /**
     * Versucht, eine schräge Rampe nach oben in Richtung Nexus zu buddeln.
     * Kein Leitern-Bau – nur Blöcke zu AIR machen.
     *
     * @return true, wenn ein Buddel-Job gestartet wurde, sonst false
     */
    private boolean tryDigUpToNexus() {
        // Nur Zombies, die generell buddeln dürfen
        if (!canDigDown()) {
            return false;
        }

        if (terrainModifier.isBusy()) {
            return false;
        }

        NexusAccess nexus = getNexus();
        if (nexus == null) {
            return false;
        }

        Level world = level();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.blockPosition();

        // Vertikaler Abstand: Nexus über uns?
        int dyUp = nexusPos.getY() - mobPos.getY(); // positiv = Nexus höher
        if (dyUp <= 3) {
            // Nexus nicht deutlich höher -> hier keine Rampe bauen
            return false;
        }

        // Horizontale Richtung zum Nexus bestimmen
        double dx = (nexusPos.getX() + 0.5D) - this.getX();
        double dz = (nexusPos.getZ() + 0.5D) - this.getZ();

        Direction dir = Direction.getApproximateNearest(dx, 0.0D, dz);
        if (!dir.getAxis().isHorizontal()) {
            dir = this.getDirection();
        }

        // Optional: nicht rampen, wenn wir SEHR weit weg sind
        double horizDistSq = dx * dx + dz * dz;
        if (horizDistSq > 16.0D * 16.0D) {
            // zu weit weg, erstmal normal hinlaufen lassen
            return false;
        }

        // Wenn direkt vor/nach oben schon Luft ist, kann es sein, dass wir
        // in einem offenen Bereich stehen -> dann lieber nicht sinnlos Rampen spammen
        BlockPos forwardPos = mobPos.relative(dir);
        BlockPos forwardUpPos = forwardPos.above();
        boolean standingOnRamp = world.getBlockState(mobPos.below()).is(Blocks.COBBLESTONE);
        if (!standingOnRamp
                && world.getBlockState(forwardPos).isAir()
                && world.getBlockState(forwardUpPos).isAir()) {
            return false;
        }

        // Wie viele "Stufen" wollen wir bauen?
        // Grob: so viele wie vertikaler Abstand, aber gedeckelt
        int steps = Math.min(dyUp + 1, 16);
        steps = Math.max(3, steps);

        final Direction rampDir = dir;
        final int rampSteps = steps;

        terrainModifier.submitJob(mobPos, Notifiable.NONE, pos ->
                terrainBuilder.askBuildRampUp(pos, rampDir, rampSteps)
                        .filter(entry -> getEyePosition().distanceToSqr(PosUtils.center(entry.pos()))
                                <= TERRAIN_REACH_SQR)
        );

        return true;
    }


    /**
     * Versucht, einen Schacht nach unten in Richtung Nexus zu graben.
     * Kein Leitern-Bau – nur Blöcke zu AIR machen.
     */
    private void tryDigDownToNexus() {
        // Nur bestimmte Varianten dürfen buddeln
        if (!canDigDown()) {
            return;
        }

        // Wenn eh gerade ein Job läuft -> nichts tun
        if (terrainModifier.isBusy()) {
            return;
        }

        // Nexus bestimmen (über IHasNexus)
        NexusAccess nexus = getNexus();
        if (nexus == null) {
            return;
        }

        Level world = level();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.blockPosition();

        // Wir sind interessanter, wenn wir ÜBER dem Nexus stehen
        int dy = mobPos.getY() - nexusPos.getY(); // Achtung: diesmal MOB - NEXUS

        // Nur buddeln, wenn wir mindestens 3 Blöcke über dem Nexus sind
        if (dy <= 3) {
            return;
        }

        // -> Horizontaler Abstand ist uns erstmal egal
        // Wenn du willst, kannst du später noch ein Limit nachrüsten.

        // Wenn direkt unter uns schon Luft ist, stehen wir evtl. in einer Höhle
        // => dann lassen wir es (sonst buddeln sie sich durch die gesamte Map)
        BlockPos below = mobPos.below();
        if (world.getBlockState(below).isAir()) {
            return;
        }

        // Wie tief wollen wir maximal buddeln?
        int maxDepthToNexus = dy - 1;              // bissl über Nexus aufhören
        int depth = Math.min(maxDepthToNexus, 16); // erstmal max. 16 Blöcke

        if (depth <= 0) {
            return;
        }

        final int shaftDepth = depth;

        // Job an TerrainModifier übergeben
        terrainModifier.submitJob(mobPos, Notifiable.NONE, pos ->
                terrainBuilder.askDigShaftDown(pos, shaftDepth)
        );
    }





    public boolean isTar() {
        return isTar(this);
    }

    public boolean isPigman() {
        return getTier() == 2 && getFlavour() == 3;
    }

    @Override
    public boolean getBurnsInDay() {
        return isTar();
    }

    @Override
    protected Component getTypeName() {
        if (isTar()) {
            return Component.translatable(getType().getDescriptionId() + ".tar");
        }
        if (isPigman()) {
            return Component.translatable(getType().getDescriptionId() + ".pigman");
        }
        return super.getTypeName();
    }

    @Override
    protected void initTieredAttributes() {
        setBaseMovementSpeed(0.19F);
        setAttackStrength(4);

        if (getTier() == 1) {
            maxSelfDamage = 6;
            selfDamage = 3;
            flammability = 3;

            if (getFlavour() == 0) {
                getNavigatorNew().setCanDestroyBlocks(true);
            } else if (getFlavour() == 1) {
                setAttackStrength(6);
                setItemInHand(InteractionHand.MAIN_HAND, Items.WOODEN_SWORD.getDefaultInstance());
                setDropChance(EquipmentSlot.MAINHAND, 0.2F);
                getNavigatorNew().setCanDestroyBlocks(false);
            }
        } else if (getTier() == 2) {
            setBaseMovementSpeed(0.19F);
            if (getFlavour() == 0) {
                setAttackStrength(7);
                selfDamage = 4;
                maxSelfDamage = 12;
                flammability = 4;
                setItemSlot(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultInstance());
                setDropChance(EquipmentSlot.CHEST, 0.25F);
                getNavigatorNew().setCanDestroyBlocks(true);
            } else if (getFlavour() == 1) {
                setAttackStrength(10);
                selfDamage = 3;
                maxSelfDamage = 9;
                setItemInHand(InteractionHand.MAIN_HAND, Items.IRON_SWORD.getDefaultInstance());
                setDropChance(EquipmentSlot.MAINHAND, 0.25F);
                getNavigatorNew().setCanDestroyBlocks(false);
            }
        }

        if (isTar()) {
            setAttackStrength(5);
            selfDamage = 3;
            maxSelfDamage = 9;
            flammability = 1;
            getNavigatorNew().setCanDestroyBlocks(true);
        }

        if (isPigman()) {
            setBaseMovementSpeed(0.25F);
            setAttackStrength(8);
            setFireImmune(true);
            setItemInHand(InteractionHand.MAIN_HAND, Items.GOLDEN_SWORD.getDefaultInstance());
            setDropChance(EquipmentSlot.MAINHAND, 0.2F);
            getNavigatorNew().setCanDestroyBlocks(true);
        }

        if (isBrute()) {
            setBaseMovementSpeed(0.17F);
            setAttackStrength(18);
            selfDamage = 4;
            maxSelfDamage = 20;
            flammability = 4;
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            setDropChance(EquipmentSlot.MAINHAND, 0);
            getNavigatorNew().setCanDestroyBlocks(true);
        }
    }

    @Override
    public int getTextureId() {
        return switch(getTier()) {
            case 2 -> switch(getFlavour()) {
                case 2 -> TAR;
                case 3 -> ZOMBIE_PIGMAN;
                default -> (((int)(Math.abs(getUUID().getLeastSignificantBits()) % 2)) + 1) * 2; //2,4
            };
            case 3 -> BRUTE;
            default -> (int)(Math.abs(getUUID().getLeastSignificantBits()) % 2); // 0,1
        };
    }

    @Override
    protected void sunlightDamageTick() {
        if (isTar()) {
            igniteForSeconds(8);
        } else {
            super.sunlightDamageTick();
        }
    }

    @Override
    public void updateAnimation(boolean override) {

    }

    @Override
    public SoundEvent getAmbientSound() {
        if (super.getTier() == 3) {
            return getRandom().nextInt(3) == 0 ? InvSounds.ENTITY_BIG_ZOMBIE_AMBIENT : null;
        }

        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    private void spreadTarFire() {
        for (BlockPos pos : BlockPos.withinManhattan(blockPosition(), 2, 2, 2)) {
            var state = level().getBlockState(pos);
            if (state.isAir() || state.ignitedByLava()) {
                level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }

        List<Entity> entities = level().getEntities(this, getBoundingBox().inflate(1.5, 1.5, 1.5));
        for (int el = entities.size() - 1; el >= 0; el--) {
            // Refresh nearby entities for as long as the Tar Zombie burns
            // instead of applying one fixed eight-second ignition.
            entities.get(el).igniteForTicks(20);
        }
    }
}
