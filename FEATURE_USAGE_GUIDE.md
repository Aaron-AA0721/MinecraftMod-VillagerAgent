# VillagerAgent Feature Usage Guide

## 1. Configurable Item Pickup Interval

### Configuration
Edit `config/villageragent-common.toml`:
```toml
[Agent Behavior]
    villager_pickup_interval = 10  # Default: 10 ticks (0.5 seconds)
```

### Usage
- Villagers will attempt to pick up items every N ticks
- Lower values = more frequent pickup attempts
- Range: 1-200 ticks

---

## 2. Vision/Observation System

### How It Works
```java
// Scan surroundings and record observations
List<VillagerObservation> observations = VillagerVisionSystem.scanSurroundings(villager, world);

// Record observation as memory
agent.recordObservation(observation);
```

### Vision Range
- Horizontal: 16 blocks
- Vertical: 8 blocks up/down
- Scans: Blocks, Entities, Items

### Memory Storage
- Observations stored as strings in agent memory
- Max 50 memories (oldest removed when exceeded)
- Format: "Observed: [type] '[name]' at [pos]: [details]"

---

## 3. Profession-Based Goals

### Supported Professions
| Profession | Goals |
|-----------|-------|
| Farmer | Harvest crops, collect seeds, plant seeds |
| Weaponsmith | Find ore, smelt ingots, craft weapons |
| Toolsmith | Find ore, smelt ingots, craft tools |
| Librarian | Collect paper/leather, craft books |
| Cleric | Find redstone, brew potions |
| Default | Explore, socialize |

### Auto-Generation
Goals are automatically generated when profession is set:
```java
agent.updateProfession("farmer");  // Auto-generates farmer goals
```

---

## 4. Crafting System

### Available Recipes
- 2 planks → 4 sticks
- 1 stick + 2 iron ingots → iron sword
- 1 stick + 3 iron ingots → iron pickaxe
- 1 stick + 3 iron ingots → iron axe
- 1 raw iron → 1 iron ingot

### Crafting Execution
```java
// Check available recipes
List<CraftingRecipe> available = CraftingAction.getAvailableRecipes(agent);

// Execute crafting (verified)
boolean success = CraftingAction.executeCraft(agent, "Iron Sword", craftingTablePos);

// Get recipe description for LLM
String description = CraftingAction.getAvailableRecipesDescription(agent);
```

### Recipe Verification
- Recipes are verified before crafting
- Inventory is checked for required items
- Invalid recipes are rejected
- Crafting actions are recorded in memory

---

## 5. LLM-Guided Crafting

### Network Packet
```java
// Send crafting request from LLM
CraftingRequestPacket packet = new CraftingRequestPacket(
    villagerId,
    "Iron Sword",
    craftingTablePos
);
ModNetworking.CHANNEL.sendToServer(packet);
```

### LLM Integration
1. LLM receives available recipes
2. LLM decides what to craft
3. LLM sends CraftingRequestPacket
4. Server verifies and executes crafting
5. Result recorded in agent memory

