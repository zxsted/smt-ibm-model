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

import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel3;

/**
 * @author tuanphong94
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
//		String enFile = "/home/tuanphong94/workspace/smt-data/toy/toy.en";
//		String foFile = "/home/tuanphong94/workspace/smt-data/toy/toy.de";
//		 String enFile = "/home/tuanphong94/workspace/smt-data/50.en";
//		 String foFile = "/home/tuanphong94/workspace/smt-data/50.vn";
//		 String enFile = "/home/tuanphong94/workspace/smt-data/200.en";
//		 String foFile = "/home/tuanphong94/workspace/smt-data/200.vn";
//		 String enFile = "/home/tuanphong94/workspace/smt-data/1k.en";
//		 String foFile = "/home/tuanphong94/workspace/smt-data/1k.vn";
//		 String enFile = "/home/tuanphong94/workspace/smt-data/100k.en";
//		 String foFile = "/home/tuanphong94/workspace/smt-data/100k.vn";
		 String enFile = "/home/tuanphong94/workspace/smt-data/290k.en";
		 String foFile = "/home/tuanphong94/workspace/smt-data/290k.vn";
		IBMModel3 model = new IBMModel3(enFile, foFile);
		model.train();
//		model.printTransProbs();
		// model.save("/home/tuanphong94/workspace/smt-data/model/");

		// System.out.println("Load saved model...");
		// IBMModel3 loadedModel = new
		// IBMModel3("/home/tuanphong94/workspace/smt-data/model/");
		// loadedModel.printTransProbs();
	}

}
