package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.client.VillagerChatHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client with villager's chat response
 */
public class VillagerResponsePacket {
    private final UUID villagerId;
    private final String villagerName;
    private final String response;
    
    public VillagerResponsePacket(UUID villagerId, String villagerName, String response) {
        this.villagerId = villagerId;
        this.villagerName = villagerName;
        this.response = response;
    }
    
    public static void encode(VillagerResponsePacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeUtf(packet.villagerName, 100);
        buffer.writeUtf(packet.response, 2000);
    }
    
    public static VillagerResponsePacket decode(PacketBuffer buffer) {
        return new VillagerResponsePacket(
                buffer.readUUID(),
                buffer.readUtf(100),
                buffer.readUtf(2000)
        );
    }
    
    public static void handle(VillagerResponsePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client side
            VillagerChatHandler.receiveResponse(packet.villagerId, packet.villagerName, packet.response);
        });
        ctx.get().setPacketHandled(true);
    }
    
    public UUID getVillagerId() {
        return villagerId;
    }
    
    public String getVillagerName() {
        return villagerName;
    }
    
    public String getResponse() {
        return response;
    }
}

