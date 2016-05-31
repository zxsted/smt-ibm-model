package vn.edu.vnu.uet.nlp.smt;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		String enFile = "/home/tuanphong94/workspace/smt-data/toy/toy.en";
		String foFile = "/home/tuanphong94/workspace/smt-data/toy/toy.de";
		// String enFile = "/home/tuanphong94/workspace/smt-data/100k.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/100k.vn";
		// String enFile = "/home/tuanphong94/workspace/smt-data/290k.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/290k.vn";
		IBMModel2 model = new IBMModel2(enFile, foFile);
		model.train();
		model.printTransProbs();
		model.save("/home/tuanphong94/workspace/smt-data/model/");
		
		System.out.println("Load saved model...");
		IBMModel2 loadedModel = new IBMModel2("/home/tuanphong94/workspace/smt-data/model/");
		loadedModel.printTransProbs();
	}

}
