# Changelog

## 1.3.36

- Custom seeds recheck the used hand after planting permissions. Swapped stacks and changes to their count, name, genes, rare trait or ancestry cancel the old placement without spending the newer seed.
- Soil and target space must still match the checked plot. New blocks, replacement crops, changed moisture and farmland upgrades made during access checks are preserved instead of being overwritten or receiving an old placement.
- Dead players and contexts from another world cannot plant crops. Death, Spectator mode, dimension changes and revoked build access during a permission check also cancel the attempt.
- A seed must match the dispatched species and, for player use, be the actual stack in the used hand. Detached copies, unrelated seeds and empty automation contexts cannot create free crops.
- Planted traits come from one validated copy of the seed. Placement notifications cannot mix old genes with newer traits or variety data, or write the original seed's genetics into a replacement species.
- Stale contexts no longer load faraway chunks to inspect soil, and placements outside build height return safely. All crop species and farmland tiers retain normal planting, including Creative use, valid playerless automation, Adventure CanPlaceOn permissions and cancelled-placement refunds.

## 1.3.35

- Journal field breeding rechecks the held book after permissions at both parents. Swapped books and changes to their name, count or saved records cancel the cross without overwriting newer journal data.
- Both parents must remain the original placed crops with unchanged state and saved data. Replacements, younger growth, changed supports, edited genes and newer variety or nutrition data cannot inherit the old cross.
- Breeding rights spent during an access check are respected. A cross completed inside a permission callback cannot produce a second seed or have its saved journal selection restored by the older attempt.
- Death, a dimension change, switching to Spectator, lost build permission or switching to parent-selection mode cancels the pending cross without spending the remaining parent rights.
- Invalid journal dispatches and detached context copies cannot record discoveries on another held item or breed crops. Dead players and contexts from another world are rejected before records are written.
- Ordinary inspection and recording remain available without field-edit permission. A plain permission denial keeps the selected parent for a later authorized retry; normal breeding still retains parent data and returns one offspring with its inherited genes, ancestry and variety.

## 1.3.34

- Trowels and rakes recheck the actual tool after item-use permissions. Swapped tools, changed names or saved data, and detached context copies cannot spend durability or edit the field through an old click.
- Dead players cannot use garden tools. Death, Spectator mode, a dimension change, lost build permission or switching crouch mode during an access check cancels the pending work.
- Single-crop tending and raking capture the plant and soil before their permissions. Replacement plants, newer growth and changed soil are preserved instead of receiving the old action's growth, nutrition or watering.
- Tilling rechecks the player, tool and target after tool-modification events and final permissions. Interrupted events leave the dirt, held item and cooldown alone.
- Area raking rechecks each plot and the current tool before clearing weeds. Tool or player changes stop the patch; a separately changed plot is skipped while other permitted plots remain usable.
- Finishing a single use checks the held tool before adding its active appearance, wear or cooldown. Normal tending, tilling, planting and raking retain their existing effects and durability costs.

## 1.3.33

- Genetic Clippers recheck the used hand after item-use permissions. Swapped tools and changes to their name, count or saved data cancel the old click without creating a sample or spending the plant's clipping credit.
- Dead players cannot clip crops. Death, Spectator mode, a dimension change or revoked build permission during an access check also cancels the pending clipping.
- Clipping keeps its original plant through permissions. Replanted crops and replacement crop entities cannot inherit an old click, even when the block state matches.
- A crop replaced with another species cannot produce a sample labelled with the previous crop's species. Newer growth, weed and support states are left alone; a crop made immature during permissions keeps its sample credit.
- Clipping requires the actual Genetic Clippers stack in the used hand. Unrelated tools and detached context copies cannot claim samples.
- Normal clipping still returns one accurate sample in either hand, including with a full inventory. Genes, rare traits and variety names remain accurate, and the planted crop retains its growth, nutrition, ancestry and supports. Sample credit is recorded before delivery so reward callbacks cannot collect it twice.

## 1.3.32

