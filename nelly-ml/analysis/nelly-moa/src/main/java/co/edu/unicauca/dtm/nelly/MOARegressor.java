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
	private static final Map<String, Integer> options;
	static {
		options = new HashMap<String, Integer>();
		options.put("--help", 0);
		options.put("--arff", 1);
		options.put("--out", 2);
		options.put("--learner", 3);
		options.put("--idxClass", 4);
		options.put("--idxTrain", 5);
		options.put("--thrTrain", 6);
	}

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
	 * @param args
	 */
	public static void main(String[] args) {
		// Define default paths
		String arffPath = System.getProperty("user.home") + File.separator + "data.arff";
		String outPath = System.getProperty("user.home") + File.separator + "out.csv";
		//
		String learnerName = "fimtdd";
		//
		int indexClass = -1;
		int indexTrain = -1;
		int thresholdTrain = 0;
		// Get parameters from arguments
		for (int i = 0; i < args.length; i++) {
			// Check that given option exists
			int option = 0;
			if (options.containsKey(args[i])) {
				option = options.get(args[i]);
			} else {
				System.out.println("Option " + args[i] + " does not exist");
				printHelp();
				System.exit(1);
			}
			// Set parameter corresponding to option
			switch (option) {
			// Help
			case 0:
				printHelp();
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
				// Parse index of the column with the values to predict
				try {
					indexClass = Integer.parseInt(args[i]);
				} catch (Exception e) {
					indexClass = -1;
					System.out.println("Error parsing index class '" + args[i]
							+ "' to integer. Using by default the second-last column as values to predict.");
				}
				break;
			// INDEX TRAIN
			case 5:
				i++;
				// Parse index of the column that identifies instances for training
				try {
					indexTrain = Integer.parseInt(args[i]);
				} catch (Exception e) {
					indexTrain = -1;
					System.out.println("Error parsing index train '" + args[i]
							+ "' to integer. Using by default the last column as identifier of instances for training.");
				}
				break;
			// THRESHOLD TRAIN
			case 6:
				i++;
				// Parse threshold that identifies instances for training
				try {
					thresholdTrain = Integer.parseInt(args[i]);
				} catch (Exception e) {
					thresholdTrain = 0;
					System.out.println("Error parsing threshold train '" + args[i]
							+ "' to integer. Using by default 0 as threshold of instances for training.");
				}
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
		Classifier learner = getLearner(learnerName);
		// Run
		MOARegressor regressor = new MOARegressor();
		regressor.run(readStream(arffPath, indexClass), learner, indexTrain, thresholdTrain);
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
	private static Classifier getLearner(String learnerName) {
		// Check that given learner name exists
		int learnerOption = -1;
		if (learnerOptions.containsKey(learnerName)) {
			learnerOption = learnerOptions.get(learnerName);
		} else {
			System.out.println("Learner " + learnerName + " does not exist");
			printHelp();
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
			// Not working with a class index other than the second last due to MOA implementation issues
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
	 * @param stream
	 * @param learner
	 * @param indexTrain
	 * @param thresholdTrain
	 */
	private void run(ArffFileStream stream, Classifier learner, int indexTrain, int thresholdTrain) {
		//
		InstancesHeader ih = stream.getHeader();
		if (indexTrain == -1) {
			indexTrain = ih.numAttributes() - 1;
		}
		ih.deleteAttributeAt(indexTrain);
		InstancesHeader actualHeader = new InstancesHeader(ih);
		//
		stream.prepareForUse();
		learner.prepareForUse();
		learner.setModelContext(actualHeader);
		// Evaluate learner
		double sumOfErrors = 0;
		double sumOfSquareErrors = 0;
		int countTrainSamples = 0;
		int countTestSamples = 0;
		StringBuilder result = new StringBuilder();

		int negatives = 0;

//		for (int i = 0; i < 0; i++) {
		while (stream.hasMoreInstances()) {
			// Obtain instance
			Instance instance = stream.nextInstance().getData();
			int train = (int) instance.value(indexTrain);

			// Remove column that indicates training
			instance.deleteAttributeAt(indexTrain);
			instance.setDataset(actualHeader);

			// Box-Cox transformation (Cube Root)
			// System.out.println(instance.toString());
			// instance.setClassValue(Math.cbrt(instance.classValue()));
			// System.out.println(instance.toString());
			// instance.setClassValue(Math.pow(instance.classValue(), 3));
			// System.out.println(instance.toString());

			// Adjusted Log Transformation
			double minValue = 10000;
			// instance.setClassValue(Math.log(instance.classValue() + 1 - minValue));
			// System.out.println(instance.toString());
			// instance.setClassValue(Math.exp(instance.classValue()) - 1 + minValue);
			// System.out.println(instance.toString());

			// Check if the instance is for prediction or training
			if (train == 0) {
				// // Predict
				// double predictedValue = learner.getPredictionForInstance(instance).getVote(0,
				// 0);
				// System.out.println(predictedValue);
				System.out.println(learner.getVotesForInstance(instance).length);
				double predictedValue = 0.0;
				if (learner.getVotesForInstance(instance).length > 0) {
					predictedValue = learner.getVotesForInstance(instance)[0];
				}
				System.out.println(predictedValue);
				System.out.println("----");

				if (predictedValue < 0) {
					negatives++;
				}

				// double predictedValue =
				// Math.abs(learner.getPredictionForInstance(instance).getVote(0, 0));
				// // Get real value
				double actualValue = instance.classValue();
				// // Compute error between predicted and actual value
				// double error = Math.abs(predictedValue - actualValue);
				// // Add error to the sum
				// sumOfErrors += error;
				// sumOfSquareErrors += Math.pow(error, 2);
				// // Count test samples
				countTestSamples++;
				// // Compute MAE
				// double mae = sumOfErrors / countTestSamples;
				// double rmse = Math.sqrt(sumOfSquareErrors / countTestSamples);
				// // Generate report
				// result.append(countTestSamples).append(",");
				// result.append(countTrainSamples).append(",");
				// result.append(predictedValue).append(",");
				// result.append(actualValue).append(",");
				// result.append(error).append(",");
				// result.append(mae).append(",");
				// result.append(rmse).append("\n");
				// result.append("\n");
			} else {
				// Check threshold
				if (instance.classValue() > thresholdTrain) {
					learner.trainOnInstance(instance);
					countTrainSamples++;
				}
			}
		}

		System.out.println("NEGATIVES = " + negatives);

		// System.out.println(result.toString());

		// // Check if output path exists
		// File outFile = new File(outPath);
		// if (outFile.exists()) {
		// outFile.delete();
		// }
		// // Create CSV file writer
		// FileOutputStream output;
		// try {
		// output = new FileOutputStream(outFile);
		// // output.write(report.toString().getBytes());
		// output.close();
		// } catch (FileNotFoundException e1) {
		// System.err.println("Internal error. File '" + outFile.getAbsolutePath() + "'
		// does not exist");
		// } catch (IOException e) {
		// System.err.println("Internal error. Exception thrown when writing on the file
		// '"
		// + outFile.getAbsolutePath() + "'");
		// }

	}
	
	/**
	 * @param arffPath
	 * @param indexClass
	 * @return
	 */
	private static ArffFileStream readStream(String arffPath, int indexClass) {
		// Read ARFF file
		ArffFileStream stream = new ArffFileStream(arffPath, indexClass);
		//
		if (indexClass == -1) {
			indexClass = stream.getHeader().numAttributes() - 1;
			stream.classIndexOption.setValue(indexClass);
			stream.restart();
		}
		return stream;
	}

}
