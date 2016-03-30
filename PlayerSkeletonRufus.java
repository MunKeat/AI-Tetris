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
import java.util.Random;


public class PlayerSkeletonRufus {
	
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
	 * Just pick a random move!
	 */
	public int pickRandomMove(State s, int[][] legalMoves) {
		Random rand = new Random();
		int nextMove = rand.nextInt(legalMoves.length); 
		System.out.println("Pick Move: Orientation " + (legalMoves[nextMove][0]+1) + ", Slot " + (legalMoves[nextMove][1]+1) + "\n\n");
		return nextMove;
	}
	
	/**
	 * Picks the best next move by finding a move that gives the lowest column height sum in the next state
	 */
	public int pickMove(State s, int[][] legalMoves, double[] weights) {
		
		printLegalMoves(legalMoves);
		int bestMove = computeScores(s, legalMoves, weights);
		
		return bestMove;
	}

	/**
	 * Prints out the legal moves for the next piece
	 */
	private void printLegalMoves(int[][] legalMoves) {
		System.out.println("Legal Moves: ");
		
		/*Each legal move is represented by (orientation, slot)*/
		int orient = -1; //orientation of current legal move
		int slot = -1; //slot of current legal move
		
		/*Iterate through legalMoves, which is ordered by orientation, then slot.*/
		for (int i = 0; i < legalMoves.length; i++) {
			
			if (legalMoves[i][State.ORIENT] != orient) { //check if orientation of current legal move is different from previous
				orient = legalMoves[i][State.ORIENT];
				if (i != 0) {
					System.out.print("]\n");
				}
				System.out.print("Orientation " + (orient+1) + ": [");
				
			} else if (i != 0){
				System.out.print(", ");
			}
			
			slot = legalMoves[i][State.SLOT];
			System.out.print((slot+1));
		}
		System.out.println("]");
		System.out.print("\nScores:");
	}

	/**
	 * Compute the score of each legal move (possible next state) 
	 * using the heuristics: column height sum, hole score, bumpiness, and max height score.
	 * And find the best move which has the lowest score
	 * @return best move
	 */
	private int computeScores(State s, int[][] legalMoves, double[] weights){
		
		/*Initializes data for the next state*/
		State nextState;
		int[] top;
		int[][] field;
		
		ArrayList<Double> scores = new ArrayList<Double>(); //lower the score the better
		ArrayList<Integer> lowestScoreMoves = new ArrayList<Integer>();
		double lowestScore = Double.MAX_VALUE;
		
		int heightSum = 0;
		int holeScore = 0;
		int bumpiness = 0;
		ArrayList<Integer> heights = new ArrayList<Integer>();
		int maxHeightSum = 0;
		double score = 0;
		
		int orient = -1; //orientation of current legal move
		
		for (int i = 0; i < legalMoves.length; i++) {
			
			/*Compute the score of each legal move*/
			nextState = new FutureState(s);
			nextState.makeMove(i);
			heights.clear();
			
			if (nextState.hasLost()) {
				scores.add(Double.MAX_VALUE);
				
			} else {
				top = nextState.getTop();
				field = nextState.getField();
				heightSum = 0;
				holeScore = 0;
				bumpiness = 0;
				
				for (int j = 0; j < top.length; j++) {
					heightSum += top[j];
					
					int k = 0;
					int holeLength;
					while (k <= top[j]-2){
						holeLength = 1;
						if (field[k][j] == 0) {
							for (int m = k+1; m < field.length; m++) {
								if (field[m][j] == 0) {
									holeLength++;
								} else {
									break;
								}
							}
							holeScore += Math.pow(2, holeLength-1); //holeScore increases exponentially with length of hole
						}
						k += holeLength;
					}
					
					if (j < top.length-1) {
						int diff = Math.abs(top[j+1] - top[j]);
						if (diff >= 3 && j > 0) {
							if (top[j+1] > top[j] && top[j-1] > top[j] && Math.abs(top[j-1] - top[j]) >= 3){
								bumpiness += Math.pow(2, diff-1); //gives more weight to deep well (weight increases exponentially)
							} else if (j < top.length - 2) {
								if (top[j+1] < top[j] && top[j+1] < top[j+2] && Math.abs(top[j+2] - top[j+1]) >= 3){
									bumpiness += Math.pow(2, diff-1); //gives more weight to deep well (weight increases exponentially)
								} else {
									bumpiness += diff;
								}
							} else {
								bumpiness += diff;
							}
						} else {
							bumpiness += diff;
						}
					}
					
					heights.add(top[j]);
				}
				
				if (top[1] > top[0]) {
					bumpiness += Math.pow(2, top[1] - top[0] - 1);
				}
				if (top[top.length-2] > top[top.length-1]) {
					bumpiness += Math.pow(2, top[top.length-2] - top[top.length-1] - 1);
				}
				
				Collections.sort(heights);
				Collections.reverse(heights);
				maxHeightSum = heights.get(0) + heights.get(1) + heights.get(2);
				
				score = weights[0]*heightSum + weights[1]*holeScore + weights[2]*bumpiness + weights[3]*maxHeightSum;
				scores.add(score);
				
				/*Find the move(s) that has the lowest score*/
				if (score <= lowestScore) {
					if (score < lowestScore) {
						lowestScore = score;
						lowestScoreMoves.clear();
					}
					lowestScoreMoves.add(i);
				}
			}
			
			orient = printScores(legalMoves, nextState, heightSum, holeScore,
					 bumpiness, score, maxHeightSum, orient, i);
		}

		int bestMove;
		if (lowestScore == Double.MAX_VALUE) {
			lowestScore = -1;
			bestMove = 0;
		} else {
			bestMove = lowestScoreMoves.get(0);
		}
		
		printMoves(legalMoves, lowestScoreMoves, lowestScore, bestMove);
	
		return bestMove;
	}

