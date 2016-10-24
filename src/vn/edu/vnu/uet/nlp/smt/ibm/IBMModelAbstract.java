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
	public static final NumberFormat formatter = new DecimalFormat("#0.0000");

	protected List<SentencePair> sentPairs;

	protected Dictionary enDict;
	protected Dictionary foDict;

	protected TObjectDoubleHashMap<WordPair> t; // translation probability table
												// t(e|f)
	protected TObjectDoubleHashMap<WordPair> countT;
	protected double[] totalT;

	public static int MAX_ITER_1 = 5;
	public static int MAX_ITER_2 = 5;
	public static int MAX_ITER_3 = 3;

	protected boolean usingNull; // use NULL token in foreign sentences or not?
									// Model 3 requires usingNull = true

	protected int iStart; // iteration starting point for foreign word (0 if use
							// NULL token in foreign sentences or 1 otherwise)

	static final double alpha = 1E-4; // use for Laplace smoothing
	static final double MIN_PROB_VALUE = 1E-5;
	static final double MIN_SCORE_VALUE = 1E-5;
	static final double MIN_SCORE_LOG_VALUE = -1E6;

	private static final double smoothProb = 0;

	public static int MAX_LENGTH = 50;

	public IBMModelAbstract(String targetFile, String sourceFile, final boolean usingNull) {
		System.out.print("Reading training data...");

		long start = System.currentTimeMillis();

		this.usingNull = usingNull;
		if (usingNull) {
			iStart = 0;
		} else {
			iStart = 1;
		}

		enDict = new Dictionary(targetFile);
		foDict = new Dictionary(sourceFile, usingNull);

		initSentPairs(targetFile, sourceFile);

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
		String defaultTFileName = model + IConstants.defaultTransProbs;

		try {
			System.out.print("Loading translation probability...");
			long start = System.currentTimeMillis();
			t = Utils.loadObject(tFileName);
			long end = System.currentTimeMillis();
			long time = end - start;
			System.out.println(" [" + time + " ms]");

			System.out.print("Loading target language dictionary...");
			start = System.currentTimeMillis();
			enDict = Utils.loadObject(enFileName);
			end = System.currentTimeMillis();
			time = end - start;
			System.out.println(" [" + time + " ms]");

			System.out.print("Loading source language dictionary...");
			start = System.currentTimeMillis();
			foDict = Utils.loadObject(foFileName);
			end = System.currentTimeMillis();
			time = end - start;
			System.out.println(" [" + time + " ms]");

			// System.out.print("Loading sentence pairs...");
			// start = System.currentTimeMillis();
			// sentPairs = Utils.loadObject(sentFileName);
			// end = System.currentTimeMillis();
			// time = end - start;
			// System.out.println(" [" + time + " ms]");

			System.out.print("Loading default translation probability...");
			start = System.currentTimeMillis();
			defaultT = Utils.loadObject(defaultTFileName);
			end = System.currentTimeMillis();
			time = end - start;
			System.out.println(" [" + time + " ms]");

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		usingNull = foDict.isForeign();
		if (usingNull) {
			iStart = 0;
		} else {
			iStart = 1;
		}
	}

	public IBMModelAbstract(IBMModelAbstract md) {
		this.usingNull = md.usingNull;
		this.iStart = md.iStart;
		this.enDict = md.enDict;
		this.foDict = md.foDict;
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
		totalT = new double[foDict.size()];
	}

	protected void initTransProbs() {
		t = new TObjectDoubleHashMap<WordPair>();

		// uniform distribution
		double value = 1 / (double) enDict.size();

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

				if (enLineWords.length > MAX_LENGTH || foLineWords.length > MAX_LENGTH
						|| enLineWords.length + foLineWords.length < 2) {
					continue;
				}

				int[] enArray = new int[enLineWords.length];
				int[] foArray = new int[foLineWords.length];

				for (int i = 0; i < enArray.length; i++) {
					enArray[i] = enDict.getIndex(enLineWords[i]);
				}

				for (int i = 0; i < foArray.length; i++) {
					foArray[i] = foDict.getIndex(foLineWords[i]);
				}

				sentPairs.add(new SentencePair(new Sentence(enArray), new Sentence(foArray, usingNull)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public double getTransProb(String tarWord, String srcWord) {
		if (!enDict.containsWord(tarWord) || !foDict.containsWord(srcWord)) {
			if (tarWord.equals(srcWord)) {
				return 1.0;
			} else {
				return smoothProb;
			}
		}

		return getTransProb(enDict.getIndex(tarWord), foDict.getIndex(srcWord));
	}

	public double getTransProb(int e, int f) {
		WordPair ef = new WordPair(e, f);
		if (t.contains(ef)) {
			return t.get(ef);
		}

		return smoothProb;
	}

	public Dictionary getTrgDict() {
		return enDict;
	}

	public Dictionary getSourceDict() {
		return foDict;
	}

	public void printModels() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

		System.out.println("Translation probabilities:");
		System.out.print("\t");
		for (int e = 0; e < enDict.size(); e++) {
			System.out.print(enDict.getWord(e) + "\t");
		}
		System.out.println();
		for (int f = 0; f < foDict.size(); f++) {
			System.out.print(foDict.getWord(f) + "\t");
			for (int e = 0; e < enDict.size(); e++) {
				System.out.print(formatter.format(getTransProb(e, f)) + "\t");
			}
			System.out.println();
		}
	}

	public void printDataInfo() {
		if (sentPairs != null) {
			System.out.println("Number of sentence pairs: " + sentPairs.size());
		}
		System.out.println("Target language dictionary size: " + enDict.size());
		System.out.println("Source language dictionary size: " + foDict.size());
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
		Utils.saveObject(enDict, enFileName);

		// Save foreign dictionary
		String foFileName = folder + IConstants.foDictName;
		Utils.saveObject(foDict, foFileName);

		// Save default probabilities
		String defaultTFileName = folder + IConstants.defaultTransProbs;
		Utils.saveObject(defaultT, defaultTFileName);
	}

	protected TObjectDoubleHashMap<Integer> defaultT;

	protected void smoothT() {
		initTotalT();
		for (int f = 0; f < foDict.size(); f++) {
			for (int e = 0; e < enDict.size(); e++) {
				WordPair ef = new WordPair(e, f);
				totalT[f] += (t.containsKey(ef)) ? (t.get(ef) + alpha) : alpha;
			}
		}

		Set<WordPair> keySet = t.keySet();
		for (WordPair ef : keySet) {
			t.put(ef, (t.get(ef) + alpha) / totalT[ef.getF()]);
		}

		defaultT = new TObjectDoubleHashMap<Integer>();

		for (int f = 0; f < foDict.size(); f++) {
			defaultT.put(f, alpha / totalT[f]);
		}
	}

}
