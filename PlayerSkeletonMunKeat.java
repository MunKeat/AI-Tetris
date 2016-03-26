import java.util.Arrays;

public class PlayerSkeletonMunKeat {

    private double COEFFICIENT_AGGREGATE_HEIGHT;
    private double COEFFICIENT_COMPLETED_LINES;
    private double COEFFICIENT_EMPTY_SLOTS;
    private double COEFFICIENT_BUMPINESS;
    
    private static int DURATION_WAIT = 0;
    private static boolean DEBUG_FLAG = false;

    PlayerSkeletonMunKeat() {
        COEFFICIENT_AGGREGATE_HEIGHT = -0.51;
        COEFFICIENT_COMPLETED_LINES = 0.76;
        COEFFICIENT_EMPTY_SLOTS = -0.35;
        COEFFICIENT_BUMPINESS = -0.18;
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

        System.out.println("Completed Line: " + completedLines + "\n"
                + "Slots: " + emptySlots + "\n"
                + "Bumpiness: " + bumpiness + "\n"
                + "Aggregate Height: " + aggregateHeight
                + "\n\n\n");
        
        
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
        
        if(DEBUG_FLAG) {
            System.out.println("\n");
            System.out.println("Placing Piece #" + piece + " || " + "Orientation: " + orientation);
            System.out.println("Top (Simulated Move): " + Arrays.toString(getTop(simulatedField)));
            calculateHoles(simulatedField);
            System.out.println("Aggregate Height (Simulated Move): " + calculateAggregateHeight(simulatedField));
            int val = 0;
            System.out.println("Internal Representation: ");
            for (int i = simulatedField.length - 2; i >= 0; i--) { // for each row
                System.out.print("[ ");
                for (int j = 0; j < simulatedField[i].length; j++) { // for each column
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

        //DEBUG
        int[] holes = new int[calculatedCol];
        
        for (int col = 0; col < calculatedCol; col++) {
            for (int row = 0; row < calculatedRow; row++) {
                if (field[row][col] == 0) {
                    temporarySpace++;
                } else if (field[row][col] != 0) {
                    
                    if(DEBUG_FLAG) {
                        holes[col] += temporarySpace;
                    }
                    
                    countedHoles += temporarySpace;
                    temporarySpace = 0;
                }
            }
            
            temporarySpace = 0;
        }
        
        if(DEBUG_FLAG) {
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
            
            if(DEBUG_FLAG) {
                
            }
        }
        
        return bumpiness;
    }

    private int calculateAggregateHeight(int[][] field) {
        // Assume that width and height are constants
        int calculatedCol = field[field.length - 1].length;
        int aggregateHeight = 0;
        int[] top = getTop(field);

        for(int height: top) {
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
                        if ((turn + 1) < 10) {
                            System.out.print("0");
                        }
                        System.out.print((turn + 1) + " ");
                    } else {
                        System.out.print("   ");
                    }
                }
                System.out.println("");
            }
            System.out.println("");
            /*
             * for(int i = pHeight[piece][orient]-1; i >= 0; i--) { for(int j =
             * 0; j < pWidth[piece][orient]; j++) { System.out.println("[" +
             * (i+1) + " " + (j+1) + " " + pBottom[piece][orient][j] + " " +
             * pTop[piece][orient][j] + "]"); } }
             */
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
                if (val < 10) {
                    System.out.print("0");
                }
                System.out.print(field[field.length - 1][j] + " ");
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
                    if (val < 10) {
                        System.out.print("0");
                    }
                    System.out.print(val + " ");
                }
            }
            System.out.println("]");
        }

        // Print the 'top' (column height) of each column
        int top[] = s.getTop();
        System.out.println("---------------------------------");
        System.out.print("[ ");
        for (int i = 0; i < top.length; i++) {
            if (top[i] < 10) {
                System.out.print("0");
            }
            System.out.print(top[i] + " ");
        }
        System.out.print("]  <--- Column heights");

        if (!s.hasLost()) {
            // Print the legal moves
            System.out.println("\n\nLegal Moves: ");
            int orient = 0;
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
            if(DEBUG_FLAG) { reportState(s, s.legalMoves());}
            p.pickMove(s);
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(DURATION_WAIT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }
}
