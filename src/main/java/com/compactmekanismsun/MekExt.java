package com.compactmekanismsun;

import com.compactmekanismsun.config.MekExtConfig;
import com.compactmekanismsun.network.MekExtPacketHandler;
import com.compactmekanismsun.registries.MekBlocks;
import com.compactmekanismsun.registries.MekContainerTypes;
import com.compactmekanismsun.registries.MekCreativeTabs;
import com.compactmekanismsun.registries.MekTileEntityTypes;
import mekanism.common.Mekanism;
import mekanism.common.lib.Version;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(MekExt.MOD_ID)
public class MekExt {

    public static final String MOD_ID = "compactmekanismsun";

    public MekExt(IEventBus modBus, ModContainer container) {
        // 网络通道：挂 Mekanism 的 BasePacketHandler，payload 注册交给它
        new MekExtPacketHandler(modBus, new Version(container));
        // 触发 BlockType 静态初始化（机器属性表）
        MekBlocks.init();
        MekContainerTypes.CONTAINER_TYPES.register(modBus);
        MekTileEntityTypes.TILE_ENTITY_TYPES.register(modBus);
        MekBlocks.BLOCKS.register(modBus);
        MekCreativeTabs.CREATIVE_TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        // 配置：config/compactmekanismsun/general.toml（SERVER 类型，随存档/服务器生效）
        MekExtConfig.registerConfigs(container);
        modBus.addListener(MekExtConfig::onConfigLoad);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Mekanism.logger.info("MekExt loaded");
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}