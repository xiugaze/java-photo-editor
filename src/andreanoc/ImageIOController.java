/*
 * CS1021-11
 * Winter 2021-2022
 * Lab 9: Image Manipulation Continued
 * Caleb Andreano
 * 02/14/2021
 */

package andreanoc;

import edu.msoe.cs1021.ImageUtil;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;


import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements the Event Handlers and helper methods
 * for the ImageIO gui
 */
public class ImageIOController {

    @FXML
    private ImageView iv;
    @FXML
    private VBox mainBox;

    @FXML private TextField field00;
    @FXML private TextField field01;
    @FXML private TextField field02;
    @FXML private TextField field10;
    @FXML private TextField field11;
    @FXML private TextField field12;
    @FXML private TextField field20;
    @FXML private TextField field21;
    @FXML private TextField field22;

    @FXML private Button apply;

    @FXML private Label colorLabel;
    @FXML private Label filenameLabel;

    @FXML private Button fileButton;
    @FXML private ContextMenu fileMenu;
    @FXML private Button viewButton;
    @FXML private ContextMenu viewMenu;

    @FXML private VBox container;
    @FXML private VBox leftPanel;
    @FXML private VBox rightPanel;

    @FXML private MenuItem save;
    @FXML private MenuItem reload;
    @FXML private Button redShift;
    @FXML private Button greenShift;
    @FXML private Button blueShift;
    @FXML private Button redGray;
    @FXML private Button negative;
    @FXML private Button grayscale;
    @FXML private Button nuke;
    @FXML private Button blur;
    @FXML private Button sharpen;
    @FXML private Button emboss;


    private Stage otherStage;


    private double[] weights;

    /**
     * Identity matrix for maintaining pixel color
     */
    public final int[] identity = new int[] {
            0, 0, 0,
            0, 1, 0,
            0, 0, 0
    };
    private final double[] identitiyDouble = new double[] {
            0, 0, 0,
            0, 1, 0,
            0, 0, 0
    };

    private Image currentImage;
    private Path currentImagePath;
    private final Alert alert = new Alert(Alert.AlertType.ERROR);


    /**
     * Handler for apply button
     * @param e ActionEvent from button
     */
    public void applyHandler(ActionEvent e) {
        if (currentImage != null) {
            currentImage = setFilter();
            iv.setImage(currentImage);
        }
    }

    /**
     * Handler for nuke button
     * @param e ActionEvent from button
     */
    public void nukeHandler(ActionEvent e) {
        embossHandler(e);
        for(int i = 0; i < 3; i++) {
            applyHandler(e);
        }
        sharpenHandler(e);
        for(int i = 0; i < 3; i++) {
            applyHandler(e);
        }
        setFilterTexts(new int[] {
                0, 0, 0,
                0, 1, 0,
                0, 0, 0
        });
    }

    /**
     * handler for Sharpen button
     * @param e actionEvent from button
     */
    public void sharpenHandler(ActionEvent e) {
        setFilterTexts(new int[] {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        });
        applyHandler(new ActionEvent());
    }

    /**
     * handler for blur button
     * @param e actionEvent from button
     */
    public void blurHandler(ActionEvent e) {
        setFilterTexts(new int[] {
                0, 1, 0,
                1, 5, 1,
                0, 1, 0
        });
        applyHandler(new ActionEvent());
    }

    /**
     * handler for emboss button
     * @param e actionEvent from button
     */
    public void embossHandler(ActionEvent e) {
        setFilterTexts(new int[] {
                -2, -1, -0,
                -1, 1, 1,
                0, 1, 2
        });
        applyHandler(new ActionEvent());
    }

