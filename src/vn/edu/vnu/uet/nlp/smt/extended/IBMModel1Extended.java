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
package vn.edu.vnu.uet.nlp.smt.extended;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel1;
import vn.edu.vnu.uet.nlp.smt.structs.Dictionary;
import vn.edu.vnu.uet.nlp.smt.structs.Sentence;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public class IBMModel1Extended extends IBMModel1 {
	static final double epsilon = 1E-7;

	List<LabeledSentencePair> labeledSentPairs;
	Set<WordPair> alignedWordPairs;

	// constructor for training
	public IBMModel1Extended(String enFile, String foFile, String labeledData) throws IOException {
		super(enFile, foFile);

		initLabeledSentPairs(labeledData);
		initAlignedWordPairs();

	}

	// constructor for reusing
	public IBMModel1Extended(String model) {
		super(model);
	}

	@Override
	public void train() {
		printDataInfo();

		System.out.print("Initializing Extended IBM Model 1...");
		long ss = System.currentTimeMillis();
		initTransProbs();
		initCountT();
		initTotalT();
		long ee = System.currentTimeMillis();
		long initTime = ee - ss;
		System.out.println(" [" + initTime + " ms]");

		System.out.println("Start EM training for Extended IBM Model 1...");
		mainLoop();

		System.out.println("Optimize t(e|f) with labeled data...");
		optimizeParameters();
	}

	private void optimizeParameters() {
		for (int iter = 1; iter <= 10; iter++) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();
			List<Set<SingleAlignment>> guessedAlignment = predictAlignmentForLabeledData();

			for (int index = 0; index < guessedAlignment.size(); index++) {
				Set<SingleAlignment> guessed = guessedAlignment.get(index);
				Set<SingleAlignment> gold = labeledSentPairs.get(index).getAlignment();

				// align thua
				for (SingleAlignment a : guessed) {
					if (!gold.contains(a)) {
						WordPair ef = labeledSentPairs.get(index).getWordPair(a.getTrg(), a.getSrc());
						if (t.contains(ef)) {
							double newValue = t.get(ef) - epsilon;
							if (newValue > 0) {
								t.put(ef, newValue);
							} else {
								t.put(ef, 0.0);
							}
						} else {
							t.put(ef, 0.0);
						}
					}
				}

				// align thieu
				for (SingleAlignment a : gold) {
					if (!guessed.contains(a)) {
						WordPair ef = labeledSentPairs.get(index).getWordPair(a.getTrg(), a.getSrc());
						if (t.contains(ef)) {
							t.put(ef, t.get(ef) + epsilon);
						} else {
							t.put(ef, epsilon);
						}
					}
				}
			}

			initTotalT();
			Set<WordPair> keySet = t.keySet();
			for (WordPair ef : keySet) {
				totalT[ef.getF()] += t.get(ef);
			}
			for (WordPair ef : keySet) {
				t.put(ef, t.get(ef) / totalT[ef.getF()]);
			}

			long end = System.currentTimeMillis();
			long time = end - start;

			System.out.println(" [" + time + " ms]");
		}
	}

	private List<Set<SingleAlignment>> predictAlignmentForLabeledData() {
		List<Set<SingleAlignment>> result = new ArrayList<Set<SingleAlignment>>();

		for (LabeledSentencePair pair : labeledSentPairs) {
			result.add(predictAlignment(pair));
		}

		return result;
	}

	private Set<SingleAlignment> predictAlignment(LabeledSentencePair pair) {
		Set<SingleAlignment> result = new HashSet<SingleAlignment>();

		int lf = pair.getF().length();
		int le = pair.getE().length();

		for (int j = 1; j <= le; j++) {
			double max_prob = 0.0;
			int max_i = 0;
			for (int i = iStart; i <= lf; i++) {
				double new_prob = getTransProb(pair.getE().get(j), pair.getF().get(i));
				if (new_prob > max_prob) {
					max_i = i;
					max_prob = new_prob;
				}
			}

			if (max_i > 0) {
				result.add(new SingleAlignment(max_i, j));
			}
		}

		return result;
	}

	@Override
	public void printDataInfo() {
		super.printDataInfo();
		if (labeledSentPairs != null) {
			System.out.println("Number of labeled sentence pairs: " + labeledSentPairs.size());
		}
		if (alignedWordPairs != null) {
			System.out.println("Number of aligned word pairs: " + alignedWordPairs.size());
		}
	}

	@Override
	protected void initTransProbs() {
		t = new TObjectDoubleHashMap<WordPair>();
		initCountT();
		initTotalT();

		double value = 1 + (double) labeledSentPairs.size() / (double) sentPairs.size();

		for (SentencePair pair : sentPairs) {
			int le = pair.getE().length();
			int lf = pair.getF().length();

			for (int j = 1; j <= le; j++) {
				for (int i = iStart; i <= lf; i++) {
					WordPair ef = pair.getWordPair(j, i);
					int f = pair.getF().get(i);
					double c;
					if (alignedWordPairs.contains(ef)) {
						c = value;
					} else {
						c = 1.0;
					}

					if (countT.containsKey(ef)) {
						countT.put(ef, countT.get(ef) + c);
					} else {
						countT.put(ef, c);
					}

					totalT[f] += c;
				}
			}
		}

		Set<WordPair> keySet = countT.keySet();

		for (WordPair ef : keySet) {
			double initValue = countT.get(ef) / totalT[ef.getF()];
			t.put(ef, initValue);
		}
	}

	private void initAlignedWordPairs() {
		alignedWordPairs = new HashSet<WordPair>();
		for (LabeledSentencePair pair : labeledSentPairs) {
			Set<SingleAlignment> set = pair.getAlignment();
			for (SingleAlignment a : set) {
				alignedWordPairs.add(pair.getWordPair(a.getTrg(), a.getSrc()));
			}
		}
	}

	private void initLabeledSentPairs(String labeledData) throws IOException {
		labeledSentPairs = new ArrayList<LabeledSentencePair>();
		int errCnt = 0;
		BufferedReader br = Files.newBufferedReader(Paths.get(labeledData), StandardCharsets.UTF_8);
		for (String foLine; (foLine = br.readLine()) != null;) {
			if (foLine.isEmpty()) {
				continue;
			}

			String enLine = br.readLine();
			if (enLine.isEmpty()) {
				continue;
			}

			String alignLine = br.readLine();
			if (alignLine.isEmpty()) {
				continue;
			}

			try {
				int[] foArray = buildIndexArray(foLine, foDict);
				int[] enArray = buildIndexArray(enLine, enDict);

				Sentence enSent = new Sentence(enArray);
				Sentence foSent = new Sentence(foArray);
				Set<SingleAlignment> align = buildAlignment(alignLine, foArray.length, enArray.length);

				labeledSentPairs.add(new LabeledSentencePair(enSent, foSent, align));
			} catch (Exception e) {
				errCnt++;
			}
		}

		System.out.println("Labeled data has " + errCnt + " error pairs.");
	}

	private Set<SingleAlignment> buildAlignment(String alignLine, int srcLength, int trgLength) {
		Set<SingleAlignment> align = new HashSet<SingleAlignment>();
		String[] toks = alignLine.split("\\s+");

		for (String tok : toks) {
			int index = tok.lastIndexOf(':');
			if (index < 0) {
				continue;
			}
			String srcs = tok.substring(0, index);
			String trgs = tok.substring(index + 1);

			String[] src = srcs.split(",");
			String[] trg = trgs.split(",");

			for (String s : src) {
				if (s.isEmpty()) {
					continue;
				}
				int first = Integer.parseInt(s) + 1;
				if (first <= 0 || first > srcLength) {
					continue;
				}

				for (String t : trg) {
					if (t.isEmpty()) {
						continue;
					}
					int second = Integer.parseInt(t) + 1;
					if (second <= 0 || second > trgLength) {
						continue;
					}

					align.add(new SingleAlignment(first, second));
				}
			}
		}

		return align;
	}

	private int[] buildIndexArray(String line, Dictionary dict) {
		String[] toks = line.split("\\s+");
		int[] result = new int[toks.length];
		for (int i = 0; i < toks.length; i++) {
			String tok = toks[i];
			int index = tok.lastIndexOf(':');
			String word = tok.substring(0, index);
			if (dict.containsWord(word)) {
				result[i] = dict.getIndex(word);
			} else {
				dict.put(word);
				result[i] = dict.getIndex(word);
			}
		}

		return result;
	}

}
