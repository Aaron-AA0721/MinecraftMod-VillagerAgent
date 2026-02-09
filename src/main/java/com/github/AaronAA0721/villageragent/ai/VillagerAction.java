package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.util.math.BlockPos;

/**
 * Represents an action a villager can take.
 * For world-interacting actions (HARVEST, GROW), the villager must walk to
 * targetBlockPos before the action executes.
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

    /** Phases for actions that require the villager to walk somewhere first. */
    public enum ActionPhase {
        SEARCHING,      // Looking for a target block
        WALKING,        // Walking toward the target block
        ACTING          // Close enough — performing the action
    }

    private ActionType actionType;
    private ActionPhase phase = ActionPhase.SEARCHING;
    private String description;
    private String targetRecipe;    // For CRAFT actions
    private String targetItem;      // For HARVEST/GATHER actions
    private int targetQuantity;     // For HARVEST/GATHER actions
    private long idleDuration;      // For IDLE actions (in ticks)
    private long createdTime;
    private BlockPos targetBlockPos; // The block the villager is walking toward
    private int stuckTicks;         // How many ticks the villager has been unable to reach target

    public VillagerAction(ActionType actionType, String description) {
        this.actionType = actionType;
        this.description = description;
        this.createdTime = System.currentTimeMillis();
    }

    // Getters and setters
    public ActionType getActionType() { return actionType; }
    public String getDescription() { return description; }
    public ActionPhase getPhase() { return phase; }
    public void setPhase(ActionPhase phase) { this.phase = phase; }
    public String getTargetRecipe() { return targetRecipe; }
    public void setTargetRecipe(String recipe) { this.targetRecipe = recipe; }
    public String getTargetItem() { return targetItem; }
    public void setTargetItem(String item) { this.targetItem = item; }
    public int getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(int quantity) { this.targetQuantity = quantity; }
    public long getIdleDuration() { return idleDuration; }
    public void setIdleDuration(long duration) { this.idleDuration = duration; }
    public long getCreatedTime() { return createdTime; }
    public BlockPos getTargetBlockPos() { return targetBlockPos; }
    public void setTargetBlockPos(BlockPos pos) { this.targetBlockPos = pos; }
    public int getStuckTicks() { return stuckTicks; }
    public void incrementStuckTicks() { this.stuckTicks++; }
    public void resetStuckTicks() { this.stuckTicks = 0; }

    @Override
    public String toString() {
        return String.format("[%s/%s] %s", actionType, phase, description);
    }
}

