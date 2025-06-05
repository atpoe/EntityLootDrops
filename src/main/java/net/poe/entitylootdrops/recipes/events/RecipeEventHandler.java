package net.poe.entitylootdrops.recipes.events;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.recipes.RecipeConfig;
import net.poe.entitylootdrops.recipes.model.RecipeEntry;

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ItemStack craftedItem = event.getCrafting();
        
        // Find recipes that produce this item
        executeCraftingCommands(player, craftedItem);
    }
    
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.getEntity();
        ItemStack smeltedItem = event.getSmelting();
        
        // Find smelting recipes that produce this item
        executeSmeltingCommands(player, smeltedItem);
    }
    
    private static void executeCraftingCommands(ServerPlayer player, ItemStack craftedItem) {
        try {
            List<RecipeEntry> allRecipes = RecipeConfig.getAllRecipes();
            
            for (RecipeEntry recipe : allRecipes) {
                if (recipe.isEnabled() && recipe.hasCommand()) {
                    String recipeType = recipe.getType().toLowerCase();
                    
                    // Check if this is a crafting recipe (shaped or shapeless)
                    if (recipeType.equals("shaped") || recipeType.equals("shapeless")) {
                        // Check if this recipe produces the same result
                        if (recipe.getOutputItem() != null && 
                            itemMatches(craftedItem, recipe.getOutputItem(), recipe.getOutputCount())) {
                            executeCommands(player, recipe);
                            break; // Only execute once per crafted item
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing crafting commands", e);
        }
    }
    
    private static void executeSmeltingCommands(ServerPlayer player, ItemStack smeltedItem) {
        try {
            List<RecipeEntry> allRecipes = RecipeConfig.getAllRecipes();
            
            for (RecipeEntry recipe : allRecipes) {
                if (recipe.isEnabled() && recipe.hasCommand()) {
                    String recipeType = recipe.getType().toLowerCase();
                    
                    // Check if this is a smelting recipe
                    if (recipeType.equals("furnace") || recipeType.equals("blasting") || 
                        recipeType.equals("smoking") || recipeType.equals("campfire")) {
                        
                        // Check if this recipe produces the same result
                        if (recipe.getOutputItem() != null && 
                            itemMatches(smeltedItem, recipe.getOutputItem(), recipe.getOutputCount())) {
                            executeCommands(player, recipe);
                            break; // Only execute once per smelted item
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing smelting commands", e);
        }
    }
    
    private static boolean itemMatches(ItemStack stack, String outputItem, int outputCount) {
        try {
            // Get the item's resource location
            ResourceLocation itemLocation = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemLocation == null) {
                return false;
            }
            
            // Compare item ID and count
            String itemId = itemLocation.toString();
            return itemId.equals(outputItem) && stack.getCount() >= outputCount;
            
        } catch (Exception e) {
            LOGGER.error("Error matching item: {}", outputItem, e);
            return false;
        }
    }
    
    private static void executeCommands(ServerPlayer player, RecipeEntry recipe) {
        try {
            // Check command chance
            if (recipe.getCommandChance() < 100.0f) {
                float roll = RANDOM.nextFloat() * 100.0f;
                if (roll > recipe.getCommandChance()) {
                    return; // Don't execute commands this time
                }
            }
            
            List<String> commands = recipe.getCraftCommands();
            if (commands == null || commands.isEmpty()) {
                return;
            }
            
            for (String command : commands) {
                if (command == null || command.trim().isEmpty()) {
                    continue;
                }
                
                // Replace placeholders
                String processedCommand = processCommand(command, player);
                
                // Execute the command
                executeCommand(player, processedCommand);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing commands for recipe: {}", recipe.getName(), e);
        }
    }
    
    private static String processCommand(String command, ServerPlayer player) {
        // Replace common placeholders
        String processed = command
            .replace("@p", player.getGameProfile().getName())
            .replace("{player}", player.getGameProfile().getName())
            .replace("{x}", String.valueOf((int) player.getX()))
            .replace("{y}", String.valueOf((int) player.getY()))
            .replace("{z}", String.valueOf((int) player.getZ()));
        
        // Handle ~ coordinates
        if (processed.contains("~")) {
            processed = processed.replace("~ ~ ~", 
                String.valueOf((int) player.getX()) + " " + 
                String.valueOf((int) player.getY()) + " " + 
                String.valueOf((int) player.getZ()));
        }
        
        return processed;
    }
    
    private static void executeCommand(ServerPlayer player, String command) {
        try {
            // Execute as server command at player's location
            player.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack(), command);
                
            LOGGER.debug("Executed recipe command: {}", command);
            
        } catch (Exception e) {
            LOGGER.error("Failed to execute recipe command: {}", command, e);
        }
    }
}
