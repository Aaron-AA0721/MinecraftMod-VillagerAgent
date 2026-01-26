package com.github.AaronAA0721.villageragent.ai;

import com.github.AaronAA0721.villageragent.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
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
     * Update all agents in the world
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
     * Update a single agent's AI
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
        // TODO: Implementation for gathering resources
        // This will be expanded in the world interaction system
        agent.addMemory("Tried to gather " + goal.getTargetItem());
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

