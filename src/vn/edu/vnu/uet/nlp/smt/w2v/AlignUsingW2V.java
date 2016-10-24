package vn.edu.vnu.uet.nlp.smt.w2v;

import java.io.BufferedWriter;
import java.util.List;

import vn.edu.vnu.uet.nlp.smt.alignment.AlignForModel1;
import vn.edu.vnu.uet.nlp.smt.alignment.Alignment;
import vn.edu.vnu.uet.nlp.smt.alignment.ReadData;
import vn.edu.vnu.uet.nlp.smt.alignment.Sentence;
import vn.edu.vnu.uet.nlp.smt.alignment.SentencePair;

public class AlignUsingW2V {
	public static final String number = "10k";

	public static void main(String[] args) throws Exception {
		String srcFile = "/home/tuanphong94/workspace/smt-data/" + number + ".en";
		String trgFile = "/home/tuanphong94/workspace/smt-data/" + number + ".vn";
		String trgW2V = "/home/tuanphong94/workspace/smt-data/w2v/290k.w2v.en";
		String vW2V = "/home/tuanphong94/workspace/smt-data/w2v/290k.w2v.vn";
		String weightFile = "/home/tuanphong94/workspace/smt-data/w2v/w.gd";
		String testFile = "/home/tuanphong94/app/aligner/all/bitext.en.vn.txt";
		String outFile = "/home/tuanphong94/app/aligner/all/align.w2v.guessed";

		IBMModel1W2V model = new IBMModel1W2V(trgFile, srcFile, vW2V, trgW2V, weightFile);

		BufferedWriter bw = ReadData.newBufferedWrite(outFile);
		List<SentencePair> pairs = AlignForModel1.getSentencePairs(testFile);

		for (SentencePair pair : pairs) {
			Sentence en = pair.getSentE();
			Sentence vn = pair.getSentV();

			for (int j = 0; j < vn.length(); j++) {
				String v = vn.get(j);

				double minDis = IBMModel1W2V.MAX_DISTANCE;
				int max_i = -1;
				for (int i = 0; i < en.length(); i++) {
					String e = en.get(i);

					double newDis = model.getDistance(v, e);
					if (newDis < minDis) {
						minDis = newDis;
						max_i = i;
					}
				}

				if (max_i >= 0) {
					pair.addAlignment(new Alignment(max_i, j, false));
				}

			}

			bw.write(ReadData.string(pair.getAlignment()));
			bw.newLine();
			bw.flush();

		}
	}
}
