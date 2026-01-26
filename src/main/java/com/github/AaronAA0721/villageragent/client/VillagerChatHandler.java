package com.github.AaronAA0721.villageragent.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Client-side handler for villager chat interactions
 */
public class VillagerChatHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static VillagerChatScreen currentChatScreen = null;

    /**
     * Open the chat screen with villager data
     */
    public static void openChatScreen(UUID villagerId, String villagerName, String profession, String personality, List<ItemStack> inventory) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LOGGER.info("Opening chat screen for " + profession + " " + villagerName);

        currentChatScreen = new VillagerChatScreen(villagerId, villagerName, profession, personality, inventory);
        mc.setScreen(currentChatScreen);
    }

    /**
     * Receive a chat response from the villager
     */
    public static void receiveResponse(UUID villagerId, String villagerName, String response) {
        LOGGER.info("Received response from " + villagerName + ": " + response);

        if (currentChatScreen != null && currentChatScreen.getVillagerId().equals(villagerId)) {
            currentChatScreen.addMessage(villagerName, response, false);
        }
    }

    /**
     * Receive a trade result
     */
    public static void receiveTradeResult(UUID villagerId, boolean accepted, String message) {
        LOGGER.info("Trade result: " + (accepted ? "ACCEPTED" : "REJECTED") + " - " + message);

        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;

        // Check if trade screen is open
        if (currentScreen instanceof VillagerTradeScreen) {
            VillagerTradeScreen tradeScreen = (VillagerTradeScreen) currentScreen;
            if (tradeScreen.getVillagerId().equals(villagerId)) {
                tradeScreen.showTradeResult(accepted, message);
                return;
            }
        }

        // Otherwise check chat screen
        if (currentChatScreen != null && currentChatScreen.getVillagerId().equals(villagerId)) {
            currentChatScreen.showTradeResult(accepted, message);
        }
    }

    /**
     * Close the current chat screen
     */
    public static void closeScreen() {
        if (currentChatScreen != null) {
            Minecraft.getInstance().setScreen(null);
            currentChatScreen = null;
        }
    }

    /**
     * Check if chat screen is currently open
     */
    public static boolean isScreenOpen() {
        Minecraft mc = Minecraft.getInstance();
        return currentChatScreen != null &&
               (mc.screen == currentChatScreen || mc.screen instanceof VillagerTradeScreen);
    }

    /**
     * Get the current chat screen (if open)
     */
    public static VillagerChatScreen getCurrentChatScreen() {
        return currentChatScreen;
    }
}

