/*
 * Copyright 2018 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.edu.unicauca.dtm.nelly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.clusterers.Clusterer;
import moa.clusterers.outliers.MyBaseOutlierDetector.Outlier;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import moa.clusterers.outliers.AbstractC.AbstractC;
import moa.clusterers.outliers.Angiulli.ApproxSTORM;
import moa.clusterers.outliers.Angiulli.ExactSTORM;
import moa.clusterers.outliers.AnyOut.AnyOut;
import moa.clusterers.outliers.MCOD.MCOD;
import moa.clusterers.outliers.SimpleCOD.SimpleCOD;
import moa.core.TimingUtils;
import moa.streams.ArffFileStream;

/**
 * 
 * 
 * Copyright 2018 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0 (see LICENSE for details)
 * 
 * @author festradasolano
 */
public class MOAOutlier {
	
	/**
	 * 
	 */
	private FileOutputStream output;

	/**
	 * 
	 */
	private static final Map<String, Integer> options;
	static {
		options = new HashMap<String, Integer>();
		options.put("--help", 0);
		options.put("--arff", 1);
		options.put("--out", 2);
		options.put("--learner", 3);
		options.put("--idxClass", 4);
		options.put("--pClass", 5);
	}

	/**
	 * 
	 */
	private static final Map<String, Integer> learnerOptions;
	static {
		learnerOptions = new HashMap<String, Integer>();
		learnerOptions.put("abstractc", 0);
		learnerOptions.put("approxstorm", 1);
		learnerOptions.put("exactstorm", 2);
		learnerOptions.put("anyout", 3);
		learnerOptions.put("mcod", 4);
		learnerOptions.put("simplecod", 5);
	}
	
	/**
	 * 
	 */
	private static final boolean NO_TRAIN_COLUMN = false;

	/**
	 * Constructor
	 */
	public MOAOutlier() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param outPath file path for writing the results 
	 */
	public MOAOutlier(String outPath) {
		super();
		this.createOutputWriter(outPath);
		this.writeCSVHeader();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Define default arguments
		String arffPath = System.getProperty("user.home") + File.separator + "data.arff";
		String outPath = System.getProperty("user.home") + File.separator + "out.csv";
		String learnerName = "mcod";
		int indexClass = -1;
		String positiveClass = "E";
		// Get parameters from arguments
		for (int i = 0; i < args.length; i++) {
			// Check that given option exists
			int option = 0;
			if (MOAOutlier.options.containsKey(args[i])) {
				option = MOAOutlier.options.get(args[i]);
			} else {
				System.out.println("Option " + args[i] + " does not exist");
				MOAOutlier.printHelp();
				System.exit(1);
			}
			// Set parameter corresponding to option
			switch (option) {
			// Help
			case 0:
				MOAOutlier.printHelp();
				System.exit(0);
				break;
			// ARFF
			case 1:
				i++;
				arffPath = args[i];
				break;
			// OUTPUT
			case 2:
				i++;
				outPath = args[i];
				break;
			// LEARNER
			case 3:
				i++;
				learnerName = args[i];
				break;
			// INDEX CLASS
			case 4:
				i++;
				// Parse index of the class column
				try {
					indexClass = Integer.parseInt(args[i]);
				} catch (Exception e) {
					System.out.println("Error parsing idxClass '" + args[i] + "' to integer");
					MOAOutlier.printHelp();
					System.exit(1);
				}
				break;
			// POSITIVE CLASS
			case 6:
				i++;
				positiveClass = args[i];
				break;
			// ERROR
			default:
				System.err.println("Internal error. Option " + option + " is not implemented");
				System.exit(1);
				break;
			}
		}
		// Check if ARFF path exists
		if (!new File(arffPath).exists()) {
			System.out.println("File path '" + arffPath + "' does not exist");
			System.exit(1);
		}
		// Get learning algorithm
		MyBaseOutlierDetector learner = MOAOutlier.getLearner(learnerName);
		// Get ARFF file stream
		ArffFileStream stream = MOAUtilities.readStream(arffPath, indexClass, NO_TRAIN_COLUMN);
		// Check that positive class exist
		Attribute classAtt = stream.getHeader().classAttribute();
		if (!classAtt.getAttributeValues().contains(positiveClass)) {
			System.out.println("Class value '" + positiveClass + "' does not exist. The set of class values is " + classAtt.getAttributeValues());
			System.exit(1);
		}
		// Get index of positive class
		int idxPositive = classAtt.indexOfValue(positiveClass);
		// Run
		MOAOutlier outlier = new MOAOutlier(outPath);
		outlier.run(stream, learner, idxPositive);
		// Close output writer
		outlier.closeOutputWriter();
	}

