# Changelog

## 1.3.8

- Botanical Greenhouse upgrades, heater refills and bucket transfers now check build and interaction permissions. Spectators cannot change them through a direct right-click.
- Collecting water consumes one empty bucket from the held stack and keeps the rest. If the inventory is full, the filled bucket drops beside the player.
- An empty main hand lets an offhand bucket reach the controller instead of opening its screen first.
- Door installation checks item-use permission at both halves of the entrance before removing glass.
- Retrofitting a door returns both glass blocks even when a Creative inventory is full.
- Removed or replaced controllers reject direct water, fuel and upgrade calls through old references.

## 1.3.7

- The Batch Cooking Station can choose fresh alternatives while keeping required container ingredients, such as milk buckets. This also works when several ingredients share alternatives in a datapack recipe.
- Cooking still waits without consuming anything when the required containers cannot fit in the return slots.
- Garden Order Board and Preserving Station rewards drop beside players with full Creative inventories instead of disappearing.
- One-time commissions are marked claimed before their reward is delivered. An item-delivery callback cannot claim the same commission again.
- Regular market progression is saved before the emerald reward is handed over, so callbacks see the completed delivery.
- Removed cooking stations and greenhouse storage reject late pickup and shift-clicks. Vertical Greenhouses also invalidate their old screens.
- Removed or incomplete Vertical Greenhouses reject sided item transfers through stale automation references.

## 1.3.6

- Moved the optional asset-inventory script into local backups and removed its README command. It is not needed to build or run the mod.
- Nutrient Compost checks build and interaction permissions before growing a crop or using an item. Calls without a player leave the crop alone instead of crashing after growth.
- Compost growth rejects stale crop states and reports success only when the growth change succeeds. It cannot replace a newer crop or restore one that was removed.
- Genetic Clippers check permissions before taking a crop's one sample. A denied use leaves the sample available for an authorized player.
- Genetic samples, soil specimens and field-bred seeds drop beside the player when their inventory is full, including in Creative. Repeated clicks cannot claim them twice.
- Grandmas Journal checks permission at both parent crops before breeding. A rejected cross keeps the selected parent for a later retry.
- Field crosses mark both parents as used and save the journal before handing over the seed, so item-delivery callbacks see the completed cross.
- Same-species field crosses keep existing hybrid ancestry even when only one parent has it or the parents have different variety names. Names still pass on only when both parents share one.

## 1.3.5

- Greenhouse heating, ventilation, planting presets and harvest export settings now respect build permissions.
- Removed or replaced greenhouses, Breeding Stations, Soil Research Tables and Genetics Laboratories reject late inventory clicks. Putting the same block back cannot reopen the old inventory.
- Research controls reject spectators, players without build permission and clicks sent after walking away. Locked laboratory modes also reject the selection on the server.
- Soil Research Table and laboratory upgrades check permissions before changing the machine or using a module. A table only consumes its module after the upgrade succeeds.
- Breaking a Mk II or Mk III Soil Research Table returns its installed upgrade modules along with the inventory.
- Hoppers use the research machines' input slot rules. They cannot insert unrelated items or fill result slots; donor seeds require unlocked hybrid mode.
- Grafting waits when neither genes nor rare traits can improve. Seeds, compost, samples and supports stay in place, and the laboratory explains why. Useful gene improvements and unlocked rare-trait transfers still work.

## 1.3.4

- Moved old update scripts, screenshots and verification reports into local backups. Kept the current build tools and changed the asset audit to write reports under `build/`.
- Reworked 23 seed icons and cleaned up String Seeds. All 265 seed textures have been checked at 64×64 and at inventory size.
- Unnamed crops now return seeds with their original genes, rare traits and hybrid ancestry. Resource-crop loot no longer mixes in seeds with default genes.
- Breeding Stations keep an existing hybrid's ancestry when crossing two seeds of the same species.
- Seed Bags and Seed Vaults accept the same supported vanilla and modded planting items as breeding machines.
- Seed planting checks permissions before changing a plot or using a seed. Greenhouse settings and vault locks also respect build permissions.
- Starter journals are dropped safely when a Creative inventory is full. Joining again won't issue extra copies.
- Scheduled sprinklers keep using stored water after the supply reaches its reserve. Refills still protect the reserve, and schedules and redstone still stop watering.
- Reloading a controller cannot leave an old valve-open state active. The Spanner and machine screens explain when water is held in reserve.
- Journal recipe pictures keep all ingredient alternatives when the catalogue is rebuilt. The guides explain accepted breeding batches and irrigation reserves.
- Smaller journal tabs fit their controls. On small GUI resolutions, use Ctrl+scroll or Ctrl+arrow keys to reach the rest of a journal or machine panel; Shift+scroll moves sideways.
- Long irrigation zone names have a full-name tooltip. Vertical Greenhouses no longer send unnecessary comparator updates.

