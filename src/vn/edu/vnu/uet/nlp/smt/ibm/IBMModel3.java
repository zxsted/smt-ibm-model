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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.CombinatoricsUtils;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.structs.Alignment;
import vn.edu.vnu.uet.nlp.smt.structs.FertWord;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;
import vn.edu.vnu.uet.nlp.smt.utils.IConstants;
import vn.edu.vnu.uet.nlp.smt.utils.Utils;

/**
 * @author tuanphong94
 *
 */
public class IBMModel3 extends IBMModel2 {

	double[][][][] d;
	double[][][][] countD;
	double[][][] totalD;

	double p0;
	double countP0;
	double countP1;

	TObjectDoubleHashMap<FertWord> n;
	TObjectDoubleHashMap<FertWord> countN;
	double[] totalN;

	static final int DEFAULT_MAX_FERT = 40;

	public IBMModel3(String enFile, String foFile) {
		super(enFile, foFile, true);
	}

	public IBMModel3(String model) {
		super(model);

		if (!model.endsWith("/")) {
			model = model + File.pathSeparator;
		}

		String dFileName = model + IConstants.distortionModelName;
		String nFileName = model + IConstants.fertilityModelName;
		String nullInsertionFileName = model + IConstants.nullInsertionModelName;

		try {
			d = Utils.loadArray(dFileName);
			n = Utils.loadObject(nFileName);
			BufferedReader br = Files.newBufferedReader(Paths.get(nullInsertionFileName));
			String line = br.readLine();
			if (line == null || !line.startsWith("p0 = ")) {
				System.err.println("Error in null insertion file!");
				return;
			}
			p0 = Double.parseDouble(line.substring("p0 = ".length()));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void train() {
		super.train();

		// Initial probability of null insertion
		p0 = 0.5;

		initDistortion();
		initFertility();

		System.out.println("Start training IBM Model 3...");
		int iter = 1;
		while (!CONVERGE) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			// initialize
			int maxFert = 0;

			initCountT();
			initTotalT();

			initCountD();
			initTotalD();

			initCountN();
			initTotalN();

			countP0 = 0;
			countP1 = 0;

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();

				// Sample the alignment space
				List<Alignment> listA = sample(p);

				// Collect counts
				double subTotal = 0;
				double[] tempProbs = new double[listA.size()];
				int numSpamle = listA.size();

				for (int it = 0; it < numSpamle; it++) {
					Alignment align = listA.get(it);
					tempProbs[it] = probability(align.getA(), p, align.getPhi());
					subTotal += tempProbs[it];
				}

				for (int it = 0; it < numSpamle; it++) {
					Alignment align = listA.get(it);
					double c = (tempProbs[it] + alpha) / (subTotal + alpha * numSpamle);
					int cNull = 0;

					for (int j = 1; j <= le; j++) {
						int i = align.getA()[j];
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);

						// Lexical translation
						if (countT.containsKey(ef)) {
							countT.put(ef, countT.get(ef) + c);
						} else {
							countT.put(ef, c);
						}

						totalT[f] += c;

						// Distortion
						countD[j][i][le][lf] += c;
						totalD[i][le][lf] += c;

						if (i == 0) {
							cNull++;
						}
					}

					// Collect the counts of null insetion
					countP0 += cNull * c;
					countP1 += (le - 2 * cNull) * c;

					// Collect the counts of fertility
					for (int i = 0; i <= lf; i++) {
						int fertility = 0;
						for (int j = 1; j <= le; j++) {
							if (i == align.getA()[j]) {
								fertility++;
							}
						}

						int f = p.getF().get(i);
						FertWord fw = new FertWord(fertility, f);
						if (countN.containsKey(fw)) {
							countN.put(fw, countN.get(fw) + c);
						} else {
							countN.put(fw, c);
						}

						totalN[f] += c;

						if (fertility > maxFert) {
							maxFert = fertility;
						}
					}
				}
			}

			// Estimate translation probability distribution
			Set<WordPair> keySet = countT.keySet();

			for (WordPair ef : keySet) {
				double value = countT.get(ef) / totalT[ef.getF()];
				t.put(ef, value);
			}

			// Estimate distortion
			for (int lf = 1; lf <= maxLf; lf++) {
				for (int le = 1; le <= maxLe; le++) {
					for (int i = 0; i <= lf; i++) {
						for (int j = 1; j <= le; j++) {
							d[j][i][le][lf] = countD[j][i][le][lf] / totalD[i][le][lf];
						}
					}
				}
			}

			// Estimate the fertility, n(Fertility | input word)
			Set<FertWord> keySetFert = countN.keySet();

			for (FertWord fw : keySetFert) {
				n.put(fw, countN.get(fw) / totalN[fw.getF()]);
			}

			// Estimate the probability of null insertion
			double p1 = countP1 / (countP1 + countP0);
			p0 = 1 - p1;

			long end = System.currentTimeMillis();

			long time = end - start;

			System.out.println(" [" + time + " ms]");

			iter++;
			if (iter > MAX_ITER_3) {
				CONVERGE = true;
			}
		}

	}

	private void initTotalN() {
		totalN = new double[foDict.size()];

	}

	private void initCountN() {
		countN = new TObjectDoubleHashMap<FertWord>();
	}

	private void initFertility() {
		n = new TObjectDoubleHashMap<FertWord>();
		double value = 1 / (double) (maxLe + 1);

		for (int fert = 0; fert <= maxLe; fert++) {
			for (int f = 0; f < foDict.size(); f++) {
				FertWord fw = new FertWord(fert, f);
				n.put(fw, value);
			}
		}
	}

	private void initDistortion() {
		d = new double[maxLe + 1][maxLf + 1][maxLe + 1][maxLf + 1];

		for (int le = 1; le <= maxLe; le++) {
			double value = 1 / (double) (le + 1);
			for (int lf = 1; lf <= maxLf; lf++) {
				for (int i = 0; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						d[j][i][le][lf] = value;
					}
				}
			}
		}
	}

	private void initTotalD() {
		totalD = new double[maxLf + 1][maxLe + 1][maxLf + 1];
	}

	private void initCountD() {
		countD = new double[maxLe + 1][maxLf + 1][maxLe + 1][maxLf + 1];
	}

	/**
	 * This function returns a sample from the entire alignment space. First, it
	 * pegs one alignment point and finds out the best alignment through the IBM
	 * model 2. Then, using the hillclimb approach, it finds out the best
	 * alignment on local and returns all its neighborings, which are swapped or
	 * moved one distance from the best alignment.
	 */
	private List<Alignment> sample(SentencePair p) {
		List<Alignment> listA = new ArrayList<Alignment>();
		int le = p.getE().length();
		int lf = p.getF().length();

		// Compute normalization
		for (int i = 0; i <= lf; i++) {
			for (int j = 1; j <= le; j++) {
				int[] align = new int[le + 1];
				int[] phi = new int[lf + 1];

				// Pegging one alignment point
				align[j] = i;
				phi[i] = 1;

				for (int jj = 1; jj <= le; jj++) {
					if (jj == j) {
						continue;
					}

					double maxAlign = 0.0;
					int bestI = 1;

					for (int ii = 0; ii <= lf; ii++) {
						double alignProb = t.get(p.getWordPair(jj, ii)) * a[ii][jj][le][lf];
						if (alignProb > maxAlign) {
							maxAlign = alignProb;
							bestI = ii;
						}
					}

					align[jj] = bestI;
					phi[bestI]++;
				}
				align = hillClimb(align, j, p, phi);
				listA.addAll(neighboring(align, j, p, phi));
			}
		}

		return listA;
	}

	@SuppressWarnings("unused")
	private void printArray(int[] align) {
		System.out.println();
		for (int i : align) {
			System.out.print(i + " ");
		}
		System.out.println();
	}

	/**
	 * This function returns the best alignment on local. It gets some
	 * neighboring alignments and finds out the alignment with highest
	 * probability in those alignment spaces. If the current alignment recorded
	 * has the highest probability, then stop the search loop. If not, then
	 * continue the search loop until it finds out the highest probability of
	 * alignment in local.
	 */
	private int[] hillClimb(int[] a, int jPegged, SentencePair p, int[] phi) {
		int[] aOld;
		int[] phiTemp = cloneIntArray(phi);

		while (true) {
			aOld = cloneIntArray(a);

			for (Alignment neighbor : neighboring(a, jPegged, p, phiTemp)) {
				int[] aNeigh = neighbor.getA();
				int[] phiNeigh = neighbor.getPhi();

				double probNeigh = probability(aNeigh, p, phiNeigh);
				double probNew = probability(a, p, phiTemp);

				if (probNeigh > probNew) {
					a = cloneIntArray(aNeigh);
					phiTemp = cloneIntArray(phiNeigh);
				}

			}

			if (Arrays.equals(a, aOld)) {
				break;
			}
		}

		return a;
	}

	/**
	 * This function returns the neighboring alignments from the given alignment
	 * by moving or swapping one distance.
	 */
	private List<Alignment> neighboring(int[] a, int jPegged, SentencePair p, int[] phi) {
		List<Alignment> listN = new ArrayList<Alignment>();
		int le = p.getE().length();
		int lf = p.getF().length();

		// Moves
		for (int j = 1; j <= le; j++) {
			if (j == jPegged) {
				continue;
			}

			for (int i = 0; i <= lf; i++) {
				int[] newA = cloneIntArray(a);
				newA[j] = i;

				int[] newPhi = cloneIntArray(phi);

				if (newPhi[a[j]] > 0) {
					newPhi[a[j]]--;
					newPhi[i]++;
				}

				if (!Arrays.equals(newA, a)) {
					listN.add(new Alignment(newA, newPhi));
				}
			}
		}

		// Swaps
		for (int j1 = 1; j1 <= le; j1++) {
			if (j1 == jPegged) {
				continue;
			}

			for (int j2 = 1; j2 <= le; j2++) {
				if (j2 == jPegged || j2 == j1) {
					continue;
				}

				int[] newA = cloneIntArray(a);
				int[] newPhi = cloneIntArray(phi);

				newA[j1] = a[j2];
				newA[j2] = a[j1];

				if (!Arrays.equals(newA, a)) {
					listN.add(new Alignment(newA, newPhi));
				}
			}
		}

		return listN;
	}

	/**
	 * This function returns the probability given an alignment. The phi
	 * variable represents the fertility according to the current alignment,
	 * which records how many output words are generated by each input word.
	 */
	private double probability(int[] a, SentencePair p, int[] phi) {
		int le = p.getE().length();
		int lf = p.getF().length();

		double p1 = 1 - p0;

		double total = 1.0;

		// Compute the NULL insertion
		total *= Math.pow(p1, phi[0]) * Math.pow(p0, le - 2 * phi[0]);
		if (total == 0) {
			return total;
		}

		// Compute the combination (le - fert[0]) choose fert[0]
		for (int i = 1; i <= phi[0]; i++) {
			total *= (le - phi[0] - i + 1) / i;
			if (total == 0) {
				return total;
			}
		}

		// Compute fertilities term
		for (int i = 0; i <= lf; i++) {
			total *= CombinatoricsUtils.factorial(phi[i]) * n.get(new FertWord(phi[i], p.getF().get(i)));

			if (total == 0) {
				return total;
			}
		}

		// Multiply the lexical and distortion probabilities
		for (int j = 1; j <= le; j++) {
			int i = a[j];
			total *= t.get(p.getWordPair(j, i));
			total *= d[j][i][le][lf];
			if (total == 0) {
				return total;
			}
		}

		return total;
	}

	static int[] cloneIntArray(int[] src) {
		int[] dest = new int[src.length];
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}

	@Override
	public void printTransProbs() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

		super.printTransProbs();

		System.out.println("Distortion probabilities:");
		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 0; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						double value = d[j][i][le][lf];
						if (value <= 1 && value > 0)
							System.out.println("d(" + j + "|" + i + ", " + le + ", " + lf + ") = " + value);
					}
				}
			}
		}

		System.out.println("Fertility probabilities:");
		for (int f = 0; f < foDict.size(); f++) {
			for (int fert = 0; fert <= maxLe; fert++) {
				FertWord fw = new FertWord(fert, f);
				System.out.println("n(" + fw.getFert() + "|" + foDict.getWord(fw.getF()) + ") = " + n.get(fw));
			}
		}

		System.out.println("Null insertion:");
		System.out.println("p0 = " + p0);
	}

	@Override
	public void save(String folder) throws IOException {
		super.save(folder);

		File fol = new File(folder);
		if (!fol.isDirectory()) {
			System.err.println(folder + " is not a folder! Cannot save model!");
			return;
		}

		if (!folder.endsWith("/")) {
			folder = folder + File.pathSeparator;
		}

		// Save distortion
		String dFileName = folder + IConstants.distortionModelName;
		Utils.saveArray(d, maxLe, maxLf, maxLe, maxLf, dFileName, iStart);

		// Save fertility
		String nFileName = folder + IConstants.fertilityModelName;
		Utils.saveObject(n, nFileName);

		// Save null insertion
		String nullInsertionFileName = folder + IConstants.nullInsertionModelName;

		BufferedWriter bw = Files.newBufferedWriter(Paths.get(nullInsertionFileName), StandardOpenOption.CREATE);
		bw.write("p0 = " + p0);
		bw.flush();
		bw.close();

	}

}
