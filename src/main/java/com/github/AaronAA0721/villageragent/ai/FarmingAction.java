package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.block.*;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Handles farming actions for villagers: harvesting mature crops and planting seeds.
 *
 * Uses a walk-then-act pattern:
 * 1. Scan for a target block within sight range
 * 2. Walk toward it using the villager's navigation
 * 3. When within reach (1 block), perform the action on that single block
 * 4. Repeat — dropped items are picked up by the general item attraction system
 */
public class FarmingAction {
    private static final Logger LOGGER = LogManager.getLogger();

    /** How far the villager can see to find crops/farmland. */
    public static final int SCAN_RADIUS = 5;
    private static final int SCAN_HEIGHT = 2;

    /** The villager must be within this distance (squared) to interact with a block.
     *  2.0 = 1 block away in any horizontal direction (including diagonal). */
    public static final double INTERACT_RANGE_SQ = 2.0;

    /** If the villager hasn't reached its target in this many checks, give up.
     *  At FARMING_TICK_INTERVAL=3, 100 checks ≈ 300 ticks ≈ 15 seconds. */
    public static final int STUCK_TIMEOUT_TICKS = 100;

    /**
     * Minimum dot-product between the villager's look direction and the direction
     * to a candidate block for it to be considered "in front".
     * cos(60°) = 0.5 → a 120° forward cone.
     */
    private static final double FORWARD_CONE_DOT = 0.5;

    // Map of seed items to the crop blocks they plant
    private static final Map<Item, Block> SEED_TO_CROP = new LinkedHashMap<>();
    // Reverse map: crop block → seed item
    private static final Map<Block, Item> CROP_TO_SEED = new HashMap<>();
    // Map of crop blocks to their max age property
    private static final Map<Block, IntegerProperty> CROP_AGE_PROPERTIES = new HashMap<>();
    // Map of crop blocks to their max age value
    private static final Map<Block, Integer> CROP_MAX_AGE = new HashMap<>();

    private static final Random RANDOM = new Random();

    static {
        SEED_TO_CROP.put(Items.WHEAT_SEEDS, Blocks.WHEAT);
        SEED_TO_CROP.put(Items.CARROT, Blocks.CARROTS);
        SEED_TO_CROP.put(Items.POTATO, Blocks.POTATOES);
        SEED_TO_CROP.put(Items.BEETROOT_SEEDS, Blocks.BEETROOTS);

        // Build reverse map
        for (Map.Entry<Item, Block> entry : SEED_TO_CROP.entrySet()) {
            CROP_TO_SEED.put(entry.getValue(), entry.getKey());
        }

        CROP_AGE_PROPERTIES.put(Blocks.WHEAT, CropsBlock.AGE);
        CROP_AGE_PROPERTIES.put(Blocks.CARROTS, CropsBlock.AGE);
        CROP_AGE_PROPERTIES.put(Blocks.POTATOES, CropsBlock.AGE);
        CROP_AGE_PROPERTIES.put(Blocks.BEETROOTS, BeetrootBlock.AGE);

        CROP_MAX_AGE.put(Blocks.WHEAT, 7);
        CROP_MAX_AGE.put(Blocks.CARROTS, 7);
        CROP_MAX_AGE.put(Blocks.POTATOES, 7);
        CROP_MAX_AGE.put(Blocks.BEETROOTS, 3);
    }

    // ---------------------------------------------------------------
    //  Target-finding (returns the single closest match in the
    //  villager's forward cone, or null)
    // ---------------------------------------------------------------