- Compost Bin deposits recheck the used hand after item-use permissions. Swapped materials and changes to their count, name or saved data cancel the old deposit without raising the fill level or spending the newer item.
- Ready-batch collection rechecks the player's held item and the bin after permissions are checked. An interrupted collection leaves the batch ready and issues no reward.
- Dead players cannot deposit, collect or feed crops. Death, Spectator mode, a dimension change or revoked build permission during an access check also cancels the pending action.
- Nutrient Compost retains the original plant through access checks. A replacement crop cannot inherit the old feed, even with the same block state; newer growth is preserved too.
- Crop feeding requires actual Nutrient Compost in the used hand. Changed compost stacks and invalid item dispatches cannot grow a crop by spending another item.
- Normal Survival and Creative costs remain the same: leaves add one fill point, pulled weeds add two, a ready batch returns two compost, and feeding adds up to two growth stages plus 25 nutrition. Mature crops keep the compost and receive no extra nutrition.

## 1.3.31

- Vertical Greenhouse bucket fills recheck the used hand after both section and base permissions. Swapped buckets and changes to their count, name or saved data cancel the old fill without transferring water or replacing the newer held item.
- A fill keeps its original tank through access checks. Replacing the controller or rotating the column cancels the pending transfer instead of filling a newer machine.
- Dead players cannot fill a column. Death, Spectator mode, a dimension change or revoked build permission during access checks also leaves the tank and bucket alone.
- Controller lookups check that the section and tank are still current. An unrelated block snapshot now returns safely instead of trying to read greenhouse properties from it.
- Creative fills retain the supply bucket without adding empty buckets to the inventory, matching the other irrigation fittings.
- Manual filling still works through all three sections in either hand. Full and incomplete columns keep the supplied bucket, stacked supplies spend one bucket at a time, and a full inventory drops exactly one returned container.

## 1.3.30

- Botanical Greenhouse upgrades, heater fuel and bucket actions recheck the used hand after permissions are checked. Replaced items and changes to their count, name or saved data cancel the old action without spending resources or overwriting the new held item.
- Resource clicks retain their original controller. A replacement controller cannot inherit the pending fill, fuel or upgrade, even when its block state matches.
- Dead players cannot use controller resources. Death, a controller rotation or switching to crouch mode during an access check cancels the pending action and preserves the newer state.
- Door installation saves both wall states before checking doorway permissions. Changes to either glass block cancel the retrofit before anything is cleared or refunded.
- Doorway permissions also recheck the original controller, facing and held door. Replaced controllers, changed door items and switching to crouch mode leave the wall and inventory alone.
- Normal survival and Creative resource actions retain their existing costs. Offhand door installation still returns each removed glass block once, and a door that genuinely cannot be placed restores the original wall and unspent item.

## 1.3.29

- Old valve ticks and neighbor updates cannot reopen a newly closed valve or restore its previous fitting state. Connection repairs check the current state before writing it back.
- Manual valve toggles recheck the used hand after permission callbacks. Replaced items and changes to their count or saved data cancel the old click, including clicks that would otherwise reach a replacement valve.
- Dead players cannot toggle valves. Death, Spectator mode or a dimension change during an access check also cancels the pending toggle.
- Flow checks reject stale open snapshots and safely return unavailable for replacement blocks. Removed valves cannot authorize scheduled or refill flow.
- Removed and detached valves reject name, controller and schedule changes. Old load callbacks cannot queue repairs at a replacement block, and callbacks on valves without a world no longer crash or erase their saved links.
- Controller links copy their assigned position so a caller cannot silently move a link by changing a mutable position. Normal linking, release, manual controls, redstone repairs and Unicode zone names remain covered by gameplay checks.

## 1.3.28

- Sprinkler and basin bucket actions recheck the used hand after permission callbacks. Replaced buckets and changes to their count, name or saved data cancel the old transfer without spending water or overwriting the new held item.
- Bucket actions retain the original sprinkler or basin through access checks. Replacing the block entity cancels the old click, even when the replacement has the same block state.
- Dead players cannot fill or drain these fittings. Death, Spectator mode or a dimension change during a permission callback also cancels the pending transfer.
- Basin upgrades recheck the original basin, held material, tier and water state after permissions are checked. Replaced materials and replacement basins remain untouched, and a player who stops crouching no longer spends an upgrade material.
- Both basin upgrades still use one Copper or Amethyst Block, preserve stored water and apply their normal watering radius. Main-hand and offhand bucket stacks retain their exact fill and collection costs.
- A fitting repair on the same sprinkler still allows a valid fill. Filling starts one paid spray, and collecting a stored bucket keeps that already-paid interval running.

