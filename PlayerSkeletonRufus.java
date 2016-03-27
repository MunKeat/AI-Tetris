import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;


public class PlayerSkeletonRufus {
	
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
	public int pickMove(State s, int[][] legalMoves) {
		System.out.println("Searching for best move...");
		
		//////*Prints out the legal moves for the next piece*//////
		System.out.println("\nLegal Moves: ");
		
		//Each legal move is represented by (orientation, slot)
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
		///////////////////////////////////////////////////////////
		
		
		////////*Finds the best move based on heuristic*////////
		System.out.print("\nScores:");
		
		/*Initializes data for the next state*/
		State nextState;
		int[] top;
		int[][] field;
		int heightSum = 0;
		int holes = 0;
		int bumpiness = 0;
		double score = 0;
		
		ArrayList<Double> scores = new ArrayList<Double>();
		ArrayList<Integer> maxHeights = new ArrayList<Integer>(); //tiebreaker
		orient = -1; //orientation of current legal move
		slot = -1; //slot of current legal move
		
		/*For each possible next state, calculate its score based on heuristic*/
		for (int i = 0; i < legalMoves.length; i++) {
			
			nextState = new FutureState(s);
			nextState.makeMove(i);
			
			if (nextState.hasLost()) {
				scores.add(Double.MAX_VALUE);
				maxHeights.add(Integer.MAX_VALUE);
				
			} else {
				top = nextState.getTop();
				field = nextState.getField();
				heightSum = 0;
				holes = 0;
				bumpiness = 0;
				int maxHeight = 0;
				
				for (int j = 0; j < top.length; j++) {
					heightSum += top[j];
					
					if (j != top.length-1) {
						bumpiness += Math.abs(top[j+1] - top[j]);
					}
					
					for (int k = top[j]-2; k >= 0; k--) {
						if (field[k][j] == 0) {
							holes++;
						}
					}
					
					if (top[j] > maxHeight) {
						maxHeight = top[j];
					}
					
				}		
				
				score = (0.55)*heightSum + (0.2)*holes + (0.25)*bumpiness;
				scores.add(score);
				maxHeights.add(maxHeight);
			}
			
			/*Prints out the score details of each possible next state*/
			if (legalMoves[i][State.ORIENT] != orient) { //check if orientation of current legal move is different from previous
				orient = legalMoves[i][State.ORIENT];
				System.out.print("\nOrientation " + (orient+1) + ": ");
			} else if (i != 0){
				System.out.print(", ");
			}
			slot = legalMoves[i][State.SLOT];
			System.out.print((slot+1));
			
			if (nextState.hasLost()) {
				System.out.print("[-1]");
			} else {
				DecimalFormat df = new DecimalFormat("#.00"); 
				System.out.print("[" + df.format(score) + "](" + heightSum + "/" + holes + "/" + bumpiness + ")");
			}
		}
		////////////////////////////////////////////////////////
		
		
		//////////*Determine the best best move out of the all the best moves found (tie-breaker)*//////////
		ArrayList<Integer> bestMoves = new ArrayList<Integer>();
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < scores.size(); i++) {
			if (scores.get(i) <= bestScore) {
				if (scores.get(i) < bestScore) {
					bestMoves.clear();
					bestScore = scores.get(i);
				}
				bestMoves.add(i);
			}
		}
		
		int bestMove = 0;
		if (bestMoves.size() == 1) {
			if (bestScore == Integer.MAX_VALUE) {
				bestScore = -1;
			}
			bestMove = bestMoves.get(0);
			
		} else {
			System.out.print("\nMax Height:");
			int lowestMaxHeight = Integer.MAX_VALUE;
			
			for (int i = 0; i < bestMoves.size(); i++) {
				int move = bestMoves.get(i);
				int maxHeight = maxHeights.get(move);
				System.out.print("\n" + (legalMoves[move][State.ORIENT]+1) + "," + (legalMoves[move][State.SLOT]+1) + ": " + maxHeight);
				
				if (maxHeight < lowestMaxHeight) {
					lowestMaxHeight = maxHeight;
					bestMove = move;
				}
			}
		}

