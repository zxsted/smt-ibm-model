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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel3;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModelAbstract;

/**
 * @author tuanphong94
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
		RunOption option = new RunOption();
		CmdLineParser parser = new CmdLineParser(option);

		if (args.length < 6) {
			System.out.println(Main.class.getName() + " [arguments..]");
			parser.printUsage(System.out);
			return;
		}

		try {
			parser.parseArgument(args);
			IBMModelAbstract.MAX_ITER_1 = option.n1;
			IBMModelAbstract.MAX_ITER_2 = option.n2;
			IBMModelAbstract.MAX_ITER_3 = option.n3;

			String enFile = option.tar.getAbsolutePath();
			String foFile = option.src.getAbsolutePath();

			IBMModel3 model = new IBMModel3(enFile, foFile);
			model.train();
			model.save(option.output.getAbsolutePath());

		} catch (CmdLineException e) {
			System.out.println(Main.class.getName() + " [arguments..]");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

}
