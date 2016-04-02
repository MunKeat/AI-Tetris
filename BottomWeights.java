import java.util.ArrayList;
import java.util.Random;

public class BottomWeights{
	private ArrayList<double[]> newWeights;
	private ArrayList<double[]> originalWeights;
	private ArrayList<Double> results;
	
	private int totalWeights;
	
	private Random rand = new Random();
	
	private Storage storage = new Storage();
	
	public BottomWeights(){}
	
	public BottomWeights(ArrayList<double[]> weights, ArrayList<Double> results){
		this.originalWeights = weights;
		this.results = results;
		totalWeights = originalWeights.size();
	}
	
	public void setWeights(ArrayList<double[]> weights){
		this.originalWeights = weights;
		totalWeights = originalWeights.size();
		this.newWeights = new ArrayList<double[]>();
	}
	
	public void setResults(ArrayList<Double> results){
		this.results = results;
	}
	
	public ArrayList<double[]> removeBottomThirtyPercentAndFill(){
		if(this.originalWeights == null || this.results == null){
			return null;
		}
		sortReverseWeights();
		newWeights = (ArrayList<double[]>) originalWeights.subList(0, 699);
		addRandomWeights();
		saveChildrenWeights(newWeights);
		return newWeights;
	}
	
	private void addRandomWeights(){
		int size = newWeights.get(0).length;
		for(int i=0; i<300; ++i){
			double[] weight = new double[size];
			for(int j=0; j<size; ++j){
				double randomWeight = rand.nextInt(1000000)/1000000.0;
				weight[j] = randomWeight;
			}
			newWeights.add(weight);
		}
	}
	
	private void sortReverseWeights() {
		for(int pivot = 0; pivot<totalWeights; ++pivot){
			int storeIndex = pivot+1;
			for(int i=pivot+1; i<totalWeights; ++i){
				if(results.get(i) > results.get(pivot)){
					swap(i,storeIndex);
					++storeIndex;
				}
			}
			swap(pivot,storeIndex);
		}
	}
	
	private void swap(int i, int j){
		double[] tempArray = originalWeights.get(i);
		Double temp = results.get(i);
		originalWeights.set(i, originalWeights.get(j));
		results.set(i, results.get(j));
		originalWeights.set(j, tempArray);
		results.set(j, temp);
	}

	public ArrayList<double[]> removeBottomThirtyPercentAndFill(ArrayList<double[]> weights, ArrayList<Double> results){
		this.originalWeights = weights;
		this.newWeights = new ArrayList<double[]>();
		this.results = results;
		return removeBottomThirtyPercentAndFill();
	}
	
	public void saveChildrenWeights(ArrayList<double[]> weights){
		storage.storeWeights(weights);
	}
}