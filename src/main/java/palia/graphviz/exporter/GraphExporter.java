package palia.graphviz.exporter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import palia.graphviz.Dot;
import palia.graphviz.DotNode;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class GraphExporter {

	public static Dot exportTPA(TPA tpa) {
		Map<UUID, DotNode> idToNodes = new HashMap<>();
		
		Dot dot = new Dot();
		for (Node n : tpa.getNodes()) {
			DotNode dotNode = dot.addNode(n.getName());
			idToNodes.put(n.getId(), dotNode);
		}
		
		for(Transition t : tpa.getTransitions()) {
			for (Node source : t.getSourceNodes()) {
				for (Node target : t.getEndNodes()) {
					dot.addEdge(idToNodes.get(source.getId()), idToNodes.get(target.getId()));
				}
			}
		}
		
		return dot;
	}
	
	public static void exportSVG(TPA tpa, File destination) throws IOException {
		exportSVG(exportTPA(tpa), destination);
	}
	
	public static void exportSVG(Dot dot, File destination) throws IOException {
		export(dot, Format.SVG, destination);
	}
	
	private static void export(Dot dot, Format format, File destination) throws IOException {
		Graphviz.fromString(dot.toString()).render(Format.SVG).toFile(destination);
	}
}
