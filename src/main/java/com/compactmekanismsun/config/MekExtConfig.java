package com.compactmekanismsun.config;

import com.compactmekanismsun.MekExt;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mekanism.common.config.IMekanismConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * 配置注册入口。沿用 Mekanism 的 {@link IMekanismConfig} 体系，
 * 这样配置值能被 Mekanism 的缓存机制在重载时刷新。
 */
public class MekExtConfig {

    private static final Map<IConfigSpec, IMekanismConfig> KNOWN_CONFIGS = new HashMap<>();

    public static final MekExtGeneralConfig GENERAL = new MekExtGeneralConfig();

    private MekExtConfig() {
    }

    public static void registerConfigs(ModContainer container) {
        container.registerConfig(
              GENERAL.getConfigType(),
              GENERAL.getConfigSpec(),
              MekExt.MOD_ID + "/" + GENERAL.getFileName() + ".toml");
        KNOWN_CONFIGS.put(GENERAL.getConfigSpec(), GENERAL);
    }

    public static void onConfigLoad(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        IMekanismConfig mekanismConfig;
        if (config.getModId().equals(MekExt.MOD_ID)
              && (mekanismConfig = KNOWN_CONFIGS.get(config.getSpec())) != null) {
            mekanismConfig.clearCache(event instanceof ModConfigEvent.Unloading);
        }
    }

    public static Collection<IMekanismConfig> getConfigs() {
        return Collections.unmodifiableCollection(KNOWN_CONFIGS.values());
    }
}