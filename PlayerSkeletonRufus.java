import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;


public class PlayerSkeletonRufus {
	
	/////////////////////////////*Modify the 8 elements below to choose how you want to run the program*////////////////////////////////
	
	//Set mode as 1 to play game, 2 to run standard fitness test, 3 to run timed fitness test, 4 to run standard trial, 5 to run timed trial
	private static int mode = 2; 
	
	//Weighted factors are aggregate height, number of holes, bumpiness, maximum heights sum, holes depth, and wells depth
	private static double[] weightsDefault = normalizeWeight(new double[] 
										    //{20, 35, 20, 19, 3, 3}); // -- modify for game or fitness test (mode 1, 2 or 3)
											{0.0167, 0.6619, 0.0829, 0.0158, 0.1293, 0.0935});

	private static boolean printAIthoughts = false; //set as true to debug the AI's method of determining best move
	private static boolean printGameStatesInfo = true; //set as true to print game states' information to the console (if not writing to file)
	private static boolean writeResultToFile = true; //set as true to write results to file
	
	private static int numberOfWeightSets = 10; //how many weight sets to generate -- for trials (mode 4 or 5)
	private static int numberOfGames = 10; //how many games to run for a weight set -- for fitness test or trials (mode 2, 3, 4 or 5)
	private static int maxNumberOfTurns = 200;  //how many turns before a game terminates -- for timed fitness test or timed trial (mode 3 or 5)
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/*Don't modify these elements*/
	private static int numberOfWeightedFactors = 6;
	private static DecimalFormat fourDP = new DecimalFormat("0.0000");
	private static boolean computingScore = false; //stops printing of results when AI is computing the scores
	private static File output;
	private static FileOutputStream fos;
	private static BufferedOutputStream bos;
	private static PrintStream ps;
	
	
	/**
	 * Simulates what a state will be like after making a legal move
	 */
	public class FutureState extends State{
		
		//////*Copies the field, top and nextPiece from the current state*//////
		/*Initializes the variables*/
		private int[][] field = new int[ROWS][COLS];
		private int[] top  = new int[COLS];;
		private int piece = -1; 
		
		/*Constructor*/
		public FutureState(State s){
			setField(s);
			setTop(s);
			setPiece(s);
		}
		
		/*Copies the data*/
		public void setField(State s){
			int[][] f = s.getField();
			for (int i = 0; i < f.length; i++) {
				System.arraycopy(f[i], 0, this.field[i], 0, f[i].length);
			}
		}
		public void setTop(State s){
			System.arraycopy( s.getTop(), 0, this.top, 0, top.length);
		}
		public void setPiece(State s){
			this.piece = s.getNextPiece();
		}
		////////////////////////////////////////////////////////////////////////
		
		
		private int[][] pWidth = State.getpWidth();
		private int[][] pHeight = State.getpHeight();
		private int[][][] pBottom = State.getpBottom();
		private int[][][] pTop = State.getpTop();
		
		private int squareValue = 1; //represents a filled square
		
		
		////////*Overrides some of the methods in State*////////
		/*Override getter methods in State*/
		public int[][] getField() {
			return this.field;
		}
		public int[] getTop() {
			return this.top;
		}
		public int getPiece() {
			return this.piece;
		}
		
		/*Override makeMove methods in State*/
		public void makeMove(int move) {
			this.makeMove(legalMoves[piece][move]);
		}
		public void makeMove(int[] move) {
			this.makeMove(move[ORIENT],move[SLOT]);
		}
		public boolean makeMove(int orient, int slot) {
			//height if the first column makes contact
			int height = top[slot]-pBottom[piece][orient][0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pWidth[piece][orient];c++) {
				height = Math.max(height,top[slot+c]-pBottom[piece][orient][c]);
			}
			
			//check if game ended
			if(height+pHeight[piece][orient] >= ROWS) {
				lost = true;
				return false;
			}
			
			//for each column in the piece - fill in the appropriate blocks
			for(int i = 0; i < pWidth[piece][orient]; i++) {
				//from bottom to top of brick
				for(int h = height+pBottom[piece][orient][i]; h < height+pTop[piece][orient][i]; h++) {
					field[h][i+slot] = squareValue;
				}
			}
			
			//adjust top
			for(int c = 0; c < pWidth[piece][orient]; c++) {
				top[slot+c]=height+pTop[piece][orient][c];
			}
			
			//check for full rows - starting at the top
			for(int r = height+pHeight[piece][orient]-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < COLS; c++) {
					if(field[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					//for each column
					for(int c = 0; c < COLS; c++) {
	
						//slide down all bricks
						for(int i = r; i < top[c]; i++) {
							field[i][c] = field[i+1][c];
						}
						//lower the top
						top[c]--;
						while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
					}
				}
			}
			return true;
		}
		////////////////////////////////////////////////////////
	}




