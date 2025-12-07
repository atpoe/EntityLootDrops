package net.poe.entitylootdrops.mixin.emi;

import fzzyhmstrs.emi_loot.server.SimpleLootPoolBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SimpleLootPoolBuilder.class})
public interface SimpleLootPoolBuilderAccessor {
    @Accessor("map")
    Object2IntMap<ItemStack> getMap();
}
