package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Persists villager agent data across world saves/loads
 */
public class VillagerAgentSavedData extends WorldSavedData {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATA_NAME = "villageragent_data";

    public VillagerAgentSavedData() {
        super(DATA_NAME);
    }

    public VillagerAgentSavedData(String name) {
        super(name);
    }

    /**
     * Get or create the saved data for a world
     */
    public static VillagerAgentSavedData get(ServerWorld world) {
        return world.getDataStorage().computeIfAbsent(
                VillagerAgentSavedData::new,
                DATA_NAME
        );
    }

    /**
     * Save all agent data to NBT
     */
    @Override
    public CompoundNBT save(CompoundNBT compound) {
        ListNBT agentsList = new ListNBT();

        // Save all agents
        for (VillagerAgentData agent : VillagerAgentManager.getAllAgents()) {
            CompoundNBT agentNBT = agent.serializeNBT();
            agentsList.add(agentNBT);
        }

        compound.put("Agents", agentsList);
        LOGGER.info("Saved " + agentsList.size() + " villager agents to world data");
        return compound;
    }

    /**
     * Load all agent data from NBT (called automatically by Minecraft)
     */
    @Override
    public void load(CompoundNBT compound) {
        ListNBT agentsList = compound.getList("Agents", 10);

        for (int i = 0; i < agentsList.size(); i++) {
            CompoundNBT agentNBT = agentsList.getCompound(i);
            UUID villagerId = agentNBT.getUUID("VillagerId");

            // Create new agent and load data
            VillagerAgentData agent = new VillagerAgentData(villagerId);
            agent.deserializeNBT(agentNBT);

            // Add to manager
            VillagerAgentManager.addAgent(villagerId, agent);
        }

        LOGGER.info("Loaded " + agentsList.size() + " villager agents from world data");
    }
}

