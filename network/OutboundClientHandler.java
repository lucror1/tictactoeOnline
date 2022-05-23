package network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutboundClientHandler extends ChannelOutboundHandlerAdapter {
    // Keep a reference to the client
    private Client client;

    public OutboundClientHandler(Client client) {
        this.client = client;
    }

    // This is currently a dummy method that always returns true
    // In the future, it should return true if it is safe to disconnect
    // I.e the most recent ChannelFuture has finished
    public boolean isSafeToDisconnect() {
        return true;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        //System.out.println("write");

        ctx.write(msg, promise);
    }
}
