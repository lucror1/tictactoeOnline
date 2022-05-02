import java.util.ArrayList;

import javax.lang.model.util.ElementScanner14;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
public class tictactoe extends Application
{
    public ArrayList<ArrayList<Boolean>> currentgrid;
    private GridPane grid;
    public boolean mover;
    public boolean freeze;
    private int winner;
    private BorderPane bp;
    public tictactoe()
    {
        bp = new BorderPane();
        winner =0;
        freeze=false;
        currentgrid= new ArrayList<ArrayList<Boolean>>();
        grid = new GridPane();
        mover=true;
    }
    public static void main(String[] args) throws Exception {
        launch(args);
    }
    
    @Override
    public void start(Stage primary)
    {

        makegrid();
        bp.setCenter(grid);
        Scene scene = new Scene(bp, 800, 500);
        primary.setScene(scene);
        primary.show();
    }
    private void makegrid()
    {
        currentgrid.add(new ArrayList<Boolean>());
        currentgrid.add(new ArrayList<Boolean>());
        currentgrid.add(new ArrayList<Boolean>());
        for(int i = 0; i<9;i++)
        {
            tile x = new tile("", i%3, i/3, false );
            grid.add(x, i%3, i/3);
            currentgrid.get(i%3).add(null);
        }
        Button reset = new Button("Reset");
        reset.setPrefSize(75,75);
        reset.setOnAction(e->{
            currentgrid.clear();    
            makegrid();
            freeze= false;
            mover=true;
            winner=0;
            check();
        });
        grid.add(reset, 3,1);
        grid.setHgap(10); 
        grid.setVgap(10);
    }
    public void check()
    {
        ArrayList<Boolean> xWins= new ArrayList<Boolean>();
        xWins.add(true);
        xWins.add(true);
        xWins.add(true);
        ArrayList<Boolean> oWins= new ArrayList<Boolean>();
        oWins.add(false);
        oWins.add(false);
        oWins.add(false);
        if(!currentgrid.get(0).contains(null)&&!currentgrid.get(1).contains(null)&&!currentgrid.get(2).contains(null))
        {
            winner = 3;
            freeze = true;
        }
        for(int i =0; i<3; i++)
        {
            if (currentgrid.get(i).equals(xWins)==true)
            {
                winner =1;
                freeze = true;
            }
            if (currentgrid.get(i).equals(oWins)==true)
            {
                winner = 2;
                freeze = true;
            }
        }
        for(int i =0; i<3; i++)
        {
            ArrayList<Boolean> col= new ArrayList<Boolean>();
            col.add(currentgrid.get(0).get(i));
            col.add(currentgrid.get(1).get(i));
            col.add(currentgrid.get(2).get(i));
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
        ArrayList<Boolean> diag1= new ArrayList<Boolean>();
        diag1.add(currentgrid.get(0).get(0));
        diag1.add(currentgrid.get(1).get(1));
        diag1.add(currentgrid.get(2).get(2));
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
        ArrayList<Boolean> diag2= new ArrayList<Boolean>();
        diag2.add(currentgrid.get(0).get(2));
        diag2.add(currentgrid.get(1).get(1));
        diag2.add(currentgrid.get(2).get(0));
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
            bp.setRight(Win);
        }
        else if(winner == 2)
        {
            Text Win = new Text();
            String x="O Wins!";
            Win.setText(x);
            bp.setRight(Win);
        }
        else if(winner == 3)
        {
            Text Win = new Text();
            String x="Draw";
            Win.setText(x);
            bp.setRight(Win);
        }
        else{
            Text Win = new Text();
            String x="";
            Win.setText(x);
            bp.setRight(Win);
        }
    }
    class tile extends Button
    {
        private String ch;
        private int x;
        private int y;
        private boolean clicked;
        public tile(String ch, int x,int y, boolean clicked)
        {
            super(ch);
            this.x=x;
            this.y=y;
            this.clicked= clicked;
            setPrefSize(75,75);
            setOnAction(e->{
                if(freeze== false)
                {
                    if (clicked ==false)
                    {
                        if (mover==true)
                        {
                            grid.getChildren().remove(this);
                            grid.add(new tile("X", x, y, true),x, y);
                            mover= false;
                            currentgrid.get(y).set(x,true);
                            check();
                        }
                        else
                        {
                            grid.getChildren().remove(this);
                            grid.add(new tile("O", x, y, true),x, y);
                            currentgrid.get(y).set(x,false);
                            mover= true;
                            check();
                        }
                    }
                }
            });
        }
    }
}
