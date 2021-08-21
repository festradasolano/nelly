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

import moa.classifiers.Classifier;
import moa.classifiers.functions.AdaGrad;
import moa.classifiers.functions.SGD;
import moa.classifiers.meta.RandomRules;
import moa.classifiers.rules.AMRulesRegressor;
import moa.classifiers.rules.functions.AdaptiveNodePredictor;
import moa.classifiers.rules.functions.FadingTargetMean;
import moa.classifiers.rules.functions.LowPassFilteredLearner;
import moa.classifiers.rules.functions.Perceptron;
import moa.classifiers.rules.functions.TargetMean;
import moa.classifiers.rules.meta.RandomAMRules;
import moa.classifiers.trees.FIMTDD;
import moa.classifiers.trees.ORTO;
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
public class MOARegressor {

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
		options.put("--idxDV", 4);
		options.put("--idxTrain", 5);
		options.put("--thrTrain", 6);
		options.put("--logMinDV", 7);
	}

	/**
	 * 
	 */
	private static final Map<String, Integer> learnerOptions;
	static {
		learnerOptions = new HashMap<String, Integer>();
		learnerOptions.put("adagrad", 0);
		learnerOptions.put("sgd", 1);
		learnerOptions.put("randomrules", 2);
		learnerOptions.put("amrules", 3);
		learnerOptions.put("adaptivenode", 4);
		learnerOptions.put("fadingtarget", 5);
		learnerOptions.put("lowpassfiler", 6);
		learnerOptions.put("perceptron", 7);
		learnerOptions.put("targetmean", 8);
		learnerOptions.put("randomamrules", 9);
		learnerOptions.put("fimtdd", 10);
		learnerOptions.put("orto", 11);
	}

	/**
	 * Constructor
	 */
	public MOARegressor() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param outPath file path for writing the results 
	 */
	public MOARegressor(String outPath) {
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
		String learnerName = "fimtdd";
		int indexDV = -1;
		int indexTrain = -1;
		double thresholdTrain = 0;
		double logMinDV = -1;
		// Get parameters from arguments
		for (int i = 0; i < args.length; i++) {
			// Check that given option exists
			int option = 0;
			if (MOARegressor.options.containsKey(args[i])) {
				option = MOARegressor.options.get(args[i]);
			} else {
				System.out.println("Option " + args[i] + " does not exist");
				MOARegressor.printHelp();
				System.exit(1);
			}
			// Set parameter corresponding to option
			switch (option) {
			// Help
			case 0:
				MOARegressor.printHelp();
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
			// INDEX DEPENDENT VARIABLE
			case 4:
				i++;
				// Parse index of the column of dependent variables
				try {
					indexDV = Integer.parseInt(args[i]);
				} catch (Exception e) {
					indexDV = -1;
					System.out.println("Error parsing idxDV '" + args[i]
							+ "' to integer. Using by default the SECOND-LAST column for dependent variables.");
				}
				break;
			// INDEX TRAIN
			case 5:
				i++;
				// Parse index of the column that identifies training instances
				try {
					indexTrain = Integer.parseInt(args[i]);
				} catch (Exception e) {
					indexTrain = -1;
					System.out.println("Error parsing idxTrain '" + args[i]
							+ "' to integer. Using by default the LAST column as identifier of training instances.");
				}
				break;
			// THRESHOLD TRAIN
			case 6:
				i++;
				// Parse threshold that identifies instances for training
				try {
					thresholdTrain = Double.parseDouble(args[i]);
				} catch (Exception e) {
					thresholdTrain = 0;
					System.out.println("Error parsing thrTrain '" + args[i]
							+ "' to integer. Using by default 0 as threshold of training instances.");
				}
				break;
			// LOG TRANSFORMATION
			case 7:
				i++;
				// Parse minimum value for log transformation
				try {
					logMinDV = Double.parseDouble(args[i]);
				} catch (Exception e) {
					logMinDV = -1;
					System.out.println("Error parsing logMinDV '" + args[i]
							+ "' to integer. By default, log transformation is not carried out.");
				}
				break;
			// ERROR
			default:
				System.err.println("Internal error. Option " + option + " is not implemented");
				System.exit(1);
				break;
			}
		}
		System.out.println("----");
		// Check if ARFF path exists
		if (!new File(arffPath).exists()) {
			System.out.println("File path '" + arffPath + "' does not exist");
			System.exit(1);
		}
		// Get learning algorithm
		Classifier learner = MOARegressor.getLearner(learnerName);
		// Run
		MOARegressor regressor = new MOARegressor(outPath);
		regressor.run(MOAUtilities.readStream(arffPath, indexDV), learner, indexTrain, thresholdTrain, logMinDV);
		// Close output writer
		regressor.closeOutputWriter();
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
	 * @param regressorName
	 * @return
	 */
	private static Classifier getLearner(String regressorName) {
		// Check that given learner name exists
		int learnerOption = -1;
		if (MOARegressor.learnerOptions.containsKey(regressorName)) {
			learnerOption = MOARegressor.learnerOptions.get(regressorName);
		} else {
			System.out.println("Learner " + regressorName + " does not exist");
			MOARegressor.printHelp();
			System.exit(1);
		}
		// Return corresponding learner
		switch (learnerOption) {
		case 0:
			return new AdaGrad();
		case 1:
			return new SGD();
		case 2:
			return new RandomRules();
		case 3:
			return new AMRulesRegressor();
		case 4:
			// Based on Perceptron - same issues
			return new AdaptiveNodePredictor();
		case 5:
			return new FadingTargetMean();
		case 6:
			return new LowPassFilteredLearner();
		case 7:
			// Not working with a class index other than the second last due to MOA
			// implementation issues
			return new Perceptron();
		case 8:
			return new TargetMean();
		case 9:
			return new RandomAMRules();
		case 10:
			return new FIMTDD();
		case 11:
			return new ORTO();
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
	 * 
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
	 * @param thresholdTrain
	 * @param logMinDV
	 */
	private void run(ArffFileStream stream, Classifier learner, int indexTrain, double thresholdTrain,
			double logMinDV) {
		// Check if default index train (last column)
		InstancesHeader ih = stream.getHeader();
		if (indexTrain == -1) {
			indexTrain = ih.numAttributes() - 1;
		}
		// Get train attribute
		Attribute trainAtt = ih.attribute(indexTrain);
		// Set actual header to learner
		ih.deleteAttributeAt(indexTrain);
		InstancesHeader actualHeader = new InstancesHeader(ih);
		learner.setModelContext(actualHeader);
		// Prepare for running
		stream.prepareForUse();
		learner.prepareForUse();
		// Initialize error sums and counters
		double sumErrors = 0;
		double sumSquareErrors = 0;
		int countTrainSamples = 0;
		int countTestSamples = 0;
		int countErrorSamples = 0;
		// Get starting CPU time
		boolean precise = TimingUtils.enablePreciseTiming();
		long startTotalTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		// Go through each instance
		while (stream.hasMoreInstances()) {
			// Get instance data
			Instance instance = stream.nextInstance().getData();
			String train = trainAtt.value((int) instance.value(indexTrain));
			// Remove value that indicates training
			instance.deleteAttributeAt(indexTrain);
			instance.setDataset(actualHeader);
			// Get actual value of dependent variable
			double actualDV = instance.classValue();
			// Check if Log transformation is enabled
			if (logMinDV >= 0) {
				// Adjusted Log transformation on dependent variable
				instance.setClassValue(Math.log(actualDV + 1 - logMinDV));
			}
			// Check if instance is for testing or training
			if (train.equalsIgnoreCase(MOAUtilities.TESTING_INSTANCE)) {
				long startPredictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
				// Predict value
				double prediction = 0.0;
				if (learner.getVotesForInstance(instance).length > 0) {
					prediction = learner.getVotesForInstance(instance)[0];
				}
				// Check if Log transformation is enabled
				if (logMinDV >= 0) {
					// Back adjusted Log transformation on prediction
					prediction = Math.exp(prediction) - 1 + logMinDV;
				}
				// Check prediction time (in nanoseconds)
				double predictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread() - startPredictionTime;
				// Compute error between predicted and actual values
				double error = Math.abs(prediction - actualDV);
				double squareError = Math.pow(error, 2);
				// Add error to the sums
				sumErrors += error;
				sumSquareErrors += squareError;
				// Count test samples
				countTestSamples++;
				// Write CSV result
				this.writeCSVResult(countTestSamples, countTrainSamples, prediction, actualDV, error, squareError,
						predictionTime);
			} else if (train.equalsIgnoreCase(MOAUtilities.TRAINING_INSTANCE)) {
				// Check threshold
				if (instance.classValue() > thresholdTrain) {
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
		// Check elapsed time
		double totalTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTotalTime);
		// Compute MAE and RMSE
		double mae = sumErrors / countTestSamples;
		double rmse = Math.sqrt(sumSquareErrors / countTestSamples);
		// Write last CSV result ()
		this.writeCSVResult(countTestSamples, countTrainSamples, 0, 0, mae, rmse, totalTime);
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
		report.append("Errors\n");
		report.append(" - MAE = ").append(mae).append("\n");
		report.append(" - RMSE = ").append(rmse).append("\n");
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
