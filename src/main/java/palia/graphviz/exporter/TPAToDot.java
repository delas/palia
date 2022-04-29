package palia.graphviz.exporter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import palia.graphviz.Dot;
import palia.graphviz.DotEdge;
import palia.graphviz.DotNode;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class TPAToDot {

	public static Dot exportTPA(TPA tpa) {
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

		for (Transition t : tpa.getTransitions()) {
			for (Node source : t.getSourceNodes()) {
				for (Node target : t.getEndNodes()) {
					DotEdge edge = dot.addEdge(idToNodes.get(source.getId()), idToNodes.get(target.getId()));
					edge.setOption("tailclip", "false");
					edge.setOption("label", "" + t.getFrequency());
					edge.setOption("fontname", "Arial");
					edge.setOption("fontsize", "10");
				}
			}
		}

		return dot;
	}

	public static Dot export(TPA tpa) {
		Map<UUID, DotNode> idToStartNodes = new HashMap<>();
		Map<UUID, DotNode> idToTargetNodes = new HashMap<>();

		Dot dot = new Dot();
		dot.setOption("rankdir", "LR");
		dot.setOption("outputorder", "edgesfirst");
//		dot.setOption("splines", "ortho");

		DotNode startNode = makeUtilityNode(dot, "@Start");
		DotNode endNode = makeUtilityNode(dot, "@End");

		for (Node n : tpa.getNodes()) {
			DotNode dotNode = makeActivityNode(dot, n.getName());
			idToStartNodes.put(n.getId(), dotNode);
			idToTargetNodes.put(n.getId(), dotNode);

			if (n.isStartingNode()) {
				dot.addEdge(startNode, dotNode);
			}
			if (n.isFinalNode()) {
				dot.addEdge(dotNode, endNode);
			}
		}

		for (Node n : tpa.getNodes()) {
			Set<Transition> outgoing = n.getOutTransitions(false);
			if (outgoing.size() > 1) {
				DotNode gateway = makeGatewayNode(dot, "&times;");
				makeEdge(dot, idToStartNodes.get(n.getId()), gateway);
				idToStartNodes.put(n.getId(), gateway);
			}

			Set<Transition> incoming = n.getInTransitions(false);
			if (incoming.size() > 1) {
				DotNode gateway = makeGatewayNode(dot, "&times;");
				makeEdge(dot, gateway, idToTargetNodes.get(n.getId()));
				idToTargetNodes.put(n.getId(), gateway);
			}
		}

		for (Transition t : tpa.getTransitions()) {
			Collection<Node> sources = t.getSourceNodes();
			Collection<Node> targets = t.getEndNodes();

			// sequence flow or XOR gateway
			if (sources.size() == 1 && targets.size() == 1) {
				DotNode sourceNode = idToStartNodes.get(sources.stream().findAny().get().getId());
				DotNode targetNode = idToTargetNodes.get(targets.stream().findAny().get().getId());
				makeEdge(dot, sourceNode, targetNode);
			}

			// parallel split
			if (sources.size() == 1 && targets.size() > 1) {
				DotNode sourceNode = idToStartNodes.get(sources.stream().findAny().get().getId());
				DotNode gateway = makeGatewayNode(dot, "+");
				makeEdge(dot, sourceNode, gateway);
				sourceNode = gateway;

				for (Node target : targets) {
					DotNode targetNode = idToTargetNodes.get(target.getId());
					makeEdge(dot, sourceNode, targetNode);
				}
			}

			// parallel join
			if (sources.size() > 1 && targets.size() == 1) {
				DotNode targetNode = idToTargetNodes.get(targets.stream().findAny().get().getId());
				DotNode gateway = makeGatewayNode(dot, "+");
				makeEdge(dot, gateway, targetNode);
				targetNode = gateway;

				for (Node source : sources) {
					DotNode sourceNode = idToStartNodes.get(source.getId());
					makeEdge(dot, sourceNode, targetNode);
				}
			}

			// parallel join AND parallel split
			if (sources.size() > 1 && targets.size() > 1) {
				DotNode gateway1 = makeGatewayNode(dot, "+");
				DotNode gateway2 = makeGatewayNode(dot, "+");
				makeEdge(dot, gateway1, gateway2);
				for (Node source : sources) {
					makeEdge(dot, idToStartNodes.get(source.getId()), gateway1);
				}
				for (Node target : targets) {
					makeEdge(dot, gateway2, idToTargetNodes.get(target.getId()));
				}
			}
		}

		return dot;
	}

	private static DotEdge makeEdge(Dot dot, DotNode source, DotNode target) {
		DotEdge edge = dot.addEdge(source, target);
		edge.setOption("tailclip", "false");
		return edge;
	}

	private static DotNode makeActivityNode(Dot dot, String name) {
		DotNode dotNode = dot.addNode(name);
		dotNode.setOption("shape", "box");
		dotNode.setOption("style", "rounded,filled");
		dotNode.setOption("fillcolor", "#70b356");
		dotNode.setOption("fontname", "Arial");
		dotNode.setOption("fontsize", "9");
		return dotNode;
	}

	private static DotNode makeUtilityNode(Dot dot, String name) {
		DotNode dotNode = dot.addNode(name);
		dotNode.setOption("shape", "circle");
		dotNode.setOption("style", "filled");
		dotNode.setOption("fillcolor", "#048ab5");
		dotNode.setOption("fontcolor", "white");
		dotNode.setOption("fontname", "Arial");
		dotNode.setOption("fontsize", "8");
		return dotNode;
	}

	private static DotNode makeGatewayNode(Dot dot, String name) {
		DotNode dotNode = dot.addNode(
				"<<table border='0'><tr><td></td></tr><tr><td valign='bottom'>" + name + "</td></tr></table>>");
		dotNode.setOption("shape", "diamond");
		dotNode.setOption("style", "filled");
		dotNode.setOption("fillcolor", "white");
		dotNode.setOption("fontcolor", "black");
		dotNode.setOption("fontname", "Arial");

		dotNode.setOption("width", "0.4");
		dotNode.setOption("height", "0.4");
		dotNode.setOption("fontsize", "30");
		dotNode.setOption("fixedsize", "true");

		return dotNode;
	}
}
