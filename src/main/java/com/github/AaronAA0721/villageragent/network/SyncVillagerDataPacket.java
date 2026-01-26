package com.github.AaronAA0721.villageragent.network;

import com.github.AaronAA0721.villageragent.client.VillagerChatHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync villager data for GUI
 */
public class SyncVillagerDataPacket {
    private final UUID villagerId;
    private final String villagerName;
    private final String profession;
    private final String personality;
    private final List<ItemStack> inventoryItems;

    public SyncVillagerDataPacket(UUID villagerId, String villagerName, String profession, String personality, List<ItemStack> items) {
        this.villagerId = villagerId;
        this.villagerName = villagerName;
        this.profession = profession;
        this.personality = personality;
        this.inventoryItems = items;
    }

    public static void encode(SyncVillagerDataPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.villagerId);
        buffer.writeUtf(packet.villagerName, 100);
        buffer.writeUtf(packet.profession, 100);
        buffer.writeUtf(packet.personality, 200);
        buffer.writeInt(packet.inventoryItems.size());
        for (ItemStack item : packet.inventoryItems) {
            buffer.writeItem(item);
        }
    }

    public static SyncVillagerDataPacket decode(PacketBuffer buffer) {
        UUID id = buffer.readUUID();
        String name = buffer.readUtf(100);
        String profession = buffer.readUtf(100);
        String personality = buffer.readUtf(200);
        int itemCount = buffer.readInt();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(buffer.readItem());
        }
        return new SyncVillagerDataPacket(id, name, profession, personality, items);
    }

    public static void handle(SyncVillagerDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client side - open the chat GUI
            VillagerChatHandler.openChatScreen(
                    packet.villagerId,
                    packet.villagerName,
                    packet.profession,
                    packet.personality,
                    packet.inventoryItems
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public UUID getVillagerId() {
        return villagerId;
    }

    public String getVillagerName() {
        return villagerName;
    }

    public String getProfession() {
        return profession;
    }

    public String getPersonality() {
        return personality;
    }

    public List<ItemStack> getInventoryItems() {
        return inventoryItems;
    }
}

