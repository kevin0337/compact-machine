package com.compactmekanismsun.config;

import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.config.value.CachedLongValue;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 紧凑型人造太阳的可调数值，落盘在 {@code config/compactmekanismsun/general.toml}。
 *
 * <p>数值默认值对齐上游 mekanismsun 的人造太阳多方块（`MSGeneralConfig`），
 * 这样一台紧凑机 = 一座满配多方块的产能。
 */
public class MekExtGeneralConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedLongValue energyPerHydrogen;
    public final CachedLongValue fuelCapacity;
    public final CachedLongValue wasteCapacity;
    public final CachedLongValue energyCapacity;
    public final CachedDoubleValue maxBurnRate;

    MekExtGeneralConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Compact Artificial Sun").push("compact_artificial_sun");

        energyPerHydrogen = CachedLongValue.wrap(this, builder
              .comment("FE produced per mB of hydrogen burned")
              .defineInRange("energyPerHydrogen", 2_048_000_000L, 1L, 1_000_000_000_000_000L));
        fuelCapacity = CachedLongValue.wrap(this, builder
              .comment("Hydrogen tank capacity (mB). 256 B = 256000 mB.")
              .defineInRange("fuelCapacity", 256_000L, 1L, 10_000_000L));
        wasteCapacity = CachedLongValue.wrap(this, builder
              .comment("Helium tank capacity (mB). 256 B = 256000 mB.")
              .defineInRange("wasteCapacity", 256_000L, 1L, 10_000_000L));
        energyCapacity = CachedLongValue.wrap(this, builder
              .comment("Internal energy buffer (FE).",
                    "Default = maxBurnRate x energyPerHydrogen x 20 ticks",
                    "(100 mB/t x 2,048,000,000 FE/mB x 20 = 4.096 T), so one second of full-rate output fits.",
                    "This is a hard cap: the buffer is exactly this value, nothing scales it up for you.")
              .defineInRange("energyCapacity", 4_096_000_000_000L, 1L, Long.MAX_VALUE));
        maxBurnRate = CachedDoubleValue.wrap(this, builder
              .comment("Maximum hydrogen burn rate (mB/t)")
              .defineInRange("maxBurnRate", 100.0, 0.1, 1000.0));

        builder.pop();
        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "general";
    }

    @Override
    public String getTranslation() {
        return "General Config";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public ModConfig.Type getConfigType() {
        return ModConfig.Type.SERVER;
    }
}