	/**
	 * Report the state of the current turn
	 */
	public static void reportState(State s, int[][] legalMoves){
		if (printGameStatesInfo || writeResultToFile) {
			printTurn(s);
			if (!s.hasLost()) {
				printRowsCleared(s);
				printNextPiece(s);
			}
			printField(s);
		}
		
		int[] heightStats = computeHeightsStats(s);
		double[] holeStats = computeHolesStats(s);
		double[] bumpinessStats = computeBumpiness(s);
		
		int aggregateHeight = heightStats[0];
		int numberOfHoles = (int)holeStats[0];
		int bumpiness = (int)bumpinessStats[0];
		int maxHeightsSum = heightStats[1];
		double holesDepth = holeStats[1];
		double wellsDepth = bumpinessStats[1];
		
		/*Prints out the weighted factors*/
		if (printGameStatesInfo || writeResultToFile) {
			System.out.println("\nAggregate Height = " + aggregateHeight);
			System.out.println("Number of holes = " + numberOfHoles);
			System.out.println("Bumpiness = " + bumpiness);
			System.out.println("Max Heights Sum = " + maxHeightsSum);
			System.out.println("Holes Depth = " + fourDP.format(holesDepth));
			System.out.println("Wells Depth = " + fourDP.format(wellsDepth));
		}
	}

	/**
	 * Just pick a random move!
	 */
	/*public int pickRandomMove(State s, int[][] legalMoves) {
		Random rand = new Random();
		int nextMove = rand.nextInt(legalMoves.length); 
		System.out.println("Pick Move: Orientation " + (legalMoves[nextMove][0]+1) + ", Slot " + (legalMoves[nextMove][1]+1) + "\n\n");
		return nextMove;
	}*/
	
	/**
	 * Prints out the turn number
	 */
	private static void printTurn(State s) {
		if (s.hasLost()) {
			System.out.println("\n-------- Game Over! --------\n");
		} else {
			int turn = s.getTurnNumber() + 1;
			System.out.println("\n-------------- Turn " + turn + " --------------");
		}
	}

	/**
	 * Prints out the number of rows cleared
	 */
	private static void printRowsCleared(State s) {
		System.out.println("\nRows cleared = " + s.getRowsCleared());
	}

	/**
	 * Prints out the ID and shape of the next piece
	 */
	private static void printNextPiece(State s) {
		
		//[pieceID][orientation]
		int[][] pWidth = State.getpWidth(); //width of the piece
		int[][] pHeight = State.getpHeight(); //height of the piece
		
		//[pieceID][orientation][column]
		int[][][] pBottom = State.getpBottom(); //row number of the square below the bottom-most filled square in that column
		int[][][] pTop = State.getpTop(); //row number of the top-most filled square in that column
		
		int piece = s.getNextPiece();
		int turn = s.getTurnNumber();
		
		/*Prints out the ID*/
		System.out.println("\nNext Piece = " + piece);
		
		/*Prints out the shape*/
		int orient = 0;
		int squareValue = 0; //value that represents the piece's squares in the field
		
		for(int i = pHeight[piece][orient]; i > 0; i--) { //for each row of the piece, from top to bottom
			for(int j = 0; j < pWidth[piece][orient]; j++) { //for each column of the piece, from left to right
				
				if (pBottom[piece][orient][j] < i && pTop[piece][orient][j] >= i) { 
					squareValue = (turn+1) % 100; //squareValue is based on next turn number, limited to 2 digits
					if (squareValue < 10) {
						System.out.print("0");
					}
					System.out.print(squareValue + " "); //filled square
				
				} else { 
					System.out.print("   "); //empty square
				}
				
			}
			System.out.println("");
		}
		System.out.println("");
		
	}

