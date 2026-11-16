# Changelog

## 1.2.24

- Forming a Water Reservoir at a chunk edge leaves the chunks beyond the shell unloaded.
- Dismantling restores copper and clears connected pipe fittings using loaded chunks.
- The reservoir core handles routine structure checks and Mk II generation. Shell pieces can still clean themselves up if the core is missing.
- Small transfers leave the shell blocks alone until the gauge crosses a band. Comparators still track the water level.
- Small transfers keep pump connections valid. Dismantling still disconnects the old tank.

## 1.2.23

- A roof or closed valve cuts off sprinkler hydration as soon as it blocks the spray.
- Removing an obstruction during a paid spray restores hydration and growth on plots that were already watered.
- Sprinklers use one hydration ticket across their covered chunks. Unloading the head clears it from the whole area.
- The Spanner's sprinkler preview stops at the bottom of the world.
- The coverage outline sits just above the soil and hides with the HUD.

## 1.2.22

- Spanner traces and sprinkler inspection now agree on when redstone, an adjacent valve, or an unloaded fitting stops a spray.
- Starting a trace in an unloaded chunk reports the missing chunk and leaves it unloaded.
- Valve zone reports name the disabled pipe face or tell you whether a remote valve is closed by hand or held shut by redstone.
- Players without building permission can read valve reports where interaction is available. Renaming and fitting changes still require permission.
- Aim the Spanner at any section of a Vertical Greenhouse to check its water level and pipe supply.

## 1.2.21

- Valves align with usable fittings. A disabled pipe face or reservoir panel without a port no longer turns the valve.
- Loading a valve refreshes its alignment and connectors alongside its redstone state.
- Changing a pipe port at a chunk edge updates the loaded valve or sprinkler straight away.
- The Spanner ignores disabled ports that can be bypassed through a pipe loop.
- Valve inspection distinguishes a manual shutoff from redstone power, and the item tooltip explains that side branches work.

## 1.2.20

- Reopening a valve mid-spray resumes watering only on plots the sprinkler already reached and can still reach.
- Loading a sprinkler chunk starts with a fresh spray cycle. The sprinkler keeps its stored water.
- Sprinkler inspection shows how many reachable plots are being watered and which are waiting for the next spray.
- An empty sprinkler reports a disabled pipe port, an unloaded supply path, or an incomplete reservoir when it cannot refill.
- Idle sprinkler heads skip flow checks on neighbor updates. Changing sprinkler modes leaves comparators alone because the tank level stays the same.

## 1.2.19

- The sprinkler growth bonus applies to plots watered by the current spray and stops if their path becomes blocked.
- The Spanner tells you when reachable farmland is waiting for its first spray.
- A quick remote valve switch can resume the paid growth bonus without using more water.
- Checking an individual crop for sprinkler coverage scans only its own column.
- A sprinkler reuses the supply paths it just checked when it refills for a spray.

## 1.2.18

- The Spanner respects block permissions when changing pipes, sprinklers and valves.
- Forming a Water Reservoir checks permission for the entire shell and centre before replacing any blocks.
- Reservoirs explain why a bucket transfer failed, including when the shell is in an unloaded chunk.
- Using a Conduit distinguishes an already upgraded tank from an unavailable shell.
- Inspecting a Mk II tank shows its generation rate and bucket controls.

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
