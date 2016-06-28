package vn.edu.vnu.uet.nlp.smt.bin;

import java.io.File;

import org.kohsuke.args4j.Option;

public class RunOption {
	@Option(name = "-src", usage = "source file")
	File src;

	@Option(name = "-tar", usage = "target file")
	File tar;

	@Option(name = "-o", usage = "output path")
	File output;

	@Option(name = "-n1", usage = "number of iteration for Model 1")
	int n1;

	@Option(name = "-n2", usage = "number of iteration for Model 1")
	int n2;

	@Option(name = "-n3", usage = "number of iteration for Model 1")
	int n3;

	@Option(name = "-l", usage = "maximum length of a sentence (default: 30)")
	int maxLength = 30;
}
