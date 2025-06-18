package net.poe.entitylootdrops.blockdrops.regeneration;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class ForceRegenerationTask {
    private static boolean running = false;
    private static Iterator<BlockRegenerationManager.PersistentRegenerationData> iterator = null;
    private static List<BlockRegenerationManager.PersistentRegenerationData> toRegenerate = null;

    public static boolean isRunning() {
        return running;
    }

    public static void start() {
        if (running) return;
        // Gather all regeneration data into a flat list
        toRegenerate = new LinkedList<>();
        for (Map<String, BlockRegenerationManager.PersistentRegenerationData> map : BlockRegenerationManager.getAllRegeneratingBlocks().values()) {
            toRegenerate.addAll(map.values());
        }
        iterator = toRegenerate.iterator();
        running = true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!running || event.phase != TickEvent.Phase.END) return;
        if (iterator != null && iterator.hasNext()) {
            BlockRegenerationManager.PersistentRegenerationData data = iterator.next();
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(data.getDimensionId())));
                if (level != null) {
                    BlockRegenerationManager.forceRegenerateBlock(level, new BlockPos(data.getX(), data.getY(), data.getZ()));
                }
            }
        }
        if (!iterator.hasNext()) {
            running = false;
            iterator = null;
            toRegenerate = null;
        }
    }
}