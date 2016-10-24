package vn.edu.vnu.uet.nlp.smt.w2v;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.structs.Dictionary;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public class WordPOSDistances {
	private static final double MAX_DISTANCE = 12;
	public static final double MIN_DISTANCE = 0.4;
	public static final double delta = 0.5;

	private Word2VecContainer srcCon;
	private Word2VecContainer trgCon;

	private TObjectDoubleHashMap<WordPair> disMap;
	private TObjectDoubleHashMap<WordPair> factorMap;

	float[][] weight;

	private Map<String, Vector<Double>> transferMap;

	public WordPOSDistances(String srcFile, String trgFile, String weightFile, Dictionary srcDict, Dictionary trgDict,
			Set<WordPair> t) throws Exception {

		System.out.print("Load word2vec models...");
		long start = System.currentTimeMillis();
		srcCon = new Word2VecContainer(srcFile);
		trgCon = new Word2VecContainer(trgFile);
		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(" [" + time + " ms]");

		System.out
				.println("Source word2vec model: size = " + srcCon.getDictSize() + " vector = " + srcCon.getVecSize());
		System.out
				.println("Target word2vec model: size = " + trgCon.getDictSize() + " vector = " + trgCon.getVecSize());

		compute(weightFile, srcDict, trgDict, t);
	}

	private void compute(String weightFile, Dictionary srcDict, Dictionary trgDict, Set<WordPair> keySet)
			throws IOException {
		System.out.print("Compute distances...");
		long start = System.currentTimeMillis();

		disMap = new TObjectDoubleHashMap<WordPair>();
		factorMap = new TObjectDoubleHashMap<WordPair>();

		weight = computeWeight(weightFile);

		for (WordPair wp : keySet) {
			int sI = wp.getF();
			int tI = wp.getE();

			String src = srcDict.getWord(sI);
			String trg = trgDict.getWord(tI);

			double dis;

			if (src.equals(trg)) {
				dis = MIN_DISTANCE;
			} else {
				try {
					String ss = src.substring(0, src.lastIndexOf('/'));
					String tt = trg.substring(0, trg.lastIndexOf('/'));

					if (ss.equals(tt)) {
						dis = MIN_DISTANCE;
					} else {
						Vector<Double> srcVec = transfer(src);
						Vector<Double> trgVec = trgCon.getVec(trg);

						dis = distance(srcVec, trgVec);
					}

				} catch (IndexOutOfBoundsException e) {
					Vector<Double> srcVec = transfer(src);
					Vector<Double> trgVec = trgCon.getVec(trg);

					dis = distance(srcVec, trgVec);
				}
			}

			disMap.put(wp, dis);
			factorMap.put(wp, Math.pow(delta, dis));
		}

		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(" [" + time + " ms]");
	}

	public double getDistance(WordPair ef) {
		return disMap.get(ef);
	}

	public double getFactor(WordPair ef) {
		return factorMap.get(ef);
	}

	private double distance(Vector<Double> a, Vector<Double> b) {
		if (a == null || b == null) {
			return MAX_DISTANCE;
		}

		double dis = 0;
		int size = a.size();
		for (int i = 0; i < size; i++) {
			dis += (a.elementAt(i) - b.elementAt(i)) * (a.elementAt(i) - b.elementAt(i));
		}
		return Math.sqrt(dis);
	}

	private Vector<Double> transfer(String src) {
		if (transferMap == null) {
			transferMap = new HashMap<String, Vector<Double>>();
		}

		if (!srcCon.inMap(src)) {
			return null;
		}

		if (transferMap.containsKey(src)) {
			return transferMap.get(src);
		} else {
			Vector<Double> vec = srcCon.getVec(src);
			Vector<Double> newVec = new Vector<Double>();

			for (int i = 0; i < trgCon.getVecSize(); i++) {
				double val = 0;
				for (int j = 0; j < weight[i].length; j++) {
					val += weight[i][j] * vec.get(j);
				}
				newVec.add(val);
			}
			transferMap.put(src, newVec);
			return newVec;
		}
	}

	private float[][] computeWeight(String weightFile) throws IOException {
		float[][] w;

		BufferedReader br = Files.newBufferedReader(Paths.get(weightFile));
		String first = br.readLine();
		String[] dim = first.split("\\s+");

		int row = Integer.parseInt(dim[0]);
		int col = Integer.parseInt(dim[1]);

		w = new float[row][col];

		for (int i = 0; i < row; i++) {
			String line = br.readLine();
			String[] val = line.split("\\s+");
			for (int j = 0; j < col; j++) {
				float v = Float.parseFloat(val[j]);
				w[i][j] = v;
			}
		}

		return w;
	}
}