	/**
	 * Prints out the playing field, showing how the game currently looks like
	 */
	private static void printField(State s) {
		
		//[row][column]
		int[][] field = s.getField();
		//[column]
		int top[] = s.getTop(); //the height of the highest filled square of that column
		int squareValue;
		
		System.out.println("  1  2  3  4  5  6  7  8  9  10");
		System.out.println("---------------------------------");
		
		/*Prints the hidden row at the top (if any square of this row is filled, means game over)*/
		System.out.print("[ ");
		
		for (int j = 0; j < field[field.length-1].length; j++) {
			squareValue = field[field.length-1][j] % 100;
			if (squareValue == 0) {
				System.out.print("XX "); //empty square
			} else {
				if (squareValue < 10) {
					System.out.print("0");
				}
				System.out.print(squareValue + " "); //filled square
			}
		}
		System.out.println("]  <--- Can't touch this");
	
		/*Prints the rest of the playing field*/
		for (int i = field.length-2; i >= 0; i--) {
			System.out.print("[ ");
			
			for (int j = 0; j < field[i].length; j++) {
				
				if (field[i][j] == 0) {
					if (top[j] > i) {
						System.out.print("** "); //hole
					} else {
						System.out.print("-- "); //empty square that is not a hole
					}
					
				} else {
					squareValue = field[i][j] % 100;
					if (squareValue < 10) {
						System.out.print("0");
					}
					System.out.print(squareValue + " "); //filled square
				}
			}
			
			System.out.println("]");
		}
		
	}

	/**
	 * Picks the best next move by finding a move that gives the lowest score in the next state
	 */
	public int pickMove(State s, int[][] legalMoves, double[] weights) {
		
		if (printAIthoughts && (printGameStatesInfo|writeResultToFile)) {
			//printLegalMoves(legalMoves);
		}
		int bestMove = computeScores(s, legalMoves, weights);
		
		return bestMove;
	}

	/**
	 * Prints out the legal moves for the next piece
	 */
	public void printLegalMoves(int[][] legalMoves) {
		System.out.println("Legal Moves: ");
		
		//Each legal move is represented by (orientation, slot)//
		int orient = -1; //orientation of current legal move
		int slot = -1; //slot of current legal move
		
		//Iterate through legalMoves, which is ordered by orientation, then slot.//
		for (int i = 0; i < legalMoves.length; i++) {
			
			if (legalMoves[i][State.ORIENT] != orient) { //check if orientation of current legal move is different from previous
				orient = legalMoves[i][State.ORIENT];
				if (i != 0) {
					System.out.print("]\n"); //if yes, print orientation
				}
				System.out.print("Orientation " + (orient+1) + ": [");
				
			} else if (i != 0){
				System.out.print(", "); //if no, just print the slot
			}
			
			slot = legalMoves[i][State.SLOT];
			System.out.print((slot+1));
		}
		System.out.println("]");
		System.out.print("\nScores:");
	}

	/**
	 * Compute the score of each legal move (possible next state) 
	 * using the heuristics: aggregate height, hole score, bumpiness, and max heights sum.
	 * And find the best move which has the lowest score
	 * @return best move
	 */
	private int computeScores(State s, int[][] legalMoves, double[] weights){
		
		/*Initializes data for the next state*/
		State nextState;
		
		ArrayList<Double> scores = new ArrayList<Double>(); //lower the score the better
		ArrayList<Integer> lowestScoreMoves = new ArrayList<Integer>();
		double lowestScore = Double.MAX_VALUE;
		int orient = -1; //orientation of current legal move
		
		ArrayList<Integer> heights = new ArrayList<Integer>();
		int aggregateHeight = 0;
		int numberOfHoles = 0;
		int bumpiness = 0;
		int maxHeightsSum = 0;
		double holesDepth = 0;
		double wellsDepth = 0;
		double score = 0;
		
		for (int i = 0; i < legalMoves.length; i++) {
			
			/*Compute the score of each legal move*/
			nextState = new FutureState(s);
			nextState.makeMove(i);
			heights.clear();
			
			if (nextState.hasLost()) {
				scores.add(Double.MAX_VALUE);
				
			} else {
				computingScore = true;
				
				int[] heightStats = computeHeightsStats(nextState);
				double[] holesStats = computeHolesStats(nextState);
				double[] bumpinessStats = computeBumpiness(nextState);
				
				aggregateHeight = heightStats[0];
				numberOfHoles = (int)holesStats[0];
				bumpiness = (int)bumpinessStats[0];
				maxHeightsSum = heightStats[1];
				holesDepth = holesStats[1];
				wellsDepth = bumpinessStats[1];
				
				score = weights[0]*aggregateHeight + weights[1]*numberOfHoles + weights[2]*bumpiness 
						+ weights[3]*maxHeightsSum + weights[4]*holesDepth + weights[5]*wellsDepth;
				scores.add(score);
				
				/*Find the move(s) that has the lowest score*/
				if (score <= lowestScore) {
					if (score < lowestScore) {
						lowestScore = score;
						lowestScoreMoves.clear();
					}
					lowestScoreMoves.add(i);
				}
				
				computingScore = false;
			}
			
			if (printAIthoughts && (printGameStatesInfo|writeResultToFile)) {
				orient = printScores(legalMoves, nextState, aggregateHeight, numberOfHoles,
						 bumpiness, score, maxHeightsSum, orient, i);
			}
		}

		int bestMove;
		if (lowestScore == Double.MAX_VALUE) {
			lowestScore = -1;
			bestMove = 0;
		} else {
			bestMove = lowestScoreMoves.get(0);
		}
		
		if (printAIthoughts && (printGameStatesInfo|writeResultToFile)){
			printMoves(legalMoves, lowestScoreMoves, lowestScore, bestMove);
		}
		
		return bestMove;
	}

