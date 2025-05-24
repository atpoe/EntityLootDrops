package net.poe.entitylootdrops;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Configuration class for custom recipes with NBT data support.
 * Allows users to define their own crafting recipes with custom NBT data.
 */
public class RecipeConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String RECIPES_DIR = "Recipes";
    private static final String SHAPED_DIR = "Shaped";
    private static final String SHAPELESS_DIR = "Shapeless";
    
    // Storage for loaded recipes
    private static List<CustomRecipe> shapedRecipes = new ArrayList<>();
    private static List<CustomRecipe> shapelessRecipes = new ArrayList<>();
    
    /**
     * Represents a custom recipe configuration.
     */
    public static class CustomRecipe {
        private String name;                // Recipe name (used for ID)
        private String type;                // "shaped" or "shapeless"
        private String outputItem;          // Output item ID
        private int outputCount = 1;        // Output item count
        private String outputNbt;           // NBT data for output item
        private List<String> pattern;       // Pattern for shaped recipes (e.g. ["XXX", "X X", "XXX"])
        private Map<String, String> key;    // Key mapping for shaped recipes (e.g. {"X": "minecraft:iron_ingot"})
        private List<String> ingredients;   // Ingredients for shapeless recipes
        private String group = "";          // Recipe group
        private String _comment;            // Comment for documentation
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
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
        
        public Map<String, String> getKey() { return key; }
        public void setKey(Map<String, String> key) { this.key = key; }
        
        public List<String> getIngredients() { return ingredients; }
        public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
        
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        
        public String getComment() { return _comment; }
        public void setComment(String comment) { this._comment = comment; }
        
        // Helper methods
        public boolean isShaped() { return "shaped".equalsIgnoreCase(type); }
        public boolean isShapeless() { return "shapeless".equalsIgnoreCase(type); }
        public boolean hasOutputNbt() { return outputNbt != null && !outputNbt.isEmpty(); }
        
        /**
         * Creates an ItemStack for the output item with NBT data if specified.
         */
        public ItemStack createOutputStack() {
            try {
                ResourceLocation itemId = new ResourceLocation(outputItem);
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item == null) {
                    LOGGER.error("Invalid output item ID in recipe {}: {}", name, outputItem);
                    return ItemStack.EMPTY;
                }
                
                ItemStack stack = new ItemStack(item, outputCount);
                
                if (hasOutputNbt()) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(outputNbt);
                        stack.setTag(nbt);
                    } catch (Exception e) {
                        LOGGER.error("Invalid NBT data in recipe {}: {}", name, e.getMessage());
                    }
                }
                
                return stack;
            } catch (Exception e) {
                LOGGER.error("Error creating output stack for recipe {}: {}", name, e.getMessage());
                return ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * Loads all custom recipe configurations.
     */
    public static void loadConfig() {
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Load all recipes
        loadAllRecipes();
        
        LOGGER.info("Loaded recipe configuration: {} shaped recipes, {} shapeless recipes",
            shapedRecipes.size(), shapelessRecipes.size());
    }
    
    /**
     * Creates the necessary directory structure for recipe configurations.
     */
    private static void createConfigDirectories() {
        try {
            // Create main recipes directory
            Path recipesDir = Paths.get(CONFIG_DIR, RECIPES_DIR);
            Files.createDirectories(recipesDir);
            
            // Create shaped recipes directory
            Path shapedDir = recipesDir.resolve(SHAPED_DIR);
            Files.createDirectories(shapedDir);
            
            // Create shapeless recipes directory
            Path shapelessDir = recipesDir.resolve(SHAPELESS_DIR);
            Files.createDirectories(shapelessDir);
            
            // Create example recipes
            createExampleRecipes(shapedDir, shapelessDir);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create recipe config directories", e);
        }
    }
    
    /**
     * Creates example recipe configurations.
     */
    private static void createExampleRecipes(Path shapedDir, Path shapelessDir) throws IOException {
        // Create example shaped recipe
        Path exampleShapedPath = shapedDir.resolve("example_shaped_recipe.json");
        if (!Files.exists(exampleShapedPath)) {
            CustomRecipe shapedRecipe = new CustomRecipe();
            shapedRecipe.setName("diamond_helmet_with_enchants");
            shapedRecipe.setType("shaped");
            shapedRecipe.setOutputItem("minecraft:diamond_helmet");
            shapedRecipe.setOutputCount(1);
            shapedRecipe.setOutputNbt("{display:{Name:'{\"text\":\"Enchanted Helmet\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:protection\",lvl:4},{id:\"minecraft:unbreaking\",lvl:3}]}");
            shapedRecipe.setPattern(List.of("XXX", "X X"));
            
            Map<String, String> key = new HashMap<>();
            key.put("X", "minecraft:diamond");
            shapedRecipe.setKey(key);
            
            shapedRecipe.setGroup("helmets");
            shapedRecipe.setComment("Example shaped recipe with NBT data");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleShapedPath, gson.toJson(shapedRecipe).getBytes());
        }
        
        // Create example shapeless recipe
        Path exampleShapelessPath = shapelessDir.resolve("example_shapeless_recipe.json");
        if (!Files.exists(exampleShapelessPath)) {
            CustomRecipe shapelessRecipe = new CustomRecipe();
            shapelessRecipe.setName("golden_apple_from_gold_blocks");
            shapelessRecipe.setType("shapeless");
            shapelessRecipe.setOutputItem("minecraft:golden_apple");
            shapelessRecipe.setOutputCount(1);
            shapelessRecipe.setIngredients(List.of(
                "minecraft:gold_block",
                "minecraft:gold_block",
                "minecraft:apple"
            ));
            shapelessRecipe.setComment("Example shapeless recipe");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleShapelessPath, gson.toJson(shapelessRecipe).getBytes());
        }
        
        // Create example shaped recipe with complex NBT
        Path exampleComplexNbtPath = shapedDir.resolve("example_complex_nbt_recipe.json");
        if (!Files.exists(exampleComplexNbtPath)) {
            CustomRecipe complexRecipe = new CustomRecipe();
            complexRecipe.setName("super_sword");
            complexRecipe.setType("shaped");
            complexRecipe.setOutputItem("minecraft:diamond_sword");
            complexRecipe.setOutputCount(1);
            complexRecipe.setOutputNbt("{display:{Name:'{\"text\":\"Super Sword\",\"color\":\"gold\",\"bold\":true}',Lore:['{\"text\":\"A legendary weapon\",\"color\":\"purple\",\"italic\":true}']},Enchantments:[{id:\"minecraft:sharpness\",lvl:10},{id:\"minecraft:looting\",lvl:5}],AttributeModifiers:[{AttributeName:\"generic.attack_damage\",Name:\"generic.attack_damage\",Amount:20,Operation:0,UUID:[I;123,456,789,0],Slot:\"mainhand\"}],Unbreakable:1b,HideFlags:63}");
            complexRecipe.setPattern(List.of(
                " X ",
                " X ",
                " Y "
            ));
            
            Map<String, String> key = new HashMap<>();
            key.put("X", "minecraft:nether_star");
            key.put("Y", "minecraft:blaze_rod");
            complexRecipe.setKey(key);
            
            complexRecipe.setGroup("weapons");
            complexRecipe.setComment("Example recipe with complex NBT data");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleComplexNbtPath, gson.toJson(complexRecipe).getBytes());
        }
    }
    
    /**
     * Loads all recipe configurations.
     */
    private static void loadAllRecipes() {
        // Clear existing recipes
        shapedRecipes.clear();
        shapelessRecipes.clear();
        
        try {
            // Load shaped recipes
            Path shapedDir = Paths.get(CONFIG_DIR, RECIPES_DIR, SHAPED_DIR);
            if (Files.exists(shapedDir)) {
                Files.list(shapedDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Gson gson = new Gson();
                            CustomRecipe recipe = gson.fromJson(json, CustomRecipe.class);
                            
                            if (recipe != null && recipe.getName() != null && recipe.getOutputItem() != null) {
                                if (recipe.getType() == null) {
                                    recipe.setType("shaped"); // Default to shaped if not specified
                                }
                                
                                if (recipe.isShaped()) {
                                    shapedRecipes.add(recipe);
                                    LOGGER.debug("Loaded shaped recipe: {}", recipe.getName());
                                } else {
                                    LOGGER.warn("Recipe in shaped directory has incorrect type: {}", recipe.getName());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading shaped recipe file: {}", path, e);
                        }
                    });
            }
            
            // Load shapeless recipes
            Path shapelessDir = Paths.get(CONFIG_DIR, RECIPES_DIR, SHAPELESS_DIR);
            if (Files.exists(shapelessDir)) {
                Files.list(shapelessDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Gson gson = new Gson();
                            CustomRecipe recipe = gson.fromJson(json, CustomRecipe.class);
                            
                            if (recipe != null && recipe.getName() != null && recipe.getOutputItem() != null) {
                                if (recipe.getType() == null) {
                                    recipe.setType("shapeless"); // Default to shapeless if not specified
                                }
                                
                                if (recipe.isShapeless()) {
                                    shapelessRecipes.add(recipe);
                                    LOGGER.debug("Loaded shapeless recipe: {}", recipe.getName());
                                } else {
                                    LOGGER.warn("Recipe in shapeless directory has incorrect type: {}", recipe.getName());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading shapeless recipe file: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load recipes", e);
        }
    }
    
    /**
     * Gets all shaped recipes.
     */
    public static List<CustomRecipe> getShapedRecipes() {
        return Collections.unmodifiableList(shapedRecipes);
    }
    
    /**
     * Gets all shapeless recipes.
     */
    public static List<CustomRecipe> getShapelessRecipes() {
        return Collections.unmodifiableList(shapelessRecipes);
    }
}
