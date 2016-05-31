package vn.edu.vnu.uet.nlp.smt.structs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Dictionary implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2971140367051267737L;

	private Map<String, Integer> dict;
	private Map<Integer, String> reverseDict;

	public Dictionary(String filename) {
		dict = new HashMap<String, Integer>();
		reverseDict = new HashMap<Integer, String>();

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
						reverseDict.put(count, tok);
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

	public boolean containsWord(String word) {
		return dict.containsKey(word);
	}

	public String getWord(int index) {
		return reverseDict.get(index);
	}

	public boolean containsIndex(int index) {
		return reverseDict.containsKey(index);
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
