package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a crafting recipe with inputs and outputs
 */
public class CraftingRecipe {
    public enum WorkstationType {
        NONE,           // No workstation needed
        CRAFTING_TABLE, // Crafting table
        FURNACE,        // Furnace
        SMITHING_TABLE  // Smithing table
    }

    private String name;
    private Map<Item, Integer> inputs; // Item -> quantity needed
    private Map<Item, Integer> outputs; // Item -> quantity produced
    private WorkstationType workstationType;

    public CraftingRecipe(String name) {
        this(name, WorkstationType.NONE);
    }

    public CraftingRecipe(String name, WorkstationType workstationType) {
        this.name = name;
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
        this.workstationType = workstationType;
    }
    
    public void addInput(Item item, int quantity) {
        inputs.put(item, quantity);
    }
    
    public void addOutput(Item item, int quantity) {
        outputs.put(item, quantity);
    }
    
    public String getName() { return name; }
    public Map<Item, Integer> getInputs() { return inputs; }
    public Map<Item, Integer> getOutputs() { return outputs; }
    public WorkstationType getWorkstationType() { return workstationType; }
    
    /**
     * Check if inventory has all required inputs
     */
    public boolean canCraft(AgentInventory inventory) {
        for (Map.Entry<Item, Integer> input : inputs.entrySet()) {
            ItemStack stack = new ItemStack(input.getKey());
            if (inventory.countItem(stack) < input.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Perform the crafting operation
     */
    public boolean craft(AgentInventory inventory) {
        if (!canCraft(inventory)) {
            return false;
        }
        
        // Remove inputs
        for (Map.Entry<Item, Integer> input : inputs.entrySet()) {
            ItemStack stack = new ItemStack(input.getKey());
            inventory.removeItem(stack, input.getValue());
        }
        
        // Add outputs
        for (Map.Entry<Item, Integer> output : outputs.entrySet()) {
            ItemStack stack = new ItemStack(output.getKey(), output.getValue());
            inventory.addItem(stack);
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

