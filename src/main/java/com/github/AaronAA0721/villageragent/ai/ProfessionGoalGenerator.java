package com.github.AaronAA0721.villageragent.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates profession-specific goals for villagers
 */
public class ProfessionGoalGenerator {
    
    /**
     * Generate goals based on villager profession
     */
    public static List<AgentGoal> generateGoalsForProfession(String profession) {
        List<AgentGoal> goals = new ArrayList<>();
        
        switch (profession.toLowerCase()) {
            case "farmer":
                goals.addAll(generateFarmerGoals());
                break;
            case "weaponsmith":
                goals.addAll(generateWeaponsmithGoals());
                break;
            case "toolsmith":
                goals.addAll(generateToolsmithGoals());
                break;
            case "librarian":
                goals.addAll(generateLibrarianGoals());
                break;
            case "cleric":
                goals.addAll(generateClericGoals());
                break;
            default:
                goals.addAll(generateDefaultGoals());
                break;
        }
        
        return goals;
    }
    
    private static List<AgentGoal> generateFarmerGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Find and harvest crops", 8));
        goals.add(new AgentGoal("gather", "Collect seeds", 7));
        goals.add(new AgentGoal("craft", "Plant seeds in farmland", 9));
        goals.add(new AgentGoal("gather", "Collect wheat and other crops", 8));
        return goals;
    }
    
    private static List<AgentGoal> generateWeaponsmithGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Find iron ore", 9));
        goals.add(new AgentGoal("craft", "Smelt iron ore into ingots", 8));
        goals.add(new AgentGoal("craft", "Craft iron swords", 7));
        goals.add(new AgentGoal("craft", "Craft iron axes", 7));
        goals.add(new AgentGoal("gather", "Collect wood for handles", 6));
        return goals;
    }
    
    private static List<AgentGoal> generateToolsmithGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Find iron ore", 9));
        goals.add(new AgentGoal("craft", "Smelt iron ore into ingots", 8));
        goals.add(new AgentGoal("craft", "Craft iron pickaxes", 8));
        goals.add(new AgentGoal("craft", "Craft iron axes", 7));
        goals.add(new AgentGoal("gather", "Collect wood for handles", 6));
        return goals;
    }
    
    private static List<AgentGoal> generateLibrarianGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Collect paper", 7));
        goals.add(new AgentGoal("gather", "Collect leather", 6));
        goals.add(new AgentGoal("craft", "Craft books", 8));
        goals.add(new AgentGoal("socialize", "Share knowledge with other villagers", 5));
        return goals;
    }
    
    private static List<AgentGoal> generateClericGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Find redstone", 7));
        goals.add(new AgentGoal("gather", "Find glowstone", 6));
        goals.add(new AgentGoal("craft", "Brew potions", 8));
        goals.add(new AgentGoal("socialize", "Help other villagers", 6));
        return goals;
    }
    
    private static List<AgentGoal> generateDefaultGoals() {
        List<AgentGoal> goals = new ArrayList<>();
        goals.add(new AgentGoal("gather", "Explore and gather resources", 5));
        goals.add(new AgentGoal("socialize", "Interact with other villagers", 4));
        return goals;
    }
}

