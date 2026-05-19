package com.pathmind;

import com.pathmind.util.VersionSupport;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main mod class for Pathmind on NeoForge.
 */
@Mod(PathmindMod.MOD_ID)
public class PathmindMod {
    public static final String MOD_ID = "pathmind";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public PathmindMod(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("Initializing Pathmind mod");

        String minecraftVersion = FMLLoader.versionInfo().mcVersion();
        if (!VersionSupport.isSupported(minecraftVersion)) {
            LOGGER.warn("Pathmind targets Minecraft {} but detected {}", VersionSupport.SUPPORTED_RANGE, minecraftVersion);
        }

        LOGGER.info("Pathmind mod initialized successfully");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
