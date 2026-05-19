package com.pathmind.util;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Loader-agnostic paths and mod discovery (replaces Fabric Loader calls).
 */
public final class ModPaths {
    private ModPaths() {
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Optional<IModInfo> getModInfo(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo());
    }
}
