package network;

import java.net.SocketException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InboundServerHandler extends ChannelInboundHandlerAdapter {
    // Keep a reference to the server
    private Server server;

    // Keep track of this connection's identity
    private byte identity = Settings.IDENTITY_UNASSIGNED;

    public InboundServerHandler(Server server) {
        this.server = server;
    }

    // This is currently a dummy method that always returns true
    // In the future, it should return true if it is safe to disconnect
    // I.e the most recent ChannelFuture has finished
    public boolean isSafeToDisconnect() {
        return true;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //System.out.println("channelActive");

        // Get a reference to this so it can be referenced in the close future
        InboundServerHandler h = this;

        // Set up some clean up for when the channel closes
        ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                // Set the flag for the correct identity in the server
                switch (h.identity) {
                    case Settings.IDENTITY_X:
                        server.setIsXConnected(false);
                        break;
                    case Settings.IDENTITY_O:
                        server.setIsOConnected(false);
                        break;
                }

                // Reset this identity
                h.identity = Settings.IDENTITY_UNASSIGNED;
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        //System.out.println("channelRead");
        
        // Switch on the first byte
        switch (buf.readByte()) {
            case Settings.PACKET_HEADER_REQUEST_IDENITY:
                // Handle the identity request
                handleIdentityRequest(ctx, buf);
                return;
            case Settings.PACKET_HEADER_REQUEST_BOARD_STATE:
                // Handle the board state request
                handleBoardStateRequest(ctx, buf);
                return;
            case Settings.PACKET_HEADER_SEND_BUTTON_PRESS:
                // Handle the button press request
                handleSendButtonPress(ctx, buf);
                return;
            case Settings.PACKET_HEADER_RESET_GAME:
                // Handle reset request
                handleResetRequest(ctx, buf);
                return;
        }
    }

    private void handleIdentityRequest(ChannelHandlerContext ctx, ByteBuf buf) {
        // Create a buffer for a response
        ByteBuf response = ctx.alloc().buffer(Settings.PACKET_SIZE_REQUEST_IDENTITY_SERVER);

        // Write the response header
        response.writeByte(Settings.PACKET_HEADER_REQUEST_IDENITY);

        // If X is available, then tell the client to be X
        if (!server.getIsXConnected()) {
            this.identity = Settings.IDENTITY_X;
            server.setIsXConnected(true);
        }

        // If O is available, then assign that
        else if (!server.getIsOConnected()) {
            this.identity = Settings.IDENTITY_O;
            server.setIsOConnected(true);
        }

        // If all else fails, assign the game full identity
        else {
            this.identity = Settings.IDENTITY_GAME_FULL;
        }

        // Write the identity to the buffer
        response.writeByte(this.identity);

        // Write the response
        ctx.writeAndFlush(response);
    }

    private void handleBoardStateRequest(ChannelHandlerContext ctx, ByteBuf buf) {
        // Create a buffer for the response
        ByteBuf response = ctx.alloc().buffer(Settings.PACKET_SIZE_REQUEST_BOARD_STATE_SERVER);

        // Write the response header
        response.writeByte(Settings.PACKET_HEADER_REQUEST_BOARD_STATE);

        // Write the timestamp for when the board was created
        response.writeInt(server.getTimeBoardCreated());

        // Write the timestamp for when the board was last updated
        response.writeInt(server.getTimeLastUpdated());

        // Write the current turn
        response.writeByte(server.getCurrentTurn());

        // Write the required subboard
        response.writeByte(server.getRequiredSubBoard());

        // Write the board as a byte buf
        response.writeBytes(superBoardToByteBuf(server.getSuperBoard()));

        // Write the response
        ctx.writeAndFlush(response);
    }

    private void handleSendButtonPress(ChannelHandlerContext ctx, ByteBuf buf) {
        // Create a buffer for the response with the proper header
        ByteBuf response = ctx.alloc().buffer(Settings.PACKET_SIZE_SEND_BUTTON_PRESS_SERVER);
        response.writeByte(Settings.PACKET_HEADER_SEND_BUTTON_PRESS);

        // Read all the packet info in
        byte userIdentity = buf.readByte();
        int time = buf.readInt();
        byte outerCoord = buf.readByte();
        byte innerCoord = buf.readByte();

        // Make sure the provided identity is the one for the current turn
        if (userIdentity != server.getCurrentTurn()) {
            // If not, write the appropriate error code
            response.writeByte(Settings.ERROR_WRONG_TURN);
        }

        // Make sure that the subboard is correct
        else if (outerCoord != server.getRequiredSubBoard() && server.getRequiredSubBoard() != 9) {
            response.writeByte(Settings.ERROR_INVALID_SUBBOARD);
        }

        // Check if the given tile is empty
        // If not, write the correct error and send it back
        else if (server.getSuperBoard()[outerCoord][innerCoord] != Settings.BOARD_WINNER_NULL) {
            response.writeByte(Settings.ERROR_INVALID_TILE);
        }
        
        // If this point is reached, everything is okay
        // Write no error
        else {
            System.out.println("Value accepted");
            System.out.println(outerCoord);
            System.out.println(server.getRequiredSubBoard());

            response.writeByte(Settings.ERROR_VALUE_ACCEPTED);

            // Set the corresponding tile in the server's board
            server.setTile(outerCoord, innerCoord, userIdentity);

            // Also flip the turn
            server.flipCurrentTurn();

            // Update the time, but only if it is newer
            // If a client sends a old timestamp, it should still be accepted
            //   so long as it does not conflict with the current board
            if (server.getTimeLastUpdated() < time) {
                server.setTimeLastUpdated(time);
            }

            // Get the new required subboard
            // Assume that it is just the inner coordinate
            int requiredSubBoard = innerCoord;

            // If the inner coordinate points towards a board that has a winner or is drawn,
            //   set the required subboard to wild
            if (checkBoardForWinner(server.getSuperBoard()[innerCoord]) != Settings.BOARD_WINNER_NULL) {
                requiredSubBoard = 9;
            }

            // Update the server's required subboard
            server.setRequiredSubBoard(requiredSubBoard);
        }

        // Send the response to the client
        ctx.writeAndFlush(response);
    }

    private void handleResetRequest(ChannelHandlerContext ctx, ByteBuf buf) {
        // Reset the game
        server.resetGame();

        // Create a response buffer with the correct header
        ByteBuf response = ctx.alloc().buffer(Settings.PACKET_SIZE_RESET_GAME_SERVER);
        response.writeByte(Settings.PACKET_HEADER_RESET_GAME);

        // Write the new board state
        response.writeBytes(superBoardToByteBuf(server.getSuperBoard()));

        // Send the response to the client
        ctx.writeAndFlush(response);

        // On a reset update the time the board was created
        server.setTimeBoardCreated(getCurTime());
    }

    // Convert a super board into a ByteBuf
    // The super board is the whole board, which is made of 9 sub-boards
    // Board is a 9x9 array where each row is a sub-board
    //   and each value in each row is a tile
    // The rows and values should be organized to the first
    //   value is the top left, the second value is the top middle, etc.
    // Ex.
    // 1|2|3
    // -----
    // 4|5|6 -> [f, 1,2,3,4,5,6,7,8,9] for one sub-board (where f is the win flag)
    // -----
    // 7|8|9
    private ByteBuf superBoardToByteBuf(byte[][] superBoard) {
        // Validate the size of board
        if (superBoard.length != 9) {
            throw new IllegalArgumentException("board is the wrong size (should be 9x9)");
        }
        
        // Create a byte buf to hold the bytes
        ByteBuf buf = Unpooled.buffer(91);

        // Store who won each subboard
        byte[] subboardWinners = new byte[9];

        // Iterate over each subboard
        for (int i = 0; i < superBoard.length; i++) {
            // Find who won the subboard
            subboardWinners[i] = checkBoardForWinner(superBoard[i]);
        }

        // Use the subboardWinners to find the superboard winner
        // Write that as the first byte in the output buffer
        buf.writeByte(checkBoardForWinner(subboardWinners));

        // Loop over each subboard and write its info
        for (int i = 0; i < superBoard.length; i++) {
            // Write the win condition of the ith board
            buf.writeByte(subboardWinners[i]);

            // Write the subboard itself
            buf.writeBytes(superBoard[i]);
        }

        return buf;
    }

    // Check a board for a winner
    // The board should be a 1d array where the first value
    //   is the top left value of the board, the second value is the top middle, etc.
    private byte checkBoardForWinner(byte[] board) {
        // Verify that the board is the right length
        if (board.length != 9) {
            throw new IllegalArgumentException("board is the wrong size");
        }

        // Keep track of the winner
        byte winner = Settings.BOARD_WINNER_NULL;

        // Check each row
        for (int i = 0; i < 3; i++) {
            // A row is winning if all the values are the same and have been played on
            if (board[3*i] != Settings.BOARD_WINNER_NULL && board[3*i] != Settings.BOARD_WINNER_DRAW &&
                board[3*i] == board[3*i+1] && board[3*i] == board[3*i+2]) {
                
                winner = board[3*i];
                break;
            }
        }

        // Only check columns if no row has been found
        if (winner == Settings.BOARD_WINNER_NULL) {
            for (int i = 0; i < 3; i++) {
                if (board[i] != Settings.BOARD_WINNER_NULL && board[i] != Settings.BOARD_WINNER_DRAW &&
                    board[i] == board[i+3] && board[i] == board[i+6]) {
                    
                    winner = board[i];
                    break;
                }
            }
        }

        // Only check the diagonals if no row or column has been found
        if (winner == Settings.BOARD_WINNER_NULL) {
            // Top left to bottom right diagonal
            if (board[0] != Settings.BOARD_WINNER_NULL && board[0] != Settings.BOARD_WINNER_DRAW &&
                board[0] == board[4] && board[0] == board[8]) {
                
                winner = board[0];
            }
            // Top right to bottom left diagonal
            else if (board[2] != Settings.BOARD_WINNER_NULL && board[2] != Settings.BOARD_WINNER_DRAW &&
                     board[2] == board[4] && board[2] == board[6]) {

                winner = board[2];
            }
        }

        // If no winner has been found at this point, check if
        //   any empty tiles still exist
        // If none exist, then the board is a draw
        if (winner == Settings.BOARD_WINNER_NULL) {
            boolean emptyFound = false;
            for (int i = 0; i < 9; i++) {
                if (board[i] == Settings.BOARD_WINNER_NULL) {
                    emptyFound = true;
                    break;
                }
            }

            // If an empty was not found, set the board as a tie
            if (!emptyFound) {
                winner = Settings.BOARD_WINNER_DRAW;
            }
        }

        return winner;
    }

    // Get the current time in seconds
    private int getCurTime() {
        return (int)(System.currentTimeMillis() / 1000L);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            System.out.println("[*] The client has closed the connection.");
        }
        else {
            //System.out.println("[*] An unknown exception has occured.");
            cause.printStackTrace();
        }
    }
}
