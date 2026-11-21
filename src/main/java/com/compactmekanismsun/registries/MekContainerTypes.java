package com.compactmekanismsun.registries;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class MekContainerTypes {

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MekExt.MOD_ID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityCompactArtificialSun>> COMPACT_ARTIFICIAL_SUN =
          CONTAINER_TYPES.register(MekBlocks.COMPACT_ARTIFICIAL_SUN, TileEntityCompactArtificialSun.class);

    private MekContainerTypes() {
    }
}