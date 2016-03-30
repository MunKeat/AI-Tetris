import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerSkeletonMunKeatTrainer {
    private final static String DATASET_START = "DATASET BEGIN";
    private final static String DATASET_END = "DATASET END";
    private final static String DATASET_FILE = "mk-weights.txt";
    private final static String IMMORTALITY_TAG = "*";
    private final static Double IMMORTALITY_TAG_IN_ARRAYLIST = Double.POSITIVE_INFINITY;

    private final static int NUMBER_OF_PARAMETER = 4;
    private final static int LEARNING_ROUNDS = 1000;
    private final static double REMOVAL = 0.6;
    private final static int MAXIMUM_NUMBER_DATA_SET = 1000;
    private final static double MUTATION_RATE = 0.25;
    private final static double MUTATION_DEGREE = 0.001;
    
    private final DecimalFormat decimalFormatter = new DecimalFormat("000000.00000");

    public void geneticAlgorithm() throws Exception {
        // Check if file exist. If not, create new file.
        // Seed with new, random value if empty
        ArrayList<ArrayList<Double>> contentValue = checkIfDatasetFileExist();

        System.out.println("Experiment will now commence...");
        // Run experiment, get statistics if not done so
        runExperiment(contentValue);
        // Sort, cull 60% of the weakest, excluding those with *
        populationReduction(contentValue);
        // Reproduce to make 50%, remaining 10% shall be randomly generated
        reproduce(contentValue, 0.5);
        
        System.out.println("End!");
    }
    
    private void populationReduction(ArrayList<ArrayList<Double>> contentValue) throws FileNotFoundException {
        Collections.sort(contentValue, new Comparator<ArrayList<Double>>() {
            @Override
            public int compare(ArrayList<Double> first, ArrayList<Double> second) {
                Double firstMeanValue = 0.0;
                Double secondMeanValue = 0.0;
                Double firstScaledVariance = 0.0;
                Double secondScaledVariance = 0.0;

                if (first.size() >= 8) {
                    firstMeanValue = first.get(6);
                    firstScaledVariance = first.get(7);
                }

                if (second.size() >= 8) {
                    secondMeanValue = second.get(6);
                    secondScaledVariance = second.get(7);
                }

                if(secondMeanValue == firstMeanValue) {
                    return secondScaledVariance.compareTo(firstScaledVariance);
                } else {
                    return secondMeanValue.compareTo(firstMeanValue);
                }
            }
        });
        
        printArrayList(contentValue);
        
        int numberOfParametersToRemove = (int) (contentValue.size() * REMOVAL);
        
        for(int i = contentValue.size(); i >= numberOfParametersToRemove ; i--) {
            if(!contentValue.get(numberOfParametersToRemove).contains(IMMORTALITY_TAG_IN_ARRAYLIST)) {
                contentValue.remove(numberOfParametersToRemove);
            } else {
                continue;
            }
        }
        
        printArrayList(contentValue);
        
    }
    
    private void reproduce(ArrayList<ArrayList<Double>> contentValue, double percentageToPopulate) throws Exception {
        int numbersToPopulate = (int) (percentageToPopulate * contentValue.size());
        
        for(int initalCount = 0 ; initalCount < numbersToPopulate ; initalCount++) {
            appendToArrayListContent(contentValue, generateChildValues(contentValue));
        }
        
        printArrayList(contentValue);
    }
    
    private double[] generateChildValues(ArrayList<ArrayList<Double>> contentValue) throws Exception {
        //TODO
        double[] childValue = new double[NUMBER_OF_PARAMETER];
        int length = contentValue.size();
        
        int indexOfParentOne = ThreadLocalRandom.current().nextInt(0, length);
        int indexOfParentTwo = ThreadLocalRandom.current().nextInt(0, length);
        
        //TODO
        //Generate two
        ArrayList<Double> parentOne = contentValue.get(indexOfParentOne);
        ArrayList<Double> parentTwo = contentValue.get(indexOfParentTwo);
        
        if(parentOne.size() <= NUMBER_OF_PARAMETER) {
            runExperiment(contentValue, parentOne);
        }
        
        if(parentTwo.size() <= NUMBER_OF_PARAMETER) {
            runExperiment(contentValue, parentTwo);
        }
        
        for(int i = 0; i < 4; i++) {
            double totalRoundsSolvedByParents = parentOne.get(NUMBER_OF_PARAMETER + 3) + parentTwo.get(NUMBER_OF_PARAMETER + 3);
            childValue[i] = (parentOne.get(NUMBER_OF_PARAMETER + 3) * parentOne.get(i) / totalRoundsSolvedByParents)
                            + (parentTwo.get(NUMBER_OF_PARAMETER + 3) * parentTwo.get(i) / totalRoundsSolvedByParents);
        }
        
        return childValue;
    }
    
    private void runExperiment(ArrayList<ArrayList<Double>> contentValue) throws Exception {  
        Collections.sort(contentValue, new Comparator<ArrayList<Double>>() {
            @Override
            public int compare(ArrayList<Double> first, ArrayList<Double> second) {
                Double firstMeanValue = 0.0;
                Double secondMeanValue = 0.0;
                Double firstScaledVariance = 0.0;
                Double secondScaledVariance = 0.0;

                if (first.size() >= 8) {
                    firstMeanValue = first.get(6);
                    firstScaledVariance = first.get(7);
                }

                if (second.size() >= 8) {
                    secondMeanValue = second.get(6);
                    secondScaledVariance = second.get(7);
                }

                if(secondMeanValue == firstMeanValue) {
                    return secondScaledVariance.compareTo(firstScaledVariance);
                } else {
                    return secondMeanValue.compareTo(firstMeanValue);
                }
            }
        });
        
        
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);
        
        for (ArrayList<Double> dataset : contentValue) {
            int index = contentValue.indexOf(dataset);
            
            System.out.println("Running experiment #" + index + "..." );
            
            if(dataset.size() >= NUMBER_OF_PARAMETER + 4) {
                continue;
            }
            
            int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
            double[] parameter = extractParameters(dataset);
            ArrayList<Double> valuesCollected = new ArrayList<Double>();
            
            List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
            for (int experimentCount = 0; experimentCount < LEARNING_ROUNDS; experimentCount++) {
                
                Callable<Integer> callable = new Callable<Integer>() {
                    public Integer call() throws Exception {
                        State s = new State();
                        PlayerSkeletonMunKeat p = new PlayerSkeletonMunKeat(parameter);
                        while (!s.hasLost()) {
                            p.pickMove(s);
                        }

                        int rowCleared = s.getRowsCleared();
                        
                        System.gc();
                        return rowCleared;
                    }
                };
                futures.add(service.submit(callable));                
            }
            
            for (Future<Integer> future : futures) {
                int value = future.get();
                lowest = lowest > value ? value : lowest;
                highest = highest < value ? value : highest;
                valuesCollected.add((double) value);
            }
            
            double mean = getMean(valuesCollected);
            double scaledSD = getScaledStandardDeviation(valuesCollected, mean);
                  
            dataset.add((double)lowest);
            dataset.add((double)highest);
            dataset.add(mean);
            dataset.add(scaledSD);
            
            printArrayList(contentValue);
        }
        
        service.shutdown();
    }
    
    private void runExperiment(ArrayList<ArrayList<Double>> contentValue, ArrayList<Double> parameter) throws Exception {  
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);

        if (parameter.size() >= NUMBER_OF_PARAMETER + 4) {
            return;
        }

        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        double[] parametersInArray = extractParameters(parameter);
        ArrayList<Double> valuesCollected = new ArrayList<Double>();

        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
        for (int experimentCount = 0; experimentCount < LEARNING_ROUNDS; experimentCount++) {

            Callable<Integer> callable = new Callable<Integer>() {
                public Integer call() throws Exception {
                    State s = new State();
                    PlayerSkeletonMunKeat p = new PlayerSkeletonMunKeat(parametersInArray);
                    while (!s.hasLost()) {
                        p.pickMove(s);
                    }

                    int rowCleared = s.getRowsCleared();

                    return rowCleared;
                }
            };
            futures.add(service.submit(callable));

            for (Future<Integer> future : futures) {
                int value = future.get();
                lowest = lowest > value ? value : lowest;
                highest = highest < value ? value : highest;
                valuesCollected.add((double) value);
            }

            double mean = getMean(valuesCollected);
            double scaledSD = getScaledStandardDeviation(valuesCollected, mean);

            parameter.add((double) lowest);
            parameter.add((double) highest);
            parameter.add(mean);
            parameter.add(scaledSD);
        }
        service.shutdown();
        printArrayList(contentValue);
    }
    
    private void printArrayList(ArrayList<ArrayList<Double>> variable) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(DATASET_FILE);
        
        for(ArrayList<Double> line : variable) {
            String input = line.toString().replace("[", "").replace("]", "");
            String[] inputSplit = input.split(",");
            
            for(int i = 0; i < inputSplit.length; i++) {
                double value = new Double(inputSplit[i]);
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    // integral type
                    inputSplit[i] = String.format("%05d", (int) value);
                } else if (Math.floor(value) == 0) {
                    inputSplit[i] = String.format("%.5f", value);
                } else {
                    inputSplit[i] = decimalFormatter.format(value);
                }
            }
            
            input = String.join(", ", inputSplit);
            pw.println(input);
        }
        
        pw.flush();
        pw.close();
    }
    
    private double[] extractParameters(ArrayList<Double> dataset) {
        double[] parameters = new double[4];
        
        for(int i = 0; i < 4; i++) {
            parameters[i] = dataset.get(i);
        }
        
        return parameters;
    }

    private ArrayList<ArrayList<Double>> checkIfDatasetFileExist() throws IOException {
        System.out.println(String.format("Checking if file %s exist...", DATASET_FILE));
        
        String line;
        BufferedReader br;
        boolean isSeedValueGenerated = false;
        
        ArrayList<ArrayList<Double>> contentValue = new ArrayList<ArrayList<Double>>();
        
        String currentWorkingDir = PlayerSkeletonMunKeatTrainer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(currentWorkingDir + DATASET_FILE, "UTF-8");
        
        File file = new File(decodedPath);
        if(!file.exists() && !file.isDirectory()) {
            file.createNewFile();
        }

        br = new BufferedReader(new FileReader(file.toString()));
        while ((line = br.readLine()) != null) {
            appendToArrayListContent(contentValue, line);
        }
        br.close();

        if (contentValue.size() < MAXIMUM_NUMBER_DATA_SET) {
            for (int i = contentValue.size(); i <= MAXIMUM_NUMBER_DATA_SET; i++) {
                appendToArrayListContent(contentValue, generateRandomValues());
            }
            
            isSeedValueGenerated = true;
        }
        
        
        if(isSeedValueGenerated){
            printArrayList(contentValue);
        }
        

        return contentValue;
    }

    private boolean appendToArrayListContent(ArrayList<ArrayList<Double>> contentValue, String line) {
        String[] stringRepresentation = line.split(",");
        ArrayList<Double> values = new ArrayList<Double>();

        for (String value : stringRepresentation) {
            if(values.equals(IMMORTALITY_TAG)) {
                values.add(IMMORTALITY_TAG_IN_ARRAYLIST);
            } else {
                values.add(new Double(value));
            }
        }

        contentValue.add(values);
        return true;
    }

    private boolean appendToArrayListContent(ArrayList<ArrayList<Double>> contentValue, double[] line) {
        ArrayList<Double> values = new ArrayList<Double>();

        for (double value : line) {
            values.add(value);
        }

        contentValue.add( values);
        return true;
    }
    
    private double getMean(ArrayList<Double> array) {
        double total = 0;
        for(Double value : array) {
            total += value;
        }
        
        return total/array.size();
    }

    private double getScaledStandardDeviation(ArrayList<Double> array, double mean) {
        double sum = 0;
        for (double scores : array) {
            sum += Math.pow((scores - mean), 2);
        }

        return (Math.sqrt(sum / (array.size()))) / mean;
    }

    private double[] generateRandomValues() {
        double[] answer = new double[4];

        for (int i = 0; i < 4; i++) {
            answer[i] = ThreadLocalRandom.current().nextDouble(0, 1);
        }

        return answer;
    }
    
    public static void main(String[] s) {
        
        try {
            PlayerSkeletonMunKeatTrainer pt = new PlayerSkeletonMunKeatTrainer();
            pt.geneticAlgorithm();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
