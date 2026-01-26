package com.github.AaronAA0721.villageragent.ai;

import com.github.AaronAA0721.villageragent.config.ModConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stores AI agent data for each villager including personality, memory, goals, and inventory
 */
public class VillagerAgentData {
    private static final Logger LOGGER = LogManager.getLogger();

    private UUID villagerId;
    private String name;
    private String profession;  // The villager's actual Minecraft profession (Farmer, Librarian, etc.)
    private String personality;
    private List<String> conversationHistory;
    private List<String> memories;
    private Map<String, Integer> relationships; // villager UUID -> relationship score
    private List<AgentGoal> goals;
    private AgentInventory inventory;
    private Map<String, Object> preferences; // trading preferences, item values, etc.
    private long lastThinkTime;
    private String currentActivity;
    private VillagerAction currentAction;  // Current action being executed
    private long actionStartTime;  // When the current action started
    private boolean llmGenerationFailed = false;
    private String llmErrorMessage = null;
    private long lastRestockTime = 0;  // Track when villager last restocked at job block

    public VillagerAgentData(UUID villagerId) {
        this.villagerId = villagerId;
        this.profession = "Villager";  // Default, will be updated from actual villager
        this.conversationHistory = new ArrayList<>();
        this.memories = new ArrayList<>();
        this.relationships = new HashMap<>();
        this.goals = new ArrayList<>();
        this.inventory = new AgentInventory();
        this.preferences = new HashMap<>();
        this.lastThinkTime = 0;
        this.currentActivity = "idle";

        // Try to generate name and personality using LLM
        if (shouldUseLLM()) {
            generateWithLLM();
        } else {
            // Fallback to random generation
            this.personality = generateRandomPersonality();
            this.name = generateRandomName();
        }
    }

    /**
     * Update the profession from the actual Minecraft villager entity
     * Only gives starter items when transitioning from no job to having a job
     */
    public void updateProfession(String newProfession) {
        if (newProfession != null && !newProfession.isEmpty()) {
            String oldProfession = this.profession;
            boolean hadNoJob = oldProfession == null || oldProfession.isEmpty() ||
                               oldProfession.equalsIgnoreCase("none") ||
                               oldProfession.equalsIgnoreCase("villager");
            boolean gettingJob = !newProfession.equalsIgnoreCase("none") &&
                                 !newProfession.equalsIgnoreCase("villager");

            this.profession = newProfession;

            // Generate profession-specific goals
            this.goals = ProfessionGoalGenerator.generateGoalsForProfession(newProfession);
            LOGGER.info("Generated " + this.goals.size() + " goals for profession: " + newProfession);

            // Only give starter items when getting a job from not having one
            if (hadNoJob && gettingJob) {
                JobStarterItems.giveStarterItems(this);
                LOGGER.info("Gave starter items to " + name + " for new profession: " + newProfession);
                addMemory("Received starter items for new profession: " + newProfession);
            }
        }
    }

    public String getProfession() {
        return profession;
    }

    /**
     * Restock items when villager visits their job block
     * Called when villager reaches their workstation
     */
    public void restockAtJobBlock() {
        if (profession == null || profession.isEmpty()) {
            return;
        }

        JobStarterItems.giveStarterItems(this);
        LOGGER.info(name + " restocked items at job block for profession: " + profession);
        addMemory("Restocked items at job block");
    }
    
    /**
     * Check if we should use LLM for generation
     */
    private boolean shouldUseLLM() {
        return ModConfig.ENABLE_AI_AGENTS.get();
    }

    /**
     * Generate name and personality using LLM
     */
    private void generateWithLLM() {
        String apiKey = ModConfig.LLM_API_KEY.get();

        // Check if API key is configured
        if (apiKey == null || apiKey.trim().isEmpty()) {
            useFallbackGeneration("API key not configured. Please set llmApiKey in config file.");
            return;
        }

        String systemPrompt = "You are a creative assistant that generates unique medieval villager characters. " +
                "Respond ONLY with a JSON object in this exact format: {\"name\":\"VillagerName\",\"personality\":\"personality description\"}. " +
                "The name should be a single medieval-style first name. " +
                "The personality should be a short phrase (3-6 words) describing their character traits.";

        String userPrompt = "Generate a unique villager character with a medieval name and interesting personality.";

        try {
            CompletableFuture<String> future = LLMService.queryLLM(systemPrompt, userPrompt);
            String response = future.get(10, TimeUnit.SECONDS); // 10 second timeout

            LOGGER.info("LLM Response received: " + response);

            // Parse the JSON response
            if (response.contains("{") && response.contains("}")) {
                String json = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
                LOGGER.info("Extracted JSON: " + json);

                // Simple JSON parsing (avoiding external libraries)
                String nameValue = extractJsonValue(json, "name");
                String personalityValue = extractJsonValue(json, "personality");

                LOGGER.info("Parsed name: " + nameValue + ", personality: " + personalityValue);

                if (nameValue != null && personalityValue != null && !nameValue.isEmpty() && !personalityValue.isEmpty()) {
                    this.name = nameValue;
                    this.personality = personalityValue;
                    LOGGER.info("Generated villager via LLM: " + name + " - " + personality);
                    return;
                }
            }

            // If parsing failed, use fallback
            LOGGER.warn("Failed to parse LLM response: " + response);
            useFallbackGeneration("Invalid response format from LLM. Response: " + response.substring(0, Math.min(100, response.length())));

        } catch (Exception e) {
            LOGGER.error("Error generating villager with LLM: " + e.getMessage(), e);
            useFallbackGeneration("Failed to connect to LLM: " + e.getMessage());
        }
    }

