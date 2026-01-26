# VillagerAgent Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VillagerAgent Mod                         │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           VillagerAgentData (Core Agent)             │   │
│  │  - UUID, Name, Profession, Personality              │   │
│  │  - Memories (List<String>)                          │   │
│  │  - Goals (List<AgentGoal>)                          │   │
│  │  - Inventory (AgentInventory)                       │   │
│  │  - Relationships, Preferences                       │   │
│  └──────────────────────────────────────────────────────┘   │
│           ▲                    ▲                    ▲         │
│           │                    │                    │         │
│    ┌──────┴──────┐    ┌────────┴────────┐  ┌──────┴──────┐  │
│    │             │    │                 │  │             │  │
│    ▼             ▼    ▼                 ▼  ▼             ▼  │
│  Vision      Goals    Crafting      Inventory   Memories   │
│  System      System   System        System      System     │
│    │             │    │                 │          │       │
│    ▼             ▼    ▼                 ▼          ▼       │
│  Vision      Profession  Crafting    Agent      Record    │
│  System      Goal        Action      Inventory  Observation│
│             Generator    Packet                            │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         VillagerAgentManager (Central Hub)           │  │
│  │  - Manages all agents (Map<UUID, VillagerAgentData>) │  │
│  │  - Periodic AI updates                              │  │
│  │  - Goal processing                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      VillagerAgentSavedData (Persistence)            │  │
│  │  - Saves/loads agent data to NBT                    │  │
│  │  - Persists across world saves                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### Vision System Flow
```
VillagerEventHandler (periodic tick)
    ↓
VillagerVisionSystem.scanSurroundings()
    ├─ scanBlocks() → List<VillagerObservation>
    ├─ scanEntities() → List<VillagerObservation>
    └─ scanItems() → List<VillagerObservation>
    ↓
agent.recordObservation(observation)
    ↓
addMemory("Observed: ...")
    ↓
Stored in agent.memories (max 50)
```

### Goal System Flow
```
VillagerEventHandler (on profession update)
    ↓
agent.updateProfession("farmer")
    ↓
ProfessionGoalGenerator.generateGoalsForProfession()
    ↓
agent.goals = List<AgentGoal>
    ↓
Stored in agent.goals
```

### Crafting System Flow
```
LLM Decision
    ↓
CraftingRequestPacket(villagerId, recipeName, pos)
    ↓
ModNetworking.CHANNEL.sendToServer()
    ↓
CraftingRequestPacket.handle()
    ↓
CraftingAction.executeCraft()
    ├─ RecipeRegistry.getRecipe(name)
    ├─ recipe.canCraft(inventory)
    ├─ recipe.craft(inventory)
    └─ agent.addMemory("Successfully crafted...")
    ↓
Result stored in memory
```

## Class Dependencies

```
VillagerAgentData
├── AgentInventory
├── AgentGoal
├── VillagerObservation (via recordObservation)
└── ProfessionGoalGenerator (via updateProfession)

VillagerVisionSystem
├── VillagerObservation
└── LivingEntity, World (Minecraft)

CraftingAction
├── CraftingRecipe
├── RecipeRegistry
└── VillagerAgentData

CraftingRequestPacket
├── CraftingAction
└── VillagerAgentManager

ProfessionGoalGenerator
└── AgentGoal

RecipeRegistry
└── CraftingRecipe
```

## Configuration Integration

```
ModConfig
├── VILLAGER_PICKUP_INTERVAL (new)
├── ENABLE_AI_AGENTS
├── AGENT_THINK_INTERVAL
├── ENABLE_WORLD_INTERACTION
└── ENABLE_AUTO_PICKUP
```

## Network Packets

```
ModNetworking.CHANNEL
├── ChatMessagePacket (existing)
├── VillagerResponsePacket (existing)
├── TradeRequestPacket (existing)
├── TradeResultPacket (existing)
├── OpenChatPacket (existing)
├── SyncVillagerDataPacket (existing)
└── CraftingRequestPacket (new)
```

## Memory Persistence

```
VillagerAgentSavedData
├── save(CompoundNBT)
│   └── Serializes all agents via serializeNBT()
└── load(CompoundNBT)
    └── Deserializes all agents via deserializeNBT()

VillagerAgentData
├── serializeNBT()
│   ├── Basic data (UUID, name, profession, etc.)
│   ├── Memories (ListNBT)
│   ├── Conversations (ListNBT)
│   └── Inventory (AgentInventory.serializeNBT())
└── deserializeNBT()
    └── Restores all data from NBT
```