## 1.3.27

- Reservoir bucket and Conduit actions recheck the used hand after panel and core permission callbacks. Replaced items and changes to their count, name or saved data cancel the old action without transferring water or spending a module.
- Reservoir clicks reject dead players and stop if a permission callback kills the player, changes dimension or switches to Spectator. Revoked build permission also leaves the tank and held item alone.
- Replacing a reservoir core during either permission check cancels the old click. An old panel location cannot redirect a bucket into a newer core; gauge-only changes still allow normal interaction with the same tank.
- Old reservoir ticks cannot advance Mk II generation after the core's block state changes. The next current tick resumes the saved cycle and produces exactly one bucket when it completes.
- Late gauge updates cannot overwrite a newer panel state or restore a dismantled shell part over another block.
- Generation through old references rejects removed, detached and incomplete tanks, including tanks without a world. Normal 10-second generation, saved partial cycles, offhand upgrades and stacked-bucket collection keep their existing costs.

## 1.3.26

- Mk III basin refill ticks check the current basin, world, position and block state. An old tick cannot use one basin's water source to fill a different basin or its replacement.
- Late basin load callbacks cannot restore stored water or apply an old upgrade tier to a replacement. Legacy upgrades still migrate normally on the current basin.
- Detached basins cannot register hydration tickets or water farmland beside an empty replacement. Current filled basins retain their normal range and hydration.
- Sprinkler controls, watering cycles, load callbacks and bucket-start callbacks check that the head is still placed. Old heads cannot spend buffered water, drain connected basins, change replacement fittings or start a free spray. Starting a head without a world also returns safely.
- Sprinkler ticks reject the wrong world, position and old block state before updating fittings or charging for another spray. Current heads still use exactly 25 mB per interval.
- Delayed removal or unload cleanup from an old sprinkler leaves a replacement's growth bonus registered. Removing the current sprinkler still clears its own bonus and hydration ticket, and load callbacks preserve an already-paid spray.

## 1.3.25

- Closing the Botanical Greenhouse dashboard, Seed Bank, Harvest Manager or storage disables its old controls and transfers. Each screen checks its owning viewer and rejects dead players and spectators.
- Seed Bank and storage slots check access before direct insertion or withdrawal, including the player inventory slots. Old slot references cannot move named seeds, genetics or harvested items after the screen closes or the controller is removed.
- Closing one player's screen leaves other viewers' screens usable. Adventure players retain ordinary inventory access, and greenhouse previews still work without a world or player.
- Invalid Seed Bank status indices no longer read another slot's eligibility, gene filter or preset status. Valid seed counts, exclusions and saved planting presets keep their existing behavior.
- Botanical Greenhouse ticks check the current controller, world, position and block state before advancing work. Late ticks cannot spend a removed or replaced controller's water and heater fuel.
- A current greenhouse still uses exactly one mB of water and one second of heater fuel per working second. After a rotation, stale ticks are ignored and the next current tick resumes normally.

## 1.3.24

- Closing a Vertical Greenhouse screen disables its old controls and item transfers. Controls and individual slots check the owning viewer and reject dead players and spectators. Ordinary Adventure inventory access and world-free previews still work.
- Removed, replaced or incomplete Vertical Greenhouses reject direct changes to growing, breeding, pairing, water intake, output routes, batch targets and seed filters. Rejected calls preserve the previous settings and progress.
- Invalid output-face and target-gene indices are ignored safely instead of throwing an exception or changing another setting.
- Greenhouse ticks check the current machine, world, position and facing before advancing work. Direct processing also checks its world, and late ticks leave removed machines' old status and progress alone.
- Automation receives a copy of the greenhouse's output-slot list. Changing that list can no longer expose planted parent trays or alter other machines' output permissions.
- Removed and incomplete columns expose no automation slots. Normal output routing, exact breeding water costs and planted-parent retention remain covered by gameplay checks.

