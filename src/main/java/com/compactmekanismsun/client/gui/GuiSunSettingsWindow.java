package com.compactmekanismsun.client.gui;

import com.compactmekanismsun.network.PacketSunSettings;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun.SunMode;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 「人造太阳模式」独立设置窗。
 *
 * <p>一个按钮循环切换 燃烧 → 熄灭 → 关闭，按钮文字就是当前状态。
 * 不用 {@code ToggleButton}：它只有开/关两套贴图，表达不了三态。
 * 用 {@link MekanismButton} 是取它的底色与按下动画，和旁边的 Mekanism 控件同一套视觉。
 *
 * <p>模式与「机器有没有在燃烧」完全解耦：燃烧/熄灭都只改世界时间与天气，跟燃料、能量无关。
 */
public class GuiSunSettingsWindow extends GuiWindow {

    static final int WIDTH = 140;
    private static final int HEIGHT = 66;

    private final TileEntityCompactArtificialSun tile;
    private MekanismButton modeButton;

    public GuiSunSettingsWindow(IGuiWrapper gui, int x, int y, TileEntityCompactArtificialSun tile, SelectedWindowData windowData) {
        super(gui, x, y, WIDTH, HEIGHT, windowData);
        this.tile = tile;
        interactionStrategy = InteractionStrategy.ALL;
        addChild(new GuiInnerScreen(gui, relativeX + 8, relativeY + 20, WIDTH - 16, 26, this::screenText));
        addChild(modeButton = new MekanismButton(gui, relativeX + 8, relativeY + 48, WIDTH - 16, 16,
              tile.getSunMode().getText(), (element, mouseX, mouseY) -> {
                  cycle();
                  return true;
              }));
    }

    private void cycle() {
        // 先改本地值让按钮立刻变，再把新状态发给服务端，由服务端落盘并同步回来。
        tile.cycleSunMode();
        modeButton.setMessage(tile.getSunMode().getText());
        PacketDistributor.sendToServer(PacketSunSettings.mode(tile.getSunMode().ordinal(), tile.getBlockPos()));
    }

    private List<Component> screenText() {
        ArrayList<Component> lines = new ArrayList<>(2);
        lines.add(Component.translatable("gui.compactmekanismsun.settings"));
        lines.add(tile.getSunMode().getText());
        return lines;
    }
}