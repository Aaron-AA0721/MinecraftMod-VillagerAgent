package com.github.AaronAA0721.villageragent.ai;

/**
 * Represents an action a villager can take
 */
public class VillagerAction {
    public enum ActionType {
        CRAFT,          // Craft an item at a workstation
        HARVEST,        // Harvest crops or resources
        GROW,           // Plant seeds or grow crops
        IDLE,           // Do nothing for a time interval
        MOVE,           // Move to a location
        GATHER,         // Gather items from ground
        UNKNOWN         // Unknown action from LLM
    }
    
    private ActionType actionType;
    private String description;
    private String targetRecipe;    // For CRAFT actions
    private String targetItem;      // For HARVEST/GATHER actions
    private int targetQuantity;     // For HARVEST/GATHER actions
    private long idleDuration;      // For IDLE actions (in ticks)
    private long createdTime;
    
    public VillagerAction(ActionType actionType, String description) {
        this.actionType = actionType;
        this.description = description;
        this.createdTime = System.currentTimeMillis();
    }
    
    // Getters and setters
    public ActionType getActionType() { return actionType; }
    public String getDescription() { return description; }
    public String getTargetRecipe() { return targetRecipe; }
    public void setTargetRecipe(String recipe) { this.targetRecipe = recipe; }
    public String getTargetItem() { return targetItem; }
    public void setTargetItem(String item) { this.targetItem = item; }
    public int getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(int quantity) { this.targetQuantity = quantity; }
    public long getIdleDuration() { return idleDuration; }
    public void setIdleDuration(long duration) { this.idleDuration = duration; }
    public long getCreatedTime() { return createdTime; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s", actionType, description);
    }
}

