package vn.edu.vnu.uet.nlp.smt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sentence {
	private String content;
	private List<String> words;
	private Map<String, Integer> dict;

	public Sentence(String _content) {
		content = _content;
		words = Arrays.asList(content.split("\\s+"));

		initDict();
	}

	private void initDict() {
		dict = new HashMap<String, Integer>();
		int c = 0;
		for (String word : words) {
			if (!dict.containsKey(word)) {
				dict.put(word, c);
				c++;
			}
		}
	}

	public int getIndexInDict(String word) {
		return dict.get(word);
	}

	public int dictSize() {
		return dict.size();
	}

	public String getContent() {
		return content;
	}

	public List<String> getWords() {
		return words;
	}

	public Map<String, Integer> getDict() {
		return dict;
	}
	
	@Override
	public String toString() {
		return content + "\n" + words + "\n" + dict;
	}
}