		System.out.println("\n\nBest Move: Orientation " + (legalMoves[bestMove][State.ORIENT]+1) + ", Slot " 
		+ (legalMoves[bestMove][State.SLOT]+1) + " (Score = " + bestScore + ")\n\n");
		///////////////////////////////////////////////////////////////////////////////////////////////////
		
		return bestMove;
	}
	
	
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
					field[h][i+slot] = this.squareValue;
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
		//[pieceID][orientation]
		int[][] pWidth = State.getpWidth(); //width of the piece
		int[][] pHeight = State.getpHeight(); //height of the piece
		
		//[pieceID][orientation][column]
		int[][][] pBottom = State.getpBottom(); //row number of the square below the bottom-most filled square in that column
		int[][][] pTop = State.getpTop(); //row number of the top-most filled square in that column
		
		//[row][column]
		int[][] field = s.getField();
		
		//[column]
		int top[] = s.getTop(); //the height of the highest filled square of that column
		
		int turn = s.getTurnNumber();
		int piece = s.getNextPiece();
		int rowsCleared = s.getRowsCleared();
		
		if (s.hasLost()) {
			System.out.println("-------- Game Over! --------\n");
		} else {
			System.out.println("-------------- Turn " + turn + " --------------");
			
			/////////*Prints out the number of rows cleared*/////////
			System.out.println("\nRows cleared = " + rowsCleared);
			/////////////////////////////////////////////////////////
			
			
			////////*Prints out the next piece*////////
			System.out.println("\nNext Piece = " + piece);
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
			///////////////////////////////////////////
		}
		
		
		/////////*Prints out the playing field, and counts the number of holes*/////////
		int holes = 0; //a hole is an empty square that has a filled square somewhere above it
		
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
		////////////////////////////////////////////////////////////////////////////////
		
		
		//////*Prints out the height of each column*//////
		System.out.println("---------------------------------");
		System.out.print("[ ");
		
		int heightSum = 0; //sum of column heights
		int bumpiness = 0;
		int height;
		
		for (int i = 0; i < top.length-1; i++) {
			height = top[i];
			if (height < 10) {
				System.out.print("0");
			}
			heightSum += height;
			System.out.print(height + " ");
			
			bumpiness += Math.abs(top[i+1] - top[i]);
		}
		heightSum += top[top.length-1];
		
		System.out.println("---------------------------------");
		System.out.println("  1  2  3  4  5  6  7  8  9  10");
		//////////////////////////////////////////////////
		
		
		//////*Prints out the stats used in the heuristic*//////
		System.out.println("\nColumn Heights Sum = " + heightSum);
		System.out.println("Number of holes = " + holes);
		System.out.println("Bumpiness = " + bumpiness + "\n");
		////////////////////////////////////////////
	}
	
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		
		try {
			File output = new File("output.txt");
			FileOutputStream fos = new FileOutputStream(output);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			PrintStream ps = new PrintStream(bos);
			System.setOut(ps);
			
			while(!s.hasLost()) {
				reportState(s, s.legalMoves());
				s.makeMove(p.pickMove(s,s.legalMoves()));
				s.draw();
				s.drawNext(0,0);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			reportState(s, s.legalMoves());
			System.out.println("\nYou have completed "+ s.getRowsCleared() +" rows.");
			
			ps.close();
			bos.close();
			fos.close();
			
			DateFormat df = new SimpleDateFormat("ddMMyyHHmmss");
			Date date = new Date();
			
			File outputRenamed;
			if (s.getRowsCleared() < 100) {
				outputRenamed = new File("output0" + s.getRowsCleared() + "_" + df.format(date) + ".txt");
			} else {
				outputRenamed = new File("output" + s.getRowsCleared() + "_" + df.format(date) + ".txt");
			}
			output.renameTo(outputRenamed);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