	/**
	 * Prints help
	 */
	private static void printHelp() {
		System.out.println("");
		System.out.println("=========");
		System.out.println("NELLY-MOA");
		System.out.println("=========");
		System.out.println("Options:");
		System.out.println("  --help\tDisplay this help");
		System.out.println("  --arff\tFile that contains ...");
		System.out.println("  --out\t\tFile to output the results in CSV format");
		System.out.println("  --learner\tRegressor model ...");
	}

	/**
	 * @param learnerName
	 * @return
	 */
	private static MyBaseOutlierDetector getLearner(String learnerName) {
		// Check that given learner name exists
		int learnerOption = -1;
		if (MOAOutlier.learnerOptions.containsKey(learnerName)) {
			learnerOption = MOAOutlier.learnerOptions.get(learnerName);
		} else {
			System.out.println("Learner " + learnerName + " does not exist");
			MOAOutlier.printHelp();
			System.exit(1);
		}
		// Return corresponding learner
		switch (learnerOption) {
		case 0:
			return new AbstractC();
		case 1:
			return new ApproxSTORM();
		case 2:
			return new ExactSTORM();
		case 3:
			return new AnyOut();
		case 4:
			return new MCOD();
		case 5:
			return new SimpleCOD();
		default:
			System.err.println("Internal error. Learner " + learnerOption + " is not implemented");
			System.exit(1);
		}
		return null;
	}