    /**
     * Simple JSON value extractor
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return null;

            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) return null;

            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;

            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Use fallback random generation when LLM fails
     */
    private void useFallbackGeneration(String errorMessage) {
        this.llmGenerationFailed = true;
        this.llmErrorMessage = errorMessage;
        this.personality = generateRandomPersonality();
        this.name = generateRandomName();
    }

    private String generateRandomPersonality() {
        String[] traits = {
            "friendly and generous",
            "shrewd and business-minded",
            "cautious and reserved",
            "adventurous and bold",
            "wise and thoughtful",
            "cheerful and optimistic",
            "grumpy but fair",
            "curious and inquisitive"
        };
        return traits[new Random().nextInt(traits.length)];
    }

    private String generateRandomName() {
        String[] names = {
            "Aldric", "Beatrice", "Cedric", "Diana", "Edmund", "Fiona",
            "Gregory", "Helena", "Isaac", "Juliana", "Kenneth", "Lydia",
            "Marcus", "Natalia", "Oliver", "Penelope", "Quentin", "Rosalind"
        };
        return names[new Random().nextInt(names.length)];
    }
    
    // Getters and setters
    public UUID getVillagerId() { return villagerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public List<String> getConversationHistory() { return conversationHistory; }
    public List<String> getMemories() { return memories; }
    public Map<String, Integer> getRelationships() { return relationships; }
    public List<AgentGoal> getGoals() { return goals; }
    public AgentInventory getInventory() { return inventory; }
    public Map<String, Object> getPreferences() { return preferences; }
    public long getLastThinkTime() { return lastThinkTime; }
    public void setLastThinkTime(long time) { this.lastThinkTime = time; }
    public String getCurrentActivity() { return currentActivity; }
    public void setCurrentActivity(String activity) { this.currentActivity = activity; }
    public VillagerAction getCurrentAction() { return currentAction; }
    public void setCurrentAction(VillagerAction action) {
        this.currentAction = action;
        this.actionStartTime = System.currentTimeMillis();
    }
    public long getActionStartTime() { return actionStartTime; }
    public boolean hasLLMGenerationFailed() { return llmGenerationFailed; }
    public String getLLMErrorMessage() { return llmErrorMessage; }
    public long getLastRestockTime() { return lastRestockTime; }
    public void setLastRestockTime(long time) { this.lastRestockTime = time; }
    
    public void addMemory(String memory) {
        memories.add(memory);
        if (memories.size() > 50) { // Keep only recent memories
            memories.remove(0);
        }
    }
    
    public void addConversation(String conversation) {
        conversationHistory.add(conversation);
        if (conversationHistory.size() > 20) {
            conversationHistory.remove(0);
        }
    }

    public void recordObservation(VillagerObservation observation) {
        addMemory("Observed: " + observation.toString());
    }

    /**
     * Build an action request for the LLM
     * Called after each task is completed to decide what to do next
     */
    public ActionRequest buildActionRequest() {
        ActionRequest request = new ActionRequest(villagerId, name, profession, personality);

        // Set current inventory
        java.util.List<String> inventoryItems = new java.util.ArrayList<>();
        for (net.minecraft.item.ItemStack stack : inventory.getItems()) {
            if (!stack.isEmpty()) {
                inventoryItems.add(stack.getCount() + "x " + stack.getItem().getRegistryName());
            }
        }
        request.setInventoryItems(inventoryItems);

        // Set recent observations (last 10)
        java.util.List<String> recentObs = new java.util.ArrayList<>();
        int start = Math.max(0, memories.size() - 10);
        for (int i = start; i < memories.size(); i++) {
            if (memories.get(i).startsWith("Observed:")) {
                recentObs.add(memories.get(i));
            }
        }
        request.setRecentObservations(recentObs);

        // Set goals
        java.util.List<String> goalStrings = new java.util.ArrayList<>();
        for (AgentGoal goal : goals) {
            goalStrings.add(goal.toString());
        }
        request.setGoals(goalStrings);

        // Set memories
        request.setMemories(new java.util.ArrayList<>(memories));

        // Set available recipes for this profession
        java.util.List<CraftingRecipe> profRecipes = RecipeRegistry.getRecipesForProfession(profession);
        java.util.List<String> recipeNames = new java.util.ArrayList<>();
        for (CraftingRecipe recipe : profRecipes) {
            recipeNames.add(recipe.getName() + " (requires: " + recipe.getWorkstationType() + ")");
        }
        request.setAvailableRecipes(recipeNames);

        // Set available actions
        java.util.List<String> actions = new java.util.ArrayList<>();
        actions.add("CRAFT - Make an item at a workstation");
        actions.add("HARVEST - Gather crops or resources");
        actions.add("GROW - Plant seeds or grow crops");
        actions.add("IDLE - Rest for a while");
        actions.add("MOVE - Go to a location");
        actions.add("GATHER - Pick up items from ground");
        request.setAvailableActions(actions);

        return request;
    }

    public void updateRelationship(String otherVillagerId, int change) {
        int current = relationships.getOrDefault(otherVillagerId, 0);
        relationships.put(otherVillagerId, Math.max(-100, Math.min(100, current + change)));
    }

    /**
     * Generate a chat response using LLM
     * @param playerName The name of the player talking to the villager
     * @param playerMessage The message from the player (null for greeting)
     * @return CompletableFuture with the villager's response
     */
    public CompletableFuture<String> generateChatResponse(String playerName, String playerMessage) {
        String systemPrompt = "You are " + name + ", a " + profession + " in a medieval Minecraft village. " +
                "Your job/profession is: " + profession + ". " +
                "Your personality: " + personality + ". " +
                "Respond in character as this villager. Keep responses short (1-2 sentences). " +
                "Be friendly but stay in character. Don't break the fourth wall. " +
                "Your responses should reflect your profession - for example, a Farmer talks about crops, " +
                "a Librarian about books, a Blacksmith about tools and armor.";

        // Build context from recent memories
        StringBuilder context = new StringBuilder();
        if (!memories.isEmpty()) {
            context.append("Recent memories: ");
            int start = Math.max(0, memories.size() - 3);
            for (int i = start; i < memories.size(); i++) {
                context.append(memories.get(i)).append(". ");
            }
        }

        String userPrompt;
        if (playerMessage == null || playerMessage.isEmpty()) {
            userPrompt = context + "A player named " + playerName + " approaches you. Greet them as a " + profession + ".";
        } else {
            userPrompt = context + "Player " + playerName + " says: \"" + playerMessage + "\". Respond in character as a " + profession + ".";
        }

        return LLMService.queryLLM(systemPrompt, userPrompt)
                .thenApply(response -> {
                    // Store the conversation
                    addConversation(playerName + ": " + (playerMessage != null ? playerMessage : "[greeting]"));
                    addConversation(name + ": " + response);
                    return response;
                })
                .exceptionally(e -> {
                    LOGGER.error("Error generating chat response: " + e.getMessage());
                    return "Hmm... I seem to have lost my train of thought.";
                });
    }

    // NBT serialization for saving/loading
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUUID("VillagerId", villagerId);
        nbt.putString("Name", name);
        nbt.putString("Profession", profession);
        nbt.putString("Personality", personality);
        nbt.putString("CurrentActivity", currentActivity);
        nbt.putLong("LastThinkTime", lastThinkTime);
        nbt.putLong("LastRestockTime", lastRestockTime);  // Save restock time

        // Save memories
        ListNBT memoriesNBT = new ListNBT();
        for (String memory : memories) {
            CompoundNBT memNBT = new CompoundNBT();
            memNBT.putString("Memory", memory);
            memoriesNBT.add(memNBT);
        }
        nbt.put("Memories", memoriesNBT);

        // Save conversation history
        ListNBT conversationsNBT = new ListNBT();
        for (String conv : conversationHistory) {
            CompoundNBT convNBT = new CompoundNBT();
            convNBT.putString("Conversation", conv);
            conversationsNBT.add(convNBT);
        }
        nbt.put("Conversations", conversationsNBT);

        // Save inventory
        nbt.put("Inventory", inventory.serializeNBT());

        return nbt;
    }

