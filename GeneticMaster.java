import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


public class GeneticMaster {
	
	private int numberOfWeightSets = 0;
	private int numberOfGames = 0;
	private int maxNumberOfTurns = 0;
	private int maxNumberOfRowsCleared = 0;
	private int numberOfChildWeightSets = 0;
	private int numberOfTopWeightSets = 50;
	
	public GeneticMaster(int nWeightSets, int nGames, int nTurns){
		numberOfWeightSets = nWeightSets;
		numberOfChildWeightSets = nWeightSets*3/10;
		numberOfGames = nGames;
		maxNumberOfTurns = nTurns;
		maxNumberOfRowsCleared = maxNumberOfTurns*4/10;
		fitnessTester = new FitnessTester(numberOfGames, maxNumberOfTurns);
	}
	
	private ArrayList<double[]> weightSetsList = new ArrayList<double[]>(); 
	private ArrayList<Double> fitnessScoresList;
	private FitnessTester fitnessTester;
	private SelectionMutation matchMaker;
	
	private static final String DEFAULT_WEIGHTS_FILE_NAME = "group06Weights.txt";
	private boolean writeReport = true; //set as true if you want to save the test report to file
	
	private int numberOfWeightedFactors = 7;
	private DecimalFormat fourDP = new DecimalFormat("0.0000");
	private File output;
	private FileOutputStream fos;
	private BufferedOutputStream bos;
	private PrintStream ps;
	private Storage storage = new Storage();
	private static final int POPULATIONREPORT = 1;
	private static final int OFFSPRINGREPORT = 2;
	private static final int ADVANCEDTESTREPORT = 3;
	
	/**
	 * Create Population
	 * This is the first step of the genetic algorithm.
	 * (1) Generate nWeightSets number of random weight sets and run a fitness test on each set over nGames number of games.
	 *     Each game will terminate after nTurns number of turns.
	 * (2) Sorts and prints the weight sets from highest to lowest score.
	 * (3) Store the list of weights and list of fitness scores in 2 separate text files (can be retrieved via Storage)
	 * (Optional) Write the population report to a text file in the Reports folder.
	 */
	private void createPopulation(){
		
		ArrayList<ArrayList<Integer>> gameScoresList = new ArrayList<ArrayList<Integer>>();
		ArrayList<double[]> unrankedFitnessScoresList = new ArrayList<double[]>();
		
		System.out.println("Creating population of " + numberOfWeightSets + " weight sets...\n");
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			double[] weights = generateRandomWeightSet();
			weightSetsList.add(weights);
		}
		
		for (int j = 0; j < numberOfWeightSets; j++) {
			double[] weights = weightSetsList.get(j);
			System.out.print((j+1) + ") ");
			double fitnessScore = fitnessTester.runFitnessTest(weights);
			
			gameScoresList.add(fitnessTester.getGameScores());
			unrankedFitnessScoresList.add(new double[]{fitnessScore, j});
		}
		
		fitnessTester.rankWeightSets(weightSetsList, unrankedFitnessScoresList, gameScoresList, true);
		fitnessScoresList = fitnessTester.getRankedFitnessScores();
		
