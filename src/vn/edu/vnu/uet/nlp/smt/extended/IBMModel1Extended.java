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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public class IBMModel1Extended extends IBMModelExtendAbstract {
	// constructor for training
	public IBMModel1Extended(String enFile, String foFile, String labeledData) throws IOException {
		super(enFile, foFile, labeledData);
	}

	public IBMModel1Extended(String model) {
		super(model);
	}

	@Override
	public void train() {
		train(true);
	}

	public void train(boolean optimize) {
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

		if (optimize) {
			System.out.println("Optimize t(e|f) with labeled data...");
			optimizeParameters();
		}
	}

	private void optimizeParameters() {
		int numIters = 100;
		double[] fmeasure = new double[numIters + 1];

		List<Set<SingleAlignment>> guessedAlignment = predictAlignmentForLabeledData();
		fmeasure[0] = computeFMeasureOnLabeledData(guessedAlignment);

		System.out.println("\tInitial F-Score = " + fmeasure[0]);

		for (int iter = 1; iter <= numIters; iter++) {
			System.out.print("Iteration " + (iter));

			long start = System.currentTimeMillis();

			Set<WordPair> thua = new HashSet<WordPair>();
			Set<WordPair> thieu = new HashSet<WordPair>();

			for (int index = 0; index < guessedAlignment.size(); index++) {
				Set<SingleAlignment> guessed = guessedAlignment.get(index);
				Set<SingleAlignment> gold = labeledSentPairs.get(index).getAlignment();

				// align thua
				for (SingleAlignment a : guessed) {
					if (!gold.contains(a)) {
						WordPair ef = labeledSentPairs.get(index).getWordPair(a.getTrg(), a.getSrc());
						thua.add(ef);
					}
				}

				// align thieu
				for (SingleAlignment a : gold) {
					if (!guessed.contains(a)) {
						WordPair ef = labeledSentPairs.get(index).getWordPair(a.getTrg(), a.getSrc());
						thieu.add(ef);
					}
				}

			}

			for (WordPair ef : thua) {
				if (thieu.contains(ef)) {
					continue;
				}

				if (t.containsKey(ef)) {
					t.put(ef, (t.get(ef) - delta) > 0 ? t.get(ef) - delta : 0.0);
				}
			}

			for (WordPair ef : thieu) {
				if (thua.contains(ef)) {
					continue;
				}

				if (t.containsKey(ef)) {
					t.put(ef, t.get(ef) + delta);
				} else {
					if (enDict.containsIndex(ef.getE()) && foDict.containsIndex(ef.getF())) {
						t.put(ef, delta);
					}
				}
			}

			// normalization
			nomalizeT();

			guessedAlignment = predictAlignmentForLabeledData();
			fmeasure[iter] = computeFMeasureOnLabeledData(guessedAlignment);

			long end = System.currentTimeMillis();
			long time = end - start;

			System.out.println(" [" + time + " ms] : " + "F-Score = " + fmeasure[iter]);
		}
	}

	@Override
	protected void initTransProbs() {
		super.initTransProbs();

		// add probability from labeled data
		Set<WordPair> keySet = t.keySet();
		TObjectDoubleHashMap<WordPair> labeledDataProb = computeTFromLabeledData();

		for (WordPair ef : keySet) {
			if (labeledDataProb.containsKey(ef)) {
				t.put(ef, t.get(ef) + labeledDataProb.get(ef));
			}
		}

		// nomalization
		nomalizeT();
	}

}
