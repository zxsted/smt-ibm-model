package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Dictionary {
	private Map<String, Integer> dict;
	private Map<Integer, String> converseDict;

	public Dictionary(String filename) {
		dict = new HashMap<String, Integer>();
		converseDict = new HashMap<Integer, String>();

		BufferedReader br = null;

		try {
			br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);
			int count = 0;
			for (String line; (line = br.readLine()) != null;) {
				if (line.isEmpty()) {
					continue;
				}

				String[] tokens = line.split("\\s+");
				for (String tok : tokens) {
					if (!dict.containsKey(tok)) {
						dict.put(tok, count);
						converseDict.put(count, tok);
						count++;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getIndex(String word) {
		return dict.get(word);
	}

	public String getWord(int index) {
		return converseDict.get(index);
	}

	public Map<String, Integer> getDict() {
		return dict;
	}

	public int size() {
		return dict.size();
	}

	@Override
	public String toString() {
		return dict.toString();
	}
}
