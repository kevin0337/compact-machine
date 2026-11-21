package com.compactmekanismsun.client.gui;

import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import java.util.function.Supplier;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「常阳模式」设置页的侧边标签。
 *
 * <p>位置放在侧面配置页（y=6）与运输配置页（y=34）下面，y=62，与它们同一条竖列、同样的外观，
 * 点击后打开独立的 {@link GuiSunSettingsWindow}。
 */
public class GuiSunSettingsTab extends GuiWindowCreatorTab<TileEntityCompactArtificialSun, GuiSunSettingsTab> {

    public GuiSunSettingsTab(IGuiWrapper gui, TileEntityCompactArtificialSun tile, Supplier<GuiSunSettingsTab> elementSupplier) {
        super(MekanismUtils.getResource(ResourceType.GUI, "configuration.png"), gui, tile, -26, 62, 26, 18, true, elementSupplier);
        setTooltip(TooltipUtils.create(Component.translatable("gui.compactmekanismsun.settings")));
    }

    @Override
    protected void colorTab(GuiGraphics guiGraphics) {
        MekanismRenderer.color(guiGraphics, SpecialColors.TAB_CONFIGURATION);
    }

    @Override
    protected GuiWindow createWindow(SelectedWindowData windowData) {
        return new GuiSunSettingsWindow(gui(), (getGuiWidth() - GuiSunSettingsWindow.WIDTH) / 2, 15, dataSource, windowData);
    }

    @Override
    protected SelectedWindowData getNextWindowData() {
        // 不复用 Mekanism 的 WindowType：我们的窗口不需要被记住位置/钉住，
        // 用 UNSPECIFIED 最省事，也不会污染它的窗口状态。
        return SelectedWindowData.UNSPECIFIED;
    }
}