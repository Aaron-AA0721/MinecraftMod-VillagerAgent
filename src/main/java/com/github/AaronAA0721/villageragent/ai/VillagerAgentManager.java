package com.github.AaronAA0721.villageragent.ai;

import com.github.AaronAA0721.villageragent.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all AI villager agents in the world
 */
public class VillagerAgentManager {
    private static final Random RANDOM = new Random();

    /**
     * Chance per tick that an idle farmer glances around for farming work.
     * Farming ticks run every tick (not gated by AGENT_THINK_INTERVAL),
     * so 1/200 ≈ once every 10 seconds on average.
     */
    private static final double FARMING_SCAN_CHANCE = 0.005;

    /**
     * How often (in ticks) the farming state machine runs for active farmers.
     * Walking/acting checks run on this interval for responsiveness.
     * Idle scanning uses its own random chance per tick.
     */
    private static final int FARMING_TICK_INTERVAL = 3;

    /**
     * Cooldown (in ticks) after the villager finishes a farming session before
     * it starts scanning for new work again.  200-400 ticks ≈ 10-20 seconds.
     */
    private static final int FARMING_COOLDOWN_MIN_TICKS = 200;
    private static final int FARMING_COOLDOWN_MAX_TICKS = 400;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<UUID, VillagerAgentData> agents = new ConcurrentHashMap<>();
    
    /**
     * Get or create agent data for a villager
     */
    public static VillagerAgentData getOrCreateAgent(VillagerEntity villager) {
        UUID id = villager.getUUID();
        return agents.computeIfAbsent(id, uuid -> {
            LOGGER.info("Creating new AI agent for villager: " + uuid);
            return new VillagerAgentData(uuid);
        });
    }
    
    /**
     * Get agent data if it exists
     */
    public static VillagerAgentData getAgent(UUID villagerId) {
        return agents.get(villagerId);
    }
    
    /**
     * Add an agent directly (used when loading from saved data)
     */
    public static void addAgent(UUID villagerId, VillagerAgentData agent) {
        agents.put(villagerId, agent);
        LOGGER.info("Added AI agent: " + villagerId);
    }

    /**
     * Remove agent data (when villager dies or is removed)
     */
    public static void removeAgent(UUID villagerId) {
        agents.remove(villagerId);
        LOGGER.info("Removed AI agent: " + villagerId);
    }
    
    /**
     * Update all agents in the world (slow tick — goals, restocking, etc.)
     */
    public static void tickAgents(World world) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;

        long currentTime = world.getGameTime();
        int thinkInterval = ModConfig.AGENT_THINK_INTERVAL.get();

