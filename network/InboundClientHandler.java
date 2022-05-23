package network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InboundClientHandler extends ChannelInboundHandlerAdapter {
    // Keep a reference to the client
    private Client client;

    public InboundClientHandler(Client client) {
        this.client = client;
    }

    // This is currently a dummy method that always returns true
    // In the future, it should return true if it is safe to disconnect
    // I.e the most recent ChannelFuture has finished
    public boolean isSafeToDisconnect() {
        return true;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //System.out.println("channelActive.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        //System.out.println("chanelRead.");

        // Switch on the first byte of buf
        switch (buf.readByte()) {
            case Settings.PACKET_HEADER_REQUEST_IDENITY:
                // Handle the response for an identity
                handleIdentityResponse(ctx, buf);
                break;
            case Settings.PACKET_HEADER_REQUEST_BOARD_STATE:
                // Handle the new board state
                handleBoardStateResponse(ctx, buf);
                break;
            case Settings.PACKET_HEADER_SEND_BUTTON_PRESS:
                // Handle the error message
                handleSendButtonPressResposne(ctx, buf);
                break;
            case Settings.PACKET_HEADER_RESET_GAME:
                // Handle the new board state
                break;
        }
    }

    private void handleIdentityResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        // Read the identity in
        // The first byte has already been consumed by the switch statement
        byte identity = buf.readByte();

        // Set the client's identity
        client.setIdentity(identity);

        // DEBUG: Print the identity
        System.out.print("Client identity: ");
        switch (identity) {
            case Settings.IDENTITY_UNASSIGNED:
                System.out.println("U");
                break;
            case Settings.IDENTITY_X:
                System.out.println("X");
                break;
            case Settings.IDENTITY_O:
                System.out.println("O");
                break;
            case Settings.IDENTITY_GAME_FULL:
                System.out.println("F");
                break;
        }
    }

    private void handleBoardStateResponse(ChannelHandlerContext ctx, ByteBuf buf) {
        // Read the creation and update time stamp
        int timeBoardCreated = buf.readInt();
        int timeLastUpdated = buf.readInt();

        // If the board was created more recently than the clients, automatically
        //   accept it, even if the timeLastUpdated is different
        if (timeBoardCreated > client.getTimeBoardCreated()) {
            // Update the creation and update time
            client.setTimeBoardCreated(timeBoardCreated);
            client.setTimeLastUpdated(timeBoardCreated);
        }

        // If the time is from before the last time that the client updated the board, ignore it
        else if (timeLastUpdated < client.getTimeLastUpdated()) {
            System.out.println("too old");
            System.out.println(client.getTimeLastUpdated());
            System.out.println(timeLastUpdated);
            return;
        }

        // If it is newer, update the client's last updated time
        else {
            client.setTimeLastUpdated(timeLastUpdated);
        }

        // Read the current turn
        byte curTurn = buf.readByte();

        // Read the required subboard
        byte requiredSubBoard = buf.readByte();

        // Read the superboard winner
        byte winner = buf.readByte();

        // Read the board state into the client
        byte[][] newBoard = new byte[9][9];
        byte[] subBoardWinners = new byte[9];
        
        // Outer read for each board
        for (int i = 0; i < 9; i++) {
            // Read the winner flag
            subBoardWinners[i] = buf.readByte();

            // Inner read for each tile
            for (int j = 0; j < 9; j++) {
                newBoard[i][j] = buf.readByte();
            }
        }

        /* System.out.println("***");
        for (byte[] b : newBoard) {
            System.out.println(Arrays.toString(b));
        }
        System.out.println("***"); */

        // Write all the information to the client
        client.setWinner(winner);
        client.setCurrentTurn(curTurn);
        client.setRequiredSubBoard(requiredSubBoard);
        client.setSubBoardWinners(subBoardWinners);
        client.setBoard(newBoard);
    }

    private void handleSendButtonPressResposne(ChannelHandlerContext ctx, ByteBuf buf) {
        // Read the error code
        byte errorCode = buf.readByte();

        // TODO: switch on the byte to decide how to handle the error code
        switch (errorCode) {
            case Settings.ERROR_VALUE_ACCEPTED:
                System.out.println("Value accepted");
                break;
            case Settings.ERROR_WRONG_TURN:
                System.out.println("Wrong turn");
                break;
            case Settings.ERROR_INVALID_SUBBOARD:
                System.out.println("Invalid subboard");
                break;
            case Settings.ERROR_INVALID_TILE:
                System.out.println("Invalid tile");
                break;
        }
    }


}
