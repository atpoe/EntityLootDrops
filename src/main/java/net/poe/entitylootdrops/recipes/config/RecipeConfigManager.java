package net.poe.entitylootdrops.recipes.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.poe.entitylootdrops.recipes.model.RecipeEntry;

/**
 * Manages recipe configurations.
 */
public class RecipeConfigManager {
    private List<RecipeEntry> allRecipes = new ArrayList<>();
    
    public void clearConfigurations() {
        allRecipes.clear();
    }
    
    public void addRecipes(List<RecipeEntry> recipes) {
        if (recipes != null) {
            allRecipes.addAll(recipes);
        }
    }
    
    public List<RecipeEntry> getAllRecipes() {
        return new ArrayList<>(allRecipes);
    }
    
    public List<RecipeEntry> getEnabledRecipes() {
        return allRecipes.stream()
            .filter(RecipeEntry::isEnabled)
            .collect(Collectors.toList());
    }
    
    public List<RecipeEntry> getRecipesByType(String type) {
        return allRecipes.stream()
            .filter(recipe -> type.equals(recipe.getType()))
            .filter(RecipeEntry::isEnabled)
            .collect(Collectors.toList());
    }
    
    public List<RecipeEntry> getReplacementRecipes() {
        return allRecipes.stream()
            .filter(RecipeEntry::shouldReplace)
            .filter(RecipeEntry::isEnabled)
            .collect(Collectors.toList());
    }
    
    public int getRecipeCount() {
        return allRecipes.size();
    }
    
    public int getEnabledRecipeCount() {
        return (int) allRecipes.stream().filter(RecipeEntry::isEnabled).count();
    }
    
    public int getDisabledRecipeCount() {
        return (int) allRecipes.stream().filter(recipe -> !recipe.isEnabled()).count();
    }
    
    public List<RecipeEntry> getCraftingRecipes() {
        return getRecipesByType("crafting");
    }
    
    public List<RecipeEntry> getFurnaceRecipes() {
        return getRecipesByType("furnace");
    }
    
    public List<RecipeEntry> getBlastingRecipes() {
        return getRecipesByType("blasting");
    }
    
    public List<RecipeEntry> getSmokingRecipes() {
        return getRecipesByType("smoking");
    }
    
    public List<RecipeEntry> getCampfireRecipes() {
        return getRecipesByType("campfire");
    }
    
    public List<RecipeEntry> getStonecuttingRecipes() {
        return getRecipesByType("stonecutting");
    }
    
    public List<RecipeEntry> getSmithingRecipes() {
        return getRecipesByType("smithing");
    }
    
    public boolean hasRecipes() {
        return !allRecipes.isEmpty();
    }
    
    public boolean hasEnabledRecipes() {
        return allRecipes.stream().anyMatch(RecipeEntry::isEnabled);
    }
    
    public RecipeEntry getRecipeByName(String name) {
        return allRecipes.stream()
            .filter(recipe -> name.equals(recipe.getName()))
            .findFirst()
            .orElse(null);
    }
}
