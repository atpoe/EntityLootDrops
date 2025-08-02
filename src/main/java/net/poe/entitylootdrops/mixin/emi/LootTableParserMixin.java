package net.poe.entitylootdrops.mixin.emi;

import fzzyhmstrs.emi_loot.mixins.BinomialLootNumberProviderAccessor;
import fzzyhmstrs.emi_loot.mixins.LootPoolAccessor;
import net.poe.entitylootdrops.mixin.LootPoolSingletonContainerAccessor;
import fzzyhmstrs.emi_loot.mixins.UniformLootNumberProviderAccessor;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.server.MobLootTableSender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.LootTablePools2;
import net.poe.entitylootdrops.SetItemCountFunctionNumberProviderAccessor;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static net.minecraft.world.level.storage.loot.LootPool.lootPool;
import static net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem;

@Mixin(value = LootTableParser.class, remap = false)
public abstract class LootTableParserMixin {
    @Shadow
    public static String currentTable;

    @Final
    @Shadow
    private static Map<ResourceLocation, MobLootTableSender> mobSenders;

    @Shadow
    private static MobLootTableSender parseMobLootTable(LootTable lootTable, ResourceLocation id, ResourceLocation mobId) {
        return null;
    }

    @Unique
    private static Map<String, Boolean> entitylootdrops$entitiesDone = new HashMap<>();

    @Inject(
            method = "parseLootTables",
            at = @At("TAIL")
    )
    private static void parseLootTables(LootDataManager manager, Map<LootDataId<?>, ?> tables, CallbackInfo ci) {
        ForgeRegistries.ENTITY_TYPES.forEach(type -> {
            if (entitylootdrops$entitiesDone.containsKey(type.getDescriptionId())) {
                return;
            }
            var mobTableId = type.getDefaultLootTable();
            LootTable mobTable = manager.getLootTable(mobTableId);
            if (mobTable == LootTable.EMPTY) {
                ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(type);
                int size = addLootPool(mobTable, mobId, type);
                if (size > 0) {
                    currentTable = mobTableId.toString();
                    mobSenders.put(mobTableId, parseMobLootTable(mobTable, mobTableId, mobId));
                }
            }
        });
    }

    @Unique
    private static int addLootPool(LootTable mobTable, ResourceLocation mobTableId, EntityType<?> entityType) {
        List<LootPool> lootPools = new ArrayList<>();
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (entitylootdrops$shouldApplyDrop(drop, mobTableId.toString(), entityType) && drop.hasItem()) {
                addAdvancedLootPool(lootPools, drop);
            }
        }

