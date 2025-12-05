package com.invasion.block;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.container.NexusScreenHandler;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.Nexus;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class NexusBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {
    private static final int[] SLOTS = {0, 1};

    private UUID nexusId = UUID.randomUUID();
    @Nullable
    private Nexus nexus;

    public NexusBlockEntity(BlockPos pos, BlockState state) {
        super(InvBlockEntities.NEXUS, pos, state);
    }

    public NexusAccess getNexus() {
        if (nexus == null && getWorld() instanceof ServerWorld sw) {
            nexus = WorldNexusStorage.of(sw).getOrCreate(nexusId, getPos());
        }
        return nexus;
    }
    public UUID getNexusId() {
        return nexusId;
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (nexus != null && nexus.getWorld() != world) {
            nexus = null;
        }
    }

    @Override
    public void setStack(int i, ItemStack stack) {
        if (getNexus() != null) {
            nexus.getHeldItems().setStack(i, stack);
        }
    }

    @Override
    public ItemStack getStack(int i) {
        return getNexus() != null ? nexus.getHeldItems().getStack(i) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return getNexus() != null ? nexus.getHeldItems().removeStack(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity entityplayer) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return getNexus() != null && nexus.getHeldItems().isEmpty();
    }

    @Override
    public ItemStack removeStack(int slot) {
        return getNexus() != null ? nexus.getHeldItems().removeStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public void clear() {
        if (getNexus() != null) {
            nexus.getHeldItems().clear();
        }
    }

    @Override
    public int size() {
        return getNexus() != null ? nexus.getHeldItems().size() : 0;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public void tick(ServerWorld world, BlockPos pos, BlockState state) {

    }

    public void discard() {
        if (getWorld() instanceof ServerWorld sw) {
            WorldNexusStorage.of(sw).destroyNexus(nexusId);
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (getNexus() == null) {
            return null;
        }
        return new NexusScreenHandler(syncId, playerInventory, this, nexus.getProperties(), ScreenHandlerContext.create(player.getWorld(), getPos()));
    }

    @Override
    public Text getDisplayName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    protected void readNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(compound, lookup);
        nexusId = compound.getUuid("nexusId");
        nexus = null;
    }

    @Override
    public void writeNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(compound, lookup);
        compound.putUuid("nexusId", nexusId);
    }
}