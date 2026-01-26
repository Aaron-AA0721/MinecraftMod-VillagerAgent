package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.ai.VillagerAgentData;
import com.github.AaronAA0721.villageragent.ai.VillagerAgentManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when player sends a chat message
 */
public class ChatMessagePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final UUID villagerId;
    private final String message;
    
    public ChatMessagePacket(UUID villagerId, String message) {
        this.villagerId = villagerId;
        this.message = message;
    }
    
    public static void encode(ChatMessagePacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeUtf(packet.message, 500);
    }
    
    public static ChatMessagePacket decode(PacketBuffer buffer) {
        return new ChatMessagePacket(buffer.readUUID(), buffer.readUtf(500));
    }
    
    public static void handle(ChatMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            
            VillagerAgentData agent = VillagerAgentManager.getAgent(packet.villagerId);
            if (agent == null) {
                LOGGER.warn("No agent found for villager: " + packet.villagerId);
                return;
            }
            
            String playerName = player.getName().getString();
            LOGGER.info("Chat from " + playerName + " to " + agent.getName() + ": " + packet.message);
            
            // Generate LLM response asynchronously
            agent.generateChatResponse(playerName, packet.message).thenAccept(response -> {
                // Send response back to client
                VillagerResponsePacket responsePacket = new VillagerResponsePacket(
                        packet.villagerId,
                        agent.getName(),
                        response
                );
                ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        responsePacket
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}

