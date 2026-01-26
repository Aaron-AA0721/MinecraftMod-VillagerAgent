package com.github.AaronAA0721.villageragent.ai;

import java.util.List;
import java.util.UUID;

/**
 * Request sent to LLM asking what action the villager should take next
 * Contains current state: inventory, observations, and goals
 */
public class ActionRequest {
    private UUID villagerId;
    private String villagerName;
    private String profession;
    private String personality;
    
    // Current state
    private List<String> inventoryItems;      // Current inventory
    private List<String> recentObservations;  // What villager sees
    private List<String> goals;               // Current goals
    private List<String> memories;            // Recent memories
    
    // Available options
    private List<String> availableRecipes;    // Recipes villager can craft
    private List<String> availableActions;    // Possible actions
    
    public ActionRequest(UUID villagerId, String villagerName, String profession, String personality) {
        this.villagerId = villagerId;
        this.villagerName = villagerName;
        this.profession = profession;
        this.personality = personality;
    }
    
    // Getters and setters
    public UUID getVillagerId() { return villagerId; }
    public String getVillagerName() { return villagerName; }
    public String getProfession() { return profession; }
    public String getPersonality() { return personality; }
    
    public List<String> getInventoryItems() { return inventoryItems; }
    public void setInventoryItems(List<String> items) { this.inventoryItems = items; }
    
    public List<String> getRecentObservations() { return recentObservations; }
    public void setRecentObservations(List<String> observations) { this.recentObservations = observations; }
    
    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = goals; }
    
    public List<String> getMemories() { return memories; }
    public void setMemories(List<String> memories) { this.memories = memories; }
    
    public List<String> getAvailableRecipes() { return availableRecipes; }
    public void setAvailableRecipes(List<String> recipes) { this.availableRecipes = recipes; }
    
    public List<String> getAvailableActions() { return availableActions; }
    public void setAvailableActions(List<String> actions) { this.availableActions = actions; }
    
    /**
     * Build a prompt for the LLM based on this request
     */
    public String buildPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(villagerName).append(", a ").append(profession).append(" villager.\n");
        prompt.append("Personality: ").append(personality).append("\n\n");
        
        prompt.append("Current Inventory:\n");
        if (inventoryItems != null && !inventoryItems.isEmpty()) {
            for (String item : inventoryItems) {
                prompt.append("- ").append(item).append("\n");
            }
        } else {
            prompt.append("- Empty\n");
        }
        
        prompt.append("\nWhat you see around you:\n");
        if (recentObservations != null && !recentObservations.isEmpty()) {
            for (String obs : recentObservations) {
                prompt.append("- ").append(obs).append("\n");
            }
        } else {
            prompt.append("- Nothing special\n");
        }
        
        prompt.append("\nYour goals:\n");
        if (goals != null && !goals.isEmpty()) {
            for (String goal : goals) {
                prompt.append("- ").append(goal).append("\n");
            }
        } else {
            prompt.append("- No specific goals\n");
        }
        
        prompt.append("\nRecipes you know:\n");
        if (availableRecipes != null && !availableRecipes.isEmpty()) {
            for (String recipe : availableRecipes) {
                prompt.append("- ").append(recipe).append("\n");
            }
        } else {
            prompt.append("- No recipes available\n");
        }
        
        prompt.append("\nWhat should you do next? Choose from:\n");
        if (availableActions != null && !availableActions.isEmpty()) {
            for (String action : availableActions) {
                prompt.append("- ").append(action).append("\n");
            }
        }
        
        return prompt.toString();
    }
}

