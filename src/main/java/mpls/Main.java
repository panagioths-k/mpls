package mpls;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sun.misc.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    public static int viewportWidth = 500;                  //window dimensions
    public static int viewportHeight = 500;
    public static int viewportLeftMargin = 50;              //left margin before components start drawing
    public static double sizeOffset = 0.5;                  //rectangle size multiplier
    public static int lineVerticalOffset = 10;
    public static int rows = 4;                             //grid dimensions
    public static int columns = 3;
    public static double boldLineStrokeWidth = 4;           //line and text style
    public static String TEXT_STYLE = "-fx-font: 24 arial;";
    public static Color RECT_FILL_COLOR = Color.CYAN;       //desired rectangle fill color
    public static String CONNECTION_FILENAME = "conn.txt";

    private Map<Rectangle, Convection> convections = new HashMap<>();
    private List<ValidConnection> validConnections = new ArrayList<>();
    private List<Line> selectedLines = new ArrayList<>();
    List<Node> shapes = new ArrayList<>();
    final Stage errorPopup = new Stage();                   //list of elements

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        List<Rectangle> rectangles = new ArrayList<>();
        createRects(rectangles);
        createLines(rectangles);                            //create rects, read txt file and generate lines

        boolean validSchema = true;                         //used for accepting the schema as correct or not

        TextField sourceInput = new TextField();            //add menu elements for inputs and labels
        TextField destinationInput = new TextField();
        drawMenu(sourceInput, destinationInput);

        createConnections();                                //generate a complete connection model

        String errors = validateConnections();              //check for invalid rules
        if (!errors.trim().isEmpty()) {
            validSchema = false;
            updateErrorPopup(errors);
        }

        Group root = new Group();                           //draw components on screen
        root.getChildren().addAll(shapes);
        primaryStage.setTitle("MultiProtocol Label Switch (8x8)");
        primaryStage.setScene(new Scene(root, viewportWidth, viewportHeight));
        primaryStage.show();

        if (!validSchema) {                                 //show errors if any
            errorPopup.show();
        }
    }

    /**
     * Create rectangles that will later be drawn on screen, in a grid like formation
     *
     * @param rectangles list or Rectangles as reference
     */
    private void createRects(List<Rectangle> rectangles) {

        int n = rows * columns;
        int startingX = viewportLeftMargin;
        int availableWidth = (int) viewportWidth - 2 * viewportLeftMargin;
        int sizeWidth = (int) ((availableWidth / columns) * sizeOffset);
        int startingY = viewportLeftMargin;
        int availableHeight = (int) viewportHeight - 2 * viewportLeftMargin;
        int sizeHeight = (int) (availableHeight / rows * sizeOffset);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {             //position based on rows and columns
                Rectangle rectangle = new Rectangle(startingX + j * (availableWidth / columns), startingY + i * (availableHeight / rows), sizeWidth, sizeHeight);
                rectangle.setFill(RECT_FILL_COLOR);
                shapes.add(rectangle);
                rectangles.add(rectangle);
            }
        }
    }

    /**
     * Create lines among the rectangles, using the connections as they are read from a resource txt file
     *
     * @param rectangles list of rectangles, from with the lines will start and end
     */
    private void createLines(List<Rectangle> rectangles) {

        List<Line> lines = new ArrayList<>();                           //read resource file
        try {

            InputStream inputStream =this.getClass().getClassLoader().getResourceAsStream(CONNECTION_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            int totalConnections = Integer.parseInt(br.readLine());
            for (int i = 0; i < totalConnections; i++) {                //for each line generate a connection

                String[] paramsStr = br.readLine().split(" ");

                Connection conn = new Connection(Integer.parseInt(paramsStr[0]), Integer.parseInt(paramsStr[1]),
                        (Integer.parseInt(paramsStr[2]) == 0) ? false : true, Integer.parseInt(paramsStr[3]),
                        Integer.parseInt(paramsStr[4]), (Integer.parseInt(paramsStr[5]) == 0) ? false : true);

                Rectangle sourceRectangle = rectangles.get(conn.getSourceRow() * columns + conn.getSourceColumn());
                Rectangle destinationRectangle = rectangles.get(conn.getDestinationRow() * columns + conn.getDestinationColumn());
                Line line = new Line(sourceRectangle.getX() + sourceRectangle.getWidth(),
                        sourceRectangle.getY() + (conn.isSource() ? sourceRectangle.getHeight() - lineVerticalOffset : lineVerticalOffset), destinationRectangle.getX(),
                        destinationRectangle.getY() + (conn.isDestination() ? destinationRectangle.getHeight() - lineVerticalOffset : lineVerticalOffset));

                lines.add(line);

                if (!convections.containsKey(sourceRectangle)) {        //create a Convention object using the current file line
                    convections.put(sourceRectangle, new Convection());
                    Convection convection = convections.get(sourceRectangle);
                    convection.setRectangle(sourceRectangle);           //convention from which the line starts
                    convection.setSourceConvection(conn.getSourceColumn() == 0);
                    convection.setI(conn.getSourceRow());
                    convection.setJ(conn.getSourceColumn());
                    createTextForRectangle(convection);
                }
                Convection convection = convections.get(sourceRectangle);
                if (convection.isSourceConvection()) {
                    if (!conn.isSource()) {
                        convection.setUpFlag((conn.getSourceRow() * 2));
                    } else {
                        convection.setDownFlag((conn.getSourceRow() * 2 + 1));
                    }
                }

                if (!convections.containsKey(destinationRectangle)) {   //convention from which the line ends
                    convections.put(destinationRectangle, new Convection());
                    Convection convectionDest = convections.get(destinationRectangle);
                    convectionDest.setRectangle(destinationRectangle);
                    convectionDest.setFinalConvection(conn.getDestinationColumn() == columns - 1);
                    convectionDest.setI(conn.getDestinationRow());
                    convectionDest.setJ(conn.getDestinationColumn());
                    createTextForRectangle(convectionDest);
                }
                Convection convectionDest = convections.get(destinationRectangle);
                if (convectionDest.isFinalConvection()) {               //update connection flags
                    if (!conn.isDestination()) {
                        convectionDest.setUpFlag((conn.getDestinationRow() * 2));
                    } else {
                        convectionDest.setDownFlag((conn.getDestinationRow() * 2 + 1));
                    }
                }

                if (!conn.isSource()) {
                    convection.setNextUpConvection(convectionDest);
                    convection.setNextUpConvectionIsDown(conn.isDestination());
                    convection.setOutUp(line);
                    if (!convection.isNextUpConvectionIsDown()) {
                        convectionDest.setInUp(line);
                    } else {
                        convectionDest.setInDown(line);
                    }
                } else {
                    convection.setNextDownConvection(convectionDest);
                    convection.setNextDownConvectionIsDown(conn.isDestination());
                    convection.setOutDown(line);
                    if (!convection.isNextUpConvectionIsDown()) {
                        convectionDest.setInUp(line);
                    } else {
                        convectionDest.setInDown(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        shapes.addAll(lines);
    }

    /**
     * Draw numbers around left and right rectangles
     *
     * @param convection convection with rectangle
     */
    private void createTextForRectangle(Convection convection) {
        if (convection.isSourceConvection()) {
            Line lineUp = new Line(convection.getRectangle().getX() - 20, convection.getRectangle().getY() + lineVerticalOffset, convection.getRectangle().getX(), convection.getRectangle().getY() + lineVerticalOffset);
            Text textUp = new Text(lineUp.getStartX() - 20, lineUp.getStartY(), (convection.getI() * 2) + "");
            textUp.setStyle(TEXT_STYLE);
            Line lineDown = new Line(convection.getRectangle().getX() - 20, convection.getRectangle().getY() + convection.getRectangle().getHeight() - lineVerticalOffset, convection.getRectangle().getX(), convection.getRectangle().getY() + convection.getRectangle().getHeight() - lineVerticalOffset);
            Text textDown = new Text(lineDown.getStartX() - 20, lineDown.getStartY(), (convection.getI() * 2 + 1) + "");
            textDown.setStyle(TEXT_STYLE);

            convection.setInUp(lineUp);
            convection.setTextUp(textUp);
            convection.setInDown(lineDown);
            convection.setTextDown(textDown);
            shapes.addAll(Arrays.asList(lineUp, textUp, lineDown, textDown));
        } else if (convection.isFinalConvection()) {
            Line lineUp = new Line(convection.getRectangle().getX() + convection.getRectangle().getWidth(), convection.getRectangle().getY() + lineVerticalOffset, convection.getRectangle().getX() + convection.getRectangle().getWidth() + 20, convection.getRectangle().getY() + lineVerticalOffset);
            Text textUp = new Text(lineUp.getEndX() + 20, lineUp.getStartY(), (convection.getI() * 2) + "");
            textUp.setStyle(TEXT_STYLE);
            Line lineDown = new Line(convection.getRectangle().getX() + convection.getRectangle().getWidth(), convection.getRectangle().getY() + convection.getRectangle().getHeight() - lineVerticalOffset, convection.getRectangle().getX() + convection.getRectangle().getWidth() + 20, convection.getRectangle().getY() + convection.getRectangle().getHeight() - lineVerticalOffset);
            Text textDown = new Text(lineDown.getEndX() + 20, lineDown.getStartY(), (convection.getI() * 2 + 1) + "");
            textDown.setStyle(TEXT_STYLE);

            convection.setOutUp(lineUp);
            convection.setTextUp(textUp);
            convection.setOutDown(lineDown);
            convection.setTextDown(textDown);
            shapes.addAll(Arrays.asList(lineUp, textUp, lineDown, textDown));
        }
    }

    /**
     * Create all available {@link ValidConnection connections} using the current schema
     */
    private void createConnections() {

        List<Convection> conventionsList = new ArrayList<>(convections.values());
        for (Convection convention : conventionsList) {
            if (convention.isSourceConvection()) {                      //start from the left (source) convention

                createInnerConnection(convention, "", convention.getUpFlag(), false);
                createInnerConnection(convention, "", convention.getDownFlag(), true);
            }
        }
    }

    /**
     * Inserts a valid connection each time a recursive call reaches a final convention
     *
     * @param convention current convention to visit
     * @param code       binary code so far (0: same side crossing, 1: intersection crossing)
     * @param sourceFlag number from which the recursion began
     * @param isDown     whether we are visiting the current convention from a down side convention or not
     */
    private void createInnerConnection(Convection convention, String code, Integer sourceFlag, boolean isDown) {

        if (!convention.isFinalConvection()) {
            createInnerConnection(convention.getNextUpConvection(), code.concat(isDown ? "1" : "0"), sourceFlag, convention.isNextUpConvectionIsDown());
            createInnerConnection(convention.getNextDownConvection(), code.concat(isDown ? "0" : "1"), sourceFlag, convention.isNextDownConvectionIsDown());
        } else {

            validConnections.add(new ValidConnection(sourceFlag, convention.getUpFlag(), code.concat(isDown ? "1" : "0")));
            System.out.println(validConnections.get(validConnections.size() - 1).getSource() + " -> " +
                    validConnections.get(validConnections.size() - 1).getDestination() + ": " + validConnections.get(validConnections.size() - 1).getCode());

            validConnections.add(new ValidConnection(sourceFlag, convention.getDownFlag(), code.concat(isDown ? "0" : "1")));
            System.out.println(validConnections.get(validConnections.size() - 1).getSource() + " -> " +
                    validConnections.get(validConnections.size() - 1).getDestination() + ": " + validConnections.get(validConnections.size() - 1).getCode());
        }
    }

    /**
     * Define, based on specific rules whether the schema is valid or not
     *
     * <p>Checks whether everybody can communicate with everyone</p>
     *
     * @return
     */
    private String validateConnections() {

        int n = rows * 2;                                               //* 2 because we are dealing with binary conventions
        int errors = 0;
        String errorsStr = "";                                          //errors so far

        for (int i = 0; i < n; i++) {
            int finalI = i;
            List<ValidConnection> currentConnections =                  //get connections that start for each number
                    validConnections.stream().filter(v -> v.getSource() == finalI).collect(Collectors.toList());
            if (currentConnections.size() < n) {                        //they must have a specific size, otherwise they cant communicate with everyone
                errorsStr = errorsStr.concat("Source ").concat(finalI + "").concat(" has less than ").concat(n + "").concat(" connections.\n");
                errors++;
            } else if (currentConnections.size() > n) {
                errorsStr = errorsStr.concat("Source ").concat(finalI + "").concat(" has more than ").concat(n + "").concat(" connections\n");
                errors++;
            }
        }
        return !errorsStr.trim().isEmpty() ? "Found ".concat(errors + "").concat(" errors.\n").concat(errorsStr) : "";
    }

    /**
     * If any errors found, prepare a popup window that will show these errors
     *
     * @param errors
     */
    private void updateErrorPopup(String errors) {

        errorPopup.initModality(Modality.WINDOW_MODAL);                 //prepare
        Scene myDialogScene = new Scene(VBoxBuilder.create()
                .children(new Text(errors))
                .alignment(Pos.CENTER)
                .padding(new Insets(10))
                .build());

        errorPopup.setScene(myDialogScene);                             //add to scene
    }

    /**
     * Draws input fields for starting and ending point
     *
     * @param sourceInput      reference for starting number input
     * @param destinationInput reference for ending number input
     */
    private void drawMenu(TextField sourceInput, TextField destinationInput) {

        GridPane grid = new GridPane();                                 //init a grid
        Label sourceLabel = new Label("Source");
        grid.add(sourceLabel, 0, 1);              //add source label and input that accepts only numbers
        sourceInput.setMaxWidth(50);
        sourceInput.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d{0,4}")) {
                    sourceInput.setText(oldValue);
                }
            }
        });
        grid.add(sourceInput, 1, 1);

        Label destinationLabel = new Label("Destination");        //add destination label and input that accepts only numbers
        grid.add(destinationLabel, 2, 1);
        destinationInput.setMaxWidth(50);
        grid.add(destinationInput, 3, 1);
        destinationInput.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d{0,4}")) {
                    destinationInput.setText(oldValue);
                }
            }
        });

        Button findRouteBtn = new Button("Find Route");           //add play button
        grid.add(findRouteBtn, 4, 1);

        Label resultLabel = new Label("");                        //add label on which the binary code will be shown
        resultLabel.setStyle(TEXT_STYLE);
        grid.add(resultLabel, 5, 1);

        findRouteBtn.setOnAction(new EventHandler<ActionEvent>() {      //on button press find the correct route and the binary code
            @Override
            public void handle(ActionEvent e) {
                if (!sourceInput.getText().trim().isEmpty() && !destinationInput.getText().trim().isEmpty())
                    resultLabel.setText(findRoute
                            (Integer.parseInt(sourceInput.getText()), Integer.parseInt(destinationInput.getText())));
            }
        });

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setPadding(new Insets(viewportHeight - 50, 25, 25, 25));

        shapes.add(grid);
    }

