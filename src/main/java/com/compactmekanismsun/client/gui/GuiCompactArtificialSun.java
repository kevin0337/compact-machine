package com.compactmekanismsun.client.gui;

import com.compactmekanismsun.config.MekExtConfig;
import com.compactmekanismsun.network.PacketSunSettings;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.GuiHeatTab;
import mekanism.client.gui.element.tab.GuiWarningTab;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.IWarningTracker;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.InputValidator;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 紧凑型人造太阳 GUI。
 *
 * <p>布局照上游 {@code GuiArtificialSun}：左氢 gauge、右氦 gauge、最右能量 gauge，
 * 中间 {@link GuiInnerScreen} 只显示「状态」和「设定速率」两行，下方文本框输入燃烧速率（可任意小数）。
 *
 * <p>左下角挂 Mekanism 机器统一的能量 tab（{@link GuiEnergyTab}）与温度 tab（{@link GuiHeatTab}），
 * 位置分别是 y=137 和 y=109，正好接在侧面配置(6)、运输配置(34)、{@link GuiSunSettingsTab}(62) 下面。
 *
 * <p>「常阳模式」开关不在这里——它是一个独立设置页，由 {@link GuiSunSettingsTab}
 * 挂在左侧标签列的最下面，点开是 {@link GuiSunSettingsWindow}。
 *
 * <p>继承 {@link GuiConfigurableTile} 会自动带上侧面配置页与运输配置页。
 */
public class GuiCompactArtificialSun extends GuiConfigurableTile<TileEntityCompactArtificialSun, MekanismTileContainer<TileEntityCompactArtificialSun>> {

    private GuiTextField burnRateField;
    private GuiSunSettingsTab sunSettingsTab;

    public GuiCompactArtificialSun(MekanismTileContainer<TileEntityCompactArtificialSun> container, Inventory inventory, Component title) {
        super(container, inventory, title);
        dynamicSlots = true;
        // 上游同样把面板拉高 16px，给文本框腾出位置
        imageHeight += 16;
        inventoryLabelY = imageHeight - 92;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiChemicalGauge(() -> tile.fuelTank, () -> List.of(tile.fuelTank, tile.wasteTank), GaugeType.STANDARD, this, 7, 20));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.wasteTank, () -> List.of(tile.fuelTank, tile.wasteTank), GaugeType.STANDARD, this, 125, 20));
        addRenderableWidget(new GuiEnergyGauge(tile.energyContainer, GaugeType.STANDARD, this, 150, 20));
        addRenderableWidget(new GuiInnerScreen(this, 30, 20, 80, 42, this::screenText));

        // 燃烧速率输入框：照上游 GuiArtificialSun 的坐标（40, 68, 54x12），
        // DECIMAL 校验 + 回车/勾选提交，可输入任意小数（0 ~ 配置上限）
        burnRateField = addRenderableWidget(new GuiTextField(this, 40, 68, 54, 12));
        burnRateField.setEnterHandler(this::applyBurnRate);
        burnRateField.setInputValidator(InputValidator.DECIMAL);
        burnRateField.setMaxLength(8);
        burnRateField.addCheckmarkButton(this::applyBurnRate);

        // 常阳开关挪到独立设置页，和侧面/运输配置页排在同一条竖列上
        sunSettingsTab = addRenderableWidget(new GuiSunSettingsTab(this, tile, () -> sunSettingsTab));

        // 左下角：Mek 机器统一的两块信息 tab。坐标由元素自己定（-26,137）和（-26,109），
        // 正好排在我们那个 y=62 的设置页下面，互不相交。
        addRenderableWidget(new GuiEnergyTab(this, () -> List.of(
              Component.translatable("gui.compactmekanismsun.producing", EnergyDisplay.of(tile.getOutputPerTick()).getTextComponent()),
              MekanismLang.ENERGY.translate(EnergyDisplay.of(tile.energyContainer.getEnergy())))));
        addRenderableWidget(new GuiHeatTab(this, () -> List.of(
              MekanismLang.TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(tile.getCoreTemperature(), TemperatureUnit.KELVIN, true)))));
    }

    @Override
    protected void addWarningTab(IWarningTracker warningTracker) {
        // 默认的警告 tab 也占左下角，会和温度 tab 撞在一起；Mekanism 自己的热力发电机也是这么挪到右侧的。
        addRenderableWidget(new GuiWarningTab(this, warningTracker, false));
    }

    /**
     * 中间信息屏：状态 / 设定速率。
     *
     * <p>「实烧速率」和「燃料存量」两行已按要求移除——燃烧是否真的在进行看状态行就够了，
     * 燃料存量看左边第一根 gauge。屏幕上只留用户真正会盯着看的两项。
     */
    private List<Component> screenText() {
        ArrayList<Component> lines = new ArrayList<>(2);
        lines.add(MekanismLang.STATUS.translate(tile.getActive() ? MekanismLang.ACTIVE : MekanismLang.IDLE));
        lines.add(Component.translatable("gui.compactmekanismsun.burn_rate", formatRate(tile.getBurnRate())));
        return lines;
    }

    /** 提交文本框里的速率；超过上限就钳到上限，而不是拒绝。 */
    private void applyBurnRate() {
        String text = burnRateField.getText();
        if (text.isEmpty()) {
            return;
        }
        try {
            double rate = Double.parseDouble(text);
            // 用户要求：填超上限时取最大速率，而不是静默丢弃
            double clamped = Mth.clamp(rate, 0.0, MekExtConfig.GENERAL.maxBurnRate.get());
            tile.setBurnRate(clamped);
            pushSettings();
        } catch (NumberFormatException ignored) {
            // 输入框里有非法内容，静默丢弃
        }
        burnRateField.setText("");
    }

    private static String formatRate(double rate) {
        if (rate == Math.rint(rate)) {
            return Long.toString((long) rate);
        }
        return String.format(Locale.ROOT, "%.2f", rate);
    }

    private void pushSettings() {
        // 只发速率：带上 sunMode 会把客户端那份可能过期的模式一并推回服务端。
        PacketDistributor.sendToServer(PacketSunSettings.rate(tile.getBurnRate(), tile.getBlockPos()));
    }
}