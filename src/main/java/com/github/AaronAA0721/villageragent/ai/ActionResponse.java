package com.github.AaronAA0721.villageragent.ai;

import java.util.UUID;

/**
 * Response from LLM containing the next action for a villager
 */
public class ActionResponse {
    private UUID villagerId;
    private VillagerAction action;
    private String reasoning;  // Why the LLM chose this action
    
    public ActionResponse(UUID villagerId, VillagerAction action, String reasoning) {
        this.villagerId = villagerId;
        this.action = action;
        this.reasoning = reasoning;
    }
    
    // Getters
    public UUID getVillagerId() { return villagerId; }
    public VillagerAction getAction() { return action; }
    public String getReasoning() { return reasoning; }
    
    @Override
    public String toString() {
        return String.format("Action for %s: %s (Reason: %s)", villagerId, action, reasoning);
    }
}

