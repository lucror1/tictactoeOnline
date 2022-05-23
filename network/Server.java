package network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    // Keep track of if the server is listing
    private boolean isListening;

    // Keep track of who is connected
    private boolean isXConnected = false;
    private boolean isOConnected = false;

    // Store the board
    private byte[][] superBoard = new byte[9][9];

    // Store the subboard that the player has to play on
    // 9 is wild
    private int requiredSubBoard = 9;

    // Keep track of the last time that the board was created and updated
    private int timeBoardCreated;
    private int timeLastUpdated;

    // The current turn
    private byte currentTurn = Settings.IDENTITY_X;

    public Server(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is invalid");
        }

        this.port = port;

        // Set the last updated time to right now
        // This could lead to an integer overflow in a few decades, but I won't be in this class then
        this.timeBoardCreated = (int)(System.currentTimeMillis() / 1000L);
        this.timeLastUpdated = timeBoardCreated;
    }

    public Server() {
        this(Settings.DEFAULT_PORT);
    }

    public void listen() throws InterruptedException {
        // Make sure that the server is not already listening
        if (isListening) {
            throw new IllegalStateException("Cannot listen while already listening");
        }

        // Get a reference for the server so it can be passed into the server handlers
        Server s = this;

        // Start up the server
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(this.bossGroup, this.workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new InboundServerHandler(s));
            }
        });

        // Start listening
        b.bind(port).sync();

        // Mark the server as listening
        isListening = true;
    }

    public void disconnect() throws InterruptedException {
        if (!isListening) {
            throw new IllegalStateException("Cannot stop listening if not listening");
        }

        // Sleep a bit to make sure that things really are finished
        // Sometimes the message isn't actually finished sending, even after syncing
        Thread.sleep(100);

        // Shutdown the boss and worker groups
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // Mark the server as not listening
        isListening = false;

        // Reset the state
        resetState();
    }

    public boolean getIsXConnected() {
        return isXConnected;
    }

    public void setIsXConnected(boolean isConnected) {
        isXConnected = isConnected;
    }

    public boolean getIsOConnected() {
        return isOConnected;
    }

    public void setIsOConnected(boolean isConnected) {
        isOConnected = isConnected;
    }

    public void setTimeLastUpdated(int newTime) {
        this.timeLastUpdated = newTime;
    }

    public int getTimeLastUpdated() {
        return timeLastUpdated;
    }

    public void setTimeBoardCreated(int newTime) {
        this.timeBoardCreated = newTime;
    }

    public int getTimeBoardCreated() {
        return this.timeBoardCreated;
    }

    public void setCurrentTurn(byte turn) {
        this.currentTurn = turn;
    }

    public void flipCurrentTurn() {
        currentTurn = currentTurn == Settings.IDENTITY_X ? Settings.IDENTITY_O : Settings.IDENTITY_X;
    }

    public byte getCurrentTurn() {
        return this.currentTurn;
    }

    public byte[][] getSuperBoard() {
        return superBoard;
    }

    public void setTile(int outerCoord, int innerCoord, byte value) {
        this.superBoard[outerCoord][innerCoord] = value;
    }

    public void setRequiredSubBoard(int board) {
        this.requiredSubBoard = board;
    }

    public int getRequiredSubBoard() {
        return this.requiredSubBoard;
    }

    public void resetGame() {
        this.superBoard = new byte[9][9];
        this.currentTurn = Settings.IDENTITY_X;
    }

    private void resetState() {
        port = -1;

        bossGroup = null;
        workerGroup = null;
    }

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        server.listen();
    }
}
