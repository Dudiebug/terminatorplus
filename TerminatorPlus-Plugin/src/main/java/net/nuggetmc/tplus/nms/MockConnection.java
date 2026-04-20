package net.nuggetmc.tplus.nms;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.SocketAddress;

@SuppressWarnings("JavaReflectionMemberAccess")
public class MockConnection extends Connection {
    private static final Field PACKET_LISTENER_FIELD;
    private static final Field DISCONNECT_LISTENER_FIELD;

    static {
        // Resolve by type + declaration order so this works across both Mojang-mapped
        // (26.x, unobfuscated) and Spigot-reobf'd (1.21.x) runtimes, where field names
        // are different (`packetListener`/`disconnectListener` vs obf letters).
        Field disconnectListenerField = null;
        Field packetListenerField = null;
        for (Field field : Connection.class.getDeclaredFields()) {
            if (!field.getType().equals(PacketListener.class)) continue;
            if (disconnectListenerField == null) {
                disconnectListenerField = field;
            } else if (packetListenerField == null) {
                packetListenerField = field;
                break;
            }
        }
        if (disconnectListenerField == null || packetListenerField == null) {
            throw new RuntimeException("Could not locate PacketListener fields on Connection");
        }
        disconnectListenerField.setAccessible(true);
        packetListenerField.setAccessible(true);
        DISCONNECT_LISTENER_FIELD = disconnectListenerField;
        PACKET_LISTENER_FIELD = packetListenerField;
    }

    public MockConnection() {
        super(PacketFlow.SERVERBOUND);
        this.channel = new MockChannel(null);
        this.address = new SocketAddress() {
        };
    }

    @Override
    public void flushChannel() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void send(@NotNull Packet<?> packet) {
    }

    @Override
    public void send(@NotNull Packet<?> packet, ChannelFutureListener sendListener) {
    }

    @Override
    public void send(@NotNull Packet<?> packet, ChannelFutureListener sendListener, boolean flag) {
    }

    @Override
    public void setListenerForServerboundHandshake(@NotNull PacketListener packetListener) {
        try {
            PACKET_LISTENER_FIELD.set(this, packetListener);
            DISCONNECT_LISTENER_FIELD.set(this, null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
