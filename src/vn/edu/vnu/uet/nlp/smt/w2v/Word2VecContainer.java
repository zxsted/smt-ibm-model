package vn.edu.vnu.uet.nlp.smt.w2v;

import java.io.BufferedReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Word2VecContainer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2466350448187861027L;
	private static final String DOT = "</s>";
	private Map<String, Vector<Double>> map;
	private int mapSize;
	private int vecSize;

	public Word2VecContainer(String filename) throws Exception {
		BufferedReader br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);

		String first = br.readLine();
		if (first == null || first.isEmpty()) {
			throw new Exception("Word2Vec model is improper: at first line!");
		}

		String[] toks = first.split("\\s+");
		if (toks.length != 2) {
			throw new Exception("Word2Vec model is improper: at first line!");
		}

		mapSize = Integer.parseInt(toks[0]);
		vecSize = Integer.parseInt(toks[1]);

		map = new HashMap<String, Vector<Double>>(mapSize);
		for (String line; (line = br.readLine()) != null;) {
			if (line.isEmpty()) {
				continue;
			}

			String[] tokens = line.trim().split("\\s+");
			if (tokens.length != vecSize + 1) {
				throw new Exception("Word2Vec model is improper: at line" + line + "!");
			}

			String word = tokens[0];
			Vector<Double> vec = new Vector<Double>(vecSize);
			for (int i = 1; i <= vecSize; i++) {
				vec.add(Double.parseDouble(tokens[i]));
			}

			map.put(word, vec);
		}

		if (map.size() != mapSize) {
			throw new Exception("Word2Vec model is improper: number of words is wrong!");
		}
	}

	public Vector<Double> getVec(String word) {
		return map.get(word);
	}

	public boolean inMap(String word) {
		return map.containsKey(word);
	}

	public int getDictSize() {
		return mapSize;
	}

	public int getVecSize() {
		return vecSize;
	}

	public Vector<Double> dotToWord(String word) {
		if (!map.containsKey(word)) {
			return null;
		}

		Vector<Double> result = new Vector<Double>(vecSize);
		Vector<Double> vec = map.get(word);
		Vector<Double> dot = map.get(DOT);

		for (int i = 0; i < vecSize; i++) {
			result.add(vec.elementAt(i) - dot.elementAt(i));
		}

		return result;
	}
}
