import java.util.Arrays;

public class PlayerSkeletonMingxuan {

	private final int FAIL = -99999;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {

		PreState ps = init(s);

		int choice = bestUtil(ps, legalMoves);
		ps.clearField();
		
		return choice;
	}


	private int bestUtil(PreState ps, int[][] legalMoves) {
		int noOfOrient = legalMoves.length;
		int bestOrient = 0;
		double bestUtilScore = FAIL;
		for (int n = 0; n < noOfOrient; n++) {
			int orient = legalMoves[n][0];
			int slot = legalMoves[n][1];
			double utility = calUtil(ps, orient, slot);
			if (utility > bestUtilScore) {
				System.out.println("updated new best score from " + bestUtilScore + " to " + utility);
				bestUtilScore = utility;
				bestOrient = n;
				System.out.println("now the best orientation is " + bestOrient);
			}
		}
		return bestOrient;
	}

	private double calUtil(PreState ps, int orient, int slot) {
		System.out.println("This piece is " + ps.getNextPiece());
		ps.makeMove(orient, slot);
		if (ps.hasLost()) {
			return FAIL;
		} else {
			int totalH = calH(ps);
			int complete = ps.getRowsCleared();
			int bumpiness = calBump(ps);
			int maxH = maxH(ps);
			double util = -0.3*totalH + complete - 0.5*bumpiness - 0.5*maxH;
			System.out.println("orient and slot are " + orient + " and " + slot + " and utility is " + util);
			return util;
		}
	}

	private int calH(PreState s) {
		int[] eachH = s.getTop();
		int totalH = 0;
		for (int i = 0; i < eachH.length; i++) {
			totalH += eachH[i];
		}
		return totalH;
	}
	
	private int calBump(PreState s) {
		int totalB = 0;
		for (int i = 0; i < s.COLS - 1; i++) {
			int diff = Math.abs(s.getTop()[i] - s.getTop()[i+1]);
			totalB += diff;
		}
		return totalB;
	}
	
	private int maxH(PreState s) {
		int[] height = s.getTop();
		int maxH = 0;
		for (int i = 0; i < s.COLS; i++) {
			maxH = Math.max(maxH, height[i]);
		}
		return maxH;
	}
	
	private PreState init(State s) {
		int[][] customField = new int[State.ROWS][State.COLS];
		for (int i = 0; i < State.ROWS; i++) {
			customField[i] = Arrays.copyOf(s.getField()[i], s.getField()[i].length);
		}
		int nextPiece = s.getNextPiece();
		int rowsCleared = s.getRowsCleared();
		
		int col = customField[0].length;
		int[] top = new int[col];
		for (int j = 0; j < col; j++) {
			for (int k = 0; k < customField.length; k++) {
				if (customField[k][j] != 0) {
					top[j] = k + 1;
				}
			}
		}
		int turn = s.getTurnNumber();
		int[][][] pBottom = State.getpBottom(); 
		int[][] pHeight = State.getpHeight(); 
		int[] pOrient = State.getpOrients();
		int[][][] pTop = State.getpTop(); 
		int[][] pWidth = State.getpWidth();

		PreState ps = new PreState(customField, nextPiece, rowsCleared,
				top, turn, pBottom, pHeight, pOrient,
				pTop, pWidth);
		
		return ps;
	}

	public static void reportState(State s, int[][] legalMoves){
		int[][] pWidth = State.getpWidth();
		int[][] pHeight = State.getpHeight();
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int turn = s.getTurnNumber();
		int piece = s.getNextPiece();

		if (!s.hasLost()) {
			System.out.println("-------------- Turn " + turn + " --------------");
			System.out.println("Next Piece: " + piece);

			//Prints out the next piece
			for(int i = pHeight[piece][0]-1; i >= 0; i--) {
				for(int j = 0; j < pWidth[piece][0]; j++) {
					if (pBottom[piece][0][j] < (i+1) && pTop[piece][0][j] >= (i+1)) {
						turn = turn%100;
						if ((turn+1) < 10) {
							System.out.print("0");
						}
						System.out.print((turn+1) + " ");
					} else {
						System.out.print("   ");
					}
				}
				System.out.println("");
			}
			System.out.println("");
			/*for(int i = pHeight[piece][orient]-1; i >= 0; i--) {
				for(int j = 0; j < pWidth[piece][orient]; j++) {
					System.out.println("[" + (i+1) + " " + (j+1) + " " + pBottom[piece][orient][j] + " " + pTop[piece][orient][j] + "]");
				}
			}*/
		} else {
			System.out.println("-------- Game Over! --------");
		}

		/*Print out the playing field*/
		int[][] field = s.getField();
		System.out.println("Field:");
		int val;
		//Print the hidden row at the top (if any square of this row is occupied, means game over)
		System.out.print("[ ");
		for (int j = 0; j < field[field.length-1].length; j++) { //for each column
			val = field[field.length-1][j]%100;
			if (val == 0) {
				System.out.print("XX "); //empty square
			} else {
				//occupied square (number represents the turn in which the piece is placed)
				if (val < 10) {
					System.out.print("0");
				}
				System.out.print(field[field.length-1][j] + " "); }
		}
		System.out.println("]  <--- Can't touch this");
		//Print the rest of the playing field (all the visible rows)
		for (int i = field.length-2; i >= 0; i--) { //for each row
			System.out.print("[ ");
			for (int j = 0; j < field[i].length; j++) { //for each column
				val = field[i][j]%100;
				if (val == 0) {
					System.out.print("-- "); //empty square
				} else {
					//occupied square (number represents the turn in which the piece is placed)
					if (val < 10) {
						System.out.print("0");
					}
					System.out.print(val + " "); 
				}
			}
			System.out.println("]");
		}

		//Print the 'top' (column height) of each column
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
			//Print the legal moves
			System.out.println("\n\nLegal Moves: ");
			int orient = 0;
			System.out.print("Orientation " + (orient+1) + ": [");
			for (int i = 0; i < legalMoves.length; i++) {
				int slot;
				if (legalMoves[i][0] != orient) {
					orient = legalMoves[i][0];
					System.out.print("]\nOrientation " + (orient+1) + ": [");
				} else if (i != 0){
					System.out.print(", ");
				}
				slot = legalMoves[i][1]+1;
				System.out.print(slot);
			}
			System.out.print("]");
		}
		System.out.print("\n\n\n");
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeletonMingxuan p = new PlayerSkeletonMingxuan();
		while(!s.hasLost()) {
			//reportState(s, s.legalMoves());
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//reportState(s, s.legalMoves());
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

}
