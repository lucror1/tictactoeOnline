# Types of packets

* Connection
  * C: request identity from server (X or O)
  * S: respond with client identity or null byte if the game is full
* Request board state
  * C: request board state from server
  * S: last updated time + victory flag + respond with state
* Send a button press
  * C: send identity, outer and inner board location, and current time
  * S: whether the move was valid and was accepted by the server
* Reset game
  * C: request a reset from the server
  * S: send the reset board state

## Connection packet

### Client

* Header (1 byte):
  * Must be 0

### Server

* Header (1 byte):
  * Must be 0
* Identity (1 byte)
  * 1 - X identity
  * 2 - O identity
  * Any other value - game full

## Request board state

### Client

* Header (1 byte):
  * Must be 1

### Server

* Header (1 byte):
  * Must be 1
* Timestamp (4 bytes)
  * This should be the time when the board was last updated
  * The value should be a 32-bit integer
* Victory flag (1 byte)
  * 0 - no one has won the entire game
  * 1 - X has won the entire game
  * 2 - O has won the entire game
  * Any other value - the entire game was a draw
* Board state (90 bytes)
  * Each group of 10 bytes is 1 sub-board
    * The boards are ordered from left to right, top to bottom
    * Ex. the bottom center board should have a value of 7
  * The first byte in each sub-board represents the state of the whole board
    * 0 - no one has won the sub-board yet
    * 1 - X has won the sub-board
    * 2 - O has won the sub-board
    * Any other value - the sub-board was a draw
  * The remaining 9 bytes represent each tile in the sub-board
    * The tiles are ordered from left to right, top to bottom
      * Ex. the bottom center board should have a value of 7
    * 1 - X has played at that position
    * 2 - O has played at that position
    * Any other value - no player has played at that position

## Send a button press

### Client

* Header (1 byte)
  * Must be 2
* Identity (1 byte)
* Timestamp (4 bytes)
  * This should be the time when the packet was sent
  * The value should be a 32-bit integer
* Outer board location (1 byte)
  * Must be between 0 and 8, inclusive
  * The boards are ordered from top to bottom, left to right
    * Ex. the bottom center board should have a value of 7
* Inner board location (1 byte)
  * Must be between 0 and 8, inclusive
  * The boards are ordered from top to bottom, left to right
    * Ex. the bottom center board should have a value of 7

### Server

* Header (1 byte)
  * Must be 2
* Accepted flag (1 byte)
  * 1 - the value was accepted
  * 2 - it is not your turn
  * 3 - the given tile was not in a valid sub-board
  * 4 - the given tile was already played on
  * Any other value - an unspecified error occured

## Reset game

### Client

* Header (1 byte)
  * Must be 3
* Identity (1 byte)

### Server
* Header (1 byte)
  * Must be 3
* Board state (90 bytes)
  * Each group of 10 bytes is 1 sub-board
    * The boards are ordered from left to right, top to bottom
    * Ex. the bottom center board should have a value of 7
  * The first byte in each sub-board represents the state of the whole board
    * 0 - no one has won the sub-board yet
    * 1 - X has won the sub-board
    * 2 - O has won the sub-board
    * Any other value - the sub-board was a draw
  * The remaining 9 bytes represent each tile in the sub-board
    * The tiles are ordered from left to right, top to bottom
      * Ex. the bottom center board should have a value of 7
    * 1 - X has played at that position
    * 2 - O has played at that position
    * Any other value - no player has played at that position