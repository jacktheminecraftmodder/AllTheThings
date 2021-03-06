package com.jacktheminecraftmodder.allm.content.containers;

import com.jacktheminecraftmodder.allm.Register;
import com.jacktheminecraftmodder.allm.content.Tileentities.ElectricAlloyFurnaceTile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IWorldPosCallable;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ElectricAlloyFurnaceContainer extends Container {

    public final ElectricAlloyFurnaceTile tileEntity;
    private final IWorldPosCallable canInteractWithCallable;

    /**
     * Logical-client-side constructor, called from {@link ContainerType#create(IContainerFactory)}
     * Calls the logical-server-side constructor with the TileEntity at the pos in the PacketBuffer
     */
    public ElectricAlloyFurnaceContainer(final int windowId, final PlayerInventory playerInventory, final PacketBuffer data) {
        this(windowId, playerInventory, getTileEntity(playerInventory, data));
    }

    public ElectricAlloyFurnaceContainer(final int windowId, final PlayerInventory playerInventory, final ElectricAlloyFurnaceTile tileEntity) {
        super(Register.ELECTRIC_ALLOY_FURNACE_CONTAINER.get(), windowId);
        this.tileEntity = tileEntity;
        this.canInteractWithCallable = IWorldPosCallable.of(tileEntity.getWorld(), tileEntity.getPos());

        // Add tracking for data (Syncs to client/updates value when it changes)
        this.trackInt(new FunctionalIntReferenceHolder(() -> tileEntity.smeltTimeLeft, v -> tileEntity.smeltTimeLeft = (short) v));
        this.trackInt(new FunctionalIntReferenceHolder(() -> tileEntity.maxSmeltTime, v -> tileEntity.maxSmeltTime = (short) v));

        // Add all the slots for the tileEntity's inventory and the playerInventory to this container

        // Tile inventory slot(s)
        this.addSlot(new SlotItemHandler(tileEntity.inventory, ElectricAlloyFurnaceTile.INPUT_SLOT, 56, 35));
        this.addSlot(new SlotItemHandler(tileEntity.inventory, ElectricAlloyFurnaceTile.OUTPUT_SLOT, 116, 35));

        final int playerInventoryStartX = 8;
        final int playerInventoryStartY = 84;
        final int slotSizePlus2 = 18; // slots are 16x16, plus 2 (for spacing/borders) is 18x18

        // Player Top Inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, 9 + (row * 9) + column, playerInventoryStartX + (column * slotSizePlus2), playerInventoryStartY + (row * slotSizePlus2)));
            }
        }

        final int playerHotbarY = playerInventoryStartY + slotSizePlus2 * 3 + 4;
        // Player Hotbar slots
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, playerInventoryStartX + (column * slotSizePlus2), playerHotbarY));
        }
    }

    private static ElectricAlloyFurnaceTile getTileEntity(final PlayerInventory playerInventory, final PacketBuffer data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null!");
        Objects.requireNonNull(data, "data cannot be null!");
        final TileEntity tileAtPos = playerInventory.player.world.getTileEntity(data.readBlockPos());
        if (tileAtPos instanceof ElectricAlloyFurnaceTile)
            return (ElectricAlloyFurnaceTile) tileAtPos;
        throw new IllegalStateException("Tile entity is not correct! " + tileAtPos);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(final PlayerEntity player, final int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        final Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            final ItemStack slotStack = slot.getStack();
            returnStack = slotStack.copy();

            final int containerSlots = this.inventorySlots.size() - player.inventory.mainInventory.size();
            if (index < containerSlots) {
                if (!mergeItemStack(slotStack, containerSlots, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(slotStack, 0, containerSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return returnStack;
    }

    @Override
    public boolean canInteractWith(@Nonnull final PlayerEntity player) {
        return isWithinUsableDistance(canInteractWithCallable, player, Register.ELECTRIC_ALLOY_FURNACE.get());
    }

}
