package net.poe.entitylootdrops.recipes.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.poe.entitylootdrops.recipes.model.RecipeEntry;

/**
 * Handles loading recipe configurations from organized folders.
 * Updated to support both string and datapack object formats for ingredients.
 * Removed removeOriginal option - only replaceRecipe is used now.
 */
public class RecipeConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String RECIPES_DIR = "Recipes";
    
    // Recipe type directories
    private static final String CRAFTING_DIR = "Crafting";
    private static final String FURNACE_DIR = "Furnace";
    private static final String BLASTING_DIR = "Blasting";
    private static final String SMOKING_DIR = "Smoking";
    private static final String CAMPFIRE_DIR = "Campfire";
    private static final String STONECUTTING_DIR = "Stonecutting";
    private static final String SMITHING_DIR = "Smithing";
    
    // Crafting subdirectories
    private static final String SHAPED_DIR = "Shaped";
    private static final String SHAPELESS_DIR = "Shapeless";
    
    private final RecipeConfigManager configManager;
    
    public RecipeConfigLoader(RecipeConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public void loadConfig() {
        createConfigDirectories();
        loadAllRecipes();
    }
    
    private void createConfigDirectories() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            Path recipesDir = configDir.resolve(RECIPES_DIR);
            Files.createDirectories(recipesDir);
            
            // Create directories for each recipe type
            createRecipeTypeDirectories(recipesDir);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create recipe config directories", e);
        }
    }
    
    private void createRecipeTypeDirectories(Path recipesDir) throws IOException {
        // Create main recipe type directories with examples
        createCraftingDirectory(recipesDir);
        createRecipeTypeDirectory(recipesDir, FURNACE_DIR, this::createFurnaceExamples);
        createRecipeTypeDirectory(recipesDir, BLASTING_DIR, this::createBlastingExamples);
        createRecipeTypeDirectory(recipesDir, SMOKING_DIR, this::createSmokingExamples);
        createRecipeTypeDirectory(recipesDir, CAMPFIRE_DIR, this::createCampfireExamples);
        createRecipeTypeDirectory(recipesDir, STONECUTTING_DIR, this::createStonecuttingExamples);
        createRecipeTypeDirectory(recipesDir, SMITHING_DIR, this::createSmithingExamples);
    }
    
    @FunctionalInterface
    private interface ExampleCreator {
        void createExamples(Path dir) throws IOException;
    }
    
    private void createCraftingDirectory(Path recipesDir) throws IOException {
        Path craftingDir = recipesDir.resolve(CRAFTING_DIR);
        Files.createDirectories(craftingDir);
        
        // Create Shaped subdirectory
        Path shapedDir = craftingDir.resolve(SHAPED_DIR);
        Files.createDirectories(shapedDir);
        createShapedExamples(shapedDir);
        
        // Create Shapeless subdirectory
        Path shapelessDir = craftingDir.resolve(SHAPELESS_DIR);
        Files.createDirectories(shapelessDir);
        createShapelessExamples(shapelessDir);
        
        // Create custom directory for general crafting recipes
        Path craftingCustomDir = craftingDir.resolve("custom");
        Files.createDirectories(craftingCustomDir);
    }
    
    private void createRecipeTypeDirectory(Path recipesDir, String dirName, ExampleCreator exampleCreator) throws IOException {
        Path typeDir = recipesDir.resolve(dirName);
        Files.createDirectories(typeDir);
        
        // Create examples directory
        Path examplesDir = typeDir.resolve("examples");
        Files.createDirectories(examplesDir);
        exampleCreator.createExamples(examplesDir);
        
        // Create a custom directory for users
        Path customDir = typeDir.resolve("custom");
        Files.createDirectories(customDir);
    }
    
    private void createShapedExamples(Path shapedDir) throws IOException {
        Path shapedFile = shapedDir.resolve("shaped_examples.json");
        if (!Files.exists(shapedFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            // Example 1: Enchanted Diamond Sword (String format)
            RecipeEntry shapedExample = new RecipeEntry();
            shapedExample.setName("enchanted_diamond_sword");
            shapedExample.setType("shaped");
            shapedExample.setEnabled(true);
            shapedExample.setReplaceRecipe("minecraft:diamond_sword");
            shapedExample.setOutputItem("minecraft:diamond_sword");
            shapedExample.setOutputCount(1);
            shapedExample.setOutputNbt("{Enchantments:[{id:\"minecraft:sharpness\",lvl:5}]}");
            shapedExample.setPattern(Arrays.asList(" D ", " D ", " S "));
            
            Map<String, Object> key = new HashMap<>();
            key.put("D", "minecraft:diamond");
            key.put("S", "minecraft:stick");
            shapedExample.setKey(key);
            
            shapedExample.setGroup("weapons");
            shapedExample.setCraftCommands(Arrays.asList("tellraw @a {\"text\":\"Someone crafted an enchanted diamond sword!\",\"color\":\"gold\"}"));
            shapedExample.setCommandChance(100.0f);
            shapedExample.setComment("enchanted diamond sword replacement - uses string format");
            examples.add(shapedExample);
            
            // Example 2: Custom Pickaxe (Mixed format)
            RecipeEntry shapedExample2 = new RecipeEntry();
            shapedExample2.setName("custom_pickaxe");
            shapedExample2.setType("shaped");
            shapedExample2.setEnabled(false);
            shapedExample2.setOutputItem("minecraft:diamond_pickaxe");
            shapedExample2.setOutputCount(1);
            shapedExample2.setOutputNbt("{Enchantments:[{id:\"minecraft:efficiency\",lvl:3},{id:\"minecraft:unbreaking\",lvl:2}]}");
            shapedExample2.setPattern(Arrays.asList("DDD", " S ", " S "));
            
            Map<String, Object> key2 = new HashMap<>();
            key2.put("D", "minecraft:diamond");
            
            Map<String, String> stickObj = new HashMap<>();
            stickObj.put("item", "minecraft:stick");
            key2.put("S", stickObj);
            
            shapedExample2.setKey(key2);
            
            shapedExample2.setGroup("tools");
            shapedExample2.setCraftCommands(Arrays.asList("particle minecraft:enchant ~ ~ ~ 0.5 0.5 0.5 0.1 30"));
            shapedExample2.setCommandChance(100.0f);
            shapedExample2.setComment("custom enchanted diamond pickaxe - shows mixed string and object formats");
            examples.add(shapedExample2);
            
            // Example 3: Bounty Board (Datapack object format)
            RecipeEntry bountyBoard = new RecipeEntry();
            bountyBoard.setName("bountyboard_replacement");
            bountyBoard.setType("shaped");
            bountyBoard.setEnabled(false);
            bountyBoard.setReplaceRecipe("bountiful:bountyboard");
            bountyBoard.setOutputItem("bountiful:bountyboard");
            bountyBoard.setOutputCount(1);
            bountyBoard.setOutputNbt("");
            bountyBoard.setPattern(Arrays.asList("ABA", "CDC", "ABA"));
            
            Map<String, Object> bountyKey = new HashMap<>();
            
            Map<String, String> planksObj = new HashMap<>();
            planksObj.put("item", "minecraft:oak_planks");
            bountyKey.put("A", planksObj);
            
            Map<String, String> logObj = new HashMap<>();
            logObj.put("item", "minecraft:oak_log");
            bountyKey.put("B", logObj);
            
            Map<String, String> paperObj = new HashMap<>();
            paperObj.put("item", "minecraft:paper");
            bountyKey.put("C", paperObj);
            
            Map<String, String> stoneObj = new HashMap<>();
            stoneObj.put("item", "mmorpg:stone/3");
            bountyKey.put("D", stoneObj);
            
            bountyBoard.setKey(bountyKey);
            
            bountyBoard.setGroup("block");
            bountyBoard.setCraftCommands(Arrays.asList(""));
            bountyBoard.setCommandChance(100.0f);
            bountyBoard.setComment("bountyboard - uses datapack object format");
            examples.add(bountyBoard);
            
            // Example 4: Tag-based recipe
            RecipeEntry tagExample = new RecipeEntry();
            tagExample.setName("tag_based_sword");
            tagExample.setType("shaped");
            tagExample.setEnabled(false);
            tagExample.setOutputItem("minecraft:iron_sword");
            tagExample.setOutputCount(1);
            tagExample.setOutputNbt("");
            tagExample.setPattern(Arrays.asList(" I ", " I ", " S "));
            
            Map<String, Object> tagKey = new HashMap<>();
            
            Map<String, String> ironTagObj = new HashMap<>();
            ironTagObj.put("tag", "forge:ingots/iron");
            tagKey.put("I", ironTagObj);
            
            Map<String, String> stickTagObj = new HashMap<>();
            stickTagObj.put("tag", "forge:rods/wooden");
            tagKey.put("S", stickTagObj);
            
            tagExample.setKey(tagKey);
            
            tagExample.setGroup("weapons");
            tagExample.setCraftCommands(Arrays.asList("tellraw @p {\"text\":\"Crafted with tags!\",\"color\":\"green\"}"));
            tagExample.setCommandChance(100.0f);
            tagExample.setComment("iron sword using forge tags - shows tag format");
            examples.add(tagExample);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(shapedFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createShapelessExamples(Path shapelessDir) throws IOException {
        Path shapelessFile = shapelessDir.resolve("shapeless_examples.json");
        if (!Files.exists(shapelessFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            // Example 1: Easy Golden Apple (Mixed format)
            RecipeEntry shapelessExample = new RecipeEntry();
            shapelessExample.setName("golden_apple_easy");
            shapelessExample.setType("shapeless");
            shapelessExample.setEnabled(false);
            shapelessExample.setOutputItem("minecraft:golden_apple");
            shapelessExample.setOutputCount(1);
            shapelessExample.setOutputNbt("");
            
            List<Object> ingredients = new ArrayList<>();
            ingredients.add("minecraft:apple");
            
            Map<String, String> goldObj = new HashMap<>();
            goldObj.put("item", "minecraft:gold_ingot");
            ingredients.add(goldObj);
            
            shapelessExample.setIngredients(ingredients);
            shapelessExample.setGroup("food");
            shapelessExample.setCraftCommands(Arrays.asList("particle minecraft:end_rod ~ ~ ~ 0.5 0.5 0.5 0.1 20"));
            shapelessExample.setCommandChance(50.0f);
            shapelessExample.setComment("easier golden apple recipe - shows mixed string and object formats");
            examples.add(shapelessExample);
            
            // Example 2: Quick Bread (String format)
            RecipeEntry shapelessExample2 = new RecipeEntry();
            shapelessExample2.setName("quick_bread");
            shapelessExample2.setType("shapeless");
            shapelessExample2.setEnabled(false);
            shapelessExample2.setOutputItem("minecraft:bread");
            shapelessExample2.setOutputCount(3);
            shapelessExample2.setOutputNbt("");
            
            List<Object> ingredients2 = new ArrayList<>();
            ingredients2.add("minecraft:wheat");
            ingredients2.add("minecraft:wheat");
            ingredients2.add("minecraft:water_bucket");
            shapelessExample2.setIngredients(ingredients2);
            
            shapelessExample2.setGroup("food");
            shapelessExample2.setCraftCommands(Arrays.asList("playsound minecraft:entity.player.burp player @p ~ ~ ~ 1 1"));
            shapelessExample2.setCommandChance(25.0f);
            shapelessExample2.setComment("quick bread recipe with water bucket - uses string format");
            examples.add(shapelessExample2);
            
            // Example 3: Tag-based shapeless recipe
            RecipeEntry tagShapeless = new RecipeEntry();
            tagShapeless.setName("any_wood_sticks");
            tagShapeless.setType("shapeless");
            tagShapeless.setEnabled(false);
            tagShapeless.setOutputItem("minecraft:stick");
            tagShapeless.setOutputCount(8);
            tagShapeless.setOutputNbt("");
            
            List<Object> tagIngredients = new ArrayList<>();
            
            Map<String, String> logTagObj = new HashMap<>();
            logTagObj.put("tag", "minecraft:logs");
            tagIngredients.add(logTagObj);
            
            tagShapeless.setIngredients(tagIngredients);
            tagShapeless.setGroup("materials");
            tagShapeless.setCraftCommands(Arrays.asList("tellraw @p {\"text\":\"Made sticks from any log!\",\"color\":\"yellow\"}"));
            tagShapeless.setCommandChance(100.0f);
            tagShapeless.setComment("sticks from any log using minecraft:logs tag");
            examples.add(tagShapeless);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(shapelessFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createFurnaceExamples(Path examplesDir) throws IOException {
        Path furnaceFile = examplesDir.resolve("furnace_examples.json");
        if (!Files.exists(furnaceFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            RecipeEntry furnaceExample = new RecipeEntry();
            furnaceExample.setName("coal_to_diamond");
            furnaceExample.setType("furnace");
            furnaceExample.setEnabled(false);
            furnaceExample.setInput("minecraft:coal");
            furnaceExample.setOutputItem("minecraft:diamond");
            furnaceExample.setOutputCount(1);
            furnaceExample.setOutputNbt("");
            furnaceExample.setExperience(10.0f);
            furnaceExample.setCookingTime(400);
            furnaceExample.setGroup("alchemy");
            furnaceExample.setCraftCommands(Arrays.asList("tellraw @p {\"text\":\"You turned coal into diamond!\",\"color\":\"aqua\"}"));
            furnaceExample.setCommandChance(100.0f);
            furnaceExample.setComment("smelt coal into diamond");
            examples.add(furnaceExample);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(furnaceFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createBlastingExamples(Path examplesDir) throws IOException {
        Path blastingFile = examplesDir.resolve("blasting_examples.json");
        if (!Files.exists(blastingFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            RecipeEntry blastingExample = new RecipeEntry();
            blastingExample.setName("fast_iron_ingot");
            blastingExample.setType("blasting");
            blastingExample.setEnabled(false);
            blastingExample.setReplaceRecipe("minecraft:iron_ingot_from_blasting_raw_iron");
            blastingExample.setInput("minecraft:raw_iron");
            blastingExample.setOutputItem("minecraft:iron_ingot");
            blastingExample.setOutputCount(2);
            blastingExample.setOutputNbt("");
            blastingExample.setExperience(1.4f);
            blastingExample.setCookingTime(50);
            blastingExample.setGroup("smelting");
            blastingExample.setCraftCommands(Arrays.asList("particle minecraft:flame ~ ~ ~ 0.3 0.3 0.3 0.1 10"));
            blastingExample.setCommandChance(75.0f);
            blastingExample.setComment("double iron ingot output from blasting");
            examples.add(blastingExample);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(blastingFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createSmokingExamples(Path examplesDir) throws IOException {
        Path smokingFile = examplesDir.resolve("smoking_examples.json");
        if (!Files.exists(smokingFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            RecipeEntry smokingExample = new RecipeEntry();
            smokingExample.setName("super_cooked_beef");
            smokingExample.setType("smoking");
            smokingExample.setEnabled(false);
            smokingExample.setInput("minecraft:raw_beef");
            smokingExample.setOutputItem("minecraft:cooked_beef");
            smokingExample.setOutputCount(2);
            smokingExample.setOutputNbt("{display:{Name:'{\"text\":\"Super Cooked Beef\",\"color\":\"gold\"}'}}");
            smokingExample.setExperience(0.7f);
            smokingExample.setCookingTime(100);
            smokingExample.setGroup("cooking");
            smokingExample.setCraftCommands(Arrays.asList("playsound minecraft:entity.player.burp player @p ~ ~ ~ 1 1"));
            smokingExample.setCommandChance(100.0f);
            smokingExample.setComment("enhanced beef smoking with custom name");
            examples.add(smokingExample);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(smokingFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createCampfireExamples(Path examplesDir) throws IOException {
        Path campfireFile = examplesDir.resolve("campfire_examples.json");
        if (!Files.exists(campfireFile)) {
            List<RecipeEntry> examples = new ArrayList<>();
            
            RecipeEntry campfireExample = new RecipeEntry();
            campfireExample.setName("campfire_special_fish");
            campfireExample.setType("campfire");
            campfireExample.setEnabled(false);
            campfireExample.setInput("minecraft:raw_cod");
            campfireExample.setOutputItem("minecraft:cooked_cod");
            campfireExample.setOutputCount(1);
            campfireExample.setOutputNbt("{display:{Name:'{\"text\":\"Campfire Grilled Fish\",\"color\":\"yellow\"}'}}");
            campfireExample.setExperience(0.5f);
            campfireExample.setCookingTime(600);
            campfireExample.setGroup("cooking");
            campfireExample.setCraftCommands(Arrays.asList("particle minecraft:smoke ~ ~1 ~ 0.2 0.2 0.2 0.05 15"));
            campfireExample.setCommandChance(100.0f);
            campfireExample.setComment("special campfire-cooked fish with custom name");
            examples.add(campfireExample);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(campfireFile, gson.toJson(examples).getBytes());
        }
    }
    
    private void createStonecuttingExamples(Path examplesDir) throws IOException {
    Path stonecuttingFile = examplesDir.resolve("stonecutting_examples.json");
    if (!Files.exists(stonecuttingFile)) {
        List<RecipeEntry> examples = new ArrayList<>();
        
        RecipeEntry stonecuttingExample = new RecipeEntry();
        stonecuttingExample.setName("stone_to_diamond");
        stonecuttingExample.setType("stonecutting");
        stonecuttingExample.setEnabled(false);
        stonecuttingExample.setInput("minecraft:stone");
        stonecuttingExample.setOutputItem("minecraft:diamond");
        stonecuttingExample.setOutputCount(1);
        stonecuttingExample.setOutputNbt("");
        stonecuttingExample.setGroup("alchemy");
        stonecuttingExample.setCraftCommands(Arrays.asList("playsound minecraft:block.note_block.pling player @p ~ ~ ~ 1 2"));
        stonecuttingExample.setCommandChance(100.0f);
        stonecuttingExample.setComment("cut stone into diamond (magical!)");
        examples.add(stonecuttingExample);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(stonecuttingFile, gson.toJson(examples).getBytes());
    }
}

private void createSmithingExamples(Path examplesDir) throws IOException {
    Path smithingFile = examplesDir.resolve("smithing_examples.json");
    if (!Files.exists(smithingFile)) {
        List<RecipeEntry> examples = new ArrayList<>();
        
        RecipeEntry smithingExample = new RecipeEntry();
        smithingExample.setName("upgrade_diamond_sword");
        smithingExample.setType("smithing");
        smithingExample.setEnabled(false);
        smithingExample.setTemplate("minecraft:netherite_upgrade_smithing_template");
        smithingExample.setBase("minecraft:diamond_sword");
        smithingExample.setAddition("minecraft:netherite_ingot");
        smithingExample.setOutputItem("minecraft:netherite_sword");
        smithingExample.setOutputCount(1);
        smithingExample.setOutputNbt("{Enchantments:[{id:\"minecraft:sharpness\",lvl:10}],display:{Name:'{\"text\":\"Legendary Netherite Sword\",\"color\":\"dark_red\"}'}}");
        smithingExample.setGroup("weapons");
        smithingExample.setCraftCommands(Arrays.asList("tellraw @p {\"text\":\"You created a legendary weapon!\",\"color\":\"dark_red\"}"));
        smithingExample.setCommandChance(100.0f);
        smithingExample.setComment("smithing with bonus enchantment and custom name");
        examples.add(smithingExample);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(smithingFile, gson.toJson(examples).getBytes());
    }
}

private void loadAllRecipes() {
    configManager.clearConfigurations();
    
    Path recipesDir = Paths.get(CONFIG_DIR, RECIPES_DIR);
    
    if (Files.exists(recipesDir)) {
        // Load from each recipe type directory recursively
        loadRecipesFromTypeDirectory(recipesDir.resolve(CRAFTING_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(FURNACE_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(BLASTING_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(SMOKING_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(CAMPFIRE_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(STONECUTTING_DIR));
        loadRecipesFromTypeDirectory(recipesDir.resolve(SMITHING_DIR));
    }
}

private void loadRecipesFromTypeDirectory(Path typeDir) {
    if (!Files.exists(typeDir) || !Files.isDirectory(typeDir)) {
        return;
    }
    
    try {
        // Walk through all subdirectories recursively
        Files.walk(typeDir)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(this::loadRecipesFromFile);
            
    } catch (IOException e) {
        LOGGER.error("Failed to load recipes from directory: {}", typeDir, e);
    }
}

private void loadRecipesFromFile(Path file) {
    try {
        String json = new String(Files.readAllBytes(file));
        
        // Check if file is empty
        if (json.trim().isEmpty()) {
            LOGGER.warn("Skipping empty recipe file: {}", file);
            return;
        }
        
        Gson gson = new Gson();
        
        Type listType = new TypeToken<List<RecipeEntry>>(){}.getType();
        List<RecipeEntry> recipes = gson.fromJson(json, listType);
        
        if (recipes != null && !recipes.isEmpty()) {
            // Validate each recipe before adding
            List<RecipeEntry> validRecipes = new ArrayList<>();
            for (RecipeEntry recipe : recipes) {
                if (validateRecipe(recipe, file)) {
                    validRecipes.add(recipe);
                } else {
                    LOGGER.warn("Skipping invalid recipe '{}' in file: {}", 
                        recipe != null ? recipe.getName() : "null", file);
                }
            }
            
            if (!validRecipes.isEmpty()) {
                configManager.addRecipes(validRecipes);
                // Log the relative path from the Recipes directory
                Path relativePath = Paths.get(CONFIG_DIR, RECIPES_DIR).relativize(file);
                LOGGER.debug("Loaded {} valid recipes from {}", validRecipes.size(), relativePath);
            }
        } else {
            LOGGER.warn("No recipes found in file: {}", file);
        }
        
    } catch (JsonSyntaxException e) {
        LOGGER.error("Invalid JSON syntax in recipe file: {} - {}", file, e.getMessage());
    } catch (JsonParseException e) {
        LOGGER.error("Failed to parse JSON in recipe file: {} - {}", file, e.getMessage());
    } catch (IOException e) {
        LOGGER.error("Failed to read recipe file: {} - {}", file, e.getMessage());
    } catch (Exception e) {
        LOGGER.error("Unexpected error loading recipes from file: {} - {}", file, e.getMessage(), e);
    }
}

private boolean validateRecipe(RecipeEntry recipe, Path file) {
    if (recipe == null) {
        LOGGER.error("Recipe is null in file: {}", file);
        return false;
    }
    
    if (recipe.getName() == null || recipe.getName().trim().isEmpty()) {
        LOGGER.error("Recipe missing name in file: {}", file);
        return false;
    }
    
    if (recipe.getType() == null || recipe.getType().trim().isEmpty()) {
        LOGGER.error("Recipe '{}' missing type in file: {}", recipe.getName(), file);
        return false;
    }
    
    if (recipe.getOutputItem() == null || recipe.getOutputItem().trim().isEmpty()) {
        LOGGER.error("Recipe '{}' missing outputItem in file: {}", recipe.getName(), file);
        return false;
    }
    
    // Validate based on recipe type
    String type = recipe.getType().toLowerCase();
    switch (type) {
        case "shaped":
        case "shapeless":
            return validateCraftingRecipe(recipe, file);
        case "furnace":
        case "blasting":
        case "smoking":
        case "campfire":
            return validateCookingRecipe(recipe, file);
        case "stonecutting":
            return validateStonecuttingRecipe(recipe, file);
        case "smithing":
            return validateSmithingRecipe(recipe, file);
        default:
            LOGGER.error("Recipe '{}' has unknown type '{}' in file: {}", recipe.getName(), recipe.getType(), file);
            return false;
    }
}

private boolean validateCraftingRecipe(RecipeEntry recipe, Path file) {
    boolean hasPattern = recipe.getPattern() != null && !recipe.getPattern().isEmpty();
    boolean hasKey = recipe.getKey() != null && !recipe.getKey().isEmpty();
    boolean hasIngredients = recipe.getIngredients() != null && !recipe.getIngredients().isEmpty();
    
    if (recipe.getType().equals("shaped") && hasPattern && hasKey) {
        // Shaped recipe - validate pattern and key
        return true;
    } else if (recipe.getType().equals("shapeless") && hasIngredients) {
        // Shapeless recipe - validate ingredients
        return true;
    } else {
        LOGGER.error("Crafting recipe '{}' must have either (pattern + key) for shaped or ingredients for shapeless in file: {}", 
            recipe.getName(), file);
        return false;
    }
}

private boolean validateCookingRecipe(RecipeEntry recipe, Path file) {
    if (recipe.getInput() == null || recipe.getInput().trim().isEmpty()) {
        LOGGER.error("Cooking recipe '{}' missing input in file: {}", recipe.getName(), file);
        return false;
    }
    return true;
}

private boolean validateStonecuttingRecipe(RecipeEntry recipe, Path file) {
    if (recipe.getInput() == null || recipe.getInput().trim().isEmpty()) {
        LOGGER.error("Stonecutting recipe '{}' missing input in file: {}", recipe.getName(), file);
        return false;
    }
    return true;
}

private boolean validateSmithingRecipe(RecipeEntry recipe, Path file) {
    if (recipe.getTemplate() == null || recipe.getTemplate().trim().isEmpty()) {
        LOGGER.error("Smithing recipe '{}' missing template in file: {}", recipe.getName(), file);
        return false;
    }
    if (recipe.getBase() == null || recipe.getBase().trim().isEmpty()) {
        LOGGER.error("Smithing recipe '{}' missing base in file: {}", recipe.getName(), file);
        return false;
    }
    if (recipe.getAddition() == null || recipe.getAddition().trim().isEmpty()) {
        LOGGER.error("Smithing recipe '{}' missing addition in file: {}", recipe.getName(), file);
        return false;
    }
    return true;
}
}