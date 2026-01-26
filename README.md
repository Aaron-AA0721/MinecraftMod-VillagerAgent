# 🤖 VillagerAgent - AI-Powered Minecraft Villagers

Transform Minecraft villagers into intelligent AI agents with personalities, memories, and the ability to interact with the world using Large Language Models!

## ✨ Features

### ✅ Currently Implemented

- **🎭 Unique Personalities**: 8 different personality types (friendly, shrewd, cautious, adventurous, wise, cheerful, grumpy, curious)
- **📝 Memory System**: Villagers remember the last 50 events and 20 conversations
- **🎯 Goal System**: Villagers autonomously generate and pursue goals (gather, craft, trade, socialize)
- **🎒 Custom Inventory**: Each villager has a 27-slot inventory to store items
- **🤝 Relationship Tracking**: Villagers track relationships with other villagers (-100 to +100)
- **💬 Player Interaction**: Sneak + right-click to talk to villagers
- **🧠 LLM Integration**: OpenAI and Anthropic API support for intelligent responses
- **⚙️ Full Configuration**: Customize all aspects via config file

### 🚧 Coming Soon

- **🌾 World Interaction**: Farming, harvesting, crafting, building
- **💬 Villager-to-Villager Chat**: AI-powered conversations between villagers
- **💰 Dynamic Trading**: LLM-based price negotiation and persuasion
- **🎨 Custom Chat GUI**: Beautiful interface for player-villager conversations
- **🏰 Village Coordination**: Villagers work together on tasks

## 📦 Installation

1. Download the mod JAR file
2. Place it in your Minecraft `mods` folder
3. Launch Minecraft with Forge 36.2.42 (Minecraft 1.16.5)
4. Configure your LLM API key (see Configuration below)

## ⚙️ Configuration

After first launch, edit `config/villageragent-common.toml`:

```toml
[LLM Settings]
    # API type: "openai" or "anthropic"
    llm_api_type = "openai"
    
    # Your API key from OpenAI or Anthropic
    llm_api_key = "sk-your-api-key-here"
    
    # API endpoint URL
    llm_api_url = "https://api.openai.com/v1/chat/completions"
    
    # Model to use (e.g., "gpt-3.5-turbo", "gpt-4", "claude-3-sonnet-20240229")
    llm_model = "gpt-3.5-turbo"
    
    # Maximum tokens for responses (50-1000)
    llm_max_tokens = 150
    
    # Temperature for creativity (0.0-2.0)
    llm_temperature = 0.7

[Agent Behavior]
    # Enable/disable AI agents
    enable_ai_agents = true
    
    # Ticks between AI updates (20 ticks = 1 second)
    agent_think_interval = 100
    
    # Enable villager-to-villager chat
    enable_villager_chat = true
    
    # Enable world interaction (farming, crafting, etc.)
    enable_world_interaction = true
```

### Getting an API Key

**OpenAI:**
1. Go to https://platform.openai.com/
2. Sign up or log in
3. Navigate to API Keys
4. Create a new secret key
5. Copy and paste into config

**Anthropic:**
1. Go to https://console.anthropic.com/
2. Sign up or log in
3. Navigate to API Keys
4. Create a new key
5. Copy and paste into config
6. Change `llm_api_url` to `https://api.anthropic.com/v1/messages`
7. Change `llm_model` to `claude-3-sonnet-20240229` or similar

## 🎮 How to Use

### Talking to Villagers

1. Find a villager in the world
2. **Sneak + Right-Click** on the villager
3. The villager will greet you with their personality
4. (Full chat GUI coming soon!)

### Observing AI Behavior

- Check the Minecraft logs to see:
  - AI agent creation
  - Goal generation
  - Memory updates
  - Villager activities

### Example Log Output

```
[VillagerAgent] Creating new AI agent for villager: 12345678-1234-1234-1234-123456789abc
[VillagerAgent] Generated new goal for Beatrice: Gather wheat (Priority: 7)
[VillagerAgent] Beatrice: New memory - Talked with player Steve
```

## 🏗️ Architecture

```
src/main/java/com/gitlab/AaronAA0721/villageragent/
├── Villageragent.java              # Main mod class
├── ai/
│   ├── VillagerAgentData.java      # AI agent data structure
│   ├── AgentGoal.java              # Goal system
│   ├── AgentInventory.java         # Custom inventory
│   ├── VillagerAgentManager.java   # Central AI controller
│   └── LLMService.java             # LLM API integration
├── config/
│   └── ModConfig.java              # Configuration system
└── events/
    └── VillagerEventHandler.java   # Event handling
```

## 🛠️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/Aaron-AA0721/MinecraftMod-VillagerAgent.git
cd VillagerAgent

# Build with Gradle (requires Gradle 8.8+)
gradle build

# Or use your IDE (IntelliJ IDEA recommended)
# Import as Gradle project and run build task
```

### Requirements

- Java 8 JDK
- Minecraft 1.16.5
- Forge 36.2.42
- Gradle 8.8+

## 📝 Roadmap

- [x] Core AI agent system
- [x] Personality and memory
- [x] Goal system
- [x] Custom inventory
- [x] LLM integration (OpenAI/Anthropic)
- [x] Basic player interaction
- [ ] Advanced LLM-based goal generation
- [ ] Custom chat GUI
- [ ] World interaction (farming, crafting)
- [ ] Villager-to-villager communication
- [ ] Dynamic trading system
- [ ] Pathfinding and navigation
- [ ] Village coordination

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests.

## 📄 License

All Rights Reserved

## 🙏 Acknowledgments

- Minecraft Forge team
- OpenAI and Anthropic for LLM APIs
- The Minecraft modding community

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Made with ❤️ for the Minecraft community**

