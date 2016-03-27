import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Random;

public class PlayerSkeletonDaphne {
	
	private static final Logger error = Logger.getLogger( PlayerSkeletonDaphne.class.getName() );
	
	//related to file I/O
	private static final String fileName = "weights.txt";
	private BufferedReader weightsFileReader;
	private BufferedOutputStream weightsFileWriter;
	private int count = 1;
	private static int numCycles = 1;
	
	//related to weights
	private double[] bestWeights;
	private double bestScore;
	private double averageScore;
	private double[] scoreHistory;
	private double[] currentWeights;
	//private double holes = 0;
	private static Random rand = new Random();
	
	//magic strings and numbers
	private static final int MAX_WEIGHTS_BOUNDARY = 100;
	private static final int TOTAL_WEIGHT_PARAMETERS = 4;
	private static final int ROTATION = 0;
	private static final int LOCATION = 1;
	
	public PlayerSkeletonDaphne(){}
	
	//START OF retrieval and storing of private variables
	public int getCount(){
		return count;
	}
	
	public void resetCount(){
		count = 1;
	}
	
	public void incrementCount(){
		++count;
	}
	
	public double[] getBestWeights(){
		return bestWeights;
	}
	
	public void setBestWeights(double[] weights){
		bestWeights = weights;
	}
	
	public void setBestWeights(int index, double weight){
		bestWeights[index]= weight;
	}
	
	public double getAverageScore(){
		return averageScore;
	}
	
	public void setAverageScore(double score){
		averageScore = score;
	}
	
	public double getBestScore(){
		return bestScore;
	}
	
	public void setBestScore(double score){
		bestScore = score;
	}
	
	public double[] getScoreHistory(){
		return scoreHistory;
	}
	
	public void setScoreHistory(double[] history){
		scoreHistory = history;
	}
	
	public void setScoreHistory(int index, double score){
		scoreHistory[index]= score;
	}
	
	public double[] getCurrentWeights(){
		return currentWeights;
	}
	
	public void setCurrentWeights(double[] weights){
		currentWeights = weights;
	}
	
	public void setCurrentWeights(int index, double weight){
		currentWeights[index]= weight;
	}
	//END OF retrieval and storing of private variables

	
	
	
	
	//START OF implement this function to have a working system
	public int[] pickMove(State s, int[][] legalMoves) {
		int[] move = {0,0};
		int numPossibleRotations = State.getpOrients()[s.getNextPiece()];
		int rows = s.getField().length;
		int cols = s.getField()[0].length;
		double bestHueristic = -1000000;
		
		for(int i=0; i<numPossibleRotations; ++i){
			for(int j=0; j<cols-State.getpWidth()[s.getNextPiece()][i]+1; ++j){
				double hueristic = calculateHueristic(s,i,j,rows,cols);
				if(hueristic > bestHueristic){
					bestHueristic = hueristic;
					move[ROTATION] = i;
					move[LOCATION] = j;
					//holes = calculateHoles(field, s.getTop(), rows);
				}
			}
		}

		return move;
	}
	
	private double calculateHueristic(State s, int orient, int location, int rows, int cols){
		double heuristic = 0;
		int[][] fieldCopy = s.getField();
		int[][] field = new int[rows][];
		for(int i=0; i<rows; ++i){
			field[i] = Arrays.copyOf(fieldCopy[i], fieldCopy[i].length);
		}
		int[] top = Arrays.copyOf(s.getTop(), s.getTop().length);
		int[] param = new int[1];
		//printDoubleArray(s.getField()); //debugging purpose
		boolean continueGame = true;
		double holes = calculateHoles(field, top, rows);
		continueGame = simulateMove(orient, location, s, field, top, rows, cols, param);
		if(continueGame){
			for(int i=0; i<TOTAL_WEIGHT_PARAMETERS; ++i){
				heuristic = addLocalParameter(i, s, top, field, heuristic, param, rows, holes);
			}
		}else{
			heuristic = -1000000;
		}
		
		return heuristic;
	}

	private double addLocalParameter(int index, State s, int[] top, int[][] field, double heuristic, int[] param, int rows, double holes) {
		switch(index){
			case 0: //solved
				heuristic += currentWeights[index]*calculateAggregatedHeight(top);
				break;
			case 1: //solved
				heuristic += currentWeights[index]*calculateCompletedLines(param);
				break;
			case 2:
				heuristic += currentWeights[index]*(calculateHoles(field, top, rows)-holes);
				break;
			case 3: //solved
				heuristic += currentWeights[index]*calculateBumpiness(top);
				break;
			case 4:
				heuristic += currentWeights[index]*calculateMaxHeight(top);
				break;
		}
		return heuristic;
	}
	
