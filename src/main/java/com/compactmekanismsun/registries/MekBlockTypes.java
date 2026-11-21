package com.compactmekanismsun.registries;

import com.compactmekanismsun.MekLang;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismSounds;

public class MekBlockTypes {

    public static final Machine<TileEntityCompactArtificialSun> COMPACT_ARTIFICIAL_SUN = Machine.MachineBuilder
          .createMachine(() -> MekTileEntityTypes.COMPACT_ARTIFICIAL_SUN, MekLang.DESCRIPTION_COMPACT_ARTIFICIAL_SUN)
          .withGui(() -> MekContainerTypes.COMPACT_ARTIFICIAL_SUN)
          .withSound(MekanismSounds.ENRICHMENT_CHAMBER)
          // 只配置「哪些传输类型可以配面」：化学品（进氢/出氦）+ 能量（只出不进）。
          .withSideConfig(TransmissionType.CHEMICAL, TransmissionType.ENERGY)
          // Machine 默认带 ACTIVE_LIGHT(8)，换成满亮度 15：燃烧时像一颗真太阳，熄灭时完全不着光。
          // 光照由 AttributeStateActive.adjustProperties 自动按 active 属性切换，不要在方块属性里手写 lightLevel。
          .replace(new Attribute[]{Attributes.ACTIVE_FULL_LIGHT})
          .withSupportedUpgrades(Upgrade.MUFFLING)
          .build();

    private MekBlockTypes() {
    }
}