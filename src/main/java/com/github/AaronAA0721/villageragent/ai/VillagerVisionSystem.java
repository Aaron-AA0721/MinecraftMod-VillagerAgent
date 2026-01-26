package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Vision system for villagers - allows them to see and record observations
 */
public class VillagerVisionSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int VISION_RANGE = 16; // blocks
    private static final int VISION_HEIGHT = 8; // blocks up/down
    
    /**
     * Scan the area in front of the villager and record observations
     */
    public static List<VillagerObservation> scanSurroundings(LivingEntity villager, World world) {
        List<VillagerObservation> observations = new ArrayList<>();
        
        Vector3d villagerPos = villager.position();
        BlockPos centerPos = villager.blockPosition();
        
        // Scan blocks in front of villager
        scanBlocks(centerPos, world, observations);
        
        // Scan nearby entities
        scanEntities(villager, world, observations);
        
        // Scan items on ground
        scanItems(centerPos, world, observations);
        
        return observations;
    }
    
    private static void scanBlocks(BlockPos center, World world, List<VillagerObservation> observations) {
        for (int x = -VISION_RANGE; x <= VISION_RANGE; x++) {
            for (int y = -VISION_HEIGHT; y <= VISION_HEIGHT; y++) {
                for (int z = -VISION_RANGE; z <= VISION_RANGE; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    
                    if (!block.isAir(state, world, pos)) {
                        String blockName = block.getRegistryName().toString();
                        String details = "Block state: " + state.toString();
                        observations.add(new VillagerObservation("block", blockName, pos, details));
                    }
                }
            }
        }
    }
    
    private static void scanEntities(LivingEntity villager, World world, List<VillagerObservation> observations) {
        List<Entity> nearby = world.getEntities(villager, villager.getBoundingBox().inflate(VISION_RANGE));
        
        for (Entity entity : nearby) {
            if (entity == villager) continue;
            
            String entityName = entity.getType().getRegistryName().toString();
            String details = "Health: " + (entity instanceof LivingEntity ? ((LivingEntity) entity).getHealth() : "N/A");
            observations.add(new VillagerObservation("entity", entityName, entity.blockPosition(), details));
        }
    }
    
    private static void scanItems(BlockPos center, World world, List<VillagerObservation> observations) {
        List<Entity> items = world.getEntities(null, 
            new net.minecraft.util.math.AxisAlignedBB(
                center.getX() - VISION_RANGE, center.getY() - VISION_HEIGHT, center.getZ() - VISION_RANGE,
                center.getX() + VISION_RANGE, center.getY() + VISION_HEIGHT, center.getZ() + VISION_RANGE
            )
        );
        
        for (Entity entity : items) {
            if (entity instanceof net.minecraft.entity.item.ItemEntity) {
                net.minecraft.entity.item.ItemEntity itemEntity = (net.minecraft.entity.item.ItemEntity) entity;
                ItemStack stack = itemEntity.getItem();
                String itemName = stack.getItem().getRegistryName().toString();
                String details = "Count: " + stack.getCount();
                observations.add(new VillagerObservation("item", itemName, entity.blockPosition(), details));
            }
        }
    }
}

