package com.compactmekanismsun.registries;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

public class MekTileEntityTypes {

    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(MekExt.MOD_ID);

    public static final TileEntityTypeRegistryObject<TileEntityCompactArtificialSun> COMPACT_ARTIFICIAL_SUN =
          TILE_ENTITY_TYPES.mekBuilder(MekBlocks.COMPACT_ARTIFICIAL_SUN, TileEntityCompactArtificialSun::new)
                .clientTicker(TileEntityMekanism::tickClient)
                .serverTicker(TileEntityMekanism::tickServer)
                .withSimple(Capabilities.CONFIG_CARD)
                .build();

    private MekTileEntityTypes() {
    }
}