	private int printScores(int[][] legalMoves, State nextState, int heightSum,
			                int holeScore, int bumpiness, double score, int maxHeightSum, 
			                int orient, int i) {
		
		//check if orientation of current legal move is different from previous
		if (legalMoves[i][State.ORIENT] != orient) { 
			orient = legalMoves[i][State.ORIENT];
			System.out.print("\nOrientation " + (orient+1) + ": "); //if yes, print orientation
		} else if (i != 0){
			System.out.print(", ");
		}
		
		//Print slot
		int slot = legalMoves[i][State.SLOT];
		System.out.print((slot+1));
		
		//Print score & details
		if (nextState.hasLost()) {
			System.out.print("[-1]");
		} else {
			DecimalFormat df = new DecimalFormat("#.00"); 
			System.out.print("[" + df.format(score) + "](" + heightSum + "/" + holeScore + "/" + bumpiness + "/" + maxHeightSum + ")");
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
		+ (legalMoves[bestMove][State.SLOT]+1) + " (Score = " + lowestScore + ")\n\n");
	}

	
	
	
	/**
	 * Report the state of the current turn
	 */
	public static void reportState(State s, int[][] legalMoves){
		
		printTurn(s);
		
		if (!s.hasLost()) {
			printRowsCleared(s);
			printNextPiece(s);
		}
		
		int holes = analyzeField(s);
		int[] heightResult = analyzeColumnHeights(s);
		int heightSum = heightResult[0];
		int maxHeightScore = heightResult[1];
		int bumpiness = analyzeBumpiness(s);
		
		/*Prints out the attributes used in the heuristic*/
		System.out.println("\nColumn Heights Sum = " + heightSum);
		System.out.println("Number of holes = " + holes);
		System.out.println("Bumpiness = " + bumpiness);
		System.out.println("Max Height Score = " + maxHeightScore + "\n");
		
	}

