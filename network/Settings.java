package network;

// This is a class that should only include constants that need to be consistent across files
// TODO: consider enums. How to convert enum to byte value?
public class Settings {
    // The default port for the server to listen on and for the client to connect to
    public static final int DEFAULT_PORT = 8080;
    
    // Integer values for identities
    public static final byte IDENTITY_UNASSIGNED = 0;
    public static final byte IDENTITY_X = 1;
    public static final byte IDENTITY_O = 2;
    public static final byte IDENTITY_GAME_FULL = 3;

    // Packet header values
    public static final byte PACKET_HEADER_REQUEST_IDENITY = 0;
    public static final byte PACKET_HEADER_REQUEST_BOARD_STATE = 1;
    public static final byte PACKET_HEADER_SEND_BUTTON_PRESS = 2;
    public static final byte PACKET_HEADER_RESET_GAME = 3;

    // Size of packets for the client and server
    public static final byte PACKET_SIZE_REQUEST_IDENTITY_CLIENT = 1;
    public static final byte PACKET_SIZE_REQUEST_IDENTITY_SERVER = 2;
    public static final byte PACKET_SIZE_REQUEST_BOARD_STATE_CLIENT = 1;
    public static final byte PACKET_SIZE_REQUEST_BOARD_STATE_SERVER = 102;
    public static final byte PACKET_SIZE_SEND_BUTTON_PRESS_CLIENT = 8;
    public static final byte PACKET_SIZE_SEND_BUTTON_PRESS_SERVER = 2;
    public static final byte PACKET_SIZE_RESET_GAME_CLIENT = 1;
    public static final byte PACKET_SIZE_RESET_GAME_SERVER = 92;

    // Victory values
    public static final byte BOARD_WINNER_NULL = 0;
    public static final byte BOARD_WINNER_X = 1;
    public static final byte BOARD_WINNER_O = 2;
    public static final byte BOARD_WINNER_DRAW = 3;

    // Error values
    public static final byte ERROR_VALUE_ACCEPTED = 0;
    public static final byte ERROR_WRONG_TURN = 1;
    public static final byte ERROR_INVALID_SUBBOARD = 2;
    public static final byte ERROR_INVALID_TILE = 3;
}
