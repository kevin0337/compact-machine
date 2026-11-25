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
 *
 * <p><b>能量单位是 J，不是 FE。</b>Mekanism 的能量容器内部一律以 Joule 存储，
 * 上游配置项（{@code hydrogenEnergy} / {@code energyCapacity}）填的也是 J。
 * GUI 默认按 FE 显示（{@code general.energyType=FORGE_ENERGY}），
 * 换算走 {@code general.feConversionRate}（默认 2.5，即 {@code J = FE × 2.5}）。
 * 所以配置里写的 2,048,000,000 在能量信息页上会显示成 819,200,000 FE —— 这是换算，不是数值不符。
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
              .comment("Energy (Joules) produced per mB of hydrogen burned.",
                    "Mekanism stores energy in Joules internally; the GUI displays FE by default,",
                    "converting at general.feConversionRate (default 2.5 => 1 FE = 2.5 J).",
                    "Default 2,048,000,000 J = 819,200,000 FE per mB.")
              .defineInRange("energyPerHydrogen", 2_048_000_000L, 1L, 1_000_000_000_000_000L));
        fuelCapacity = CachedLongValue.wrap(this, builder
              .comment("Hydrogen tank capacity (mB). 256 B = 256000 mB.")
              .defineInRange("fuelCapacity", 256_000L, 1L, 10_000_000L));
        wasteCapacity = CachedLongValue.wrap(this, builder
              .comment("Helium tank capacity (mB). 256 B = 256000 mB.")
              .defineInRange("wasteCapacity", 256_000L, 1L, 10_000_000L));
        energyCapacity = CachedLongValue.wrap(this, builder
              .comment("Internal energy buffer (Joules; shown as FE in the GUI at 1 FE = 2.5 J).",
                    "Default = maxBurnRate x energyPerHydrogen x 20 ticks",
                    "(100 mB/t x 2,048,000,000 J/mB x 20 = 4.096 TJ = 1.6384 TFE), so one second of full-rate output fits.",
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