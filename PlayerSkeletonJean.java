import java.util.Arrays;

public class PlayerSkeletonJean {
	private static final int COLS = State.COLS;
	private static final int ROWS = State.ROWS;

	private int totalHeight;
	private int[] heights;
	private int lines;
	private int holes;
	private int bumps;

	private final double HCO = -0.510066;
	private final double LCO = 0.760666;
	private final double HOCO = -0.35663;
	private final double BCO = -0.184483;

	// implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		int move =0;
		double max = Double.NEGATIVE_INFINITY;
		System.out.println("next piece = " + s.getNextPiece());
		for (int i = 0; i < legalMoves.length; i++) {
			int[][] currentState = new int[ROWS][COLS];
			for(int j=0; j<ROWS; j++){
				currentState[j] = Arrays.copyOf(s.getField()[j], s.getField()[j].length);
			}
			int[][] newState = visualiseMove(s, currentState, legalMoves[i][0], legalMoves[i][1]);
			if (newState!=null) {
				calculateHeight(newState);
				//calculateLines(newState);
				calculateHoles(newState);
				calculateBumps();
				//System.out.println("totalHeight = "+totalHeight);
				//System.out.println("lines ="+lines);
				//System.out.println("holes = "+holes);
				//System.out.println("bumps = "+bumps);
				double heuristics = HCO * totalHeight + LCO * lines + HOCO * holes + BCO * bumps;
				if (heuristics > max) {
					max = heuristics;
					move = i;
				}
			}

		}
		 return move;
	}

	private int[][] visualiseMove(State s, int[][] curr, int orient, int slot) {
		int nextPiece = s.getNextPiece();
		int[][][] pBottom = State.getpBottom();
		int[][] pWidth = State.getpWidth();
		int[][] pHeight = State.getpHeight();
		int[][][] pTop = State.getpTop();
		int turn = s.getTurnNumber() + 1;
		int[] top = Arrays.copyOf(s.getTop(), s.getTop().length);
		
		int height = top[slot] - pBottom[nextPiece][orient][0];
		// for each column beyond the first in the piece
		for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
			height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
		}

		// check if game ended
		if (height + pHeight[nextPiece][orient] >= ROWS) {
			return null;
		}

		// for each column in the piece - fill in the appropriate blocks
		for (int i = 0; i < pWidth[nextPiece][orient]; i++) {

			// from bottom to top of brick
			for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
				curr[h][i + slot] = turn;
			}
		}

		// adjust top
		for (int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot + c] = height + pTop[nextPiece][orient][c];
		}

		int rowsCleared = 0;

		// check for full rows - starting at the top
		for (int r = height + pHeight[nextPiece][orient] - 1; r >= height; r--) {
			// check all columns in the row
			boolean full = true;
			for (int c = 0; c < COLS; c++) {
				if (curr[r][c] == 0) {
					full = false;
					break;
				}
			}
			// if the row was full - remove it and slide above stuff down
			if (full) {
				rowsCleared++;
				// for each column
				for (int c = 0; c < COLS; c++) {

					// slide down all bricks
					for (int i = r; i < top[c]; i++) {
						curr[i][c] = curr[i + 1][c];
					}
					// lower the top
					top[c]--;
					while (top[c] >= 1 && curr[top[c] - 1][c] == 0)
						top[c]--;
				}
			}
		}
		lines = rowsCleared;
		return curr;
	}

	public int calculateHeight(int[][] currentState) {
		heights = new int[COLS];
		totalHeight = 0;
		for (int j = 0; j < COLS; j++) {
			int sum = 0;
			for (int i = ROWS - 1; i >= 0; i--) {
				if (currentState[i][j] > 0) {
					sum = (i + 1);
					heights[j] = sum;
					break;
				}
			}
			totalHeight += sum;
		}
		return totalHeight;
	}

	/*public int calculateLines(int[][] currentState) {
		boolean clear = true;
		for (int i = 0; i < ROWS; i++) {
			lines = 0;
			if (currentState[i][0] > 0) {
				for (int j = 1; j < COLS; j++) {
					if (currentState[i][j] == 0) {
						clear = false;
					}
				}
			}
			if (clear == true) {
				lines++;
			}

		}

		return lines;
	}*/

	public int calculateHoles(int[][] currentState) {
		holes = 0;
		for (int j = 0; j < COLS; j++) {
			for (int i = 0; i < ROWS - 1; i++) {
				// considering consecutive holes in the column as one.
				if (currentState[i][j] == 0 && currentState[i + 1][j] > 0) {
					holes++;
				}
			}
		}
		return holes;
	}

	public int calculateBumps() {
		bumps = 0;
		for (int i = 0; i < COLS - 1; i++) {
			bumps += Math.abs(heights[i] - heights[i + 1]);
		}
		return bumps;
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
		PlayerSkeletonJean p = new PlayerSkeletonJean();
		//s.printLegal();
		while (!s.hasLost()) {
			// reportState(s, s.legalMoves());
			s.makeMove(p.pickMove(s, s.legalMoves()));
			s.draw();
			s.drawNext(0, 0);
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// reportState(s, s.legalMoves());
		System.out.println("You have completed " + s.getRowsCleared() + " rows.");
	}

}
