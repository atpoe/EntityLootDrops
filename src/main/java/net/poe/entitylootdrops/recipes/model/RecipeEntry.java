package net.poe.entitylootdrops.recipes.model;

import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Universal recipe configuration that supports all recipe types.
 * Updated to support both string and datapack object formats for ingredients.
 * Removed removeOriginal option - only replaceRecipe is used now.
 */
public class RecipeEntry {
    // Basic recipe info
    private String name;
    private String type; // "shaped", "shapeless", "furnace", "blasting", "smoking", "campfire", "stonecutting", "smithing"
    private boolean enabled = true;
    private String replaceRecipe; // Recipe ID to replace (optional)
    private String _comment;
    private String group = "";
    
    // Output configuration
    private String outputItem;
    private int outputCount = 1;
    private String outputNbt = "";
    
    // Input configuration (varies by recipe type)
    private String input; // For furnace-type recipes
    private List<String> pattern; // For shaped crafting
    private Map<String, Object> key; // For shaped crafting (char -> item mapping)
    private List<Object> ingredients; // For shapeless crafting
    
    // Smithing specific
    private String base;
    private String addition;
    private String template;
    
    // Furnace-type specific
    private float experience = 0.1f;
    private int cookingTime = 200;
    
    // Command execution
    private List<String> craftCommands;
    private float commandChance = 100.0f;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getReplaceRecipe() { return replaceRecipe; }
    public void setReplaceRecipe(String replaceRecipe) { this.replaceRecipe = replaceRecipe; }
    
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    
    public String getOutputItem() { return outputItem; }
    public void setOutputItem(String outputItem) { this.outputItem = outputItem; }
    
    public int getOutputCount() { return outputCount; }
    public void setOutputCount(int outputCount) { this.outputCount = outputCount; }
    
    public String getOutputNbt() { return outputNbt; }
    public void setOutputNbt(String outputNbt) { this.outputNbt = outputNbt; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    
    public List<String> getPattern() { return pattern; }
    public void setPattern(List<String> pattern) { this.pattern = pattern; }
    
    public Map<String, Object> getKey() { return key; }
    public void setKey(Map<String, Object> key) { this.key = key; }
    
    public List<Object> getIngredients() { return ingredients; }
    public void setIngredients(List<Object> ingredients) { this.ingredients = ingredients; }
    
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    
    public String getAddition() { return addition; }
    public void setAddition(String addition) { this.addition = addition; }
    
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    
    public float getExperience() { return experience; }
    public void setExperience(float experience) { this.experience = experience; }
    
    public int getCookingTime() { return cookingTime; }
    public void setCookingTime(int cookingTime) { this.cookingTime = cookingTime; }
    
    public List<String> getCraftCommands() { return craftCommands; }
    public void setCraftCommands(List<String> craftCommands) { this.craftCommands = craftCommands; }
    
    public float getCommandChance() { return commandChance; }
    public void setCommandChance(float commandChance) { this.commandChance = commandChance; }
    
    public void setComment(String comment) { this._comment = comment; }
    public String getComment() { return _comment; }
    
    // Compatibility methods for the registration system
    public String getOutput() { return outputItem; }
    public void setOutput(String output) { this.outputItem = output; }
    
    public String getCommand() { 
        if (craftCommands != null && !craftCommands.isEmpty()) {
            return String.join(";", craftCommands);
        }
        return null;
    }
    public void setCommand(String command) { 
        if (command != null && !command.trim().isEmpty()) {
            this.craftCommands = List.of(command);
        }
    }
    
    // Helper methods
    public boolean hasCommand() { 
        return craftCommands != null && !craftCommands.isEmpty() && 
               craftCommands.stream().anyMatch(cmd -> cmd != null && !cmd.trim().isEmpty());
    }
    
    public boolean hasNbt() { 
        return outputNbt != null && !outputNbt.trim().isEmpty(); 
    }
    
    public boolean shouldReplace() { 
        return replaceRecipe != null && !replaceRecipe.trim().isEmpty(); 
    }
    
    public ItemStack createOutputStack() {
        try {
            ResourceLocation itemLocation = new ResourceLocation(outputItem);
            Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
            
            if (item == null) {
                return ItemStack.EMPTY;
            }
            
            ItemStack stack = new ItemStack(item, outputCount);
            
            if (hasNbt()) {
                try {
                    CompoundTag nbtTag = TagParser.parseTag(outputNbt);
                    stack.setTag(nbtTag);
                } catch (Exception e) {
                    // Log error but continue
                }
            }
            
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Creates an ingredient from either string format or datapack object format.
     * Supports:
     * - "minecraft:item_name" (simple string)
     * - "#forge:tag_name" (tag string)
     * - {"item": "minecraft:item_name"} (datapack item object)
     * - {"tag": "forge:tag_name"} (datapack tag object)
     */
    public Ingredient createIngredient(Object ingredientData) {
        if (ingredientData == null) {
            return Ingredient.EMPTY;
        }
        
        try {
            if (ingredientData instanceof String) {
                // Handle simple string format
                return createIngredientFromString((String) ingredientData);
            } else if (ingredientData instanceof Map) {
                // Handle datapack object format
                return createIngredientFromDatapackObject((Map<?, ?>) ingredientData);
            } else {
                return Ingredient.EMPTY;
            }
        } catch (Exception e) {
            return Ingredient.EMPTY;
        }
    }
    
    /**
     * Creates ingredient from simple string format.
     */
    private Ingredient createIngredientFromString(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return Ingredient.EMPTY;
        }
        
        try {
            if (itemId.startsWith("#")) {
                // Tag ingredient
                ResourceLocation tagLocation = new ResourceLocation(itemId.substring(1));
                return Ingredient.of(ForgeRegistries.ITEMS.tags().createTagKey(tagLocation));
            } else {
                // Item ingredient
                ResourceLocation itemLocation = new ResourceLocation(itemId);
                Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
                return item != null ? Ingredient.of(item) : Ingredient.EMPTY;
            }
        } catch (Exception e) {
            return Ingredient.EMPTY;
        }
    }
    
    /**
     * Creates ingredient from datapack object format.
     * Supports: {"item": "minecraft:item_name"} and {"tag": "forge:tag_name"}
     */
    private Ingredient createIngredientFromDatapackObject(Map<?, ?> datapackObject) {
        try {
            if (datapackObject.containsKey("item")) {
                // Handle {"item": "minecraft:item_name"}
                Object itemValue = datapackObject.get("item");
                if (itemValue instanceof String) {
                    String itemId = (String) itemValue;
                    ResourceLocation itemLocation = new ResourceLocation(itemId);
                    Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
                    return item != null ? Ingredient.of(item) : Ingredient.EMPTY;
                }
            } else if (datapackObject.containsKey("tag")) {
                // Handle {"tag": "forge:tag_name"}
                Object tagValue = datapackObject.get("tag");
                if (tagValue instanceof String) {
                    String tagId = (String) tagValue;
                    ResourceLocation tagLocation = new ResourceLocation(tagId);
                    return Ingredient.of(ForgeRegistries.ITEMS.tags().createTagKey(tagLocation));
                }
            }
            
            return Ingredient.EMPTY;
        } catch (Exception e) {
            return Ingredient.EMPTY;
        }
    }
    
    /**
     * Convenience method for backward compatibility with string-based ingredient creation.
     */
    public Ingredient createIngredient(String itemId) {
        return createIngredient((Object) itemId);
    }
}
