package net.poe.entitylootdrops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Configuration class for custom recipes with NBT data support and Forge tag compatibility.
 * Supports crafting table, brewing stand, furnace, smithing table recipes, and recipe replacements.
 */
public class RecipeConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String RECIPES_DIR = "Recipes";
    private static final String SHAPED_DIR = "Shaped";
    private static final String SHAPELESS_DIR = "Shapeless";
    private static final String BREWING_DIR = "Brewing";
    private static final String FURNACE_DIR = "Furnace";
    private static final String SMITHING_DIR = "Smithing";
    
    // Configuration file for recipe settings
    private static final String RECIPE_CONFIG_FILE = "recipe_settings.properties";
    
    // Configuration properties
    private static boolean generateExampleRecipes = true;
    
    // Storage for loaded recipes - made thread-safe for reload operations
    private static volatile List<CustomRecipe> shapedRecipes = new ArrayList<>();
    private static volatile List<CustomRecipe> shapelessRecipes = new ArrayList<>();
    private static volatile List<BrewingRecipe> brewingRecipes = new ArrayList<>();
    private static volatile List<FurnaceRecipe> furnaceRecipes = new ArrayList<>();
    private static volatile List<SmithingRecipe> smithingRecipes = new ArrayList<>();
    
    // Flag to track if recipes are currently being reloaded
    private static volatile boolean isReloading = false;
    
    /**
     * Helper class for parsing ingredient entries that may contain tags.
     */
    public static class IngredientEntry {
        private String item;
        private String tag;
        private int count = 1;
        
        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public boolean isTag() { return tag != null && !tag.isEmpty(); }
        public boolean isItem() { return item != null && !item.isEmpty(); }
        
        /**
         * Creates a Minecraft Ingredient from this entry.
         */
        public Ingredient createIngredient() {
            try {
                if (isTag()) {
                    // Handle Forge tags
                    ResourceLocation tagLocation = new ResourceLocation(tag);
                    TagKey<Item> itemTag = ItemTags.create(tagLocation);
                    return Ingredient.of(itemTag);
                } else if (isItem()) {
                    // Handle regular items
                    ResourceLocation itemLocation = new ResourceLocation(item);
                    Item itemObj = ForgeRegistries.ITEMS.getValue(itemLocation);
                    if (itemObj != null) {
                        return Ingredient.of(itemObj);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create ingredient from entry: {}", e.getMessage());
            }
            return Ingredient.EMPTY;
        }
        
        /**
         * Creates an ItemStack for display purposes (uses first item from tag if applicable).
         */
        public ItemStack createDisplayStack() {
            try {
                if (isTag()) {
                    ResourceLocation tagLocation = new ResourceLocation(tag);
                    TagKey<Item> itemTag = ItemTags.create(tagLocation);
                    
                    // Get first item from tag for display
                    var tagItems = ForgeRegistries.ITEMS.tags().getTag(itemTag);
                    if (!tagItems.isEmpty()) {
                        Item firstItem = tagItems.iterator().next();
                        return new ItemStack(firstItem, count);
                    }
                } else if (isItem()) {
                    ResourceLocation itemLocation = new ResourceLocation(item);
                    Item itemObj = ForgeRegistries.ITEMS.getValue(itemLocation);
                    if (itemObj != null) {
                        return new ItemStack(itemObj, count);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create display stack: {}", e.getMessage());
            }
            return ItemStack.EMPTY;
        }
        
        @Override
        public String toString() {
            if (isTag()) {
                return "tag:" + tag + (count > 1 ? " x" + count : "");
            } else if (isItem()) {
                return "item:" + item + (count > 1 ? " x" + count : "");
            }
            return "empty";
        }
    }
    
    /**
     * Base class for all recipe types.
     */
    public static abstract class BaseRecipe {
        private String name;                // Recipe name (used for ID)
        private String _comment;            // Comment for documentation
        private List<String> craftCommands; // Commands to execute when item is crafted
        private boolean enabled = true;     // Whether this recipe is enabled
        private String recipeToReplace;     // Optional: vanilla recipe ID to replace
        private boolean removeOriginal = true; // Whether to remove the original recipe when replacing
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getComment() { return _comment; }
        public void setComment(String comment) { this._comment = comment; }
        
        public List<String> getCraftCommands() { return craftCommands; }
        public void setCraftCommands(List<String> craftCommands) { this.craftCommands = craftCommands; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getRecipeToReplace() { return recipeToReplace; }
        public void setRecipeToReplace(String recipeToReplace) { this.recipeToReplace = recipeToReplace; }
        
        public boolean isRemoveOriginal() { return removeOriginal; }
        public void setRemoveOriginal(boolean removeOriginal) { this.removeOriginal = removeOriginal; }
        
        // Helper methods
        public boolean hasCommands() { return craftCommands != null && !craftCommands.isEmpty(); }
        public boolean isReplacement() { return recipeToReplace != null && !recipeToReplace.isEmpty(); }
    }
    
    /**
     * Represents a custom crafting recipe configuration with tag support.
     */
    public static class CustomRecipe extends BaseRecipe {
        private String type;                // "shaped" or "shapeless"
        private String outputItem;          // Output item ID
        private int outputCount = 1;        // Output item count
        private String outputNbt;           // NBT data for output item
        private List<String> pattern;       // Pattern for shaped recipes (e.g. ["XXX", "X X", "XXX"])
        private Map<String, Object> key;    // Key mapping for shaped recipes - now supports both strings and objects
        private List<Object> ingredients;   // Ingredients for shapeless recipes - now supports both strings and objects
        private String group = "";          // Recipe group
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getOutputItem() { return outputItem; }
        public void setOutputItem(String outputItem) { this.outputItem = outputItem; }
        
        public int getOutputCount() { return outputCount; }
        public void setOutputCount(int outputCount) { this.outputCount = outputCount; }
        
        public String getOutputNbt() { return outputNbt; }
        public void setOutputNbt(String outputNbt) { this.outputNbt = outputNbt; }
        
        public List<String> getPattern() { return pattern; }
        public void setPattern(List<String> pattern) { this.pattern = pattern; }
        
        public Map<String, Object> getKey() { return key; }
        public void setKey(Map<String, Object> key) { this.key = key; }
        
        public List<Object> getIngredients() { return ingredients; }
        public void setIngredients(List<Object> ingredients) { this.ingredients = ingredients; }
        
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        
        // Helper methods
        public boolean isShaped() { return "shaped".equalsIgnoreCase(type); }
        public boolean isShapeless() { return "shapeless".equalsIgnoreCase(type); }
        public boolean hasOutputNbt() { return outputNbt != null && !outputNbt.isEmpty(); }
        
        /**
         * Parses an ingredient entry from JSON object or string.
         */
        private IngredientEntry parseIngredientEntry(Object obj) {
            IngredientEntry entry = new IngredientEntry();
            
            if (obj instanceof String) {
                // Simple string format
                entry.setItem((String) obj);
            } else if (obj instanceof Map) {
                // Object format with tag/item support
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                
                if (map.containsKey("tag")) {
                    entry.setTag((String) map.get("tag"));
                } else if (map.containsKey("item")) {
                    entry.setItem((String) map.get("item"));
                }
                
                if (map.containsKey("count")) {
                    Object countObj = map.get("count");
                    if (countObj instanceof Number) {
                        entry.setCount(((Number) countObj).intValue());
                    }
                }
            }
            
            return entry;
        }
        
        /**
         * Gets parsed ingredient entries for shaped recipe keys.
         */
        public Map<String, IngredientEntry> getParsedKey() {
            Map<String, IngredientEntry> parsedKey = new HashMap<>();
            
            if (key != null) {
                for (Map.Entry<String, Object> entry : key.entrySet()) {
                    parsedKey.put(entry.getKey(), parseIngredientEntry(entry.getValue()));
                }
            }
            
            return parsedKey;
        }
        
        /**
         * Gets parsed ingredient entries for shapeless recipes.
         */
        public List<IngredientEntry> getParsedIngredients() {
            List<IngredientEntry> parsedIngredients = new ArrayList<>();
            
            if (ingredients != null) {
                for (Object obj : ingredients) {
                    parsedIngredients.add(parseIngredientEntry(obj));
                }
            }
            
            return parsedIngredients;
        }
        
        /**
         * Creates an ItemStack for the output item with NBT data if specified.
         */
        public ItemStack createOutputStack() {
            try {
                ResourceLocation itemId = new ResourceLocation(outputItem);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item == null) {
                    LOGGER.error("Invalid output item ID in recipe {}: {}", getName(), outputItem);
                    return ItemStack.EMPTY;
                }
                
                ItemStack stack = new ItemStack(item, outputCount);
                
                if (hasOutputNbt()) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(outputNbt);
                        stack.setTag(nbt);
                    } catch (Exception e) {
                        LOGGER.error("Invalid NBT data in recipe {}: {}", getName(), e.getMessage());
                    }
                }
                
                return stack;
            } catch (Exception e) {
                LOGGER.error("Error creating output stack for recipe {}: {}", getName(), e.getMessage());
                return ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * Represents a custom brewing recipe configuration.
     */
    public static class BrewingRecipe extends BaseRecipe {
        private String inputPotion;         // Input potion ID (e.g. "minecraft:water" or "minecraft:potion:awkward")
        private String ingredient;          // Ingredient item ID
        private String outputPotion;        // Output potion ID
        private String outputNbt;           // Additional NBT data for output potion
        private int brewingTime = 400;      // Brewing time in ticks (default: 20 seconds)
        
        // Getters and setters
        public String getInputPotion() { return inputPotion; }
        public void setInputPotion(String inputPotion) { this.inputPotion = inputPotion; }
        
        public String getIngredient() { return ingredient; }
        public void setIngredient(String ingredient) { this.ingredient = ingredient; }
        
        public String getOutputPotion() { return outputPotion; }
        public void setOutputPotion(String outputPotion) { this.outputPotion = outputPotion; }
        
        public String getOutputNbt() { return outputNbt; }
        public void setOutputNbt(String outputNbt) { this.outputNbt = outputNbt; }
        
        public int getBrewingTime() { return brewingTime; }
        public void setBrewingTime(int brewingTime) { this.brewingTime = brewingTime; }
        
        // Helper methods
        public boolean hasOutputNbt() { return outputNbt != null && !outputNbt.isEmpty(); }
        
        /**
         * Creates the input potion ItemStack.
         */
        public ItemStack createInputStack() {
            try {
                String[] parts = inputPotion.split(":");
                if (parts.length < 2) return ItemStack.EMPTY;
                
                String itemName = parts[0] + ":" + parts[1];
                ResourceLocation itemId = new ResourceLocation(itemName);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item == null) return ItemStack.EMPTY;
                ItemStack stack = new ItemStack(item);
                
                if (parts.length >= 3) {
                    ResourceLocation potionId = new ResourceLocation(parts[2]);
                    Potion potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        PotionUtils.setPotion(stack, potion);
                    }
                }
                
                return stack;
            } catch (Exception e) {
                LOGGER.error("Error creating input potion: {}", e.getMessage());
                return ItemStack.EMPTY;
            }
        }
        
        /**
         * Creates the ingredient ItemStack.
         */
        public ItemStack createIngredientStack() {
            try {
                ResourceLocation itemId = new ResourceLocation(ingredient);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                return item != null ? new ItemStack(item) : ItemStack.EMPTY;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
        
        /**
         * Creates the output potion ItemStack.
         */
        public ItemStack createOutputStack() {
            try {
                String[] parts = outputPotion.split(":");
                if (parts.length < 2) return ItemStack.EMPTY;
                
                String itemName = parts[0] + ":" + parts[1];
                ResourceLocation itemId = new ResourceLocation(itemName);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item == null) return ItemStack.EMPTY;
                ItemStack stack = new ItemStack(item);
                
                if (parts.length >= 3) {
                    ResourceLocation potionId = new ResourceLocation(parts[2]);
                    Potion potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        PotionUtils.setPotion(stack, potion);
                    }
                }
                
                if (hasOutputNbt()) {
                    CompoundTag nbt = TagParser.parseTag(outputNbt);
                    CompoundTag existingNbt = stack.getTag();
                    if (existingNbt != null) {
                        nbt.merge(existingNbt);
                    }
                    stack.setTag(nbt);
                }
                
                return stack;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * Represents a custom furnace recipe configuration.
     */
    public static class FurnaceRecipe extends BaseRecipe {
        private String input;               // Input item ID
        private String output;              // Output item ID
        private int outputCount = 1;        // Output item count
        private String outputNbt;           // NBT data for output item
        private float experience = 0.1f;    // Experience given
        private int cookingTime = 200;      // Cooking time in ticks
        
        // Getters and setters
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        
        public int getOutputCount() { return outputCount; }
        public void setOutputCount(int outputCount) { this.outputCount = outputCount; }
        
        public String getOutputNbt() { return outputNbt; }
        public void setOutputNbt(String outputNbt) { this.outputNbt = outputNbt; }
        
        public float getExperience() { return experience; }
        public void setExperience(float experience) { this.experience = experience; }
        
        public int getCookingTime() { return cookingTime; }
        public void setCookingTime(int cookingTime) { this.cookingTime = cookingTime; }
        
        // Helper methods
        public boolean hasOutputNbt() { return outputNbt != null && !outputNbt.isEmpty(); }
        
        public ItemStack createInputStack() {
            try {
                ResourceLocation itemId = new ResourceLocation(input);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                return item != null ? new ItemStack(item) : ItemStack.EMPTY;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
        
        public ItemStack createOutputStack() {
            try {
                ResourceLocation itemId = new ResourceLocation(output);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) return ItemStack.EMPTY;
                
                ItemStack stack = new ItemStack(item, outputCount);
                if (hasOutputNbt()) {
                    stack.setTag(TagParser.parseTag(outputNbt));
                }
                return stack;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * Represents a custom smithing recipe configuration.
     */
    public static class SmithingRecipe extends BaseRecipe {
    private String template;            // Template item ID (e.g., "minecraft:netherite_upgrade_smithing_template")
    private String baseItem;            // Base item ID
    private String additionItem;        // Addition item ID
    private String outputItem;          // Output item ID
    private int outputCount = 1;        // Output item count
    private String outputNbt;           // NBT data for output item
    private boolean copyNbt = true;     // Whether to copy NBT from base item
    
    // Getters and setters
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    
    public String getBaseItem() { return baseItem; }
    public void setBaseItem(String baseItem) { this.baseItem = baseItem; }
    
    public String getAdditionItem() { return additionItem; }
    public void setAdditionItem(String additionItem) { this.additionItem = additionItem; }
    
    public String getOutputItem() { return outputItem; }
    public void setOutputItem(String outputItem) { this.outputItem = outputItem; }
    
    public int getOutputCount() { return outputCount; }
    public void setOutputCount(int outputCount) { this.outputCount = outputCount; }
    
    public String getOutputNbt() { return outputNbt; }
    public void setOutputNbt(String outputNbt) { this.outputNbt = outputNbt; }
    
    public boolean isCopyNbt() { return copyNbt; }
    public void setCopyNbt(boolean copyNbt) { this.copyNbt = copyNbt; }
    
    // Helper methods
    public boolean hasOutputNbt() { return outputNbt != null && !outputNbt.isEmpty(); }
    
    public ItemStack createTemplateStack() {
        try {
            ResourceLocation itemId = new ResourceLocation(template);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    public ItemStack createBaseStack() {
        try {
            ResourceLocation itemId = new ResourceLocation(baseItem);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    public ItemStack createAdditionStack() {
        try {
            ResourceLocation itemId = new ResourceLocation(additionItem);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    public ItemStack createOutputStack() {
        try {
            ResourceLocation itemId = new ResourceLocation(outputItem);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return ItemStack.EMPTY;
            
            ItemStack stack = new ItemStack(item, outputCount);
            if (hasOutputNbt()) {
                stack.setTag(TagParser.parseTag(outputNbt));
            }
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}

    
    /**
     * Loads all custom recipe configurations with proper reload handling.
     */
    public static synchronized void loadConfig() {
        // Set reload flag to prevent concurrent access issues
        isReloading = true;
        
        try {
            // Create directories if they don't exist
            createConfigDirectories();
            
            // Load recipe settings
            loadRecipeSettings();
            
            // Load all recipes
            loadAllRecipes();
            
            // Initialize the crafting event cache
            CraftingEventHandler.initRecipeCache();
            
            LOGGER.info("Loaded recipe configuration: {} shaped, {} shapeless, {} brewing, {} furnace, {} smithing recipes",
                shapedRecipes.size(), shapelessRecipes.size(), brewingRecipes.size(), 
                furnaceRecipes.size(), smithingRecipes.size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to load recipe configuration", e);
        } finally {
            isReloading = false;
        }
    }
    
    /**
     * Loads recipe settings from the properties file.
     */
    private static void loadRecipeSettings() {
        Path configFile = Paths.get(CONFIG_DIR, RECIPE_CONFIG_FILE);
        Properties props = new Properties();
        
        // Create default settings if file doesn't exist
        if (!Files.exists(configFile)) {
            try {
                props.setProperty("generateExampleRecipes", "true");
                Files.createDirectories(configFile.getParent());
                try (var out = Files.newOutputStream(configFile)) {
                    props.store(out, "EntityLootDrops Recipe Settings");
                }
                LOGGER.info("Created default recipe settings file");
            } catch (IOException e) {
                LOGGER.error("Failed to create recipe settings file", e);
            }
        } else {
            // Load existing settings
            try (var in = Files.newInputStream(configFile)) {
                props.load(in);
                LOGGER.debug("Loaded recipe settings from file");
            } catch (IOException e) {
                LOGGER.error("Failed to load recipe settings", e);
            }
        }
        
        // Parse settings
        generateExampleRecipes = Boolean.parseBoolean(props.getProperty("generateExampleRecipes", "true"));
        LOGGER.info("Recipe example generation is {}", generateExampleRecipes ? "enabled" : "disabled");
    }
    
    /**
     * Saves recipe settings to the properties file.
     */
    private static void saveRecipeSettings() {
        Path configFile = Paths.get(CONFIG_DIR, RECIPE_CONFIG_FILE);
        Properties props = new Properties();
        
        // Set properties
        props.setProperty("generateExampleRecipes", String.valueOf(generateExampleRecipes));
        
        // Save to file
        try {
            Files.createDirectories(configFile.getParent());
            try (var out = Files.newOutputStream(configFile)) {
                props.store(out, "EntityLootDrops Recipe Settings");
                LOGGER.debug("Saved recipe settings to file");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save recipe settings", e);
        }
    }
    
    /**
     * Creates the necessary directory structure for recipe configurations.
     */
    private static void createConfigDirectories() {
        try {
            // Create main recipes directory
            Path recipesDir = Paths.get(CONFIG_DIR, RECIPES_DIR);
            Files.createDirectories(recipesDir);
            
            // Create recipe type directories
            Path shapedDir = recipesDir.resolve(SHAPED_DIR);
            Files.createDirectories(shapedDir);
            
            Path shapelessDir = recipesDir.resolve(SHAPELESS_DIR);
            Files.createDirectories(shapelessDir);
            
            Path brewingDir = recipesDir.resolve(BREWING_DIR);
            Files.createDirectories(brewingDir);
            
            Path furnaceDir = recipesDir.resolve(FURNACE_DIR);
            Files.createDirectories(furnaceDir);
            
            Path smithingDir = recipesDir.resolve(SMITHING_DIR);
            Files.createDirectories(smithingDir);
            
            // Create example recipes if enabled
            if (generateExampleRecipes) {
                createExampleRecipes(shapedDir, shapelessDir, brewingDir, furnaceDir, smithingDir);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to create recipe config directories", e);
        }
    }
    
    /**
     * Disables example recipe generation by updating the configuration.
     */
    public static void disableExampleRecipes() {
        generateExampleRecipes = false;
        saveRecipeSettings();
        LOGGER.info("Example recipe generation has been disabled");
    }
    
    /**
     * Enables example recipe generation by updating the configuration.
     */
    public static void enableExampleRecipes() {
        generateExampleRecipes = true;
        saveRecipeSettings();
        LOGGER.info("Example recipe generation has been enabled");
    }
    
/**
 * Creates example recipe configurations with tag support.
 */
private static void createExampleRecipes(
        Path shapedDir, 
        Path shapelessDir, 
        Path brewingDir, 
        Path furnaceDir, 
        Path smithingDir) throws IOException {
    
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Create example shaped recipe with tag support
    Path exampleTagRecipePath = shapedDir.resolve("example_obsidian_recipe.json");
    if (!Files.exists(exampleTagRecipePath)) {
        // Create a JSON string manually to ensure proper tag format
        String recipeJson = """
            {
              "name": "obsidian_helmet",
              "type": "shaped",
              "outputItem": "minecraft:diamond_helmet",
              "outputCount": 1,
              "outputNbt": "{display:{Name:'{\\"text\\":\\"Obsidian Helmet\\",\\"color\\":\\"dark_purple\\"}'},Enchantments:[{id:\\"minecraft:protection\\",lvl:5}]}",
              "pattern": [
                "XXX",
                "X X"
              ],
              "key": {
                "X": {
                  "tag": "forge:obsidian"
                }
              },
              "group": "helmets",
              "enabled": true,
              "recipeToReplace": "minecraft:leather_helmet",
              "removeOriginal": false,
              "_comment": "Example shaped recipe using Forge obsidian tag",
              "craftCommands": [
                "tellraw {player} {\\"text\\":\\"You crafted an Obsidian Helmet!\\",\\"color\\":\\"dark_purple\\"}",
                "playsound minecraft:block.anvil.use player {player} ~ ~ ~ 0.5 1.2"
              ]
            }
            """;
        
        Files.write(exampleTagRecipePath, recipeJson.getBytes());
    }
    
    // Create example shapeless recipe with mixed tag and item ingredients
    Path exampleMixedRecipePath = shapelessDir.resolve("example_mixed_ingredients.json");
    if (!Files.exists(exampleMixedRecipePath)) {
        String recipeJson = """
            {
              "name": "mixed_ingredients_recipe",
              "type": "shapeless",
              "outputItem": "minecraft:golden_apple",
              "outputCount": 2,
              "ingredients": [
                "minecraft:apple",
                {
                  "tag": "forge:ingots/gold",
                  "count": 2
                },
                {
                  "item": "minecraft:diamond"
                }
              ],
              "enabled": true,
              "recipeToReplace": "minecraft:golden_apple",
              "removeOriginal": true,
              "_comment": "Example shapeless recipe mixing tags and items",
              "craftCommands": [
                "tellraw {player} {\\"text\\":\\"You used mixed ingredients!\\",\\"color\\":\\"gold\\"}"
              ]
            }
            """;
        
        Files.write(exampleMixedRecipePath, recipeJson.getBytes());
    }
    
    // Keep existing example recipes but update them
    createStandardExampleRecipes(shapedDir, shapelessDir, brewingDir, furnaceDir, smithingDir, gson);
    
    LOGGER.info("Created example recipe files with tag support and recipe replacement examples");
}

    
/**
 * Creates standard example recipes (non-tag versions).
 */
private static void createStandardExampleRecipes(
        Path shapedDir, 
        Path shapelessDir, 
        Path brewingDir, 
        Path furnaceDir, 
        Path smithingDir,
        Gson gson) throws IOException {
    
    // Create example shaped recipe
    Path exampleShapedPath = shapedDir.resolve("example_shaped_recipe.json");
    if (!Files.exists(exampleShapedPath)) {
        CustomRecipe shapedRecipe = new CustomRecipe();
        shapedRecipe.setName("diamond_helmet_with_enchants");
        shapedRecipe.setType("shaped");
        shapedRecipe.setOutputItem("minecraft:diamond_helmet");
        shapedRecipe.setOutputCount(1);
        shapedRecipe.setOutputNbt("{display:{Name:'{\"text\":\"Enchanted Diamond Helmet\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:protection\",lvl:4},{id:\"minecraft:unbreaking\",lvl:3}]}");
        shapedRecipe.setPattern(List.of("DDD", "D D"));
        
        Map<String, Object> key = new HashMap<>();
        key.put("D", "minecraft:diamond");
        shapedRecipe.setKey(key);
        
        shapedRecipe.setGroup("helmets");
        shapedRecipe.setEnabled(true);
        shapedRecipe.setComment("Example shaped recipe creating an enchanted diamond helmet");
        shapedRecipe.setRecipeToReplace("minecraft:diamond_helmet");
        shapedRecipe.setRemoveOriginal(true);
        shapedRecipe.setCraftCommands(List.of(
            "tellraw {player} {\"text\":\"You crafted an enchanted helmet!\",\"color\":\"aqua\"}",
            "playsound minecraft:block.anvil.use player {player} ~ ~ ~ 0.5 1.2"
        ));
        
        Files.write(exampleShapedPath, gson.toJson(shapedRecipe).getBytes());
    }
    
    // Create example shapeless recipe
    Path exampleShapelessPath = shapelessDir.resolve("example_shapeless_recipe.json");
    if (!Files.exists(exampleShapelessPath)) {
        CustomRecipe shapelessRecipe = new CustomRecipe();
        shapelessRecipe.setName("golden_apple_enhanced");
        shapelessRecipe.setType("shapeless");
        shapelessRecipe.setOutputItem("minecraft:enchanted_golden_apple");
        shapelessRecipe.setOutputCount(1);
        shapelessRecipe.setIngredients(List.of(
            "minecraft:golden_apple",
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:nether_star"
        ));
        shapelessRecipe.setEnabled(true);
        shapelessRecipe.setComment("Example shapeless recipe for enhanced golden apple");
        shapelessRecipe.setRecipeToReplace("minecraft:enchanted_golden_apple");
        shapelessRecipe.setRemoveOriginal(false);
        shapelessRecipe.setCraftCommands(List.of(
            "tellraw {player} {\"text\":\"You created an enhanced golden apple!\",\"color\":\"gold\"}",
            "effect give {player} minecraft:regeneration 30 2"
        ));
        
        Files.write(exampleShapelessPath, gson.toJson(shapelessRecipe).getBytes());
    }
    
    // Create example brewing recipe
    Path exampleBrewingPath = brewingDir.resolve("example_brewing_recipe.json");
    if (!Files.exists(exampleBrewingPath)) {
        BrewingRecipe brewingRecipe = new BrewingRecipe();
        brewingRecipe.setName("strength_potion_enhanced");
        brewingRecipe.setInputPotion("minecraft:potion:strength");
        brewingRecipe.setIngredient("minecraft:diamond");
        brewingRecipe.setOutputPotion("minecraft:potion:strength");
        brewingRecipe.setOutputNbt("{CustomPotionEffects:[{Id:5,Amplifier:2,Duration:3600}]}");
        brewingRecipe.setBrewingTime(400);
        brewingRecipe.setEnabled(true);
        brewingRecipe.setComment("Example brewing recipe for enhanced strength potion");
        brewingRecipe.setRecipeToReplace("minecraft:brewing_stand");
        brewingRecipe.setRemoveOriginal(false);
        brewingRecipe.setCraftCommands(List.of(
            "tellraw {player} {\"text\":\"You brewed an enhanced strength potion!\",\"color\":\"red\"}"
        ));
        
        Files.write(exampleBrewingPath, gson.toJson(brewingRecipe).getBytes());
    }
    
    // Create example furnace recipe
    Path exampleFurnacePath = furnaceDir.resolve("example_furnace_recipe.json");
    if (!Files.exists(exampleFurnacePath)) {
        FurnaceRecipe furnaceRecipe = new FurnaceRecipe();
        furnaceRecipe.setName("diamond_from_coal");
        furnaceRecipe.setInput("minecraft:coal_block");
        furnaceRecipe.setOutput("minecraft:diamond");
        furnaceRecipe.setOutputCount(1);
        furnaceRecipe.setExperience(10.0f);
        furnaceRecipe.setCookingTime(400);
        furnaceRecipe.setEnabled(true);
        furnaceRecipe.setComment("Example furnace recipe converting coal blocks to diamonds");
        furnaceRecipe.setRecipeToReplace("minecraft:iron_ingot_from_iron_ore");
        furnaceRecipe.setRemoveOriginal(false);
        furnaceRecipe.setCraftCommands(List.of(
            "tellraw {player} {\"text\":\"You smelted a diamond from coal!\",\"color\":\"aqua\"}"
        ));
        
        Files.write(exampleFurnacePath, gson.toJson(furnaceRecipe).getBytes());
    }
    
    // Create example smithing recipe
    Path exampleSmithingPath = smithingDir.resolve("example_smithing_recipe.json");
    if (!Files.exists(exampleSmithingPath)) {
        SmithingRecipe smithingRecipe = new SmithingRecipe();
        smithingRecipe.setName("netherite_helmet_enhanced");
        smithingRecipe.setTemplate("minecraft:netherite_upgrade_smithing_template");
        smithingRecipe.setBaseItem("minecraft:diamond_helmet");
        smithingRecipe.setAdditionItem("minecraft:netherite_ingot");
        smithingRecipe.setOutputItem("minecraft:netherite_helmet");
        smithingRecipe.setOutputCount(1);
        smithingRecipe.setOutputNbt("{display:{Name:'{\"text\":\"Enhanced Netherite Helmet\",\"color\":\"dark_red\"}'},Enchantments:[{id:\"minecraft:protection\",lvl:5}]}");
        smithingRecipe.setCopyNbt(true);
        smithingRecipe.setEnabled(true);
        smithingRecipe.setComment("Example smithing recipe for enhanced netherite helmet with template");
        smithingRecipe.setRecipeToReplace("minecraft:netherite_helmet_smithing");
        smithingRecipe.setRemoveOriginal(true);
        smithingRecipe.setCraftCommands(List.of(
            "tellraw {player} {\"text\":\"You forged an enhanced netherite helmet!\",\"color\":\"dark_red\"}",
            "playsound minecraft:block.smithing_table.use player {player} ~ ~ ~ 1.0 1.0"
    ));
    
    Files.write(exampleSmithingPath, gson.toJson(smithingRecipe).getBytes());
    }
}
    
    /**
     * Loads all recipe types from their respective directories.
     */
    private static void loadAllRecipes() {
        // Clear existing recipes to prevent duplicates during reload
        shapedRecipes.clear();
        shapelessRecipes.clear();
        brewingRecipes.clear();
        furnaceRecipes.clear();
        smithingRecipes.clear();
        
        // Load each recipe type
        loadShapedRecipes();
        loadShapelessRecipes();
        loadBrewingRecipes();
        loadFurnaceRecipes();
        loadSmithingRecipes();
    }
    
    /**
     * Loads shaped recipes from the Shaped directory.
     */
    private static void loadShapedRecipes() {
        Path shapedDir = Paths.get(CONFIG_DIR, RECIPES_DIR, SHAPED_DIR);
        List<CustomRecipe> recipes = loadRecipesFromDirectory(shapedDir, CustomRecipe.class);
        
        for (CustomRecipe recipe : recipes) {
            if (recipe.isEnabled() && recipe.isShaped()) {
                shapedRecipes.add(recipe);
                LOGGER.debug("Loaded shaped recipe: {}", recipe.getName());
            }
        }
        
        LOGGER.info("Loaded {} shaped recipes", shapedRecipes.size());
    }
    
    /**
     * Loads shapeless recipes from the Shapeless directory.
     */
    private static void loadShapelessRecipes() {
        Path shapelessDir = Paths.get(CONFIG_DIR, RECIPES_DIR, SHAPELESS_DIR);
        List<CustomRecipe> recipes = loadRecipesFromDirectory(shapelessDir, CustomRecipe.class);
        
        for (CustomRecipe recipe : recipes) {
            if (recipe.isEnabled() && recipe.isShapeless()) {
                shapelessRecipes.add(recipe);
                LOGGER.debug("Loaded shapeless recipe: {}", recipe.getName());
            }
        }
        
        LOGGER.info("Loaded {} shapeless recipes", shapelessRecipes.size());
    }
    
    /**
     * Loads brewing recipes from the Brewing directory.
     */
    private static void loadBrewingRecipes() {
        Path brewingDir = Paths.get(CONFIG_DIR, RECIPES_DIR, BREWING_DIR);
        List<BrewingRecipe> recipes = loadRecipesFromDirectory(brewingDir, BrewingRecipe.class);
        
        for (BrewingRecipe recipe : recipes) {
            if (recipe.isEnabled()) {
                brewingRecipes.add(recipe);
                LOGGER.debug("Loaded brewing recipe: {}", recipe.getName());
            }
        }
        
        LOGGER.info("Loaded {} brewing recipes", brewingRecipes.size());
    }
    
    /**
     * Loads furnace recipes from the Furnace directory.
     */
    private static void loadFurnaceRecipes() {
        Path furnaceDir = Paths.get(CONFIG_DIR, RECIPES_DIR, FURNACE_DIR);
        List<FurnaceRecipe> recipes = loadRecipesFromDirectory(furnaceDir, FurnaceRecipe.class);
        
        for (FurnaceRecipe recipe : recipes) {
            if (recipe.isEnabled()) {
                furnaceRecipes.add(recipe);
                LOGGER.debug("Loaded furnace recipe: {}", recipe.getName());
            }
        }
        
        LOGGER.info("Loaded {} furnace recipes", furnaceRecipes.size());
    }
    
    /**
     * Loads smithing recipes from the Smithing directory.
     */
    private static void loadSmithingRecipes() {
        Path smithingDir = Paths.get(CONFIG_DIR, RECIPES_DIR, SMITHING_DIR);
        List<SmithingRecipe> recipes = loadRecipesFromDirectory(smithingDir, SmithingRecipe.class);
        
        for (SmithingRecipe recipe : recipes) {
            if (recipe.isEnabled()) {
                smithingRecipes.add(recipe);
                LOGGER.debug("Loaded smithing recipe: {}", recipe.getName());
            }
        }
        
        LOGGER.info("Loaded {} smithing recipes", smithingRecipes.size());
    }
    
    /**
     * Generic method to load recipes from a directory.
     */
    private static <T> List<T> loadRecipesFromDirectory(Path directory, Class<T> recipeClass) {
        List<T> recipes = new ArrayList<>();
        
        if (!Files.exists(directory)) {
            LOGGER.debug("Recipe directory does not exist: {}", directory);
            return recipes;
        }
        
        try {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Gson gson = new Gson();
                        T recipe = gson.fromJson(content, recipeClass);
                        
                        if (recipe != null) {
                            recipes.add(recipe);
                            LOGGER.debug("Loaded recipe from file: {}", path.getFileName());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load recipe from file {}: {}", path.getFileName(), e.getMessage());
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Failed to read recipe directory {}: {}", directory, e.getMessage());
        }
        
        return recipes;
    }
    
    // Getter methods for accessing loaded recipes (thread-safe)
    public static List<CustomRecipe> getShapedRecipes() {
        return isReloading ? Collections.emptyList() : new ArrayList<>(shapedRecipes);
    }
    
    public static List<CustomRecipe> getShapelessRecipes() {
        return isReloading ? Collections.emptyList() : new ArrayList<>(shapelessRecipes);
    }
    
    public static List<BrewingRecipe> getBrewingRecipes() {
        return isReloading ? Collections.emptyList() : new ArrayList<>(brewingRecipes);
    }
    
    public static List<FurnaceRecipe> getFurnaceRecipes() {
        return isReloading ? Collections.emptyList() : new ArrayList<>(furnaceRecipes);
    }
    
    public static List<SmithingRecipe> getSmithingRecipes() {
        return isReloading ? Collections.emptyList() : new ArrayList<>(smithingRecipes);
    }
    
    /**
     * Gets the total count of all loaded recipes.
     */
    public static int getTotalRecipeCount() {
        if (isReloading) {
            return 0;
        }
        return shapedRecipes.size() + shapelessRecipes.size() + brewingRecipes.size() + 
               furnaceRecipes.size() + smithingRecipes.size();
    }
    
    /**
     * Gets the count of recipes that are configured as replacements.
     */
    public static int getReplacementRecipeCount() {
        if (isReloading) {
            return 0;
        }
        
        int count = 0;
        
        // Count shaped replacement recipes
        for (CustomRecipe recipe : shapedRecipes) {
            if (recipe.isReplacement()) {
                count++;
            }
        }
        
        // Count shapeless replacement recipes
        for (CustomRecipe recipe : shapelessRecipes) {
            if (recipe.isReplacement()) {
                count++;
            }
        }
        
        // Count brewing replacement recipes
        for (BrewingRecipe recipe : brewingRecipes) {
            if (recipe.isReplacement()) {
                count++;
            }
        }
        
        // Count furnace replacement recipes
        for (FurnaceRecipe recipe : furnaceRecipes) {
            if (recipe.isReplacement()) {
                count++;
            }
        }
        
        // Count smithing replacement recipes
        for (SmithingRecipe recipe : smithingRecipes) {
            if (recipe.isReplacement()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Gets the count of shaped recipes.
     */
    public static int getShapedRecipeCount() {
        return isReloading ? 0 : shapedRecipes.size();
    }
    
    /**
     * Gets the count of shapeless recipes.
     */
    public static int getShapelessRecipeCount() {
        return isReloading ? 0 : shapelessRecipes.size();
    }
    
    /**
     * Gets the count of brewing recipes.
     */
    public static int getBrewingRecipeCount() {
        return isReloading ? 0 : brewingRecipes.size();
    }
    
    /**
     * Gets the count of furnace recipes.
     */
    public static int getFurnaceRecipeCount() {
        return isReloading ? 0 : furnaceRecipes.size();
    }
    
    /**
     * Gets the count of smithing recipes.
     */
    public static int getSmithingRecipeCount() {
        return isReloading ? 0 : smithingRecipes.size();
    }
    
    /**
     * Gets detailed recipe statistics.
     */
    public static Map<String, Integer> getRecipeStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        if (isReloading) {
            stats.put("total", 0);
            stats.put("shaped", 0);
            stats.put("shapeless", 0);
            stats.put("brewing", 0);
            stats.put("furnace", 0);
            stats.put("smithing", 0);
            stats.put("replacements", 0);
            return stats;
        }
        
        stats.put("shaped", shapedRecipes.size());
        stats.put("shapeless", shapelessRecipes.size());
        stats.put("brewing", brewingRecipes.size());
        stats.put("furnace", furnaceRecipes.size());
        stats.put("smithing", smithingRecipes.size());
        stats.put("total", getTotalRecipeCount());
        stats.put("replacements", getReplacementRecipeCount());
        
        return stats;
    }
    
    /**
     * Checks if any recipes are loaded.
     */
    public static boolean hasRecipes() {
        return getTotalRecipeCount() > 0;
    }
    
    /**
     * Gets all custom recipes (shaped and shapeless combined).
     */
    public static List<CustomRecipe> getAllCustomRecipes() {
        if (isReloading) {
            return Collections.emptyList();
        }
        
        List<CustomRecipe> allRecipes = new ArrayList<>();
        allRecipes.addAll(shapedRecipes);
        allRecipes.addAll(shapelessRecipes);
        return allRecipes;
    }
    
    /**
     * Finds a recipe by name across all recipe types.
     */
    public static BaseRecipe findRecipeByName(String name) {
        if (isReloading || name == null || name.isEmpty()) {
            return null;
        }
        
        // Search in shaped recipes
        for (CustomRecipe recipe : shapedRecipes) {
            if (name.equals(recipe.getName())) {
                return recipe;
            }
        }
        
        // Search in shapeless recipes
        for (CustomRecipe recipe : shapelessRecipes) {
            if (name.equals(recipe.getName())) {
                return recipe;
            }
        }
        
        // Search in brewing recipes
        for (BrewingRecipe recipe : brewingRecipes) {
            if (name.equals(recipe.getName())) {
                return recipe;
            }
        }
        
        // Search in furnace recipes
        for (FurnaceRecipe recipe : furnaceRecipes) {
            if (name.equals(recipe.getName())) {
                return recipe;
            }
        }
        
        // Search in smithing recipes
        for (SmithingRecipe recipe : smithingRecipes) {
            if (name.equals(recipe.getName())) {
                return recipe;
            }
        }
        
        return null;
    }
    
    /**
     * Reloads all recipe configurations safely.
     */
    public static void reloadConfig() {
        LOGGER.info("Reloading recipe configuration...");
        loadConfig();
        LOGGER.info("Recipe configuration reloaded successfully");
    }
    
    /**
     * Checks if recipes are currently being reloaded.
     */
    public static boolean isReloading() {
        return isReloading;
    }
}

