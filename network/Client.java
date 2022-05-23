package network;

import java.util.Arrays;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client {
    private String host = null;
    private int port = -1;

    // Need to know the group so it can be shut down
    private EventLoopGroup group;

    // Keep track of the connection's channel
    private Channel ch;

    // Keep track of if the client is connect or not
    private boolean isConnected;

    // Keep track of the client's identity
    private byte identity = Settings.IDENTITY_UNASSIGNED;

    // Keep track of the current turn
    private byte curTurn = Settings.IDENTITY_X;

    // Keep track of who has won each board and the game
    private byte winner = Settings.BOARD_WINNER_NULL;
    private byte[] subBoardWinners = new byte[9];

    // Keep track of the board
    private byte[][] superBoard = new byte[9][9];

    // The required subboard to play on
    // 9 is wild, 0-8 are the linearized subboards
    private byte requiredSubBoard = 9;

    // Keep track of the last time that the board was created and updated
    // Force it to be 0 at the start so it will always accept the first
    //   server packet
    private int timeBoardCreated = 0;
    private int timeLastUpdated = 0;

    // This flag is for the UI to know if the board has been updated or not
    private boolean boardUpdated = false;

    // The last channel future
    // Used to make sure icky stuff doesn't happen with simultaneous requests
    private ChannelFuture lastChannelFuture;

    public Client(String host, int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is invalid");
        }

        this.host = host;
        this.port = port;
    }

    public Client(String host) {
        this(host, Settings.DEFAULT_PORT);
    }

    public void connect() {
        // Make sure the client isn't already connected
        if (isConnected) {
            throw new IllegalStateException("Cannot connect while already connected");
        }

        // Get a reference to this so the client handlers can access this
        Client c = this;

        // Bootstrap the client
        this.group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
            b.group(this.group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new InboundClientHandler(c));
                }
             });
            
        // Connect and grab a reference to the channel
        try {
            this.ch = b.connect(host, port).sync().channel();
        } catch (InterruptedException ex) {}

        // Mark the client as connected
        isConnected = true;
    }

    public void disconnect() {
        // Make sure the client is connected before disconnecting
        if (!isConnected) {
            throw new IllegalStateException("Cannot disconnect while not connected");
        }

        // Sometimes the server does not recieve the final message
        //   because the client closes the connection too early
        // This is a hacky way to make sure that the message actually sends
        // If this stops working, either increase the timer or find a better method
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
        
        // Shut down the boss/worker group
        this.group.shutdownGracefully();

        // Mark the client as not connected
        isConnected = false;

        // Reset the state
        resetState();
    }

    // Request an identity from the server
    public void requestIdentity() {
        // Wait for the last channel future to finish if need be
        if (lastChannelFuture != null) {
            if (!lastChannelFuture.isDone()) {
                try {
                    lastChannelFuture.sync();
                } catch (InterruptedException ex) {}
            }
        }

        // Make a buffer with the request identity byte
        ByteBuf buf = ch.alloc().buffer(Settings.PACKET_SIZE_REQUEST_IDENTITY_CLIENT);
        buf.writeByte(Settings.PACKET_HEADER_REQUEST_IDENITY);

        this.lastChannelFuture = ch.writeAndFlush(buf);
    }

    // Request the board state from the server
    public void requestBoardState() {
        // Wait for the last channel future to finish if need be
        if (lastChannelFuture != null) {
            if (!lastChannelFuture.isDone()) {
                try {
                    lastChannelFuture.sync();
                } catch (InterruptedException ex) {}
            }
        }

        // Make a buffer with the request board state byte
        ByteBuf buf = ch.alloc().buffer(Settings.PACKET_SIZE_REQUEST_BOARD_STATE_CLIENT);
        buf.writeByte(Settings.PACKET_HEADER_REQUEST_BOARD_STATE);

        this.lastChannelFuture = ch.writeAndFlush(buf);
    }

    // Send a tile click to the server
    // outerCoord should refer to the subboard that the tile is on (0-8, inclusive)
    // innerCoord should refer to the exact tile (0-8, inclusive)
    public void sendButtonPress(int outerCoord, int innerCoord) {
        // Wait for the last channel future to finish if need be
        if (lastChannelFuture != null) {
            if (!lastChannelFuture.isDone()) {
                try {
                    lastChannelFuture.sync();
                } catch (InterruptedException ex) {}
            }
        }

        // Validate outerCoord and innerCoord
        if (outerCoord < 0 || outerCoord > 8) {
            throw new IllegalArgumentException("outerCoord must be between 0 and 8");
        }
        if (innerCoord < 0 || innerCoord > 8) {
            throw new IllegalArgumentException("innerCoord must be between 0 and 8");
        }

        // Create a buffer for the request
        ByteBuf buf = ch.alloc().buffer(Settings.PACKET_SIZE_SEND_BUTTON_PRESS_CLIENT);

        // Write the packet header
        buf.writeByte(Settings.PACKET_HEADER_SEND_BUTTON_PRESS);

        // Write the client's identity
        buf.writeByte(this.identity);

        // Write the current time
        buf.writeInt(getCurTime());

        // Write the outer and inner coordinate
        buf.writeByte(outerCoord);
        buf.writeByte(innerCoord);

        // Send the buffer to the server
        this.lastChannelFuture = ch.writeAndFlush(buf);
    }

    // Request that the board be reset
    public void requestReset() {
        // Wait for the last channel future to finish if need be
        if (lastChannelFuture != null) {
            if (!lastChannelFuture.isDone()) {
                try {
                    lastChannelFuture.sync();
                } catch (InterruptedException ex) {}
            }
        }

        // Create a buffer with the correct header
        ByteBuf buf = ch.alloc().buffer(Settings.PACKET_SIZE_RESET_GAME_CLIENT);
        buf.writeByte(Settings.PACKET_HEADER_RESET_GAME);

        this.lastChannelFuture = ch.writeAndFlush(buf);
    }

    // Change the client's identity
    // Should only be called by the InboundClientHandler
    public void setIdentity(byte identity) {
        this.identity = identity;
    }

    // Get the client's identity
    // This can be called by anything
    public byte getIdentity() {
        return this.identity;
    }

    // Set whose turn it is
    // This should only be called by the InboundClientHandler
    public void setCurrentTurn(byte curTurn) {
        this.curTurn = curTurn;
    }

    // Get the current turn
    public byte getCurrentTurn() {
        return this.curTurn;
    }

    // Change the client's win state
    // Should only be called by the InboundClientHandler
    public void setWinner(byte winner) {
        this.winner = winner;
    }

    // Get the client's win state
    // This can safely be called by anything
    public byte getWinner() {
        return this.winner;
    }

    // Change the client's subboard winners
    // Should only be called by the InboundClientHandler
    public void setSubBoardWinners(byte[] winners) {
        this.subBoardWinners = winners;
    }

    // Get the client's subboard winners
    // Can be called by anything
    public byte[] getSubBoardWinners() {
        return this.subBoardWinners;
    }

    // Change the client's board
    // Should only be called by the InboundClientHandler
    public void setBoard(byte[][] board) {
        this.superBoard = board;
    }

    // Get the client's board
    // This can safely be called by anything
    public byte[][] getBoard() {
        return this.superBoard;
    }

    // Set the client's required superboard
    // This should only be called by the InboundClientHandler
    public void setRequiredSubBoard(byte subboard) {
        this.requiredSubBoard = subboard;
    }

    // Get the client's required subboard
    // This is safe for anything to call
    public byte getRequiredSubBoard() {
        return this.requiredSubBoard;
    }

    // Set the time that the board was created
    // Should only be called by the InboundClientHandler
    public void setTimeBoardCreated(int newTime) {
        this.timeBoardCreated = newTime;
    }

    public int getTimeBoardCreated() {
        return this.timeBoardCreated;
    }

    // Set the time that the board was last updated
    // Can only be safely called by the InboundClientHandler
    public void setTimeLastUpdated(int newTime) {
        this.timeLastUpdated = newTime;
    }

    public int getTimeLastUpdated() {
        return this.timeLastUpdated;
    }

    // Used by the InboundClientHanlder to set when the board has changed
    //   and by the GUI to set when that change has been read
    public void setBoardUpdated(boolean b) {
        this.boardUpdated = b;
    }

    // Call this from anywhere
    public boolean getBoardUpdated() {
        return this.boardUpdated;
    }

    // Check if the client is connected
    public boolean isConnected() {
        return this.isConnected;
    }

    // Reset the game
    public void resetGame() {
        // Reset the turn
        curTurn = Settings.IDENTITY_X;

        // Reset the superboard and subboard winners
        winner = Settings.BOARD_WINNER_NULL;
        subBoardWinners = new byte[9];

        // Reset the superboard
        superBoard = new byte[9][9];

        // Reset the required subboard to wild
        requiredSubBoard = 9;
    }

    // Reset the state of the client
    // THIS WILL INVALIDATE THE HOST AND PORT
    // TRYING TO CONNECT BEFORE SETTING THE HOST AND PORT WILL ERROR
    private void resetState() {
        host = null;
        port = -1;

        ch = null;
        group = null;

        identity = Settings.IDENTITY_UNASSIGNED;

        // TODO: should the client handlers be nulled out?
        // Or should their state be reset
        // Or nothing at all?
    }

    // Get the current time in seconds
    private int getCurTime() {
        // This could lead to an integer overflow in a few decades, but I won't be in this class then
        return (int)(System.currentTimeMillis() / 1000L);
    }

    public static void main(String[] args) throws InterruptedException{
        // Init the client
        Client client = new Client("localhost", Settings.DEFAULT_PORT);

        // Connect to the client
        client.connect();

        // Request an identity
        client.requestIdentity();

        // Wait for the identity to be updated
        while(client.getIdentity() == Settings.IDENTITY_UNASSIGNED) {
            Thread.sleep(250);
        }

        // Spin up a thread to request the board state
        new Thread(() -> {
            while (client.isConnected) {
                // Print the board state
                System.out.println("*******************");
                System.out.printf("Winner: %s\n", client.winner);
                System.out.printf("Current turn: %s\n", client.curTurn);
                for (int i = 0; i < client.superBoard.length; i++) {
                    System.out.printf("Board %d:\n", i);
                    System.out.printf("  Won by: %s\n", client.subBoardWinners[i]);
                    System.out.printf("  State: %s\n", Arrays.toString(client.superBoard[i]));
                }
                System.out.println("*******************");

                client.requestBoardState();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {}
            }
        }).start();

        // DEBUG
        // Spin up a new thread to test sending clicks
        /* new Thread(() -> {
            while (client.isConnected) {
                // Send a request for the same tile over and over again
                client.sendButtonPress(0, 0);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {}
            }
        }).start(); */

        // Send 10 button presses with a random delay between them
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(rand.nextInt(1000));
            } catch (InterruptedException ex) {}

            client.sendButtonPress(rand.nextInt(9), rand.nextInt(9));
        }

        client.disconnect();
    }
}