	/**
	 * Computes and prints out the height of each column.
	 * Computes and return the aggregate height (sum of all heights) and the max heights sum (sum of the 3 highest columns)
	 * @return {aggregateHeight, maxHeightsSum}
	 */
	private static int[] computeHeightsStats(State s) {
		int[] top = s.getTop();
		
		if (!computingScore && (printGameStatesInfo|writeResultToFile)) {
			System.out.println("---------------------------------");
			System.out.print("[ ");
		}
		
		ArrayList<Integer> heights = new ArrayList<Integer>();
		int aggregateHeight = 0; //sum of all column heights
		for (int i = 0; i < top.length; i++) {
			int height = top[i];
			heights.add(height);
			aggregateHeight += height;
			
			if (!computingScore && (printGameStatesInfo|writeResultToFile)) {
				if (height < 10) {
					System.out.print("0");
				}
				System.out.print(height + " ");
			}
		}
		
		if (!computingScore && (printGameStatesInfo|writeResultToFile)) {
			System.out.println("]\n---------------------------------");
			System.out.println("  1  2  3  4  5  6  7  8  9  10 ");
		}
		
		Collections.sort(heights);
		Collections.reverse(heights);
		int maxHeightsSum = heights.get(0) + heights.get(1) + heights.get(2);
		
		int[] result = {aggregateHeight, maxHeightsSum};
		return result;
	}

	/**
	 * Computes the number of holes in the field and the depth contributed by deep holes.
	 * A hole is an empty square that has a filled square somewhere above it.
	 * A deep hole (formed by stacking multiple holes) is 1 column wide and >=3 rows tall.
	 * @return {numberOfHoles, holesDepth}
	 */
	private static double[] computeHolesStats(State nextState) {
		int[] top = nextState.getTop();
		int[][] field = nextState.getField();
		double numberOfHoles = 0;
		double holesDepth = 0;
		
		for (int i = 0; i < top.length; i++) {
			int k = 0;
			int holeLength;
			while (k <= top[i]-2){
				holeLength = 1;
				if (field[k][i] == 0) {
					for (int m = k+1; m < field.length; m++) {
						if (field[m][i] == 0) {
							holeLength++;
						} else {
							break;
						}
					}
					if (holeLength >= 3) { //deep hole
						holesDepth += Math.pow(2, holeLength-1); //holesDepth increases exponentially with depth of hole
					}
					numberOfHoles += holeLength;
				}
				k += holeLength;
			}
		}
		
		return new double[]{numberOfHoles, holesDepth};
	}

