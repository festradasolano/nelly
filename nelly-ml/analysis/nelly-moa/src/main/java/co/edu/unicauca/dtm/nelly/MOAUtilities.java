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
import java.io.IOException;

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
public class MOAUtilities {

	/**
	 * 
	 */
	public static final String TESTING_INSTANCE = "F";

	/**
	 * 
	 */
	public static final String TRAINING_INSTANCE = "T";
	
	/**
	 * 
	 */
	private static final int DEFAULT_INDEX_CLASS = -1;
	
	/**
	 * 
	 */
	private static final boolean HAS_TRAIN = true;
	
	/**
	 * Creates a file. Exits if the program throws an error while creating the file
	 * 
	 * @param file
	 *            to create
	 */
	private static void createFile(File file) {
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error creating file " + file.getAbsolutePath());
			System.exit(1);
		}
	}

	/**
	 * @param path
	 * @param defaultName
	 * @return
	 */
	public static File getFile(String path, String defaultName) {
		// Check if path exists
		File file = new File(path);
		if (file.exists()) {
			// Check if path is a directory or a file
			if (file.isDirectory()) {
				System.out.println("Path '" + path + "' points to an existing folder");
				System.out.println("Creating file '" + defaultName + "' in this folder path");
				file = new File(file.getAbsolutePath() + File.separator + defaultName);
				if (file.exists()) {
					System.out.println("Overriding existing file '" + file.getName() + "'");
					file.delete();
				}
				MOAUtilities.createFile(file);
			} else {
				System.out.println("Path '" + path + "' points to an existing file");
				System.out.println("Overriding existing file '" + file.getName() + "'");
				file.delete();
				MOAUtilities.createFile(file);
			}
		} else {
			System.out.println("Path '" + path + "' does not exist");
			// Check if path ends with an extension (i.e., a file)
			if (file.getName().matches(".+\\..+")) {
				System.out.println("Handling path '" + path + "' as a file");
				// Check if parent folder exists
				if (!file.getParentFile().exists()) {
					System.out.println("Creating parent folder path '" + file.getParent() + "'");
					file.getParentFile().mkdirs();
				}
				System.out.println("Creating file '" + file.getName() + "'");
				MOAUtilities.createFile(file);
			} else {
				System.out.println("Handling path '" + path + "' as a folder");
				System.out.println("Creating folder path '" + file.getAbsolutePath() + "'");
				file.mkdirs();
				System.out.println("Creating file '" + defaultName + "' in this folder path");
				file = new File(file.getAbsolutePath() + File.separator + defaultName);
				MOAUtilities.createFile(file);
			}
		}
		return file;
	}

	/**
	 * @param arffPath
	 * @param indexClass
	 * @return
	 */
	public static ArffFileStream readStream(String arffPath, int indexClass) {
		return readStream(arffPath, indexClass, HAS_TRAIN);
	}
	
	/**
	 * @param arffPath
	 * @param indexClass
	 * @param indexTrain
	 * @return
	 */
	public static ArffFileStream readStream(String arffPath, int indexClass, boolean hasTrain) {
		// Read ARFF file
		ArffFileStream stream = new ArffFileStream(arffPath, indexClass);
		// Check if default index class
		if (indexClass == DEFAULT_INDEX_CLASS) {
			// Set index class to last column
			indexClass = stream.getHeader().numAttributes();
			// Check if stream has train column
			if (hasTrain) {
				// Set index class to last-second column
				indexClass--;
			}
			stream.classIndexOption.setValue(indexClass);
			stream.restart();
		}
		// Return stream
		return stream;
	}

}
