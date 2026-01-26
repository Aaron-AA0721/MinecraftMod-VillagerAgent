package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.Villageragent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Network handler for client-server communication
 */
public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Villageragent.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        // Client -> Server: Player sends chat message
        CHANNEL.registerMessage(nextId(),
                ChatMessagePacket.class,
                ChatMessagePacket::encode,
                ChatMessagePacket::decode,
                ChatMessagePacket::handle);
        
        // Server -> Client: Villager response
        CHANNEL.registerMessage(nextId(),
                VillagerResponsePacket.class,
                VillagerResponsePacket::encode,
                VillagerResponsePacket::decode,
                VillagerResponsePacket::handle);
        
        // Client -> Server: Trade request
        CHANNEL.registerMessage(nextId(),
                TradeRequestPacket.class,
                TradeRequestPacket::encode,
                TradeRequestPacket::decode,
                TradeRequestPacket::handle);
        
        // Server -> Client: Trade result
        CHANNEL.registerMessage(nextId(),
                TradeResultPacket.class,
                TradeResultPacket::encode,
                TradeResultPacket::decode,
                TradeResultPacket::handle);
        
        // Client -> Server: Open chat GUI request
        CHANNEL.registerMessage(nextId(),
                OpenChatPacket.class,
                OpenChatPacket::encode,
                OpenChatPacket::decode,
                OpenChatPacket::handle);
        
        // Server -> Client: Sync villager data for GUI
        CHANNEL.registerMessage(nextId(),
                SyncVillagerDataPacket.class,
                SyncVillagerDataPacket::encode,
                SyncVillagerDataPacket::decode,
                SyncVillagerDataPacket::handle);

        // Server -> Client: Crafting request
        CHANNEL.registerMessage(nextId(),
                CraftingRequestPacket.class,
                CraftingRequestPacket::encode,
                CraftingRequestPacket::decode,
                CraftingRequestPacket::handle);
    }
}