	/**
	 * Computes the bumpiness of the columns and the depth contributed by deep wells
	 * Bumpiness is defined by how much the 'roof' of the columns move up and down
	 * A well is an empty square that is flanked by 2 adjacent filled squares and is not a hole
	 * A deep well (formed by stacking multiple wells) is 1 column wide and >=3 rows tall
	 * @return {bumpiness, wellsDepth}
	 */
	private static double[] computeBumpiness(State s) {
		int[] top = s.getTop();
		double bumpiness = 0;
		double wellsDepth = 0;
		
		for (int i = 0; i < top.length -1; i++) {
			
			int rightHeightDifference = Math.abs(top[i+1] - top[i]);
			bumpiness += rightHeightDifference;
			
			if (rightHeightDifference >= 3) {
				if (i > 0) {
					int leftHeightDifference = Math.abs(top[i-1] - top[i]);
					if (top[i+1] > top[i] && top[i-1] > top[i] && leftHeightDifference >= 3){ //deep well in this column
						wellsDepth += Math.pow(2, rightHeightDifference-1); //wellsDepth increases exponentially with depth of well
					}
					
				} else if (i < top.length - 2) {
					int farRightHeightDifference = Math.abs(top[i+2] - top[i+1]);
					if (top[i+1] < top[i] && top[i+1] < top[i+2] && farRightHeightDifference >= 3){ //deep well in the next column
						wellsDepth += Math.pow(2, rightHeightDifference-1); //wellsDepth increases exponentially with depth of hole
					}
				}
			}
		}
		
		if (top[1] > top[0]) { //well in the leftmost column
			int heightDifference = top[1] - top[0];
			bumpiness += heightDifference;
			if (heightDifference >= 3) { //deep well in this column
				wellsDepth += Math.pow(2, heightDifference-1);
			}
		}
		
		if (top[top.length-2] > top[top.length-1]) { //well in the rightmost column
			int heightDifference = top[top.length-2] - top[top.length-1];
			bumpiness += heightDifference;
			if (heightDifference >= 3) { //deep well in this column
				wellsDepth += Math.pow(2, heightDifference-1);
			}
		}
		
		return new double[]{bumpiness, wellsDepth};
	}

	private int printScores(int[][] legalMoves, State nextState, int aggregateHeight,
						double holeScore, double bumpiness, double score, 
						int maxHeightsSum, int orient, int i) {
		
		//check if orientation of current legal move is different from previous
		if (legalMoves[i][State.ORIENT] != orient) { 
			orient = legalMoves[i][State.ORIENT];
			System.out.print("\nOrientation " + (orient+1) + ": "); //if yes, print orientation
		} else if (i != 0){
			System.out.print(", "); //if no, just print the slot
		}
		
		//Print slot
		int slot = legalMoves[i][State.SLOT];
		System.out.print((slot+1));
		
		//Print score & details
		if (nextState.hasLost()) {
			System.out.print("[-1]()");
		} else {
			DecimalFormat df = new DecimalFormat("#.00"); 
			System.out.print("[" + df.format(score) + "](" + aggregateHeight + "/" + df.format(holeScore) + "/" + df.format(bumpiness) + "/" + maxHeightsSum + ")");
		}
		
		return orient;
	}

	private void printMoves(int[][] legalMoves, ArrayList<Integer> lowestScoreMoves, double lowestScore, int bestMove) {
		
		if (lowestScoreMoves.size() > 1) {
			/*Prints out all the moves with the lowest score*/
			System.out.print("\n\nTied moves:");
			for (int i = 0; i < lowestScoreMoves.size(); i++) {
				int move = lowestScoreMoves.get(i);
				System.out.print("\n" + (legalMoves[move][State.ORIENT]+1) + "," + (legalMoves[move][State.SLOT]+1));
			}
		}

		/*Prints out the best move*/
		System.out.println("\n\nBest Move: Orientation " + (legalMoves[bestMove][State.ORIENT]+1) + ", Slot " 
		+ (legalMoves[bestMove][State.SLOT]+1) + " (Score = " + lowestScore + ")\n");
	}

	
	
	
	private static double[] normalizeWeight(double[] weightsDefault) {
		double weightSum = 0;
		for (int j = 0; j < weightsDefault.length; j++) {
			weightSum += weightsDefault[j];
		}
		for (int j = 0; j < weightsDefault.length; j++) {
			weightsDefault[j] = weightsDefault[j] / weightSum;  
		}
		return weightsDefault;
	}

	private static String getDateTime(){
		DateFormat daf = new SimpleDateFormat("ddMMM_HH-mm-ss");
		Date date = new Date();
		return daf.format(date);
	}
	
