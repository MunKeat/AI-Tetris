import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PlayerSkeletonLocalBeam {
	
	private final static int MILLION = 1000000;
	private final static int MAX_BEAM = 3;
	private final static int LAST_BEAM = 2;
	//private final static int REPLACED_INDEX = 1;
	private double[] weights;
	
	//private static final Logger localBeamLog = Logger.getLogger( PlayerSkeletonLocalBeam.class.getName() );
	
	public PlayerSkeletonLocalBeam(){}
	
	public PlayerSkeletonLocalBeam(double[] weights){
		setWeights(weights);
	}
	
	public void setWeights(double[] weights){
		this.weights = weights;
	}

	//implement this function to have a working system
	public int localBeamMove(State s, int[][] legalMoves) {
		
		//int[] replacedIndex = new int[REPLACED_INDEX];
		//int[] stateNumber = new int[MAX_BEAM];
		double[] beamHeuristic = new double[MAX_BEAM];
		int[] position = new int[MAX_BEAM];
		
		for(int beamIndex=0; beamIndex<MAX_BEAM; ++beamIndex){
			beamHeuristic[beamIndex] = -MILLION;
		}
		
		FutureState[] nextState = new FutureState[MAX_BEAM];
		
		int chosenMove = 0;
		//double lowestBeamHeuristic = -MILLION;
		int nextPiece = s.getNextPiece();
		
		ArrayList<double[]> heurList = new ArrayList<double[]>();
		ArrayList<FutureState> stateList = new ArrayList<FutureState>();
		
		//System.out.println("\n\nPIECE: " + nextPiece);
		//System.out.print("SCORES: ");
		for(int move = 0; move < legalMoves.length; ++move){
			FutureState nextS = new FutureState(s, weights);
			nextS.setBlock(nextPiece);
			
			double heuristic = nextS.getHeuristic(legalMoves[move]);
			heurList.add(new double[]{heuristic, move});
			stateList.add(nextS);
			//System.out.print(heuristic + ", ");
			/*if(heuristic > lowestBeamHeuristic){
				beamHeuristic[LAST_BEAM] = heuristic;
				lowestBeamHeuristic = sort(beamHeuristic,stateNumber,replacedIndex);
				nextState[replacedIndex[0]] = new FutureState(nextS, weights);
				position[replacedIndex[0]] = move;
			}*/
		}	
		
		final Comparator<double[]> heuComparator = new Comparator<double[]>() {
	        @Override
	        public int compare(double[] o1, double[] o2) {
	            return Double.compare(o2[0], o1[0]); //rank from highest to lowest score
	        }
	    };
	    Collections.sort(heurList, heuComparator);
		for (int i = 0; i < MAX_BEAM; i++) {
			nextState[i] = new FutureState(stateList.get((int)heurList.get(i)[1]), weights);
			nextState[i].setBlock(nextPiece);
			position[i] = (int)heurList.get(i)[1];
			nextState[i].makeMove(position[i]);
		}
		
		/*System.out.print("\nBEST MOVES: ");
		for (int i = 0; i < position.length; i++) {
			System.out.print(position[i] + ", ");
		}*/
		
		double[][] totalScores = new double[MAX_BEAM][State.N_PIECES];
		
		for(int beamIndex = 0; beamIndex < MAX_BEAM; ++beamIndex){
			
			FutureState nextNextState = new FutureState(nextState[beamIndex], weights);
			for(int block = 0; block < State.N_PIECES; ++block){
				nextNextState.setBlock(block);
				legalMoves = nextNextState.legalMoves();
				double highestBeamHeuristic = -MILLION;
				
				for(int move = 0; move < legalMoves.length; ++move){
					FutureState nextS = new FutureState(nextNextState, weights);
					//totalScores[beamIndex][block] += nextS.getHeuristic(legalMoves[move]);
					double heuristic = nextS.getHeuristic(legalMoves[move]);
					if (heuristic > highestBeamHeuristic) {
						highestBeamHeuristic = heuristic;
					}
				}
				totalScores[beamIndex][block] += highestBeamHeuristic;
			}
		}
		
		double[] beamTotalScore = new double[MAX_BEAM];
		for(int beamIndex = 0; beamIndex<MAX_BEAM; ++beamIndex){
			ArrayList<Double> nextNextTotals = new ArrayList<Double>();
			for(int index = 0; index < State.N_PIECES; ++index){
				nextNextTotals.add(totalScores[beamIndex][index]);
			}
			Collections.sort(nextNextTotals);
			Collections.reverse(nextNextTotals);
			beamTotalScore[beamIndex] += (nextNextTotals.get(0) + nextNextTotals.get(1) + nextNextTotals.get(2))/3;
			beamTotalScore[beamIndex] += heurList.get(beamIndex)[0];
		}
		
		/*System.out.print("\nBEST SCORES: ");
		for (int i = 0; i < beamTotalScore.length; i++) {
			System.out.print(beamTotalScore[i] + ", ");
		}*/
		
		double score = -MILLION;
		for(int beamIndex = 0; beamIndex<MAX_BEAM; ++beamIndex){
			if(beamTotalScore[beamIndex] > score){
				score = beamTotalScore[beamIndex];
				chosenMove = position[beamIndex];
			}
		}
		
		//System.out.println("\nCHOSEN MOVE: " + chosenMove);
		return chosenMove;
	}
	
	public double sort(double[] beamHeuristic, int[] stateNumber, int[] replacedIndex){
		replacedIndex[0] = LAST_BEAM;
		
		for(int i=0; i<MAX_BEAM; ++i){
			for(int j=0; j<MAX_BEAM; ++j){
				if(beamHeuristic[i] < beamHeuristic[j]){
					/*if(replacedIndex[0] == i){
						replacedIndex[0] = j;
					}else if(replacedIndex[0] == j){
						replacedIndex[0] = i;
					}*/
					double temp = beamHeuristic[j];
					beamHeuristic[j] = beamHeuristic[i];
					beamHeuristic[i] = temp;
					int tempState = stateNumber[j];
					stateNumber[j] = stateNumber[i];
					stateNumber[i] = tempState;
				}
			}
		}
		return beamHeuristic[LAST_BEAM];
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeletonLocalBeam p = new PlayerSkeletonLocalBeam(new double[]{-0.0031, 0.0129, -0.6032, -0.0533, -0.0899, -0.2354, -0.0281});
		while(!s.hasLost()) {
			s.makeMove(p.localBeamMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			/*try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	
}

/**
 * Simulates what a state will be like after making a legal move
 */
class FutureState extends State{
	
	private double[] weight;
	private final static int MILLION = 1000000;
	
	/*Initializes the variables*/
	private int[][] field = new int[ROWS][];
	private int[] top  = new int[COLS];
	private int piece; 
	
	/*Constructor*/
	public FutureState(State s, double[] weight){
		setField(s, weight);
		setTop(s);
		setPiece(s);
	}
	
	private void setWeight(double[] weight) {
		this.weight = weight;
	}

	public void setField(State s, double[] weight){
		setWeight(weight);
		int[][] stateField = s.getField();
		
		for(int i=0; i< ROWS; ++i){
			field[i] = Arrays.copyOf(stateField[i], stateField[i].length);
		}
	}
	
	public void setTop(State s){
		System.arraycopy( s.getTop(), 0, this.top, 0, top.length);
	}
	
	public void setPiece(State s){
		this.piece = s.getNextPiece();
	}
	
	public void setBlock(int block){
		this.piece = block;
	}
	
	private int[][] pWidth = State.getpWidth();
	private int[][] pHeight = State.getpHeight();
	private int[][][] pBottom = State.getpBottom();
	private int[][][] pTop = State.getpTop();
	
	private int numFilledSquares = 1;
	
	/*override*/
	
	public int[][] getField() {
		return this.field;
	}
	public int[] getTop() {
		return this.top;
	}
	public int getPiece() {
		return this.piece;
	}
	
	public void makeMove(int move) {
		this.makeMove(legalMoves[piece][move]);
	}
	public void makeMove(int[] move) {
		this.makeMove(move[ORIENT],move[SLOT]);
	}
	
	public double getHeuristic(int[] move){
		double heuristic = -MILLION;

		int[] linesCompleted = new int[1];
		boolean isContinue = simulateMove(linesCompleted, move);
		if(isContinue){
			
			int[] heightStats = computeHeightsStats();
			double[] holesStats = computeHolesStats();
			double[] bumpinessStats = computeBumpiness();
			
			int aggregateHeight = heightStats[0];
			int numberOfHoles = (int)holesStats[0];
			int bumpiness = (int)bumpinessStats[0];
			int top3HeightsSum = heightStats[1];
			double holesDepth = holesStats[1];
			double wellsDepth = bumpinessStats[1];
			
			heuristic = weight[0] * aggregateHeight + weight[1] * linesCompleted[0]
						+ weight[2] * numberOfHoles + weight[3] * bumpiness + weight[4]
						* top3HeightsSum + weight[5] * holesDepth + weight[6] * wellsDepth;
		}
		
		return heuristic;
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
				field[h][i+slot] = numFilledSquares;
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
	
	private boolean simulateMove(int[] linesComplete, int[] move){
		int location = move[SLOT];
		int orient = move[ORIENT];
		
		//height if the first column makes contact
		int height = top[location]-pBottom[this.piece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[this.piece][orient];++c) {
			height = Math.max(height,top[location+c]-pBottom[this.piece][orient][c]);
		}
			
		//check if game ended
		if(height+pHeight[this.piece][orient] >= ROWS) {		
			return false;
		}

			
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[this.piece][orient]; ++i) {
				
			//from bottom to top of brick
			for(int h = height+pBottom[this.piece][orient][i]; h < height+pTop[this.piece][orient][i]; ++h) {
				field[h][i+location] = 1;
			}
		}
			
		//adjust top
		for(int c = 0; c < pWidth[this.piece][orient]; ++c) {
			top[location+c]=height+pTop[this.piece][orient][c];
		}
			
		linesComplete[0] = 0;
			
		//check for full rows - starting at the top
		for(int r = height+pHeight[this.piece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; ++c) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				++linesComplete[0];
				
				//for each column
				for(int c = 0; c < COLS; ++c) {

					//slide down all bricks
					for(int i = r; i < top[c]; ++i) {
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
	
	/**
	 * Computes the number of holes in the field and the depth contributed by deep holes.
	 * A hole is an empty square that has a filled square somewhere above it.
	 * A deep hole (formed by stacking multiple holes) is 1 column wide and >=3 rows tall.
	 * @return {numberOfHoles, holesDepth}
	 */
	private double[] computeHolesStats() {

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
	private double[] computeBumpiness() {
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
	
	/**
	 * Computes and prints out the height of each column.
	 * Computes and return the aggregate height (sum of all heights) and the max heights sum (sum of the 3 highest columns)
	 * @return {aggregateHeight, maxHeightsSum}
	 */
	private int[] computeHeightsStats() {

		ArrayList<Integer> heights = new ArrayList<Integer>();
		int aggregateHeight = 0; //sum of all column heights
		for (int i = 0; i < top.length; i++) {
			int height = top[i];
			heights.add(height);
			aggregateHeight += height;
			
		}
		
		Collections.sort(heights);
		Collections.reverse(heights);
		int maxHeightsSum = heights.get(0) + heights.get(1) + heights.get(2);
		
		int[] result = {aggregateHeight, maxHeightsSum};
		return result;
	}
}
