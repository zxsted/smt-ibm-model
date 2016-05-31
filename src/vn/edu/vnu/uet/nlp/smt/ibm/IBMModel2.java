package vn.edu.vnu.uet.nlp.smt.ibm;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;
import vn.edu.vnu.uet.nlp.smt.utils.IConstants;
import vn.edu.vnu.uet.nlp.smt.utils.Utils;

public class IBMModel2 extends IBMModel1 {

	double[][][][] a;
	double[][][][] countA;
	double[][][] totalA;

	int maxLe = 0, maxLf = 0;

	public IBMModel2(String enFile, String foFile) {
		super(enFile, foFile);
	}

	public IBMModel2(String model) {
		super(model);

		String aFileName = model + IConstants.alignmentModelName;
		try {
			a = Utils.loadArray(aFileName);
			String maxLeLf = Utils.loadMaxLeLf(aFileName);
			String[] tokens = maxLeLf.split(" ");
			maxLe = Integer.parseInt(tokens[0]);
			maxLf = Integer.parseInt(tokens[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initAlignment() {
		for (SentencePair p : sentPairs) {
			if (p.getE().length() > maxLe) {
				maxLe = p.getE().length();
			}

			if (p.getF().length() > maxLf) {
				maxLf = p.getF().length();
			}
		}

		a = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];

		for (int lf = 1; lf <= maxLf; lf++) {
			double value = 1 / (double) (lf + 1);
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						a[i][j][le][lf] = value;
					}
				}
			}
		}
	}

	@Override
	public void train() {
		super.train();
		initAlignment();

		System.out.println("Start training IBM Model 2...");
		int iter = 1;
		while (!CONVERGE) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			// initialize
			initCount();
			initTotal();

			initCountA();
			initTotalA();

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();

				double subTotal;

				// compute normalization
				for (int j = 1; j <= le; j++) {
					subTotal = 0;

					for (int i = 1; i <= lf; i++) {
						WordPair ef = p.getWordPair(j, i);
						subTotal += t.get(ef) * a[i][j][le][lf];
					}

					// collect counts
					for (int i = 1; i <= lf; i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);

						double c = t.get(ef) * a[i][j][le][lf] / subTotal;

						if (count.containsKey(ef)) {
							count.put(ef, count.get(ef) + c);
						} else {
							count.put(ef, c);
						}

						total[f] += c;

						countA[i][j][le][lf] += c;
						totalA[j][le][lf] += c;
					}
				}
			}

			// estimate probabilities
			Set<WordPair> keySet = count.keySet();

			for (WordPair ef : keySet) {
				double value = count.get(ef) / total[ef.getF()];
				t.put(ef, value);
			}

			for (int lf = 1; lf <= maxLf; lf++) {
				for (int le = 1; le <= maxLe; le++) {
					for (int i = 1; i <= maxLf; i++) {
						for (int j = 1; j <= maxLe; j++) {
							a[i][j][le][lf] = countA[i][j][le][lf] / totalA[j][le][lf];
						}
					}
				}
			}

			long end = System.currentTimeMillis();

			long time = end - start;

			System.out.println(" [" + time + " ms]");

			// printTransProbs();

			iter++;
			if (iter > MAX_ITER_2) {
				CONVERGE = true;
			}
		}
	}

	private void initTotalA() {
		totalA = new double[maxLe + 1][maxLe + 1][maxLf + 1];
	}

	private void initCountA() {
		countA = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];
	}

	@Override
	public void printTransProbs() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

		System.out.println("Translation probabilities:");
		super.printTransProbs();

		System.out.println("Alignment probabilities:");
		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						double value = a[i][j][le][lf];
						if (value <= 1 && value > 0) {
							System.out.println("a(" + j + "|" + i + ", " + le + ", " + lf + ") = " + value);
						}
					}
				}
			}
		}
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

		// Save alignment
		String aFileName = folder + IConstants.alignmentModelName;
		Utils.saveArray(a, maxLe, maxLf, aFileName);
	}

}
