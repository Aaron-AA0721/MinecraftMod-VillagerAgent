package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.util.math.BlockPos;

/**
 * Represents a goal that an AI villager wants to accomplish
 */
public class AgentGoal {
    private String goalType; // "gather", "craft", "trade", "build", "socialize", etc.
    private String description;
    private int priority; // 1-10, higher is more important
    private BlockPos targetLocation;
    private String targetItem;
    private int targetQuantity;
    private boolean completed;
    private long createdTime;
    
    public AgentGoal(String goalType, String description, int priority) {
        this.goalType = goalType;
        this.description = description;
        this.priority = priority;
        this.completed = false;
        this.createdTime = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getGoalType() { return goalType; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public BlockPos getTargetLocation() { return targetLocation; }
    public void setTargetLocation(BlockPos pos) { this.targetLocation = pos; }
    public String getTargetItem() { return targetItem; }
    public void setTargetItem(String item) { this.targetItem = item; }
    public int getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(int quantity) { this.targetQuantity = quantity; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getCreatedTime() { return createdTime; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (Priority: %d)", goalType, description, priority);
    }
}