	private static void writeToFile(String fileName){
		try {
			output = new File(fileName);
			fos = new FileOutputStream(output);
			bos = new BufferedOutputStream(fos);
			ps = new PrintStream(bos);
			System.setOut(ps);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void checkFolderExists(String folderName){
		File dir = new File(folderName);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
	
	private static void closeStreams(){
		try {
			ps.close();
			bos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Mode 1: Play Game!
	 * Runs the program and watch the AI play a game of tetris
	 * The game states will be written to a text file in the GameReport folder.
	 */
	private static void playGame() {
		State s = new State();
		new TFrame(s);
		PlayerSkeletonRufus p = new PlayerSkeletonRufus();
		
		if (writeResultToFile) {
			writeToFile("gameReport.txt");
		}
		
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weightsDefault) + "\n");
		
		while(!s.hasLost()) {
			reportState(s, s.legalMoves());
			s.makeMove(p.pickMove(s, s.legalMoves(), weightsDefault));
			s.draw();
			s.drawNext(0,0);
			/*try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
		reportState(s, s.legalMoves());
		System.out.println("\nYou have completed "+ s.getRowsCleared() +" rows.");
		
		if (writeResultToFile) {
			closeStreams();
			checkFolderExists("GameReports");
			renameOutput("GameReports/gameReport_" + convertWeightsToStringOfIntegers(weightsDefault) + "__[" + s.getRowsCleared() + "]__(" + getDateTime() + ").txt");
		}
	}

	private static String convertWeightsToStringOfDecimals(double[] weights) {
		String s = "";
		s += fourDP.format(weights[0]);
		for (int i = 1; i < weights.length; i++) {
			s += ", " + fourDP.format(weights[i]);
		}
		return s;
	}

	private static String convertWeightsToStringOfIntegers(double[] weights) {
		String s = "";
		s += (int)Math.round(weights[0]*10000);
		for (int i = 1; i < weights.length; i++) {
			s += "-" + (int)Math.round(weights[i]*10000);
		}
		return s;
	}

	private static void renameOutput(String fileName) {
		File outputRenamed = new File(fileName);
		output.renameTo(outputRenamed);
	}

	/**
	 * Mode 2: Standard Fitness Test
	 * Test the fitness of a weight set by running the game n times to obtain the average score.
	 * Writes the result of the fitness test to a text file in the FitnessReports folder.
	 */
	private static void runStandardFitnessTest() {
		runFitnessTest(false);
	}
	
	/**
	 * Mode 3: Timed Fitness Test
	 * Same as standard fitness test, except the game will terminate after the k-th turn.
	 * Writes the result of the fitness test to a text file in the FitnessReports folder.
	 */
	private static void runTimedFitnessTest() {
		runFitnessTest(true);
	}
	
	private static void runFitnessTest(boolean timed) {
		ArrayList<Integer> gameScores = new ArrayList<Integer>();
		printAIthoughts = false;
		if (timed) {
			System.out.println("Max number of turns = " + maxNumberOfTurns);
			//4 = number of squares in a piece, 10 = number of squares in a row
			System.out.println("Max number of rows that can be cleared = " + (maxNumberOfTurns*4/10) + "\n"); 
		} else {
			maxNumberOfTurns = Integer.MAX_VALUE;
		}
		
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weightsDefault));
		
		for (int i = 0; i < numberOfGames; i++) {
			State s = new State();
			PlayerSkeletonRufus p = new PlayerSkeletonRufus();
			
			while(!s.hasLost() && s.getTurnNumber() < maxNumberOfTurns) {
				s.makeMove(p.pickMove(s, s.legalMoves(), weightsDefault));
			}
			System.out.println("Game " + (i+1) + ": " + s.getRowsCleared());
			gameScores.add(s.getRowsCleared());
		}
		double fitnessScore = getAverageScore(gameScores); 
		System.out.println("Average Score: " + fourDP.format(fitnessScore) + "\n");

		if (writeResultToFile) {
			writeFitnessTestResultToFile(gameScores, fitnessScore, timed);
		}
	}

	private static double getAverageScore(ArrayList<Integer> gameScores) {
		double scoreSum = 0;
		
		for (int i = 0; i < gameScores.size(); i++) {
			scoreSum += gameScores.get(i);
		}
		double averageScore = scoreSum / gameScores.size();
		
		return averageScore;
	}
	
	private static void writeFitnessTestResultToFile(ArrayList<Integer> gameScores, double fitnessScore, boolean timed) {
		checkFolderExists("FitnessReports");
		if (timed) {
			writeToFile("FitnessReports/timedFitnessReport_" + convertWeightsToStringOfIntegers(weightsDefault) 
					   + "__" + maxNumberOfTurns + "__[" + (int)(fitnessScore) + "]__(" + getDateTime() + ").txt");
		} else {
			writeToFile("FitnessReports/standardFitnessReport_" + convertWeightsToStringOfIntegers(weightsDefault) 
					   + "__[" + (int)(fitnessScore) + "]__(" + getDateTime() + ").txt");
		}
		
		if (timed) {
			System.out.println("\nMax number of turns = " + maxNumberOfTurns);
			//4 = number of squares in a piece, 10 = number of squares in a row
			System.out.println("Max number of rows that can be cleared = " + (maxNumberOfTurns*4/10) + "\n"); 
		}
		
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weightsDefault));
		
		for (int i = 0; i < gameScores.size(); i++) {
			System.out.println("Game " + (i+1) + ": " + gameScores.get(i));
		}
		
		System.out.println("Average Score: " + fourDP.format(fitnessScore) + "\n");
		
		closeStreams();
	}

	/**
	 * Mode 4: Standard Trial
	 * Generate m random weight sets and run a standard fitness test on each set over n games.
	 * Reports the ranking of the weight sets from highest to lowest score.
	 * Write the result of the trial to a text file in the StandardTrialReports folder.
	 */
	private static void runStandardTrial() {
		runTrial(false);
	}

	/**
	 * Mode 5: Timed Trial (USE THIS FOR RUNNING GENETIC ALGORITHM)
	 * Same as standard trial, except each game will terminate after the k-th turn.
	 * Reports the ranking of the weight sets from highest to lowest score.
	 * Write the result of the trial to a text file in the TimedTrialReports folder.
	 */
	private static void runTimedTrial() {
		runTrial(true);
	}
	
	private static void runTrial(boolean timed) {
		ArrayList<double[]> weightSetsList = new ArrayList<double[]>();
		ArrayList<ArrayList<Integer>> gameScoresList = new ArrayList<ArrayList<Integer>>();
		ArrayList<double[]> fitnessScoresList = new ArrayList<double[]>(); //[fitness score][set index]
		if (!timed) {
			maxNumberOfTurns = Integer.MAX_VALUE;
		}
		printAIthoughts = false;
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			ArrayList<Integer> gameScores = new ArrayList<Integer>();
			
			double[] weights = generateRandomWeightSet();
			System.out.println("Weights Set " + (j+1) + ": " + convertWeightsToStringOfDecimals(weights));
			
			for (int i = 0; i < numberOfGames; i++) {
				State s = new State();
				PlayerSkeletonRufus p = new PlayerSkeletonRufus();
				
				while(!s.hasLost() && s.getTurnNumber() < maxNumberOfTurns) {
					s.makeMove(p.pickMove(s, s.legalMoves(), weights));
				}
				System.out.println("Game " + (i+1) + ": " + s.getRowsCleared());
				gameScores.add(s.getRowsCleared());
			}
			double fitnessScore = getAverageScore(gameScores); 
			System.out.println("Average Score: " + fourDP.format(fitnessScore) + "\n");
			
			weightSetsList.add(weights);
			gameScoresList.add(gameScores);
			fitnessScoresList.add(new double[]{fitnessScore, j});
		}
		
		sortFitnessScores(fitnessScoresList);
		sortWeightSetsList(weightSetsList, fitnessScoresList);
		printTrialRanking(fitnessScoresList, weightSetsList, timed);
		
		//writeWeightSetsToFile(weightSetsList);
		//writeFitnessScoresToFile(fitnessScoresList);
		if (writeResultToFile) {
			writeTrialResultToFile(gameScoresList, fitnessScoresList, weightSetsList, timed);
		}
	}

