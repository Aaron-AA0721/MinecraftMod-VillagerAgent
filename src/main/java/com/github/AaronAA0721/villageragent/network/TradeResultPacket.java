package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.client.VillagerChatHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client with trade result
 */
public class TradeResultPacket {
    private final UUID villagerId;
    private final boolean accepted;
    private final String message;
    
    public TradeResultPacket(UUID villagerId, boolean accepted, String message) {
        this.villagerId = villagerId;
        this.accepted = accepted;
        this.message = message;
    }
    
    public static void encode(TradeResultPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeBoolean(packet.accepted);
        buffer.writeUtf(packet.message, 500);
    }
    
    public static TradeResultPacket decode(PacketBuffer buffer) {
        return new TradeResultPacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readUtf(500)
        );
    }
    
    public static void handle(TradeResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client side
            VillagerChatHandler.receiveTradeResult(packet.villagerId, packet.accepted, packet.message);
        });
        ctx.get().setPacketHandled(true);
    }
    
    public UUID getVillagerId() {
        return villagerId;
    }
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public String getMessage() {
        return message;
    }
}

