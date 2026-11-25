package com.agrievolution.content;

import com.agrievolution.platform.Game;

import com.agrievolution.platform.ModInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;

public final class CropPollination {
    public static final int BONUS = 3;
    static final int WINDOW = 6000;
    private static final String LAST_FLOWER = "AgriLastPollinatedFlower";
    static final TagKey<Block> FLOWERS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "pollinator_crops"));

    public static void beeVisit(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Bee bee) || !(bee.level() instanceof ServerLevel level)) return;
        var memory = Game.hooks().persistentData(bee);
        if (!bee.hasNectar()) {
            memory.remove(LAST_FLOWER);
            return;
        }
        BlockPos flower = bee.getSavedFlowerPos();
        if (flower == null || !flower.closerToCenterThan(bee.position(), 2.5)
                || memory.contains(LAST_FLOWER) && memory.getLong(LAST_FLOWER) == flower.asLong()) return;
        if (!markVisit(level, flower)) return;
        memory.putLong(LAST_FLOWER, flower.asLong());
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, flower.getX() + .5, flower.getY() + .7,
                flower.getZ() + .5, 4, .25, .2, .25, 0);
    }

    static boolean markVisit(ServerLevel level, BlockPos flower) {
        if (!level.hasChunkAt(flower)) return false;
        var state = level.getBlockState(flower);
        if (!state.is(FLOWERS) || !(state.getBlock() instanceof AgriCropBlock crop) || !crop.isMaxAge(state)
                || !(level.getBlockEntity(flower) instanceof AECropBlockEntity plant) || plant.hasCrossed()) return false;
        plant.markPollinated(level.getGameTime() + WINDOW);
        return true;
    }

    private CropPollination() {}
}