        for (Map.Entry<UUID, VillagerAgentData> entry : agents.entrySet()) {
            VillagerAgentData agent = entry.getValue();

            // Only update periodically to avoid performance issues
            if (currentTime - agent.getLastThinkTime() >= thinkInterval) {
                agent.setLastThinkTime(currentTime);
                updateAgent(world, agent);
            }
        }
    }

    /**
     * Fast tick for farming — runs every tick so walking/acting is responsive.
     * Separated from the slow AI think loop to avoid 100-tick delays between
     * walk checks and action execution.
     */
    public static void tickFarming(World world) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;
        if (!ModConfig.ENABLE_WORLD_INTERACTION.get()) return;
        if (!(world instanceof ServerWorld)) return;

        long currentTime = world.getGameTime();
        ServerWorld serverWorld = (ServerWorld) world;

        for (VillagerAgentData agent : agents.values()) {
            String profession = agent.getProfession();
            if (profession == null || !profession.equalsIgnoreCase("farmer")) continue;

            // Active farming (walking/acting) runs every FARMING_TICK_INTERVAL ticks
            // Idle scanning runs every tick but with a low random chance
            boolean hasActiveAction = agent.getCurrentAction() != null && isFarmingAction(agent.getCurrentAction());
            boolean inFarmingState = agent.isInFarmingState();
            boolean onCooldown = agent.isOnFarmingCooldown();

            if (hasActiveAction || inFarmingState || onCooldown) {
                // Active farming — run every few ticks for responsiveness
                if (currentTime % FARMING_TICK_INTERVAL != 0) continue;
            }
            // else: idle — runs every tick, gated by FARMING_SCAN_CHANCE inside performFarmerActions

            VillagerEntity villager = findVillagerEntity(world, agent.getVillagerId());
            if (villager == null) continue;

            performFarmerActions(villager, serverWorld, agent);
        }
    }

    /**
     * Update a single agent's AI (slow tick — goals, restocking, etc.)
     * Farming is handled separately by tickFarming().
     */
    private static void updateAgent(World world, VillagerAgentData agent) {
        // Find the actual villager entity
        VillagerEntity villager = findVillagerEntity(world, agent.getVillagerId());
        if (villager == null) {
            return; // Villager not loaded or doesn't exist
        }

        // Check if villager is at their job block for restocking
        checkJobBlockRestock(villager, agent);

        // Process current goals
        processGoals(villager, agent);

        // Decide on new actions based on AI
        if (agent.getGoals().isEmpty() || shouldGenerateNewGoals(agent)) {
            generateNewGoals(villager, agent);
        }
    }

    /**
     * Farmer-specific automatic actions — state machine that runs every few ticks.
     *
     * States:
     * 1. **Walking** — an action is in progress (walking to a block). Continue it.
     * 2. **Cooldown** — the villager just finished a farming session and is resting.
     *    Tick down the cooldown; do nothing else.
     * 3. **Farming** — the villager is actively working an area. Scan 360° for the
     *    next block to harvest/plant. If nothing left → exit + start cooldown.
     * 4. **Idle** — the villager is wandering. Every ~10 s on average (random roll)
     *    it glances at its forward cone. If it spots work → enter farming state.
     */
    private static void performFarmerActions(VillagerEntity villager, ServerWorld world, VillagerAgentData agent) {
        // ── 1. Walking to a target block — continue the action ──
        VillagerAction current = agent.getCurrentAction();
        if (current != null && isFarmingAction(current)) {
            continueFarmingAction(villager, world, agent, current);
            return;
        }

        // ── 2. Cooldown after a farming session ──
        if (agent.isOnFarmingCooldown()) {
            agent.tickFarmingCooldown(FARMING_TICK_INTERVAL);
            return; // resting — do nothing
        }

        // ── 3. In farming state — scan 360° for the next reachable block ──
        if (agent.isInFarmingState()) {
            BlockPos villagerPos = villager.blockPosition();

            // Priority 1: harvest mature crops (try nearest reachable)
            BlockPos cropTarget = findFirstReachable(villager,
                    FarmingAction.findMatureCropsSorted(world, villagerPos));
            if (cropTarget != null) {
                startFarmingAction(villager, agent, VillagerAction.ActionType.HARVEST,
                        "Harvesting area", cropTarget);
                return;
            }

            // Priority 2: plant seeds on empty farmland
            if (FarmingAction.hasSeeds(agent)) {
                BlockPos farmlandTarget = findFirstReachable(villager,
                        FarmingAction.findEmptyFarmlandSorted(world, villagerPos));
                if (farmlandTarget != null) {
                    startFarmingAction(villager, agent, VillagerAction.ActionType.GROW,
                            "Planting area", farmlandTarget);
                    return;
                }
            }

            // Nothing reachable — exit farming state, start cooldown
            exitFarmingState(agent);
            return;
        }

        // ── 4. Idle — random chance to glance at forward cone ──
        if (RANDOM.nextDouble() > FARMING_SCAN_CHANCE) {
            return; // not looking this tick
        }

        BlockPos villagerPos = villager.blockPosition();
        float headYaw = villager.yHeadRot;

        // Check forward cone for anything to do
        BlockPos cropTarget = FarmingAction.findNearestMatureCrop(world, villagerPos, headYaw);
        if (cropTarget != null) {
            enterFarmingState(agent);
            startFarmingAction(villager, agent, VillagerAction.ActionType.HARVEST,
                    "Noticed crops — starting harvest", cropTarget);
            return;
        }

        if (FarmingAction.hasSeeds(agent)) {
            BlockPos farmlandTarget = FarmingAction.findNearestEmptyFarmland(world, villagerPos, headYaw);
            if (farmlandTarget != null) {
                enterFarmingState(agent);
                startFarmingAction(villager, agent, VillagerAction.ActionType.GROW,
                        "Noticed farmland — starting planting", farmlandTarget);
                return;
            }
        }
    }

    /** Enter farming state — the villager commits to working the area. */
    private static void enterFarmingState(VillagerAgentData agent) {
        agent.setInFarmingState(true);
        agent.setCurrentActivity("farming");
        LOGGER.debug(agent.getName() + " entered farming state");
    }

    /** Exit farming state and start a cooldown before the next scan cycle. */
    private static void exitFarmingState(VillagerAgentData agent) {
        agent.setInFarmingState(false);
        int cooldown = FARMING_COOLDOWN_MIN_TICKS
                + RANDOM.nextInt(FARMING_COOLDOWN_MAX_TICKS - FARMING_COOLDOWN_MIN_TICKS + 1);
        agent.setFarmingCooldownTicks(cooldown);
        agent.setCurrentActivity("idle");
        LOGGER.debug(agent.getName() + " finished farming — cooldown " + cooldown + " ticks");
    }

    /**
     * Given a list of candidate BlockPos (sorted nearest-first), return the first
     * one the villager can actually path to, or null if none are reachable.
     * Uses Minecraft's built-in A* pathfinder — cheap for short distances.
     */
    private static BlockPos findFirstReachable(VillagerEntity villager, List<BlockPos> candidates) {
        for (BlockPos pos : candidates) {
            Path path = villager.getNavigation().createPath(pos, 1);
            if (path != null) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isFarmingAction(VillagerAction action) {
        return action.getActionType() == VillagerAction.ActionType.HARVEST
            || action.getActionType() == VillagerAction.ActionType.GROW;
    }

    /**
     * Create a new farming action, set the target block, and start walking.
     */
    private static void startFarmingAction(VillagerEntity villager, VillagerAgentData agent,
                                            VillagerAction.ActionType type, String desc, BlockPos target) {
        VillagerAction action = new VillagerAction(type, desc);
        action.setTargetBlockPos(target);
        action.setPhase(VillagerAction.ActionPhase.WALKING);
        agent.setCurrentAction(action);

        // Keep "farming" as the activity while in farming state
        if (!agent.isInFarmingState()) {
            agent.setCurrentActivity(type == VillagerAction.ActionType.HARVEST ? "harvesting" : "planting");
        }

        // Tell the villager to walk toward the target block
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);

        LOGGER.debug(agent.getName() + " [" + desc + "] — walking to " + target);
    }

    /**
     * Continue an in-progress farming action: check distance, act or keep walking.
     *
     * Vanilla villager brain activities (WORK, IDLE, MEET, etc.) continuously
     * issue their own navigation commands that override ours.  To prevent the
     * villager from wandering away mid-farm we:
     *   1. Stop the current vanilla navigation path every call.
     *   2. Re-issue our own moveTo every call (every FARMING_TICK_INTERVAL ticks).
     */
    private static void continueFarmingAction(VillagerEntity villager, ServerWorld world,
                                               VillagerAgentData agent, VillagerAction action) {
        BlockPos target = action.getTargetBlockPos();
        if (target == null) {
            agent.setCurrentAction(null);
            return;
        }

        BlockPos villagerPos = villager.blockPosition();
        double distSq = villagerPos.distSqr(target);

        // Check if we've arrived (within 1 block)
        if (distSq <= FarmingAction.INTERACT_RANGE_SQ) {
            action.setPhase(VillagerAction.ActionPhase.ACTING);
            performFarmingActionAtBlock(villager, world, agent, action, target);
            agent.setCurrentAction(null);
            return;
        }

        // Still walking — check if stuck
        action.incrementStuckTicks();
        if (action.getStuckTicks() > FarmingAction.STUCK_TIMEOUT_TICKS) {
            LOGGER.debug(agent.getName() + " gave up reaching " + target + " (stuck)");
            agent.setCurrentAction(null);
            if (!agent.isInFarmingState()) {
                agent.setCurrentActivity("idle");
            }
            return;
        }

        // Cancel whatever vanilla AI decided to do, then re-assert our path.
        // This runs every FARMING_TICK_INTERVAL (3) ticks — fast enough to
        // override vanilla brain tasks without visible jitter.
        villager.getNavigation().stop();
        villager.getNavigation().moveTo(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
    }

    /**
     * The villager has arrived at the target block — perform the actual action.
     */
    private static void performFarmingActionAtBlock(VillagerEntity villager, ServerWorld world,
                                                     VillagerAgentData agent, VillagerAction action,
                                                     BlockPos target) {
        switch (action.getActionType()) {
            case HARVEST:
                // Verify the crop is still there and mature
                if (FarmingAction.isMatureCrop(world, target)) {
                    // Remember which crop was here before harvesting
                    net.minecraft.block.Block harvestedCrop = FarmingAction.harvestBlockAt(villager, world, agent, target);
                    // After harvesting, replant the same crop type
                    if (harvestedCrop != null && FarmingAction.isEmptyFarmland(world, target.below())) {
                        FarmingAction.plantSpecificCropAt(villager, world, agent, target.below(), harvestedCrop);
                    }
                } else {
                    LOGGER.debug(agent.getName() + " arrived but crop at " + target + " is gone");
                }
                break;
            case GROW:
                // Plant on empty farmland: prefer same crop as adjacent blocks, else random
                if (FarmingAction.isEmptyFarmland(world, target)) {
                    FarmingAction.plantSmartAt(villager, world, agent, target);
                } else {
                    LOGGER.debug(agent.getName() + " arrived but farmland at " + target + " is occupied");
                }
                break;
            default:
                break;
        }
    }

    /**
     * Check if villager is at their job block and should restock
     * Mimics vanilla Minecraft behavior where villagers restock at their workstation
     */
    private static void checkJobBlockRestock(VillagerEntity villager, VillagerAgentData agent) {
        // Get the villager's job site from their brain memory
        Optional<GlobalPos> jobSiteOptional = villager.getBrain()
            .getMemory(MemoryModuleType.JOB_SITE);

        if (!jobSiteOptional.isPresent()) {
            return; // No job site assigned
        }

        GlobalPos jobSite = jobSiteOptional.get();
        BlockPos jobBlockPos = jobSite.pos();
        BlockPos villagerPos = villager.blockPosition();

        // Check if villager is within 2 blocks of their job site
        double distance = villagerPos.distSqr(jobBlockPos);
        if (distance <= 4.0) { // 2 blocks squared
            // Check if enough time has passed since last restock (once per Minecraft day = 24000 ticks)
            long currentTime = villager.level.getGameTime();
            long lastRestockTime = agent.getLastRestockTime();

            // Restock once per Minecraft day (24000 ticks)
            if (currentTime - lastRestockTime >= 24000) {
                agent.restockAtJobBlock();
                agent.setLastRestockTime(currentTime);
                LOGGER.info("Villager " + agent.getName() + " restocked at job block");
            }
        }
    }
    
    /**
     * Find a villager entity in the world by UUID
     */
    private static VillagerEntity findVillagerEntity(World world, UUID villagerId) {
        // In Minecraft 1.16.5, we need to use ServerWorld to get entities
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Entity entity = serverWorld.getEntity(villagerId);
            if (entity instanceof VillagerEntity) {
                return (VillagerEntity) entity;
            }
        }
        return null;
    }
    
    /**
     * Process the agent's current goals
     */
    private static void processGoals(VillagerEntity villager, VillagerAgentData agent) {
        List<AgentGoal> goals = agent.getGoals();
        if (goals.isEmpty()) return;
        
        // Sort by priority (highest first)
        goals.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        AgentGoal currentGoal = goals.get(0);
        
        // Execute goal based on type
        switch (currentGoal.getGoalType()) {
            case "gather":
                executeGatherGoal(villager, agent, currentGoal);
                break;
            case "craft":
                executeCraftGoal(villager, agent, currentGoal);
                break;
            case "trade":
                executeTradeGoal(villager, agent, currentGoal);
                break;
            case "socialize":
                executeSocializeGoal(villager, agent, currentGoal);
                break;
            default:
                LOGGER.warn("Unknown goal type: " + currentGoal.getGoalType());
        }
        
        // Remove completed goals
        goals.removeIf(AgentGoal::isCompleted);
    }
    
    private static void executeGatherGoal(VillagerEntity villager, VillagerAgentData agent, AgentGoal goal) {
        // For farmer villagers gathering crops, the farming walk-then-act system
        // handles this automatically via performFarmerActions. Just log intent.
        agent.addMemory("Trying to gather " + goal.getTargetItem());
    }
    
    private static void executeCraftGoal(VillagerEntity villager, VillagerAgentData agent, AgentGoal goal) {
        // TODO: Implementation for crafting items
        agent.addMemory("Tried to craft " + goal.getTargetItem());
    }
    
    private static void executeTradeGoal(VillagerEntity villager, VillagerAgentData agent, AgentGoal goal) {
        // TODO: Implementation for trading with players or other villagers
        agent.addMemory("Looking for trading opportunities");
    }
    
    private static void executeSocializeGoal(VillagerEntity villager, VillagerAgentData agent, AgentGoal goal) {
        // TODO: Implementation for villager-to-villager interaction
        agent.addMemory("Socializing with other villagers");
    }

    /**
     * Check if we should generate new goals for this agent
     */
    private static boolean shouldGenerateNewGoals(VillagerAgentData agent) {
        List<AgentGoal> goals = agent.getGoals();
        if (goals.isEmpty()) return true;

        // Generate new goals if current goals are old (5 minutes)
        long currentTime = System.currentTimeMillis();
        for (AgentGoal goal : goals) {
            if (currentTime - goal.getCreatedTime() > 300000) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate new goals for the agent using simple logic
     * TODO: Integrate with LLM for more intelligent goal generation
     */
    private static void generateNewGoals(VillagerEntity villager, VillagerAgentData agent) {
        // For now, generate simple random goals
        // Later this will use LLM to generate contextual, personality-driven goals

        Random random = new Random();
        String[] goalTypes = {"gather", "craft", "trade", "socialize"};
        String[] gatherItems = {"wheat", "carrots", "potatoes", "wood", "stone"};
        String[] craftItems = {"bread", "tools", "armor"};

        String goalType = goalTypes[random.nextInt(goalTypes.length)];
        AgentGoal newGoal;

        switch (goalType) {
            case "gather":
                String item = gatherItems[random.nextInt(gatherItems.length)];
                newGoal = new AgentGoal("gather", "Gather " + item, random.nextInt(5) + 3);
                newGoal.setTargetItem(item);
                newGoal.setTargetQuantity(random.nextInt(10) + 5);
                break;
            case "craft":
                String craftItem = craftItems[random.nextInt(craftItems.length)];
                newGoal = new AgentGoal("craft", "Craft " + craftItem, random.nextInt(5) + 3);
                newGoal.setTargetItem(craftItem);
                break;
            case "trade":
                newGoal = new AgentGoal("trade", "Look for trading opportunities", random.nextInt(5) + 3);
                break;
            case "socialize":
                newGoal = new AgentGoal("socialize", "Talk with other villagers", random.nextInt(3) + 1);
                break;
            default:
                return;
        }

        agent.getGoals().add(newGoal);
        agent.addMemory("New goal: " + newGoal.getDescription());
        LOGGER.debug("Generated new goal for " + agent.getName() + ": " + newGoal.getDescription());
    }

    /**
     * Get all agents (for debugging/admin purposes)
     */
    public static Collection<VillagerAgentData> getAllAgents() {
        return agents.values();
    }

    /**
     * Get the number of active agents
     */
    public static int getAgentCount() {
        return agents.size();
    }
}