## 1.3.23

- Lunch Boxes recheck the player and packed food after modded food-property queries. Opening a menu, starting another item use, dying, changing game mode or changing dimension cancels that serving before it is spent.
- Moving or replacing a box during food selection cancels the old attempt. Newer food contents and changes to the box's pause, priority, protection or other saved components are preserved.
- Food callbacks cannot turn a detached inspection copy into extra stored portions. A completed serving is saved before its normal food effects and returned container are processed.
- Protected food slots are skipped before querying their food properties for auto-feeding. Unprotected food keeps its normal priority and left-to-right selection order.
- Lunch Box deposits recheck the menu and source stack after checking food properties. A callback that closes the screen, replaces the box or changes the source cannot lose the food or overwrite the replacement.
- Food inspection cannot recursively start another menu transfer. Shift-click validates food before moving it and reuses that result for its destination slots; ordinary deposits, named food, sorting and returned bowls still work normally.

## 1.3.22

- Closing an Irrigation Controller screen disables its old controls. Controls check the owning viewer and reject dead players and spectators. Adventure players can still read the schedule, and closing one screen leaves other viewers' screens usable.
- Removed or replaced controllers cannot change settings, link valves, release the replacement controller's zones or authorize water refills. Removed valves cannot occupy a controller slot.
- Controller ticks check the machine's current world, position and facing before updating its valves. After a rotation, the next current tick resumes scheduling normally.
- Invalid zone and action indices are ignored safely, including reserve and power controls, instead of throwing an exception or changing another setting.
- Saved valve links use the same 32-block limit as new Spanner links. The exact boundary remains valid, and duplicate saved entries cannot overwrite an accepted slot's valve and schedule.
- Releasing zones clears the active-zone display and manual override as well as the valve links. Normal schedules, manual-run expiry and protected water reserves retain their existing behavior.

## 1.3.21

- Soil Research Table upgrades check the current table before installation. An old click cannot demote a Mk III table, undo a rotation or install a module in a replacement machine.
- Both table upgrades respect item-use permissions and recheck the held module after permission callbacks. Changed or replaced modules are left alone, and dead players cannot install upgrades.
- An empty main hand now passes table upgrade clicks to the offhand. Successful installations still use one module and retain the table's inventory.
- Closing a Soil Research Table disables its old controls and transfers. Screens and individual slots check the owning viewer and reject dead players and spectators, while ordinary Adventure inventory use remains available.
- Analysis and refinement recheck the player and the placed table after permission checks. Removed tables reject insertion queries and late status changes.
- Added regression coverage for each tier's research limit, upgrade requirements and all five refinement recipes, including exact ingredient costs and retained sampler research.

## 1.3.20

- Genetics Laboratories reject old research ticks after removal, replacement, rotation or a mismatched world or position. Rejected ticks leave seeds, donor samples, compost, supports and progress alone.
- Changing a seed or donor just before saving no longer carries research time into the replacement inputs. Topping up the same seed line still preserves progress. Falling back from a locked or invalid saved mode clears its old progress.
- Closing a laboratory screen disables its old controls and transfers. Screens and individual slots check the owning viewer and reject spectators and dead players, while ordinary Adventure inventory use remains available.
- Removed laboratories reject direct upgrade, mode and insertion calls through old references.
- Laboratory upgrades respect the used hand's item-use permission. They recheck the held module and the placed machine after permission callbacks, so changed items and replacement machines are left alone.
- An empty main hand passes laboratory upgrade clicks to the offhand. Both upgrade tiers still consume one module only when installation succeeds, and installed upgrades remain recoverable when the machine is broken.

## 1.3.19

- Batch Cooking Stations prepare meals from a saved copy of their inputs and commit only if the inventory, current order and recipe still match. Recipe callbacks cannot charge replacement ingredients or overwrite newly inserted outputs.
- Cancelling the current batch during a recipe callback cancels that cooking attempt. The next queued batch keeps its count, and appending a later batch still works normally.
- Recursive recipe callbacks cannot cook another serving from the same station. A failed callback leaves the station able to retry.
- Removed or replaced stations reject cooking, queue changes and sided automation through old references. Cooking also checks the station's world, and invalid inventory or queue indices are rejected safely.
- Closing a cooking screen disables its old controls and transfers. Screens and individual slots check the owning viewer and reject dead players and spectators, while ordinary Adventure inventory and queue use remain available.
- Breaking a kitchen or replacing its recipe during preparation cancels the old attempt. Existing ingredient alternatives and returned containers still use the same recipes.

