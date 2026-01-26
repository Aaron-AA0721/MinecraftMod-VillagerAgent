package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.item.Items;

import java.util.*;

/**
 * Registry of all hardcoded crafting recipes
 * Recipes are organized by profession
 */
public class RecipeRegistry {
    private static final List<CraftingRecipe> allRecipes = new ArrayList<>();
    private static final Map<String, List<CraftingRecipe>> professionRecipes = new HashMap<>();

    static {
        initializeRecipes();
        assignRecipesToProfessions();
    }

    private static void initializeRecipes() {
        // 2 planks -> 4 sticks (no workstation needed)
        CraftingRecipe stickRecipe = new CraftingRecipe("Sticks from Planks", CraftingRecipe.WorkstationType.NONE);
        stickRecipe.addInput(Items.OAK_PLANKS, 2);
        stickRecipe.addOutput(Items.STICK, 4);
        allRecipes.add(stickRecipe);

        // 1 stick + 2 iron ingots -> iron sword (crafting table)
        CraftingRecipe ironSwordRecipe = new CraftingRecipe("Iron Sword", CraftingRecipe.WorkstationType.CRAFTING_TABLE);
        ironSwordRecipe.addInput(Items.STICK, 1);
        ironSwordRecipe.addInput(Items.IRON_INGOT, 2);
        ironSwordRecipe.addOutput(Items.IRON_SWORD, 1);
        allRecipes.add(ironSwordRecipe);

        // 1 stick + 3 iron ingots -> iron pickaxe (crafting table)
        CraftingRecipe ironPickaxeRecipe = new CraftingRecipe("Iron Pickaxe", CraftingRecipe.WorkstationType.CRAFTING_TABLE);
        ironPickaxeRecipe.addInput(Items.STICK, 1);
        ironPickaxeRecipe.addInput(Items.IRON_INGOT, 3);
        ironPickaxeRecipe.addOutput(Items.IRON_PICKAXE, 1);
        allRecipes.add(ironPickaxeRecipe);

        // 1 stick + 3 iron ingots -> iron axe (crafting table)
        CraftingRecipe ironAxeRecipe = new CraftingRecipe("Iron Axe", CraftingRecipe.WorkstationType.CRAFTING_TABLE);
        ironAxeRecipe.addInput(Items.STICK, 1);
        ironAxeRecipe.addInput(Items.IRON_INGOT, 3);
        ironAxeRecipe.addOutput(Items.IRON_AXE, 1);
        allRecipes.add(ironAxeRecipe);

        // Iron ore -> iron ingot (furnace)
        CraftingRecipe ironIngotRecipe = new CraftingRecipe("Iron Ingot from Ore", CraftingRecipe.WorkstationType.FURNACE);
        ironIngotRecipe.addInput(Items.IRON_ORE, 1);
        ironIngotRecipe.addOutput(Items.IRON_INGOT, 1);
        allRecipes.add(ironIngotRecipe);
    }

    private static void assignRecipesToProfessions() {
        // Weaponsmith recipes
        List<CraftingRecipe> weaponsmithRecipes = new ArrayList<>();
        weaponsmithRecipes.add(getRecipe("Iron Sword"));
        weaponsmithRecipes.add(getRecipe("Iron Axe"));
        weaponsmithRecipes.add(getRecipe("Iron Ingot from Ore"));
        weaponsmithRecipes.add(getRecipe("Sticks from Planks"));
        professionRecipes.put("weaponsmith", weaponsmithRecipes);

        // Toolsmith recipes
        List<CraftingRecipe> toolsmithRecipes = new ArrayList<>();
        toolsmithRecipes.add(getRecipe("Iron Pickaxe"));
        toolsmithRecipes.add(getRecipe("Iron Axe"));
        toolsmithRecipes.add(getRecipe("Iron Ingot from Ore"));
        toolsmithRecipes.add(getRecipe("Sticks from Planks"));
        professionRecipes.put("toolsmith", toolsmithRecipes);

        // Farmer recipes (none for now, will be added later)
        professionRecipes.put("farmer", new ArrayList<>());

        // Default recipes for other professions
        List<CraftingRecipe> defaultRecipes = new ArrayList<>();
        defaultRecipes.add(getRecipe("Sticks from Planks"));
        professionRecipes.put("default", defaultRecipes);
    }
    
    public static List<CraftingRecipe> getAllRecipes() {
        return new ArrayList<>(allRecipes);
    }

    public static CraftingRecipe getRecipe(String name) {
        for (CraftingRecipe recipe : allRecipes) {
            if (recipe.getName().equalsIgnoreCase(name)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Get recipes known by a specific profession
     */
    public static List<CraftingRecipe> getRecipesForProfession(String profession) {
        return professionRecipes.getOrDefault(profession.toLowerCase(), professionRecipes.get("default"));
    }

    /**
     * Get available recipes for a profession that the villager can currently craft
     */
    public static List<CraftingRecipe> getAvailableRecipesForProfession(String profession, AgentInventory inventory) {
        List<CraftingRecipe> available = new ArrayList<>();
        List<CraftingRecipe> profRecipes = getRecipesForProfession(profession);

        for (CraftingRecipe recipe : profRecipes) {
            if (recipe.canCraft(inventory)) {
                available.add(recipe);
            }
        }
        return available;
    }

    public static List<CraftingRecipe> getAvailableRecipes(AgentInventory inventory) {
        List<CraftingRecipe> available = new ArrayList<>();
        for (CraftingRecipe recipe : allRecipes) {
            if (recipe.canCraft(inventory)) {
                available.add(recipe);
            }
        }
        return available;
    }
}

