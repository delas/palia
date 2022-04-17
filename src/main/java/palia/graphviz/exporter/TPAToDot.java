package palia.graphviz.exporter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import palia.graphviz.Dot;
import palia.graphviz.DotEdge;
import palia.graphviz.DotNode;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class TPAToDot {

	public static Dot export(TPA tpa) {
		Map<UUID, DotNode> idToNodes = new HashMap<>();
		
		Dot dot = new Dot();
		dot.setOption("rankdir", "LR");
		dot.setOption("outputorder", "edgesfirst");
		
		DotNode startNode = dot.addNode("@Start");
		startNode.setOption("shape", "circle");
		startNode.setOption("style", "filled");
		startNode.setOption("fillcolor", "#048ab5");
		startNode.setOption("fontcolor", "white");
		startNode.setOption("fontname", "Arial");
		startNode.setOption("fontsize", "8");
		
		DotNode endNode = dot.addNode("@End");
		endNode.setOption("shape", "circle");
		endNode.setOption("style", "filled");
		endNode.setOption("fillcolor", "#048ab5");
		endNode.setOption("fontcolor", "white");
		endNode.setOption("fontname", "Arial");
		endNode.setOption("fontsize", "8");
		
		for (Node n : tpa.getNodes()) {
			DotNode dotNode = dot.addNode(n.getName());
			dotNode.setOption("shape", "box");
			dotNode.setOption("style", "rounded,filled");
			dotNode.setOption("fillcolor", "#70b356");
			dotNode.setOption("fontname", "Arial");
			dotNode.setOption("fontsize", "9");
			idToNodes.put(n.getId(), dotNode);
			
			if (n.isStartingNode()) {
				dot.addEdge(startNode, dotNode);
			}
			if (n.isFinalNode()) {
				dot.addEdge(dotNode, endNode);
			}
		}
		
		for(Transition t : tpa.getTransitions()) {
			for (Node source : t.getSourceNodes()) {
				for (Node target : t.getEndNodes()) {
					DotEdge edge = dot.addEdge(idToNodes.get(source.getId()), idToNodes.get(target.getId()));
					edge.setOption("tailclip", "false");
				}
			}
		}
		
		return dot;
	}
}
