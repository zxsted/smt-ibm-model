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

/**
 * @author tuanphong94
 *
 */
public class IBMModel1 extends IBMModelAbstract {
	public IBMModel1(String enFile, String foFile) {
		this(enFile, foFile, false);
	}

	public IBMModel1(String model) {
		super(model);
	}

	public IBMModel1(String targetFile, String sourceFile, boolean usingNull) {
		super(targetFile, sourceFile, usingNull);
	}

	public IBMModel1(IBMModel1 md) {
		super(md);
	}

	@Override
	public void train() {
		printDataInfo();

		System.out.print("Initializing IBM Model 1...");
		long ss = System.currentTimeMillis();
		initTransProbs();
		initCountT();
		initTotalT();
		long ee = System.currentTimeMillis();
		long initTime = ee - ss;
		System.out.println(" [" + initTime + " ms]");

		System.out.println("Start training IBM Model 1...");
		mainLoop();
	}

	protected void mainLoop() {
		for (int iter = 1; iter <= MAX_ITER_1; iter++) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			if (iter > 1) {
				initCountT();
				initTotalT();
			}

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();
				double subTotal;

				for (int j = 1; j <= le; j++) {
					subTotal = 0;

					// compute normalization
					for (int i = iStart; i <= lf; i++) {
						WordPair ef = p.getWordPair(j, i);
						subTotal += t.get(ef);
					}

					// collect counts
					for (int i = iStart; i <= lf; i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);
						double c = t.get(ef) / subTotal;

						if (countT.containsKey(ef)) {
							countT.put(ef, countT.get(ef) + c);
						} else {
							countT.put(ef, c);
						}

						totalT[f] += c;
					}
				}
			}

			// estimate probabilities
			Set<WordPair> keySet = countT.keySet();

			for (WordPair ef : keySet) {
				double value = countT.get(ef) / totalT[ef.getF()];
				t.put(ef, value);
			}

			long end = System.currentTimeMillis();
			long time = end - start;

			System.out.println(" [" + time + " ms]");
		}
	}

}
