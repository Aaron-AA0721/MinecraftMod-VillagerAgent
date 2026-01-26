package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles crafting actions for villagers
 * LLM decides what to craft, this class verifies and executes
 */
public class CraftingAction {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Execute a crafting action with recipe verification
     * @param agent The villager agent
     * @param recipeName The name of the recipe to craft
     * @param craftingTablePos The position of the crafting table (optional)
     * @return true if crafting was successful
     */
    public static boolean executeCraft(VillagerAgentData agent, String recipeName, BlockPos craftingTablePos) {
        // Get the recipe from registry
        CraftingRecipe recipe = RecipeRegistry.getRecipe(recipeName);
        
        if (recipe == null) {
            LOGGER.warn("Recipe not found: " + recipeName);
            agent.addMemory("Tried to craft unknown recipe: " + recipeName);
            return false;
        }
        
        // Verify the villager has the required items
        if (!recipe.canCraft(agent.getInventory())) {
            LOGGER.warn("Cannot craft " + recipeName + " - missing ingredients");
            agent.addMemory("Cannot craft " + recipeName + " - missing ingredients");
            return false;
        }
        
        // Perform the crafting
        boolean success = recipe.craft(agent.getInventory());
        
        if (success) {
            LOGGER.info("Villager " + agent.getName() + " crafted: " + recipeName);
            agent.addMemory("Successfully crafted: " + recipeName);
            agent.setCurrentActivity("crafting");
        } else {
            LOGGER.warn("Crafting failed for: " + recipeName);
            agent.addMemory("Crafting failed: " + recipeName);
        }
        
        return success;
    }
    
    /**
     * Get a list of recipes the villager can currently craft
     */
    public static java.util.List<CraftingRecipe> getAvailableRecipes(VillagerAgentData agent) {
        return RecipeRegistry.getAvailableRecipes(agent.getInventory());
    }
    
    /**
     * Get a description of available recipes for LLM
     */
    public static String getAvailableRecipesDescription(VillagerAgentData agent) {
        java.util.List<CraftingRecipe> available = getAvailableRecipes(agent);
        
        if (available.isEmpty()) {
            return "No recipes available with current inventory.";
        }
        
        StringBuilder sb = new StringBuilder("Available recipes:\n");
        for (CraftingRecipe recipe : available) {
            sb.append("- ").append(recipe.getName()).append("\n");
        }
        return sb.toString();
    }
}

