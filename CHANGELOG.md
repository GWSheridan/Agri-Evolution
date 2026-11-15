# Changelog

## 1.2.17

- The Vertical Greenhouse shows when a pipe port is disabled.
- It reports when its water supply crosses into an unloaded chunk.
- It identifies incomplete reservoirs connected to its water intake.
- Loaded valves refresh their redstone indicator from the current signal.
- Breaking a reservoir shell across a chunk border leaves the remaining pieces and tank in place until every part can be dismantled together.

## 1.2.16

- Pipes and sprinklers check loaded neighbors when working out their connections on placement.
- Manual and redstone valve changes leave neighboring unloaded chunks alone.
- A valve with a sprinkler and a side pipe aligns with the sprinkler.
- Reservoir inspection distinguishes a missing shell from one that extends into an unloaded chunk.
- The Spanner points to unavailable reservoir inlets and explains the problem in valve reports.

## 1.2.15

- Filling or draining a Water Reservoir keeps comparator updates in loaded chunks.
- Dismantling a Water Reservoir clears the comparator reading, including through a solid block.
- Irrigation Basins can fill, hydrate loaded farmland and supply sprinklers at chunk edges without loading the next chunk.

## 1.2.14

- Valve inspection counts water and capacity from both basins and reservoirs.
- The valve report counts sprinklers that are currently spraying. The Journal explains when a closed valve can be bypassed.
- Builds keep the retired texture notice out of the JAR, including when it is left in the generated resources folder.

## 1.2.13

- Collecting water from a basin uses one empty bucket and keeps the rest of the stack.
- A sprinkler can refill from its supply when it has less than the 25 mB needed to spray. It keeps any water left in the tank.
- Changing a pipe port keeps both fittings in sync without loading extra chunks. The Spanner tells you when the selected face needs its neighboring chunk loaded.

## 1.2.12

- Use an empty bucket on a sprinkler to take 1,000 mB from its tank.

## 1.2.11

- A redstone comparator can read the sprinkler tank. Empty gives 0, and full gives 15.

## 1.2.10

- The Spanner's sprinkler panel shows whether the head is watering or why it stopped, including at chunk edges.

## 1.2.9

- Reopen a valve during a sprinkler's two-second spray to resume it without using extra water.

## 1.2.8

- Inspecting a valve leaves neighboring chunks unloaded and tells you if the line continues into one.
- The Spanner shows where a pipe connection leads into an unloaded chunk.
- The Spanner points to the valve shutting off a sprinkler, even if another pipe leads to a full water source.
- Filling a sprinkler with a water bucket starts the spray right away. It now waits two seconds before using water for the next spray.
- Hand-filled sprinklers can water loaded farmland at the edge of a chunk. Sprinklers pause if a connected fitting is in an unloaded chunk next door.
- Saved pipes, valves and sprinklers keep their connections when you rotate or mirror a structure. Disabled pipe ports turn with the rest of the layout.
- Formed Water Reservoirs stay assembled when a saved structure is rotated or mirrored. Their outlets move with the shell, and the tank keeps its stored water and Mk II upgrade.

## 1.2.1 — 15 November 2026

- Fixed the gap under Agri Evolution crops on farmland. Seedlings and mature plants now sit on the soil.

## 1.2.0 — 14 November 2026

- Name a seed at an anvil and the seeds you harvest from that plant keep the name. Right-click the crop with Grandmas Journal to record the variety, its best traits and any known parents. You can find named seeds in the Seed Vault search.
- Check the Garden Order Board’s **Seeds** tab for commissions. There’s one for high-yield Tomato, one for fast and hardy Corn, and one for Lettuce with a rare trait. Datapacks can add repeatable orders under **Custom**.
- Grow Basil next to Tomato to halve the chance of new weeds. Harvest ripe Beans next to Corn to give the Corn 8 nutrition. The Soil Sampler shows the bonus.
- Pick a crop in the breeding planner to track which parent seeds you already have in your inventory and Seed Bags.
- Send Vertical Greenhouse offspring straight into a nearby Seed Vault. You can filter what goes in by crop or by its Growth, Yield and Resilience.
- Rename a Spanner at an anvil and use it on a valve to name its irrigation zone. Inspect the zone to see its water supply and connected sprinklers.
- With JEI installed, you can look up field and lab crosses, preserving recipes and Vertical Greenhouse harvests.

## 1.0.0 — 14 November 2026

First public release of Agri Evolution for Minecraft 1.21.1, NeoForge 21.1.265, and Java 21.

- Start with Wild Wheat and breed food, herbs, and resource crops. Grandmas Journal has illustrated crosses and growing tips.
- Check growing conditions with the Soil Sampler, clear weeds with a rake, and compare seed genetics in the Seed Vault.
- Water fields with basins, sprinklers, and copper reservoirs. The late-game Vertical Greenhouse grows and breeds seeds across six trays.
- Cook with your harvests, pack a Lunch Box, and trade produce for emeralds at the Garden Order Board.

## 0.94.1 — 14 November 2026

- Reworked the Seed Vault front into a wooden cabinet with three recessed drawers, copper pulls, and cream labels.
- Includes the expanded Garden Order Board, Compost Bin, Seed Vault, bee-assisted field breeding, and farming milestones from v0.94.0.

Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21
