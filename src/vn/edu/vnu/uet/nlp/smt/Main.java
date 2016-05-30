package vn.edu.vnu.uet.nlp.smt;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		String enFile = "/home/tuanphong94/workspace/smt-data/1k.en";
		String foFile = "/home/tuanphong94/workspace/smt-data/1k.vn";
		IBMModel2 model = new IBMModel2(enFile, foFile);
		model.train();
		model.saveModel("/home/tuanphong94/workspace/smt-data/model.1k.en-vi.smt");
	}

}
