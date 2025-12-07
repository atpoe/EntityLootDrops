package net.poe.entitylootdrops.mixin.emi;

import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.server.ComplexLootPoolBuilder;
import fzzyhmstrs.emi_loot.server.SimpleLootPoolBuilder;
import fzzyhmstrs.emi_loot.util.TextKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;
import java.util.List;

@Mixin({ComplexLootPoolBuilder.class})
public interface ComplexLootPoolBuilderAccessor {
    @Accessor("map")
    HashMap<List<TextKey>, SimpleLootPoolBuilder> getMap();

    @Accessor("conditions")
    List<LootTableParser.LootConditionResult> getConditions();

    @Accessor("functions")
    List<LootTableParser.LootFunctionResult> getFunctions();
}
