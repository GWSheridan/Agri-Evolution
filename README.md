# Agri Evolution

I built Agri Evolution around the kind of farm I wanted to play in Minecraft. Start with a handful of Wild Seeds, cross crops to see what grows, and work your way up to a greenhouse that can grow and breed seeds while you tend the rest of the farm.

## Install

Agri Evolution **1.2.0** is built for **Minecraft 1.21.1**, **NeoForge 21.1.265**, and **Java 21**. Put the release JAR in your NeoForge profile’s `mods` folder. For multiplayer, install the same version on the server and every client. [Download 1.2.0](https://github.com/GWSheridan/Agri-Evolution/releases/tag/v1.2.0) · [Changelog](CHANGELOG.md)

## Start your first garden

1. Break short or tall grass in the Overworld to find **Wild Wheat Seeds**. These take the place of the usual wheat seed drops from grass.
2. Till soil near water, plant the seeds on hydrated farmland, and let the crops mature. If weeds appear, use a rake on the crop or the farmland below it.
3. Open **Grandmas Journal** for illustrated breeding recipes, crop discoveries, and growing tips. Use the **Building & Machines Journal** for tools, irrigation, and construction guides. You receive both books the first time you join a world with the mod.
4. For your first cross, craft a Wild Seed with a dirt block to get a Dirt Seed. Grow it next to a Wild Wheat plant. Sneak-right-click one mature parent with Grandmas Journal, then right-click the other for a chance to get Lettuce Seeds. Replant both parents before trying again.
5. Use Grandmas Journal to find more crosses. Keep parents within eight blocks of each other and check the growing instructions for any soil or catalyst a new crop needs.

## What you can build and grow

- **Breed and study crops.** Discover food, herbs, and resources through crosses. Improve traits such as growth and yield, compare seed stacks in the Seed Vault, and use the Soil Sampler to check growing conditions. The Breeding Station can also produce another seed from matching vanilla or supported modded seeds with Nutrient Compost.
- **Set up irrigation.** Start with an Irrigation Basin or fill a Sprinkler by hand. For larger plots, use a Spanner to form a copper Water Reservoir and run pipes through valves to your sprinklers. The Mk II Reservoir replenishes its own water.
- **Automate a greenhouse.** Build around the Greenhouse Controller, following the layout in the Building & Machines Journal. Later, build a three-block-tall Vertical Greenhouse. It uses water to grow up to six trays and breed selected seeds, with separate outputs for harvests and offspring.
- **Cook and put harvests to work.** Make sandwiches, soups, meals, desserts, and preserves. Stock a Lunch Box for automatic feeding, turn spare crops and weeds into compost, or deliver supplies at a Garden Order Board to earn emeralds.

- **Keep your best seed lines.** Give seeds a name at an anvil, then use Grandmas Journal on the plant to record the variety. Search for it by name in the Seed Vault. Open the Garden Order Board’s **Seeds** tab to see which traits earn rewards. [Writing datapack orders](docs/GARDEN_ORDERS.md)
- **Plant crops that help each other.** Grow Basil next to Tomato for fewer weeds, or harvest Beans next to Corn to give it a boost. The Soil Sampler shows which bonus a plot has.

## Build from source

Use a Java 21 JDK and the included Gradle wrapper. The built JAR is in `build/libs`.

```powershell
.\gradlew.bat --no-daemon build
```

## Bugs and suggestions

If you find a bug, tell me what happened, how to reproduce it, and which Minecraft, NeoForge, and mod versions you used. Include a screenshot or crash report if it helps. You can use the same issue tracker for suggestions. [Issue tracker](https://github.com/GWSheridan/Agri-Evolution/issues)

## Credits

- Author: GWSHERIDAN
- Original Creator: GWSHERIDAN
- Developer Credit: GWSHERIDAN

AgriCraft got me interested in crop breeding. I built Agri Evolution as a standalone mod so I could take farming, genetics, and automation in my own direction, with my own gameplay code and textures. [AgriCraft](https://github.com/AgriCraft/AgriCraft)

## License

Agri Evolution is available under the MIT License. [MIT License](LICENSE)
