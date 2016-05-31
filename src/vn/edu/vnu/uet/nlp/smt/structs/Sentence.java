package vn.edu.vnu.uet.nlp.smt.structs;

import java.util.ArrayList;
import java.util.List;

public class Sentence {
	private List<Integer> content;

	public Sentence(int[] array) {
		content = new ArrayList<Integer>(array.length);

		for (int i = 0; i < array.length; i++) {
			content.add(array[i]);
		}
	}

	public int get(int index) {
		return content.get(index - 1);
	}

	public int length() {
		return content.size();
	}
}