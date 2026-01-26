package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom inventory system for AI villagers
 */
public class AgentInventory {
    private static final int INVENTORY_SIZE = 27; // 3 rows like a chest
    private List<ItemStack> items;
    
    public AgentInventory() {
        this.items = new ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items.add(ItemStack.EMPTY);
        }
    }
    
    public boolean addItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Try to merge with existing stacks first
        for (int i = 0; i < items.size(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && existing.sameItem(stack)) {
                int maxStack = Math.min(stack.getMaxStackSize(), existing.getMaxStackSize());
                int space = maxStack - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return true;
                }
            }
        }
        
        // Find empty slot
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                return true;
            }
        }
        
        return false; // Inventory full
    }
    
    public boolean removeItem(ItemStack stack, int count) {
        int remaining = count;
        for (int i = 0; i < items.size(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && existing.sameItem(stack)) {
                int toRemove = Math.min(remaining, existing.getCount());
                existing.shrink(toRemove);
                remaining -= toRemove;
                if (existing.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
                if (remaining <= 0) return true;
            }
        }
        return remaining == 0;
    }
    
    public int countItem(ItemStack stack) {
        int count = 0;
        for (ItemStack item : items) {
            if (!item.isEmpty() && item.sameItem(stack)) {
                count += item.getCount();
            }
        }
        return count;
    }
    
    public boolean hasItem(ItemStack stack, int count) {
        return countItem(stack) >= count;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < items.size()) {
            return items.get(slot);
        }
        return ItemStack.EMPTY;
    }
    
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
        }
    }
    
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
    
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT itemsNBT = new ListNBT();
        
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                CompoundNBT itemNBT = new CompoundNBT();
                itemNBT.putByte("Slot", (byte) i);
                items.get(i).save(itemNBT);
                itemsNBT.add(itemNBT);
            }
        }
        
        nbt.put("Items", itemsNBT);
        return nbt;
    }
    
    public void deserializeNBT(CompoundNBT nbt) {
        ListNBT itemsNBT = nbt.getList("Items", 10);

        // Clear inventory
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        // Load items
        for (int i = 0; i < itemsNBT.size(); i++) {
            CompoundNBT itemNBT = itemsNBT.getCompound(i);
            int slot = itemNBT.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.size()) {
                items.set(slot, ItemStack.of(itemNBT));
            }
        }
    }

    /**
     * Get a Minecraft IInventory wrapper for GUI display
     */
    public IInventory getMinecraftInventory() {
        return new IInventory() {
            @Override
            public int getContainerSize() {
                return INVENTORY_SIZE;
            }

            @Override
            public boolean isEmpty() {
                return AgentInventory.this.isEmpty();
            }

            @Override
            public ItemStack getItem(int slot) {
                return getStackInSlot(slot);
            }

            @Override
            public ItemStack removeItem(int slot, int count) {
                ItemStack stack = getStackInSlot(slot);
                if (stack.isEmpty()) return ItemStack.EMPTY;

                ItemStack result;
                if (stack.getCount() <= count) {
                    result = stack.copy();
                    setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    result = stack.split(count);
                    if (stack.isEmpty()) {
                        setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
                setChanged();
                return result;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                ItemStack stack = getStackInSlot(slot);
                setStackInSlot(slot, ItemStack.EMPTY);
                return stack;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                setStackInSlot(slot, stack);
                setChanged();
            }

            @Override
            public void setChanged() {
                // Mark as changed
            }

            @Override
            public boolean stillValid(PlayerEntity player) {
                return true;
            }

            @Override
            public void clearContent() {
                for (int i = 0; i < items.size(); i++) {
                    items.set(i, ItemStack.EMPTY);
                }
            }
        };
    }
}

