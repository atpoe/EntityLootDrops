package net.poe.entitylootdrops.util;

import net.minecraftforge.fml.ModList;

public class ModCompatibility {
    public static final boolean EMI_LOOT_LOADED = ModList.get().isLoaded("emi_loot");
    
    public static boolean isEmiLootLoaded() {
        return EMI_LOOT_LOADED;
    }
}