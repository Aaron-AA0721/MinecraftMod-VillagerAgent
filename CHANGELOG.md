# Changelog

All notable changes to the VillagerAgent mod will be documented in this file.

## [1.0.0] - 2026-01-19

### Added
- Initial release of VillagerAgent mod
- Core AI agent system with personality and memory
- 8 unique personality types for villagers
- 18 random names for villagers
- Memory system (stores last 50 memories)
- Conversation history (tracks last 20 conversations)
- Relationship tracking between villagers (-100 to +100)
- Goal system with 5 goal types (gather, craft, trade, build, socialize)
- Priority-based goal execution (1-10 scale)
- Custom 27-slot inventory for each villager
- Smart item stacking in inventory
- NBT serialization for persistent data
- LLM integration with OpenAI API
- LLM integration with Anthropic API
- Async LLM request processing
- Configuration system with TOML file
- Player interaction (Sneak + Right-click to talk)
- Event handling for villager lifecycle
- Automatic AI agent creation on villager spawn
- Automatic AI agent removal on villager death
- Periodic AI updates (configurable interval)
- Comprehensive logging system

### Configuration Options
- LLM API type selection (openai/anthropic)
- LLM API key configuration
- LLM API URL customization
- LLM model selection
- Max tokens for responses (50-1000)
- Temperature control (0.0-2.0)
- Enable/disable AI agents
- Configurable think interval
- Enable/disable villager chat
- Enable/disable world interaction

### Technical Details
- Minecraft version: 1.16.5
- Forge version: 36.2.42
- Java version: 8
- Total lines of code: ~900+
- 8 Java source files
- 3 documentation files

### Fixed
- JsonParser compatibility issue with Minecraft 1.16.5's Gson version
  - Changed from `JsonParser.parseString()` (Gson 2.8.6+) to `new JsonParser().parse()` (older Gson)
  - Affects both OpenAI and Anthropic API integration methods
- World.getAllEntities() method not available in Minecraft 1.16.5
  - Changed to use ServerWorld.getEntity(UUID) for entity lookup
  - More efficient and compatible with Minecraft 1.16.5 API
- Package name corrected from com.gitlab to com.github
  - All Java files migrated to correct package structure
  - Matches GitHub repository hosting

### Known Issues
- World interaction features are stubs (not yet implemented)
- Villager-to-villager chat not yet implemented
- Dynamic trading system not yet implemented
- Custom chat GUI not yet implemented
- Goal generation is currently random (LLM integration planned)

### Documentation
- README.md - Complete project documentation
- QUICKSTART.md - 5-minute setup guide
- IMPLEMENTATION_GUIDE.md - Technical implementation details
- CHANGELOG.md - Version history

### Future Plans
- Advanced LLM-based goal generation
- Custom chat GUI for player-villager conversations
- World interaction (farming, harvesting, crafting)
- Villager-to-villager communication
- Dynamic trading with LLM-based negotiation
- Pathfinding and navigation
- Village coordination features

---

## Version History

### [1.0.0] - 2026-01-19
- Initial release with core AI features

---

## Compatibility Notes

### Minecraft Versions
- **Supported**: 1.16.5
- **Forge**: 36.2.42
- **Java**: 8

### API Compatibility
- **OpenAI**: Compatible with GPT-3.5-turbo, GPT-4, and other chat models
- **Anthropic**: Compatible with Claude 3 Sonnet and other models

### Known Incompatibilities
- Not compatible with Minecraft versions other than 1.16.5
- Requires Forge (not compatible with Fabric)
- Requires Java 8 (not tested with newer Java versions)

---

## Migration Guide

### From No Mod to 1.0.0
1. Install the mod JAR in your mods folder
2. Launch Minecraft once to generate config
3. Edit `config/villageragent-common.toml` with your API key
4. Restart Minecraft
5. Existing villagers will automatically get AI agents

### Configuration Migration
- First-time users: Config file auto-generates with defaults
- API key must be manually added to config file
- All settings have sensible defaults

---

## Credits

- **Developer**: Aaron-AA0721
- **Minecraft Forge**: For the modding framework
- **OpenAI**: For GPT API
- **Anthropic**: For Claude API
- **Minecraft Community**: For inspiration and support

---

## License

All Rights Reserved

---

## Support

For issues, questions, or contributions:
- GitHub: https://github.com/Aaron-AA0721/MinecraftMod-VillagerAgent
- Open an issue for bug reports
- Pull requests welcome for contributions

---

**Last Updated**: 2026-01-19

