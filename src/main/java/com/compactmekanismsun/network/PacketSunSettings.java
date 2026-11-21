package com.compactmekanismsun.network;

import com.compactmekanismsun.MekExt;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun;
import com.compactmekanismsun.tile.TileEntityCompactArtificialSun.SunMode;
import io.netty.buffer.ByteBuf;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：改燃烧速率 / 切换人造太阳模式。
 *
 * <p>{@code mask} 是「本次到底要改哪个字段」的位掩码，不是多余参数。
 * 之前一个包无条件同时写两个字段：切模式时会把客户端那份（可能过期的）{@code burnRate}
 * 一并推回服务端，改速率时又会把过期的 {@code sunMode} 推回去，多人高延迟下两边互相踩。
 * 现在谁改的就只发谁，另一个字段在服务端原样不动。
 *
 * <p>模式用 {@code int} 传序号而不是三个 boolean——{@link SunMode} 是三态，
 * 用布尔表达不了「保持夜晚」，以后再加状态也不用改包结构。
 */
public record PacketSunSettings(int mask, int sunMode, double burnRate, BlockPos pos) implements IMekanismPacket {

    /** 本次包里携带 {@link #sunMode()}。 */
    public static final int HAS_MODE = 1;
    /** 本次包里携带 {@link #burnRate()}。 */
    public static final int HAS_RATE = 2;

    /** GUI 到方块的最大交互距离（格），超过就当作无效请求丢弃。 */
    private static final double MAX_DISTANCE_SQR = 64.0 * 64.0;

    /** 只改模式，不碰速率。 */
    public static PacketSunSettings mode(int sunMode, BlockPos pos) {
        return new PacketSunSettings(HAS_MODE, sunMode, 0.0, pos);
    }

    /** 只改速率，不碰模式。 */
    public static PacketSunSettings rate(double burnRate, BlockPos pos) {
        return new PacketSunSettings(HAS_RATE, 0, burnRate, pos);
    }

    public static final CustomPacketPayload.Type<PacketSunSettings> TYPE =
          new CustomPacketPayload.Type<>(MekExt.rl("sun_settings"));

    public static final StreamCodec<ByteBuf, PacketSunSettings> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, PacketSunSettings::mask,
          ByteBufCodecs.VAR_INT, PacketSunSettings::sunMode,
          ByteBufCodecs.DOUBLE, PacketSunSettings::burnRate,
          BlockPos.STREAM_CODEC, PacketSunSettings::pos,
          PacketSunSettings::new
    );

    @Override
    public CustomPacketPayload.Type<PacketSunSettings> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) {
                return;
            }
            // 三条校验缺一不可：隔太远、区块没加载、没权限都直接丢掉。
            if (player.blockPosition().distSqr(pos) > MAX_DISTANCE_SQR) {
                return;
            }
            if (!player.level().isLoaded(pos)) {
                return;
            }
            if (!IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), pos)) {
                return;
            }
            if (player.level().getBlockEntity(pos) instanceof TileEntityCompactArtificialSun tile) {
                if ((mask & HAS_MODE) != 0) {
                    tile.setSunMode(SunMode.byIndex(sunMode));
                }
                // 服务端会再过一遍 setBurnRate 里的有限性检查与 clamp，这里不用重复限制
                if ((mask & HAS_RATE) != 0) {
                    tile.setBurnRate(burnRate);
                }
            }
        });
    }
}