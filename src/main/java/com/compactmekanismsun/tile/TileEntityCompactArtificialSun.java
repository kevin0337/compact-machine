package com.compactmekanismsun.tile;

import com.hamburger0abcde.mekanismsun.common.registries.MSChemicals;
import com.compactmekanismsun.config.MekExtConfig;
import com.compactmekanismsun.registries.MekBlocks;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ChemicalSlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 紧凑型人造太阳：把上游 mekanismsun 的 7×7×7 多方块「人造太阳」压成一台单方块机器。
 *
 * <p>行为完全对齐上游 {@code ArtificialSunMultiblockData}：
 * <ul>
 *   <li>燃料：氢（{@link MekanismChemicals#HYDROGEN}）</li>
 *   <li>产物：能量（{@link #ENERGY_PER_HYDROGEN} J / mB 氢）+ 氦（{@link MSChemicals#HELIUM}，1:1）</li>
 *   <li>副作用：燃烧时把世界时间钉在 1000（正午）并关闭下雨，可通过 {@link #sunMode} 开关</li>
 * </ul>
 *
 * <p>数值默认取上游 {@code MSGeneralConfig} 的同名配置默认值，保证「一台顶一个满配多方块」。
 * 唯一的例外是 {@link #ENERGY_CAPACITY}：上游是 81,920,000,000 J（默认 1 mB/t 下 40 tick 的存量），
 * 本机改成满档 100 mB/t 跑满 1 秒所需的 4.096 TJ。
 *
 * <p><b>能量单位是 J。</b>Mekanism 能量容器内部按 Joule 存，GUI 默认按 FE 显示（{@code J = FE × 2.5}），
 * 所以界面上的产能 / 存量数字是配置值除以 2.5 之后的读数。
 */
public class TileEntityCompactArtificialSun extends TileEntityConfigurableMachine {

    /** 燃料容量默认值（mB 氢，= 256 B），实际值见 {@code config/compactmekanismsun/general.toml}。 */
    public static final long FUEL_CAPACITY = 256_000L;
    /** 氦缓存容量默认值（mB，= 256 B）。 */
    public static final long WASTE_CAPACITY = 256_000L;
    /**
     * 内部能量缓存默认值（J）。
     *
     * <p>= 100 mB/t × 2,048,000,000 J/mB × 20 tick = 4.096 TJ（= 1.6384 TFE），即默认满档 1 秒的产出。
     * 实际容量就是配置里 {@code energyCapacity} 的值，不做任何自动放大（见 {@link #getEnergyBufferCapacity()}）。
     */
    public static final long ENERGY_CAPACITY = 4_096_000_000_000L;
    /** 每 mB 氢产出能量的默认值（J；GUI 按 FE 显示时 ÷2.5 = 819,200,000 FE/mB）。 */
    public static final long ENERGY_PER_HYDROGEN = 2_048_000_000L;
    /** 默认燃烧速率（mB/t），对齐上游 sunDefaultBurnRate。 */
    public static final double DEFAULT_BURN_RATE = 1.0;
    /** 燃烧速率上限默认值（mB/t），对齐上游 sunMaxBurnRate。 */
    public static final double MAX_BURN_RATE = 100.0;
    /** 环境温度（K）：不燃烧时核心回落到的温度。 */
    public static final double AMBIENT_TEMPERATURE = 300.0;
    /** 满档燃烧（{@code maxBurnRate}）时的核心温度（K）。 */
    public static final double MAX_CORE_TEMPERATURE = 5000.0;
    /**
     * 每 tick 向目标温度靠近的比例。
     *
     * <p>温度只是显示量，不做真实的传热/散热模拟；给个一阶惯性就能让它升降有过程感，
     * 又不会在机器刚点着的一瞬间跳到 5000 K。
     */
    private static final double TEMPERATURE_EASING = 0.08;
    /**
     * 瞬时燃烧速率的一阶低通系数，只影响显示。
     *
     * <p>{@code lastBurnRate} 是瞬时值：速率设成 0.1 时每 10 tick 才有 1 mB，
     * 直接拿它算温度和产出，GUI 会每秒抖十次（0 / 满值 / 0）。低通一次就稳了。
     */
    private static final double RATE_SMOOTHING = 0.15;

    @ContainerSync
    public IChemicalTank fuelTank;
    @ContainerSync
    public IChemicalTank wasteTank;
    @ContainerSync
    public IEnergyContainer energyContainer;

    /** 目标燃烧速率（mB/t）。 */
    @ContainerSync
    private double burnRate = DEFAULT_BURN_RATE;
    /** 上一 tick 实际烧掉的氢（mB，可为小数），用于 GUI 显示。 */
    @ContainerSync
    private double lastBurnRate;
    /** 平滑后的燃烧速率（mB/t），温度与「产出」显示都读它，不读瞬时值。 */
    @ContainerSync
    private double smoothBurnRate;
    /**
     * 核心温度（K）。
     *
     * <p>纯显示量，不参与产能与消耗的计算，所以不需要写进 NBT：存档时按环境温度重新起算即可。
     */
    @ContainerSync
    private double coreTemperature = AMBIENT_TEMPERATURE;
    /**
     * 不足 1 mB 的燃料余额。
     *
     * <p>燃料箱只认整数 mB，但燃烧速率可以小于 1（配置下限 0.1）。
     * 直接 {@code clampToLong(0.1)} 会截成 0，机器永远不动；所以把每 tick 的需求累加到这里，
     * 攒够 1 mB 才真正扣燃料、产能、产氦。
     */
    private double partialFuel;
    /** 氦的小数累积量（1 mB 氢 = 1 mB 氦）。 */
    private double partialWaste;
    /** 「人造太阳模式」：三态，默认关闭。 */
    @ContainerSync
    private SunMode sunMode = SunMode.OFF;

    /**
     * 「人造太阳模式」：三态互斥，由设置页里的按钮循环切换。
     *
     * <p>不是布尔值——只要开就是二选一，而「保持夜晚」和「保持正午」是对等的两个方向，
     * 都得能单独选中，另外还需要一个完全不干预世界的关闭态。
     */
    public enum SunMode {
        /** 保持正午并停雨。 */
        BURNING("gui.compactmekanismsun.mode.burning"),
        /** 保持夜晚并停雨。 */
        NIGHT("gui.compactmekanismsun.mode.night"),
        /** 不干预世界时间与天气。 */
        OFF("gui.compactmekanismsun.mode.off");

        private final String translationKey;

        SunMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getText() {
            return Component.translatable(translationKey);
        }

        public SunMode next() {
            SunMode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public static SunMode byIndex(int index) {
            SunMode[] all = values();
            return index >= 0 && index < all.length ? all[index] : OFF;
        }
    }

    public TileEntityCompactArtificialSun(BlockPos pos, BlockState state) {
        super(MekBlocks.COMPACT_ARTIFICIAL_SUN, pos, state);

        // 化学品：顶部进氢，底部出氦，侧面可配。
        ConfigInfo chemicalConfig = configComponent.getConfig(TransmissionType.CHEMICAL);
        if (chemicalConfig != null) {
            chemicalConfig.addSlotInfo(DataType.INPUT, new ChemicalSlotInfo(true, false, fuelTank));
            chemicalConfig.addSlotInfo(DataType.OUTPUT, new ChemicalSlotInfo(false, true, wasteTank));
            chemicalConfig.setDataType(DataType.INPUT, RelativeSide.TOP);
            chemicalConfig.setDataType(DataType.OUTPUT, RelativeSide.BOTTOM);
            chemicalConfig.setCanEject(true);
            chemicalConfig.setEjecting(true);
        }

        // 能量：只出不进（它是发电机，不耗电）。
        configComponent.setupOutputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL, TransmissionType.ENERGY)
              .setCanTankEject(tank -> tank == wasteTank);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(fuelTank = VariableCapacityChemicalTank.create(
              () -> MekExtConfig.GENERAL.fuelCapacity.get(),
              // 外部不能把氢抽走（氢只能被这台机器自己烧掉），但内部/手动读回不受限。
              // 注意：这里不能写成无条件的 false——那会连 internal 操作一起拒掉。
              (stack, automationType) -> automationType != AutomationType.EXTERNAL,
              (stack, automationType) -> true,
              stack -> stack.is(MekanismChemicals.HYDROGEN),
              ChemicalAttributeValidator.ALWAYS_ALLOW,
              listener));
        builder.addTank(wasteTank = VariableCapacityChemicalTank.create(
              () -> MekExtConfig.GENERAL.wasteCapacity.get(),
              // 氦只出：外部可以抽。
              (stack, automationType) -> true,
              // 外部不能往里灌氦；但机器自己产出的氦必须能进去。
              // 之前这里写成无条件 false，导致 insert() 在 canInsertModern 处直接返回，
              // 机器烧了氢却一点氦都存不下——就是「机器没有产物」的根因。
              (stack, automationType) -> automationType != AutomationType.EXTERNAL,
              stack -> stack.is(MSChemicals.HELIUM),
              ChemicalAttributeValidator.ALWAYS_ALLOW,
              listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = VariableCapacityEnergyContainer.output(this::getEnergyBufferCapacity, listener));
        return builder.build();
    }

    /**
     * 能量缓存容量，<b>直接等于 {@code energyCapacity} 配置值</b>。
     *
     * <p>之前按 {@code maxBurnRate × energyPerHydrogen × 20} 自动放大，但实测玩家看到的上限
     * 和这个公式对不上，等于配置写了却没有可预期的效果。现在不再做任何隐式放大：
     * 配置多少就是多少，想要更大就自己调 {@code energyCapacity}。
     *
     * <p>末尾取 {@code max(capacity, stored)} 是防一个边界：玩家把容量调小时，
     * {@code getNeeded()} 会变成负数，{@code BasicEnergyContainer.insert} 里
     * {@code toAdd = min(正数, 负数)} 反而会把已存能量越减越少。
     */
    private long getEnergyBufferCapacity() {
        long configured = MekExtConfig.GENERAL.energyCapacity.get();
        long stored = energyContainer == null ? 0L : energyContainer.getEnergy();
        return Math.max(configured, stored);
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();

        long stored = fuelTank.getStored();
        long energyNeeded = energyContainer.getNeeded();
        long wasteNeeded = wasteTank.getNeeded();

        // 燃料箱只认整 mB，而速率可以小到 0.1：先把这 tick 的需求攒进 partialFuel，
        // 攒够整 mB 才扣燃料、产能。否则 (long) 0.1 == 0，机器会一直卡在「不燃烧」。
        //
        // 停机条件一共五个：没氢、能量槽满、氦槽满、速率为 0、红石未激活。
        // 氦槽满要停机，是因为产物出不去再烧只会白烧氢；红石那块必须显式判，
        // TileEntityMekanism 只提供支持位和 tab，不会自动拦住 onUpdateServer。
        // 一 tick 能烧多少，还受「能量缓冲剩余空间」约束：多烧的那部分 insert 装不进去就是白烧，
        // 而且 whole * energyPerHydrogen 在大配置下会溢出 long，溢出成负数会让 insert 反过来扣电。
        long energyPerMb = Math.max(1L, MekExtConfig.GENERAL.energyPerHydrogen.get());
        long maxByEnergy = energyNeeded / energyPerMb;

        double burnedThisTick = 0.0;
        if (stored > 0L && energyNeeded > 0L && wasteNeeded > 0L && burnRate > 0.0 && isRedstoneActivated()) {
            partialFuel += burnRate;
            long whole = Math.min(Math.min((long) partialFuel, stored), maxByEnergy);
            if (whole > 0L) {
                partialFuel -= whole;
                burnedThisTick = whole;
                energyContainer.insert(whole * energyPerMb, Action.EXECUTE, AutomationType.INTERNAL);
                fuelTank.shrinkStack(whole, Action.EXECUTE);

                partialWaste += whole;
                long newWaste = Mth.lfloor(partialWaste);
                if (newWaste > 0L) {
                    ChemicalStack remainder = wasteTank.insert(MSChemicals.HELIUM.asStack(newWaste), Action.EXECUTE, AutomationType.INTERNAL);
                    // 只扣掉真正塞进槽里的量：氦槽满了的时候余额留在 partialWaste，
                    // 等玩家或管道腾出空间再继续产，而不是凭空蒸发。
                    partialWaste -= newWaste - remainder.getAmount();
                }
            }
        }
        // 燃料耗尽时别让余额无限涨，最多留 1 mB 的欠账。
        if (partialFuel > 1.0) {
            partialFuel = 1.0;
        }
        lastBurnRate = burnedThisTick;
        smoothBurnRate += (burnedThisTick - smoothBurnRate) * RATE_SMOOTHING;
        setActive(burnedThisTick > 0.0);
        updateCoreTemperature();

        // 人造太阳模式是独立开关：只要不是「关闭」就作用于世界，跟这一 tick 有没有在烧无关。
        if (sunMode != SunMode.OFF) {
            applySunMode(level);
        }
        return sendUpdatePacket;
    }

    /** 按当前模式作用到世界：正午 / 夜晚，两种都顺带停雨。 */
    private void applySunMode(Level level) {
        // onUpdateServer 只在服务端跑；时间与天气会由服务端自然同步给客户端。
        if (level instanceof ServerLevel serverLevel) {
            switch (sunMode) {
                case BURNING -> {
                    serverLevel.setDayTime(1000L);
                    serverLevel.getLevelData().setRaining(false);
                }
                case NIGHT -> {
                    serverLevel.setDayTime(18000L);
                    serverLevel.getLevelData().setRaining(false);
                }
                case OFF -> {
                    // 不会走到这里，外层已过滤
                }
            }
        }
    }

    public SunMode getSunMode() {
        return sunMode;
    }

    public void setSunMode(SunMode mode) {
        if (this.sunMode != mode) {
            this.sunMode = mode;
            markForSave();
        }
    }

    /** 循环切换到下一个模式，供设置页按钮使用。 */
    public void cycleSunMode() {
        setSunMode(sunMode.next());
    }

    public double getBurnRate() {
        return burnRate;
    }

    public void setBurnRate(double rate) {
        // 速率从客户端包进来，必须挡住 NaN / Infinity：Mth.clamp 对 NaN 全是 false 分支，
        // 会原样把 NaN 放进来，接着 coreTemperature 也变 NaN，存档写进去就再也回不来了。
        if (!Double.isFinite(rate)) {
            return;
        }
        double clamped = Mth.clamp(rate, 0.0, MekExtConfig.GENERAL.maxBurnRate.get());
        if (this.burnRate != clamped) {
            this.burnRate = clamped;
            markForSave();
        }
    }

    public double getLastBurnRate() {
        return lastBurnRate;
    }

    /** 核心温度（K），供 GUI 的温度信息页显示。 */
    public double getCoreTemperature() {
        return coreTemperature;
    }

    /** 当前每 tick 的产能（J），供 GUI 的能量信息页显示（该页按 FE 换算）。 */
    public long getOutputPerTick() {
        return (long) (smoothBurnRate * MekExtConfig.GENERAL.energyPerHydrogen.get());
    }

    /**
     * 让核心温度平滑趋近「当前燃烧速率对应的目标温度」。
     *
     * <p>目标温度按 {@code burnRate / maxBurnRate} 在环境温度与 {@link #MAX_CORE_TEMPERATURE} 之间线性插值；
     * 不燃烧时目标就是环境温度，所以停机后温度会自己降回去。
     * 差到 0.5 K 以内就直接吸附，否则会永远差一点点、每 tick 都触发一次同步。
     */
    private void updateCoreTemperature() {
        double target = AMBIENT_TEMPERATURE;
        if (smoothBurnRate > 1.0E-6) {
            double maxRate = Math.max(1.0E-6, MekExtConfig.GENERAL.maxBurnRate.get());
            double ratio = Mth.clamp(smoothBurnRate / maxRate, 0.0, 1.0);
            target = AMBIENT_TEMPERATURE + ratio * (MAX_CORE_TEMPERATURE - AMBIENT_TEMPERATURE);
        }
        coreTemperature += (target - coreTemperature) * TEMPERATURE_EASING;
        if (Math.abs(target - coreTemperature) < 0.5) {
            coreTemperature = target;
        }
    }

    /** 供 GUI 显示的燃料占比。 */
    public double getFuelScale() {
        long capacity = fuelTank.getCapacity();
        return capacity == 0L ? 0.0 : (double) fuelTank.getStored() / capacity;
    }

    /** 供 GUI 显示的氦占比。 */
    public double getWasteScale() {
        long capacity = wasteTank.getCapacity();
        return capacity == 0L ? 0.0 : (double) wasteTank.getStored() / capacity;
    }

    /**
     * 持久化自定义字段。
     *
     * <p>{@code @ContainerSync} 只负责 GUI 同步，{@code markForSave()} 只是把区块标记成脏，
     * 两者都不会把字段写进 NBT；{@code TileEntityMekanism.saveAdditional} 也只存组件、容器内容和
     * active 状态，不碰子类字段。所以这几个值必须自己存，否则退出重进就回到默认值。
     *
     * <p><b>不要</b>覆写 {@link #persists} 去排除 CHEMICAL/ENERGY：{@code saveAdditional} 里是
     * {@code if (persists(type)) type.saveTo(...)} 才写容器内容，排除掉等于「氢、氦、电全都不存档」。
     * 上游多方块端口那样写是因为它的箱子归 {@code MultiblockData} 管，单方块机器没有这层代理。
     */
    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        // 早于本字段存在的存档没有 burn_rate；直接 getDouble 会拿到 0，机器读档后彻底不烧。
        // 同时把读到的值过一遍 clamp+有限性检查：老存档可能带着超过新 maxBurnRate 的值，
        // 或者被外部工具写进去的 NaN。
        double savedRate = nbt.getDouble("burn_rate");
        this.burnRate = nbt.contains("burn_rate") && Double.isFinite(savedRate)
              ? Mth.clamp(savedRate, 0.0, MekExtConfig.GENERAL.maxBurnRate.get())
              : DEFAULT_BURN_RATE;
        this.sunMode = SunMode.byIndex(nbt.getInt("sun_mode"));
        this.partialWaste = finiteOr(nbt.getDouble("partial_waste"), 0.0);
        this.partialFuel = finiteOr(nbt.getDouble("partial_fuel"), 0.0);
        this.lastBurnRate = finiteOr(nbt.getDouble("last_burn_rate"), 0.0);
        this.smoothBurnRate = finiteOr(nbt.getDouble("smooth_burn_rate"), 0.0);
        // 温度是显示量但会跨存档保留，否则每次重进都从 300 K 重新爬。
        this.coreTemperature = Mth.clamp(finiteOr(nbt.getDouble("core_temperature"), AMBIENT_TEMPERATURE),
              AMBIENT_TEMPERATURE, MAX_CORE_TEMPERATURE);
    }

    /** 读档时挡 NaN/Infinity：外部工具或旧版本可能把非有限值写进 NBT，NaN 会一路污染显示和计算。 */
    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbtTags, provider);
        nbtTags.putDouble("burn_rate", burnRate);
        nbtTags.putInt("sun_mode", sunMode.ordinal());
        nbtTags.putDouble("partial_waste", partialWaste);
        nbtTags.putDouble("partial_fuel", partialFuel);
        nbtTags.putDouble("last_burn_rate", lastBurnRate);
        nbtTags.putDouble("smooth_burn_rate", smoothBurnRate);
        nbtTags.putDouble("core_temperature", coreTemperature);
    }
}