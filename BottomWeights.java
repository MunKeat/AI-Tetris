import java.util.ArrayList;

public class BottomWeights{
	private ArrayList<double[]> originalWeights;
	private ArrayList<Double> results;
	ArrayList<double[]> unrankedFitnessScoresList = new ArrayList<double[]>();
	FitnessTester fitnessTester;
	
	private int totalWeights;
	private int populationSize;
	
	ArrayList<double[]> newWeights = new ArrayList<double[]>();
	ArrayList<Double> newScores = new ArrayList<Double>();
	
	//private Random rand = new Random();
	
	private Storage storage = new Storage();
	
	public BottomWeights(){}
	
	public BottomWeights(ArrayList<double[]> weights, ArrayList<Double> results, FitnessTester fitnessTester){
		this.originalWeights = weights;
		this.results = results;
		for (int i = 0; i < results.size(); i++) {
			unrankedFitnessScoresList.add(new double[]{results.get(i), i});
		}
		totalWeights = originalWeights.size();
		populationSize = totalWeights*10/13;
		this.fitnessTester = fitnessTester;
	}
	
	public void setWeights(ArrayList<double[]> weights){
		this.originalWeights = weights;
		totalWeights = originalWeights.size();
	}
	
	public void setResults(ArrayList<Double> results){
		this.results = results;
	}
	
	/*public ArrayList<double[]> removeBottomThirtyPercentAndFill(){
		if(this.originalWeights == null || this.results == null){
			return null;
		}
		
		sortReverseWeights();
		addRandomWeights();
		saveChildrenWeights(originalWeights);
		
		return originalWeights;
	}*/
	
	public void removeBottomThirtyPercentAndFill(){
		if(this.originalWeights == null || this.results == null){
			return;
		}
		
		//sortReverseWeights();
		fitnessTester.rankWeightSets(originalWeights, unrankedFitnessScoresList, null, false);
		results = fitnessTester.getRankedFitnessScores();
		
		for (int i = 0; i < populationSize; i++) {
			newWeights.add(originalWeights.get(i));
			newScores.add(results.get(i));
		}
	}
	
	/*private void addRandomWeights(){
		int size = originalWeights.get(0).length;
		for(int i=700; i<1000; ++i){
			double[] weight = new double[size];
			for(int j=0; j<size; ++j){
				double randomWeight = rand.nextInt(1000000)/1000000.0;
				weight[j] = randomWeight;
			}
			originalWeights.set(i, weight);
		}
	}*/
	
	/*private void sortReverseWeights() {
		for(int pivot = 0; pivot<totalWeights-1; ++pivot){
			int storeIndex = pivot+1;
			for(int i=pivot+1; i<totalWeights; ++i){
				if(results.get(i) > results.get(pivot)){
					swap(i,storeIndex);
					++storeIndex;
				}
			}
			swap(pivot,storeIndex-1);
		}
	}*/
	
	/*private void swap(int i, int j){
		//System.out.println("i = " + i + ", j = " + j);
		double[] tempArray = originalWeights.get(i);
		Double temp = results.get(i);
		originalWeights.set(i, originalWeights.get(j));
		results.set(i, results.get(j));
		originalWeights.set(j, tempArray);
		results.set(j, temp);
	}*/

	/*public ArrayList<double[]> removeBottomThirtyPercentAndFill(ArrayList<double[]> weights, ArrayList<Double> results){
		this.originalWeights = weights;
		this.results = results;
		return removeBottomThirtyPercentAndFill();
	}*/
	
	public void saveChildrenWeights(ArrayList<double[]> weights){
		storage.storeWeights(weights);
	}
	
	public ArrayList<double[]> getNewWeights(){
		return newWeights;
	}
	
	public ArrayList<Double> getNewScores(){
		return newScores;
	}
	
	/*
	public static void main(String args[]){
		ArrayList<double[]> w = new ArrayList<double[]>();
		ArrayList<Double> r = new ArrayList<Double>();
		Random rand = new Random();
		for(int i=0; i<1000; ++i){
			double[] w2 = new double[1];
			w2[0] = rand.nextDouble();
			w.add(w2);
			r.add((Double) (rand.nextInt(1000)/100.0));
		}
		BottomWeights bw = new BottomWeights(w,r);
		bw.removeBottomThirtyPercentAndFill();
		for(int i=0; i<1000; ++i){
			System.out.println(bw.results.get(i));
		}		
	}
	*/
}