package com.compactmekanismsun.registries;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.resource.BlockResourceInfo;

public class MekBlocks {

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MekExt.MOD_ID);

    public static final BlockRegistryObject<BlockTile.BlockTileModel<TileEntityCompactArtificialSun, Machine<TileEntityCompactArtificialSun>>,
          ItemBlockTooltip<BlockTile.BlockTileModel<TileEntityCompactArtificialSun, Machine<TileEntityCompactArtificialSun>>>> COMPACT_ARTIFICIAL_SUN =
          BLOCKS.registerDetails("compact_artificial_sun", () -> new BlockTile.BlockTileModel<>(
                MekBlockTypes.COMPACT_ARTIFICIAL_SUN,
                properties -> properties
                      .mapColor(BlockResourceInfo.STEEL.getMapColor())
                      .strength(0.5f)));

    private MekBlocks() {
    }

    /** 仅用于强制触发本类与 {@link MekBlockTypes} 的静态初始化 */
    public static void init() {
    }
}