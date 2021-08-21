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
import java.util.StringTokenizer;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.Classifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.functions.AdaGrad;
import moa.classifiers.functions.MajorityClass;
import moa.classifiers.functions.NoChange;
import moa.classifiers.functions.Perceptron;
import moa.classifiers.functions.SGD;
import moa.classifiers.functions.SPegasos;
import moa.classifiers.lazy.SAMkNN;
import moa.classifiers.lazy.kNN;
import moa.classifiers.lazy.kNNwithPAW;
import moa.classifiers.lazy.kNNwithPAWandADWIN;
import moa.classifiers.meta.ADACC;
import moa.classifiers.meta.ADOB;
import moa.classifiers.meta.AccuracyUpdatedEnsemble;
import moa.classifiers.meta.AccuracyWeightedEnsemble;
import moa.classifiers.meta.AdaptiveRandomForest;
import moa.classifiers.meta.BOLE;
import moa.classifiers.meta.DACC;
import moa.classifiers.meta.DynamicWeightedMajority;
import moa.classifiers.meta.HeterogeneousEnsembleBlast;
import moa.classifiers.meta.HeterogeneousEnsembleBlastFadingFactors;
import moa.classifiers.meta.LearnNSE;
import moa.classifiers.meta.LeveragingBag;
import moa.classifiers.meta.LimAttClassifier;
import moa.classifiers.meta.OCBoost;
import moa.classifiers.meta.OnlineAccuracyUpdatedEnsemble;
import moa.classifiers.meta.OnlineSmoothBoost;
import moa.classifiers.meta.OzaBag;
import moa.classifiers.meta.OzaBagASHT;
import moa.classifiers.meta.OzaBagAdwin;
import moa.classifiers.meta.OzaBoost;
import moa.classifiers.meta.OzaBoostAdwin;
import moa.classifiers.meta.PairedLearners;
import moa.classifiers.meta.RCD;
import moa.classifiers.meta.TemporallyAugmentedClassifier;
import moa.classifiers.meta.WEKAClassifier;
import moa.classifiers.meta.WeightedMajorityAlgorithm;
import moa.classifiers.rules.RuleClassifier;
import moa.classifiers.rules.RuleClassifierNBayes;
import moa.classifiers.trees.ARFHoeffdingTree;
import moa.classifiers.trees.ASHoeffdingTree;
import moa.classifiers.trees.AdaHoeffdingOptionTree;
import moa.classifiers.trees.DecisionStump;
import moa.classifiers.trees.HoeffdingAdaptiveTree;
import moa.classifiers.trees.HoeffdingAdaptiveTreeClassifLeaves;
import moa.classifiers.trees.HoeffdingOptionTree;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.trees.HoeffdingTreeClassifLeaves;
import moa.classifiers.trees.LimAttHoeffdingTree;
import moa.classifiers.trees.RandomHoeffdingTree;
import moa.classifiers.trees.iadem.Iadem2;
import moa.classifiers.trees.iadem.Iadem3;
import moa.core.TimingUtils;
import moa.core.Utils;
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
public class MOABinClassifier {

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
		options.put("--idxTrain", 5);
		options.put("--pClass", 6);
		options.put("--weights", 7);
	}

	/**
	 * 
	 */
	private static final Map<String, Integer> learnerOptions;
	static {
		learnerOptions = new HashMap<String, Integer>();
		learnerOptions.put("naiveBayes", 0);
		learnerOptions.put("adaGrad", 1);
		learnerOptions.put("majority", 2);
		learnerOptions.put("noChange", 3);
		learnerOptions.put("perceptron", 4);
		learnerOptions.put("sgd", 5);
		learnerOptions.put("sPegasos", 6);
		learnerOptions.put("samKnn", 7);
		learnerOptions.put("knn", 8);
		learnerOptions.put("knnPaw", 9);
		learnerOptions.put("knnPawAdwin", 10);
		learnerOptions.put("adacc", 11);
		learnerOptions.put("adob", 12);
		learnerOptions.put("accUpdate", 13);
		learnerOptions.put("accWeight", 14);
		learnerOptions.put("adaptRForest", 15);
		learnerOptions.put("bole", 16);
		learnerOptions.put("dacc", 17);
		learnerOptions.put("dynWeigthMaj", 18);
		learnerOptions.put("hetBlast", 19);
		learnerOptions.put("hetBlastFade", 20);
		learnerOptions.put("learnNse", 21);
		learnerOptions.put("leverageBag", 22);
		learnerOptions.put("limAtt", 23);
		learnerOptions.put("ocBoost", 24);
		learnerOptions.put("onlineAccUpdate", 25);
		learnerOptions.put("onlineSmoothBoost", 26);
		learnerOptions.put("ozaBag", 27);
		learnerOptions.put("ozaBagAsht", 28);
		learnerOptions.put("ozaBagAdwin", 29);
		learnerOptions.put("ozaBoost", 30);
		learnerOptions.put("ozaBoostAdwin", 31);
		learnerOptions.put("paired", 32);
		learnerOptions.put("rcd", 33);
		learnerOptions.put("tempAugmented", 34);
		learnerOptions.put("weka", 35);
		learnerOptions.put("weightMajority", 36);
		learnerOptions.put("rule", 37);
		learnerOptions.put("ruleNBayes", 38);
		learnerOptions.put("arfHoeffding", 39);
		learnerOptions.put("asHoeffding", 40);
		learnerOptions.put("adaHoeffding", 41);
		learnerOptions.put("decisionStump", 42);
		learnerOptions.put("hoeffdingAdap", 43);
		learnerOptions.put("hoeffdingAdapLeaves", 44);
		learnerOptions.put("hoeffdingOpt", 45);
		learnerOptions.put("hoeffding", 46);
		learnerOptions.put("hoeffdingLeaves", 47);
		learnerOptions.put("limAttHoeffding", 48);
		learnerOptions.put("randomHoeffding", 49);
		learnerOptions.put("iadem2", 50);
		learnerOptions.put("iadem3", 51);
	}

	/**
	 * 
	 */
	private static final Map<String, Integer> weightOptions;
	static {
		weightOptions = new HashMap<String, Integer>();
		weightOptions.put("default", 0);
		weightOptions.put("constant", 1);
		weightOptions.put("inverse", 2);
	}

	/**
	 * Constructor
	 */
	public MOABinClassifier() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param outPath
	 *            file path for writing the results
	 */
	public MOABinClassifier(String outPath) {
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
		String learnerName = "hoeffding";
		int indexClass = -1;
		int indexTrain = -1;
		String positiveClass = "E";
		int weightOption = 0;
		double positiveWeight = 1.0;
		double negativeWeight = 1.0;
		// Get parameters from arguments
		for (int i = 0; i < args.length; i++) {
			// Check that given option exists
			int option = 0;
			if (MOABinClassifier.options.containsKey(args[i])) {
				option = MOABinClassifier.options.get(args[i]);
			} else {
				System.out.println("Option '" + args[i] + "' does not exist");
				MOABinClassifier.printHelp();
				System.exit(1);
			}
			// Set parameter corresponding to option
			switch (option) {
			// Help
			case 0:
				MOABinClassifier.printHelp();
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
					MOABinClassifier.printHelp();
					System.exit(1);
				}
				break;
			// INDEX TRAIN
			case 5:
				i++;
				// Parse index of the column that identifies training instances
				try {
					indexTrain = Integer.parseInt(args[i]);
				} catch (Exception e) {
					System.out.println("Error parsing idxTrain '" + args[i] + "' to integer");
					MOABinClassifier.printHelp();
					System.exit(1);
				}
				break;
			// POSITIVE CLASS
			case 6:
				i++;
				positiveClass = args[i];
				break;
			// WEIGHTS
			case 7:
				i++;
				// Tokenize weight option argument
				StringTokenizer st = new StringTokenizer(args[i], ",");
				// Check that given weight option exists
				String weightArg = st.nextToken();
				if (MOABinClassifier.weightOptions.containsKey(weightArg)) {
					weightOption = MOABinClassifier.weightOptions.get(weightArg);
					// Check if weight option is not the default
					if (weightOption != 0) {
						// Check that 2 parameters were given with the weight option
						if (st.countTokens() != 2) {
							System.out.println("Weight option '" + weightArg
									+ "' requires two (2) numeric parameters separated by comma (e.g., " + weightArg
									+ ",1,1)");
							MOABinClassifier.printHelp();
							System.exit(1);
						}
						String pWeight = st.nextToken();
						String nWeight = st.nextToken();
						try {
							positiveWeight = Double.parseDouble(pWeight);
							negativeWeight = Double.parseDouble(nWeight);
						} catch (Exception e) {
							System.out.println("Error parsing weight parameters '" + pWeight + "' and '" + nWeight
									+ "' to double");
							MOABinClassifier.printHelp();
							System.exit(1);
						}
					}
				} else {
					System.out.println("Weight option '" + weightArg + "' does not exist");
					MOABinClassifier.printHelp();
					System.exit(1);
				}
				break;
			// ERROR
			default:
				System.err.println(
						"Internal error. Option '" + option + "' is not implemented. Please, contact the developer");
				System.exit(2);
				break;
			}
		}
		// Check if ARFF path exists
		if (!new File(arffPath).exists()) {
			System.out.println("File path '" + arffPath + "' does not exist");
			MOABinClassifier.printHelp();
			System.exit(1);
		}
		// Get learning algorithm
		Classifier learner = MOABinClassifier.getLearner(learnerName);
		// Get ARFF file stream
		ArffFileStream stream = MOAUtilities.readStream(arffPath, indexClass);
		// Check that positive class exists
		Attribute classAtt = stream.getHeader().classAttribute();
		if (classAtt.getAttributeValues() == null) {
			System.out.println("Class value '" + positiveClass + "' does not exist. The set of class values is NULL");
			MOABinClassifier.printHelp();
			System.exit(1);
		} else if (!classAtt.getAttributeValues().contains(positiveClass)) {
			System.out.println("Class value '" + positiveClass + "' does not exist. The set of class values is "
					+ classAtt.getAttributeValues());
			MOABinClassifier.printHelp();
			System.exit(1);
		}
		// Get index of positive class
		int idxPositive = classAtt.indexOfValue(positiveClass);
		// Run
		MOABinClassifier classifier = new MOABinClassifier(outPath);
		classifier.run(stream, learner, indexTrain, idxPositive, weightOption, positiveWeight, negativeWeight);
		// Close output writer
		classifier.closeOutputWriter();
	}

	/**
	 * Prints help
	 */
	private static void printHelp() {
		System.out.println("");
		System.out.println("=============================");
		System.out.println("NELLY - MOA Binary Classifier");
		System.out.println("=============================");
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
		if (MOABinClassifier.learnerOptions.containsKey(learnerName)) {
			learnerOption = MOABinClassifier.learnerOptions.get(learnerName);
		} else {
			System.out.println("Learner " + learnerName + " does not exist");
			MOABinClassifier.printHelp();
			System.exit(1);
		}
		// Return corresponding learner
		switch (learnerOption) {
		case 0:
			return new NaiveBayes();
		case 1:
			return new AdaGrad();
		case 2:
			return new MajorityClass();
		case 3:
			return new NoChange();
		case 4:
			return new Perceptron();
		case 5:
			return new SGD();
		case 6:
			return new SPegasos();
		case 7:
			return new SAMkNN();
		case 8:
			return new kNN();
		case 9:
			return new kNNwithPAW();
		case 10:
			return new kNNwithPAWandADWIN();
		case 11:
			return new ADACC();
		case 12:
			return new ADOB();
		case 13:
			return new AccuracyUpdatedEnsemble();
		case 14:
			return new AccuracyWeightedEnsemble();
		case 15:
			return new AdaptiveRandomForest();
		case 16:
			return new BOLE();
		case 17:
			return new DACC();
		case 18:
			return new DynamicWeightedMajority();
		case 19:
			return new HeterogeneousEnsembleBlast();
		case 20:
			return new HeterogeneousEnsembleBlastFadingFactors();
		case 21:
			return new LearnNSE();
		case 22:
			return new LeveragingBag();
		case 23:
			return new LimAttClassifier();
		case 24:
			return new OCBoost();
		case 25:
			return new OnlineAccuracyUpdatedEnsemble();
		case 26:
			return new OnlineSmoothBoost();
		case 27:
			return new OzaBag();
		case 28:
			return new OzaBagASHT();
		case 29:
			return new OzaBagAdwin();
		case 30:
			return new OzaBoost();
		case 31:
			return new OzaBoostAdwin();
		case 32:
			return new PairedLearners();
		case 33:
			return new RCD();
		case 34:
			return new TemporallyAugmentedClassifier();
		case 35:
			return new WEKAClassifier();
		case 36:
			return new WeightedMajorityAlgorithm();
		case 37:
			return new RuleClassifier();
		case 38:
			return new RuleClassifierNBayes();
		case 39:
			return new ARFHoeffdingTree();
		case 40:
			return new ASHoeffdingTree();
		case 41:
			return new AdaHoeffdingOptionTree();
		case 42:
			return new DecisionStump();
		case 43:
			return new HoeffdingAdaptiveTree();
		case 44:
			return new HoeffdingAdaptiveTreeClassifLeaves();
		case 45:
			return new HoeffdingOptionTree();
		case 46:
			return new HoeffdingTree();
		case 47:
			return new HoeffdingTreeClassifLeaves();
		case 48:
			return new LimAttHoeffdingTree();
		case 49:
			return new RandomHoeffdingTree();
		case 50:
			return new Iadem2();
		case 51:
			return new Iadem3();
		default:
			System.err.println(
					"Internal error. Learner " + learnerOption + " is not implemented. Please, contact the developer");
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
			System.err.println("Internal error. File '" + outFile.getAbsolutePath()
					+ "' does not exist. Please, contact the developer");
			System.exit(2);
		}
	}

	/**
	 * 
	 */
	private void writeCSVHeader() {
		StringBuilder csvHeader = new StringBuilder();
		csvHeader.append("num_tests,");
		csvHeader.append("num_trains,");
		csvHeader.append("predicted_class,");
		csvHeader.append("actual_class,");
		csvHeader.append("actual_pos,");
		csvHeader.append("actual_neg,");
		csvHeader.append("true_pos,");
		csvHeader.append("true_neg,");
		csvHeader.append("false_pos,");
		csvHeader.append("false_neg,");
		csvHeader.append("time\n");
		try {
			this.output.write(csvHeader.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(
					"Internal error. Exception thrown when writing on the file. Please, contact the developer");
			System.exit(2);
		}
	}

	/**
	 * @param stream
	 * @param learner
	 * @param indexTrain
	 * @param positiveClass
	 */
	private void run(ArffFileStream stream, Classifier learner, int indexTrain, int positiveClass, int weightOption,
			double positiveWeight, double negativeWeight) {
		// Check if default index train (last column)
		InstancesHeader ih = stream.getHeader();
		if (indexTrain == -1) {
			indexTrain = ih.numAttributes() - 1;
		}
		// Get class and train attributes
		Attribute classAtt = ih.classAttribute();
		Attribute trainAtt = ih.attribute(indexTrain);
		// Set actual header to learner
		ih.deleteAttributeAt(indexTrain);
		InstancesHeader actualHeader = new InstancesHeader(ih);
		learner.setModelContext(actualHeader);
		// Prepare for running
		stream.prepareForUse();
		learner.prepareForUse();
		// Set header again to avoid null pointer exception of some algorithms (e.g.,
		// SAM-kNN)
		learner.setModelContext(actualHeader);
		// Counters for accuracy metrics
		int actualPositives = 0;
		int actualNegatives = 0;
		int truePositives = 0;
		int trueNegatives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;
		int countTrainSamples = 0;
		int countTestSamples = 0;
		int countErrorSamples = 0;
		// Counters for inversely proportional weights
		int trainPositives = 0;
		int trainNegatives = 0;
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
			// Get actual class
			int actualClass = (int) instance.classValue();
			// Check if instance is for testing or training
			if (train.equalsIgnoreCase(MOAUtilities.TESTING_INSTANCE)) {
				long startPredictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
				// Classify instance
				int predictClass = Utils.maxIndex(learner.getVotesForInstance(instance));
				// Check if instance is positive or negative
				if (actualClass == positiveClass) {
					actualPositives++;
					// Check is prediction was correct for positive instance
					if (predictClass == actualClass) {
						truePositives++;
					} else {
						falseNegatives++;
					}
				} else {
					actualNegatives++;
					// Check is prediction was correct for negative instance
					if (predictClass == actualClass) {
						trueNegatives++;
					} else {
						falsePositives++;
					}
				}
				// Check prediction time (in nanoseconds)
				double predictionTime = TimingUtils.getNanoCPUTimeOfCurrentThread() - startPredictionTime;
				// Count test samples
				countTestSamples++;
				// Write CSV result
				this.writeCSVResult(countTestSamples, countTrainSamples, classAtt.value(predictClass),
						classAtt.value(actualClass), actualPositives, actualNegatives, truePositives, trueNegatives,
						falsePositives, falseNegatives, predictionTime);
			} else if (train.equalsIgnoreCase(MOAUtilities.TRAINING_INSTANCE)) {
				// Default weight
				double weight = 1.0;
				// Check if setting constant weights
				if (weightOption == 1) {
					if (actualClass == positiveClass)
						weight = positiveWeight;
					else
						weight = negativeWeight;
				}
				// Check if setting weights inversely proportional to the number of instances
				else if (weightOption == 2) {
					if (actualClass == positiveClass) {
						trainPositives++;
						weight = positiveWeight - (positiveWeight * trainPositives / (trainPositives + trainNegatives));
					} else {
						trainNegatives++;
						weight = negativeWeight - (negativeWeight * trainNegatives / (trainPositives + trainNegatives));
					}
				}
				// Set weight
				instance.setWeight(weight);
				// Train on instance
				learner.trainOnInstance(instance);
				countTrainSamples++;
			} else {
				// Train mark not recognized
				System.out.println(
						"Train value '" + train + "' is not recognized. Check instance " + instance.toString() + train);
				// Count error samples
				countErrorSamples++;
			}
		}
		// Check elapsed time
		double totalTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTotalTime);
		// Compute accuracy metrics
		double accuracy = 1.0 * (truePositives + trueNegatives) / (actualPositives + actualNegatives);
		double precision = 1.0 * truePositives / (truePositives + falsePositives); // Positive Predictive Value (PPV)
		double npv = 1.0 * trueNegatives / (trueNegatives + falseNegatives); // Negative Predictive Value (NPV)
		double recall = 1.0 * truePositives / actualPositives; // True Positive Rate (TPR)
		double specificity = 1.0 * trueNegatives / actualNegatives; // True Negative Rate (TNR)
		double fallOut = 1.0 * falsePositives / actualNegatives; // False Positive Rate (FPR)
		double missRate = 1.0 * falseNegatives / actualPositives; // False Negative Rate (FNR)
		double f1Score = 2 * ((precision * recall) / (precision + recall)); // Harmonic mean of precision and recall
		// Write last CSV result
		this.writeCSVResult(countTestSamples, countTrainSamples, String.valueOf(accuracy), String.valueOf(f1Score),
				precision, npv, recall, specificity, fallOut, missRate, totalTime);
		// Generate report statistics
		StringBuilder report = new StringBuilder();
		report.append("\n");
		report.append("======================\n");
		report.append("     FINAL REPORT     \n");
		report.append("======================\n");
		report.append("Done! in ").append(totalTime).append(" seconds (precise? ").append(precise).append(")\n");
		report.append("Instances\n");
		report.append(" - Test = ").append(countTestSamples).append("\n");
		report.append(" - Train = ").append(countTrainSamples).append("\n");
		report.append(" - Error = ").append(countErrorSamples).append("\n");
		report.append("\n");
		report.append("Confusion matrix\n");
		report.append("\t\t\t\t\t\t ACTUAL\n");
		report.append("\t\t\t\t\tPositive\tNegative\tTOTAL\n");
		report.append("\tPREDICTED\tPositive\t" + truePositives + "\t\t" + falsePositives + "\t\t"
				+ (truePositives + falsePositives) + "\n");
		report.append("\t\t\tNegative\t" + falseNegatives + "\t\t" + trueNegatives + "\t\t"
				+ (falseNegatives + trueNegatives) + "\n");
		report.append("\t\tTOTAL\t\t\t" + actualPositives + "\t\t" + actualNegatives + "\t\t"
				+ (actualPositives + actualNegatives) + "\n");
		report.append("\n");
		report.append("Accuracy metrics\n");
		report.append(" - Accuracy = ").append(100 * accuracy).append(" %\n");
		report.append(" - F1 score = ").append(f1Score).append("\n");
		report.append(" - Precision (PPV) = ").append(100 * precision).append(" %\n");
		report.append(" - NPV = ").append(100 * npv).append(" %\n");
		report.append(" - Recall (TPR) = ").append(100 * recall).append(" %\n");
		report.append(" - Specificity (TNR) = ").append(100 * specificity).append(" %\n");
		report.append(" - Fall-out (FPR) = ").append(100 * fallOut).append(" %\n");
		report.append(" - Miss rate (FNR) = ").append(100 * missRate).append(" %\n");
		System.out.println(report.toString());
	}

	/**
	 * @param numTests
	 * @param numTrains
	 * @param predictClass
	 * @param actualClass
	 * @param actualPositives
	 * @param actualNegatives
	 * @param truePositives
	 * @param trueNegatives
	 * @param falsePositives
	 * @param falseNegatives
	 * @param time
	 */
	private void writeCSVResult(int numTests, int numTrains, String predictClass, String actualClass,
			double actualPositives, double actualNegatives, double truePositives, double trueNegatives,
			double falsePositives, double falseNegatives, double time) {
		// Generate CSV result
		StringBuilder csvLine = new StringBuilder();
		csvLine.append(numTests).append(",");
		csvLine.append(numTrains).append(",");
		csvLine.append(predictClass).append(",");
		csvLine.append(actualClass).append(",");
		csvLine.append(actualPositives).append(",");
		csvLine.append(actualNegatives).append(",");
		csvLine.append(truePositives).append(",");
		csvLine.append(trueNegatives).append(",");
		csvLine.append(falsePositives).append(",");
		csvLine.append(falseNegatives).append(",");
		csvLine.append(time).append("\n");
		// Write result to file
		try {
			this.output.write(csvLine.toString().getBytes());
			this.output.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(
					"Internal error. Exception thrown when writing on the file. Please, contact the developer");
			System.exit(2);
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
			System.err.println(
					"Internal error. Exception thrown when closing the file writer. Please, contact the developer");
			System.exit(2);
		}
	}

}
