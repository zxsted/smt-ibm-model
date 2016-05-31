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

import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public class IBMModel1 extends IBMModelAbstract {
	public IBMModel1(String enFile, String foFile) {
		super(enFile, foFile);
	}

	public IBMModel1(String model) {
		super(model);
	}

	@Override
	public void train() {
		printDictsInfo();

		System.out.println("Start training IBM Model 1...");
		// printTransProbs();
		int iter = 1;
		while (!CONVERGE) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			if (iter > 1) {
				initCount();
				initTotal();
			}

			for (SentencePair p : sentPairs) {
				double subTotal;

				for (int j = 1; j <= p.getE().length(); j++) {
					subTotal = 0;

					// compute normalization
					for (int i = 1; i <= p.getF().length(); i++) {
						WordPair ef = p.getWordPair(j, i);
						subTotal += t.get(ef);
					}

					// collect counts
					for (int i = 1; i <= p.getF().length(); i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);
						double c = t.get(ef) / subTotal;

						if (count.containsKey(ef)) {
							count.put(ef, count.get(ef) + c);
						} else {
							count.put(ef, c);
						}

						total[f] += c;
					}
				}
			}

			// estimate probabilities
			Set<WordPair> keySet = count.keySet();

			for (WordPair ef : keySet) {
				double value = count.get(ef) / total[ef.getF()];
				t.put(ef, value);
			}

			long end = System.currentTimeMillis();

			long time = end - start;

			System.out.println(" [" + time + " ms]");

			// printTransProbs();
			iter++;
			if (iter > MAX_ITER_1) {
				CONVERGE = true;
			}
		}

		CONVERGE = false; // to be continuously used in IBM Model 2
	}

}
