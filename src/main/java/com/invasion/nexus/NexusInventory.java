package com.invasion.nexus;

import com.invasion.item.InvItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class NexusInventory extends SimpleContainer {
    static final int MAX_FLUX_GENERATION_TIME = 3000;
    static final int MAX_TRAP_COOK_TIME = 1200;

    private int cookTime;
    private int accumulatedFlux;
    private Runnable changeListener = () -> {};

    public NexusInventory() {
        super(2);
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        changeListener.run();
    }

    public int getFluxProgress() {
        return accumulatedFlux;
    }

    public void setFlugProgress(int time) {
        accumulatedFlux = time;
    }

    public int getCookTime() {
        return cookTime;
    }

    public void setCookTime(int time) {
        cookTime = time;
    }

    public void tick(NexusAccess nexus) {
        tickCookTime(nexus, getItem(0), getItem(1));
    }

    public void generateFlux(int increment) {
        accumulatedFlux += increment;
        if (accumulatedFlux >= MAX_FLUX_GENERATION_TIME) {
            ItemStack currentGeneratedItem = getItem(1);
            if (currentGeneratedItem.isEmpty()) {
                setItem(1, InvItems.RIFT_FLUX.getDefaultInstance());
                accumulatedFlux -= MAX_FLUX_GENERATION_TIME;
            } else if (currentGeneratedItem.is(InvItems.RIFT_FLUX)) {
                currentGeneratedItem.grow(1);
                accumulatedFlux -= MAX_FLUX_GENERATION_TIME;
            }
        }
    }

    private void tickCookTime(NexusAccess nexus, ItemStack firstStack, ItemStack secondStack) {
        if (!firstStack.isEmpty()) {
            if (firstStack.is(InvItems.EMPTY_TRAP)) {
                if (cookTime < MAX_TRAP_COOK_TIME) {
                    cookTime += nexus.getMode() == Mode.STOPPED ? 1 : 9;
                } else {
                    if (secondStack.isEmpty()) {
                        setItem(1, InvItems.RIFT_TRAP.getDefaultInstance());
                        firstStack.shrink(1);
                        cookTime = 0;
                    } else if (secondStack.is(InvItems.RIFT_TRAP) && secondStack.getCount() < secondStack.getMaxStackSize()) {
                        secondStack.grow(1);
                        firstStack.shrink(1);
                        cookTime = 0;
                    }
                }
            } else if (firstStack.is(InvItems.RIFT_FLUX)) {
                if (cookTime < MAX_TRAP_COOK_TIME && nexus.getLevel() >= 10) {
                    cookTime += 5;
                }

                if (cookTime >= MAX_TRAP_COOK_TIME) {
                    if (secondStack.isEmpty()) {
                        setItem(1, InvItems.STRONG_NEXUS_CATALYST.getDefaultInstance());
                        firstStack.shrink(1);
                        cookTime = 0;
                    }
                }
            }
        } else {
            cookTime = 0;
        }
    }

    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        accumulatedFlux = compound.getInt("accumulatedFlux");
        cookTime = compound.getInt("cookTime");
        ItemStack.OPTIONAL_CODEC.listOf()
                .parse(lookup.createSerializationContext(NbtOps.INSTANCE), compound.get("Items"))
                .result()
                .ifPresent(items -> {
                    clearContent();
                    for (int i = 0; i < Math.min(items.size(), getContainerSize()); i++) {
                        setItem(i, items.get(i));
                    }
                });
    }

    public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        compound.putInt("accumulatedFlux", accumulatedFlux);
        compound.putInt("cookTime", cookTime);
        ItemStack.OPTIONAL_CODEC.listOf()
                .encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), getItems())
                .result()
                .ifPresent(tag -> compound.put("Items", tag));
        return compound;
    }
}