	private double calculateMaxHeight(int[] top) {
		int tallest = -1;
		for(int i=0; i<top.length; ++i){
			if(top[i] > tallest){
				tallest = top[i];
			}
		}
		return tallest;
	}

	private double calculateAggregatedHeight(int[] top){
		int total = 0;
		for(int i=0; i<top.length; ++i){
			total += top[i];
		}
		//System.out.println("Aggregated Height = " + total);
		return total;
	}
	
	private double calculateCompletedLines(int[] param){
		//System.out.println("#completed lines = " + param[0]);
		return param[0];
	}
	/*
	private double calculateHoles(int[][] field, int[] top, int rows){
		int count = 0;
		for(int i=0; i<top.length; ++i){
			for(int j=0; j<top[i]; ++j){
				if(j < rows - 1 && field[j][i] == 0 && field[j+1][i] > 0){
					++count;
				}
			}
		}
		//System.out.println("#Number of holes = " + count);
		return count;
	}
	*/
	
	private double calculateHoles(int[][] field, int[] top, int rows){
		int numHoles = 0;
		for(int i=0; i<top.length; ++i){
			int count = 0;
			for(int j=0; j<top[i]; ++j){
				if(field[j][i] > 0){
					++count;
				}
			}
			numHoles += top[i]-count;
		}
		//System.out.println("#Number of holes = " + count);
		return numHoles;
	}
	
	/*
	private double calculateHoles(int[][] field, int[] top, int rows){
		int count = 0;
		for(int i=0; i<top.length; ++i){
			boolean block = false;
			for(int j=0; j<top[i]; ++j){
				if(field[j][i] > 0){
					block = true;
				}else{
					if(block){
						++count;
					}
				}
			}
		}
		return count;
	}
	*/

	private double calculateBumpiness(int[] top){
		double total = 0;
		for(int i=0; i<top.length-1; ++i){
			total += Math.abs(top[i]-top[i+1]);
		}
		return total;
	}
	
