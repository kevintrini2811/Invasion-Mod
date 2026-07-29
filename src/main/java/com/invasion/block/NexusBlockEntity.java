package com.invasion.block;

import java.util.UUID;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import com.invasion.block.container.NexusScreenHandler;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.Nexus;
import com.invasion.nexus.WorldNexusStorage;

public class NexusBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, BeaconBeamOwner {
    private static final int[] SLOTS = {0, 1};
    private static final List<BeaconBeamOwner.Section> BEAM_SECTIONS =
            List.of(new BeaconBeamOwner.Section(0xFFFFFFFF));

    private UUID nexusId = UUID.randomUUID();
    private boolean beamActive;
    @Nullable
    private Nexus nexus;

    public NexusBlockEntity(BlockPos pos, BlockState state) {
        super(InvBlockEntities.NEXUS, pos, state);
    }

    public NexusAccess getNexus() {
        if (nexus == null && getLevel() instanceof ServerLevel sw) {
            nexus = WorldNexusStorage.of(sw).getOrCreate(nexusId, getBlockPos());
        }
        return nexus;
    }
    public UUID getNexusId() {
        return nexusId;
    }

    public boolean toggleBeam() {
        beamActive = !beamActive;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return beamActive;
    }

    @Override
    public List<BeaconBeamOwner.Section> getBeamSections() {
        return beamActive ? BEAM_SECTIONS : List.of();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        if (nexus != null && nexus.getWorld() != world) {
            nexus = null;
        }
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        if (getNexus() != null) {
            nexus.getHeldItems().setItem(i, stack);
        }
    }

    @Override
    public ItemStack getItem(int i) {
        return getNexus() != null ? nexus.getHeldItems().getItem(i) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return getNexus() != null ? nexus.getHeldItems().removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player entityplayer) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return getNexus() != null && nexus.getHeldItems().isEmpty();
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return getNexus() != null ? nexus.getHeldItems().removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void clearContent() {
        if (getNexus() != null) {
            nexus.getHeldItems().clearContent();
        }
    }

    @Override
    public int getContainerSize() {
        return getNexus() != null ? nexus.getHeldItems().getContainerSize() : 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public void tick(ServerLevel world, BlockPos pos, BlockState state) {
        // Register a newly placed Nexus without requiring the player to open
        // or right-click it before commands can find it.
        getNexus();
    }

    public void discard() {
        if (getLevel() instanceof ServerLevel sw) {
            WorldNexusStorage.of(sw).destroyNexus(nexusId);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        if (getNexus() == null) {
            return null;
        }
        return new NexusScreenHandler(syncId, playerInventory, this, nexus.getProperties(), ContainerLevelAccess.create(player.level(), getBlockPos()));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void loadAdditional(ValueInput compound) {
        super.loadAdditional(compound);
        nexusId = compound.read("nexusId", net.minecraft.core.UUIDUtil.CODEC).orElse(UUID.randomUUID());
        beamActive = compound.getBooleanOr("beamActive", false);
        nexus = null;
    }

    @Override
    protected void saveAdditional(ValueOutput compound) {
        super.saveAdditional(compound);
        compound.store("nexusId", net.minecraft.core.UUIDUtil.CODEC, nexusId);
        compound.putBoolean("beamActive", beamActive);
    }
}
