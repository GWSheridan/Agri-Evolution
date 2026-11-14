# Custom Garden Order Board deliveries

Put each order in `data/<namespace>/garden_orders/<name>.json` inside an enabled datapack. Run `/reload` and reopen the Garden Order Board. If an order loaded, you’ll see a **Custom** tab. Players can deliver these orders straight away and repeat them as often as they have the goods.

For example, save this as `data/my_farm/garden_orders/beet_delivery.json` to trade three beetroot for two emeralds.

```json
{
  "title": "Beet Delivery",
  "ingredients": [
    { "item": "minecraft:beetroot", "count": 3 }
  ],
  "reward": { "item": "minecraft:emerald", "count": 2 }
}
```

- Keep the title to one line and at most 48 characters. An order takes one to four different items and gives one reward.
- Use full item IDs like `minecraft:beetroot` or `agri_evolution:tomato`. You can also use items from other mods installed on the server.
- Each count must be a whole number of at least one and fit in a single stack. Players can deliver from several stacks in their main inventory.
- The board shows up to 24 orders in namespace and filename order. If one is missing, check the server log for errors in its JSON file.
