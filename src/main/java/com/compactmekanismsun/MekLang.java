package com.compactmekanismsun;

import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

public enum MekLang implements ILangEntry {
    MEKEXT("other", "mod_name"),
    DESCRIPTION_COMPACT_ARTIFICIAL_SUN("description", "compact_artificial_sun"),
    COMPACT_ARTIFICIAL_SUN("block", "compact_artificial_sun");

    private final String key;

    MekLang(String type, String path) {
        this(Util.makeDescriptionId(type, MekExt.rl(path)));
    }

    MekLang(String key) {
        this.key = key;
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}