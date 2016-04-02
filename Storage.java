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
	
	private File file;
	private static final String DEFAULT_FILE_NAME = "group06Weights.txt";
	
	private static final Logger storageLog = Logger.getLogger( Storage.class.getName() );
	
	public Storage(){
		file = new File(DEFAULT_FILE_NAME);
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				storageLog.log(Level.WARNING, "Failed to create text file: group06Weights.txt");
			}
		}
	}
	
	public void storeWeights(ArrayList<double[]> weights){
		try{
			BufferedOutputStream write = new BufferedOutputStream(new FileOutputStream(file));
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
			BufferedReader read = new BufferedReader(new FileReader(file));
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
			storageLog.log(Level.WARNING, "File disappeared: group06Weights.txt");
		}catch(IOException ioe){
			storageLog.log(Level.WARNING, "Failed to read from file: group06Weights.txt");
		}catch(NullPointerException npe){
			storageLog.log(Level.FINE, "read all contents from file: group06Weights.txt");
		}
		return weights;
	}
}