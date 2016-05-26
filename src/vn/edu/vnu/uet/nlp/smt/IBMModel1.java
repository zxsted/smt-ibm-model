package vn.edu.vnu.uet.nlp.smt;

public class IBMModel1 extends IBMModelAbstract {
	public IBMModel1(String enFile, String foFile) {
		super(enFile, foFile);
	}

	@Override
	public void train() {
		System.out.println("Start training IBM Model 1. Initial translation probabilities:");
//		printTransProbs();

		int iter = 1;
		while (!CONVERGE) {
			System.out.println("Iteration " + iter);

			initCount();
			initTotal();

			for (SentencePair p : sentPairs) {
				// compute normalization
				double[] subTotal = new double[p.getE().dictSize()];

				for (String e : p.getE().getWords()) {
					int indexE = enDict.getIndex(e);
					int subIndexE = p.getE().getIndexInDict(e);
					subTotal[subIndexE] = 0;

					for (String f : p.getF().getWords()) {
						int indexF = foDict.getIndex(f);
						subTotal[subIndexE] += t[indexE][indexF];
					}
				}

				// collect counts
				for (String e : p.getE().getWords()) {
					int indexE = enDict.getIndex(e);
					int subIndexE = p.getE().getIndexInDict(e);

					for (String f : p.getF().getWords()) {
						int indexF = foDict.getIndex(f);
						count[indexE][indexF] += t[indexE][indexF] / subTotal[subIndexE];
						total[indexF] += t[indexE][indexF] / subTotal[subIndexE];
					}
				}
			}

			// estimate probabilities
			for (int f = 0; f < foDict.size(); f++) {
				for (int e = 0; e < enDict.size(); e++) {
					t[e][f] = count[e][f] / total[f];
				}
			}

			iter++;
			if (iter > MAX_ITER_1) {
				CONVERGE = true;
			}
		}
		
//		printTransProbs();

		CONVERGE = false; // to be continuously used in IBM Model 2
	}

}
