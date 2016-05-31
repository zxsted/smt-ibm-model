package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Utils {
	public static void saveArray(double[][][][] a, int maxLe, int maxLf, String filename) throws IOException {
		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
		}
		Path path = Paths.get(file.getAbsolutePath());

		BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.WRITE);
		bw = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING);

		bw.write("maxLe = " + maxLe + "\n");
		bw.write("maxLf = " + maxLf + "\n");

		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						bw.write(a[i][j][le][lf] + "");
						bw.newLine();
					}
				}
			}
			bw.flush();
		}

		bw.close();
	}

	public static double[][][][] loadArray(String filename) throws IOException {
		File file = new File(filename);
		if (!file.exists()) {
			System.err.println(filename + "does not exist!");
			return null;
		}
		Path path = Paths.get(file.getAbsolutePath());

		BufferedReader br = Files.newBufferedReader(path);

		String first = br.readLine();
		if (!first.startsWith("maxLe = ")) {
			System.err.println("First line of aligment model does match standard form!");
			return null;
		}

		String second = br.readLine();
		if (!second.startsWith("maxLf = ")) {
			System.err.println("Second line of aligment model does match standard form!");
			return null;
		}

		int maxLe = Integer.parseInt(first.substring("maxLe = ".length()));
		int maxLf = Integer.parseInt(second.substring("maxLf = ".length()));

		double[][][][] a = new double[maxLf + 1][maxLe + 1][maxLe + 1][maxLf + 1];

		String line;

		for (int lf = 1; lf <= maxLf; lf++) {
			for (int le = 1; le <= maxLe; le++) {
				for (int i = 1; i <= maxLf; i++) {
					for (int j = 1; j <= maxLe; j++) {
						line = br.readLine();
						if (line != null && !line.isEmpty()) {
							if (line.equals("NaN")) {
								a[i][j][le][lf] = 0.0;
							} else {
								a[i][j][le][lf] = Double.parseDouble(line);
							}
						}
					}
				}
			}
		}

		return a;
	}

	public static String loadMaxLeLf(String filename) throws IOException {
		File file = new File(filename);
		if (!file.exists()) {
			System.err.println(filename + "does not exist!");
			return null;
		}
		Path path = Paths.get(file.getAbsolutePath());

		BufferedReader br = Files.newBufferedReader(path);

		String first = br.readLine();
		if (!first.startsWith("maxLe = ")) {
			System.err.println("First line of aligment model does match standard form!");
			return null;
		}

		String second = br.readLine();
		if (!second.startsWith("maxLf = ")) {
			System.err.println("Second line of aligment model does match standard form!");
			return null;
		}

		return first.substring("maxLe = ".length()) + " " + second.substring("maxLf = ".length());
	}
}
