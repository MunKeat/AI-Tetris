import java.util.ArrayList;
import java.util.Random;

public class SelectionMutation {

	private ArrayList<double[]> weightSetsList;
	private ArrayList<Double> fitnessScoreList;
	private ArrayList<double[]> childWeightSets;
	private ArrayList<Double> childFitnessScores;
	private ArrayList<double[]> newPopulation;
	private Random rand = new Random();

	public SelectionMutation(ArrayList<double[]> weightSetsList, ArrayList<Double> fitnessScoreList) {
		this.weightSetsList = weightSetsList;
		this.fitnessScoreList = fitnessScoreList;
		produceOffspring();
	}

	private void produceOffspring() {
		childWeightSets = new ArrayList<double[]>();
		childFitnessScores = new ArrayList<Double>();
		tournamentSelection();
	}

	private void tournamentSelection() {
		// use percentage instead of 1000, 100 and 300 to avoid future changes in population size
		int population = weightSetsList.size();
		int sizeOfEachBracket = (int) (0.1 * population);
		int numberOfOffspring = (int) (0.3 * population);

		int i;
		for (i = 0; i < numberOfOffspring; i++) {
			System.out.print((i+1) + ") ");
			ArrayList<double[]> twoParents = findingBestTwo(population, sizeOfEachBracket);
			double[] offspring = newOffspring(twoParents);
			childWeightSets.add(offspring);
		}

		newPopulation = new ArrayList<double[]>();
		newPopulation.addAll(weightSetsList);
		newPopulation.addAll(childWeightSets);
		
	}

	// find the best two parents out of each bracket
	private ArrayList<double[]> findingBestTwo(int population, int sizeOfEachBracket) {

		double[] bestTwoScores = new double[2];
		bestTwoScores[0] = 0;
		bestTwoScores[1] = -1;
		ArrayList<double[]> bestTwoWeightSets = new ArrayList<double[]>();
		bestTwoWeightSets.add(0, new double[6]);
		bestTwoWeightSets.add(1, new double[6]);
		int i;
		System.out.print("Bracket: ");
		for (i = 0; i < sizeOfEachBracket; i++) {
			int position = rand.nextInt(population);
			double fitnessScore = fitnessScoreList.get(position);
			System.out.print(fitnessScore + " ");
			if (fitnessScore > bestTwoScores[0]) {
				bestTwoScores[1] = bestTwoScores[0];
				bestTwoScores[0] = fitnessScore;
				bestTwoWeightSets.set(1, bestTwoWeightSets.get(0));
				bestTwoWeightSets.set(0, weightSetsList.get(position));
			} else if (fitnessScore > bestTwoScores[1]) {
				bestTwoScores[1] = fitnessScore;
				bestTwoWeightSets.set(1, weightSetsList.get(position));
			}
		}
		System.out.println("\nBest 2 Scores: " + bestTwoScores[0] + ", " + bestTwoScores[1]);
		System.out.print("Parent Weights: [");
		for (int j = 0; j < 5; j++) {
			System.out.print(bestTwoWeightSets.get(0)[j] + ", ");
		}
		System.out.print(bestTwoWeightSets.get(0)[5] + "], [");
		for (int j = 0; j < 5; j++) {
			System.out.print(bestTwoWeightSets.get(1)[j] + ", ");
		}
		System.out.print(bestTwoWeightSets.get(1)[5] + "]");
		
		ArrayList<double[]> parentsWithFitnessScores = new ArrayList<double[]>();
		parentsWithFitnessScores.addAll(bestTwoWeightSets);
		parentsWithFitnessScores.add(2, bestTwoScores);
		return parentsWithFitnessScores;
	}

	// returns the weight set of the child 
	private double[] newOffspring(ArrayList<double[]> parents) {
		double[] offspringWeightSet = new double[6];
		double[] parentsFitnessScores = parents.get(2);
		double parentAScore = parentsFitnessScores[0];
		double parentBScore = parentsFitnessScores[1];
		double[] parentAWeight = parents.get(0);
		double[] parentBWeight = parents.get(1);
		int i;
		for (i = 0; i < offspringWeightSet.length; i++) {
			offspringWeightSet[i] = (parentAScore/ (parentAScore+parentBScore)) * parentAWeight[i]
					+ (parentBScore/ (parentAScore+parentBScore)) * parentBWeight[i];
		}
		System.out.print("\nChild Weight: ");
		for (i = 0; i < offspringWeightSet.length-1; i++) {
			System.out.print(offspringWeightSet[i] + ", ");
		}
		System.out.println(offspringWeightSet[5] + "]\n");
		
		// 5% chance for mutation to occur
		if (rand.nextInt(20) == 0) {
			offspringWeightSet = mutation(offspringWeightSet);
		}

		return offspringWeightSet;
	}

	private double[] mutation(double[] offspringWeight) {
		System.out.print("MUTATION! [");
		double[] mutatedOffspring = offspringWeight;
		int selectAttribute = rand.nextInt(6);
		int sign = (int) Math.pow(-1, rand.nextInt(2) + 1);
		// scale of mutation is limited within the range of +- 0.2
		double mutatedAttribute = offspringWeight[selectAttribute] + sign * 0.2 * Math.random();
		mutatedOffspring[selectAttribute] = mutatedAttribute;
		for (int i = 0; i < offspringWeight.length-1; i++) {
			System.out.print(offspringWeight[i] + ", ");
		}
		System.out.println(offspringWeight[5] + "]");
		return mutatedOffspring;
	}
	
	public ArrayList<double[]> getChildWeightSets() {
		return this.childWeightSets;
	}
	
	public ArrayList<Double> getChildFitnessScores() {
		return this.childFitnessScores;
	}
	
	public ArrayList<double[]> getNewPopulation() {
		return this.newPopulation;
	}
}