	/**
	 * @param outPath
	 */
	private void createOutputWriter(String outPath) {
		File outFile = MOAUtilities.getFile(outPath, "out.csv");
		try {
			this.output = new FileOutputStream(outFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Internal error. File '" + outFile.getAbsolutePath() + "' does not exist");
			System.exit(1);
		}
	}

	/**
	 * FIXME
	 */
	private void writeCSVHeader() {
		StringBuilder csvHeader = new StringBuilder();
		csvHeader.append("num_tests,");
		csvHeader.append("num_trains,");
		csvHeader.append("prediction,");
		csvHeader.append("actual_value,");
		csvHeader.append("error,");
		csvHeader.append("square_error,");
		csvHeader.append("time\n");
		try {
			this.output.write(csvHeader.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Internal error. Exception thrown when writing on the file");
			System.exit(1);
		}
	}

	/**
	 * @param stream
	 * @param learner
	 * @param indexTrain
	 * @return
	 */
	private void run(ArffFileStream stream, MyBaseOutlierDetector learner, int positiveClass) {
		// Set header to learner
		learner.setModelContext(stream.getHeader());
		// Prepare for running
		stream.prepareForUse();
		learner.prepareForUse();
		// Initialize counters
		int actualPositives = 0;
		int actualNegatives = 0;
		int truePositives = 0;
		int trueNegatives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;
		int countTrainSamples = 0;
		int countTestSamples = 0;
		int countErrorSamples = 0;
		// Get starting CPU time
		boolean precise = TimingUtils.enablePreciseTiming();
		long startTotalTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		// Go through each instance
		for (int i = 0; i < 1000; i++) {
//		while (stream.hasMoreInstances()) {
			// Get instance data
			Instance instance = stream.nextInstance().getData();
			String train = trainAtt.value((int) instance.value(indexTrain));
			// Remove value that indicates training
			instance.deleteAttributeAt(indexTrain);
			instance.setDataset(actualHeader);
			// Get actual class
			int idxActualClass = (int) instance.classValue();
			String actualClass = instance.classAttribute().value(idxActualClass);
			// Check if instance is for testing or training
			if (train.equalsIgnoreCase(MOAUtilities.TESTING_INSTANCE)) {
				long startPredictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
				// Predict outlier
				int idxPredictClass = idxNormalClass;
//				System.out.println("Prediction = " + learner.getVotesForInstance(instance));
//				if (learner.getVotesForInstance(instance).length > 0) {
//					idxPredictClass = (int) learner.getVotesForInstance(instance)[0];
//				}
				
				
				
				// Check prediction time (in nanoseconds)
				double predictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread() - startPredictionTime;
				
//				System.out.println("Prediction = " + idxPredictClass);
				
//				// Compute error between predicted and actual values
//				double error = Math.abs(idxPredictClass - actualClass);
//				double squareError = Math.pow(error, 2);
//				// Add error to the sums
//				sumErrors += error;
//				sumSquareErrors += squareError;
				// Count test samples
				countTestSamples++;
//				// Write CSV result
//				this.writeCSVResult(countTestSamples, countTrainSamples, idxPredictClass, actualClass, error, squareError,
//						predictionTime);
			} else if (train.equalsIgnoreCase(MOAUtilities.TRAINING_INSTANCE)) {
				// Check if instance belongs to normal class
				if (actualClass.equalsIgnoreCase(normalClass)) {
					// Train on instance
					learner.trainOnInstance(instance);
					countTrainSamples++;
				}
			} else {
				// Train mark not recognized
				System.out.println("Train value '" + train + "' is not recognized. Check instance " + instance.toString() + train);
				// Count error samples
				countErrorSamples++;
			}
		}
		
//		System.out.println(learner.PrintOutliers());
		learner.PrintOutliers();
		
//		System.out.println("Result = " + learner.getClusteringResult());
		
		// Check elapsed time
		double totalTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTotalTime);
		// Compute MAE and RMSE
//		double mae = sumErrors / countTestSamples;
//		double rmse = Math.sqrt(sumSquareErrors / countTestSamples);
		// Write last CSV result ()
//		this.writeCSVResult(countTestSamples, countTrainSamples, 0, 0, mae, rmse, totalTime);
		// Generate report statistics
		StringBuilder report = new StringBuilder();
		report.append("======================\n");
		report.append("     FINAL REPORT     \n");
		report.append("======================\n");
		report.append("Done! in ").append(totalTime).append(" seconds (precise? ").append(precise).append(")\n");
		report.append("Instances\n");
		report.append(" - Test = ").append(countTestSamples).append("\n");
		report.append(" - Train = ").append(countTrainSamples).append("\n");
		report.append(" - Error = ").append(countErrorSamples).append("\n");
//		report.append("Errors\n");
//		report.append(" - MAE = ").append(mae).append("\n");
//		report.append(" - RMSE = ").append(rmse).append("\n");
		System.out.println(report.toString());
	}

	/**
	 * @param numTests
	 * @param numTrains
	 * @param prediction
	 * @param actual
	 * @param error
	 * @param squareError
	 * @param time
	 */
	private void writeCSVResult(int numTests, int numTrains, double prediction, double actual, double error,
			double squareError, double time) {
		// Generate CSV result
		StringBuilder csvLine = new StringBuilder();
		csvLine.append(numTests).append(",");
		csvLine.append(numTrains).append(",");
		csvLine.append(prediction).append(",");
		csvLine.append(actual).append(",");
		csvLine.append(error).append(",");
		csvLine.append(squareError).append(",");
		csvLine.append(time).append("\n");
		// Write result to file
		try {
			this.output.write(csvLine.toString().getBytes());
			this.output.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Internal error. Exception thrown when writing on the file");
			System.exit(1);
		}
	}

	/**
	 * 
	 */
	private void closeOutputWriter() {
		try {
			this.output.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Internal error. Exception thrown when closing the file writer");
			System.exit(1);
		}
	}

}
