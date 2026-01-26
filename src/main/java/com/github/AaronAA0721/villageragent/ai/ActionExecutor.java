package com.github.AaronAA0721.villageragent.ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes actions decided by the LLM
 * Currently just logs actions - actual implementation will be added later
 */
public class ActionExecutor {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Execute an action for a villager
     * Implementation will be added in future sessions
     */
    public static void executeAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info("Executing action for " + agent.getName() + ": " + action);
        
        switch (action.getActionType()) {
            case CRAFT:
                executeCraftAction(agent, action);
                break;
            case HARVEST:
                executeHarvestAction(agent, action);
                break;
            case GROW:
                executeGrowAction(agent, action);
                break;
            case IDLE:
                executeIdleAction(agent, action);
                break;
            case MOVE:
                executeMoveAction(agent, action);
                break;
            case GATHER:
                executeGatherAction(agent, action);
                break;
            case UNKNOWN:
                LOGGER.warn("Unknown action for " + agent.getName());
                agent.addMemory("Received unknown action from LLM");
                break;
        }
    }
    
    private static void executeCraftAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info(agent.getName() + " is crafting: " + action.getTargetRecipe());
        agent.addMemory("Started crafting: " + action.getTargetRecipe());
        agent.setCurrentActivity("crafting");
        // TODO: Implement crafting logic - find crafting table, move to it, craft
    }
    
    private static void executeHarvestAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info(agent.getName() + " is harvesting: " + action.getTargetItem());
        agent.addMemory("Started harvesting: " + action.getTargetItem());
        agent.setCurrentActivity("harvesting");
        // TODO: Implement harvesting logic - find crops, harvest them
    }
    
    private static void executeGrowAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info(agent.getName() + " is growing: " + action.getTargetItem());
        agent.addMemory("Started growing: " + action.getTargetItem());
        agent.setCurrentActivity("growing");
        // TODO: Implement growing logic - find farmland, plant seeds
    }
    
    private static void executeIdleAction(VillagerAgentData agent, VillagerAction action) {
        long duration = action.getIdleDuration();
        LOGGER.info(agent.getName() + " is idle for " + duration + " ticks");
        agent.addMemory("Idle for " + duration + " ticks");
        agent.setCurrentActivity("idle");
        // TODO: Implement idle logic - wait for specified duration
    }
    
    private static void executeMoveAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info(agent.getName() + " is moving to: " + action.getDescription());
        agent.addMemory("Moving to: " + action.getDescription());
        agent.setCurrentActivity("moving");
        // TODO: Implement pathfinding logic
    }
    
    private static void executeGatherAction(VillagerAgentData agent, VillagerAction action) {
        LOGGER.info(agent.getName() + " is gathering: " + action.getTargetItem());
        agent.addMemory("Started gathering: " + action.getTargetItem());
        agent.setCurrentActivity("gathering");
        // TODO: Implement gathering logic - pick up items from ground
    }
}

