# VillagerAgent Code Examples

## 1. Using the Vision System

```java
// In VillagerEventHandler or periodic AI update
VillagerEntity villager = ...; // Get villager entity
ServerWorld world = ...; // Get world
VillagerAgentData agent = VillagerAgentManager.getAgent(villager.getUUID());

// Scan surroundings
List<VillagerObservation> observations = 
    VillagerVisionSystem.scanSurroundings(villager, world);

// Record observations as memories
for (VillagerObservation obs : observations) {
    agent.recordObservation(obs);
}

// Later, retrieve memories for LLM context
List<String> memories = agent.getMemories();
```

## 2. Working with Goals

```java
// Goals are auto-generated when profession is set
agent.updateProfession("farmer");

// Access goals
List<AgentGoal> goals = agent.getGoals();
for (AgentGoal goal : goals) {
    System.out.println(goal.getGoalType() + ": " + goal.getDescription());
}

// Manually create a goal
AgentGoal customGoal = new AgentGoal("gather", "Find diamonds", 10);
customGoal.setTargetItem("minecraft:diamond");
customGoal.setTargetQuantity(5);
agent.getGoals().add(customGoal);
```

## 3. Crafting Recipes

```java
// Get available recipes
List<CraftingRecipe> available = 
    CraftingAction.getAvailableRecipes(agent);

// Get recipe description for LLM
String description = 
    CraftingAction.getAvailableRecipesDescription(agent);

// Execute crafting
BlockPos craftingTablePos = new BlockPos(100, 64, 200);
boolean success = CraftingAction.executeCraft(
    agent, 
    "Iron Sword", 
    craftingTablePos
);

if (success) {
    System.out.println("Crafting successful!");
} else {
    System.out.println("Crafting failed - check inventory");
}
```

## 4. LLM Integration Example

```java
// In LLM response handler
String llmResponse = "I should craft an iron sword";

if (llmResponse.contains("craft")) {
    // Extract recipe name from LLM response
    String recipeName = "Iron Sword";
    
    // Send crafting request
    CraftingRequestPacket packet = new CraftingRequestPacket(
        agent.getVillagerId(),
        recipeName,
        craftingTablePos
    );
    ModNetworking.CHANNEL.sendToServer(packet);
}
```

## 5. Configuration Usage

```java
// In event handler or periodic update
int pickupInterval = ModConfig.VILLAGER_PICKUP_INTERVAL.get();

if (tickCounter % pickupInterval == 0) {
    // Attempt to pick up items
    List<ItemEntity> nearbyItems = world.getEntities(
        villager,
        villager.getBoundingBox().inflate(8)
    );
    
    for (ItemEntity itemEntity : nearbyItems) {
        ItemStack stack = itemEntity.getItem();
        if (agent.getInventory().addItem(stack)) {
            itemEntity.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
```

## 6. Memory Management

```java
// Add observation
VillagerObservation obs = new VillagerObservation(
    "block",
    "minecraft:crafting_table",
    new BlockPos(100, 64, 200),
    "Crafting table found"
);
agent.recordObservation(obs);

// Add memory directly
agent.addMemory("I found a crafting table at 100, 64, 200");

// Get all memories
List<String> memories = agent.getMemories();

// Memories are automatically limited to 50
// Oldest memories are removed when limit exceeded
```

## 7. Recipe Registry

```java
// Get all recipes
List<CraftingRecipe> allRecipes = RecipeRegistry.getAllRecipes();

// Get specific recipe
CraftingRecipe swordRecipe = RecipeRegistry.getRecipe("Iron Sword");

// Check if recipe exists
if (swordRecipe != null) {
    System.out.println("Recipe found: " + swordRecipe.getName());
    
    // Check inputs
    for (Map.Entry<Item, Integer> input : swordRecipe.getInputs().entrySet()) {
        System.out.println("Need: " + input.getValue() + "x " + input.getKey());
    }
}
```

## 8. Profession-Based Goals

```java
// Generate goals for a profession
List<AgentGoal> farmerGoals = 
    ProfessionGoalGenerator.generateGoalsForProfession("farmer");

// Goals are sorted by priority (higher = more important)
farmerGoals.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

// Process goals
for (AgentGoal goal : farmerGoals) {
    if (!goal.isCompleted()) {
        // Execute goal
        System.out.println("Working on: " + goal.getDescription());
    }
}
```