## 1.3.18

- Spanner edits to pipes, sprinklers, valve names and controller selections reject spectators and dead players. Valve inspection remains available without changing its settings.
- Spanner actions recheck the held tool after permission callbacks, including changes to its name or other saved data. A replaced tool is left alone.
- Replacing a sprinkler, valve or controller during a permission check cancels the old edit. Valve links recheck both machines before reserving a controller slot.
- Denied or interrupted valve links keep the selected controller for a retry. Permission denial, an unavailable controller or a replaced target no longer clears that selection. A normal link attempt still clears it, so an unnamed valve can be named on the next click.
- Reservoir formation rechecks all 26 copper blocks and the centre water source after permissions are checked. Changed blocks are preserved, and a changed held Spanner cancels assembly.
- Permission callbacks cannot start another reservoir formation inside the current one. Ordinary assembly still starts with 1000 mB and works with a Spanner in either hand.

## 1.3.17

- Seed Breeding Stations reject old ticks after removal or replacement. A late completion cannot spend parents, compost or supports, or create another offspring.
- Breeding ticks check the machine's world, position and current block state before advancing progress or checking greenhouse bonuses. A rotated machine resumes on its next current tick.
- Changing parents just before a save no longer carries the previous pair's progress into the replacement seeds. This also covers changes to seed genetics. Topping up the same parents still preserves progress across a reload.
- Closing a Breeding Station disables its old pickup and shift-click actions. Another player, a spectator or a dead viewer cannot transfer items through that screen.
- Parent, reagent, output and player-inventory slots apply the screen's access checks, including direct slot transfers after closing or removal.
- Removed stations reject insertion queries. Normal Adventure inventory use, native seed variants, genetic crosses and world-free GUI previews remain supported.

## 1.3.16

- Seed Vault screens check their owning viewer and reject spectator and dead-player transfers. A nearby player cannot use another viewer's inventory or lock controls.
- Vault access checks the player's dimension while retaining the normal interaction reach. Matching coordinates in another dimension cannot keep a vault screen active.
- Closing a vault permanently disables that screen's old pickup, shift-click, deposit and lock actions.
- Vault and player-inventory slots apply the same access checks as the screen. Old direct slot references cannot withdraw or insert seeds after closing or removal.
- Removed vaults reject placement queries and direct lock or intake-setting changes. Invalid slot and trait indices are ignored without wrapping into another lock or throwing an exception.
- Normal Adventure inventory access, separate genetic lines, locked-slot protection and world-free GUI previews remain supported. Hoppers still cannot access vault contents.

## 1.3.15

- Shared Garden Order rewards reserve each payment before handing over emeralds. Inventory callbacks cannot claim the same balance twice, and saves during delivery see the reduced balance.
- Rewards earned during an existing claim remain available. Unpaid emeralds merge back into the current balance without overwriting new earnings.
- Reward claims charge only what the inventory actually accepts, including partial insertions that return false. Rejected payments and failed insertions keep their unpaid balance available for a retry.
- Direct shared-order claims and contributions reject spectators and dead players. Other contributors can still claim independently while a payment is in progress.
- Closing the Garden Order Board or Preserving Station permanently disables its old screen. Late page, delivery, pickup and shift-click actions leave the inventory alone.
- Board and Preserving Station screens check the owning player and dimension as well as the placed block and distance. Matching coordinates in another dimension cannot keep an old board screen active.

## 1.3.14

- Soil sampling checks build and item-use permissions at the soil and its planted Agri crop. Spectators and denied players receive no specimen, survey or collection credit.
- A replaced soil block or changed held sampler cancels the old sampling action before it changes the tool or issues an item.
- Area surveys check each plot and skip denied plots in both the saved report and discovery count. If a neighboring permission check changes the held tool, the survey is cancelled without saving into the old sampler.
- Soil Sampler advice describes accessible farmland. Vigorous crops' displayed growth rolls now use the same final cap as actual crop growth.
- Direct Soil Research Table analysis and refinement reject removed tables and players without access. Rejected calls keep the sampler, specimens, soil and catalysts unchanged.

