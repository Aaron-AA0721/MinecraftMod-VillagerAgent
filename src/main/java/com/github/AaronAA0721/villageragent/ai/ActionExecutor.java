package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes actions decided by the LLM or goal system.
 * Actions that interact with the world require a VillagerEntity and ServerWorld.
 */
public class ActionExecutor {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Execute an action for a villager (legacy overload without world access).
     * Delegates to the full method with null entity/world for non-world actions.
     */
    public static void executeAction(VillagerAgentData agent, VillagerAction action) {
        executeAction(agent, action, null, null);
    }

    /**
     * Execute an action for a villager with world access for block interactions.
     * @param agent     The villager's AI data
     * @param action    The action to execute
     * @param villager  The actual villager entity (may be null for non-world actions)
     * @param world     The server world (may be null for non-world actions)
     */
    public static void executeAction(VillagerAgentData agent, VillagerAction action,
                                      VillagerEntity villager, ServerWorld world) {
        LOGGER.info("Executing action for " + agent.getName() + ": " + action);

        switch (action.getActionType()) {
            case CRAFT:
                executeCraftAction(agent, action);
                break;
            case HARVEST:
                executeHarvestAction(agent, action, villager, world);
                break;
            case GROW:
                executeGrowAction(agent, action, villager, world);
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

    /**
     * Harvest action — the walk-then-act state machine in VillagerAgentManager
     * handles the actual walking and single-block harvesting. This is only called
     * if something dispatches a HARVEST action directly through ActionExecutor.
     * It will harvest the single block at action.targetBlockPos if the villager is close enough.
     */
    private static void executeHarvestAction(VillagerAgentData agent, VillagerAction action,
                                              VillagerEntity villager, ServerWorld world) {
        agent.setCurrentActivity("harvesting");

        if (villager == null || world == null) {
            LOGGER.warn(agent.getName() + " cannot harvest: no world access");
            agent.addMemory("Tried to harvest but couldn't interact with the world");
            return;
        }

        BlockPos target = action.getTargetBlockPos();
        if (target != null && villager.blockPosition().distSqr(target) <= FarmingAction.INTERACT_RANGE_SQ) {
            FarmingAction.harvestBlockAt(villager, world, agent, target);
        } else {
            LOGGER.debug(agent.getName() + " harvest action has no reachable target");
        }
    }

    /**
     * Grow/plant action — same as harvest, the state machine handles walking.
     * This plants a single seed at action.targetBlockPos if the villager is close enough.
     */
    private static void executeGrowAction(VillagerAgentData agent, VillagerAction action,
                                           VillagerEntity villager, ServerWorld world) {
        agent.setCurrentActivity("planting");

        if (villager == null || world == null) {
            LOGGER.warn(agent.getName() + " cannot plant: no world access");
            agent.addMemory("Tried to plant but couldn't interact with the world");
            return;
        }

        BlockPos target = action.getTargetBlockPos();
        if (target != null && villager.blockPosition().distSqr(target) <= FarmingAction.INTERACT_RANGE_SQ) {
            FarmingAction.plantSeedAt(villager, world, agent, target);
        } else {
            LOGGER.debug(agent.getName() + " plant action has no reachable target");
        }
    }

    private static void executeIdleAction(VillagerAgentData agent, VillagerAction action) {
        long duration = action.getIdleDuration();
        LOGGER.info(agent.getName() + " is idle for " + duration + " ticks");
        agent.addMemory("Idle for " + duration + " ticks");
        agent.setCurrentActivity("idle");
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

