package vn.edu.vnu.uet.nlp.smt;

import java.util.HashMap;
import java.util.Map;

public class IBMModel1 extends IBMModelAbstract {
	public IBMModel1(String enFile, String foFile) {
		super(enFile, foFile);
	}

	@Override
	public void train() {
		System.out.println("Start training IBM Model 1...");

		int iter = 1;
		while (!CONVERGE) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();
			
			initCount();
			initTotal();

			for (SentencePair p : sentPairs) {
				// compute normalization
				Map<Integer, Double> subTotal = new HashMap<Integer, Double>();

				for (int j = 1; j <= p.getE().length(); j++) {
					int e = p.getE().get(j);
					subTotal.put(e, 0.0);

					for (int i = 1; i <= p.getF().length(); i++) {
						int f = p.getF().get(i);

						subTotal.put(e, subTotal.get(e) + t.get(new WordPair(e, f)));
					}
				}

				// collect counts
				for (int j = 1; j <= p.getE().length(); j++) {
					int e = p.getE().get(j);

					for (int i = 1; i <= p.getF().length(); i++) {
						int f = p.getF().get(i);
						WordPair ef = new WordPair(e, f);
						double c = t.get(ef) / subTotal.get(e);

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
			
			long end = System.currentTimeMillis();
			
			long time = end - start;
			
			System.out.println(" [" + time + " ms]");

			iter++;
			if (iter > MAX_ITER_1) {
				CONVERGE = true;
			}
		}

		CONVERGE = false; // to be continuously used in IBM Model 2
	}

}
