import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;


public class FitnessTester {
	
	private int numberOfWeightSets = 0;
	private int numberOfGames = 0;
	private int maxNumberOfTurns = 0;
	
	public FitnessTester(int nWeightSets, int nGames, int nTurns){
		numberOfWeightSets = nWeightSets;
		numberOfGames = nGames;
		maxNumberOfTurns = nTurns;
	}
	
	private boolean writeReportToFile = true; //set as true if you want to save the test report to file
	
	private int numberOfWeightedFactors = 6;
	private DecimalFormat fourDP = new DecimalFormat("0.0000");
	private File output;
	private FileOutputStream fos;
	private BufferedOutputStream bos;
	private PrintStream ps;
	
	/**
	 * Create Population
	 * This is the first step of the genetic algorithm.
	 * (1) Generate nWeightSets number of random weight sets and run a fitness test on each set over nGames number of games.
	 *     Each game will terminate after nTurns number of turns.
	 * (2) Sorts and prints the weight sets from highest to lowest score.
	 * (3) Store the list of weights and list of fitness scores in 2 separate text files (can be retrieved via Storage)
	 * (Optional) Write the population report to a text file in the Reports folder.
	 */
	public void createPopulation(){
		
		ArrayList<double[]> weightSetsList = new ArrayList<double[]>();
		ArrayList<ArrayList<Integer>> gameScoresList = new ArrayList<ArrayList<Integer>>();
		ArrayList<double[]> fitnessScoresList = new ArrayList<double[]>(); //[fitness score][set index]
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			double[] weights = generateRandomWeightSet();
			weightSetsList.add(weights);
		}
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			ArrayList<Integer> gameScores = new ArrayList<Integer>();
			double[] weights = weightSetsList.get(j);
			
