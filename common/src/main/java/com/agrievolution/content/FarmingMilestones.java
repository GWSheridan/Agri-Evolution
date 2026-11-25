package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.agrievolution.platform.Network;

public enum FarmingMilestones {
    CROSS("first_cross", "Cross two mature crops"),
    SOIL("study_soil", "Study a soil specimen"),
    RESERVOIR("form_reservoir", "Form a copper reservoir"),
    PRESERVE("first_preserve", "Make a preserve"),
    GREENHOUSE("vertical_greenhouse", "Open a Vertical Greenhouse");

    public static final String CROP_PAGE = "CROP MILESTONES", MACHINE_PAGE = "FARMING MILESTONES";
    private final ResourceLocation id;
    private final String instruction;

    FarmingMilestones(String id, String instruction) {
        this.id = ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "farming/" + id);
        this.instruction = instruction;
    }

    public String instruction() { return instruction; }
    public boolean reached(int mask) { return (mask & (1 << ordinal())) != 0; }

    public void award(Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        var advancement = server.server.getAdvancements().get(id);
        if (advancement != null && server.getAdvancements().award(advancement, "achieved")) sync(server);
    }

    public static void sync(ServerPlayer player) {
        if (!Network.canSend(player, FarmingMilestonesPayload.TYPE)) return;
        int mask = 0;
        for (var step : values()) {
            var advancement = player.server.getAdvancements().get(step.id);
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) mask |= 1 << step.ordinal();
        }
        Network.sendToPlayer(player, new FarmingMilestonesPayload(mask));
    }
}