    /**
     * returns modified image
     * @return modified image
     */
    private Image setFilter() {
        Image returnImage = this.currentImage;
        try {
            setWeights();
            System.out.println(Arrays.toString(weights));
            returnImage = ImageUtil.convolve(this.currentImage, weights);
        } catch (NumberFormatException err) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error: one or more filter values are invalid");
            alert.showAndWait();

        }
        return returnImage;
    }

    /**
     * Terminates the program on main window close
     * @param e WindowEvent from primary stage close button
     */
    public void closeAllWindows(WindowEvent e) {
        if(otherStage.isShowing()) {
            otherStage.close();
        }
    }

    /**
     * sets filter box contents given an array of ints
     * @param ints integer array
     */
    public void setFilterTexts(int[] ints) {
        field02.setText("" + ints[0]);
        field12.setText("" + ints[1]);
        field22.setText("" + ints[2]);
        field01.setText("" + ints[3]);
        field11.setText("" + ints[4]);
        field21.setText("" + ints[5]);
        field00.setText("" + ints[6]);
        field10.setText("" + ints[7]);
        field20.setText("" + ints[8]);
    }

    /**
     * sets the current array weights
     * @throws NumberFormatException
     */
    private void setWeights() throws NumberFormatException {
        double[] tempWeights = new double[] {
                parseWeight(field02), parseWeight(field12), parseWeight(field22),
                parseWeight(field01), parseWeight(field11), parseWeight(field21),
                parseWeight(field00), parseWeight(field10), parseWeight(field20)
        };
        double sum = Arrays.stream(tempWeights).sum();
        if (sum != 0) {
            weights = Arrays.stream(tempWeights).map(e -> e/sum).toArray();
        } else {
            weights = identitiyDouble;
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error: filter weights cannot add to 0");
            alert.showAndWait();
        }
    }

    /**
     * parses weight values from textfield
     * @param textField textfield to parse
     * @return returns double value
     * @throws NumberFormatException if non-double value passes through checks
     */
    private double parseWeight(TextField textField) throws NumberFormatException {
        return !textField.getText().equals("") && Double.parseDouble(textField.getText()) != 0
                ? Double.parseDouble(textField.getText()): 0;
    }

    /**
     * Ensures text fields contain valid filter values
     */
    public void checkTextFields() {
        Scanner scan;
        boolean disable = false;
        for(String e : getTexts()) {
            e = e.equals("") ? "" + 0:e;
            scan = new Scanner(e);
            if (!scan.hasNextInt()) {
                disable = true;
            }
        }
        apply.setDisable(disable);
    }

    /**
     * Returns all textfield contents as an arraylist
     * @return
     */
    private ArrayList<String> getTexts() {
        ArrayList<String> texts = new ArrayList<>();
        texts.add(field00.getText());
        texts.add(field01.getText());
        texts.add(field02.getText());
        texts.add(field10.getText());
        texts.add(field11.getText());
        texts.add(field12.getText());
        texts.add(field20.getText());
        texts.add(field21.getText());
        texts.add(field22.getText());
        return texts;
    }

    /**
     * Shows or hides right panel
     * @param e actionevent from menu
     */
    public void showRightPanel(ActionEvent e) {
        if(rightPanel.isManaged()) {
            rightPanel.setPrefWidth(3);
            container.setPrefWidth(container.getPrefWidth() + 180);
            rightPanel.setVisible(false);
            rightPanel.setManaged(false);
        } else {
            container.setPrefWidth(container.getPrefWidth() - 180);
            rightPanel.setPrefWidth(183);
            rightPanel.setVisible(true);
            rightPanel.setManaged(true);
        }
    }

    /**
     * Shows or hides left panel
     * @param e actionevent from menu
     */
    public void showLeftPanel(ActionEvent e) {
        if(leftPanel.isManaged()) {
            leftPanel.setPrefWidth(3);
            container.setPrefWidth(container.getPrefWidth() + 49);
            leftPanel.setVisible(false);
            leftPanel.setManaged(false);
        } else {
            container.setPrefWidth(container.getPrefWidth() - 49);
            leftPanel.setPrefWidth(52);
            leftPanel.setVisible(true);
            leftPanel.setManaged(true);
        }
    }

    /**
     * Displays menu attatched to View button
     * @param e actionevent from view button
     */
    public void showViewMenu(ActionEvent e) {
        if(viewMenu.isShowing()) {
            viewMenu.hide();
        } else {
            viewMenu.show(viewButton, Side.BOTTOM, 0, 0);
        }

    }

    /**
     * Handler for mouse movements inside of imageView
     * Displays color as a label
     * @param e MouseEvent from mouse move
     */
    public void displayPixel(MouseEvent e) {
        if(currentImage != null) {
            double verticalScale = currentImage.getHeight()/iv.getFitHeight();
            double horizontalScale = currentImage.getWidth()/iv.getFitWidth();
            int x = (int)Math.abs(Math.round(e.getX()*horizontalScale) - 1);
            int y = (int)Math.abs(Math.round(e.getY()*verticalScale) - 1);
            PixelReader pr = currentImage.getPixelReader();
            String color = pr.getColor(x, y).toString();
            colorLabel.setText(color.replace("0x", ""));
            colorLabel.setTextFill(pr.getColor(x, y));
        }
    }

    /**
     * Clears text on colorlabel
     * @param e mouseevent from exiting the imageview
     */
    public void clearColorLabel(MouseEvent e) {
        colorLabel.setText("");
    }

    public void setOtherStage(Stage otherStage) {
        this.otherStage = otherStage;
    }


    /**
     * Displays file menu attached to file button
     * @param e ActionEvent from button
     */
    public void showFileMenu(ActionEvent e) {
        if(fileMenu.isShowing()) {
            fileMenu.hide();
        } else {
            fileMenu.show(fileButton, Side.BOTTOM, 0, 0);
        }

    }

    /**
     * handler for keyboard shortcuts
     * @param e KeyEvent from keypress
     * @throws IOException throws from load method
     */
    public void keyCombos(KeyEvent e) throws IOException {
        if(e.getCode() == KeyCode.S && e.isShortcutDown() && currentImage != null) {
            save(new ActionEvent());
        } else if(e.getCode() == KeyCode.O && e.isShortcutDown()) {
            load(new ActionEvent());
        } else if(e.getCode() == KeyCode.R && e.isShortcutDown() && currentImage != null) {
            reload(new ActionEvent());
        }
    }
    /**
     * Shows or hides the filter screen
     * @param e ActionEvent from the button
     */
    public void showOther(ActionEvent e) {
        if(otherStage.isShowing()) {
            otherStage.hide();
        } else {
            Stage stage = (Stage) mainBox.getScene().getWindow();
            otherStage.setX(stage.getX() + 100);
            otherStage.setY(stage.getY() + 100);
            otherStage.show();
        }
    }
    /**
     * Event handler for open button
     * @param e ActionEvent from button
     * @throws IOException throws if there is an error reading file
     */
    @FXML
    public void load(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files",
                        "*.bmp", "*.png", "*.jpg", "*.msoe", "*.bmsoe")
        );
        fc.setInitialDirectory((Path.of("images").toFile()));
        File file = fc.showOpenDialog(new Stage());
        if(file != null) {
            currentImagePath = file.toPath();
            String path = currentImagePath.toString();
            System.out.println(path);
            try {
                currentImage = ImageIO.readImage(currentImagePath);
            } catch (IOException err) {
                System.err.println(err);
                alert.setHeaderText("Error: Unable to read file");
                alert.showAndWait();

            } catch (NoSuchElementException err) {
                System.err.println(err);
                alert.setHeaderText("Error: File contents is invalid");
                alert.showAndWait();
            } catch (IllegalArgumentException err) {
                System.out.println(err);
                alert.setHeaderText("Error: File contents is invalid");
                alert.showAndWait();
            } catch (IndexOutOfBoundsException err) {
                System.out.println(err);
                alert.setHeaderText("Error: File contains incorrect number of lines or colors");
                alert.showAndWait();
            } finally {
                if(currentImage != null) {
                    iv.setImage(currentImage);
                    filenameLabel.setText(path);
                    enableControls(true);
                }
            }
        }
    }

    /**
     * Handler for reload button
     * @param e ActionEvent from button
     * @throws IOException Throws when error reading file, i.e. file was deleted or moved
     */
    @FXML
    public void reload(ActionEvent e) {
        enableControls(false);
        try {
            if(currentImagePath != null) {
                currentImage = ImageIO.readImage(currentImagePath);
                iv.setImage(currentImage);
                System.out.println("reloaded");
                enableControls(true);
            }
        } catch (IOException err) {
            alert.setHeaderText("Error: unable to reload file (File may have been " +
                    "deleted or moved");
            alert.showAndWait();
        }

    }

    /**
     * Handler for save button
     * @param e ActionEvent from button
     * @throws IOException throws if error saving file
     */
    @FXML
    public void save(ActionEvent e) throws IOException {
        if(currentImage != null){
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Save directory");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                    new FileChooser.ExtensionFilter("PNG", "*.png"),
                    new FileChooser.ExtensionFilter("MSOE", "*.msoe"),
                    new FileChooser.ExtensionFilter("BMSOE", "*.bmsoe")
            );
            fc.setInitialDirectory((Path.of("images").toFile()));
            File file = fc.showSaveDialog(new Stage());
            if(file != null) {
                Path selectedFile = file.toPath();
                String fileExtension = selectedFile.getFileName().
                        toString().substring(selectedFile.getFileName()
                                .toString().lastIndexOf("."));
                try {
                    ImageIO.writeImage(selectedFile, currentImage, fileExtension);
                } catch (IOException err) {
                    System.err.println(err);
                    alert.setHeaderText("Error: unable to save selected file");
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Functional interface for apply(Color)
     */
    private interface Transformable {
        /**
         * Applies a transformation to a Color, which may be a function
         * of the Y value of the color's position
         * @param y Y value of the color's row
         * @param original original color
         * @return modified color
         */
        Color apply(int y, Color original);
    }

    /**
     * Applies a transformation to an image
     * @param image Image to perform transformation on
     * @param transform transformable
     * @return transformed image
     */
    private static Image transformImage(Image image, Transformable transform) {
        List<List<Color>> pixels = ImageIO.imageToMatrix(image);
        List<List<Color>> returnPixels = new ArrayList<>();
        for(int row = 0; row < pixels.size(); row++) {
            List<Color> workingRow = pixels.get(row);
            int workingRowIndex = row;
            returnPixels.add(workingRow.stream()
                    .map(e -> transform.apply(workingRowIndex, e))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return ImageIO.matrixToImage(returnPixels);
    }

    /**
     * Handler for grayscale button, applies grayscale filter to current image
     * @param e ActionEvent from button
     */
    @FXML
    public void grayscale(ActionEvent e) {
        if(currentImage != null) {
            currentImage = transformImage(currentImage, transformGrayscale);
            iv.setImage(currentImage);
        }
    }

    /**
     * Converts color to grayscale
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color toGrayscale(int y, Color original) {
        double gray = original.getRed() * 0.2126 + original.getGreen() *
                0.7152 + original.getBlue() * 0.0722;
        return Color.color(gray, gray, gray);
    }
    Transformable transformGrayscale = (y, c) -> toGrayscale(y, c);

    /**
     * Event handler for negative button, applies a negative filter to the current image
     * @param e ActionEvent from button
     */
    @FXML
    public void negative(ActionEvent e) {
        if(currentImage != null) {
            currentImage = transformImage(currentImage, transformNegative);
            iv.setImage(currentImage);
        }
    }

    /**
     * Converts color to negative
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color toNegative(int y, Color original) {
        return Color.color(1.0 - original.getRed(),
                1.0 - original.getGreen(), 1.0 - original.getBlue());
    }
    Transformable transformNegative = (y, c) -> toNegative(y, c);

    /**
     * Event handler for redGray button, applies redgray filter
     * @param e ActionEvent from button
     */
    @FXML
    public void redGray(ActionEvent e) throws IOException {
        reload(e);
        applyTransformation(currentImage, transformRedGray);
    }

    /**
     * Converts color to grayscale or red depending on the row index
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color redGray(int y, Color original) {
        Color returnColor;
        if(y % 2 == 0) {
            returnColor = Color.color(original.getRed(), 0, 0);
        } else {
            double gray = original.getRed() * 0.2126
                    + original.getGreen() * 0.7152
                    + original.getBlue() * 0.0722;
            returnColor = Color.color(gray, gray, gray);
        }
        return returnColor;
    }
    Transformable transformRedGray = (y, c) -> redGray(y, c);


    /**
     * Event handler for red button, applies red filter
     * @param e ActionEvent from button
     */
    @FXML
    public void red(ActionEvent e) throws IOException {
        reload(e);
        applyTransformation(currentImage, transformRed);
    }

    /**
     * Converts color to red
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color toRed(int y, Color original) {
        return Color.color(original.getRed(), 0, 0);
    }
    Transformable transformRed = (y, c) -> toRed(y, c);

    /**
     * Event handler for green button, applies green filter
     * @param e ActionEvent from button
     */
    @FXML
    public void green(ActionEvent e) throws IOException {
        reload(e);
        applyTransformation(currentImage, transformGreen);
    }

    /**
     * Converts color to green
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color toGreen(int y, Color original) {
        return Color.color(0, original.getGreen(), 0);
    }
    Transformable transformGreen = (y, c) -> toGreen(y, c);

    /**
     * Event handler for blue button, applies blue filter
     * @param e ActionEvent from button
     */
    @FXML
    public void blue(ActionEvent e) throws IOException {
        reload(e);
        applyTransformation(currentImage, transformBlue);
    }

    /**
     * Converts color to blue
     * @param y row index of the image
     * @param original original color
     * @return converted color
     */
    private static Color toBlue(int y, Color original) {
        return Color.color(0, 0, original.getBlue());
    }
    Transformable transformBlue = (y, c) -> toBlue(y, c);

    /**
     * Reloads image and applies transformation, prevents filter layering for tint transformations
     * @param image image to transform
     * @param transform transformable to apply
     */
    private void applyTransformation(Image image, Transformable transform) {
        if(currentImage != null) {
            currentImage = transformImage(image, transform);
            iv.setImage(currentImage);
        } else {
            alert.setHeaderText("Error: Unable to reload file");
            alert.showAndWait();
        }
    }
    public Pair<MenuItem[], Button[]> getControls() {
        return new Pair<>(
                new MenuItem[] {save, reload},
                new Button[] {redShift, greenShift, blueShift, redGray, grayscale,
                        negative, blur, sharpen, nuke, emboss});
    }

    public void enableControls(boolean enable) {
        for(MenuItem mi : getControls().getKey()) {
            mi.setDisable(!enable);
        }
        for(Button b : getControls().getValue()) {
            b.setDisable(!enable);
        }
    }

}
