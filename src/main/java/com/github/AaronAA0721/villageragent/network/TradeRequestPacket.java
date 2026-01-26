package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.ai.LLMService;
import com.github.AaronAA0721.villageragent.ai.VillagerAgentData;
import com.github.AaronAA0721.villageragent.ai.VillagerAgentManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when player requests a trade
 */
public class TradeRequestPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final UUID villagerId;
    private final ItemStack offerItem1;
    private final ItemStack offerItem2;
    private final ItemStack requestItem1;
    private final ItemStack requestItem2;
    
    public TradeRequestPacket(UUID villagerId, ItemStack offer1, ItemStack offer2, ItemStack request1, ItemStack request2) {
        this.villagerId = villagerId;
        this.offerItem1 = offer1;
        this.offerItem2 = offer2;
        this.requestItem1 = request1;
        this.requestItem2 = request2;
    }
    
    public static void encode(TradeRequestPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeItem(packet.offerItem1);
        buffer.writeItem(packet.offerItem2);
        buffer.writeItem(packet.requestItem1);
        buffer.writeItem(packet.requestItem2);
    }
    
    public static TradeRequestPacket decode(PacketBuffer buffer) {
        return new TradeRequestPacket(
                buffer.readUUID(),
                buffer.readItem(),
                buffer.readItem(),
                buffer.readItem(),
                buffer.readItem()
        );
    }
    
    public static void handle(TradeRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            
            VillagerAgentData agent = VillagerAgentManager.getAgent(packet.villagerId);
            if (agent == null) {
                LOGGER.warn("No agent found for villager: " + packet.villagerId);
                return;
            }
            
            // Build trade description for LLM
            String tradeDescription = buildTradeDescription(packet, agent);
            LOGGER.info("Trade request: " + tradeDescription);
            
            // Ask LLM to evaluate the trade
            evaluateTradeWithLLM(player, agent, packet, tradeDescription);
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static String buildTradeDescription(TradeRequestPacket packet, VillagerAgentData agent) {
        StringBuilder sb = new StringBuilder();

        // What the player is GIVING TO the villager (villager receives)
        sb.append("The player is GIVING YOU: ");
        if (!packet.offerItem1.isEmpty()) {
            sb.append(packet.offerItem1.getCount()).append("x ").append(getItemName(packet.offerItem1));
        }
        if (!packet.offerItem2.isEmpty()) {
            if (!packet.offerItem1.isEmpty()) sb.append(" and ");
            sb.append(packet.offerItem2.getCount()).append("x ").append(getItemName(packet.offerItem2));
        }
        if (packet.offerItem1.isEmpty() && packet.offerItem2.isEmpty()) {
            sb.append("nothing");
        }

        // What the player WANTS FROM the villager (villager must give)
        sb.append("\nIn exchange, the player WANTS FROM YOU: ");
        if (!packet.requestItem1.isEmpty()) {
            sb.append(packet.requestItem1.getCount()).append("x ").append(getItemName(packet.requestItem1));
        }
        if (!packet.requestItem2.isEmpty()) {
            if (!packet.requestItem1.isEmpty()) sb.append(" and ");
            sb.append(packet.requestItem2.getCount()).append("x ").append(getItemName(packet.requestItem2));
        }
        if (packet.requestItem1.isEmpty() && packet.requestItem2.isEmpty()) {
            sb.append("nothing");
        }
        return sb.toString();
    }

    /**
     * Get a readable item name from an ItemStack
     */
    private static String getItemName(ItemStack stack) {
        String regName = stack.getItem().getRegistryName().toString();
        // Convert "minecraft:diamond_hoe" to "diamond hoe"
        if (regName.contains(":")) {
            regName = regName.substring(regName.indexOf(":") + 1);
        }
        return regName.replace("_", " ");
    }

    /**
     * Build a description of the villager's current inventory
     */
    private static String buildInventoryDescription(VillagerAgentData agent) {
        StringBuilder sb = new StringBuilder();
        java.util.Map<String, Integer> itemCounts = new java.util.HashMap<>();

        for (ItemStack stack : agent.getInventory().getItems()) {
            if (!stack.isEmpty()) {
                String itemName = getItemName(stack);
                itemCounts.merge(itemName, stack.getCount(), Integer::sum);
            }
        }

        if (itemCounts.isEmpty()) {
            return "Your inventory is empty.";
        }

        sb.append("Your current inventory: ");
        boolean first = true;
        for (java.util.Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
            first = false;
        }
        return sb.toString();
    }

    private static void evaluateTradeWithLLM(ServerPlayerEntity player, VillagerAgentData agent,
                                              TradeRequestPacket packet, String tradeDescription) {
        String profession = agent.getProfession();
        String inventoryDesc = buildInventoryDescription(agent);

        // Flexible, personality-driven prompt
        String systemPrompt = "You are " + agent.getName() + ", a " + profession + " villager in Minecraft. " +
                "Your personality: " + agent.getPersonality() + ".\n\n" +
                "A player wants to trade with you.\n\n" +
                "YOUR CURRENT INVENTORY (items you own and can trade away):\n" + inventoryDesc + "\n\n" +
                "TRADE VALUE GUIDE:\n" +
                "- Emeralds, gold ingots, and diamonds are valuable currencies.\n" +
                "- Rare items (diamonds, enchanted gear, netherite) are very precious.\n" +
                "- Common blocks (dirt, cobblestone, grass, sand) have little value.\n" +
                "- Food and tools have moderate value depending on quality.\n" +
                "- You can only give away items you actually HAVE.\n\n" +
                "Let your personality guide your decision. Think freely about:\n" +
                "- Is this trade fair in terms of value?\n" +
                "- Do you want or need what they're giving you?\n" +
                "- Does this feel like a good deal for YOU?\n\n" +
                "Respond with EXACTLY: ACCEPT or REJECT followed by a short in-character reason (1 sentence).";

        String userPrompt = "TRADE PROPOSAL:\n" + tradeDescription +
                "\n\nDo you accept this trade? Respond in character, starting with ACCEPT or REJECT.";

        LLMService.queryLLM(systemPrompt, userPrompt).thenAccept(response -> {
            boolean accepted = response.toUpperCase().startsWith("ACCEPT");
            String reason = response.length() > 7 ? response.substring(7).trim() : response;

            // Clean up the reason - remove leading punctuation
            if (reason.startsWith(":") || reason.startsWith("-") || reason.startsWith(".")) {
                reason = reason.substring(1).trim();
            }
            if (reason.startsWith("!")) {
                reason = reason.substring(1).trim();
            }

            LOGGER.info("Trade " + (accepted ? "ACCEPTED" : "REJECTED") + ": " + reason);

            // Execute trade if accepted, otherwise return items to player
            if (accepted) {
                boolean success = executeTrade(player, agent, packet);
                if (!success) {
                    // Trade failed (items not available) - return items to player
                    reason = "Wait, I don't actually have those items. Sorry!";
                    accepted = false;
                    returnItemsToPlayer(player, packet);
                }
            } else {
                // Trade rejected - return the offered items to player
                returnItemsToPlayer(player, packet);
            }

            // Send result to client
            TradeResultPacket resultPacket = new TradeResultPacket(
                    packet.villagerId,
                    accepted,
                    reason
            );
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), resultPacket);
        });
    }

    /**
     * Return offered items to player when trade is rejected or fails
     */
    private static void returnItemsToPlayer(ServerPlayerEntity player, TradeRequestPacket packet) {
        if (!packet.offerItem1.isEmpty()) {
            player.addItem(packet.offerItem1.copy());
            LOGGER.info("Returned to player: " + packet.offerItem1.getCount() + "x " + packet.offerItem1.getItem().getRegistryName());
        }
        if (!packet.offerItem2.isEmpty()) {
            player.addItem(packet.offerItem2.copy());
            LOGGER.info("Returned to player: " + packet.offerItem2.getCount() + "x " + packet.offerItem2.getItem().getRegistryName());
        }
    }

    /**
     * Execute the trade - transfer items between player and villager
     * @return true if trade was successful, false if items weren't available
     */
    private static boolean executeTrade(ServerPlayerEntity player, VillagerAgentData agent, TradeRequestPacket packet) {
        // First verify villager has the requested items
        if (!packet.requestItem1.isEmpty()) {
            if (!agent.getInventory().hasItem(packet.requestItem1, packet.requestItem1.getCount())) {
                LOGGER.warn("Villager doesn't have enough of: " + packet.requestItem1);
                return false;
            }
        }
        if (!packet.requestItem2.isEmpty()) {
            if (!agent.getInventory().hasItem(packet.requestItem2, packet.requestItem2.getCount())) {
                LOGGER.warn("Villager doesn't have enough of: " + packet.requestItem2);
                return false;
            }
        }

        // Player's offered items are already removed from their inventory on client side
        // (they were placed in the sell slots which are separate from inventory)
        // Add player's offer to villager inventory
        if (!packet.offerItem1.isEmpty()) {
            agent.getInventory().addItem(packet.offerItem1.copy());
            LOGGER.info("Villager received: " + packet.offerItem1.getCount() + "x " + packet.offerItem1.getItem().getRegistryName());
        }
        if (!packet.offerItem2.isEmpty()) {
            agent.getInventory().addItem(packet.offerItem2.copy());
            LOGGER.info("Villager received: " + packet.offerItem2.getCount() + "x " + packet.offerItem2.getItem().getRegistryName());
        }

        // Remove requested items from villager and give to player
        if (!packet.requestItem1.isEmpty()) {
            agent.getInventory().removeItem(packet.requestItem1, packet.requestItem1.getCount());
            player.addItem(packet.requestItem1.copy());
            LOGGER.info("Player received: " + packet.requestItem1.getCount() + "x " + packet.requestItem1.getItem().getRegistryName());
        }
        if (!packet.requestItem2.isEmpty()) {
            agent.getInventory().removeItem(packet.requestItem2, packet.requestItem2.getCount());
            player.addItem(packet.requestItem2.copy());
            LOGGER.info("Player received: " + packet.requestItem2.getCount() + "x " + packet.requestItem2.getItem().getRegistryName());
        }

        agent.addMemory("Traded with player " + player.getName().getString());
        return true;
    }
}