//    private void connectFlagsWithMenus(TextField sourceInput, TextField destinationInput) {
//
//        for (Map.Entry<Rectangle, Convection> entry : convections.entrySet()) {
//            if (entry.getValue().isSourceConvection()) {
//                entry.getValue().getTextUp().addEventHandler(MouseEvent.MOUSE_CLICKED,
//                        event -> {
//                            sourceInput.setText(entry.getValue().getTextUp().getText());
//                            event.consume();
//                        });
//                entry.getValue().getTextDown().addEventHandler(MouseEvent.MOUSE_CLICKED,
//                        event -> {
//                            sourceInput.setText(entry.getValue().getTextDown().getText());
//                            event.consume();
//                        });
//            } else if (entry.getValue().isFinalConvection()) {
//                entry.getValue().getTextUp().addEventHandler(MouseEvent.MOUSE_CLICKED,
//                        event -> {
//                            destinationInput.setText(entry.getValue().getTextUp().getText());
//                            event.consume();
//                        });
//                entry.getValue().getTextDown().addEventHandler(MouseEvent.MOUSE_CLICKED,
//                        event -> {
//                            destinationInput.setText(entry.getValue().getTextDown().getText());
//                            event.consume();
//                        });
//            }
//        }
//    }

    /**
     * Given the starting and ending point, highlights all the intermediate route
     *
     * @param start starting point
     * @param end   ending point
     * @return the binary code
     */
    private String findRoute(int start, int end) {

        clearSelectedLines();
        List<Convection> convectionList = new ArrayList<>(convections.values());

        Convection startingConvection = convectionList.stream().filter(c -> c.isSourceConvection() &&
                (c.getUpFlag().equals(start) || c.getDownFlag().equals(start))).findFirst().orElse(null);
        if (startingConvection == null) {                           //find starting convection
            return "";
        }

        Convection finalConvection = convectionList.stream().filter(c -> c.isFinalConvection() &&
                (c.getUpFlag().equals(end) || c.getDownFlag().equals(end))).findFirst().orElse(null);
        if (finalConvection == null) {                              //find ending convection
            return "";
        }

        selectedLines.add((startingConvection.getUpFlag().equals(start)) ? startingConvection.getInUp() : startingConvection.getInDown());
        selectedLines.add((finalConvection.getUpFlag().equals(end)) ? finalConvection.getOutUp() : finalConvection.getOutDown());

        ValidConnection currentValidConnection = validConnections.stream().filter(v -> v.getSource() == start && v.getDestination() == end).findFirst().orElse(null);
        if (currentValidConnection == null) {                       //find connection object for the given route
            return "No connection found!";
        }

        Convection currentConvection = startingConvection;          //start from starting convection
        String code = currentValidConnection.getCode();
        boolean upFlag = startingConvection.getUpFlag().equals(start);
        for (int i = 0; i < columns - 1; i++) {

            if (upFlag) {                                           //define the next convection
                if (code.charAt(i) == '0') {
                    selectedLines.add(currentConvection.getOutUp());
                    upFlag = !currentConvection.isNextUpConvectionIsDown();
                    currentConvection = currentConvection.getNextUpConvection();

                } else {
                    selectedLines.add(currentConvection.getOutDown());
                    upFlag = !currentConvection.isNextDownConvectionIsDown();
                    currentConvection = currentConvection.getNextDownConvection();
                }
            } else {
                if (code.charAt(i) == '0') {
                    selectedLines.add(currentConvection.getOutDown());
                    upFlag = !currentConvection.isNextDownConvectionIsDown();
                    currentConvection = currentConvection.getNextDownConvection();
                } else {
                    selectedLines.add(currentConvection.getOutUp());
                    upFlag = !currentConvection.isNextUpConvectionIsDown();
                    currentConvection = currentConvection.getNextUpConvection();
                }
            }
        }


        selectedLines.forEach(l -> l.setStrokeWidth(boldLineStrokeWidth));
        return code;
    }

    /**
     * Each time the route changes, the previously highlighted lines are losing focus
     */
    private void clearSelectedLines() {
        selectedLines.forEach(l -> l.setStrokeWidth(1d));
        selectedLines.clear();
    }
}