## 1.3.3

- A late delivery click can’t hand over supplies for a new Shared order round.
- The Contribute button checks the same main inventory slots as the delivery itself. Offhand items stay put.
- Claiming emeralds only withdraws what fits in your inventory, including in Creative. The rest stay on the board.
- Shared orders have separate Contribute and Claim buttons so an older reward won’t hold up a new delivery.
- The board shows how many items you’ve personally delivered toward each open order.

## 1.3.2

- The Batch Cooking Station queues the recipe shown when you click, even while browsing recipes quickly.
- The cancel button follows its batch as the list shifts. A stale click can’t cancel another batch of the same dish.
- When the container slots are full, the station can use fresh ingredients in place of preserves. It tells you whether to clear a finished meal slot or a container slot.
- The station shows when a batch is queued and returns to idle as soon as the last batch finishes or is cancelled.
- The queue buttons show when all eight spaces are occupied or the selected recipe is unavailable. Ingredient previews follow the active recipes.

## 1.3.1

- Breaking a linked valve frees its Irrigation Controller slot once the valve’s chunk is loaded. A replacement can use that slot straight away.
- New links now enforce the 32-block range, including diagonal placements. Existing links keep their settings.
- A scheduled zone only opens if its water path will still work after the controller closes the other zones.
- Sprinklers and Vertical Greenhouses check the reserve before drawing a bucket. Two machines refilling at once can no longer draw through a scheduled valve below its reserve.
- The controller screen updates valve names and links while it’s open.

## 1.3.0

- Schedule six named irrigation zones from one controller. Set a water reserve or run a zone by hand from the controller screen.
- Set a species and minimum genes for Vertical Greenhouse batches. Only matching seeds count toward the target. The rest go into a separate output row.
- Discover six companion planting pairings with Grandmas Journal. Each recorded trial shows how to plant the pair and what it does.
- Fill shared market orders over several deliveries. The board keeps progress for the whole world, and every contributor can claim a reward after the order is filled.
- Queue meals at the Batch Cooking Station. It uses the existing recipes and puts empty recipe containers in their own slots.
- The new machines use the same copper fittings as the Seed Analyzer with their own 64×64 dials, oven and burners. Their screens use the garden-style controls.

## 1.2.38

- Removing redstone power from a sprinkler or its adjacent valve resumes the current spray straight away, without charging the tank again.
- You can fill or empty a sprinkler with a bucket in your offhand while your main hand is empty.
- Sprinkler reports show the shut valve's location and zone name, and say when redstone is holding it shut.
- Sprinklers still water soil with random ticks turned off. The status now explains why crops get no growth boost.

## 1.2.37

- Iron and obsidian crop sticks now give the advertised +8 and +12 percentage points on field and Breeding Station mutations. Wood still gives +5.
- The Breeding Station saves your sticks when the parents have no mutation recipe.
- Unused sticks stack with fresh ones after you recover them. Worn sticks keep their remaining life.
- Crop stick wear saves between whole seconds, so reloading a chunk no longer restores that wear.

## 1.2.36

- Spanner traces show the exact amount of water at the source they find.
- A shut valve's zone name appears when it blocks a trace or another valve's supply.
- Pipe and sprinkler reports give the direction of a connection into an unloaded chunk. Valve reports also give the fitting's coordinates.
- Sprinkler and Vertical Greenhouse intake reports distinguish a partly unloaded reservoir shell from a pipe crossing a chunk border.
- The Spanner marks the actual face of a capped pipe or unloaded connection.

## 1.2.35

- The Spanner shows how much water a valve's zone contains while the valve is shut, with the same flow status as the right-click report.
- A pipe inspection shows where its supply trace ends and why, including remote valves and capped ports.
- The inspection overlay clears when you switch to the wrong tool, replace the inspected block, or leave the world.
- The server skips empty inspection packets, and the sprinkler preview waits for its target chunk to load.

## 1.2.34