## 1.3.13

- Crop stick ticks check the live crop state and block entity before changing anything. Old expiry calls cannot restore removed crops, expire a replacement plant or rewind crop growth.
- Stale wear ticks no longer charge a newer crop state. Obsidian weed cleanup cannot restore an old support tier over a newer state.
- Loaded and assigned wooden and iron stick countdowns stay within their normal lifetimes. Obsidian sticks ignore old finite countdowns and remain recoverable as permanent supports.
- Installing crop sticks and recovering them with a Spanner reject spectators and respect the clicked hand's item-use permission.
- Support actions recheck the crop, block entity and held stack after permission hooks. Replaced plants and changed held items are left alone without charging an upgrade or returning another plant's sticks.
- Obsidian installation clears weeds in the same block update as the upgrade. Failed installation writes report failure, and later callbacks cannot reset wear on a different plant or support tier.

## 1.3.12

- Closing a Seed Bag or Lunch Box permanently disables that screen's old controls. Late deposits and withdrawals leave both inventories alone.
- Portable storage screens check that their saved contents still match the item. An older screen cannot restore supplies withdrawn through a newer screen or overwrite contents changed by another system.
- Seed Bag and Lunch Box pickup, shift-click and buttons reject spectators, dead players and players who do not own the inventory.
- Portable slots check the same access rules as the buttons. Offhand containers remain locked in place while normal Adventure inventory use still works.
- Lunch Box food callbacks cannot trigger another serving for the same player. The guard clears after a callback fails and does not block other players from eating.
- Food containers return through the inventory's normal overflow handling. Switching to Creative during a food callback cannot silently discard a bowl or bottle when the inventory is full.

## 1.3.11

- Greenhouse exports reserve items before handing them to another block. An insertion callback cannot export the same seed twice or duplicate it in the greenhouse's break drops.
- Both greenhouse types block recursive export calls and stop exports from removed source blocks.
- The Botanical Greenhouse rechecks its offered stack after destination simulation. A callback that replaces the stack cannot export the old item or delete its replacement.
- Rejected items return to their original output buffer without overwriting newly stored items. If that buffer has filled, the rejected items drop beside the greenhouse with their names and genetics intact.
- Partial insertion still removes only the amount the destination accepted. A callback that breaks the source cannot lose the unaccepted portion.
- Removed Seed Vaults reject direct offspring intake through stale references.

## 1.3.10

- Manual bucket transfers on sprinklers, basins, reservoirs and Vertical Greenhouses respect build and item-use permissions. Denied transfers keep the water and held bucket unchanged.
- Reservoir upgrades check permissions before using a Conduit. Reservoir panels and upper greenhouse sections also check access to their core tank.
- Valve toggles check permissions with the item in the hand being used. Late clicks cannot restore a valve that has been replaced.
- An empty main hand lets offhand buckets reach basins, reservoirs and Vertical Greenhouses. Reservoir Conduits work from the offhand too.
- Crouch-use basin upgrades work from either hand. Spectators cannot upgrade a basin or move stored water.
- Old basin fill and drain calls cannot undo a newer upgrade or restore a removed basin.
- Irrigation Controller screens reject spectators and players who have left the controller's dimension. Old screens also check that they still refer to the placed controller.

## 1.3.9

- Old Compost Bin collection calls cannot collect a second batch after fresh material has been deposited. This also prevents duplicate rewards during item-delivery callbacks.
- Late compost deposits leave newer fill levels and replacement blocks alone, without consuming material. Fermentation checks the current bin state before finishing.
- Compost deposits and collection respect item-use permissions, and spectators cannot change a bin.
- An empty main hand lets compost material in the offhand reach the bin instead of stopping at its status message.
- Rakes check item-use permission at the soil as well as the crop. Area weeding skips denied plots and charges durability only for weeds it clears.
- Trowels leave blocks replaced during a tool event intact. Failed tilling costs no durability or cooldown.

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
