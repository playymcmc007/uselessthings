package com.playymcmc007.uselessthings.client;

public class AtlasFlagHolder {
    private static final ThreadLocal<Boolean> IS_ATLAS = ThreadLocal.withInitial(() -> false);

    public static void setAtlas(boolean isAtlas) {
        IS_ATLAS.set(isAtlas);
    }

    public static boolean isAtlas() {
        return IS_ATLAS.get();
    }

    public static void clear() {
        IS_ATLAS.remove();
    }
}