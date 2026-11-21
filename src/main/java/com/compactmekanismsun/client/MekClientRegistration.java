package com.compactmekanismsun.client;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.client.gui.GuiCompactArtificialSun;
import com.compactmekanismsun.registries.MekContainerTypes;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MekExt.MOD_ID, value = Dist.CLIENT)
public class MekClientRegistration {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // GuiCompactArtificialSun 已把 TILE/CONTAINER 两个类型参数钉死，构造器签名唯一，泛型可正常推断。
        ClientRegistrationUtil.registerScreen(event, MekContainerTypes.COMPACT_ARTIFICIAL_SUN, GuiCompactArtificialSun::new);
    }
}