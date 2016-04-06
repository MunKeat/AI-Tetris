import java.util.Arrays;
import java.util.logging.Logger;

public class PlayerSkeletonLocalBeam {
	
	private final static int MILLION = 1000000;
	private final static int MAX_BEAM = 4;
	private final static int LAST_BEAM = 3;
	private final static int REPLACED_INDEX = 1;
	
	private static final Logger localBeamLog = Logger.getLogger( PlayerSkeletonLocalBeam.class.getName() );

	//implement this function to have a working system
	public int localBeamMove(State s, int[][] legalMoves) {
		
		int[] replacedIndex = new int[REPLACED_INDEX];
		int[] stateNumber = new int[MAX_BEAM];
		double[] beamHeuristic = new double[MAX_BEAM];
		int[] position = new int[MAX_BEAM];
		
		for(int beamIndex=0; beamIndex<MAX_BEAM; ++beamIndex){
			beamHeuristic[beamIndex] = -MILLION;
		}
		
		FutureState[] nextState = new FutureState[MAX_BEAM];
		
		int chosenMove = 0;
		double lowestBeamHeuristic = -MILLION;
		
		for(int move = 0; move < legalMoves.length; ++move){
			FutureState nextS = new FutureState(s);
			
			double heuristic = nextS.getHeuristic(move);
			
			if(heuristic > lowestBeamHeuristic){
				beamHeuristic[LAST_BEAM] = heuristic;
				lowestBeamHeuristic = sort(beamHeuristic,stateNumber,replacedIndex);
				nextState[replacedIndex[0]] = new FutureState(nextS);
				position[replacedIndex[0]] = move;
			}
		}
		
		double[][] totalScores = new double[MAX_BEAM][s.N_PIECES];
		
		for(int beamIndex = 0; beamIndex < MAX_BEAM; ++beamIndex){
			FutureState nextNextState = new FutureState(nextState[beamIndex]);
			for(int block = 0; block < s.N_PIECES; ++block){
				nextNextState.setBlock(block);
				legalMoves = nextNextState.legalMoves();
				
				for(int move = 0; move < legalMoves.length; ++move){
					FutureState nextS = new FutureState(nextNextState);
					totalScores[beamIndex][block] += nextS.getHeuristic(move);
				}
			}
		}
		
		double[] beamTotalScore = new double[MAX_BEAM];
		for(int beamIndex = 0; beamIndex<MAX_BEAM; ++beamIndex){
			for(int index = 0; index < s.N_PIECES; ++index){
				beamTotalScore[beamIndex] += totalScores[beamIndex][index];
			}
		}
		
		double score = -MILLION;
		for(int beamIndex = 0; beamIndex<MAX_BEAM; ++beamIndex){
			if(beamTotalScore[beamIndex] > score){
				score = beamTotalScore[beamIndex];
				chosenMove = position[beamIndex];
			}
		}
		
		return chosenMove;
	}
	
	
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeletonLocalBeam p = new PlayerSkeletonLocalBeam();
		while(!s.hasLost()) {
			s.makeMove(p.localBeamMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	
}

/**
 * Simulates what a state will be like after making a legal move
 */
class FutureState extends State{
	
	/*Initializes the variables*/
	private int[][] field = new int[ROWS][];
	private int[] top  = new int[COLS];
	private int piece; 
	
	/*Constructor*/
	public FutureState(State s){
		setField(s);
		setTop(s);
		setPiece(s);
	}
	
	/*Copies the data*/
	public void setField(State s){
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
}