    /**
     * Find the nearest mature crop that is in front of the villager.
     * @param headYaw the villager's head yaw in degrees (from {@code villager.yHeadRot})
     * @return the BlockPos of the closest visible mature crop, or null
     */
    public static BlockPos findNearestMatureCrop(ServerWorld world, BlockPos center, float headYaw) {
        double lookX = lookDirX(headYaw);
        double lookZ = lookDirZ(headYaw);

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!isInForwardCone(center, pos, lookX, lookZ)) continue;
                    if (isMatureCrop(world, pos)) {
                        double distSq = pos.distSqr(center);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            best = pos;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * Find the nearest empty farmland that is in front of the villager.
     * @param headYaw the villager's head yaw in degrees (from {@code villager.yHeadRot})
     * @return the BlockPos of the closest visible empty farmland, or null
     */
    public static BlockPos findNearestEmptyFarmland(ServerWorld world, BlockPos center, float headYaw) {
        double lookX = lookDirX(headYaw);
        double lookZ = lookDirZ(headYaw);

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!isInForwardCone(center, pos, lookX, lookZ)) continue;
                    if (isEmptyFarmland(world, pos)) {
                        double distSq = pos.distSqr(center);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            best = pos;
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---------------------------------------------------------------
    //  Full 360° scans (used when already in farming state)
    //  Returns candidates sorted by distance so the caller can
    //  iterate and pick the first one that is path-reachable.
    // ---------------------------------------------------------------

    /**
     * Find all mature crops in any direction (full 360°), sorted nearest-first.
     */
    public static List<BlockPos> findMatureCropsSorted(ServerWorld world, BlockPos center) {
        List<BlockPos> results = new ArrayList<>();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (isMatureCrop(world, pos)) {
                        results.add(pos.immutable());
                    }
                }
            }
        }
        results.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return results;
    }

    /**
     * Find all empty farmland in any direction (full 360°), sorted nearest-first.
     */
    public static List<BlockPos> findEmptyFarmlandSorted(ServerWorld world, BlockPos center) {
        List<BlockPos> results = new ArrayList<>();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (isEmptyFarmland(world, pos)) {
                        results.add(pos.immutable());
                    }
                }
            }
        }
        results.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return results;
    }

    // ---------------------------------------------------------------
    //  Forward-cone helpers
    // ---------------------------------------------------------------

    /** X component of the horizontal look direction for a given yaw (degrees). */
    private static double lookDirX(float yawDeg) {
        return -Math.sin(Math.toRadians(yawDeg));
    }

    /** Z component of the horizontal look direction for a given yaw (degrees). */
    private static double lookDirZ(float yawDeg) {
        return Math.cos(Math.toRadians(yawDeg));
    }

    /**
     * Check whether {@code target} is inside the forward cone defined by the
     * look direction (lookX, lookZ) originating from {@code origin}.
     * Blocks at the same position as the origin are always considered visible.
     */
    private static boolean isInForwardCone(BlockPos origin, BlockPos target,
                                            double lookX, double lookZ) {
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();
        double lenSq = dx * dx + dz * dz;
        if (lenSq < 1.0) return true; // same block or adjacent — always visible

        double len = Math.sqrt(lenSq);
        double dot = (dx / len) * lookX + (dz / len) * lookZ;
        return dot >= FORWARD_CONE_DOT;
    }

    // ---------------------------------------------------------------
    //  Block-level checks
    // ---------------------------------------------------------------

    /** Check whether the block at pos is a fully-grown crop. */
    public static boolean isMatureCrop(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!CROP_AGE_PROPERTIES.containsKey(block)) return false;
        IntegerProperty ageProp = CROP_AGE_PROPERTIES.get(block);
        int maxAge = CROP_MAX_AGE.get(block);
        return state.getValue(ageProp) >= maxAge;
    }

    /** Check whether the block at pos is farmland with air above it. */
    public static boolean isEmptyFarmland(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof FarmlandBlock)) return false;
        BlockPos above = pos.above();
        return world.getBlockState(above).isAir(world, above);
    }

    // ---------------------------------------------------------------
    //  Single-block actions (called only when villager is in range)
    // ---------------------------------------------------------------

    /**
     * Harvest ONE mature crop at the given position.
     * Breaks the block and collects dropped items into the agent's inventory.
     * @return the crop Block that was harvested, or null if nothing was harvested
     */
    public static Block harvestBlockAt(VillagerEntity villager, ServerWorld world,
                                          VillagerAgentData agent, BlockPos cropPos) {
        BlockState state = world.getBlockState(cropPos);
        Block block = state.getBlock();

        if (!CROP_AGE_PROPERTIES.containsKey(block)) {
            LOGGER.debug("Block at " + cropPos + " is not a crop, skipping harvest");
            return null;
        }

        // Break the crop — drops items into the world.
        // Items will be picked up by the general item attraction system.
        world.destroyBlock(cropPos, true);

        agent.addMemory("Harvested " + block.getRegistryName() + " at " + cropPos);
        LOGGER.info(agent.getName() + " harvested " + block.getRegistryName() + " at " + cropPos);
        return block;
    }

    /**
     * Plant ONE seed on the farmland block at the given position.
     * The crop is placed on the block above the farmland.
     * @return true if a seed was planted
     */
    public static boolean plantSeedAt(VillagerEntity villager, ServerWorld world,
                                       VillagerAgentData agent, BlockPos farmlandPos) {
        BlockPos plantPos = farmlandPos.above();

        // Validate the spot is still valid
        if (!world.getBlockState(plantPos).isAir(world, plantPos)) return false;
        if (!(world.getBlockState(farmlandPos).getBlock() instanceof FarmlandBlock)) return false;

        // Try each seed type the villager has
        for (Map.Entry<Item, Block> entry : SEED_TO_CROP.entrySet()) {
            Item seedItem = entry.getKey();
            Block cropBlock = entry.getValue();

            ItemStack seedStack = new ItemStack(seedItem);
            int count = agent.getInventory().countItem(seedStack);
            if (count <= 0) continue;

            // Place the crop at age 0
            world.setBlock(plantPos, cropBlock.defaultBlockState(), 3);

            // Remove one seed from inventory
            agent.getInventory().removeItem(seedStack, 1);

            agent.addMemory("Planted " + seedItem.getRegistryName() + " at " + plantPos);
            LOGGER.info(agent.getName() + " planted " + seedItem.getRegistryName() + " at " + plantPos);
            return true;
        }

        LOGGER.debug(agent.getName() + " has no seeds to plant");
        return false;
    }

    /**
     * Plant a specific crop type on the farmland block at the given position.
     * Falls back to any available seed if the villager doesn't have the requested one.
     * @param cropBlock the desired crop block to plant (e.g. Blocks.WHEAT), or null for any
     * @return true if a seed was planted
     */
    public static boolean plantSpecificCropAt(VillagerEntity villager, ServerWorld world,
                                               VillagerAgentData agent, BlockPos farmlandPos,
                                               Block cropBlock) {
        BlockPos plantPos = farmlandPos.above();

        // Validate the spot is still valid
        if (!world.getBlockState(plantPos).isAir(world, plantPos)) return false;
        if (!(world.getBlockState(farmlandPos).getBlock() instanceof FarmlandBlock)) return false;

        // Try the requested crop type first
        if (cropBlock != null) {
            Item seedItem = CROP_TO_SEED.get(cropBlock);
            if (seedItem != null) {
                ItemStack seedStack = new ItemStack(seedItem);
                if (agent.getInventory().countItem(seedStack) > 0) {
                    world.setBlock(plantPos, cropBlock.defaultBlockState(), 3);
                    agent.getInventory().removeItem(seedStack, 1);
                    agent.addMemory("Planted " + seedItem.getRegistryName() + " at " + plantPos);
                    LOGGER.info(agent.getName() + " planted " + seedItem.getRegistryName() + " at " + plantPos);
                    return true;
                }
            }
        }

        // Fallback: plant any seed the villager has
        return plantSeedAt(villager, world, agent, farmlandPos);
    }

    /**
     * Find the crop type growing on an adjacent farmland block (N/S/E/W neighbors).
     * Checks the block above each neighboring farmland for a known crop.
     * @param farmlandPos the farmland position to check neighbors of
     * @return the crop Block found adjacent, or null if none
     */
    public static Block findAdjacentCropType(ServerWorld world, BlockPos farmlandPos) {
        BlockPos[] neighbors = {
            farmlandPos.north(), farmlandPos.south(),
            farmlandPos.east(), farmlandPos.west()
        };

        for (BlockPos neighbor : neighbors) {
            // Crops sit above farmland
            BlockPos cropPos = neighbor.above();
            BlockState state = world.getBlockState(cropPos);
            Block block = state.getBlock();
            if (CROP_TO_SEED.containsKey(block)) {
                return block;
            }
        }
        return null;
    }

    /**
     * Smart planting: plant on empty farmland, preferring the same crop as adjacent blocks.
     * If no adjacent crop is found, plant a random crop the villager has seeds for.
     * @return true if a seed was planted
     */
    public static boolean plantSmartAt(VillagerEntity villager, ServerWorld world,
                                        VillagerAgentData agent, BlockPos farmlandPos) {
        // Check what crop is growing next to this farmland
        Block adjacentCrop = findAdjacentCropType(world, farmlandPos);

        if (adjacentCrop != null) {
            // Try to plant the same crop; falls back to any seed if unavailable
            return plantSpecificCropAt(villager, world, agent, farmlandPos, adjacentCrop);
        }

        // No adjacent crop — pick a random seed type the villager has
        List<Item> availableSeeds = new ArrayList<>();
        for (Item seedItem : SEED_TO_CROP.keySet()) {
            if (agent.getInventory().countItem(new ItemStack(seedItem)) > 0) {
                availableSeeds.add(seedItem);
            }
        }

        if (availableSeeds.isEmpty()) {
            LOGGER.debug(agent.getName() + " has no seeds to plant");
            return false;
        }

        Item chosenSeed = availableSeeds.get(RANDOM.nextInt(availableSeeds.size()));
        Block cropBlock = SEED_TO_CROP.get(chosenSeed);
        return plantSpecificCropAt(villager, world, agent, farmlandPos, cropBlock);
    }

    /**
     * Check whether the villager has any plantable seeds in inventory.
     */
    public static boolean hasSeeds(VillagerAgentData agent) {
        for (Item seedItem : SEED_TO_CROP.keySet()) {
            if (agent.getInventory().countItem(new ItemStack(seedItem)) > 0) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
}

