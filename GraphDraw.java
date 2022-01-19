//Nicholas Lee
//COMP2631 - Final Part II
//April 2021

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.geometry.*;
import javafx.event.*;
import javafx.scene.canvas.*;
import javafx.scene.text.*;
import java.io.*;
import java.util.*;

public class GraphDraw extends Application{
    //Canvas Instance Vars
    private static final String COPYRIGHT_TEXT = " \u00A9 2021, COMP 2631 class";

    private BorderPane root;
    private Scene scene;
    private ImageView iView;
    private Label copyright;

    private MenuBar menuBar;
    private Menu fileMenu;
    private MenuItem open;
    private MenuItem exit;

    private VBox vBox;
    private Button button1;
    private Button button2;

    final double canvasWidth = 500;
    final double canvasHeight = 500;
    final double canavasCenterXY = 229;
    Pane canvas;

    final int radius = 200;

    //Graph Instance Vars
    int numNodes;
    boolean[][] adjMatrix;
    double[] nodeCenterX;
    double[] nodeCenterY;
    boolean[] active;
    FileChooser fileChoose;
    File inFile;

    //Start Class
    //Starts the applcation and sets up the GUI and handlers
    public void start(Stage stage) throws Exception{
        stage.setTitle("GraphDraw");
        setUpGUI(stage);
        fileChoose = new FileChooser();
        setUpHandlers(stage);
		stage.show();
    }

    //Sets up the GUI after stage is set, adds menu and items and the canvas for graph
    private void setUpGUI(Stage stage){
        //Set BorderPane Root
        root = new BorderPane();

        //Top menu: Sets the files menue with open and exit options
        open = new MenuItem("Open");
        exit = new MenuItem("Exit");
        fileMenu = new Menu("File", null, open, exit);

        //Add MenuBar to top region of BorderPane
        menuBar = new MenuBar(fileMenu);
        root.setTop(menuBar);

        //Copyright label
        copyright = new Label(COPYRIGHT_TEXT);
        root.setAlignment(copyright, Pos.CENTER);
        root.setBottom(copyright);

        //Left menu
        button1 = new Button("Clear");
        button2 = new Button("Clique Me!");
        vBox = new VBox(button1, button2);
        vBox.setPrefWidth(100);
        root.setLeft(vBox);
        button1.setMinWidth(vBox.getPrefWidth());
        button2.setMinWidth(vBox.getPrefWidth());

        //Canvas (centre)
        canvas = new Pane();
        canvas.setStyle("-fx-background-color:lightgray;");
        canvas.setPrefSize(canvasWidth,canvasHeight);
        root.setCenter(canvas);

        //Set the scene
        Scene scene = new Scene(root, Color.DARKGREY);
        stage.setScene(scene);
    }