			double fitnessScore = runFitnessTest(weights, gameScores);
			gameScoresList.add(gameScores);
			fitnessScoresList.add(new double[]{fitnessScore, j});
		}
		
		sortFitnessScores(fitnessScoresList);
		sortWeightSetsList(weightSetsList, fitnessScoresList);
		printRanking(fitnessScoresList, weightSetsList);
		
		writeWeightSetsToFile(weightSetsList);
		writeFitnessScoresToFile(fitnessScoresList);
		if (writeReportToFile) {
			writePopulationReportToFile(gameScoresList, fitnessScoresList, weightSetsList);
		}
		
	}
	
	/**
	 * Test Offspring Fitness
	 * Run a fitness test on an offspring.
	 * (Optional) Write the test report to a text file in the Reports folder.
	 * @return Fitness score of offspring
	 */
	public double testOffspringFitness(double[] offspring){
		ArrayList<Integer> gameScores = new ArrayList<Integer>();
		offspring = normalizeWeight(offspring);
		
		double fitnessScore = runFitnessTest(offspring, gameScores);
		
		if (writeReportToFile) {
			writeFitnessReportToFile(offspring, gameScores, fitnessScore);
		}
		
		return fitnessScore;
	}
	
	private double runFitnessTest(double[] weights, ArrayList<Integer> gameScores) {
		if (gameScores == null) {
			gameScores = new ArrayList<Integer>();
		}
		
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weights));
		
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

		return fitnessScore;
	}
	
	private double[] normalizeWeight(double[] weightsDefault) {
		double weightSum = 0;
		for (int j = 0; j < weightsDefault.length; j++) {
			weightSum += weightsDefault[j];
		}
		for (int j = 0; j < weightsDefault.length; j++) {
			weightsDefault[j] = weightsDefault[j] / weightSum;  
		}
		return weightsDefault;
	}
	
	private double[] generateRandomWeightSet() {
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
	
	private String convertWeightsToStringOfDecimals(double[] weights) {
		String s = "";
		s += fourDP.format(weights[0]);
		for (int i = 1; i < weights.length; i++) {
			s += ", " + fourDP.format(weights[i]);
		}
		return s;
	}

	private String convertWeightsToStringOfIntegers(double[] weights) {
		String s = "";
		s += (int)Math.round(weights[0]*10000);
		for (int i = 1; i < weights.length; i++) {
			s += "-" + (int)Math.round(weights[i]*10000);
		}
		return s;
	}
	
	private double getAverageScore(ArrayList<Integer> gameScores) {
		double scoreSum = 0;
		
		for (int i = 0; i < gameScores.size(); i++) {
			scoreSum += gameScores.get(i);
		}
		double averageScore = scoreSum / gameScores.size();
		
		return averageScore;
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
	
	private void sortWeightSetsList(ArrayList<double[]> weightSetsList, ArrayList<double[]> fitnessScoresList){
		ArrayList<double[]> sortedWeightSets = new ArrayList<double[]>();
		for (int i = 0; i < weightSetsList.size(); i++) {
			sortedWeightSets.add(weightSetsList.get((int)fitnessScoresList.get(i)[1]));
		}
		for (int i = 0; i < weightSetsList.size(); i++) {
			weightSetsList.set(i, sortedWeightSets.get(i));
		}
	}
	
	private void printRanking(ArrayList<double[]> fitnessScoresList, ArrayList<double[]> weightSetsList) {
		System.out.println("--------------------------------------------");
		System.out.println("\nMax number of turns = " + maxNumberOfTurns);
		//4 = number of squares in a piece, 10 = number of squares in a row
		System.out.println("Max number of rows that can be cleared = " + (maxNumberOfTurns*4/10)); 
		
		System.out.println("\nFITNESS RANKING:");
		for (int i = 0; i < fitnessScoresList.size(); i++) {
			System.out.println((i+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(i)) 
							 + "]: " + fourDP.format(fitnessScoresList.get(i)[0]));
		}
		System.out.println("\n--------------------------------------------\n");
	}
	
	private void writeWeightSetsToFile(ArrayList<double[]> weightSetsList) {
		Storage storage = new Storage();
		storage.storeWeights(weightSetsList);
	}

	/*private void readWeightSetsFromFile() {
		Storage storage = new Storage();
		ArrayList<double[]> weightSetsList = storage.retrieveWeights();
		for (int i = 0; i < weightSetsList.size(); i++) {
			double[] weightSet = weightSetsList.get(i);
			for (int j = 0; j < weightSet.length; j++) {
				System.out.print(weightSet[j]+ " ");
			}
			System.out.println("");
		}
	}*/
	
	private void writeFitnessScoresToFile(ArrayList<double[]> fitnessScoresList) {
		Storage storage = new Storage();
		storage.storeFitnessScores(fitnessScoresList);
	}
	
	/*private void readFitnessScoresFromFile() {
		Storage storage = new Storage();
		ArrayList<Double> fitnessScoresList = storage.retrieveFitnessScores();
		for (int i = 0; i < fitnessScoresList.size(); i++) {
			System.out.println(fitnessScoresList.get(i));
		}
	}*/
	
	private void writePopulationReportToFile(ArrayList<ArrayList<Integer>> gameScoresList, 
						ArrayList<double[]> fitnessScoresList, ArrayList<double[]> weightSetsList) {
		checkFolderExists("Reports");
		writeToFile("Reports/populationReport__" + numberOfWeightSets + "-" + numberOfGames + "-" + maxNumberOfTurns + "__(" + getDateTime() + ").txt");
		 
		printRanking(fitnessScoresList, weightSetsList);
		
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
	
	private void writeFitnessReportToFile(double[] weights, ArrayList<Integer> gameScores, double fitnessScore) {
		checkFolderExists("Reports");
		writeToFile("Reports/fitnessReport_" + convertWeightsToStringOfIntegers(weights) 
				   + "__" + maxNumberOfTurns + "__[" + (int)(fitnessScore) + "]__(" + getDateTime() + ").txt");
			
		System.out.println("\nMax number of turns = " + maxNumberOfTurns);
		//4 = number of squares in a piece, 10 = number of squares in a row
		System.out.println("Max number of rows that can be cleared = " + (maxNumberOfTurns*4/10) + "\n"); 
		
		System.out.println("Weights: " + convertWeightsToStringOfDecimals(weights));
		
		for (int i = 0; i < gameScores.size(); i++) {
			System.out.println("Game " + (i+1) + ": " + gameScores.get(i));
		}
		
		System.out.println("Average Score: " + fourDP.format(fitnessScore) + "\n");
		
		closeStreams();
	}
	
	private String getDateTime(){
		DateFormat daf = new SimpleDateFormat("ddMMM_HH-mm-ss");
		Date date = new Date();
		return daf.format(date);
	}
	
	private void checkFolderExists(String folderName){
		File dir = new File(folderName);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
	
	private void writeToFile(String fileName){
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
	
	private void closeStreams(){
		try {
			ps.close();
			bos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
