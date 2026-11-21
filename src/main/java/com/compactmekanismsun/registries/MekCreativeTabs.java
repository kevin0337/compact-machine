package com.compactmekanismsun.registries;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.MekLang;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registries.MekanismCreativeTabs;
import net.minecraft.world.item.CreativeModeTab;

public class MekCreativeTabs {

    public static final CreativeTabDeferredRegister CREATIVE_TABS = new CreativeTabDeferredRegister(MekExt.MOD_ID);

    public static final MekanismDeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.registerMain(
          MekLang.MEKEXT,
          MekBlocks.COMPACT_ARTIFICIAL_SUN.getItemHolder(),
          builder -> builder.withTabsBefore(MekanismCreativeTabs.MEKANISM.getKey())
                .displayItems((displayParameters, output) -> CreativeTabDeferredRegister.addToDisplay(MekBlocks.BLOCKS, output)));

    private MekCreativeTabs() {
    }
}