	/**
	 * Prints out the turn number
	 */
	private static void printTurn(State s) {
		if (s.hasLost()) {
			System.out.println("-------- Game Over! --------\n");
		} else {
			int turn = s.getTurnNumber();
			System.out.println("-------------- Turn " + turn + " --------------");
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
	 * Analyzes and prints out the playing field
	 * @return the number of holes
	 */
	private static int analyzeField(State s) {
		
		//[row][column]
		int[][] field = s.getField();
		//[column]
		int top[] = s.getTop(); //the height of the highest filled square of that column
		
		System.out.println("  1  2  3  4  5  6  7  8  9  10");
		System.out.println("---------------------------------");
		
		/*Prints the hidden row at the top (if any square of this row is filled, means game over)*/
		System.out.print("[ ");
		int squareValue;
		
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
		int holes = 0; //a hole is an empty square that has a filled square somewhere above it
		
		for (int i = field.length-2; i >= 0; i--) {
			System.out.print("[ ");
			
			for (int j = 0; j < field[i].length; j++) {
				
				if (field[i][j] == 0) {
					if (top[j] > i) {
						holes++;
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
		
		return holes;
		
	}

	/**
	 * Analyzes and prints out the column height of each column
	 * @return the sum of all column heights
	 */
	private static int[] analyzeColumnHeights(State s) {
		int[] top = s.getTop();
		System.out.println("---------------------------------");
		System.out.print("[ ");
		
		ArrayList<Integer> heights = new ArrayList<Integer>();
		int heightSum = 0; //sum of column heights
		for (int i = 0; i < top.length; i++) {
			int height = top[i];
			if (height < 10) {
				System.out.print("0");
			}
			
			heights.add(height);
			heightSum += height;
			System.out.print(height + " ");
		}
		
		System.out.println("\n---------------------------------");
		System.out.println("  1  2  3  4  5  6  7  8  9  10 ]");
		
		Collections.sort(heights);
		Collections.reverse(heights);
		int maxHeightScore = heights.get(0) + heights.get(1) + heights.get(2);
		
		int[] result = {heightSum, maxHeightScore};
		return result;
	}

	/**
	 * Analyzes the bumpiness of the current state
	 * @return bumpiness
	 */
	private static int analyzeBumpiness(State s) {
		int[] top = s.getTop();
		int bumpiness = 0;
		for (int i = 0; i < top.length -1; i++) {
			int diff = Math.abs(top[i+1] - top[i]);
			if (diff >= 3 && i > 0) {
				if (top[i+1] > top[i] && top[i-1] > top[i] && Math.abs(top[i-1] - top[i]) >= 3){
					bumpiness += Math.pow(2, diff - 1); //gives more weight to deep well (weight increases exponentially)
				} else if (i < top.length - 2) {
					if (top[i+1] < top[i] && top[i+1] < top[i+2] && Math.abs(top[i+2] - top[i+1]) >= 3){
						bumpiness += Math.pow(2, diff-1); //gives more weight to deep well (weight increases exponentially)
					} else {
						bumpiness += diff;
					}
				} else {
					bumpiness += diff;
				}
			} else {
				bumpiness += diff;
			}
		}
		
		if (top[1] > top[0]) {
			bumpiness += Math.pow(2, top[1] - top[0]) - 1;
		}
		if (top[top.length-2] > top[top.length-1]) {
			bumpiness += Math.pow(2, top[top.length-2] - top[top.length-1] - 1);
		}
		
		return bumpiness;
	}

	
	
	/**
	 * Use this main method if you just want to run the game multiple times and get the number of rows cleared
	 * without drawing and saving the game states to file
	 */
	/*public static void main(String[] args) {
		ArrayList<Integer> gameScores = new ArrayList<Integer>();
		int numberOfGames = 15;
		
		double[] weights = {57, 27.5, 11, 4.5};
		
		double weightSum = 0;
		DecimalFormat df = new DecimalFormat("0.000");
		for (int j = 0; j < weights.length; j++) {
			weightSum += weights[j];
		}
		for (int j = 0; j < weights.length; j++) {
			weights[j] = weights[j] / weightSum;  
		}
		
		System.out.println("Weights: " + df.format(weights[0]) + ", " + df.format(weights[1]) + ", " 
				+ df.format(weights[2]) + ", " + df.format(weights[3]));
		System.out.println("\nRows Cleared: ");
		
		for (int i = 0; i < numberOfGames; i++) {
			State s = new State();
			PlayerSkeleton p = new PlayerSkeleton();
			
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s, s.legalMoves(), weights));
			}
			System.out.println("Game " + (i+1) + ": " + s.getRowsCleared());
			gameScores.add(s.getRowsCleared());
		}
		
		double scoreSum = 0;
		for (int i = 0; i < gameScores.size(); i++) {
			scoreSum += gameScores.get(i);
		} 
		System.out.println("\nAverage Score: " + df.format((scoreSum / numberOfGames)) );
		
		try {
			DateFormat daf = new SimpleDateFormat("ddMMM__HH-mm-ss");
			Date date = new Date();
			
			File output = new File("fitness" + (int)Math.round((weights[0]*1000)) + "-" + (int)Math.round((weights[1]*1000)) + "-" 
			+ (int)Math.round((weights[2]*1000)) + "-" + (int)Math.round((weights[3]*1000))  + "__" + (int)(scoreSum / numberOfGames) + "__" + daf.format(date));
			
			FileOutputStream fos = new FileOutputStream(output);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			PrintStream ps = new PrintStream(bos);
			System.setOut(ps);
			
			System.out.println("Weights: " + df.format(weights[0]) + ", " + df.format(weights[1]) + ", " 
					+ df.format(weights[2]) + ", " + df.format(weights[3]));
			System.out.println("\nRows Cleared: ");
			for (int i = 0; i < gameScores.size(); i++) {
				System.out.println("Game " + (i+1) + ": " + gameScores.get(i));
			}
			System.out.println("\nAverage Score: " + df.format((scoreSum / numberOfGames)) );
			
			ps.close();
			bos.close();
			fos.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	/**
	 * Use this main method if you want to write the states of the game to an output file
	 * and see the game being drawn out
	 */
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		
		double[] weights = {57, 27.5, 11, 4.5};
		
		double weightSum = 0;
		DecimalFormat df = new DecimalFormat("0.000");
		for (int j = 0; j < weights.length; j++) {
			weightSum += weights[j];
		}
		
		for (int j = 0; j < weights.length; j++) {
			weights[j] = weights[j] / weightSum;  
		}
		
		try {
			File output = new File("output.txt");
			FileOutputStream fos = new FileOutputStream(output);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			PrintStream ps = new PrintStream(bos);
			System.setOut(ps);
			
			System.out.println("Weights: " + df.format(weights[0]) + ", " + df.format(weights[1]) + ", " 
					+ df.format(weights[2]) + ", " + df.format(weights[3]) + "\n");
			
			while(!s.hasLost()) {
				reportState(s, s.legalMoves());
				s.makeMove(p.pickMove(s, s.legalMoves(), weights));
				s.draw();
				s.drawNext(0,0);
				//try {
					//Thread.sleep(1);
				//} catch (InterruptedException e) {
					//e.printStackTrace();
				//}
			}
			
			reportState(s, s.legalMoves());
			System.out.println("\nYou have completed "+ s.getRowsCleared() +" rows.");
			
			ps.close();
			bos.close();
			fos.close();
			
			DateFormat daf = new SimpleDateFormat("ddMMM__HH-mm-ss");
			Date date = new Date();
			
			File outputRenamed = new File((int)Math.round((weights[0]*100)) + "-" + (int)Math.round((weights[1]*100)) 
			+ "-" + (int)Math.round((weights[2]*100)) + "-" + (int)Math.round((weights[3]*100)) + "__" + s.getRowsCleared()
			+ "__" + daf.format(date));
			output.renameTo(outputRenamed);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
