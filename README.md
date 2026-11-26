# Agri Evolution

I built Agri Evolution around the kind of farm I wanted to play in Minecraft. Start with a handful of Wild Seeds, cross crops to see what grows, and work your way up to a greenhouse that can grow and breed seeds while you tend the rest of the farm.

## Install

Agri Evolution runs on **Minecraft 1.21.1** with **Java 21**. In the release downloads, choose the JAR that matches your loader and put it in your profile’s `mods` folder. [Downloads](https://github.com/GWSheridan/Agri-Evolution/releases/latest) · [Changelog](CHANGELOG.md) · [Wiki](https://github.com/GWSheridan/Agri-Evolution/wiki)

| Loader | Minimum version | Release JAR |
| --- | --- | --- |
| NeoForge | 21.1.267 | `agri_evolution-1.21.1-neoforge-*.jar` |
| Forge | 52.1.21 | `agri_evolution-1.21.1-forge-*.jar` |
| Fabric | Fabric Loader 0.20.1 and Fabric API 0.116.18+1.21.1 | `agri_evolution-1.21.1-fabric-*.jar` |

For multiplayer, use the same loader and mod version on the server and all clients. JEI is optional and must match your loader. Back up your world before upgrading. Keep an existing world on its original loader.

## Start your first garden

1. Break short or tall grass in the Overworld to find **Wild Wheat Seeds**. These take the place of the usual wheat seed drops from grass.
2. Till soil near water, plant the seeds on hydrated farmland, and let the crops mature. If weeds appear, use a rake on the crop or the farmland below it.
3. Open **Grandmas Journal** for illustrated breeding recipes, crop discoveries, and growing tips. Use the **Building & Machines Journal** for tools, irrigation, and construction guides. You receive both books the first time you join a world with the mod.
4. For your first cross, craft a Wild Seed with a dirt block to get a Dirt Seed. Grow it next to a Wild Wheat plant. Sneak-right-click one mature parent with Grandmas Journal, then right-click the other for a chance to get Lettuce Seeds. Replant both parents before trying again.
5. Use Grandmas Journal to find more crosses. Keep parents within eight blocks of each other and check the growing instructions for any soil or catalyst a new crop needs.

If a journal or machine panel is larger than your chosen GUI scale allows, hold **Ctrl** and scroll or press the arrow keys to move around it. **Ctrl+Shift+scroll** moves sideways. The text and item slots keep their normal size.

## What you can build and grow

- **Breed and study crops.** Discover food, herbs, and resources through crosses. Improve traits such as growth and yield, compare seed stacks in the Seed Vault, and use the Soil Sampler to check growing conditions. The Breeding Station can also produce another seed from matching vanilla or supported modded seeds with Nutrient Compost.
- **Set up irrigation.** Start with an Irrigation Basin or fill a Sprinkler by hand. For larger plots, use a Spanner to form a copper Water Reservoir and run pipes through valves to your sprinklers. The Mk II Reservoir replenishes its own water.
- **Automate a greenhouse.** Build around the Greenhouse Controller, following the layout in the Building & Machines Journal. Later, build a three-block-tall Vertical Greenhouse. It uses water to grow up to six trays and breed selected seeds, with separate outputs for harvests and offspring.
- **Cook and put harvests to work.** Make sandwiches, soups, meals, desserts, and preserves. Stock a Lunch Box for automatic feeding, turn spare crops and weeds into compost, or deliver supplies at a Garden Order Board to earn emeralds.

- **Keep your best seed lines.** Give seeds a name at an anvil, then use Grandmas Journal on the plant to record the variety. Search for it by name in the Seed Vault. Open the Garden Order Board’s **Seeds** tab to see which traits earn rewards. [Writing datapack orders](docs/GARDEN_ORDERS.md)
- **Plant crops that help each other.** Grow Basil next to Tomato for fewer weeds, or harvest Beans next to Corn to give it a boost. The Soil Sampler shows which bonus a plot has.

The Genetics Laboratory keeps your materials when a graft cannot improve the seed. Its screen explains when it is waiting for a better donor. Breaking an upgraded Soil Research Table returns the installed modules so you can fit them again after moving it.

Collected samples, field-bred seeds and workstation rewards drop beside you when your inventory is full, including in Creative. Breeding and clipping require permission to work on the parent crops.

## Better growing conditions

After harvesting a mature crop, try a different crop family on the same plot. Grains, legumes, roots and bulbs, leafy crops, fruiting crops, herbs, flowers and resources each form a family. Agri crops receive **+10 to their growth roll out of 200** while rotated. Wheat and vanilla root crops have a **20% chance of one extra stage after natural growth**. All resource crops share one family, so alternate them with food crops or herbs. Repeating a family keeps normal growth; planting or breaking an unripe crop does not advance the rotation history.

Craft **two Compost Mulch** from one Nutrient Compost and two Pulled Weeds. Use it on farmland or its crop. The layer lasts **one Minecraft day (20 minutes)**, halves new weeds and Agri crop nutrition loss, and halves the drying chance of upgraded farmland. It does not supply water or remove existing weeds. Wait for a layer to expire before applying another.

The Soil Sampler's **CARE** tab shows rotation and mulch from the saved sample. Resample to refresh it. Open the **resource catalyst guide** there, or choose **Crop guide** at a Soil Research Table. Search for a crop to see its matching catalyst and growth multiplier. Place the catalyst directly below the farmland, with the crop on top; the catalyst block is not consumed. Opening the guide from a table closes that table normally.

There are now **12 companion trials** to observe with Grandmas Journal. New neighbours include Garlic beside Pepper, Rosemary beside Aubergine, Mint beside Coal and Coriander beside Redstone: from stage 4, each helper halves new weeds on its partner. Harvesting ripe Lentil beside Iron or Chickpea beside Copper restores **8 nutrition** to each adjacent partner. These bonuses use the four direct neighbours, so plant side by side.

Datapacks can assign compatible crops to a rotation family with optional block tags under `agri_evolution:rotation/`: `grain`, `legume`, `root`, `leafy`, `fruit`, `herb`, `flower` and `resource`. Tagged `CropBlock` crops can receive the natural-growth bonus; unknown crops keep their usual behaviour.

## Farm Ledger

Craft a Farm Ledger with a book, a compass and a copper ingot. Open it and choose **Mark field**, then use it on two farmland corners at the same height. A field can cover up to 32×32 blocks, and each player can own 16 fields.

In **Layouts**, choose one of four plans and search for seeds. Select a seed, then click plots to paint them; Shift-click fills a whole row. Scroll to move through a large field, or Shift-scroll to move sideways. **Save** keeps the plan. **Reload** discards the draft and gets the latest saved layout. Copy layouts between fields with the same dimensions.

Turn on **Preview**, close the screen and hold the ledger to see planting outlines. Green means correct, gold means missing, red means the wrong plant, purple means unsuitable soil and grey means unknown. The ledger provides guidance; planting remains in your hands.

**Status** shows seed requirements, carried seeds and growing conditions. Nearby fields update within 64 blocks while the ledger is being viewed or previewed. Other fields retain dated snapshots. Unloaded cells remain unknown, and the ledger never loads chunks to inspect them.

Select a field, then use the ledger on a Botanical Greenhouse, Vertical Greenhouse, Irrigation Controller or Water Reservoir to link it. Each field supports eight machine links. The **Sharing** tab accepts an online player name or UUID and offers viewing or layout-editing access. Only the owner can rename or delete the field and manage invitations. Conflicting layout edits keep your draft until you reload it.

## Resource Workshop

Craft a Resource Workshop with copper ingots, a piston, smooth stone and a crafting table. Open it and use the arrows to check a recipe's material, binder and required tier. Insert the two ingredients to start processing. A full output pauses work.

Mk I makes Soil Conditioner for Compost Mulch and Copper Couplings for pipes. Mk II adds Amethyst Lenses for soil research and Reinforced Brackets for irrigation controllers. Mk III makes Nether Heat Cores and Ender Relays for greenhouse and soil research upgrades. Install the two Workshop upgrades on the placed block in order.

Hoppers feed material from above and binder from the side. Take output from below. Crops still give their usual harvests, and existing crafting recipes are still available.

## Build from source

Use Java 25 to run the Gradle wrapper and install a Java 21 JDK for compilation and Minecraft. Gradle writes each JAR to its loader’s `build/libs` directory.

```powershell
.\gradlew.bat build
```

Use `:neoforge:build`, `:forge:build` or `:fabric:build` to build a single loader. Each loader provides `runClient` and `runServer` tasks, for example `:fabric:runClient`. Add `-PwithJei` to a run to include JEI.

Install Node.js and run `.\gradlew.bat -PgameplayTests verifyMultiLoader` to compare the three JARs and run the NeoForge gameplay and resource checks. CI also starts the Forge and Fabric distributions on dedicated test servers and checks that a farm saves and reloads. To run either server check locally, use `pwsh -File tools/verify_loader_server.ps1 -Loader forge -Java C:/path/to/java21/bin/java.exe` and substitute `fabric` for the second loader.

Gameplay code and game-ready assets are in `common`. Each loader module contains its own registration, events and other platform code. The model and journal catalogue generators are in `tools`.

## Bugs and suggestions

If you find a bug, tell me what happened, how to reproduce it, and which Minecraft, loader and mod versions you used. Include a screenshot or crash report if it helps. You can use the same issue tracker for suggestions. [Issue tracker](https://github.com/GWSheridan/Agri-Evolution/issues)

## Credits

- Author: GWSHERIDAN
- Original Creator: GWSHERIDAN
- Developer Credit: GWSHERIDAN

AgriCraft got me interested in crop breeding. I built Agri Evolution as a standalone mod so I could take farming, genetics, and automation in my own direction, with my own gameplay code and textures. [AgriCraft](https://github.com/AgriCraft/AgriCraft)

## License

Agri Evolution is available under the MIT License. [MIT License](LICENSE)
