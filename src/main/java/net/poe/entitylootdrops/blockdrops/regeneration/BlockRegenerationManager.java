package net.poe.entitylootdrops.blockdrops.regeneration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Manages block regeneration for blocks that can regenerate after being broken.
 * Persists regeneration data to survive server restarts.
 */
public class BlockRegenerationManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String REGENERATION_FILE = "config/EntityLootDrops/block_regeneration.json";
    
    // Map to store regenerating blocks: Level ID -> (BlockPos -> RegenerationData)
    private static final Map<String, Map<String, PersistentRegenerationData>> regeneratingBlocks = new ConcurrentHashMap<>();
    
    /**
     * Data class to store regeneration information that can be persisted
     */
    public static class PersistentRegenerationData {
        private final String originalBlockId;
        private final String originalBlockNbt; // For complex block states
        private final long regenerationTime;
        private final int x, y, z;
        private final String dimensionId;
        
        public PersistentRegenerationData(String originalBlockId, String originalBlockNbt, 
                                        long regenerationTime, int x, int y, int z, String dimensionId) {
            this.originalBlockId = originalBlockId;
            this.originalBlockNbt = originalBlockNbt;
            this.regenerationTime = regenerationTime;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }
        
        // Getters
        public String getOriginalBlockId() { return originalBlockId; }
        public String getOriginalBlockNbt() { return originalBlockNbt; }
        public long getRegenerationTime() { return regenerationTime; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public String getDimensionId() { return dimensionId; }
        
        public BlockPos getBlockPos() { return new BlockPos(x, y, z); }
    }
    
    /**
     * Schedules a block for regeneration and saves to disk.
     */
    public static void scheduleRegeneration(ServerLevel level, BlockPos pos, BlockState originalBlock, 
                                          String brokenBlockReplace, int respawnTimeSeconds) {
        try {
            // Get the replacement block
            ResourceLocation replaceBlockId = new ResourceLocation(brokenBlockReplace);
            Block replaceBlock = ForgeRegistries.BLOCKS.getValue(replaceBlockId);
            
            if (replaceBlock == null) {
                LOGGER.error("Invalid replacement block ID: {}", brokenBlockReplace);
                return;
            }
            
            // Place the replacement block
            BlockState replaceState = replaceBlock.defaultBlockState();
            level.setBlock(pos, replaceState, 3);
            
            // Calculate regeneration time (current time + respawn time in milliseconds)
            long regenerationTime = System.currentTimeMillis() + (respawnTimeSeconds * 1000L);
            
            // Get original block info
            ResourceLocation originalBlockId = ForgeRegistries.BLOCKS.getKey(originalBlock.getBlock());
            String originalBlockIdString = originalBlockId != null ? originalBlockId.toString() : "minecraft:stone";
            
            // Create regeneration data
            String dimensionId = level.dimension().location().toString();
            String levelKey = dimensionId;
            String posKey = pos.getX() + "," + pos.getY() + "," + pos.getZ();
            
            PersistentRegenerationData data = new PersistentRegenerationData(
                originalBlockIdString,
                "", // TODO: Add NBT support if needed
                regenerationTime,
                pos.getX(), pos.getY(), pos.getZ(),
                dimensionId
            );
            
            // Store in memory
            regeneratingBlocks.computeIfAbsent(levelKey, k -> new ConcurrentHashMap<>())
                             .put(posKey, data);
            
            // Save to disk
            saveRegenerationData();
            
            LOGGER.debug("Scheduled block regeneration at {} in {} seconds", pos, respawnTimeSeconds);
            
        } catch (Exception e) {
            LOGGER.error("Failed to schedule block regeneration at {}", pos, e);
        }
    }
    
    /**
     * Processes all scheduled regenerations. Should be called periodically.
     */
    public static void processRegenerations() {
        long currentTime = System.currentTimeMillis();
        AtomicBoolean dataChanged = new AtomicBoolean(false); // Use AtomicBoolean instead
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (Map.Entry<String, Map<String, PersistentRegenerationData>> levelEntry : regeneratingBlocks.entrySet()) {
            String levelKey = levelEntry.getKey();
            Map<String, PersistentRegenerationData> blocks = levelEntry.getValue();
            
            blocks.entrySet().removeIf(entry -> {
                PersistentRegenerationData data = entry.getValue();
                
                if (currentTime >= data.getRegenerationTime()) {
                    // Time to regenerate - find the level
                    ServerLevel level = findLevelByDimension(server, data.getDimensionId());
                    if (level != null) {
                        try {
                            // Get the original block
                            ResourceLocation blockId = new ResourceLocation(data.getOriginalBlockId());
                            Block originalBlock = ForgeRegistries.BLOCKS.getValue(blockId);
                            
                            if (originalBlock != null) {
                                BlockState originalState = originalBlock.defaultBlockState();
                                level.setBlock(data.getBlockPos(), originalState, 3);
                                LOGGER.debug("Regenerated block at {}", data.getBlockPos());
                                dataChanged.set(true); // Use AtomicBoolean.set()
                                return true; // Remove from map
                            } else {
                                LOGGER.error("Could not find original block: {}", data.getOriginalBlockId());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to regenerate block at {}", data.getBlockPos(), e);
                        }
                    }
                }
                return false; // Keep in map
            });
        }
        
        // Clean up empty level maps
        regeneratingBlocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Save changes to disk if any regenerations occurred
        if (dataChanged.get()) { // Use AtomicBoolean.get()
            saveRegenerationData();
        }
    }
    
    /**
     * Loads regeneration data from disk on server start.
     */
    public static void loadRegenerationData() {
        File file = new File(REGENERATION_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<String, Map<String, PersistentRegenerationData>>>(){}.getType();
            Map<String, Map<String, PersistentRegenerationData>> loadedData = gson.fromJson(reader, type);
            
            if (loadedData != null) {
                regeneratingBlocks.clear();
                regeneratingBlocks.putAll(loadedData);
                
                int totalBlocks = regeneratingBlocks.values().stream()
                                                  .mapToInt(Map::size)
                                                  .sum();
                LOGGER.info("Loaded {} regenerating blocks from disk", totalBlocks);
                
                // Check for any blocks that should have already regenerated
                processRegenerations();
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load regeneration data from disk", e);
        }
    }
    
    /**
     * Saves regeneration data to disk.
     */
    public static void saveRegenerationData() {
        try {
            File file = new File(REGENERATION_FILE);
            file.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(regeneratingBlocks, writer);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to save regeneration data to disk", e);
        }
    }
    
    /**
     * Cancels regeneration for a specific block (e.g., if it's broken again).
     */
    public static void cancelRegeneration(ServerLevel level, BlockPos pos) {
        String levelKey = level.dimension().location().toString();
        String posKey = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        
        Map<String, PersistentRegenerationData> blocks = regeneratingBlocks.get(levelKey);
        if (blocks != null && blocks.remove(posKey) != null) {
            LOGGER.debug("Cancelled regeneration for block at {}", pos);
            saveRegenerationData();
        }
    }
    
    /**
     * Checks if a block is scheduled for regeneration.
     */
    public static boolean isScheduledForRegeneration(ServerLevel level, BlockPos pos) {
        String levelKey = level.dimension().location().toString();
        String posKey = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        
        Map<String, PersistentRegenerationData> blocks = regeneratingBlocks.get(levelKey);
        return blocks != null && blocks.containsKey(posKey);
    }
    
    /**
     * Gets the count of blocks scheduled for regeneration.
     */
    public static int getRegenerationCount() {
        return regeneratingBlocks.values().stream()
                                .mapToInt(Map::size)
                                .sum();
    }
    
    /**
     * Clears all scheduled regenerations and saves to disk.
     */
    public static void clearAll() {
        regeneratingBlocks.clear();
        saveRegenerationData();
        LOGGER.info("Cleared all scheduled block regenerations");
    }
    
    /**
     * Helper method to find a ServerLevel by dimension ID.
     */
    private static ServerLevel findLevelByDimension(MinecraftServer server, String dimensionId) {
        ResourceLocation dimension = new ResourceLocation(dimensionId);
        return server.getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimension));
    }
}
