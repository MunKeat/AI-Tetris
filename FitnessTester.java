import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class FitnessTester {
	
	private int numberOfGames = 0;
	private int maxNumberOfTurns = 0;
	private int maxNumberOfRowsCleared = 0;
	
	public FitnessTester(int nGames, int nTurns){
		numberOfGames = nGames;
		maxNumberOfTurns = nTurns;
		maxNumberOfRowsCleared = maxNumberOfTurns*4/10;
	}
	
	private ArrayList<Integer> gameScores;
	private ArrayList<Double> rankedFitnessScoresList;
	
	private DecimalFormat fourDP = new DecimalFormat("0.0000");
	
	/**
	 * Fitness Test
	 * Run a fitness test on a weight set over nGames number of games for max nTurns number of turns.
	 * @return Fitness score of offspring
	 */
	public double runFitnessTest(double[] weights) {
		gameScores = new ArrayList<Integer>();
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weights));
		
		for (int i = 0; i < numberOfGames; i++) {
			State s = new State();
			PlayerSkeletonLocalBeam p = new PlayerSkeletonLocalBeam(weights);
			
			while(!s.hasLost() && s.getTurnNumber() < maxNumberOfTurns) {
				s.makeMove(p.localBeamMove(s, s.legalMoves()));
			}
			System.out.println("Game " + (i+1) + ": " + s.getRowsCleared());
			gameScores.add(s.getRowsCleared());
		}
		double fitnessScore = getAverageScore(gameScores); 
		System.out.println("Average Score: " + fourDP.format(fitnessScore) + "\n");
		
		return fitnessScore;
	}
	
	public ArrayList<Integer> getGameScores(){
		return gameScores;
	}
	
	public void rankWeightSets(ArrayList<double[]> weightSetsList, 
							   ArrayList<double[]> fitnessScoresList, 
							   ArrayList<ArrayList<Integer>> gameScoresList, boolean print) {
		sortFitnessScores(fitnessScoresList);
		sortWeightSets(weightSetsList, fitnessScoresList);
		if (gameScoresList != null) {
			sortGameScores(gameScoresList, fitnessScoresList);
		}
		
		rankedFitnessScoresList = new ArrayList<Double>();
		for (int i = 0; i < fitnessScoresList.size(); i++) {
			rankedFitnessScoresList.add(fitnessScoresList.get(i)[0]);
		}
		
		if (print) {
			printRanking(weightSetsList, rankedFitnessScoresList);
		}
	}

	public ArrayList<Double> getRankedFitnessScores(){
		return rankedFitnessScoresList;
	}

	public void printRanking(ArrayList<double[]> weightSetsList, ArrayList<Double> fitnessScoresList) {
		System.out.println("--------------------------------------------");
		System.out.println("\nMax number of turns = " + maxNumberOfTurns);
		//4 = number of squares in a piece, 10 = number of squares in a row
		System.out.println("Max number of rows that can be cleared = " + maxNumberOfRowsCleared); 
		
		System.out.println("\nFITNESS RANKING:");
		for (int i = 0; i < fitnessScoresList.size(); i++) {
			System.out.println((i+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(i)) 
							 + "]: " + fourDP.format(fitnessScoresList.get(i)));
		}
		System.out.println("\n--------------------------------------------\n");
	}

	private double getAverageScore(ArrayList<Integer> gameScores) {
		double scoreSum = 0;
		
		for (int i = 0; i < gameScores.size(); i++) {
			scoreSum += gameScores.get(i);
		}
		double averageScore = scoreSum / gameScores.size();
		
		return averageScore;
	}
	
	public double getAverageFitness(ArrayList<Double> fitnessScores) {
		double scoreSum = 0;
		
		for (int i = 0; i < fitnessScores.size(); i++) {
			scoreSum += fitnessScores.get(i);
		}
		double averageFitness = scoreSum / fitnessScores.size();
		
		return averageFitness;
	}
	
	private void sortFitnessScores(ArrayList<double[]> fitnessScoresList){
		final Comparator<double[]> fitnessComparator = new Comparator<double[]>() {
	        @Override
	        public int compare(double[] o1, double[] o2) {
	            return Double.compare(o2[0], o1[0]); //rank from highest to lowest score
	        }
	    };
		Collections.sort(fitnessScoresList, fitnessComparator);
	}
	
	private void sortWeightSets(ArrayList<double[]> weightSetsList, ArrayList<double[]> fitnessScoresList){
		ArrayList<double[]> sortedWeightSets = new ArrayList<double[]>();
		for (int i = 0; i < weightSetsList.size(); i++) {
			sortedWeightSets.add(weightSetsList.get((int)fitnessScoresList.get(i)[1]));
		}
		for (int i = 0; i < weightSetsList.size(); i++) {
			weightSetsList.set(i, sortedWeightSets.get(i));
		}
	}
	
	private void sortGameScores(ArrayList<ArrayList<Integer>> gameScoresList, ArrayList<double[]> fitnessScoresList){
		ArrayList<ArrayList<Integer>> sortedWeightSets = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < gameScoresList.size(); i++) {
			sortedWeightSets.add(gameScoresList.get((int)fitnessScoresList.get(i)[1]));
		}
		for (int i = 0; i < gameScoresList.size(); i++) {
			gameScoresList.set(i, sortedWeightSets.get(i));
		}
	}
	
	private String convertWeightsToStringOfDecimals(double[] weights) {
		String s = "";
		s += fourDP.format(weights[0]);
		for (int i = 1; i < weights.length; i++) {
			s += ", " + fourDP.format(weights[i]);
		}
		return s;
	}
}
