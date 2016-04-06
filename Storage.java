import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class Storage{
	
	private File weightFile;
	private File fitnessFile;
	private File settingsFile;
	private static final String DEFAULT_WEIGHTS_FILE_NAME = "group06Weights.txt";
	private static final String DEFAULT_FITNESS_FILE_NAME = "group06FitnessScores.txt";
	private static final String DEFAULT_SETTINGS_FILE_NAME = "group06Settings.txt";
	
	private static final Logger storageLog = Logger.getLogger( Storage.class.getName() );
	
	public Storage(){
		weightFile = new File(DEFAULT_WEIGHTS_FILE_NAME);
		if(!weightFile.exists()){
			try {
				weightFile.createNewFile();
			} catch (IOException e) {
				storageLog.log(Level.WARNING, "Failed to create text file: " + DEFAULT_WEIGHTS_FILE_NAME);
			}
		}
		
		fitnessFile = new File(DEFAULT_FITNESS_FILE_NAME);
		if(!fitnessFile.exists()){
			try {
				fitnessFile.createNewFile();
			} catch (IOException e) {
				storageLog.log(Level.WARNING, "Failed to create text file: " + DEFAULT_FITNESS_FILE_NAME);
			}
		}
		
		settingsFile = new File(DEFAULT_SETTINGS_FILE_NAME);
		if(!settingsFile.exists()){
			try {
				settingsFile.createNewFile();
			} catch (IOException e) {
				storageLog.log(Level.WARNING, "Failed to create text file: " + DEFAULT_SETTINGS_FILE_NAME);
			}
		}
	}
	
	public void storeWeights(ArrayList<double[]> weights){
		try{
			BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream(weightFile));
			for(int i=0; i<weights.size(); ++i){
				double[] weight = weights.get(i);
				StringBuffer s = new StringBuffer();
				for(int j=0; j<weight.length-1; ++j){
					s.append(weight[j] + " ");
				}
				s.append(weight[weight.length-1] + "\n");
				byte[] contents = new String(s).getBytes();
				write.write(contents,0,contents.length);
				write.flush();
			}
			write.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: group06Weights.txt");
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to write into file: group06Weights.txt");
		}
	}
	
	public ArrayList<double[]> retrieveWeights(){
		ArrayList<double[]> weights = new ArrayList<double[]>();
		try {
			BufferedReader read = new BufferedReader(new FileReader(weightFile));
			String line;
			while((line = read.readLine())!=null){
				String[] content = line.split(" ");
				double[] weight = new double[content.length];
				for(int i=0; i<weight.length; ++i){
					weight[i] = Double.parseDouble(content[i]);
				}
				weights.add(weight);
			}
			read.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: " + DEFAULT_WEIGHTS_FILE_NAME);
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to read from file: " + DEFAULT_WEIGHTS_FILE_NAME);
		}catch(NullPointerException npe){
			storageLog.log(Level.FINE, "read all contents from file: " + DEFAULT_WEIGHTS_FILE_NAME);
		}
		return weights;
	}
	
	public void storeFitnessScores(ArrayList<Double> fitnessScores){
		try{
			BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream(fitnessFile));
			for(int i=0; i<fitnessScores.size(); ++i){
				Double scores = fitnessScores.get(i);
				StringBuffer s = new StringBuffer();
				s.append(scores + "\n");
				byte[] contents = new String(s).getBytes();
				write.write(contents,0,contents.length);
				write.flush();
			}
			write.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: " + DEFAULT_FITNESS_FILE_NAME);
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to write into file: " + DEFAULT_FITNESS_FILE_NAME);
		}
	}
	
	public ArrayList<Double> retrieveFitnessScores(){
		ArrayList<Double> fitnessScores = new ArrayList<Double>();
		try {
			BufferedReader read = new BufferedReader(new FileReader(fitnessFile));
			String line;
			while((line = read.readLine())!=null){
				double score = Double.parseDouble(line);
				fitnessScores.add(score);
			}
			read.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: " + DEFAULT_FITNESS_FILE_NAME);
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to read from file: " + DEFAULT_FITNESS_FILE_NAME);
		}catch(NullPointerException npe){
			storageLog.log(Level.FINE, "read all contents from file: " + DEFAULT_FITNESS_FILE_NAME);
		}
		return fitnessScores;
	}

	public void storeSettings(int numberOfWeightSets, int numberOfGames, int maxNumberOfTurns) {
		try{
			BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream(settingsFile));
			StringBuffer s = new StringBuffer();
			s.append(Integer.toString(numberOfWeightSets) + "\n");
			s.append(Integer.toString(numberOfGames) + "\n");
			s.append(Integer.toString(maxNumberOfTurns) + "\n");
			byte[] contents = new String(s).getBytes();
			write.write(contents,0,contents.length);
			write.flush();
			write.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: " + DEFAULT_SETTINGS_FILE_NAME);
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to write into file: " + DEFAULT_SETTINGS_FILE_NAME);
		}
	}
	
	public int[] retrieveSettings(){
		int numberOfWeightSets = 0;
		int numberOfGames = 0;
		int maxNumberOfTurns = 0;
		try {
			BufferedReader read = new BufferedReader(new FileReader(settingsFile));
			numberOfWeightSets = Integer.parseInt(read.readLine());
			numberOfGames = Integer.parseInt(read.readLine());
			maxNumberOfTurns = Integer.parseInt(read.readLine());
			read.close();
		}catch(FileNotFoundException fnfe){
			storageLog.log(Level.WARNING, "File disappeared: " + DEFAULT_SETTINGS_FILE_NAME);
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to read from file: " + DEFAULT_SETTINGS_FILE_NAME);
		}catch(NullPointerException npe){
			storageLog.log(Level.FINE, "read all contents from file: " + DEFAULT_SETTINGS_FILE_NAME);
		}
		return new int[]{numberOfWeightSets, numberOfGames, maxNumberOfTurns};
	}
}