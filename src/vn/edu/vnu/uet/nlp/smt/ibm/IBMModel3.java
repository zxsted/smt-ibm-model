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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.exception.MathArithmeticException;
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

	double[][][][] d; // distortion
	double[][][][] countD;
	double[][][] totalD;

	double p0;
	double countP0;
	double countP1;

	TObjectDoubleHashMap<FertWord> n; // fertility
	TObjectDoubleHashMap<FertWord> countN;
	double[] totalN;

	int[][] fertWordHashCode;

	public IBMModel3(String targetFile, String sourceFile) {
		super(targetFile, sourceFile, true);
		super.train();
	}

	public IBMModel3(String model) {
		super(model);

		if (!model.endsWith("/")) {
			model = model + "/";
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

	public IBMModel3(IBMModel2 md2) {
		super(md2);
		this.maxLe = md2.maxLe;
		this.maxLf = md2.maxLf;
		this.a = md2.a;
	}

	@Override
	public void train() {
		if (!this.usingNull) {
			System.err.println("The provided IBM Model 2 does not use the NULL token!"
					+ " Cannot train IBM Model 3 from this model!");
			return;
		}

		System.out.print("Initializing IBM Model 3...");
		long ss = System.currentTimeMillis();
		p0 = 0.5;

		countA = null;
		totalA = null;

		initDistortion();
		initCountD();
		initTotalD();

		initFertility();
		initCountN();
		initTotalN();

		long ee = System.currentTimeMillis();
		long initTime = ee - ss;
		System.out.println(" [" + initTime + " ms]");

		System.out.println("Start training IBM Model 3...");
		int iter = 1;
		while (!CONVERGE) {
			// System.out.print("Iteration " + iter);
			System.out.println("Iteration " + iter);
			long timeSample = 0;
			long timeCount = 0;
			long timeUpdate = 0;
			int totalSample = 0;
			long start = System.currentTimeMillis();

			// initialize
			int maxFert = 0;

			initCountT();
			initTotalT();

			if (iter > 1) {
				initCountD();
				initTotalD();
				initCountN();
				initTotalN();
			}

			countP0 = 0;
			countP1 = 0;
			int countPair = 0;

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();

				// log
				// if (p.getE().length() > 1)
				// System.out.print("Pair " + (countPair + 1) + ": " +
				// tarDict.getWord(p.getE().get(1)) + " "
				// + tarDict.getWord(p.getE().get(2)) + "...");

				// Sample the alignment space
				long startSample = System.currentTimeMillis();
				Set<Alignment> listA = sample(p);
				long endSample = System.currentTimeMillis();
				timeSample += endSample - startSample;
				totalSample += listA.size();

				// log
				// System.out.print(" Sampling: OK.");

				// Collect counts
				double subTotal = 0;

				for (Alignment align : listA) {
					subTotal += align.getProbability();
				}

				for (Alignment align : listA) {
					double c = align.getProbability() / subTotal;
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
						FertWord fw = getFertWord(fertility, f);
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

				listA.clear();

				++countPair;
				if (countPair % 1000 == 0 || countPair == sentPairs.size()) {
					System.out.println("Pair " + countPair + ", total samples = " + totalSample + ", total time: "
							+ (System.currentTimeMillis() - start) + " ms");
				}

				long endCount = System.currentTimeMillis();
				timeCount += endCount - endSample;

				// log
				// System.out.println(" Counting: OK.");
			}

			long startUpdate = System.currentTimeMillis();
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

			long totalTime = end - start;
			timeUpdate = end - startUpdate;

			System.out.println("Number of samples: " + totalSample);
			System.out.println("Sample time: " + timeSample + " ms");
			System.out.println("Count time: " + timeCount + " ms");
			System.out.println("Update time: " + timeUpdate + " ms");
			System.out.println("Total time: " + totalTime + " ms");
			// System.out.println(" [" + totalTime + " ms]");

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
		if (countN == null) {
			countN = new TObjectDoubleHashMap<FertWord>();
		} else {
			Set<FertWord> keySet = countN.keySet();

			for (FertWord fw : keySet) {
				countN.put(fw, 0.0);
			}
		}

	}

	private void initFertility() {
		n = new TObjectDoubleHashMap<FertWord>();
		fertWordHashCode = new int[maxLe + 1][foDict.size()];

		double value = 1 / (double) (maxLe + 1);

		for (SentencePair p : sentPairs) {
			int le = p.getE().length();
			for (int fert = 0; fert <= le; fert++) {
				for (int i = 0; i <= p.getF().length(); i++) {
					int f = p.getF().get(i);
					int hashCode = Utils.generateTwoIntegersHashCode(fert, f);
					fertWordHashCode[fert][f] = hashCode;
					FertWord fw = getFertWord(fert, f);
					n.put(fw, value);
				}
			}
		}
	}

	private FertWord getFertWord(int fert, int f) {
		return new FertWord(fert, f, fertWordHashCode[fert][f]);
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
	private Set<Alignment> sample(SentencePair p) {
		Set<Alignment> listA = new HashSet<Alignment>();
		int le = p.getE().length();
		int lf = p.getF().length();

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
				Alignment al = new Alignment(align, phi);
				al.setProbability(probability(al, p));

				if (al.getProbability() > 0) {
					listA.addAll(hillClimbingAndNeighboring(al, j, p));
				}
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
	 * This function computes the best alignment on local and returns its
	 * neighboring alignments. It gets some neighboring alignments and finds out
	 * the alignment with highest probability in those alignment spaces. If the
	 * current alignment recorded has the highest probability, then stop the
	 * search loop. If not, then continue the search loop until it finds out the
	 * highest probability of alignment in local.
	 */
	private Set<Alignment> hillClimbingAndNeighboring(Alignment align, int jPegged, SentencePair p) {
		Set<Alignment> result = new HashSet<Alignment>();

		while (true) {

			Set<Alignment> listAlign = findMaxNeighboring(align, jPegged, p);

			// neighbors don't have higher probability, we have the result
			if (listAlign.size() > 1 || align.getA().length == 1) {
				result = listAlign;
				break;
			}

			// neighbors have higher probability
			for (Alignment newAlign : listAlign) {
				align = newAlign;
			}
		}

		return result;
	}

	private Set<Alignment> findMaxNeighboring(Alignment align, int jPegged, SentencePair p) {
		int[] a = align.getA();
		int[] phi = align.getPhi();

		int le = p.getE().length();
		int lf = p.getF().length();

		double maxScore = 1.0;

		int maxJ = -1;
		int maxII = -1;
		int maxJ1 = -1;
		int maxJ2 = -1;

		double[][][] scoreMove = new double[le + 1][lf + 1][lf + 1];
		double[][] scoreSwap = new double[le + 1][le + 1];

		// Moves
		for (int j = 1; j <= le; j++) {
			if (j == jPegged) {
				continue;
			}

			int i = a[j];

			for (int ii = 0; ii <= lf; ii++) {
				if (ii != i) {
					double score = scoreOfMove(align, p, i, ii, j);
					if (score > maxScore) {
						maxScore = score;

						maxJ = j;
						maxII = ii;
					}
					scoreMove[j][i][ii] = score;
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

				if (a[j2] != a[j1]) {
					double score = scoreOfSwap(align, p, j1, j2);
					if (score > maxScore) {
						maxScore = score;

						maxJ1 = j1;
						maxJ2 = j2;
					}
					scoreSwap[j1][j2] = score;
				}
			}
		}

		Set<Alignment> listN = new HashSet<Alignment>();

		// returns the alignment having the highest probability which is bigger
		// than the current probability
		if (maxJ1 > 0 && maxJ2 > 0) {
			int j1 = maxJ1;
			int j2 = maxJ2;

			int[] newA = cloneIntArray(a);
			int[] newPhi = cloneIntArray(phi);

			newA[j1] = a[j2];
			newA[j2] = a[j1];

			// Compute new probability
			double newProb = maxScore * align.getProbability();
			align = new Alignment(newA, newPhi, newProb);

			listN.add(align);

			return listN;
		}

		if (maxJ > 0 && maxII >= 0) {
			int j = maxJ;
			int ii = maxII;

			int i = a[j];

			int[] newA = cloneIntArray(a);
			newA[j] = ii;

			int[] newPhi = cloneIntArray(phi);

			if (newPhi[i] > 0) {
				newPhi[i]--;
				newPhi[ii]++;
			}

			// Compute new probability
			double newProb = maxScore * align.getProbability();
			align = new Alignment(newA, newPhi, newProb);

			listN.add(align);

			return listN;
		}

		// no higher probability, returns the list
		// Moves
		for (int j = 1; j <= le; j++) {
			if (j == jPegged) {
				continue;
			}

			int i = a[j];

			for (int ii = 0; ii <= lf; ii++) {
				if (ii != i) {
					int[] newA = cloneIntArray(a);
					newA[j] = ii;

					int[] newPhi = cloneIntArray(phi);

					if (newPhi[i] > 0) {
						newPhi[i]--;
						newPhi[ii]++;
					}

					// Compute new probability
					double score = scoreMove[j][i][ii];
					if (score == 0) {
						continue;
					}
					double newProb = score * align.getProbability();
					listN.add(new Alignment(newA, newPhi, newProb));
				}

				else {
					listN.add(align);
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

				if (a[j2] != a[j1]) {

					int[] newA = cloneIntArray(a);
					int[] newPhi = cloneIntArray(phi);

					newA[j1] = a[j2];
					newA[j2] = a[j1];

					// Compute new probability
					double score = scoreSwap[j1][j2];
					if (score == 0) {
						continue;
					}
					double newProb = score * align.getProbability();
					listN.add(new Alignment(newA, newPhi, newProb));
				}
			}
		}

		return listN;
	}

	/**
	 * return (p(a'|e,f)/p(a|e,f)) whereas a' is move of a
	 */
	double scoreOfMove(Alignment al, SentencePair p, int i, int ii, int j) {
		if (i == ii) {
			return 1.0;
		}

		int le = p.getE().length();
		int lf = p.getF().length();

		int[] phi = al.getPhi();

		if (le - 2 * phi[0] + 2 <= 0 && le - phi[0] <= 0) {
			return 0.0;
		}

		int f_i = p.getF().get(i);
		int f_ii = p.getF().get(ii);

		double s = 0;

		s += Math.log(t.get(p.getWordPair(j, ii)));
		s -= Math.log(t.get(p.getWordPair(j, i)));

		if (i > 0 && ii > 0) {
			s += Math.log(phi[ii] + 1);
			s -= Math.log(phi[i]);

			s += Math.log(n.get(getFertWord(phi[ii] + 1, f_ii)));
			s -= Math.log(n.get(getFertWord(phi[ii], f_ii)));

			s += Math.log(n.get(getFertWord(phi[i] - 1, f_i)));
			s -= Math.log(n.get(getFertWord(phi[i], f_i)));

			s += Math.log(d[j][ii][le][lf]);
			s -= Math.log(d[j][i][le][lf]);

		}

		if (i == 0) {
			s += Math.log(phi[ii] + 1);

			s += Math.log(n.get(getFertWord(phi[ii] + 1, f_ii)));
			s -= Math.log(n.get(getFertWord(phi[ii], f_ii)));

			s += Math.log(d[j][ii][le][lf]);

			s += Math.log(phi[0]);
			s -= Math.log(le - 2 * phi[0] + 1);

			s += Math.log(le - phi[0] + 1);
			s -= Math.log(le - 2 * phi[0] + 2);

			s += Math.log(Math.pow(p0, 2));
			s -= Math.log(1 - p0);

		}

		if (ii == 0) {
			s -= Math.log(phi[i]);

			s += Math.log(n.get(getFertWord(phi[i] - 1, f_i)));
			s -= Math.log(n.get(getFertWord(phi[i], f_i)));

			s -= Math.log(d[j][i][le][lf]);

			s += Math.log(le - 2 * phi[0] - 1);
			s -= Math.log(le - phi[0]);

			s += Math.log(le - 2 * phi[0]);
			s -= Math.log(phi[0] + 1);

			s += Math.log(1 - p0);
			s -= Math.log(Math.pow(p0, 2));

		}

		if (Double.isNaN(s)) {
			return 0.0;
		}

		return Math.exp(s);
	}

	/**
	 * return (p(a'|e,f)/p(a|e,f)) whereas a' is swap of a
	 */
	double scoreOfSwap(Alignment al, SentencePair p, int j1, int j2) {
		int[] a = al.getA();
		int i1 = a[j1];
		int i2 = a[j2];

		if (j1 == j2 || i1 == i2) {
			return 1.0;
		}

		int le = p.getE().length();
		int lf = p.getF().length();

		double s = 0.0;

		s += Math.log(t.get(p.getWordPair(j2, i1)));
		s -= Math.log(t.get(p.getWordPair(j1, i1)));

		s += Math.log(t.get(p.getWordPair(j1, i2)));
		s -= Math.log(t.get(p.getWordPair(j2, i2)));

		if (i1 > 0 && i2 > 0) {
			s += Math.log(d[j2][i1][le][lf]);
			s -= Math.log(d[j1][i1][le][lf]);

			s += Math.log(d[j1][i2][le][lf]);
			s -= Math.log(d[j2][i2][le][lf]);
		}

		else if (i1 == 0) {
			s += Math.log(d[j1][i2][le][lf]);
			s -= Math.log(d[j2][i2][le][lf]);
		}

		else if (i2 == 0) {
			s += Math.log(d[j2][i1][le][lf]);
			s -= Math.log(d[j1][i1][le][lf]);
		}

		// System.out.println("Swap: " + s);

		if (Double.isNaN(s)) {
			return 0.0;
		}

		return Math.exp(s);
	}

	/**
	 * This function returns the probability given an alignment. The phi
	 * variable represents the fertility according to the current alignment,
	 * which records how many output words are generated by each input word.
	 */
	private double probability(Alignment align, SentencePair p) {
		int[] a = align.getA();
		int[] phi = align.getPhi();

		int le = p.getE().length();
		int lf = p.getF().length();

		if (le - 2 * phi[0] <= 0) {
			return 0.0;
		}

		double p1 = 1 - p0;

		double total = 0.0;

		// Compute the NULL insertion
		total += Math.log(Math.pow(p1, phi[0]) * Math.pow(p0, le - 2 * phi[0]));

		// Compute the combination (le - fert[0]) choose fert[0]
		for (int i = 1; i <= phi[0]; i++) {
			total += Math.log((le - phi[0] - i + 1) / i);
		}

		// Compute fertilities term
		for (int i = 0; i <= lf; i++) {
			int f = p.getF().get(i);
			try {
				total += Math.log(CombinatoricsUtils.factorial(phi[i]) * n.get(getFertWord(phi[i], f)));
			} catch (MathArithmeticException e) {
				if (phi[i] > 20) {
					total += Math.log(n.get(getFertWord(phi[i], f)));
					total += Math.log(CombinatoricsUtils.factorial(20));

					for (int tmp = 21; tmp <= phi[i]; tmp++) {
						total += Math.log(tmp);
					}
				} else {
					throw new MathArithmeticException();
				}
			}
		}

		// Multiply the lexical and distortion probabilities
		for (int j = 1; j <= le; j++) {
			int i = a[j];
			total += Math.log(t.get(p.getWordPair(j, i)));
			total += Math.log(d[j][i][le][lf]);
		}

		return Math.exp(total);
	}

	static int[] cloneIntArray(int[] src) {
		int[] dest = new int[src.length];
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}

	public double getDistortionProbability(int j, int i, int le, int lf) {
		try {
			return d[j][i][le][lf];
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0.0;
		}
	}

	public double getFertilityProbability(int fert, int f) {
		FertWord fw = new FertWord(fert, f);
		if (n.contains(fw)) {
			return n.get(fw);
		} else {
			return 0.0;
		}
	}

	public double getP0() {
		return p0;
	}

	@Override
	public void printModels() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

		super.printModels();

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
				FertWord fw = getFertWord(fert, f);
				if (n.contains(fw)) {
					System.out.println("n(" + fw.getFert() + "|" + foDict.getWord(fw.getF()) + ") = " + n.get(fw));
				}
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
			folder = folder + "/";
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
