package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.ai.CraftingAction;
import com.github.AaronAA0721.villageragent.ai.VillagerAgentData;
import com.github.AaronAA0721.villageragent.ai.VillagerAgentManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet for requesting a villager to craft something
 */
public class CraftingRequestPacket {
    private UUID villagerId;
    private String recipeName;
    private BlockPos craftingTablePos;
    
    public CraftingRequestPacket(UUID villagerId, String recipeName, BlockPos craftingTablePos) {
        this.villagerId = villagerId;
        this.recipeName = recipeName;
        this.craftingTablePos = craftingTablePos;
    }
    
    public CraftingRequestPacket() {
    }
    
    public static void encode(CraftingRequestPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeUtf(packet.recipeName, 100);
        buffer.writeBlockPos(packet.craftingTablePos);
    }
    
    public static CraftingRequestPacket decode(PacketBuffer buffer) {
        UUID villagerId = buffer.readUUID();
        String recipeName = buffer.readUtf(100);
        BlockPos craftingTablePos = buffer.readBlockPos();
        return new CraftingRequestPacket(villagerId, recipeName, craftingTablePos);
    }
    
    public static void handle(CraftingRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            VillagerAgentData agent = VillagerAgentManager.getAgent(packet.villagerId);
            if (agent != null) {
                CraftingAction.executeCraft(agent, packet.recipeName, packet.craftingTablePos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

