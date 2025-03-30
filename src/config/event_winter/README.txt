Winter Event Drops Configuration
===============================

This directory contains configuration files for winter event-specific drops.
These drops will only be active when the winter event is enabled.

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

Winter drops are separate from regular drops and are not affected by the double drops event.
Use the command '/entitylootdrops winter enable' to activate winter drops.