# 🚀 VillagerAgent Quick Start Guide

Get your AI-powered villagers up and running in 5 minutes!

## Step 1: Get an LLM API Key

### Option A: OpenAI (Recommended for beginners)

1. Go to https://platform.openai.com/signup
2. Create an account (you'll get $5 free credit)
3. Navigate to https://platform.openai.com/api-keys
4. Click "Create new secret key"
5. Copy the key (starts with `sk-...`)
6. **Save it somewhere safe!** You won't be able to see it again

### Option B: Anthropic (Alternative)

1. Go to https://console.anthropic.com/
2. Create an account
3. Navigate to API Keys section
4. Create a new key
5. Copy and save the key

## Step 2: Build the Mod

### Using IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. File → Open → Select the `VillagerAgent` folder
3. Wait for Gradle to sync (this may take a few minutes)
4. Open Gradle panel (View → Tool Windows → Gradle)
5. Navigate to: Tasks → build → build
6. Double-click to run the build
7. Find the JAR in `build/libs/villageragent-1.0-SNAPSHOT.jar`

### Using Command Line

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

## Step 3: Install the Mod

1. Make sure you have Minecraft 1.16.5 with Forge 36.2.42 installed
2. Copy `villageragent-1.0-SNAPSHOT.jar` to your Minecraft `mods` folder
3. Launch Minecraft

## Step 4: Configure the Mod

1. Launch Minecraft once to generate the config file
2. Close Minecraft
3. Navigate to `config/villageragent-common.toml`
4. Edit the file:

```toml
[LLM Settings]
    llm_api_type = "openai"
    llm_api_key = "sk-your-actual-api-key-here"  # ← PASTE YOUR KEY HERE
    llm_api_url = "https://api.openai.com/v1/chat/completions"
    llm_model = "gpt-3.5-turbo"
    llm_max_tokens = 150
    llm_temperature = 0.7

[Agent Behavior]
    enable_ai_agents = true
    agent_think_interval = 100
    enable_villager_chat = true
    enable_world_interaction = true
```

5. Save the file

## Step 5: Test It Out!

1. Launch Minecraft
2. Create a new world or load an existing one
3. Find or spawn a villager:
   - Natural village
   - Or use: `/summon minecraft:villager ~ ~ ~`
4. **Sneak + Right-Click** on the villager
5. You should see a greeting with their personality!

### Example Output

```
Beatrice: Hello! I'm friendly and generous.
(Chat GUI coming soon! For now, this is just a greeting.)
```

## Step 6: Check the Logs

Open the Minecraft logs to see the AI in action:

1. In Minecraft launcher, click "Launch Options"
2. Enable "Game Output"
3. Look for lines like:

```
[VillagerAgent] Creating new AI agent for villager: abc123...
[VillagerAgent] Generated new goal for Beatrice: Gather wheat (Priority: 7)
```

## 🎉 You're Done!

Your villagers now have:
- ✅ Unique personalities
- ✅ Memory of events
- ✅ Autonomous goals
- ✅ AI-powered responses (when LLM integration is fully enabled)

## 🐛 Troubleshooting

### "API key not configured" message

- Make sure you pasted your API key correctly in the config
- Check that there are no extra spaces or quotes
- Restart Minecraft after editing the config

### Villagers not responding

- Make sure `enable_ai_agents = true` in config
- Check that you're **sneaking** while right-clicking
- Look at the logs for error messages

### Build errors

- Make sure you have Java 8 JDK installed
- Try running `gradlew clean build` to clean and rebuild
- Check that all files are present in the project

### API errors in logs

- Check your API key is valid
- Make sure you have credits/quota remaining
- Check your internet connection
- For OpenAI: https://platform.openai.com/usage
- For Anthropic: https://console.anthropic.com/

## 💡 Tips

1. **Start with gpt-3.5-turbo** - It's fast and cheap
2. **Monitor your API usage** - Check your dashboard regularly
3. **Adjust think_interval** - Higher values = less frequent AI updates = lower costs
4. **Watch the logs** - They show you what the AI is doing

## 📚 Next Steps

- Read the full [README.md](README.md) for all features
- Check [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) for development details
- Experiment with different personalities and settings
- Join the community and share your experiences!

## 🆘 Need Help?

- Check the logs first
- Read the error messages carefully
- Open an issue on GitHub with:
  - Your Minecraft version
  - Your Forge version
  - The error message
  - Your config file (remove your API key!)

---

**Happy modding! 🎮**

