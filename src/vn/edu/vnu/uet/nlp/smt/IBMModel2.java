package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class IBMModel2 extends IBMModel1 {

	double[][][][] a; // a[i][j][le][lf]
						// i: english word position
						// j: foreign word position
	double[][][][] countA; // countA[i][j][le][lf]
	double[][][] totalA; // totalA[i][le][lf]

	int maxLe = 0, maxLf = 0;

	public IBMModel2(String enFile, String foFile) {
		super(enFile, foFile);

		initAlignment();
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

		a = new double[enDict.size()][foDict.size()][maxLe + 1][maxLf + 1];

		for (int lf = 1; lf < maxLf + 1; lf++) {
			double value = 1 / (double) (lf + 1);
			for (int le = 1; le < maxLe + 1; le++) {
				for (int j = 0; j < foDict.size(); j++) {
					for (int i = 0; i < enDict.size(); i++) {
						a[i][j][le][lf] = value;
					}
				}
			}
		}
	}

	@Override
	public void train() {
		super.train();

		System.out.println("Start training IBM Model 2.");

		int iter = 1;
		while (!CONVERGE) {
			System.out.println("Iteration " + iter);

			// initialize
			initCount();
			initTotal();

			initCountA();
			initTotalA();

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();

				double[] subTotal = new double[p.getE().dictSize()];

				// compute normalization
				for (int i1 = 0; i1 < le; i1++) {
					String e = p.getE().getWords().get(i1);
					int subIndexE = p.getE().getIndexInDict(e);
					int indexE = enDict.getIndex(e);

					subTotal[subIndexE] = 0;

					for (int j1 = 0; j1 < lf; j1++) {
						String f = p.getF().getWords().get(j1);
						int indexF = foDict.getIndex(f);

						double x1 = t[indexE][indexF];
						double x2 = a[i1][j1][le][lf];

						subTotal[subIndexE] += x1 * x2;
					}
				}

				// collect counts
				for (int i2 = 0; i2 < le; i2++) {
					String e = p.getE().getWords().get(i2);
					int subIndexE = p.getE().getIndexInDict(e);
					int indexE = enDict.getIndex(e);

					for (int j2 = 0; j2 < lf; j2++) {
						String f = p.getF().getWords().get(j2);
						int indexF = foDict.getIndex(f);

						double c = t[indexE][indexF] * a[i2][j2][le][lf] / subTotal[subIndexE];

						count[indexE][indexF] += c;
						total[indexF] += c;
						countA[i2][j2][le][lf] += c;
						totalA[i2][le][lf] += c;
					}
				}

			}

			// estimate probabilities
			for (int f = 0; f < foDict.size(); f++) {
				for (int e = 0; e < enDict.size(); e++) {
					t[e][f] = count[e][f] / total[f];
				}
			}

			for (int l2 = 1; l2 < maxLf + 1; l2++) {
				for (int l1 = 1; l1 < maxLe + 1; l1++) {
					for (int j = 0; j < foDict.size(); j++) {
						for (int i = 0; i < enDict.size(); i++) {
							a[i][j][l1][l2] = countA[i][j][l1][l2] / totalA[i][l1][l2];
						}
					}
				}
			}

			iter++;
			if (iter > MAX_ITER_2) {
				CONVERGE = true;
			}
		}
	}

	private void initTotalA() {
		totalA = new double[enDict.size()][maxLe + 1][maxLf + 1];
	}

	private void initCountA() {
		countA = new double[enDict.size()][foDict.size()][maxLe + 1][maxLf + 1];
	}

	@Override
	public void printTransProbs() {
		System.out.println("Translation probabilities:");
		super.printTransProbs();

		System.out.println("Alignment probabilities:");
		for (int lf = 1; lf < maxLf + 1; lf++) {
			for (int le = 1; le < maxLe + 1; le++) {
				for (int j = 0; j < foDict.size(); j++) {
					for (int i = 0; i < enDict.size(); i++) {
						if (a[i][j][le][lf] <= 1)
							System.out.println("a(" + j + "|" + i + ", " + le + ", " + lf + ") = " + a[i][j][le][lf]);
					}
				}
			}
		}
	}

	public void printModel(String filename) throws IOException {
		Path path = Paths.get(filename);
		BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
		bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

		for (int f = 0; f < foDict.size(); f++) {
			bw.write(foDict.getWord(f) + ": ");

			for (int e = 0; e < enDict.size(); e++) {
				if (t[e][f] > 0.1) {
					bw.write("(" + enDict.getWord(e) + ", " + t[e][f] + ")");
				}
			}
			bw.newLine();
			bw.flush();
		}
	}

}
