package com.github.AaronAA0721.villageragent.ai;

import net.minecraft.block.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.math.AxisAlignedBB;
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
 * 3. When within reach (2 blocks), perform the action on that single block
 * 4. Repeat
 */
public class FarmingAction {
    private static final Logger LOGGER = LogManager.getLogger();

    /** How far the villager can see to find crops/farmland. */
    public static final int SCAN_RADIUS = 5;
    private static final int SCAN_HEIGHT = 2;

    /** The villager must be within this distance (squared) to interact with a block. */
    public static final double INTERACT_RANGE_SQ = 4.0; // 2 blocks squared

    /** If the villager hasn't reached its target in this many ticks, give up. */
    public static final int STUCK_TIMEOUT_TICKS = 100; // ~5 seconds

    /**
     * Minimum dot-product between the villager's look direction and the direction
     * to a candidate block for it to be considered "in front".
     * cos(60°) = 0.5 → a 120° forward cone.
     */
    private static final double FORWARD_CONE_DOT = 0.5;

    // Map of seed items to the crop blocks they plant
    private static final Map<Item, Block> SEED_TO_CROP = new LinkedHashMap<>();
    // Map of crop blocks to their max age property
    private static final Map<Block, IntegerProperty> CROP_AGE_PROPERTIES = new HashMap<>();
    // Map of crop blocks to their max age value
    private static final Map<Block, Integer> CROP_MAX_AGE = new HashMap<>();

    static {
        SEED_TO_CROP.put(Items.WHEAT_SEEDS, Blocks.WHEAT);
        SEED_TO_CROP.put(Items.CARROT, Blocks.CARROTS);
        SEED_TO_CROP.put(Items.POTATO, Blocks.POTATOES);
        SEED_TO_CROP.put(Items.BEETROOT_SEEDS, Blocks.BEETROOTS);

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
    // ---------------------------------------------------------------

    /**
     * Find the nearest mature crop in any direction (full 360°).
     * Used when the villager is already committed to farming an area.
     */
    public static BlockPos findNearestMatureCrop(ServerWorld world, BlockPos center) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
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
     * Find the nearest empty farmland in any direction (full 360°).
     * Used when the villager is already committed to farming an area.
     */
    public static BlockPos findNearestEmptyFarmland(ServerWorld world, BlockPos center) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.offset(x, y, z);
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
     * @return true if the block was harvested
     */
    public static boolean harvestBlockAt(VillagerEntity villager, ServerWorld world,
                                          VillagerAgentData agent, BlockPos cropPos) {
        BlockState state = world.getBlockState(cropPos);
        Block block = state.getBlock();

        if (!CROP_AGE_PROPERTIES.containsKey(block)) {
            LOGGER.debug("Block at " + cropPos + " is not a crop, skipping harvest");
            return false;
        }

        // Break the crop — drops items into the world
        world.destroyBlock(cropPos, true);

        // Pick up dropped items near the broken block
        collectDroppedItems(villager, world, agent, cropPos);

        agent.addMemory("Harvested " + block.getRegistryName() + " at " + cropPos);
        LOGGER.info(agent.getName() + " harvested " + block.getRegistryName() + " at " + cropPos);
        return true;
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

    /**
     * Collect dropped ItemEntities near a position into the villager's inventory.
     */
    private static void collectDroppedItems(VillagerEntity villager, ServerWorld world,
                                             VillagerAgentData agent, BlockPos pos) {
        AxisAlignedBB pickupBox = new AxisAlignedBB(pos).inflate(2.0);
        List<ItemEntity> droppedItems = world.getEntitiesOfClass(ItemEntity.class, pickupBox);

        for (ItemEntity itemEntity : droppedItems) {
            if (!itemEntity.isAlive()) continue;

            ItemStack stack = itemEntity.getItem().copy();
            boolean added = agent.getInventory().addItem(stack);

            if (added) {
                itemEntity.remove();
                LOGGER.debug(agent.getName() + " picked up " + stack.getCount() + "x " +
                        stack.getItem().getRegistryName());
            }
        }
    }
}