    //Sets up the button click handlers for menu options
    private void setUpHandlers(Stage stage){
        //Open graph file, and call appropriate methods to set it up on canvas
        open.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent e) {
                inFile = fileChoose.showOpenDialog(stage);
                if(readInGraph(inFile)){
                    computeNodeCoords();
                    drawGraph();
                }

            }
        });
        //Exit the application
        exit.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent e) {
                Platform.exit();
            }
        });
        //Clears the current canvas
        button1.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent e) {
                canvas.getChildren().clear();
            }
        });
        //Clique me button
        button2.setOnAction(new EventHandler<ActionEvent>(){
            public void handle(ActionEvent e) {
                List<Integer> maxClique = findMaxClique();
                for(int i = 0; i < numNodes; i++){ //reset nodes to inactive
                    active[i] = false;
                }
                for(int i = 0; i < maxClique.size(); i++){ //activate clique nodes
                    active[maxClique.get(i)] = true;
                }
                drawGraph(); //redraw graph
            }
        });
    }

    //On file>open click, opens FileChooser so user can input graph file
    //Reads in the graph data to appropiate variables and checks it to be valid
    private boolean readInGraph(File f){
        try{
            //Open BR and check the first line for number of nodes
            BufferedReader fReader = new BufferedReader(new FileReader(f));
            numNodes = Integer.parseInt(fReader.readLine());
            if(numNodes < 0){
                System.out.println("NUM NODES INVALID!");
                return false;
            }

            //Init arrays to store adjacency matrix and active nodes
            adjMatrix = new boolean[numNodes][numNodes];
            active = new boolean[numNodes];

            //Fill Adjacanecy Matrix and active array
            for(int i = 0; i < numNodes; i++){
                int[] currLine = Arrays.stream(fReader.readLine().split(" ")).mapToInt(Integer::valueOf).toArray(); //Line string to int array
                active[i] = true; //Set all nodes to active on initial build
                for(int j = 0; j < numNodes; j++){
                    if(currLine[j] == 0){ //False = no edge
                        adjMatrix[i][j] = false;
                    }
                    else if(currLine[j] == 1){ //True = edge between i and j
                        adjMatrix[i][j] = true;
                    }
                    else{ //Adjacency matrix is invalid if not 0 or 1 after first line
                        System.out.println("MATRIX INVALID!");
                        return false;
                    }
                }
            }

            fReader.close(); // close reader
            return checkAdjMatrix(numNodes, adjMatrix); //check for valid matrix
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return false; //Graph not read, issue found
    }

    //Check the validity of the inputted adjacent matrix
    private static boolean checkAdjMatrix(int n, boolean[][] adj){
        if(n <= 0) return false; //n most be positive
        if(adj.length != n) return false; // adj must have n columns
        for(int i = 0; i < n; i++){
            if(adj[i].length != n) return false; // adj must have n rows
            for(int j = 0; j < n; j++){
                if(i == j && adj[i][j] != false) return false; // diagonal must be false
                else{
                    if(adj[i][j] != adj[j][i]) return false; // adj must be symmetric
                }
            }
        }
        return true;
    }

    //Compute all positions of graph nodes for objects
    private void computeNodeCoords(){
        //Init coordinate arrays
        nodeCenterX = new double[numNodes];
        nodeCenterY = new double[numNodes];

        //Get angle between each node
        double angle = 360.0/numNodes;

        //Find position for each node
        for(int i = 0; i < numNodes; i++){
            //Calculate X and Y coords using trig fucntions
            double currOffsetX = Math.cos((360-(angle*i)) * Math.PI / 180); 
            double currOffsetY = Math.sin((360-(angle*i)) * Math.PI / 180);
            nodeCenterX[i] = (radius*currOffsetX)+229.0;
            nodeCenterY[i] = (radius*currOffsetY)+229.0; //229px looked way more center than actual center
        }
    }

    //Draws the graph with its nodes or clears if no graph is found
    private void drawGraph(){
        //Clear graph on null
        if(adjMatrix == null){
            canvas.getChildren().clear();
        }
        //Build graph on pane
        else{
            canvas.getChildren().clear();

            //Draw Edges with helper function ( node must be active )
            for(int i = 0; i < numNodes; i++){
                if(active[i]){
                    drawEdges(i);
                }
            }

            //Draw Nodes
            for(int i = 0; i < numNodes; i++){
                //Create rounded rectangle object
                Rectangle r = new Rectangle();
                r.setX(nodeCenterX[i]-25);
                r.setY(nodeCenterY[i]-15);
                r.setWidth(50);
                r.setHeight(30);
                r.setArcWidth(5);
                r.setArcHeight(5);

                //Set to active colour if active
                if(active[i]){
                    r.setFill(Color.CYAN);
                }
                else{
                    r.setFill(Color.LIGHTCYAN);
                }
                
                //Set ID for event handler and give it text
                r.setId(Integer.toString(i));
                Text text = new Text(Integer.toString(i));
                text.setX(nodeCenterX[i]);
                text.setY(nodeCenterY[i]);
                canvas.getChildren().addAll(r,text); // add rectangle and text to canvas

                //Creating the mouse event handler 
                EventHandler<MouseEvent> eventHandler = new EventHandler<MouseEvent>() { 
                    @Override 
                    public void handle(MouseEvent e) { 
                        if(r.getFill() == Color.LIGHTCYAN){ // Inactive node, change to active and rebuild graph
                            r.setFill(Color.CYAN);
                            active[Integer.parseInt(r.getId())] = true;
                            canvas.getChildren().clear();
                            drawGraph();
                        }
                        else{
                            r.setFill(Color.LIGHTCYAN); // Active node, change to inactive and rebuild graph
                            active[Integer.parseInt(r.getId())] = false;
                            canvas.getChildren().clear();
                            drawGraph();
                        }
                    } 
                };
                r.addEventFilter(MouseEvent.MOUSE_CLICKED, eventHandler); //register handler
            }
        }
    }

    //Draws all edges
    private void drawEdges(int index){
        for(int i = 0; i < numNodes; i++){
            if(adjMatrix[index][i] == true && active[i]){ // build edge if found in adjacent matrix and node is active
                //Get start and end coords for line
                double startX = nodeCenterX[index];
                double startY = nodeCenterY[index];
                double endX = nodeCenterX[i];
                double endY = nodeCenterY[i];

                //Create Line and add to canvas
                Line line = new Line(startX, startY, endX, endY);
                canvas.getChildren().add(line);
            }
        }
    }

    List<int[]> combos = new ArrayList<int[]>(); //Holds all current combinations
    private List<Integer> findMaxClique(){
        List<Integer> maxClique = new ArrayList<Integer>();

        //Init array with all nodes
        int arr[] = new int[numNodes];
        for(int i = 0; i < numNodes; i++){
            arr[i] = i;
        }

        //Run combinations of nodes starting from largest till a clique is found
        for(int i = numNodes; i >= 2; i--){
            int data[] = new int[i];
            combinationUtil(arr, numNodes, i, 0, data, 0);

            //Test all combinations for clique
            for(int j = 0; j < combos.size(); j++){
                if(isAllConnected(combos.get(j))){
                    for(int k = 0; k < combos.get(j).length; k++){
                        maxClique.add(combos.get(j)[k]);
                    }
                    combos = new ArrayList<int[]>();
                    return maxClique; //return first and largest clique found
                }
            }
            combos = new ArrayList<int[]>();
        }
        return null; // no clique found?
    }

    //Get all combinations of certain length
    private void combinationUtil(int arr[], int n, int r, int index, int data[], int i){
        //If index reached current combo size, return the array
        if (index == r) {
            combos.add(data.clone());
            return;
        }

        // no nodes left
        if (i >= n){
            return;
        }

        //recurse
        data[index] = arr[i];
        combinationUtil(arr, n, r, index + 1, data, i + 1);
        combinationUtil(arr, n, r, index, data, i + 1);
    }

    //Checks if current graph is fully connected
    private boolean isAllConnected(int[] currNodes){
        for(int i = 0; i < currNodes.length; i++){
            for(int j = 0; j < currNodes.length; j++){
                if(i != j){ // if fully connected, the edge will be true in adjMatrix
                    if(adjMatrix[currNodes[i]][currNodes[j]] == false || adjMatrix[currNodes[j]][currNodes[i]] == false){
                        return false; //Edge doesnt exist, not a clique
                    }
                }
            }
        }
        return true; // All edges connected
    }
}