package com.github.AaronAA0721.villageragent.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // LLM Settings
    public static final ForgeConfigSpec.ConfigValue<String> LLM_API_TYPE;
    public static final ForgeConfigSpec.ConfigValue<String> LLM_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> LLM_API_URL;
    public static final ForgeConfigSpec.ConfigValue<String> LLM_MODEL;
    public static final ForgeConfigSpec.IntValue LLM_MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue LLM_TEMPERATURE;

    // Agent Behavior Settings
    public static final ForgeConfigSpec.BooleanValue ENABLE_AI_AGENTS;
    public static final ForgeConfigSpec.IntValue AGENT_THINK_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VILLAGER_CHAT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WORLD_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_PICKUP;
    public static final ForgeConfigSpec.IntValue VILLAGER_PICKUP_INTERVAL;

    static {
        BUILDER.push("LLM Settings");
        
        LLM_API_TYPE = BUILDER
                .comment("LLM API type to use (openai, anthropic, ollama, gemini)")
                .define("llm_api_type", "openai");
        
        LLM_API_KEY = BUILDER
                .comment("Your LLM API key")
                .define("llm_api_key", "");
        
        LLM_API_URL = BUILDER
                .comment("LLM API endpoint URL")
                .define("llm_api_url", "https://api.openai.com/v1/chat/completions");
        
        LLM_MODEL = BUILDER
                .comment("LLM model to use (e.g., gpt-3.5-turbo, gpt-4, claude-3-sonnet-20240229)")
                .define("llm_model", "gpt-3.5-turbo");
        
        LLM_MAX_TOKENS = BUILDER
                .comment("Maximum tokens for LLM responses")
                .defineInRange("llm_max_tokens", 150, 50, 1000);
        
        LLM_TEMPERATURE = BUILDER
                .comment("LLM temperature (creativity) - 0.0 to 2.0")
                .defineInRange("llm_temperature", 0.7, 0.0, 2.0);
        
        BUILDER.pop();
        
        BUILDER.push("Agent Behavior");
        
        ENABLE_AI_AGENTS = BUILDER
                .comment("Enable AI agents for villagers")
                .define("enable_ai_agents", true);
        
        AGENT_THINK_INTERVAL = BUILDER
                .comment("Ticks between AI agent updates (20 ticks = 1 second)")
                .defineInRange("agent_think_interval", 100, 20, 1200);
        
        ENABLE_VILLAGER_CHAT = BUILDER
                .comment("Enable villager-to-villager chat")
                .define("enable_villager_chat", true);
        
        ENABLE_WORLD_INTERACTION = BUILDER
                .comment("Enable villagers to interact with the world (farming, crafting, etc.)")
                .define("enable_world_interaction", true);

        ENABLE_AUTO_PICKUP = BUILDER
                .comment("Enable villagers to automatically pick up nearby items (experimental)")
                .define("enable_auto_pickup", false);

        VILLAGER_PICKUP_INTERVAL = BUILDER
                .comment("Ticks between villager item pickup attempts (20 ticks = 1 second)")
                .defineInRange("villager_pickup_interval", 10, 1, 200);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}

