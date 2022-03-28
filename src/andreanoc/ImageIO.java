/*
 * CS1021-11
 * Winter 2021-2022
 * Lab 9: Image Manipulation Continued
 * Caleb Andreano
 * 02/14/2021
 */

package andreanoc;

import edu.msoe.cs1021.ImageUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Image IO class
 */
public class ImageIO extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start method for launch
     * @param primaryStage stage window
     * @throws IOException throws if there is an error reading FXML file
     */
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("gui2.fxml"));
        Parent root = mainLoader.load();

        root.getStylesheets().add(getClass().getResource("style.css").toString());
        ImageIOController imageIOController = mainLoader.getController();
        imageIOController.setFilterTexts(imageIOController.identity);

        FXMLLoader aboutLoader = new FXMLLoader(getClass().getResource("about.fxml"));
        Parent aboutRoot = aboutLoader.load();
        aboutRoot.getStylesheets().add(getClass().getResource("style.css").toString());
        Stage about = new Stage();
        imageIOController.setOtherStage(about);
        about.setScene(new Scene(aboutRoot));
        about.hide();

        imageIOController.showRightPanel(new ActionEvent());

        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(ImageIO.class
                .getResourceAsStream("icons/photoshop.png"))));
        primaryStage.setTitle("qoʜƨoƚoʜq");
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        imageIOController.enableControls(false);


        primaryStage.show();
    }

    /**
     * Returns an image object from a valid file path
     * @param image path of image file
     * @return returns image object
     * @throws IOException throws if there is an error reading image from path
     */
    public static Image readImage(Path image) throws IOException {
        Image returnImage;
        String extension = image.toString().substring(image.toString().lastIndexOf("."));
        switch(extension) {
            case(".msoe") -> returnImage = readMSOE(image);
            case(".bmsoe") -> returnImage = readBMSOE(image);
            default -> returnImage = ImageUtil.readImage(image);
        }
        return returnImage;
    }

    /**
     * Reads a .msoe file and attempts to return an Image object of the correct values
     * @param image path of .msoe file to be read
     * @return If no errors are thrown, returns an Image object
     * @throws IOException throws if DataInputStream is unable to be created from
     * specified path, or if the first line of the .msoe file is not "MSOE"
     * @throws NoSuchElementException Thrown if specified dimensions in the .msoe file are invalid
     * @throws IndexOutOfBoundsException thrown if at any line in the .msoe file the colors do not
     * match the specified dimensions in the .msoe file.
     */
    private static Image readMSOE(Path image) throws IOException, NoSuchElementException,
            IllegalArgumentException, IndexOutOfBoundsException {
        WritableImage wi;
        Pair<Integer, Integer> dimensions;
        List<String> lines = new ArrayList<>();
        List<ArrayList<Color>> pixels;
        Scanner in = new Scanner(new DataInputStream(Files.newInputStream(image)));

        while(in.hasNextLine()) {
            lines.add(in.nextLine());
        }
        if(!lines.get(0).equals("MSOE")) {
            throw new IOException("Error: File Format unable to be determined from first line");
        }

        dimensions = createDimensions(lines.get(1));
        lines = new ArrayList<>(lines.subList(2, lines.size()));

        pixels = lines.stream()
                .map(lineString -> lineToArrayList(lineString).stream()
                .map(ImageIO::stringToColor)
                .collect(Collectors.toCollection(ArrayList::new)))
                .collect(Collectors.toCollection(ArrayList::new));

        wi = new WritableImage(dimensions.getKey(), dimensions.getValue());
        PixelWriter pw = wi.getPixelWriter();

        if(pixels.size() != wi.getHeight()) {
            throw new IndexOutOfBoundsException("Image Height does not match specified dimensions");
        }
        for(int row = 0; row < pixels.size(); row++) {
            List<Color> currentLine = pixels.get(row);
            if(currentLine.size() != wi.getWidth()) {
                throw new IndexOutOfBoundsException("Image Width does not match " +
                        "specified dimensions on line " + (row + 3));
            }
            for (int element = 0; element < currentLine.size(); element++) {
                pw.setColor(element, row, currentLine.get(element));
            }
        }
        return wi;
    }

    /**
     * Returns A Pair of integers read from string
     * @param dString String hopefully containing two integers
     * @return Pair of Ints
     * @throws NoSuchElementException throws when either of the numbers in the
     * string cannot be read as an integer, or when there are more values in the string than
     * specified
     */
    private static Pair<Integer, Integer> createDimensions(String dString) throws
            NoSuchElementException {
        Scanner dStringParse = new Scanner(dString);
        Pair<Integer, Integer> dimensions = new Pair<>(dStringParse.nextInt(),
                dStringParse.nextInt());
        if (dStringParse.hasNext()) {
            throw new InputMismatchException("Error: too many elements in provided dimensions");
        }
        if (dimensions.getKey() < 1 || dimensions.getValue() < 1) {
            throw new InputMismatchException("All dimensions must be positive");
        }
        return dimensions;
    }

    /**
     * Reads a BMSOE file
     * @param path target path
     * @return returns Image object
     * @throws IOException throws if file does not exist or is deleted
     * @throws NoSuchElementException throws if file data is
     */
    private static Image readBMSOE(Path path) throws IOException, NoSuchElementException {
        String bmsoe = "";
        Pair<Integer, Integer> dimensions;
        try (DataInputStream di = new DataInputStream(Files.newInputStream(path))) {
            for (int i = 0; i < 5; i++) {
                bmsoe += (char) di.readByte();
            }
            if(!bmsoe.equals("BMSOE")) {
                throw new IOException("Error: Header information is incorrect");
            }
            dimensions = new Pair<>(di.readInt(), di.readInt());
            ArrayList<ArrayList<Integer>> colorInts = new ArrayList<>();
            for (int row = 0; row < dimensions.getValue(); row++) {
                ArrayList<Integer> workingRow = new ArrayList<>();
                for(int element = 0; element < dimensions.getKey(); element++) {
                    workingRow.add(di.readInt());
                }
                colorInts.add(workingRow);
            }
            return matrixToImage(colorInts.stream()
                    .map(list -> list.stream()
                            .map(i -> intToColor(i))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Writes an image to the image directory
     * @param path target path
     * @param image image object
     * @param extension file extension
     * @throws IOException throws when there is an error writing the file
     */
    public static void writeImage(Path path, Image image, String extension) throws IOException {
        switch(extension) {
            case (".msoe") -> writeMSOE(path, image);
            case (".bmsoe") -> writeBMOSE(path, image);
            default -> ImageUtil.writeImage(path, image);
        }
    }

    /**
     * Writes an .msoe file given a destination path and an image object
     * @param path destination path
     * @param image image object
     * @throws NumberFormatException throws if there is an error reading image widht or height
     * @throws FileNotFoundException throws if path does not exist
     */
    public static void writeMSOE(Path path, Image image)
            throws NumberFormatException, FileNotFoundException {

        try (PrintWriter pw = new PrintWriter(path.toFile())) {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            pw.write("MSOE\n");
            pw.write(width + " " + height + "\n");

            List<List<Color>> pixels = imageToMatrix(image);

            List<List<String>> colorStrings = pixels.stream()
                    .map(e -> e.stream()
                            .map(ImageIO::colorToString)
                            //.map(colorObj -> colorToString(colorObj))
                            .collect(Collectors.toCollection(ArrayList::new)))
                    .collect(Collectors.toCollection(ArrayList::new));

            for(List<String> line : colorStrings) {
                System.out.println(line);
            }
            for(List<String> lines : colorStrings) {
                String line = "";
                for(String colorString : lines) {
                    line += colorString + " ";
                }
                pw.write(line + "\n");
            }
            pw.flush();
        }
    }

    /**
     * Writes a .bmsoe image file to target path
     * @param path target path
     * @param image Image object to write
     * @throws IOException throws when there is an error creating a file (access?)
     */
    public static void writeBMOSE(Path path, Image image) throws IOException {
        try(DataOutputStream dos = new DataOutputStream(Files.newOutputStream(path))) {
            char[] bmsoe = "BMSOE".toCharArray();
            for(char c : bmsoe) {
                dos.write(c);
            }
            dos.writeInt((int) image.getWidth());
            dos.writeInt((int) image.getHeight());

            List<List<Integer>> ints = imageToMatrix(image).stream()
                    .map(list -> list.stream()
                            .map(color -> colorToInt(color))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            for(List<Integer> l : ints) {
                for(int e : l) {
                    dos.writeInt(e);
                }
            }
        }
    }

    /**
     * Converts an integer to a color
     * @param color color int
     * @return Color object
     */
    private static Color intToColor(int color) {
        double red = ((color >> 16) & 0x000000FF)/255.0;
        double green = ((color >> 8) & 0x000000FF)/255.0;
        double blue = (color & 0x000000FF)/255.0;
        double alpha = ((color >> 24) & 0x000000FF)/255.0;
        return new Color(red, green, blue, alpha);
    }

    /**
     * Converts a Color to an integer
     * @param color Color object
     * @return color int
     */
    private static int colorToInt(Color color) {
        int red = ((int)(color.getRed()*255)) & 0x000000FF;
        int green = ((int)(color.getGreen()*255)) & 0x000000FF;
        int blue = ((int)(color.getBlue()*255)) & 0x000000FF;
        int alpha = ((int)(color.getOpacity()*255)) & 0x000000FF;
        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    /**
     * Converts a hexadecimal string to a Color Object
     * @param hex passed hexadecimal color string
     * @return returns Color object matching passed in string
     * @throws IllegalArgumentException throws if string does not match valid
     * hex triplet color formatting
     */
    private static Color stringToColor(String hex) throws IllegalArgumentException {
        if(!((hex.length() == 7 || hex.length() == 9) && hex.charAt(0) == '#')) {
            throw new IllegalArgumentException("Error: Specified item " + hex + " does " +
                    "not match correct hexadecimal format");
        } else {
            return Color.valueOf(hex.substring(1));
        }
    }

    /**
     * Converts a string into an ArrayList of strings
     * (used for a string containing multiple Words)
     * @param line line to convert to ArrayList of Strings
     * @return ArrayList of Strings
     */
    private static List<String> lineToArrayList(String line) {
        List<String> elements = new ArrayList<>();
        Scanner in = new Scanner(line);
        while(in.hasNext()) {
            elements.add(in.next());
        }
        return elements;
    }

    /**
     * converts color object to hex value with alpha
     * @param color Color object
     * @return returns hex color in format #XXXXXX
     */
    private static String colorToString(Color color) {
        return "#" + color.toString().replace("0x", "").toUpperCase();
    }

    /**
     * Converts an image object ot a matrix of colors
     * @param image image to convert
     * @return matrix of color objects
     */
    public static List<List<Color>> imageToMatrix(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pr = image.getPixelReader();

        List<List<Color>> pixels = new ArrayList<>();
        for(int row = 0; row < height; row++) {
            ArrayList<Color> currentLine = new ArrayList<>();
            for (int element = 0; element < width; element++) {
                currentLine.add(pr.getColor(element, row));
            }
            pixels.add(currentLine);
        }
        return pixels;
    }

    /**
     * Converts matrix of colors to image object
     * @param matrix matrix to convert to image
     * @return image object
     */
    public static Image matrixToImage(List<List<Color>> matrix) {
        int width = matrix.get(0).size();
        int height = matrix.size();

        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pw = writableImage.getPixelWriter();
        for (int row = 0; row < height; row++) {
            for (int element = 0; element < width; element++) {
                pw.setColor(element, row, matrix.get(row).get(element));
            }
        }
        return writableImage;
    }

}