    public void deserializeNBT(CompoundNBT nbt) {
        this.villagerId = nbt.getUUID("VillagerId");
        this.name = nbt.getString("Name");
        this.profession = nbt.contains("Profession") ? nbt.getString("Profession") : "Villager";
        this.personality = nbt.getString("Personality");
        this.currentActivity = nbt.getString("CurrentActivity");
        this.lastThinkTime = nbt.getLong("LastThinkTime");
        this.lastRestockTime = nbt.contains("LastRestockTime") ? nbt.getLong("LastRestockTime") : 0;  // Load restock time

        // Load memories
        ListNBT memoriesNBT = nbt.getList("Memories", 10);
        memories.clear();
        for (int i = 0; i < memoriesNBT.size(); i++) {
            CompoundNBT memNBT = memoriesNBT.getCompound(i);
            memories.add(memNBT.getString("Memory"));
        }

        // Load conversations
        ListNBT conversationsNBT = nbt.getList("Conversations", 10);
        conversationHistory.clear();
        for (int i = 0; i < conversationsNBT.size(); i++) {
            CompoundNBT convNBT = conversationsNBT.getCompound(i);
            conversationHistory.add(convNBT.getString("Conversation"));
        }

        // Load inventory
        if (nbt.contains("Inventory")) {
            inventory.deserializeNBT(nbt.getCompound("Inventory"));
        }
    }
}

