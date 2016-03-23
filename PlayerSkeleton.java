
public class PlayerSkeleton {

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		
		return 0;
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
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			reportState(s, s.legalMoves());
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		reportState(s, s.legalMoves());
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