		writePopulationToFile();
		if (writeReport) {
			writeReportToFile(weightSetsList, fitnessScoresList, gameScoresList, POPULATIONREPORT);
		}
		
	}

	private boolean populationExists(){
		File file = new File(DEFAULT_WEIGHTS_FILE_NAME);
		if (!file.exists()) {
			return false;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(DEFAULT_WEIGHTS_FILE_NAME));
			if (br.readLine() == null) {
				br.close();
			    return false;
			} else {
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
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
		
		for (int i = numberOfWeightedFactors; i > 1; i--) {
			int r = rand.nextInt(k); //pick random factor
			weights[weightIndex.get(r)] = rand.nextInt(weightBound-k+1) + 1; //assign random weight
			weightBound -= weights[weightIndex.get(r)]; //remaining unassigned weight
			weightIndex.remove(r); //remove assigned factor
			k--;
		}
		weights[weightIndex.get(0)] = weightBound;
		
		return normalizeWeight(weights);
	}

	private double[] normalizeWeight(double[] weights) {
		double weightSum = 0;
		
		for (int j = 0; j < weights.length; j++) {
			weightSum += weights[j];
		}
		for (int j = 0; j < weights.length; j++) {
			weights[j] = -(Math.round((weights[j] / weightSum) * 10000.0) / 10000.0); //normalize & negate
		}
		if (weights[1] < 0) {
			weights[1] = -weights[1];
		}
		
		return weights;
	}
	
	private void makeOffspring() {
		System.out.println("\nMaking children...\n");
		
		matchMaker = new SelectionMutation(weightSetsList, fitnessScoresList);
		ArrayList<double[]> childWeightSetsList = matchMaker.getChildWeightSets();
		
		ArrayList<ArrayList<Integer>> gameScoresList = new ArrayList<ArrayList<Integer>>();
		ArrayList<double[]> unrankedChildFitnessScoresList = new ArrayList<double[]>();
		
		for (int j = 0; j < numberOfChildWeightSets; j++) {
			double[] weights = normalizeWeight(childWeightSetsList.get(j));
			childWeightSetsList.set(j, weights);
			
			System.out.print((j+1) + ") ");
			double fitnessScore = fitnessTester.runFitnessTest(weights);
			
			gameScoresList.add(fitnessTester.getGameScores());
			unrankedChildFitnessScoresList.add(new double[]{fitnessScore, j});
		}
		
		fitnessTester.rankWeightSets(childWeightSetsList, unrankedChildFitnessScoresList, gameScoresList, false);
		ArrayList<Double> childFitnessScoresList = fitnessTester.getRankedFitnessScores();
		
		/*if (writeReport) {
			writeReportToFile(childWeightSetsList, childFitnessScoresList, gameScoresList, OFFSPRINGREPORT);
		}*/
		
		for (int i = 0; i < numberOfChildWeightSets; i++) {
			weightSetsList.add(childWeightSetsList.get(i));
			fitnessScoresList.add(childFitnessScoresList.get(i));
		}
	}

	private void eliminateWeaklings() {
		System.out.println("\nPOPULATION WITH NEW OFFSPRINGS:");
		printPopulation(weightSetsList, fitnessScoresList);
		
		System.out.println("\nPOPULATION AFTER ELIMINATION:");
		BottomWeights bottomWeights = new BottomWeights(weightSetsList, fitnessScoresList, fitnessTester);
		bottomWeights.removeBottomThirtyPercentAndFill();
		
		weightSetsList = bottomWeights.getNewWeights();
		fitnessScoresList = bottomWeights.getNewScores();
		
		writePopulationToFile();
		printPopulation(weightSetsList, fitnessScoresList);
	}

	private void writePopulationToFile() {
		storage.storeWeights(weightSetsList);
		storage.storeFitnessScores(fitnessScoresList);
		storage.storeSettings(numberOfWeightSets, numberOfGames, maxNumberOfTurns);
	}
	
	private void writeReportToFile(ArrayList<double[]> weightSetsList, 
											 ArrayList<Double> fitnessScoresList, 
											 ArrayList<ArrayList<Integer>> gameScoresList,
											 int reportType) {
		checkFolderExists("Reports");
		int nWeightSets = 0;
		if (reportType == POPULATIONREPORT) {
			writeToFile("Reports/populationReport__" + numberOfWeightSets + "-" + numberOfGames + "-" + maxNumberOfTurns + "__(" + getDateTime() + ").txt");
			System.out.println("POPULATION OF " + numberOfWeightSets + " WEIGHT SETS CREATED.\n");
			nWeightSets = numberOfWeightSets;
		} else if (reportType == OFFSPRINGREPORT) {
			writeToFile("Reports/offspringReport__" + numberOfChildWeightSets + "-" + numberOfGames + "-" + maxNumberOfTurns + "__(" + getDateTime() + ").txt");
			System.out.println(numberOfChildWeightSets + " OFFSPRINGS CREATED.\n");
			nWeightSets = numberOfChildWeightSets;
		} else if (reportType == ADVANCEDTESTREPORT){
			writeToFile("Reports/advancedTestReport__" + numberOfTopWeightSets + "-" + numberOfGames + "-" + maxNumberOfTurns + "__(" + getDateTime() + ").txt");
			System.out.println(numberOfTopWeightSets + " TOP WEIGHT SETS TESTED.\n");
			nWeightSets = numberOfTopWeightSets;
			
			double averageFitnessScore = fitnessTester.getAverageFitness(fitnessScoresList);
			System.out.println("Average Fitness Score: " + averageFitnessScore);
			
			for (int j = 0; j < numberOfTopWeightSets; j++){
				double oldFitnessScore = fitnessScoresList.get(j);
				System.out.print((j+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(j)) + "] old: " + oldFitnessScore);
				if (fitnessScoresList.get(j) >= averageFitnessScore) {
					System.out.print(" new: " + Math.min(maxNumberOfRowsCleared, (oldFitnessScore+0.1)) + " (+0.1)\n");
				} else {
					System.out.print(" new: " + (oldFitnessScore-1) + " (-1)\n");
				}
			}
			
			closeStreams();
			return;
		}
		
		fitnessTester.printRanking(weightSetsList, fitnessScoresList);
		
		for (int j = 0; j < nWeightSets; j++) {
			double[] weights = weightSetsList.get(j);
			System.out.println("Weights Set " + (j+1) + ": " + convertWeightsToStringOfDecimals(weights));
			
			ArrayList<Integer> gameScores = gameScoresList.get(j);
			for (int i = 0; i < gameScores.size(); i++) {
				System.out.println("Game " + (i+1) + ": " + gameScores.get(i));
			}
			System.out.println("Average Score: " + fourDP.format(fitnessScoresList.get(j)) + "\n");
		}
		
		closeStreams();
	}

	private boolean readPopulationFromFile(){
		System.out.println("Retrieving existing population...\n");
		
		int[] settings = storage.retrieveSettings();
		if (!(numberOfWeightSets == settings[0] && numberOfGames == settings[1] 
			&& maxNumberOfTurns == settings[2])) {
			System.out.println("Warning: Settings don't match!");
			System.out.print("Please use the same settings that you used for creating the population:");
			System.out.println(" (" + settings[0] + " weight sets, " + settings[1] + " games, " + settings[2] + " turns)");
			System.out.println("Otherwise, delete group06weights.txt and re-run the program to create a new population.");
			return false;
		}
		
		weightSetsList = storage.retrieveWeights();
		fitnessScoresList = storage.retrieveFitnessScores();
		printPopulation(weightSetsList, fitnessScoresList);
		
		return true;
	}

	private void printPopulation(ArrayList<double[]> weightSetsList, ArrayList<Double> fitnessScoresList) {
		if (fitnessScoresList == null) {
			for (int i = 0; i < weightSetsList.size(); i++) {
				System.out.println((i+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(i)) 
								   + "]");
			}
		} else {
			for (int i = 0; i < weightSetsList.size(); i++) {
				System.out.println((i+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(i)) 
								   + "]: " + fourDP.format(fitnessScoresList.get(i)));
			}
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
	
	/*private String convertWeightsToStringOfIntegers(double[] weights) {
		String s = "";
		s += (int)Math.round(weights[0]*10000);
		for (int i = 1; i < weights.length; i++) {
			s += "-" + (int)Math.round(weights[i]*10000);
		}
		return s;
	}*/
	
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
	
	public static void main(String[] args) {
		GeneticMaster geneticMaster = new GeneticMaster(250, 15, 250);
		if (!geneticMaster.populationExists()) {
			geneticMaster.createPopulation();
		} else {
			boolean isSameSettings = geneticMaster.readPopulationFromFile();
			if (isSameSettings) {
				for (int i = 0; i < 1; i++) {
					geneticMaster.makeOffspring();
					geneticMaster.eliminateWeaklings();
				}
				//geneticMaster.advancedTest(30, 3000);
			}
		}
	}

	/*private void advancedTest(int nGames, int nTurns) {
		
		FitnessTester advancedFitnessTester = new FitnessTester(nGames, nTurns);
		
		ArrayList<ArrayList<Integer>> gameScoresList = new ArrayList<ArrayList<Integer>>();
		//ArrayList<double[]> unrankedFitnessScoresList = new ArrayList<double[]>();
		ArrayList<Double> unrankedFitnessScoresList = new ArrayList<Double>();
		ArrayList<double[]> topWeightSetsList = new ArrayList<double[]>();
		
		System.out.println("\nTesting top " + numberOfTopWeightSets + " weight sets...\n");
		
		for (int j = 0; j < numberOfTopWeightSets; j++) {
			double[] weights = weightSetsList.get(j);
			topWeightSetsList.add(weights);
			System.out.print((j+1) + ") ");
			double fitnessScore = advancedFitnessTester.runFitnessTest(weights);
			
			gameScoresList.add(advancedFitnessTester.getGameScores());
			//unrankedFitnessScoresList.add(new double[]{fitnessScore, j});
			unrankedFitnessScoresList.add(fitnessScore);
		}
		
		double averageFitnessScore = fitnessTester.getAverageFitness(unrankedFitnessScoresList);
		System.out.println("Average Fitness Score: " + averageFitnessScore);
		for (int j = 0; j < numberOfTopWeightSets; j++){
			double oldFitnessScore = fitnessScoresList.get(j);
			System.out.print((j+1) + ") [" + convertWeightsToStringOfDecimals(weightSetsList.get(j)) + "] old: " + oldFitnessScore);
			if (unrankedFitnessScoresList.get(j) >= averageFitnessScore) {
				System.out.print(" new: " + Math.min(maxNumberOfRowsCleared, (oldFitnessScore+0.1)) + " (+0.1)\n");
				fitnessScoresList.set(j, oldFitnessScore+0.1);
			} else {
				System.out.print(" new: " + (oldFitnessScore-1) + " (-1)\n");
				fitnessScoresList.set(j, oldFitnessScore-1);
			}
		}
		writePopulationToFile();
		
		if (writeReport) {
			writeReportToFile(topWeightSetsList, unrankedFitnessScoresList, gameScoresList, ADVANCEDTESTREPORT);
		}
		
	}*/
}
