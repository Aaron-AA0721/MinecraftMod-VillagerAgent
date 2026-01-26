package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.util.math.BlockPos;

/**
 * Represents an observation made by a villager (entity or block seen)
 */
public class VillagerObservation {
    private String type; // "entity", "block", "item"
    private String name; // Entity type, block name, or item name
    private BlockPos position;
    private long observationTime;
    private String details; // Additional details about the observation
    
    public VillagerObservation(String type, String name, BlockPos position, String details) {
        this.type = type;
        this.name = name;
        this.position = position;
        this.details = details;
        this.observationTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getType() { return type; }
    public String getName() { return name; }
    public BlockPos getPosition() { return position; }
    public long getObservationTime() { return observationTime; }
    public String getDetails() { return details; }
    
    @Override
    public String toString() {
        return String.format("Observed %s '%s' at %s: %s", type, name, position, details);
    }
}

