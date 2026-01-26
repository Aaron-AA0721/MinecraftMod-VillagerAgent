package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Item attraction system - items are attracted to villagers like they're attracted to players
 * Villagers automatically collect nearby items within their pickup range
 */
public class ItemAttractionSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final double PICKUP_RANGE = 8.0; // blocks - same as player pickup range
    private static final double ATTRACTION_RANGE = 16.0; // blocks - items start moving towards villager
    
    /**
     * Process item attraction and pickup for a villager
     * Items within attraction range are pulled towards the villager
     * Items within pickup range are collected into inventory
     */
    public static void processItemAttraction(VillagerEntity villager, World world, VillagerAgentData agent) {
        if (villager == null || world == null || agent == null) {
            return;
        }
        
        // Get all items in attraction range
        AxisAlignedBB attractionBox = villager.getBoundingBox().inflate(ATTRACTION_RANGE);
        List<ItemEntity> nearbyItems = world.getEntitiesOfClass(ItemEntity.class, attractionBox);
        
        for (ItemEntity itemEntity : nearbyItems) {
            if (itemEntity.isAlive() && !itemEntity.getItem().isEmpty()) {
                // Pull item towards villager (like player pickup)
                attractItemToVillager(itemEntity, villager);
                
                // Check if item is close enough to pickup
                double distance = villager.distanceToSqr(itemEntity);
                if (distance < PICKUP_RANGE * PICKUP_RANGE) {
                    pickupItem(itemEntity, agent);
                }
            }
        }
    }
    
    /**
     * Attract an item towards the villager
     * Mimics the behavior of items being attracted to a player
     */
    private static void attractItemToVillager(ItemEntity itemEntity, VillagerEntity villager) {
        double dx = villager.getX() - itemEntity.getX();
        double dy = villager.getY() + 0.5 - itemEntity.getY();
        double dz = villager.getZ() - itemEntity.getZ();
        
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance > 0) {
            // Normalize direction
            dx /= distance;
            dy /= distance;
            dz /= distance;
            
            // Apply attraction velocity (similar to player pickup)
            double speed = 0.05; // Adjust for desired attraction speed
            itemEntity.setDeltaMovement(
                itemEntity.getDeltaMovement().x + dx * speed,
                itemEntity.getDeltaMovement().y + dy * speed,
                itemEntity.getDeltaMovement().z + dz * speed
            );
        }
    }
    
    /**
     * Pickup an item and add it to villager's inventory
     */
    private static void pickupItem(ItemEntity itemEntity, VillagerAgentData agent) {
        ItemStack stack = itemEntity.getItem().copy();

        // Try to add to inventory
        if (agent.getInventory().addItem(stack)) {
            // Successfully picked up - use remove() for 1.16.5
            itemEntity.remove();
            LOGGER.debug("Villager " + agent.getName() + " picked up: " + stack.getItem().getRegistryName());
            agent.addMemory("Picked up: " + stack.getCount() + "x " + stack.getItem().getRegistryName());
        }
    }
    
    /**
     * Get the pickup range (for configuration purposes)
     */
    public static double getPickupRange() {
        return PICKUP_RANGE;
    }
    
    /**
     * Get the attraction range (for configuration purposes)
     */
    public static double getAttractionRange() {
        return ATTRACTION_RANGE;
    }
}