	private static double[] generateRandomWeightSet() {
		Random rand = new Random();
		double[] weights = new double[numberOfWeightedFactors];
		
		int weightBound = 10000; //sum of weights
		ArrayList<Integer> weightIndex = new ArrayList<Integer>();
		for (int i = 0; i < numberOfWeightedFactors; i++) {
			weightIndex.add(i);
		}
		int k = numberOfWeightedFactors;
		
		for (int i = weights.length; i > 1; i--) {
			int r = rand.nextInt(k); //pick random factor
			weights[weightIndex.get(r)] = rand.nextInt(weightBound-k+1) + 1; //assign random weight
			weightBound -= weights[weightIndex.get(r)]; //remaining unassigned weight
			weightIndex.remove(r); //remove assigned factor
			k--;
		}
		weights[weightIndex.get(0)] = weightBound;
		
		return normalizeWeight(weights);
	}
	
	private static void sortFitnessScores(ArrayList<double[]> fitnessScoresList){
		final Comparator<double[]> fitnessComparator = new Comparator<double[]>() {
	        @Override
	        public int compare(double[] o1, double[] o2) {
	            return Double.compare(o2[0], o1[0]); //rank from highest to lowest score
	        }
	    };
		Collections.sort(fitnessScoresList, fitnessComparator);
	}
	
