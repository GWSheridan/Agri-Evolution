package com.agrievolution.fabric;

import com.agrievolution.platform.WaterTicket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FabricWaterTickets {
    private static final Map<ServerLevel, Map<ChunkPos, List<Ticket>>> LEVELS = new WeakHashMap<>();
    private FabricWaterTickets() {}

    static WaterTicket register(ServerLevel level, AABB bounds) {
        var min = new ChunkPos(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ));
        var max = new ChunkPos(BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ));
        return register(level, bounds::contains, ChunkPos.rangeClosed(min, max).toList());
    }

    static WaterTicket register(ServerLevel level, Predicate<Vec3> matcher, List<ChunkPos> chunks) {
        var locations = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        var ticket = new Ticket(locations, chunks, matcher);
        for (var chunk : chunks) locations.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(ticket);
        return ticket;
    }

    public static boolean contains(ServerLevel level, BlockPos soil) {
        var locations = LEVELS.get(level);
        if (locations == null) return false;
        var tickets = locations.get(new ChunkPos(soil));
        if (tickets == null) return false;
        Vec3 point = Vec3.atCenterOf(soil);
        for (var ticket : tickets) if (ticket.isValid() && ticket.matcher.test(point)) return true;
        return false;
    }

    static void unload(ServerLevel level) { LEVELS.remove(level); }

    private static final class Ticket implements WaterTicket {
        private final Map<ChunkPos, List<Ticket>> locations;
        private final List<ChunkPos> chunks;
        private final Predicate<Vec3> matcher;
        private boolean valid = true;

        private Ticket(Map<ChunkPos, List<Ticket>> locations, List<ChunkPos> chunks, Predicate<Vec3> matcher) {
            this.locations = locations;
            this.chunks = chunks;
            this.matcher = matcher;
        }

        @Override public boolean isValid() { return valid; }
        @Override public void invalidate() {
            if (!valid) return;
            valid = false;
            for (var chunk : chunks) {
                var tickets = locations.get(chunk);
                if (tickets == null) continue;
                tickets.remove(this);
                if (tickets.isEmpty()) locations.remove(chunk);
            }
        }
    }
}
