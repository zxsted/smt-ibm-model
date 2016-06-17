/*******************************************************************************
 * Copyright [2016] [Nguyen Tuan Phong]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package vn.edu.vnu.uet.nlp.smt.ibm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.structs.Dictionary;
import vn.edu.vnu.uet.nlp.smt.structs.Sentence;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;
import vn.edu.vnu.uet.nlp.smt.utils.IConstants;
import vn.edu.vnu.uet.nlp.smt.utils.Utils;

/**
 * @author tuanphong94
 *
 */
public abstract class IBMModelAbstract {
	NumberFormat formatter = new DecimalFormat("#0.0000");

	List<SentencePair> sentPairs;

	Dictionary tarDict;
	Dictionary srcDict;

	TObjectDoubleHashMap<WordPair> t; // translation probability table t(e|f)
	TObjectDoubleHashMap<WordPair> countT;
	double[] totalT;

	public static int MAX_ITER_1 = 3;
	public static int MAX_ITER_2 = 3;
	public static int MAX_ITER_3 = 3;

	protected boolean CONVERGE = false;
	boolean usingNull; // use NULL token in foreign sentences or not? Model 3
						// requires usingNull = true

	int iStart; // iteration starting point for foreign word (0 if use
				// NULL token in foreign sentences or 1 otherwise)

	static final double alpha = 1E-10; // use for Laplace smoothing

	public IBMModelAbstract(String target, String source, final boolean usingNull) {
		System.out.print("Reading training data...");

		long start = System.currentTimeMillis();

		this.usingNull = usingNull;
		if (usingNull) {
			iStart = 0;
		} else {
			iStart = 1;
		}

		tarDict = new Dictionary(target);
		srcDict = new Dictionary(source, usingNull);

		initSentPairs(target, source);

		long end = System.currentTimeMillis();

		long time = end - start;

		System.out.println(" [" + time + " ms]");
	}

	public IBMModelAbstract(String target, String source) {
		this(target, source, false);
	}

