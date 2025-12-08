package com.invasion.entity;

import java.util.List;

import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainModifier;
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
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.TerrainBuilder;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.nexus.NexusAccess;
import net.minecraft.util.math.Direction;


public class EntityIMZombie extends AbstractIMZombieEntity {
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
    private final TerrainModifier terrainModifier = new TerrainModifier(this, 32.0F);
    private final TerrainBuilder terrainBuilder = new TerrainBuilder(this, 1.0F);
    private static DefaultAttributeContainer.Builder createBaseAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.19F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    public static DefaultAttributeContainer.Builder createTierT1V0Attributes() {
        return createBaseAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    public static DefaultAttributeContainer.Builder createTierT1V1Attributes() {
        return createBaseAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    public static DefaultAttributeContainer.Builder createTierT2V0Attributes() {
        return createBaseAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0);
    }

    public static DefaultAttributeContainer.Builder createTierT2V1Attributes() {
        return createBaseAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0);
    }

    /**
     * Tar Zombie
     */
    public static DefaultAttributeContainer.Builder createTierT2V2ttributes() {
        return createBaseAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
    }
    /**
     * Zombie Pigman
     */
    public static DefaultAttributeContainer.Builder createTierT2V3ttributes() {
        return createBaseAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0);
    }
    /**
     * Zombie Brute
     */
    public static DefaultAttributeContainer.Builder createTierT3V0Attributes() {
        return createBaseAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 18.0);
    }

    public EntityIMZombie(EntityType<EntityIMZombie> type, World world) {
        super(type, world, 2F);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new PredicatedGoal(new SwimGoal(this), () -> getTier() != 2 || getFlavour() != 2));
        goalSelector.add(0, new MineBlockGoal(this));
        goalSelector.add(1, new AttackNexusGoal<>(this));
        goalSelector.add(3, new ProvideSupportGoal(this, 4, true));
        goalSelector.add(3, new PredicatedGoal(new SprintGoal<>(this), () -> getTier() == 3));
        goalSelector.add(4, new PredicatedGoal(new StoopGoal(this), () -> getTier() == 3));
        goalSelector.add(5, new GoToNexusGoal(this));
        goalSelector.add(6, new MobMeleeAttackGoal(this, 1.3F, false));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(8, new LookAtEntityGoal(this, IMCreeperEntity.class, 12));
        goalSelector.add(8, new LookAroundGoal(this));

        targetSelector.add(0, new RetaliateGoal(this));
        targetSelector.add(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PlayerEntity.class, this::getSenseRange, false), () -> getTier() != 3));
        targetSelector.add(2, new CustomRangeActiveTargetGoal<>(this, PlayerEntity.class, this::getAggroRange, true));
        targetSelector.add(3, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F), () -> getTier() != 3 && NoNexusPathGoal.isLostPathToNexus(this)));
        targetSelector.add(4, new CustomRangeActiveTargetGoal<>(this, MerchantEntity.class, this::getAggroRange, true));
        targetSelector.add(4, new CustomRangeActiveTargetGoal<>(this, IronGolemEntity.class, this::getAggroRange, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && flammability >= 20 && isOnFire()) {
            doFireball();
        }
    }
    @Override
    public void mobTick() {
        super.mobTick();

        terrainModifier.onUpdate();

        if (!getWorld().isClient) {
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

        World world = getWorld();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.getBlockPos();

        // Vertikaler Abstand: Nexus über uns?
        int dyUp = nexusPos.getY() - mobPos.getY(); // positiv = Nexus höher
        if (dyUp <= 3) {
            // Nexus nicht deutlich höher -> hier keine Rampe bauen
            return false;
        }

        // Horizontale Richtung zum Nexus bestimmen
        double dx = (nexusPos.getX() + 0.5D) - this.getX();
        double dz = (nexusPos.getZ() + 0.5D) - this.getZ();

        Direction dir = Direction.getFacing(dx, 0.0D, dz);
        if (!dir.getAxis().isHorizontal()) {
            dir = this.getHorizontalFacing();
        }

        // Optional: nicht rampen, wenn wir SEHR weit weg sind
        double horizDistSq = dx * dx + dz * dz;
        if (horizDistSq > 16.0D * 16.0D) {
            // zu weit weg, erstmal normal hinlaufen lassen
            return false;
        }

        // Wenn direkt vor/nach oben schon Luft ist, kann es sein, dass wir
        // in einem offenen Bereich stehen -> dann lieber nicht sinnlos Rampen spammen
        BlockPos forwardPos = mobPos.offset(dir);
        BlockPos forwardUpPos = forwardPos.up();
        if (world.getBlockState(forwardPos).isAir() && world.getBlockState(forwardUpPos).isAir()) {
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

        World world = getWorld();
        BlockPos nexusPos = nexus.getOrigin();
        BlockPos mobPos = this.getBlockPos();

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
        BlockPos below = mobPos.down();
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
    protected Text getDefaultName() {
        if (isTar()) {
            return Text.translatable(getType().getUntranslatedName() + ".tar");
        }
        if (isPigman()) {
            return Text.translatable(getType().getUntranslatedName() + ".pigman");
        }
        return super.getDefaultName();
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
                setStackInHand(Hand.MAIN_HAND, Items.WOODEN_SWORD.getDefaultStack());
                setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.2F);
                getNavigatorNew().setCanDestroyBlocks(false);
            }
        } else if (getTier() == 2) {
            setBaseMovementSpeed(0.19F);
            if (getFlavour() == 0) {
                setAttackStrength(7);
                selfDamage = 4;
                maxSelfDamage = 12;
                flammability = 4;
                equipStack(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultStack());
                setEquipmentDropChance(EquipmentSlot.CHEST, 0.25F);
                getNavigatorNew().setCanDestroyBlocks(true);
            } else if (getFlavour() == 1) {
                setAttackStrength(10);
                selfDamage = 3;
                maxSelfDamage = 9;
                setStackInHand(Hand.MAIN_HAND, Items.IRON_SWORD.getDefaultStack());
                setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.25F);
                getNavigatorNew().setCanDestroyBlocks(false);
            }
        }

        if (isTar()) {
            setAttackStrength(5);
            selfDamage = 3;
            maxSelfDamage = 9;
            flammability = 30;
            getNavigatorNew().setCanDestroyBlocks(true);
        }

        if (isPigman()) {
            setBaseMovementSpeed(0.25F);
            setAttackStrength(8);
            setFireImmune(true);
            setStackInHand(Hand.MAIN_HAND, Items.GOLDEN_SWORD.getDefaultStack());
            setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.2F);
            getNavigatorNew().setCanDestroyBlocks(true);
        }

        if (isBrute()) {
            setBaseMovementSpeed(0.17F);
            setAttackStrength(18);
            selfDamage = 4;
            maxSelfDamage = 20;
            flammability = 4;
            equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            setEquipmentDropChance(EquipmentSlot.MAINHAND, 0);
            getNavigatorNew().setCanDestroyBlocks(true);
        }
    }

    @Override
    public int getTextureId() {
        return switch(getTier()) {
            case 2 -> switch(getFlavour()) {
                case 2 -> TAR;
                case 3 -> ZOMBIE_PIGMAN;
                default -> (((int)(Math.abs(getUuid().getLeastSignificantBits()) % 2)) + 1) * 2; //2,4
            };
            case 3 -> BRUTE;
            default -> (int)(Math.abs(getUuid().getLeastSignificantBits()) % 2); // 0,1
        };
    }

    @Override
    protected void sunlightDamageTick() {
        if (isTar()) {
            damage(getDamageSources().generic(), 3);
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

        return SoundEvents.ENTITY_ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ZOMBIE_DEATH;
    }

    private void doFireball() {
        for (BlockPos pos : BlockPos.iterateOutwards(getBlockPos(), 2, 2, 2)) {
            if (getWorld().isAir(pos) && getWorld().getBlockState(pos.down()).isBurnable()) {
                getWorld().setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }

        List<Entity> entities = getWorld().getOtherEntities(this, getBoundingBox().expand(1.5, 1.5, 1.5));
        for (int el = entities.size() - 1; el >= 0; el--) {
            entities.get(el).setFireTicks(8);
        }
        damage(getDamageSources().explosion(this, this), 500);
    }
}