	private static void sortWeightSetsList(ArrayList<double[]> weightSetsList, ArrayList<double[]> fitnessScoresList){
		ArrayList<double[]> sortedWeightSets = new ArrayList<double[]>();
		for (int i = 0; i < weightSetsList.size(); i++) {
			sortedWeightSets.add(weightSetsList.get((int)fitnessScoresList.get(i)[1]));
		}
		for (int i = 0; i < weightSetsList.size(); i++) {
			weightSetsList.set(i, sortedWeightSets.get(i));
		}
	}
	
	private static void printTrialRanking(ArrayList<double[]> fitnessScoresList, ArrayList<double[]> weightSetsList, boolean timed) {
		if (timed) {
			System.out.println("--------------------------------------------");
			System.out.println("\nMax number of turns = " + maxNumberOfTurns);
			//4 = number of squares in a piece, 10 = number of squares in a row
			System.out.println("Max number of rows that can be cleared = " + (maxNumberOfTurns*4/10)); 
			System.out.print("\nTIMED ");
		} else {
			System.out.print("--------------------------------------------\n\nSTANDARD ");
		}
		
		System.out.println("TOURNAMENT RANKING:");
		for (int i = 0; i < fitnessScoresList.size(); i++) {
			System.out.println((i+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(i)) 
							 + "]: " + fourDP.format(fitnessScoresList.get(i)[0]));
		}
		System.out.println("\n--------------------------------------------\n");
	}

	/*private static void writeWeightSetsToFile(ArrayList<double[]> weightSetsList) {
		Storage storage = new Storage();
		storage.storeWeights(weightSetsList);
	}*/
	
	/*private static void writeFitnessScoresToFile(ArrayList<double[]> fitnessScoresList) {
		Storage storage = new Storage();
		storage.storeFitnessScores(fitnessScoresList);
	}*/
	
	private static void writeTrialResultToFile(ArrayList<ArrayList<Integer>> gameScoresList, ArrayList<double[]> fitnessScoresList, 
			 									    ArrayList<double[]> weightSetsList, boolean timed) {
		if (timed) {
			checkFolderExists("TimedTrialReports");
			writeToFile("TimedTrialReports/timedTrialReport__" + numberOfWeightSets + "-" + numberOfGames + "-" + maxNumberOfTurns 
					 + "__(" + getDateTime() + ").txt");
		} else {
			checkFolderExists("StandardTrialReports");
			writeToFile("StandardTrialReports/standardTrialReport__" + numberOfWeightSets + "-" + numberOfGames + "__(" + getDateTime() + ").txt");
		}
		 
		printTrialRanking(fitnessScoresList, weightSetsList, timed);
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			double[] weights = weightSetsList.get(j);
			System.out.println("Weights Set " + (j+1) + ": " + convertWeightsToStringOfDecimals(weights));
			
			ArrayList<Integer> gameScores = gameScoresList.get((int)fitnessScoresList.get(j)[1]);
			for (int i = 0; i < gameScores.size(); i++) {
				System.out.println("Game " + (i+1) + ": " + gameScores.get(i));
			}
			System.out.println("Average Score: " + fourDP.format(fitnessScoresList.get(j)[0]) + "\n");
		}
		
		closeStreams();
	}

	public static void main(String[] args) {
		if (mode == 1) {
			playGame();
		} else if (mode == 2){
			runStandardFitnessTest();
		} else if (mode == 3){
			runTimedFitnessTest();
		} else if (mode == 4){
			runStandardTrial();
		} else if (mode == 5){
			runTimedTrial();
		} else {
			System.out.println("Invalid game mode!");
			/*FitnessTester ft = new FitnessTester(10, 10, 200);
			ft.createPopulation();
			ft.testOffspringFitness(new double[]{2, 4, 5, 1, 2, 6});*/
		}
	}
	
}