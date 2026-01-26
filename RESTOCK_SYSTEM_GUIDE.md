# Villager Restock System Guide

## Overview
Villagers now restock items at their job blocks, respecting item stack limits and persisting across game saves.

## How It Works

### 1. Initial Job Assignment
When a villager gets a job (transitions from no job to having a job):
- `VillagerAgentData.updateProfession()` is called
- Checks if villager had no job before
- Calls `JobStarterItems.giveStarterItems()`
- Gives profession-specific starter items

### 2. Daily Restock at Job Block
Every Minecraft day (24000 ticks):
- `VillagerAgentManager.checkJobBlockRestock()` runs
- Checks if villager is within 2 blocks of their job block
- Uses villager's brain memory (`MemoryModuleType.JOB_SITE`)
- Calls `agent.restockAtJobBlock()`
- Gives items again (respecting stack limits)

### 3. Stack Limit Enforcement
When giving items:
- Gets max stack size for item (e.g., 64 for seeds)
- Counts existing items in inventory
- Calculates available space: `maxStackSize - currentCount`
- Only gives items that fit: `min(starterQuantity, availableSpace)`

### 4. Persistence
When world saves:
- `VillagerAgentData.serializeNBT()` saves `lastRestockTime`
- `VillagerAgentSavedData.save()` saves all agents
- When world loads, `deserializeNBT()` restores `lastRestockTime`

## Profession Starter Items

| Profession | Items |
|-----------|-------|
| Farmer | 32x Wheat Seeds, 1x Iron Hoe, 16x Bone Meal |
| Weaponsmith | 16x Iron Ingot, 32x Sticks, 8x Coal |
| Toolsmith | 16x Iron Ingot, 32x Sticks, 8x Coal |
| Librarian | 32x Paper, 16x Leather, 8x Ink Sac |
| Cleric | 16x Redstone, 8x Glowstone, 8x Lapis Lazuli |
| Fisherman | 1x Fishing Rod, 1x Bucket |
| Shepherd | 1x Shears, 16x White Wool |
| Cartographer | 32x Paper, 1x Compass |

## Key Classes

- **JobStarterItems**: Defines and distributes starter items
- **VillagerAgentData**: Stores agent data, handles NBT serialization
- **VillagerAgentManager**: Manages all agents, detects job block proximity
- **ItemAttractionSystem**: Handles item pickup with attraction mechanics

## Configuration

Restock interval: Once per Minecraft day (24000 ticks)
Job block detection range: 2 blocks
Item pickup range: 8 blocks
Item attraction range: 16 blocks

