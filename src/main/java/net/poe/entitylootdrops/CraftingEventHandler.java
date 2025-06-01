package net.poe.entitylootdrops;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles events related to crafting recipes.
 * Executes commands when players craft items from custom recipes.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class CraftingEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Cache for recipe lookup by output item
    private static final Map<String, RecipeConfig.BaseRecipe> recipeCache = new ConcurrentHashMap<>();
    
    /**
     * Initializes the recipe cache for faster lookups.
     * Should be called whenever recipes are loaded or reloaded.
     */
    public static void initRecipeCache() {
        recipeCache.clear();
        
        // Cache shaped recipes
        for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapedRecipes()) {
            if (recipe.hasCommands()) {
                String cacheKey = createCacheKey(recipe.getOutputItem());
                recipeCache.put(cacheKey, recipe);
                LOGGER.debug("Cached shaped recipe: {} -> {}", cacheKey, recipe.getName());
            }
        }
        
        // Cache shapeless recipes
        for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapelessRecipes()) {
            if (recipe.hasCommands()) {
                String cacheKey = createCacheKey(recipe.getOutputItem());
                recipeCache.put(cacheKey, recipe);
                LOGGER.debug("Cached shapeless recipe: {} -> {}", cacheKey, recipe.getName());
            }
        }
        
        // Cache brewing recipes
        for (RecipeConfig.BrewingRecipe recipe : RecipeConfig.getBrewingRecipes()) {
            if (recipe.hasCommands()) {
                String cacheKey = createCacheKey(recipe.getOutputPotion());
                recipeCache.put(cacheKey, recipe);
                LOGGER.debug("Cached brewing recipe: {} -> {}", cacheKey, recipe.getName());
            }
        }
        
        // Cache furnace recipes
        for (RecipeConfig.FurnaceRecipe recipe : RecipeConfig.getFurnaceRecipes()) {
            if (recipe.hasCommands()) {
                String cacheKey = createCacheKey(recipe.getOutput());
                recipeCache.put(cacheKey, recipe);
                LOGGER.debug("Cached furnace recipe: {} -> {}", cacheKey, recipe.getName());
            }
        }
        
        // Cache smithing recipes
        for (RecipeConfig.SmithingRecipe recipe : RecipeConfig.getSmithingRecipes()) {
            if (recipe.hasCommands()) {
                String cacheKey = createCacheKey(recipe.getOutputItem());
                recipeCache.put(cacheKey, recipe);
                LOGGER.debug("Cached smithing recipe: {} -> {}", cacheKey, recipe.getName());
            }
        }
        
        LOGGER.info("Initialized crafting command cache with {} recipes", recipeCache.size());
    }
    
    /**
     * Creates a cache key for a recipe based on its output item.
     */
    private static String createCacheKey(String outputItem) {
        return outputItem;
    }
    
    /**
     * Event handler for when a player crafts an item.
     * Checks if the crafted item matches a custom recipe with commands,
     * and executes those commands if it does.
     */
    @SubscribeEvent
    public static void onItemCrafted(ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        ItemStack crafted = event.getCrafting();
        
        // Skip if the crafted item is empty
        if (crafted.isEmpty()) {
            return;
        }
        
        // Get the item's registry name
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(crafted.getItem());
        if (itemId == null) {
            return;
        }
        
        String itemIdString = itemId.toString();
        LOGGER.debug("Player {} crafted item: {} (count: {})", player.getName().getString(), itemIdString, crafted.getCount());
        
        // Check for recipe with commands
        RecipeConfig.BaseRecipe recipe = recipeCache.get(itemIdString);
        if (recipe != null && recipe.hasCommands()) {
            LOGGER.info("Executing commands for crafted item: {} by player: {}", itemIdString, player.getName().getString());
            executeCommands(player, recipe.getCraftCommands());
        } else {
            LOGGER.debug("No commands found for crafted item: {}", itemIdString);
        }
    }
    
    /**
     * Executes a list of commands for a player who crafted an item.
     * 
     * @param player The player who crafted the item
     * @param commands The commands to execute
     */
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        MinecraftServer server = player.getServer();
        if (server == null) {
            LOGGER.error("Server is null, cannot execute commands");
            return;
        }
        
        // Create a command source with appropriate permissions
        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(server)
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4); // Op level permission
        
        // Execute each command
        for (String command : commands) {
            try {
                // Replace placeholders in the command
                String processedCommand = command
                    .replace("{player}", player.getName().getString())
                    .replace("{uuid}", player.getStringUUID())
                    .replace("{x}", String.valueOf((int)player.getX()))
                    .replace("{y}", String.valueOf((int)player.getY()))
                    .replace("{z}", String.valueOf((int)player.getZ()));
                
                LOGGER.debug("Executing command: {}", processedCommand);
                
                // Execute the command
                server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                
                LOGGER.debug("Successfully executed craft command: {}", processedCommand);
            } catch (Exception e) {
                LOGGER.error("Failed to execute craft command: {} - Error: {}", command, e.getMessage());
            }
        }
    }
    
    /**
     * Debug method to list all cached recipes
     */
    public static void debugCacheContents() {
        LOGGER.info("=== Recipe Cache Contents ===");
        for (Map.Entry<String, RecipeConfig.BaseRecipe> entry : recipeCache.entrySet()) {
            LOGGER.info("Key: {} -> Recipe: {} (Commands: {})", 
                entry.getKey(), 
                entry.getValue().getName(), 
                entry.getValue().getCraftCommands().size());
        }
        LOGGER.info("=== End Cache Contents ===");
    }
}