        lootPools.forEach(mobTable::addPool);
        return lootPools.size();
    }

    @Inject(
            method = "parseMobLootTable",
            at = @At("HEAD")
    )
    private static void $parseMobLootTable(LootTable lootTable, ResourceLocation lootTableId, ResourceLocation mobId, CallbackInfoReturnable<MobLootTableSender> cir) {
        entitylootdrops$entitiesDone.put(mobId.toString(), true);
        List<LootPool> lootPools = new ArrayList<>();
        lootPools.addAll(Arrays.asList(((LootTablePools2) lootTable).getPools()));

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(mobId);
        if (entityType == null) return;

        // Phase 1: Handle vanilla drop modifications and mod filtering
        boolean shouldCancelVanillaDrops = false;
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (entitylootdrops$shouldApplyDrop(drop, mobId.toString(), entityType)) {
                // Check mod filtering - since EntityDropEntry extends CustomDropEntry, no instanceof needed
                if (!drop.getAllowModIDs().isEmpty()) {
                    if (!entitylootdrops$isModAllowed(mobId, drop.getAllowModIDs())) {
                        continue;
                    }
                }

                if (!drop.isAllowDefaultDrops()) {
                    shouldCancelVanillaDrops = true;
                    break;
                }
            }
        }

        if (shouldCancelVanillaDrops) {
            lootPools.clear();
        }

        // Phase 2: Process extra vanilla drops
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (entitylootdrops$shouldApplyDrop(drop, mobId.toString(), entityType) && drop.getExtraDropChance() > 0) {
                addExtraDrop(lootPools, drop);
            }
        }

        // Phase 3: Process custom drops with advanced features
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (entitylootdrops$shouldApplyDrop(drop, mobId.toString(), entityType) && drop.hasItem()) {
                addAdvancedLootPool(lootPools, drop);
            }
        }

        ((LootTablePools2) lootTable).setPools(lootPools.toArray(new LootPool[0]));
    }

    @Unique
    private static boolean entitylootdrops$isModAllowed(ResourceLocation mobId, List<String> allowedMods) {
        String namespace = mobId.getNamespace();
        return allowedMods.contains(namespace);
    }

    @Unique
    private static boolean entitylootdrops$shouldApplyDrop(EntityDropEntry drop, String entityId, EntityType<?> entityType) {
        String dropEntityId = drop.getEntityId();

        // Direct entity match
        if (dropEntityId.equals(entityId)) {
            return true;
        }

        // Global category matches
        if (entityType.getCategory() == MobCategory.MONSTER) {
            if (dropEntityId.equals("Global_Hostile") || dropEntityId.equals("Global_Monster")) {
                return true;
            }
        }

        if (entityType.getCategory() == MobCategory.CREATURE || entityType.getCategory() == MobCategory.AMBIENT) {
            if (dropEntityId.equals("Global_Passive") || dropEntityId.equals("Global_Animal")) {
                return true;
            }
        }

        // Universal global match
        if (dropEntityId.equals("Global_All") || dropEntityId.equals("Global")) {
            return true;
        }

        return false;
    }

    @Unique
    private static void addExtraDrop(List<LootPool> lootPools, EntityDropEntry drop) {
        var addedAmount = Math.max(drop.getExtraAmountMin(), drop.getExtraAmountMax());
        for (LootPool pool : lootPools) {
            var functions = ((LootPoolAccessor) pool).getFunctions();
            for (LootItemFunction function : functions) {
                if (function instanceof SetItemCountFunction setItemCountFunction) {
                    SetItemCountFunctionNumberProviderAccessor accessor = (SetItemCountFunctionNumberProviderAccessor) setItemCountFunction;
                    var numberProvider = accessor.getNumberProvider();
                    accessor.setNumberProvider(addToNumberProvider(numberProvider, addedAmount));
                }
            }
            var entries = ((LootPoolAccessor) pool).getEntries();
            for (var entry : entries) {
                if (entry instanceof LootItem lootItem) {
                    var functions2 = ((LootPoolSingletonContainerAccessor) lootItem).getFunctions();
                    for (LootItemFunction function : functions2) {
                        if (function instanceof SetItemCountFunction setItemCountFunction) {
                            SetItemCountFunctionNumberProviderAccessor accessor = (SetItemCountFunctionNumberProviderAccessor) setItemCountFunction;
                            var numberProvider = accessor.getNumberProvider();
                            accessor.setNumberProvider(addToNumberProvider(numberProvider, addedAmount));
                        }
                    }
                }
            }
        }
    }

    @Unique
    private static NumberProvider addToNumberProvider(NumberProvider numberProvider, int addedAmount) {
        if (numberProvider instanceof UniformGenerator) {
            var min = getGeneratorMin(numberProvider);
            var max = getGeneratorMax(numberProvider) + addedAmount;
            return UniformGenerator.between(min, max);
        } else if (numberProvider instanceof BinomialDistributionGenerator binomialDistributionGenerator) {
            var max = getGeneratorMax(binomialDistributionGenerator) + addedAmount;
            return UniformGenerator.between(0, max);
        } else if (numberProvider instanceof ConstantValue constantValue) {
            return ConstantValue.exactly(constantValue.getFloat(null) + addedAmount);
        }
        return numberProvider;
    }

    @Unique
    private static float getGeneratorMin(NumberProvider numberProvider) {
        if (numberProvider instanceof UniformGenerator) {
            return getGeneratorMin(((UniformLootNumberProviderAccessor) numberProvider).getMin());
        } else if (numberProvider instanceof BinomialDistributionGenerator) {
            return 0;
        } else if (numberProvider instanceof ConstantValue constantValue) {
            return constantValue.getFloat(null);
        }
        return 0;
    }

    @Unique
    private static float getGeneratorMax(NumberProvider numberProvider) {
        if (numberProvider instanceof UniformGenerator) {
            return getGeneratorMax(((UniformLootNumberProviderAccessor) numberProvider).getMax());
        } else if (numberProvider instanceof BinomialDistributionGenerator) {
            return getGeneratorMax(((BinomialLootNumberProviderAccessor) numberProvider).getN());
        } else if (numberProvider instanceof ConstantValue constantValue) {
            return constantValue.getFloat(null);
        }
        return 0;
    }

    @Unique
    private static void addAdvancedLootPool(List<LootPool> lootPools, EntityDropEntry drop) {
        var lootItem = lootTableItem(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(drop.getItemId())));
        var builder = lootPool().add(lootItem);

        // Basic item count
        builder.apply(SetItemCountFunction.setCount(UniformGenerator.between(drop.getMinAmount(), drop.getMaxAmount())));

        // Add NBT data if present - no instanceof needed since EntityDropEntry extends CustomDropEntry
        if (drop.getNbtData() != null && !drop.getNbtData().isEmpty()) {
            try {
                CompoundTag nbtTag = TagParser.parseTag(drop.getNbtData());
                builder.apply(SetNbtFunction.setTag(nbtTag));
            } catch (Exception e) {
                // Log error but continue - invalid NBT shouldn't break the entire system
                System.err.println("Invalid NBT data in drop entry: " + drop.getNbtData());
            }
        }

        // Player kill requirement
        if (drop.isRequirePlayerKill()) {
            builder.when(LootItemKilledByPlayerCondition.killedByPlayer());
        }

        // Drop chance
        if (drop.getDropChance() < 100) {
            builder.when(LootItemRandomChanceCondition.randomChance(drop.getDropChance() / 100.0f));
        }

        // Advanced conditions - no instanceof needed
        addAdvancedConditions(builder, drop);

        lootPools.add(builder.build());
    }

    @Unique
    private static void addAdvancedConditions(LootPool.Builder builder, EntityDropEntry drop) {
        // Weather conditions - Use correct class names
        if (drop.getRequiredWeather() != null && !drop.getRequiredWeather().isEmpty()) {
            switch (drop.getRequiredWeather().toLowerCase()) {
                case "clear" -> builder.when(WeatherCheck.weather().setRaining(false).setThundering(false));
                case "rain" -> builder.when(WeatherCheck.weather().setRaining(true).setThundering(false));
                case "thunder" -> builder.when(WeatherCheck.weather().setThundering(true));
            }
        }

        // Better way - Use LocationCheck with LocationPredicate for time conditions
        /*
        if (drop.getRequiredTime() != null && !drop.getRequiredTime().isEmpty()) {
            switch (drop.getRequiredTime().toLowerCase()) {
                case "day" -> builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setTime(MinMaxBounds.Doubles.between(0.0, 0.5)).build()
                ));
                case "night" -> builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setTime(MinMaxBounds.Doubles.between(0.5, 1.0)).build()
                ));
                case "dawn" -> builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setTime(MinMaxBounds.Doubles.between(0.75, 0.25)).build()
                ));
                case "dusk" -> builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setTime(MinMaxBounds.Doubles.between(0.25, 0.75)).build()
                ));
            }
        }
    */

        // Better dimension check
        /*
        if (drop.getRequiredDimension() != null && !drop.getRequiredDimension().isEmpty()) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(drop.getRequiredDimension());
            if (dimensionId != null) {
                builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setDimension(dimensionId).build()
                ));
            }
        }


         */
        // Better biome check
        /*
        if (drop.getRequiredBiome() != null && !drop.getRequiredBiome().isEmpty()) {
            ResourceLocation biomeId = ResourceLocation.tryParse(drop.getRequiredBiome());
            if (biomeId != null) {
                builder.when(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location().setBiome(biomeId).build()
                ));
            }
        }

         */
        // Equipment conditions (simplified - would need more complex implementation for full support)
        if (drop.getRequiredEquipment() != null && !drop.getRequiredEquipment().isEmpty()) {
            // This would require a custom condition implementation
            // For now, we'll skip this as it's complex to implement properly
        }

        // Advancement conditions (simplified - would need custom implementation)
        if (drop.getRequiredAdvancement() != null && !drop.getRequiredAdvancement().isEmpty()) {
            // This would require a custom condition implementation
            // For now, we'll skip this as it's complex to implement properly
        }

        // Effect conditions (simplified - would need custom implementation)
        if (drop.getRequiredEffect() != null && !drop.getRequiredEffect().isEmpty()) {
            // This would require a custom condition implementation
            // For now, we'll skip this as it's complex to implement properly
        }
    }
}