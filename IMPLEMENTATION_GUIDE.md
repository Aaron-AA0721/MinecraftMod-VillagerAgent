# VillagerAgent Mod - Implementation Guide

## 🎮 Overview
An AI-powered Minecraft mod that transforms villagers into intelligent agents with personalities, memories, goals, and the ability to interact with the world and communicate using Large Language Models (LLMs).

## ✅ Phase 1: COMPLETED

### Files Created (7 total)

#### Core AI System
1. **`VillagerAgentData.java`** - AI agent data structure
   - 8 unique personality types
   - Random name generation (18 names)
   - Memory system (last 50 memories)
   - Conversation history (last 20)
   - Relationship tracking (-100 to +100)
   - NBT serialization

2. **`AgentGoal.java`** - Goal system
   - Goal types: gather, craft, trade, build, socialize
   - Priority system (1-10)
   - Target tracking

3. **`AgentInventory.java`** - Custom 27-slot inventory
   - Smart item stacking
   - Add/remove/count operations
   - NBT serialization

4. **`VillagerAgentManager.java`** - Central manager
   - Manages all villager agents
   - Periodic AI updates
   - Goal processing
   - Simple goal generation (will be replaced with LLM)

#### Configuration & Events
5. **`ModConfig.java`** - Configuration system
   - LLM API settings (type, key, URL, model, tokens, temperature)
   - Agent behavior settings
   - Creates `villageragent-common.toml`

6. **`VillagerEventHandler.java`** - Event handling
   - Agent creation on villager spawn
   - Agent removal on villager death
   - Periodic AI updates
   - Player interaction (Sneak + Right-click)

7. **`Villageragent.java`** - Main mod class (updated)
   - Config registration
   - Event handler registration
   - Initialization logging

## 🎯 Current Features

✅ **Personality System** - Each villager gets unique personality and name
✅ **Memory System** - Villagers remember recent events
✅ **Goal System** - Villagers generate and pursue goals
✅ **Custom Inventory** - 27-slot storage
✅ **Event Integration** - Minecraft lifecycle hooks
✅ **Configuration** - Full config file
✅ **Player Interaction** - Sneak + right-click to greet

## 🚀 How to Build & Test

### Prerequisites
- Java 8 JDK
- Minecraft 1.16.5
- Forge 36.2.42
- Gradle (or use IDE)

### Building
```bash
# If you have Gradle installed
gradle build

# Or use your IDE (IntelliJ IDEA / Eclipse)
# Import as Gradle project and run build task
```

### Configuration
After first run, edit `config/villageragent-common.toml`:

```toml
[LLM Settings]
    llm_api_type = "openai"  # or "anthropic"
    llm_api_key = "your-api-key-here"
    llm_api_url = "https://api.openai.com/v1/chat/completions"
    llm_model = "gpt-3.5-turbo"
    llm_max_tokens = 150
    llm_temperature = 0.7

[Agent Behavior]
    enable_ai_agents = true
    agent_think_interval = 100  # ticks (5 seconds)
    enable_villager_chat = true
    enable_world_interaction = true
```

### In-Game Testing
1. Spawn a villager or find one in a village
2. Check logs for AI agent creation
3. Sneak + right-click on villager to see personality
4. Watch logs for goal generation

## 📋 Phase 2: TODO

### High Priority

#### 1. LLM Integration Service
Create `src/main/java/com/gitlab/AaronAA0721/villageragent/ai/LLMService.java`
- OpenAI API integration
- Anthropic API integration
- Ollama local LLM support
- Async request handling
- Error handling and retries

#### 2. Advanced Goal Generation
Replace random goals with LLM-generated contextual goals
- Use personality in prompts
- Consider current situation and memories
- Generate realistic, personality-driven goals

#### 3. Player-Villager Chat GUI
Create custom screen for conversations
- Text input for player
- LLM-generated responses
- Conversation history display
- Negotiation mechanics

### Medium Priority

#### 4. World Interaction System
- **Farming**: Plant, harvest, collect crops
- **Pathfinding**: Navigate to resources
- **Block Interaction**: Break/place blocks
- **Crafting**: Use crafting tables
- **Tool Usage**: Equip and use tools

#### 5. Villager-to-Villager Communication
- Detect nearby villagers
- Generate contextual conversations
- Share information and gossip
- Coordinate activities

#### 6. Dynamic Trading System
- LLM-based price negotiation
- Personality affects willingness
- Relationship affects prices
- Persuasion mechanics

### Low Priority

#### 7. Advanced Features
- Profession-based behavior
- Day/night cycle awareness
- Weather awareness
- Village defense coordination
- Building and construction

## 🏗️ Architecture

```
VillagerAgent Mod
├── Core AI System
│   ├── VillagerAgentData (personality, memory, goals)
│   ├── AgentGoal (task management)
│   ├── AgentInventory (item storage)
│   └── VillagerAgentManager (central controller)
├── LLM Integration (TODO)
│   └── LLMService (API communication)
├── World Interaction (TODO)
│   ├── FarmingAI
│   ├── CraftingAI
│   └── PathfindingAI
├── Communication (TODO)
│   ├── PlayerChatGUI
│   ├── VillagerChat
│   └── TradingSystem
└── Configuration
    ├── ModConfig
    └── VillagerEventHandler
```

## 📝 Next Steps

Choose one of these paths:
1. **Implement LLM Service** - Add actual API integration
2. **Create Chat GUI** - Build player-villager conversation screen
3. **Add World Interaction** - Implement farming and resource gathering
4. **Test Current Build** - Compile and test existing features

## 🐛 Known Issues
- Gradle wrapper missing (needs manual setup)
- LLM integration not yet implemented (goals are random)
- No GUI for chat (only console messages)
- World interaction stubs only

## 📚 Resources
- [Minecraft Forge Documentation](https://docs.minecraftforge.net/)
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [Anthropic API Documentation](https://docs.anthropic.com/)