	public IBMModelAbstract(String model) {
		File fol = new File(model);

		if (!fol.isDirectory()) {
			System.err.println(model + " is not a folder! Cannot load model!");
			return;
		}

		if (!model.endsWith("/")) {
			model = model + "/";
		}

		// Load translation probabilities
		String tFileName = model + IConstants.transProbsModelName;
		String enFileName = model + IConstants.enDictName;
		String foFileName = model + IConstants.foDictName;
		// String sentFileName = model + IConstants.sentFileName;

		try {
			System.out.print("Loading translation probability...");
			long start = System.currentTimeMillis();
			t = Utils.loadObject(tFileName);
			long end = System.currentTimeMillis();
			long time = end - start;
			System.out.println(" [" + time + " ms]");

			System.out.print("Loading target language dictionary...");
			start = System.currentTimeMillis();
			tarDict = Utils.loadObject(enFileName);
			end = System.currentTimeMillis();
			time = end - start;
			System.out.println(" [" + time + " ms]");

			System.out.print("Loading source language dictionary...");
			start = System.currentTimeMillis();
			srcDict = Utils.loadObject(foFileName);
			end = System.currentTimeMillis();
			time = end - start;
			System.out.println(" [" + time + " ms]");

			// System.out.print("Loading sentence pairs...");
			// start = System.currentTimeMillis();
			// sentPairs = Utils.loadObject(sentFileName);
			// end = System.currentTimeMillis();
			// time = end - start;
			// System.out.println(" [" + time + " ms]");

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		usingNull = srcDict.isForeign();
		if (usingNull) {
			iStart = 0;
		} else {
			iStart = 1;
		}
	}

	public IBMModelAbstract(IBMModelAbstract md) {
		this.usingNull = md.usingNull;
		this.iStart = md.iStart;
		this.tarDict = md.tarDict;
		this.srcDict = md.srcDict;
		this.sentPairs = md.sentPairs;
		this.t = md.t;
	}

	public abstract void train();

	protected void initCountT() {
		if (countT == null) {
			countT = new TObjectDoubleHashMap<WordPair>();
		} else {
			Set<WordPair> keySet = countT.keySet();

			for (WordPair ef : keySet) {
				countT.put(ef, 0.0);
			}
		}
	}

	protected void initTotalT() {
		totalT = new double[srcDict.size()];
	}

	protected void initTransProbs() {
		t = new TObjectDoubleHashMap<WordPair>();

		// uniform distribution
		double value = 1 / (double) tarDict.size();

		for (SentencePair p : sentPairs) {
			for (int j = 1; j <= p.getE().length(); j++) {
				for (int i = iStart; i <= p.getF().length(); i++) {
					WordPair ef = p.getWordPair(j, i);
					t.put(ef, value);
				}
			}
		}
	}

	private void initSentPairs(String enFile, String foFile) {
		sentPairs = new ArrayList<SentencePair>();

		try {
			BufferedReader enBr = Files.newBufferedReader(Paths.get(enFile), StandardCharsets.UTF_8);
			BufferedReader foBr = Files.newBufferedReader(Paths.get(foFile), StandardCharsets.UTF_8);

			String enLine, foLine;

			// int count = 0;
			while ((enLine = enBr.readLine()) != null && (foLine = foBr.readLine()) != null) {
				String[] enLineWords = enLine.split("\\s+");
				String[] foLineWords = foLine.split("\\s+");

				if (enLineWords.length > 100 || foLineWords.length > 100) {
					continue;
				}

				int[] enArray = new int[enLineWords.length];
				int[] foArray = new int[foLineWords.length];

				for (int i = 0; i < enArray.length; i++) {
					enArray[i] = tarDict.getIndex(enLineWords[i]);
				}

				for (int i = 0; i < foArray.length; i++) {
					foArray[i] = srcDict.getIndex(foLineWords[i]);
				}

				sentPairs.add(new SentencePair(new Sentence(enArray), new Sentence(foArray, usingNull)));

				// if (++count % 1000 == 0) {
				// System.out.println(count + " sentences");
				// }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public double getTransProb(String tarWord, String srcWord) {
		if (!tarDict.containsWord(tarWord) || !srcDict.containsWord(srcWord)) {
			return 0.0;
		}
		return getProb(tarDict.getIndex(tarWord), srcDict.getIndex(srcWord));
	}

	public double getProb(int e, int f) {
		WordPair ef = new WordPair(e, f);
		if (t.contains(ef)) {
			return t.get(ef);
		}
		return 0.0;
	}

	public Dictionary getTarDict() {
		return tarDict;
	}

	public Dictionary getSourceDict() {
		return srcDict;
	}

	public void printModels() {
		if (tarDict.size() > 10 || srcDict.size() > 10) {
			return;
		}

		System.out.println("Translation probabilities:");
		System.out.print("\t");
		for (int e = 0; e < tarDict.size(); e++) {
			System.out.print(tarDict.getWord(e) + "\t");
		}
		System.out.println();
		for (int f = 0; f < srcDict.size(); f++) {
			System.out.print(srcDict.getWord(f) + "\t");
			for (int e = 0; e < tarDict.size(); e++) {
				System.out.print(formatter.format(getProb(e, f)) + "\t");
			}
			System.out.println();
		}
	}

	public void printDataInfo() {
		if (sentPairs != null) {
			System.out.println("Number of sentence pairs: " + sentPairs.size());
		}
		System.out.println("Target language dictionary size: " + tarDict.size());
		System.out.println("Source language dictionary size: " + srcDict.size());
	}

	public void save(String folder) throws IOException {
		File fol = new File(folder);
		if (!fol.exists()) {
			fol.mkdir();
		}

		if (!fol.isDirectory()) {
			System.err.println(folder + " is not a folder! Cannot save model!");
			return;
		}

		if (!folder.endsWith("/")) {
			folder = folder + "/";
		}

		// // Save sentence pairs
		// String sentFileName = folder + IConstants.sentFileName;
		// Utils.saveObject(sentPairs, sentFileName);

		// Save translation probabilities
		String tFileName = folder + IConstants.transProbsModelName;
		Utils.saveObject(t, tFileName);

		// Save english dictionary
		String enFileName = folder + IConstants.enDictName;
		Utils.saveObject(tarDict, enFileName);

		// Save foreign dictionary
		String foFileName = folder + IConstants.foDictName;
		Utils.saveObject(srcDict, foFileName);
	}

}
