import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;

public class PlayerSkeletonMunKeat {

    private double COEFFICIENT_AGGREGATE_HEIGHT;
    private double COEFFICIENT_COMPLETED_LINES;
    private double COEFFICIENT_EMPTY_SLOTS;
    private double COEFFICIENT_BUMPINESS;

    private static int DURATION_WAIT = 0;
    private static boolean DEBUG_FLAG = false;
    private static boolean REPORT_STATE = false;

    private final static String DATASET_START = "DATASET BEGIN";
    private final static String DATASET_END = "DATASET END";
    private final static int LEARNING_ROUNDS = 100;
    private final static int MAXIMUM_NUMBER_DATA_SET = 50;
    private final static double MUTATION_RATE = 0.25;
    private final static double MUTATION_DEGREE = 0.001;

    PlayerSkeletonMunKeat() {
         COEFFICIENT_AGGREGATE_HEIGHT = -0.510066; 
         COEFFICIENT_COMPLETED_LINES = 0.760666; 
         COEFFICIENT_EMPTY_SLOTS = -0.35663; 
         COEFFICIENT_BUMPINESS = -0.184483;
    }
    
    PlayerSkeletonMunKeat(double aggreHeight, double completedLines, double emptySlots, double bumpiness) {
        COEFFICIENT_AGGREGATE_HEIGHT = aggreHeight; 
        COEFFICIENT_COMPLETED_LINES = completedLines; 
        COEFFICIENT_EMPTY_SLOTS = emptySlots; 
        COEFFICIENT_BUMPINESS = bumpiness;
    }

    // implement this function to have a working system
    public void pickMove(State s) {
        int piece = s.getNextPiece();
        double calculatedUtility = 0;
        double maximumPossibleUtility = Double.NEGATIVE_INFINITY;

        int orient = 0;
        int orientation = 0;
        int columnToStartAt = 0;
        int slot = 0;

        int[][] legalMoves = s.legalMoves();

        for (int i = 0; i < legalMoves.length; i++) {
            if (legalMoves[i][State.ORIENT] != orient) {
                orient = legalMoves[i][State.ORIENT];
            }

            columnToStartAt = legalMoves[i][State.SLOT];

            calculatedUtility = calculateUtility(s, piece, orient, columnToStartAt);

            if (maximumPossibleUtility < calculatedUtility) {
                maximumPossibleUtility = calculatedUtility;
                orientation = orient;
                slot = columnToStartAt;
            }
        }

        s.makeMove(orientation, slot);
    }

    private double calculateUtility(State s, int piece, int orientation, int colToStart) {
        int[][] simulatedField = new int[State.ROWS][State.COLS];
        // Duplicate field
        for (int i = 0; i < State.ROWS; i++) {
            simulatedField[i] = Arrays.copyOf(s.getField()[i], s.getField()[i].length);
        }

        double predictedImpact = 0;

        // Parameters that will be used
        int completedLines = 0;
        int emptySlots = calculateHoles(simulatedField);
        int bumpiness = 0;
        int aggregateHeight = 0;

        completedLines = simulateNextMove(s, simulatedField, piece, orientation, colToStart);
        emptySlots = (calculateHoles(simulatedField) - emptySlots);
        bumpiness = calculateBumpiness(simulatedField);
        aggregateHeight = calculateAggregateHeight(simulatedField);

        predictedImpact = (COEFFICIENT_COMPLETED_LINES * completedLines) + (COEFFICIENT_EMPTY_SLOTS * emptySlots)
                + (COEFFICIENT_BUMPINESS * bumpiness) + (COEFFICIENT_AGGREGATE_HEIGHT * aggregateHeight);

        return predictedImpact;
    }

