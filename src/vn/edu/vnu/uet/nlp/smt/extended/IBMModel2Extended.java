package vn.edu.vnu.uet.nlp.smt.extended;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;
import vn.edu.vnu.uet.nlp.smt.utils.IConstants;
import vn.edu.vnu.uet.nlp.smt.utils.Utils;

public class IBMModel2Extended extends IBMModel1Extended {

	double[][][][] a;
	double[][][][] countA;
	double[][][] totalA;

	int maxLe = 0, maxLf = 0;

	public IBMModel2Extended(String enFile, String foFile, String labeledData) throws IOException {
		super(enFile, foFile, labeledData);
	}

	public IBMModel2Extended(String model) {
		super(model);
		if (!model.endsWith("/")) {
			model = model + "/";
		}

		String aFileName = model + IConstants.alignmentModelName;
		try {
			System.out.print("Loading alignment probability...");
			long start = System.currentTimeMillis();
			a = Utils.loadArray(aFileName);
			long end = System.currentTimeMillis();
			long time = end - start;
			System.out.println(" [" + time + " ms]");
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
		double[][][][] labeledA = computeAFromLabeledData();

		for (int lf = 1; lf <= maxLf; lf++) {
			double value = 1 / (double) (lf + 1);
			for (int le = 1; le <= maxLe; le++) {
				for (int i = iStart; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						a[i][j][le][lf] = value + labeledA[i][j][le][lf];
					}
				}
			}
		}

		// nomalization
		normalizeA();
	}

	private double[][][][] computeAFromLabeledData() {
		double[][][][] result = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];
		double[][][][] count = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];
		double[][][] total = new double[maxLe + 1][maxLe + 1][maxLf + 1];

		for (LabeledSentencePair pair : labeledSentPairs) {
			int le = pair.getE().length();
			int lf = pair.getF().length();

			Set<SingleAlignment> set = pair.getAlignment();
			for (SingleAlignment a : set) {
				int j = a.getTrg();
				int i = a.getSrc();

				count[i][j][le][lf]++;
				total[j][le][lf]++;
			}
		}

		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = iStart; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						double c = count[i][j][le][lf];
						double t = total[j][le][lf];

						if (t > 0) {
							result[i][j][le][lf] = c / t;
						} else {
							result[i][j][le][lf] = 0;
						}
					}
				}
			}
		}

		return result;
	}

	@Override
	public void train() {
		super.train(false);

		System.out.print("Initializing IBM Model 2 Extended...");
		long ss = System.currentTimeMillis();
		initAlignment();
		long ee = System.currentTimeMillis();
		long initTime = ee - ss;
		System.out.println(" [" + initTime + " ms]");

		System.out.println("Start training IBM Model 2 Extended...");
		mainLoop2();
	}

	public void mainLoop2() {
		for (int iter = 1; iter <= MAX_ITER_2; iter++) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			// initialize
			initCountT();
			initTotalT();

			initCountA();
			initTotalA();

			for (SentencePair p : sentPairs) {
				int le = p.getE().length();
				int lf = p.getF().length();

				double subTotal;

				// compute normalization
				for (int j = 1; j <= le; j++) {
					subTotal = 0;

					for (int i = iStart; i <= lf; i++) {
						WordPair ef = p.getWordPair(j, i);
						subTotal += t.get(ef) * a[i][j][le][lf];
					}

					// collect counts
					for (int i = iStart; i <= lf; i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);

						double c = t.get(ef) * a[i][j][le][lf] / subTotal;

						if (countT.containsKey(ef)) {
							countT.put(ef, countT.get(ef) + c);
						} else {
							countT.put(ef, c);
						}

						totalT[f] += c;

						countA[i][j][le][lf] += c;
						totalA[j][le][lf] += c;
					}
				}
			}

			// estimate probabilities
			Set<WordPair> keySet = countT.keySet();

			for (WordPair ef : keySet) {
				double value = countT.get(ef) / totalT[ef.getF()];
				t.put(ef, value);
			}

			for (int lf = 1; lf <= maxLf; lf++) {
				for (int le = 1; le <= maxLe; le++) {
					for (int i = iStart; i <= lf; i++) {
						for (int j = 1; j <= le; j++) {
							a[i][j][le][lf] = countA[i][j][le][lf] / totalA[j][le][lf];
						}
					}
				}
			}

			long end = System.currentTimeMillis();

			long time = end - start;

			System.out.println(" [" + time + " ms]");
		}
	}

	private void initTotalA() {
		totalA = new double[maxLe + 1][maxLe + 1][maxLf + 1];
	}

	private void initCountA() {
		countA = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];
	}

	private void normalizeA() {
		initTotalA();
		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = iStart; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						totalA[j][le][lf] += a[i][j][le][lf];
					}
				}
			}
		}

		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = iStart; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						a[i][j][le][lf] = a[i][j][le][lf] / totalA[j][le][lf];
					}
				}
			}
		}
	}

	public double getAlignmentProbability(int i, int j, int le, int lf) {
		try {
			return a[i][j][le][lf];
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0.0;
		}
	}

	@Override
	public void printModels() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

		super.printModels();

		System.out.println("Alignment probabilities:");
		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = iStart; i <= lf; i++) {
					for (int j = 1; j <= le; j++) {
						double value = a[i][j][le][lf];
						if (value <= 1 && value > 0)
							System.out.println("a(" + i + "|" + j + ", " + le + ", " + lf + ") = " + value);
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
			folder = folder + "/";
		}

		// Save alignment
		String aFileName = folder + IConstants.alignmentModelName;
		Utils.saveArray(a, maxLf, maxLe, maxLe, maxLf, aFileName, iStart);
	}

	public int getMaxLe() {
		return maxLe;
	}

	public int getMaxLf() {
		return maxLf;
	}
}
