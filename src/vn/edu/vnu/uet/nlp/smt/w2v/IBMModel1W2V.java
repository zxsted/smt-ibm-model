package vn.edu.vnu.uet.nlp.smt.w2v;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.alignment.ReadData;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel1;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModelAbstract;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;
import vn.edu.vnu.uet.nlp.smt.utils.IConstants;
import vn.edu.vnu.uet.nlp.smt.utils.Utils;

public class IBMModel1W2V extends IBMModelAbstract {
	public static final double delta = 0.8;
	public static final double MAX_DISTANCE = 25;
	public static final double MIN_DISTANCE = 1;
	private Word2VecContainer eW2V;
	private Word2VecContainer fW2V;
	private TObjectDoubleHashMap<WordPair> dis;
	private double[][] weight;

	public IBMModel1W2V(String enFile, String foFile) {
		this(enFile, foFile, false);
	}

	public IBMModel1W2V(String model) {
		super(model);

		if (!model.endsWith("/")) {
			model = model + "/";
		}

		String cosineFile = model + IConstants.distanceFile;

		try {
			System.out.print("Loading distances...");
			long start = System.currentTimeMillis();
			dis = Utils.loadObject(cosineFile);
			long end = System.currentTimeMillis();
			long time = end - start;
			System.out.println(" [" + time + " ms]");

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public IBMModel1W2V(String targetFile, String sourceFile, boolean usingNull) {
		super(targetFile, sourceFile, usingNull);
	}

	public IBMModel1W2V(IBMModel1 md) {
		super(md);
	}

	public IBMModel1W2V(String enFile, String foFile, String eW2VFile, String fW2VFile) throws Exception {
		this(enFile, foFile, false);

		System.out.print("Load word2vec models...");
		long start = System.currentTimeMillis();
		eW2V = new Word2VecContainer(eW2VFile);
		fW2V = new Word2VecContainer(fW2VFile);
		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(" [" + time + " ms]");

		System.out.println("Source word2vec model: size = " + fW2V.getDictSize() + " vector = " + fW2V.getVecSize());
		System.out.println("Target word2vec model: size = " + eW2V.getDictSize() + " vector = " + eW2V.getVecSize());
	}

	public IBMModel1W2V(String enFile, String foFile, String eW2VFile, String fW2VFile, String weightFile)
			throws Exception {
		this(enFile, foFile, eW2VFile, fW2VFile);

		System.out.println("Loading transfer weight file...");
		BufferedReader br = Files.newBufferedReader(Paths.get(weightFile));
		String firstLine = br.readLine();
		String[] toks = firstLine.split("\\s+");
		int size = Integer.parseInt(toks[0]);
		int size2 = Integer.parseInt(toks[1]);

		weight = new double[size][size2];
		int i = 0;
		for (String line; (line = br.readLine()) != null;) {
			if (i >= size) {
				break;
			}
			String[] vals = line.split("\\s+");
			for (int j = 0; j < size2; j++) {
				weight[i][j] = Double.parseDouble(vals[j]);
			}
			i++;
		}

		computeDistance();
	}

	public void printSimilaritySamples(String file) throws IOException {
		BufferedWriter bw = ReadData.newBufferedWrite(file);

		int line = 0;

		for (SentencePair p : sentPairs) {
			bw.write("Pair " + line);
			bw.newLine();

			int le = p.getE().length();
			int lf = p.getF().length();

			for (int j = 1; j <= le; j++) {
				String eWord = enDict.getWord(p.getE().get(j));
				bw.write(eWord + " ");

				Map<String, Double> map = new HashMap<String, Double>();
				for (int i = iStart; i <= lf; i++) {
					String fWord = foDict.getWord(p.getF().get(i));
					WordPair ef = p.getWordPair(j, i);
					double s = dis.get(ef);
					map.put(fWord, s);
				}

				map = sortByValue(map);
				Set<String> keySet = map.keySet();
				for (String key : keySet) {
					String str = "(" + key + ", " + map.get(key) + ") ";
					bw.write(str);
				}
				bw.newLine();
			}
			bw.newLine();

			line++;

			if (line % 1000 == 0) {
				bw.flush();
			}
		}

		bw.flush();
		bw.close();
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream()
				.sorted(Map.Entry
						.comparingByValue(/* Collections.reverseOrder() */))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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
		try {
			mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void mainLoop() throws IOException {
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
						subTotal += t.get(ef) * Math.pow(delta, dis.get(ef));
					}

					// collect counts
					for (int i = iStart; i <= lf; i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);
						double c = t.get(ef) * Math.pow(delta, dis.get(ef)) / subTotal;

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

	@Override
	public void save(String folder) throws IOException {
		super.save(folder);

		File fol = new File(folder);
		if (!fol.exists()) {
			fol.mkdir();
		}

		if (!fol.isDirectory()) {
			System.err.println(folder + " is not a folder! Cannot save model!");
			return;
		}

		if (!folder.endsWith("/")) {
			folder = folder + "/";
		}

		// Save distances
		String cosine = folder + IConstants.distanceFile;
		Utils.saveObject(dis, cosine);
	}

	public void computeDistance() {
		System.out.print("Compute distance between words...");
		long ss = System.currentTimeMillis();
		dis = new TObjectDoubleHashMap<WordPair>();
		for (SentencePair p : sentPairs) {
			int le = p.getE().length();
			int lf = p.getF().length();

			for (int j = 1; j <= le; j++) {
				String eWord = enDict.getWord(p.getE().get(j));

				for (int i = iStart; i <= lf; i++) {
					WordPair ef = p.getWordPair(j, i);
					String fWord = foDict.getWord(p.getF().get(i));

					if (!dis.containsKey(ef)) {
						double s = computeDistanceOfTwoWordInTwoLanguages(eWord, fWord);
						dis.put(ef, s);
					}

				}
			}
		}
		long ee = System.currentTimeMillis();
		long computeTime = ee - ss;
		System.out.println(" [" + computeTime + " ms]");
	}

	private double computeDistanceOfTwoWordInTwoLanguages(String eWord, String fWord) {
		if (eWord.equals(fWord)) {
			return MIN_DISTANCE;
		}
		Vector<Double> fTrans = transfer(fWord);
		return distance(eW2V.getVec(eWord), fTrans);
	}

	private Map<String, Vector<Double>> transferMap;

	private Vector<Double> transfer(String fWord) {
		if (transferMap == null) {
			transferMap = new HashMap<String, Vector<Double>>();
		}

		if (!fW2V.inMap(fWord)) {
			return null;
		}

		if (transferMap.containsKey(fWord)) {
			return transferMap.get(fWord);
		} else {
			Vector<Double> vec = fW2V.getVec(fWord);
			Vector<Double> newVec = new Vector<Double>();

			for (int i = 0; i < eW2V.getVecSize(); i++) {
				double val = 0;
				for (int j = 0; j < weight[i].length; j++) {
					val += weight[i][j] * vec.get(j);
				}
				newVec.add(val);
			}
			transferMap.put(fWord, newVec);
			return newVec;
		}
	}

	public TObjectDoubleHashMap<WordPair> getDis() {
		return dis;
	}

	public double getDistance(String eWord, String fWord) {
		if (eWord.equals(fWord)) {
			return MIN_DISTANCE;
		}

		if (!(enDict.containsWord(eWord) && foDict.containsWord(fWord))) {
			return MAX_DISTANCE;
		}

		int e = enDict.getIndex(eWord);
		int f = foDict.getIndex(fWord);

		WordPair ef = new WordPair(e, f);

		if (!dis.containsKey(ef)) {
			return MAX_DISTANCE;
		}

		return dis.get(ef);
	}

}
