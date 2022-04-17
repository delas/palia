package palia.graphviz.exporter;

import java.io.File;
import java.io.IOException;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import palia.graphviz.Dot;
import palia.model.TPA;

public class GraphExporter {

	public static void exportSVG(TPA tpa, File destination) throws IOException {
		exportSVG(TPAToDot.export(tpa), destination);
	}
	
	public static void exportSVG(Dot dot, File destination) throws IOException {
		export(dot, Format.SVG, destination);
	}
	
	private static void export(Dot dot, Format format, File destination) throws IOException {
		Graphviz.fromString(dot.toString()).render(Format.SVG).toFile(destination);
	}
}