- Reopening an adjacent valve by hand resumes an already-paid spray immediately.
- Changing a sprinkler's redstone mode with the Spanner can resume the same spray without another charge.
- A paused sprinkler reports when the paid spray can resume, even if the tank is empty.
- Removed sprinklers reject bucket transfers through stale tank references.
- Sprinklers register hydration before announcing an active spray and skip growth tracking when they reached no farmland.

## 1.2.33

- Placing pipes, valves, sprinklers, and basins from the inventory keeps adjacent unloaded chunks asleep.
- Newly placed fittings connect to their loaded neighbors immediately, including at chunk borders.
- Moved Mk II and Mk III basins place at their saved tier in one step and start empty.
- Saving basin upgrades leaves chunks beyond neighboring solid blocks unloaded.

## 1.2.32

- The Spanner tells you when a reservoir blueprint crosses into an unloaded chunk.
- Reservoir panels repair incorrect water gauges from the core tank.
- Reservoir outlets repair missing pipe arms when the adjoining chunk is available. Capped pipe faces stay closed.
- Repairing a stale gauge also refreshes the comparator reading, including through a solid block.
- A full Mk II reservoir starts a fresh ten-second generation interval once space opens.

## 1.2.31

- Placing a Vertical Greenhouse at a chunk border keeps neighboring chunks unloaded and connects the intake to existing pipes.
- Breaking a greenhouse section tears down the column and clears its pipe connectors without loading nearby chunks.
- Rotating or mirroring a greenhouse in a structure turns all three sections together.
- Pumps holding the old tank handle see an empty tank after the greenhouse is dismantled.
- Saving tank or tray changes leaves chunks beyond neighboring solid blocks unloaded.

## 1.2.30

- Pipes placed by a structure repair their own connectors and the ones on adjoining fittings. Disabled faces stay capped.
- Removing a pipe clears the arms on its loaded neighbors, including when the removal skips normal block updates.
- Adding or removing an Irrigation Basin updates adjoining pipe, valve, and sprinkler connectors at chunk borders.
- Breaking a connected reservoir outlet clears its pipe arm when the tank dismantles.

## 1.2.29

- Eclipse Lotus now gives pink dye and Moon Orchid gives light blue dye. Sunfire Lily gives yellow dye and Twilight Sage gives magenta dye.
- Field harvests, both greenhouses, and Grandmas Journal now agree on what each flower produces. Breed the dedicated resource crops for Ender Pearls, Amethyst Shards, Glow Berries, and Glowstone Dust.

## 1.2.28

- The Spanner shows how many connected sources can deliver a full bucket, alongside their combined water level.
- When every source is short of water, the trace points to the fullest tank and sprinkler inspection shows how much that tank contains.
- Tracing from any Vertical Greenhouse section starts at the intake. If the structure is incomplete, the trace says so.
- Sprinkler inspection and the Vertical Greenhouse intake reuse their supply search when explaining blocked or missing routes.
- A network that exceeds the search limit no longer reports a closed valve unless one was actually found.

## 1.2.27

- Named valves keep their zone name when moved. The name appears on the item and in the Spanner overlay.
- Renaming a valve saves its name without checking comparators or loading neighboring chunks.
- Valve zone reports show why the nearest inactive sprinkler is stopped, even while other heads are running.
- Zone reports give the coordinates of the closed valve or disabled pipe face blocking the supply.
- Oversized zones report that supply and sprinkler counts are unavailable, including when the valve is closed.

## 1.2.26

- Sprinklers repair both ends of their pipe connections when loaded. Disabled faces stay closed.
- A sprinkler rechecks saved connections when an adjoining chunk becomes available.
- Loading a valve repairs a missing pipe arm even when the valve itself already looks right.
- Removing a sprinkler clears its hydration and loaded pipe connections without loading chunks across the border.
- Removing a valve clears loaded fittings without looking for comparators in unloaded chunks.

## 1.2.25

- Mk II and Mk III basins keep their upgrade when broken and placed again. The item shows its tier and watering radius, and the basin starts empty.
- Basin upgrades respect building permissions and use the material only after the upgrade succeeds.
- A full or empty basin explains why a bucket transfer failed and keeps the held bucket intact.
- Empty-hand and Spanner inspection show the basin's water, range, and whether a Mk III basin has source water available for refills.
- Loading a legacy upgrade or removing a basin at a chunk border leaves neighboring chunks unloaded.

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
