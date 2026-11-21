package com.compactmekanismsun.network;

import com.compactmekanismsun.MekExt;
import mekanism.common.lib.Version;
import mekanism.common.network.BasePacketHandler;
import net.neoforged.bus.api.IEventBus;

/**
 * 本 mod 的网络通道：挂在 Mekanism 的 {@link BasePacketHandler} 上，
 * 这样不用自己维护 NeoForge 的 payload 注册与版本协商。
 */
public class MekExtPacketHandler extends BasePacketHandler {

    public MekExtPacketHandler(IEventBus modEventBus, Version version) {
        super(modEventBus, version);
    }

    @Override
    protected void registerClientToServer(PacketRegistrar registrar) {
        registrar.play(PacketSunSettings.TYPE, PacketSunSettings.STREAM_CODEC);
    }

    @Override
    protected void registerServerToClient(PacketRegistrar registrar) {
    }
}