	/**
	 * Modified from State.java
	 * @param orient
	 * @param location
	 * @param s
	 * @param field
	 * @param top
	 * @return true if there is still move
	 */
	private boolean simulateMove(int orient, int location, State s, int[][] field, int[] top, int rows, int cols, int[] param){
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int[][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		int turn = s.getTurnNumber() + 1;
		int nextPiece = s.getNextPiece();
		//height if the first column makes contact
		int height = top[location]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];++c) {
			height = Math.max(height,top[location+c]-pBottom[nextPiece][orient][c]);
		}
			
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= rows) {		
			return false;
		}

			
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; ++i) {
				
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; ++h) {
				field[h][i+location] = turn;
			}
		}
			
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; ++c) {
			top[location+c]=height+pTop[nextPiece][orient][c];
		}
			
		param[0] = 0;
			
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < cols; ++c) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				++param[0];
				
				//for each column
				for(int c = 0; c < cols; ++c) {

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
	
	
	public static void main(String[] args) {
		PlayerSkeletonDaphne p = new PlayerSkeletonDaphne();
		p.initializeFileRead();
		p.initializeWeights();
		p.closeFileIO();
		while(true){
			p.initializeFileRead();
			p.initializeWeights();
			p.randomizeWeights();
			for(int i=0; i<MAX_WEIGHTS_BOUNDARY; ++i){
				State s = new State();
				//TFrame t = new TFrame(s);
				p.resetCount();
				
				letsPlayGame(s, p);
				p.setScoreHistory(i, s.getRowsCleared());
				//t.dispose(); //close the frame from accumulating
			}
			compareAndStoreResults(p);
			System.out.println(numCycles + ": Average of " + MAX_WEIGHTS_BOUNDARY + " games = " + p.averageScore);
			++numCycles;
			storeWeights(p);
			p.closeFileIO();
		}
	}

	private static void letsPlayGame(State s, PlayerSkeletonDaphne p) {
		while(!s.hasLost()) {
			//reportState(s, s.legalMoves()); //for debugging?
			int[] moves = p.pickMove(s,s.legalMoves());

			//System.out.println("Piece = " + s.getNextPiece() + ", move " + p.getCount() + ": " + moves[0] + " " + moves[1]);
			s.makeMove(moves);
			//s.draw();
			//s.drawNext(0,0);
			p.incrementCount();
			//p.printDoubleArray(s.getField());
			//System.out.println("#Number of holes = " + p.holes);
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("You have completed "+s.getRowsCleared()+" rows in " + p.getCount() + " turns.");
	}
	private void randomizeWeights() {
		/*
		int numWeights = currentWeights.length;
		double[] weights = new double[numWeights];
		for(int i=0; i<numWeights; ++i){
			weights[i] = rand.nextInt(1000000)/1000000.0;
			if(i != 1){
				weights[i] = -weights[i];
			}
		}
		setCurrentWeights(weights);
		
		
		double weight = rand.nextInt(1000000)/1000000.0;
		
		int weightNum = PlayerSkeletonDaphne.numCycles%TOTAL_WEIGHT_PARAMETERS;
		if(weightNum != 1){
			weight = -weight;
		}
		setCurrentWeights(weightNum,weight);
		
		//setCurrentWeights(1,weight);
		*/
	}

	//END OF implement this function to have a working system
	
	
	
	//Helper methods for I/O
	private static void compareAndStoreResults(PlayerSkeletonDaphne p) {
		p.setAverageScore(calculateAverageScore(p));
		if(p.getBestScore() < p.getAverageScore()){
			p.setBestScore(p.getAverageScore());
			p.setBestWeights(p.getCurrentWeights());
		}
	}
	
	private double readBestScore() {
		double bestScore;
		try{
			bestScore = Double.parseDouble(weightsFileReader.readLine());
		}catch(Exception e){
			bestScore = 0;
		}
		return bestScore;
	}
	
	private double[] readBestWeights(){
		double[] bestWeights = new double[TOTAL_WEIGHT_PARAMETERS];
		
		try{
			for(int i=0; i<TOTAL_WEIGHT_PARAMETERS; ++i){
				bestWeights[i] = Double.parseDouble(weightsFileReader.readLine());
			}
		}catch(Exception e){}
		
		return bestWeights;
	}
	
	private static void storeWeights(PlayerSkeletonDaphne p){
		try {
			File f = new File(fileName);
			p.weightsFileWriter = new BufferedOutputStream(new FileOutputStream(f));
			String bestAverageScore = p.getBestScore() + "\n";
			p.weightsFileWriter.write(bestAverageScore.getBytes(),0,bestAverageScore.length());
			p.weightsFileWriter.flush();
			for(int i=0; i<TOTAL_WEIGHT_PARAMETERS; ++i){
				String content = p.getBestWeights()[i] + "\n";
				p.weightsFileWriter.write(content.getBytes(),0,content.length());
				p.weightsFileWriter.flush();
			}
		}catch(IOException e){
			error.log(Level.WARNING, "FAILED TO WRITE WEIGHTS TO FILE");
		}
	}
	
	private static double calculateAverageScore(PlayerSkeletonDaphne p){
		double total = 0;
		for (int i=0; i<MAX_WEIGHTS_BOUNDARY; ++i){
			total += p.getScoreHistory()[i];
		}
		return total/MAX_WEIGHTS_BOUNDARY;
	}
	
	private void initializeFileRead() {
		File file = new File(fileName);
		try{
			weightsFileReader = new BufferedReader(new FileReader(file));
		}catch(FileNotFoundException e){
			error.log(Level.WARNING, "FAILED TO OPEN FILE");
		}
	}
	
	private void initializeWeights() {
		scoreHistory = new double[MAX_WEIGHTS_BOUNDARY];
		bestScore = readBestScore();
		bestWeights = readBestWeights(); //get previous best weights if there is
		currentWeights = Arrays.copyOf(bestWeights, bestWeights.length);
	}

	private void closeFileIO() {
		try{
			if(weightsFileReader != null){
				weightsFileReader.close();
			}
			if(weightsFileWriter != null){
				weightsFileWriter.close();
			}
		}catch(IOException e){
			error.log(Level.WARNING, "FAILED TO CLOSE FILE");
		}catch(NullPointerException npe){
			error.log(Level.WARNING, "READER NOT YET INITIATED");
		}
	}
	
	
	
	
	//Helper methods for debugging
	private void printDoubleArray(int[][] doubleArray){
		for(int i=doubleArray.length-1; i>=0; --i){
			for(int j=0; j<doubleArray[i].length; ++j){
				System.out.print(doubleArray[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	private void printSingleArray(int[] singleArray){
		for(int i=0; i<singleArray.length; ++i){
			System.out.print(singleArray[i] + " ");
		}
		System.out.println();
	}
	
	/**
	 * Debugging printing @@author: Ruo Fan
	 * @param s
	 * @param legalMoves
	 */
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
}
