package vn.edu.vnu.uet.nlp.smt;

public class Sentence {
	private int[] content;

	public Sentence(int[] array) {
		content = new int[array.length];

		for (int i = 0; i < content.length; i++) {
			content[i] = array[i];
		}
	}

	public int get(int index) {
		return content[index - 1];
	}

	public int length() {
		return content.length;
	}
}