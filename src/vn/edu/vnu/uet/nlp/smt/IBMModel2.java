package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.map.hash.TObjectDoubleHashMap;

public class IBMModel2 extends IBMModel1 {

	TObjectDoubleHashMap<TwoPositionsAndTwoLengths> a;
	TObjectDoubleHashMap<TwoPositionsAndTwoLengths> countA;
	TObjectDoubleHashMap<OnePositionAndTwoLengths> totalA;

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

		a = new TObjectDoubleHashMap<TwoPositionsAndTwoLengths>();

		for (int lf = 1; lf <= maxLf; lf++) {
			double value = 1 / (double) (lf + 1);
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						TwoPositionsAndTwoLengths obj = new TwoPositionsAndTwoLengths(i, j, le, lf);
						a.put(obj, value);
					}
				}
			}
		}
	}

	@Override
	public void train() {
		super.train();

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

				Map<Integer, Double> subTotal = new HashMap<Integer, Double>();

				// compute normalization
				for (int j = 1; j <= le; j++) {
					int e = p.getE().get(j);
					subTotal.put(e, 0.0);

					for (int i = 1; i <= lf; i++) {
						int f = p.getF().get(i);

						WordPair ef = new WordPair(e, f);
						TwoPositionsAndTwoLengths pos = new TwoPositionsAndTwoLengths(i, j, le, lf);

						double x1 = t.get(ef);
						double x2 = a.get(pos);

						subTotal.put(e, subTotal.get(e) + x1 * x2);
					}
				}

				// collect counts
				for (int j = 1; j <= le; j++) {
					int e = p.getE().get(j);

					for (int i = 1; i <= lf; i++) {
						int f = p.getF().get(i);

						WordPair ef = new WordPair(e, f);
						TwoPositionsAndTwoLengths pos = new TwoPositionsAndTwoLengths(i, j, le, lf);

						double c = t.get(ef) * a.get(pos) / subTotal.get(e);

						if (count.containsKey(ef)) {
							count.put(ef, count.get(ef) + c);
						} else {
							count.put(ef, c);
						}

						if (total.containsKey(f)) {
							total.put(f, total.get(f) + c);
						} else {
							total.put(f, c);
						}

						if (countA.containsKey(pos)) {
							countA.put(pos, countA.get(pos) + c);
						} else {
							countA.put(pos, c);
						}

						OnePositionAndTwoLengths onePos = new OnePositionAndTwoLengths(j, le, lf);

						if (totalA.contains(onePos)) {
							totalA.put(onePos, totalA.get(onePos) + c);
						} else {
							totalA.put(onePos, c);
						}
					}
				}

			}

			// estimate probabilities
			for (int f = 0; f < foDict.size(); f++) {
				for (int e = 0; e < enDict.size(); e++) {
					WordPair ef = new WordPair(e, f);
					double value = count.get(ef) / total.get(f);
					t.put(ef, value);
				}
			}

			for (int lf = 1; lf <= maxLf; lf++) {
				for (int le = 1; le <= maxLe; le++) {
					for (int i = 1; i <= maxLf; i++) {
						for (int j = 1; j <= maxLe; j++) {
							TwoPositionsAndTwoLengths pos = new TwoPositionsAndTwoLengths(i, j, le, lf);
							OnePositionAndTwoLengths onePos = new OnePositionAndTwoLengths(j, le, lf);

							double value = countA.get(pos) / totalA.get(onePos);

							a.put(pos, value);
						}
					}
				}
			}

			long end = System.currentTimeMillis();

			long time = end - start;

			System.out.println(" [" + time + " ms]");

			iter++;
			if (iter > MAX_ITER_2) {
				CONVERGE = true;
			}
		}

	}

	private void initTotalA() {
		totalA = new TObjectDoubleHashMap<OnePositionAndTwoLengths>();
	}

	private void initCountA() {
		countA = new TObjectDoubleHashMap<TwoPositionsAndTwoLengths>();
	}

	@Override
	public void printTransProbs() {
		System.out.println("Translation probabilities:");
		super.printTransProbs();

		System.out.println("Alignment probabilities:");
		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						double value = a.get(new TwoPositionsAndTwoLengths(i, j, le, lf));
						if (value <= 1) {
							System.out.println("a(" + j + "|" + i + ", " + le + ", " + lf + ") = " + value);
						}
					}
				}
			}
		}
	}

	public void saveModel(String filename) throws IOException {
		System.out.println("Saving model...");

		Path path = Paths.get(filename);
		BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
		bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

		for (int f = 0; f < foDict.size(); f++) {
			bw.write(foDict.getWord(f) + ": ");

			for (int e = 0; e < enDict.size(); e++) {
				if (getProb(e, f) > 0.01) {
					bw.write("(" + enDict.getWord(e) + ", " + getProb(e, f) + ")");
				}
			}
			bw.newLine();
			bw.flush();
		}
	}

}
