import java.util.ArrayList;
import java.util.Optional;

import io.netty.channel.sctp.SctpNotificationHandler;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import network.*;
import javafx.scene.control.TextArea;

public class Utictactoe extends Application
{
    // Store the server and port arguments if they were provided
    private static String host = "localhost";
    private static int port = Settings.DEFAULT_PORT;

    private ArrayList<ArrayList<Integer>> winList;
    private GridPane BigGrid;
    private int gridPlayed;
    private ArrayList<GridPane> smallGrids;
    public boolean mover;
    public boolean freeze;
    private BorderPane bp;
    private ArrayList<ArrayList<ArrayList<Boolean>>> currentgrid;

    private Client client;
    private boolean winBoxOpen;

    public Utictactoe()
    {
        // Networking stuff
        this.client = new Client(host, port);

        // Set up networking threads
        networkInit();

        winList=new ArrayList<ArrayList<Integer>>(); // Stores who won what subboard
        currentgrid = new ArrayList<ArrayList<ArrayList<Boolean>>>(); // Flattened superboard, 3x3 subboard
        gridPlayed = 9; // Required subboard
        smallGrids = new ArrayList<GridPane>(); // The subboards
        bp = new BorderPane(); // Parent object
        freeze=false;
        BigGrid = new GridPane(); // Center object, holds each of the 9 smaller grids
        mover=true; // Which player is playing
        winList.add(new ArrayList<Integer>());
        winList.add(new ArrayList<Integer>());
        winList.add(new ArrayList<Integer>());
        for(int i =0; i<9; i++)
        {
            winList.get(i%3).add(0);
        }
    }
    public static void main(String[] args) throws Exception {
        // If arguments were provided, use them for the server
        if (args.length >= 1) {
            host = args[0];
        }

        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException ex) {
                System.out.println("Invalid port.");
                return;
            }
            if (port < 1 || port > 65535) {
                System.out.println("Invalid port.");
            }
        }


        launch(args);
    }
    
    @Override
    public void start(Stage primary)
    {
        makegrid();
        bp.setCenter(BigGrid);
        Scene scene = new Scene(bp, 800, 500);
        scene.getStylesheets().add("stylesheet.css");
        primary.setScene(scene);
        primary.show();
    }
    private void makegrid()
    {
        for(int i = 0 ; i<9; i++)
        {
            currentgrid.add(new ArrayList<ArrayList<Boolean>>());
            smallGrids.add(new GridPane());
        }
        for(int i = 0; i<9;i++)   
        {
            currentgrid.get(i).add(new ArrayList<Boolean>());
            currentgrid.get(i).add(new ArrayList<Boolean>());
            currentgrid.get(i).add(new ArrayList<Boolean>());
            for(int j = 0; j<9;j++)
            {
                currentgrid.get(i).get(j%3).add(null);
                tile x = new tile("", j%3, j/3, false, i );
                smallGrids.get(i).add(x, j%3, j/3);
            }
            smallGrids.get(i).setHgap(5); 
            smallGrids.get(i).setVgap(5); 
        }
        for(int i= 0; i <9; i++)
        {
            BigGrid.add(smallGrids.get(i), i%3, i/3);
        }
        BigGrid.setHgap(20); 
        BigGrid.setVgap(20);
    }
    // This is for the super-board
    public void check2()
    {
        int winner =0;
        ArrayList<Integer> xWins= new ArrayList<Integer>();
        xWins.add(1);
        xWins.add(1);
        xWins.add(1);
        ArrayList<Integer> oWins= new ArrayList<Integer>();
        oWins.add(2);
        oWins.add(2);
        oWins.add(2);
        if(!winList.get(0).contains(0)&&!winList.get(1).contains(0)&&!winList.get(2).contains(0))
        {
            winner = 3;
            freeze = true;
        }
        for(int i =0; i<3; i++)
        {
            if (winList.get(i).equals(xWins)==true)
            {
                winner =1;
                freeze = true;
            }
            if (winList.get(i).equals(oWins)==true)
            {
                winner = 2;
                freeze = true;
            }
        }
        for(int i =0; i<3; i++)
        {
            ArrayList<Integer> col= new ArrayList<Integer>();
            col.add(winList.get(0).get(i));
            col.add(winList.get(1).get(i));
            col.add(winList.get(2).get(i));
            if (col.equals(xWins)==true)
            {
                winner = 1;
                freeze = true;
            }
            if (col.equals(oWins)==true)
            {
                winner =2;
                freeze = true;
            }
        }
        ArrayList<Integer> diag1= new ArrayList<Integer>();
        diag1.add(winList.get(0).get(0));
        diag1.add(winList.get(1).get(1));
        diag1.add(winList.get(2).get(2));
        if (diag1.equals(xWins)==true)
        {
            winner =1;
            freeze = true;
        }
        if (diag1.equals(oWins)==true)
        {
            winner =2;
            freeze = true;
        }
        ArrayList<Integer> diag2= new ArrayList<Integer>();
        diag2.add(winList.get(0).get(2));
        diag2.add(winList.get(1).get(1));
        diag2.add(winList.get(2).get(0));
        if (diag2.equals(xWins)==true)
        {
            winner = 1;
            freeze = true;
        }
        if (diag2.equals(oWins)==true)
        {
            winner = 2;
            freeze = true;
        }
        if(winner == 1)
        {
            Text Win = new Text();
            String x="X Wins!";
            Win.setText(x);
            Win.setId("fancytext");
            bp.setRight(Win);
        }
        else if(winner == 2)
        {
            Text Win = new Text();
            String x="O Wins!";
            Win.setText(x);
            Win.setId("fancytext");
            bp.setRight(Win);
        }
        else if(winner == 3)
        {
            Text Win = new Text();
            String x="Draw";
            Win.setText(x);
            Win.setId("fancytext");
            bp.setRight(Win);
        }
        else{
            Text Win = new Text();
            String x="";
            Win.setText(x);
            Win.setId("fancytext");
            bp.setRight(Win);
        }
    }
    // This is for sub-boards
    public void check(int number)
    {
        int winner=0;
        ArrayList<ArrayList<Boolean>> t = currentgrid.get(number);
        ArrayList<Boolean> xWins= new ArrayList<Boolean>();
        xWins.add(true);
        xWins.add(true);
        xWins.add(true);
        ArrayList<Boolean> oWins= new ArrayList<Boolean>();
        oWins.add(false);
        oWins.add(false);
        oWins.add(false);
        if(!t.get(0).contains(null)&&!t.get(1).contains(null)&&!t.get(2).contains(null))
        {
            winner = 3;
            //freeze = true;
        }
        for(int i =0; i<3; i++)
        {
            if (t.get(i).equals(xWins)==true)
            {
                winner =1;
                //freeze = true;
            }
            if (t.get(i).equals(oWins)==true)
            {
                winner = 2;
                //freeze = true;
            }
        }
        for(int i =0; i<3; i++)
        {
            ArrayList<Boolean> col= new ArrayList<Boolean>();
            col.add(t.get(0).get(i));
            col.add(t.get(1).get(i));
            col.add(t.get(2).get(i));
            if (col.equals(xWins)==true)
            {
                winner = 1;
                //freeze = true;
            }
            if (col.equals(oWins)==true)
            {
                winner =2;
                //freeze = true;
            }
        }
        ArrayList<Boolean> diag1= new ArrayList<Boolean>();
        diag1.add(t.get(0).get(0));
        diag1.add(t.get(1).get(1));
        diag1.add(t.get(2).get(2));
        if (diag1.equals(xWins)==true)
        {
            winner =1;
            //freeze = true;
        }
        if (diag1.equals(oWins)==true)
        {
            winner =2;
            //freeze = true;
        }
        ArrayList<Boolean> diag2= new ArrayList<Boolean>();
        diag2.add(t.get(0).get(2));
        diag2.add(t.get(1).get(1));
        diag2.add(t.get(2).get(0));
        if (diag2.equals(xWins)==true)
        {
            winner = 1;
            //freeze = true;
        }
        if (diag2.equals(oWins)==true)
        {
            winner = 2;
            //freeze = true;
        }
        
        ObservableList<Node> childrens = BigGrid.getChildren();
        Node result = null;
        for (Node node : childrens) {
          if(GridPane.getRowIndex(node) == number/3 && GridPane.getColumnIndex(node) == number%3) {
             result = node;
            break;
          }
        }
        
        if(winner == 1)
        {
            winList.get(number%3).set(number/3,1);
            TextArea Win = new TextArea();
            String x="X";
            Win.setText(x);
            Win.setPrefHeight(160);
            Win.setPrefWidth(160);
            Win.setEditable(false);
            Win.setId("fancytext");
            BigGrid.getChildren().remove(result);
            BigGrid.add(Win,number%3,number/3);
            System.out.println(winList);
            winner=0;
        }
        else if(winner == 2)
        {
            winList.get(number%3).set(number/3,2);

            TextArea Win = new TextArea();
            String x="O";
            Win.setText(x);
            Win.setId("fancytext");
            Win.setPrefHeight(160);
            Win.setPrefWidth(160);
            Win.setEditable(false);
            BigGrid.getChildren().remove(result);
            BigGrid.add(Win,number%3,number/3);
            System.out.println(winList);
            winner=0;

        }
        else if(winner == 3)
        {
            winList.get(number%3).set(number/3,3);
            TextArea Win = new TextArea();
            String x="";
            Win.setText(x);
            Win.setPrefHeight(160);
            Win.setPrefWidth(160);
            Win.setEditable(false);
            Win.setId("fancytext");
            BigGrid.getChildren().remove(result);
            BigGrid.add(Win,number%3,number/3);
            System.out.println(winList);
            winner=0;

        }
        check2();
    }
    
    // Set up some threads for networking and updating the board
    private void networkInit() {
        // Connect to the server
        client.connect();

        // Request the identity and wait
        client.requestIdentity();
        while (client.getIdentity() == Settings.IDENTITY_UNASSIGNED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
        }

        // Have the client ping the server for the board every second
        new Thread(() -> {
            while (client.isConnected()) {
                client.requestBoardState();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {}
            }
        }).start();

        // Do some GUI centric tasks
        new Thread(() -> {
            while (client.isConnected()) {
                Platform.runLater(() -> {
                    // If frozen, don't do anything
                    if (freeze) {
                        return;
                    }

                    // If the client has a newer version of the board, use it
                    if (client.getBoardUpdated()) {
                        bp.setCenter(gridConvert());
                        client.setBoardUpdated(false);
                    }

                    // If the game is won, do the win thing
                    // Make sure to not spam this by using the winBoxOpen flag
                    if (client.getWinner() != Settings.BOARD_WINNER_NULL && !winBoxOpen) {
                        freeze = true;
                        makeWinBox();
                    }

                });
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {}
            }
        }).start();
    }

    // Convert Clients byte[][] representation of the board into the GUI's version
    // TODO: make the size constant (i.e. make text take same space as board)
    private GridPane gridConvert() {
        GridPane outputBoard = new GridPane();

        // Get which subboards have been won and the superboard from the client
        byte[][] superBoard = client.getBoard();
        byte[] subBoardWinners = client.getSubBoardWinners();

        // Add each subboard to the output
        for (int i = 0; i < 9; i++) {
            // If the subboard isn't won, add the subboard
            if (subBoardWinners[i] == Settings.BOARD_WINNER_NULL) {
                GridPane subboard = new GridPane();
                for (int j = 0; j < 9; j++) {
                    String tileString;
                    boolean clicked;
                    switch (superBoard[i][j]) {
                        case Settings.BOARD_WINNER_X:
                            tileString = "X";
                            clicked = true;
                            break;
                        case Settings.BOARD_WINNER_O:
                            tileString = "O";
                            clicked = true;
                            break;
                        default:
                            tileString = " ";
                            clicked = false;
                    }
                    tile t = new tile(tileString, j%3, j/3, clicked, i);
                    subboard.add(t, j%3, j/3);
                }
                subboard.setHgap(5);
                subboard.setVgap(5);
                outputBoard.add(subboard, i%3, i/3);
            }
            // Otherwise, just add a letter
            else {
                String winner = switch (subBoardWinners[i]) {
                    case Settings.BOARD_WINNER_X -> "X";
                    case Settings.BOARD_WINNER_O -> "O";
                    default -> " ";
                };
                Text t = new Text(winner);
                t.setId("fancytext");
                outputBoard.add(t, i%3, i/3);
            }
        }

        outputBoard.setHgap(20);
        outputBoard.setVgap(20);

        return outputBoard;
    }

    private void makeWinBox() {
        // Make a dialogue depending on if the player won or lost
        Dialog<ButtonData> dialog = new Dialog<>();
        dialog.setTitle("Game finished");

        // Set the appropriate win text
        if (client.getWinner() == client.getIdentity()) {
            dialog.setContentText("You won!");
        }
        else if (client.getWinner() == Settings.BOARD_WINNER_DRAW) {
            dialog.setContentText("It was a draw.");
        }
        else {
            dialog.setContentText("You lost.");
        }

        // Add a play again button
        ButtonType b = new ButtonType("Play again", ButtonData.OK_DONE);
        /* b.setOnAction(e -> {
            // Unfreeze
            freeze = false;

            // Tell the client to request a reset
            client.requestReset();

            // Reset the local board
            GridPane superboard = new GridPane();
            for (int i = 0; i< 9; i++) {
                GridPane subboard = new GridPane();
                for (int j = 0; j < 9; j++) {
                    subboard.add(new tile("", j%3, j/3, false, i), j%3, j/3);
                }
                superboard.add(subboard, i%3, i/3);
            }

            // Reset the dialog open flag
            winBoxOpen = false;
        }); */
        dialog.getDialogPane().getButtonTypes().add(b);

        // Open the dialog box
        Optional<ButtonData> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Request a reset
            client.requestReset();

            // Reset the client's internal game
            client.resetGame();

            // Reset the local board
            GridPane superboard = new GridPane();
            for (int i = 0; i< 9; i++) {
                GridPane subboard = new GridPane();
                for (int j = 0; j < 9; j++) {
                    subboard.add(new tile("", j%3, j/3, false, i), j%3, j/3);
                }
            }
            bp.setCenter(superboard);
            
            // Reset the open dialog flag
            winBoxOpen = false;

            // Unfreeze
            freeze = false;
        }
    }

    class tile extends Button
    {
        private String ch;
        private int x;
        private int y;
        private boolean clicked;
        private int number; // The linearized subboard position
        public tile(String ch, int x,int y, boolean clicked, int number)
        {
            super(ch);
            this.x=x;
            this.y=y;
            this.clicked= clicked;
            setPrefSize(50,50);
            setOnAction(e->{
                // If frozen, do nothing
                if (freeze) {
                    return;
                }
                
                // Check if it is the client's turn
                if (client.getCurrentTurn() != client.getIdentity()) {
                    System.out.println("turn");
                    return;
                }

                // Check if the subboard is valid
                if (client.getRequiredSubBoard() != 9 && client.getRequiredSubBoard() != number) {
                    System.out.println("subboard");
                    System.out.println(client.getRequiredSubBoard());
                    System.out.println(number);
                    return;
                }

                // Make sure the given tile isn't occupied
                if (client.getBoard()[number][3*y+x] != Settings.IDENTITY_UNASSIGNED) {
                    System.out.println("assigned");
                    System.out.println(client.getBoard()[number][3*y+x]);
                    return;
                }

                // Make the actual request
                client.sendButtonPress(number, 3*y+x);

                // Assume it was successful and set your own text
                if (client.getIdentity() == Settings.IDENTITY_X) {
                    setText("X");
                }
                else if (client.getIdentity() == Settings.IDENTITY_O) {
                    setText("O");
                }
            });
        }
    }
}
