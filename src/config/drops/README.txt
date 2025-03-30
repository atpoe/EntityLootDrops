Entity Loot Drops Configuration
===============================

This directory contains configuration files for custom entity drops.
You can name the JSON files anything you want, and each file can contain
drops for one entity.

Configuration Format:
{
  "entityId": "namespace:mob_id",
  "drops": [
    {
      "itemId": "namespace:item_id",
      "dropRate": 0.5,
      "minCount": 1,
      "maxCount": 3
    }
  ]
}

Properties:
- entityId: The full entity ID (e.g., "minecraft:zombie" or "modid:custom_mob")
- drops: Array of drop entries with the following properties:
  - itemId: The item ID to drop (e.g., "minecraft:diamond" or "modid:custom_item")
  - dropRate: Chance to drop (0.0 to 1.0, where 1.0 is 100%)
  - minCount: Minimum number of items to drop
  - maxCount: Maximum number of items to drop

These drops are added to the entity's default drops, not replacing them.