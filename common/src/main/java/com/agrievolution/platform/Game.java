package com.agrievolution.platform;

public final class Game {
    private static GameHooks hooks;

    private Game() {}

    public static void install(GameHooks implementation) {
        if (hooks != null) throw new IllegalStateException("Game hooks already installed");
        hooks = implementation;
    }

    public static GameHooks hooks() {
        if (hooks == null) throw new IllegalStateException("Game hooks have not started");
        return hooks;
    }
}
