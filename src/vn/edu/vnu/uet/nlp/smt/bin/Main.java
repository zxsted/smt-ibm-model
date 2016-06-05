/*******************************************************************************
 * Copyright [2016] [Nguyen Tuan Phong]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package vn.edu.vnu.uet.nlp.smt.bin;

import java.io.IOException;

import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel2;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel3;

/**
 * @author tuanphong94
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
		// String enFile = "/home/tuanphong94/workspace/smt-data/toy/toy.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/toy/toy.de";
		// String enFile = "/home/tuanphong94/workspace/smt-data/50.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/50.vn";
		// String enFile = "/home/tuanphong94/workspace/smt-data/200.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/200.vn";
		// String enFile = "/home/tuanphong94/workspace/smt-data/1k.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/1k.vn";
		// String enFile = "/home/tuanphong94/workspace/smt-data/100k.en";
		// String foFile = "/home/tuanphong94/workspace/smt-data/100k.vn";
//		String enFile = "/home/tuanphong94/workspace/smt-data/290k.en";
//		String foFile = "/home/tuanphong94/workspace/smt-data/290k.vn";
//		IBMModel2 model = new IBMModel2(enFile, foFile, true);
//		model.train();
//		// model.printModels();
//		model.save("/home/tuanphong94/workspace/smt-data/model/");

		// System.out.println("Load saved model...");
		// IBMModel3 loadedModel = new
		// IBMModel3("/home/tuanphong94/workspace/smt-data/model/");
		// loadedModel.printTransProbs();

		 String folder = "/home/tuanphong94/workspace/smt-data/model/";

		// IBMModel1 md1 = new IBMModel1(enFile, foFile, true);
		// md1.train();
		// md1.printModels();
		// md1.save(folder);

		// IBMModel1 md1 = new IBMModel1(folder);
		//
		// IBMModel2 md2 = new IBMModel2(md1);
		// md2.train();
		// md2.printModels();
		// md2.save(folder);

		 IBMModel2 md2 = new IBMModel2(folder);
		
		 IBMModel3 md3 = new IBMModel3(md2);
		 md3.train();
		// md3.printModels();
		// md3.save(folder);
	}

}
