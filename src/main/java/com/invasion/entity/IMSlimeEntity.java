package com.invasion.entity;

import java.util.ArrayList;
import java.util.List;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** A Nexus-bound slime that absorbs loose items and releases them on death. */
public final class IMSlimeEntity extends Slime
        implements Combatant<Slime>, EntityConstruct.BuildableMob {
    private static final Codec<List<ItemStack>> ABSORBED_ITEMS_CODEC =
            ItemStack.CODEC.listOf();

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private final List<ItemStack> absorbedItems = new ArrayList<>();

    public IMSlimeEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Slime asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMSlime-T1";
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        setSize(1 << getRandom().nextInt(3), true);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!ItemSearchScheduler.shouldSearch(this)) {
            return;
        }

        double pickupRadius = Math.max(1.25D, getSize() * 0.65D);
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(pickupRadius),
                candidate -> !candidate.hasPickUpDelay()
                        && !candidate.getItem().isEmpty())) {
            ItemStack stack = item.getItem().copy();
            take(item, stack.getCount());
            absorbedItems.add(stack);
            item.discard();
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        for (ItemStack stack : absorbedItems) {
            spawnAtLocation(level, stack);
        }
        absorbedItems.clear();
    }

    @Override
    protected void setUpSplitCube(
            AbstractCubeMob cubeMob, int halfSize, float xd, float zd) {
        super.setUpSplitCube(cubeMob, halfSize, xd, zd);
        if (cubeMob instanceof IMSlimeEntity slime) {
            slime.absorbedItems.clear();
            slime.setNexus(getNexus());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        nexus.writeNbt(output);
        output.store("AbsorbedItems", ABSORBED_ITEMS_CODEC, absorbedItems);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nexus.readNbt(input);
        absorbedItems.clear();
        input.read("AbsorbedItems", ABSORBED_ITEMS_CODEC)
                .ifPresent(absorbedItems::addAll);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus() && super.removeWhenFarAway(distanceSquared);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }
}

