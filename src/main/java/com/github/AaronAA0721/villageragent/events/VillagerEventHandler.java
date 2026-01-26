package com.github.AaronAA0721.villageragent.events;

import com.github.AaronAA0721.villageragent.ai.*;
import com.github.AaronAA0721.villageragent.config.ModConfig;
import com.github.AaronAA0721.villageragent.network.ModNetworking;
import com.github.AaronAA0721.villageragent.network.SyncVillagerDataPacket;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all Minecraft events related to villager AI agents
 */
public class VillagerEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private int pickupTickCounter = 0;
    
    /**
     * When a villager spawns or joins the world, create an AI agent for it
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;

        if (event.getEntity() instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) event.getEntity();
            if (!event.getWorld().isClientSide) {
                VillagerAgentData agent = VillagerAgentManager.getOrCreateAgent(villager);
                // Update profession from the actual villager entity
                updateVillagerProfession(villager, agent);
                LOGGER.info("AI Agent created for villager: " + villager.getUUID() + " (" + agent.getProfession() + ")");
            }
        }
    }

    /**
     * Get the profession name from a villager entity
     */
    private String getVillagerProfessionName(VillagerEntity villager) {
        String professionKey = villager.getVillagerData().getProfession().toString();
        // Convert "minecraft:farmer" to "Farmer"
        if (professionKey.contains(":")) {
            professionKey = professionKey.substring(professionKey.indexOf(":") + 1);
        }
        // Capitalize first letter
        if (!professionKey.isEmpty()) {
            professionKey = professionKey.substring(0, 1).toUpperCase() + professionKey.substring(1).toLowerCase();
        }
        // Handle "none" profession
        if ("None".equals(professionKey)) {
            professionKey = "Unemployed Villager";
        }
        return professionKey;
    }

    /**
     * Update the agent's profession from the villager entity
     */
    private void updateVillagerProfession(VillagerEntity villager, VillagerAgentData agent) {
        String profession = getVillagerProfessionName(villager);
        agent.updateProfession(profession);
    }
    
    /**
     * When a villager dies, drop all items from inventory and remove AI agent data
     */
    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) event.getEntity();
            VillagerAgentData agent = VillagerAgentManager.getAgent(villager.getUUID());

            if (agent != null && !villager.level.isClientSide) {
                // Drop all items from the villager's inventory
                for (int i = 0; i < agent.getInventory().getItems().size(); i++) {
                    net.minecraft.item.ItemStack stack = agent.getInventory().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        villager.spawnAtLocation(stack);
                        LOGGER.debug("Villager " + agent.getName() + " dropped: " + stack.getCount() + "x " + stack.getItem().getRegistryName());
                    }
                }

                LOGGER.info("Villager " + agent.getName() + " died and dropped all items");
            }

            VillagerAgentManager.removeAgent(villager.getUUID());
            LOGGER.info("AI Agent removed for villager: " + villager.getUUID());
        }
    }
    
    /**
     * Update all AI agents every tick and handle item pickup
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;

        if (event.phase == TickEvent.Phase.END && !event.world.isClientSide) {
            VillagerAgentManager.tickAgents(event.world);

            // Handle item pickup for all villagers using configurable interval
            pickupTickCounter++;
            int pickupInterval = ModConfig.VILLAGER_PICKUP_INTERVAL.get();
            if (pickupTickCounter >= pickupInterval) {
                pickupTickCounter = 0;
                handleVillagerItemPickup(event.world);
            }
        }
    }

    /**
     * Make villagers pick up nearby items automatically using item attraction
     * Items are attracted to villagers like they're attracted to players
     * Runs at configurable intervals (default: every 10 ticks)
     */
    private void handleVillagerItemPickup(net.minecraft.world.World world) {
        if (!ModConfig.ENABLE_AUTO_PICKUP.get()) return;

        try {
            // Only process if there are agents
            if (VillagerAgentManager.getAgentCount() == 0) return;

            // Iterate through all agents
            for (VillagerAgentData agent : VillagerAgentManager.getAllAgents()) {
                if (agent == null) continue;

                // Find the villager entity using ServerWorld
                if (!(world instanceof net.minecraft.world.server.ServerWorld)) continue;
                net.minecraft.world.server.ServerWorld serverWorld = (net.minecraft.world.server.ServerWorld) world;

                net.minecraft.entity.Entity entity = serverWorld.getEntity(agent.getVillagerId());
                if (!(entity instanceof VillagerEntity)) continue;

                VillagerEntity villager = (VillagerEntity) entity;
                if (!villager.isAlive()) continue;

                // Use the new item attraction system
                // Items are attracted to villager and automatically picked up
                ItemAttractionSystem.processItemAttraction(villager, world, agent);
            }
        } catch (Exception e) {
            LOGGER.error("Error in villager item pickup: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle player interaction with villagers
     * Right-click = Open Chat GUI with Trade Panel
     * Players can NO LONGER directly access villager inventory - must trade!
     */
    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof VillagerEntity)) return;
        if (event.getHand() != Hand.MAIN_HAND) return;

        // Only process on server side to prevent double triggering
        if (event.getWorld().isClientSide) return;

        VillagerEntity villager = (VillagerEntity) event.getTarget();
        PlayerEntity player = event.getPlayer();
        VillagerAgentData agent = VillagerAgentManager.getAgent(villager.getUUID());

        if (agent == null) return;
        if (!ModConfig.ENABLE_VILLAGER_CHAT.get()) return;

        // Cancel vanilla villager interaction (trade menu)
        event.setCanceled(true);
        event.setResult(Event.Result.DENY);

        // Update profession from the actual villager entity (in case it changed)
        updateVillagerProfession(villager, agent);

        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            String playerName = player.getName().getString();

            // Collect villager inventory items for display (read-only)
            List<ItemStack> inventoryItems = new ArrayList<>();
            for (ItemStack item : agent.getInventory().getItems()) {
                if (!item.isEmpty()) {
                    inventoryItems.add(item.copy());
                }
            }

            // Send villager data to client to open chat GUI (now includes profession)
            SyncVillagerDataPacket syncPacket = new SyncVillagerDataPacket(
                    villager.getUUID(),
                    agent.getName(),
                    agent.getProfession(),
                    agent.getPersonality(),
                    inventoryItems
            );
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), syncPacket);

            // NO automatic greeting - wait for player to say something first
            // Only reply when player sends a message

            // Log the interaction
            agent.addMemory("Started conversation with player " + playerName);
            LOGGER.info("Player " + playerName + " opened chat with " + agent.getProfession() + " " + agent.getName());
        }
    }

    /**
     * Load agent data when world loads
     */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;
        if (event.getWorld().isClientSide()) return;

        ServerWorld serverWorld = (ServerWorld) event.getWorld();
        VillagerAgentSavedData savedData = VillagerAgentSavedData.get(serverWorld);
        LOGGER.info("Loaded villager agent data from world save");
    }

    /**
     * Save agent data when world saves
     */
    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (!ModConfig.ENABLE_AI_AGENTS.get()) return;
        if (event.getWorld().isClientSide()) return;

        ServerWorld serverWorld = (ServerWorld) event.getWorld();
        VillagerAgentSavedData savedData = VillagerAgentSavedData.get(serverWorld);
        savedData.setDirty(true);
        LOGGER.info("Saved villager agent data to world save");
    }
}

