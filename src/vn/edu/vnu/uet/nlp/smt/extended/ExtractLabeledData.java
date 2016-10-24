package vn.edu.vnu.uet.nlp.smt.extended;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.structs.Sentence;

public class ExtractLabeledData {

	public static void main(String[] args) throws IOException {
		String number = "10k";
		String enFile = "/home/tuanphong94/workspace/smt-data/" + number + ".en";
		String vnFile = "/home/tuanphong94/workspace/smt-data/" + number + ".vn";
		String labeledData = "/home/tuanphong94/workspace/smt-data/alignment/full.fix.align";

		IBMModel1Extended model = new IBMModel1Extended(vnFile, enFile, labeledData);

		List<LabeledSentencePair> labeledSentPairs = model.labeledSentPairs;

		Path path = Paths.get("/home/tuanphong94/workspace/smt-data/alignment/labeled/lbd.en");
		Charset cs = StandardCharsets.UTF_8;

		BufferedWriter bw1 = Files.newBufferedWriter(path, cs, StandardOpenOption.CREATE);
		bw1 = Files.newBufferedWriter(path, cs, StandardOpenOption.WRITE);
		bw1 = Files.newBufferedWriter(path, cs, StandardOpenOption.TRUNCATE_EXISTING);

		path = Paths.get("/home/tuanphong94/workspace/smt-data/alignment/labeled/lbd.vn");
		BufferedWriter bw2 = Files.newBufferedWriter(path, cs, StandardOpenOption.CREATE);
		bw2 = Files.newBufferedWriter(path, cs, StandardOpenOption.WRITE);
		bw2 = Files.newBufferedWriter(path, cs, StandardOpenOption.TRUNCATE_EXISTING);

		path = Paths.get("/home/tuanphong94/workspace/smt-data/alignment/labeled/en-vn.notnull.align");
		BufferedWriter bw3 = Files.newBufferedWriter(path, cs, StandardOpenOption.CREATE);
		bw3 = Files.newBufferedWriter(path, cs, StandardOpenOption.WRITE);
		bw3 = Files.newBufferedWriter(path, cs, StandardOpenOption.TRUNCATE_EXISTING);

		path = Paths.get("/home/tuanphong94/workspace/smt-data/alignment/labeled/vn-en.notnull.align");
		BufferedWriter bw4 = Files.newBufferedWriter(path, cs, StandardOpenOption.CREATE);
		bw4 = Files.newBufferedWriter(path, cs, StandardOpenOption.WRITE);
		bw4 = Files.newBufferedWriter(path, cs, StandardOpenOption.TRUNCATE_EXISTING);

		for (LabeledSentencePair pair : labeledSentPairs) {
			Sentence sentE = pair.getF();
			Sentence sentV = pair.getE();

			Set<SingleAlignment> align = pair.getAlignment();

			int le = sentE.length();
			int lv = sentV.length();

			StringBuilder sb = new StringBuilder();
			for (int i = 1; i <= le; i++) {
				sb.append(model.getSourceDict().getWord(sentE.get(i)) + " ");
			}
			bw1.write(sb.toString().trim());
			bw1.newLine();
			bw1.flush();

			sb = new StringBuilder();
			for (int j = 1; j <= lv; j++) {
				sb.append(model.getTrgDict().getWord(sentV.get(j)) + " ");
			}
			bw2.write(sb.toString().trim());
			bw2.newLine();
			bw2.flush();

			List<SingleAlignment> list = new ArrayList<SingleAlignment>(align);
			Collections.sort(list, new Comparator<SingleAlignment>() {

				@Override
				public int compare(SingleAlignment o1, SingleAlignment o2) {
					if (o1.src > o2.src) {
						return 1;
					}

					else if (o1.src < o2.src) {
						return -1;
					}

					else {
						if (o1.trg > o2.trg) {
							return 1;
						}

						else if (o1.trg < o2.trg) {
							return -1;
						}
					}

					return 0;
				}

			});

			sb = new StringBuilder();
			for (SingleAlignment a : list) {
				sb.append(a.src + "-" + a.trg + " ");
			}
			bw3.write(sb.toString().trim());
			bw3.newLine();
			bw3.flush();

			Collections.sort(list, new Comparator<SingleAlignment>() {

				@Override
				public int compare(SingleAlignment o1, SingleAlignment o2) {
					if (o1.trg > o2.trg) {
						return 1;
					}

					else if (o1.trg < o2.trg) {
						return -1;
					}

					else {
						if (o1.src > o2.src) {
							return 1;
						}

						else if (o1.src < o2.src) {
							return -1;
						}
					}

					return 0;
				}

			});

			sb = new StringBuilder();
			for (SingleAlignment a : list) {
				sb.append(a.trg + "-" + a.src + " ");
			}
			bw4.write(sb.toString().trim());
			bw4.newLine();
			bw4.flush();
		}
	}

}
