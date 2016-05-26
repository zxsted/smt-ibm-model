package vn.edu.vnu.uet.nlp.smt.demo;

import vn.edu.vnu.uet.nlp.smt.IBMModel2;

public class Demo {

	public static void main(String[] args) {
		String enFile = "toy/toy.en";
		String deFile = "toy/toy.de";

		// IBMModel1 em = new IBMModel1(enFile, deFile);
		//
		// em.printDicts();
		// em.train();

		IBMModel2 md = new IBMModel2(enFile, deFile);

		md.printDicts();
		md.train();
	}

}