    private int simulateNextMove(State s, int[][] simulatedField, int piece, int orientation, int colToStart) {
        int linesCleared = 0;
        int height = Integer.MIN_VALUE;
        int turn = s.getTurnNumber() + 1;
        int simulatedFieldCol = simulatedField[simulatedField.length - 1].length;
        int simulatedFieldRow = simulatedField.length;

        int[] top = getTop(simulatedField);

        int[][] pWidth = State.getpWidth();
        int[][] pHeight = State.getpHeight();
        int[][][] pBottom = State.getpBottom();
        int[][][] pTop = State.getpTop();

        // for each column beyond the first in the piece
        for (int c = 0; c < pWidth[piece][orientation]; c++) {
            height = Math.max(height, top[colToStart + c] - pBottom[piece][orientation][c]);
        }

        // check if game ended
        if (height + pHeight[piece][orientation] >= simulatedFieldRow) {
            return Integer.MIN_VALUE;
        }

        // for each column in the piece - fill in the appropriate blocks
        for (int i = 0; i < pWidth[piece][orientation]; i++) {
            // from bottom to top of brick
            for (int h = height + pBottom[piece][orientation][i]; h < height + pTop[piece][orientation][i]; h++) {
                simulatedField[h][i + colToStart] = turn;
            }
        }

        // check for full rows - starting at the top
        for (int r = height + pHeight[piece][orientation] - 1; r >= height; r--) {
            // check all columns in the row
            boolean full = true;
            for (int c = 0; c < simulatedFieldCol; c++) {
                if (simulatedField[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            // if the row was full - remove it and slide above stuff down
            if (full) {
                linesCleared++;
                // for each column
                for (int c = 0; c < simulatedFieldCol; c++) {
                    // slide down all bricks
                    for (int i = r; i < top[c]; i++) {
                        simulatedField[i][c] = simulatedField[i + 1][c];
                    }
                }
            }
        }

        if (DEBUG_FLAG) {
            System.out.println("\n");
            System.out.println("Placing Piece #" + piece + " || " + "Orientation: " + orientation);
            System.out.println("Top (Simulated Move): " + Arrays.toString(getTop(simulatedField)));
            calculateHoles(simulatedField);
            System.out.println("Aggregate Height (Simulated Move): " + calculateAggregateHeight(simulatedField));
            int val = 0;
            System.out.println("Internal Representation: ");
            for (int i = simulatedField.length - 2; i >= 0; i--) { // for each
                                                                   // row
                System.out.print("[ ");
                for (int j = 0; j < simulatedField[i].length; j++) { // for each
                                                                     // column
                    val = simulatedField[i][j] % 100;
                    if (val == 0) {
                        System.out.print("-- ");
                    } else {
                        if (val < 10) {
                            System.out.print("0");
                        }
                        System.out.print(val + " ");
                    }
                }
                System.out.println("]");
            }
            System.out.println("\n\n");
        }

        return linesCleared;
    }

    private int calculateHoles(int[][] field) {
        // Assume that width and height are constants
        int calculatedCol = field[field.length - 1].length;
        int calculatedRow = field.length;

        int countedHoles = 0;
        int temporarySpace = 0;

        // DEBUG
        int[] holes = new int[calculatedCol];

        for (int col = 0; col < calculatedCol; col++) {
            for (int row = 0; row < calculatedRow; row++) {
                if (field[row][col] == 0) {
                    temporarySpace++;
                } else if (field[row][col] != 0) {

                    if (DEBUG_FLAG) {
                        holes[col] += temporarySpace;
                    }

                    countedHoles += temporarySpace;
                    temporarySpace = 0;
                }
            }

            temporarySpace = 0;
        }

        if (DEBUG_FLAG) {
            System.out.println("Holes: " + Arrays.toString(holes));
        }

        return countedHoles;
    }

    private int calculateBumpiness(int[][] field) {
        int bumpiness = 0;
        int calculatedCol = field[field.length - 1].length;

        int[] top = getTop(field);

        // Calculate bumpiness
        for (int col = 1; col < calculatedCol; col++) {
            bumpiness += Math.abs(top[col] - top[col - 1]);

            if (DEBUG_FLAG) {

            }
        }

        return bumpiness;
    }

    private int calculateAggregateHeight(int[][] field) {
        int aggregateHeight = 0;
        int[] top = getTop(field);

        for (int height : top) {
            aggregateHeight += height;
        }

        return aggregateHeight;
    }

    private int[] getTop(int[][] field) {
        // Assume that width and height are constants
        int calculatedCol = field[field.length - 1].length;
        int calculatedRow = field.length;

        int[] top = new int[calculatedCol];

        for (int col = 0; col < calculatedCol; col++) {
            for (int row = (calculatedRow - 1); row >= 0; row--) {
                if (field[row][col] != 0) {
                    top[col] = (row + 1);
                    break;
                }
            }
        }

        return top;
    }
    
    public void geneticAlgorithm() {
        /******************************************************************************************
         * Note: Dataset follows the convention Aggregate height, completed
         * lines, empty slots, bumpiness, <results gathered...>
         * 
         * PLEASE DON'T EDIT ANYTHING HERE
         ******************************************************************************************/
 
        // DATASET BEGIN
        int datasetIndex = 0;

        double[][] dataset = { 
                {-0.51, 0.76, -0.35, -0.18, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                {-0.5, 0.5, -0.5, -0.5, },
                };
        // DATASET END
        
        // Check if dataset has sufficient value; if not, populate them with seed values
        modifyCurrentFile(datasetIndex, dataset.length);
        
        // Run algorithm
        
        // Append data to dataset
        ArrayList<Double> scores = new ArrayList<Double>();
        int sum = 0;
        
        while(true) {
            State s = new State();
            
            PlayerSkeletonMunKeat p = new PlayerSkeletonMunKeat(dataset[datasetIndex][0], dataset[datasetIndex][1], 
                    dataset[datasetIndex][2], dataset[datasetIndex][3]);
            
            while (!s.hasLost()) {
                p.pickMove(s);
            }
            
            scores.add((double)s.getRowsCleared());
            sum += s.getRowsCleared();
            
            if(scores.size() == LEARNING_ROUNDS) {
                scores.add(((double)sum/scores.size()));
                scores.add(getStandardDeviation(scores, sum/scores.size()));
                break;
            }
        }
        
        appendResultsToFile(scores, datasetIndex);
        
        datasetIndex = (datasetIndex + 1) % MAXIMUM_NUMBER_DATA_SET ;
        
        //System.out.println(scores.toString());
    }
    
    private void appendResultsToFile(ArrayList<Double> scores, int datasetIndex) {
        final String results = scores.toString().replace("[", "").replace("]", "");
        
        ArrayList<String> fileContent = new ArrayList<String>();
        String line, path, fileName = null, decodedPath = null;
        
        try {
            // Find file path, and name
            path = PlayerSkeletonMunKeat.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            fileName = getClass().getName() + ".java";
            decodedPath = URLDecoder.decode(path + fileName, "UTF-8");
            // Read file in array
            BufferedReader br = new BufferedReader(new FileReader(decodedPath));
            while ((line = br.readLine()) != null) {
                fileContent.add(line);
            }
            br.close();
            
            boolean beginDataset = false;
            
            // Begin file modification
            for (int lineNumber = 0; lineNumber < fileContent.size(); lineNumber++) {
                line = fileContent.get(lineNumber);

                if (line.contains(DATASET_START)) {
                    beginDataset = true;
                    continue;
                } else if (line.contains(DATASET_END)) {
                    beginDataset = false;
                    continue;
                }
                
                // Begin appending if not of sufficient size
                if (beginDataset && line.contains("double[][] dataset = {")) {
                    int innerline;
                    
                    for (innerline = lineNumber; (innerline-lineNumber) < datasetIndex + 1 ; innerline++);
                    line = fileContent.get(innerline);
                    // Check if line has no results appended
                    if(line.length() - line.replace(",", "").length() <= 5) {
                        line = line.replace("},", results + "},");
                        fileContent.set(innerline, line);
                    }
                }
            }
            
            PrintWriter pw = new PrintWriter(decodedPath);
            for (String lineContent : fileContent) {
                pw.println(lineContent);
            }
            
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private double getStandardDeviation(ArrayList<Double> array, double mean) {
        double sum = 0;
 
        for (double scores: array) {
            sum += Math.pow((scores - mean), 2);
        }
 
        return Math.sqrt(sum/ (array.size())); // maybe n should go here?

    }
    
    private void modifyCurrentFile(int datasetIndex, int datasetLength) {
        ArrayList<String> fileContent = new ArrayList<String>();
        String line, path, fileName = null, decodedPath = null;
        
        try {
            // Find file path, and name
            path = PlayerSkeletonMunKeat.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            fileName = getClass().getName() + ".java";
            decodedPath = URLDecoder.decode(path + fileName, "UTF-8");
            // Read file in array
            BufferedReader br = new BufferedReader(new FileReader(decodedPath));
            while ((line = br.readLine()) != null) {
                fileContent.add(line);
            }
            br.close();
            
            modifyArrayRepresentationOfFileContent(fileContent, datasetIndex, datasetLength);
            
            PrintWriter pw = new PrintWriter(decodedPath);
            for (String lineContent : fileContent) {
                pw.println(lineContent);
            }
            
            pw.flush();
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void modifyArrayRepresentationOfFileContent(ArrayList<String> fileContent, int datasetIndex, int datasetLength) {
        String line;
        String defaultDataset = "                {-0.5, 0.5, -0.5, -0.5, },";
        
        boolean beginDataset = false, isDatasetOfSufficientSize = false;
        // Check if dataset is of sufficient size
        if (datasetLength >= MAXIMUM_NUMBER_DATA_SET / 5) {
            isDatasetOfSufficientSize = true;
        }
        
        // Begin file modification
        for (int lineNumber = 0; lineNumber < fileContent.size(); lineNumber++) {
            line = fileContent.get(lineNumber);

            if (line.contains(DATASET_START)) {
                beginDataset = true;
                continue;
            } else if (line.contains(DATASET_END)) {
                beginDataset = false;
                continue;
            }
            
            if(beginDataset && line.contains("datasetIndex")) {
                line = line.replaceAll("\\d", "" + ((datasetIndex + 1) % (Math.min(datasetLength, MAXIMUM_NUMBER_DATA_SET))));
                fileContent.set(lineNumber, line);
            }

            // Begin appending if not of sufficient size
            if (beginDataset && isDatasetOfSufficientSize == false && line.contains("double[][] dataset = {")) {
                int innerline;
                int shortFallOfDataset = (MAXIMUM_NUMBER_DATA_SET/5 - datasetLength);
                
                for (innerline = lineNumber + 1; !fileContent.get(innerline).contains("};"); innerline++);
                for (int i = 0; i < shortFallOfDataset; i++) {
                    fileContent.add((innerline), defaultDataset);
                }

                isDatasetOfSufficientSize = true;
            }
        }
    }

    public static void reportState(State s, int[][] legalMoves) {
        int[][] pWidth = State.getpWidth();
        int[][] pHeight = State.getpHeight();
        int[][][] pBottom = State.getpBottom();
        int[][][] pTop = State.getpTop();
        int turn = s.getTurnNumber();
        int piece = s.getNextPiece();

        if (!s.hasLost()) {
            System.out.println("-------------- Turn " + turn + " --------------");
            System.out.println("Next Piece: " + piece);

            // Prints out the next piece
            for (int i = pHeight[piece][0] - 1; i >= 0; i--) {
                for (int j = 0; j < pWidth[piece][0]; j++) {
                    if (pBottom[piece][0][j] < (i + 1) && pTop[piece][0][j] >= (i + 1)) {
                        turn = turn % 100;
                        System.out.print(String.format("%02d ", (turn + 1)));
                    } else {
                        System.out.print("   ");
                    }
                }
                System.out.println();
            }
            System.out.println();
        } else {
            System.out.println("-------- Game Over! --------");
        }

        /* Print out the playing field */
        int[][] field = s.getField();
        System.out.println("Field:");
        int val;
        // Print the hidden row at the top (if any square of this row is
        // occupied, means game over)
        System.out.print("[ ");
        for (int j = 0; j < field[field.length - 1].length; j++) { // for each
                                                                   // column
            val = field[field.length - 1][j] % 100;
            if (val == 0) {
                System.out.print("XX "); // empty square
            } else {
                // occupied square (number represents the turn in which the
                // piece is placed)
                System.out.print(String.format("%02d ", val));
            }
        }
        
        System.out.println("]  <--- Can't touch this");
        // Print the rest of the playing field (all the visible rows)
        for (int i = field.length - 2; i >= 0; i--) { // for each row
            System.out.print("[ ");
            for (int j = 0; j < field[i].length; j++) { // for each column
                val = field[i][j] % 100;
                if (val == 0) {
                    System.out.print("-- "); // empty square
                } else {
                    // occupied square (number represents the turn in which the
                    // piece is placed)
                    System.out.print(String.format("%02d ", val));
                }
            }
            System.out.println("]");
        }

        // Print the 'top' (column height) of each column
        int top[] = s.getTop();
        System.out.println("---------------------------------");
        System.out.print("[ ");
        for (int height : top) {
            System.out.print(String.format("%02d ", height));
        }
        System.out.print("]  <--- Column heights");

        if (!s.hasLost()) {
            int orient = 0;
            // Print the legal moves
            System.out.println("\n\nLegal Moves: ");
            System.out.print("Orientation " + (orient + 1) + ": [");
            for (int i = 0; i < legalMoves.length; i++) {
                int slot;
                if (legalMoves[i][0] != orient) {
                    orient = legalMoves[i][0];
                    System.out.print("]\nOrientation " + (orient + 1) + ": [");
                } else if (i != 0) {
                    System.out.print(", ");
                }
                slot = legalMoves[i][1] + 1;
                System.out.print(slot);
            }
            System.out.print("]");
        }
        System.out.print("\n\n\n");
    }

    public static void main(String[] args) {
        State s = new State();
        new TFrame(s);
        PlayerSkeletonMunKeat p = new PlayerSkeletonMunKeat();
        while (!s.hasLost()) {
            if (DEBUG_FLAG || REPORT_STATE) {
                reportState(s, s.legalMoves());
            }
            p.pickMove(s);
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(DURATION_WAIT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //p.geneticAlgorithm();
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
        System.exit(0);
    }
}
