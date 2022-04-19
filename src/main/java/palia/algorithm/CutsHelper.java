package palia.algorithm;

import java.util.Collection;
import java.util.HashSet;

import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class CutsHelper {

	public static boolean IsBackwarded(Node n0, Node n1) {
		for (Transition t : n0.getInTransitions()) {
			if (t.getSourceNodes().contains(n1)) {
				return true;
			}
		}
		return false;
	}

	public static boolean IsForwarded(Node n0, Node n1) {
		for (Transition t : n0.getOutTransitions()) {
			if (t.getEndNodes().contains(n1)) {
				return true;
			}
		}
		return false;
	}

	public static Collection<Node> GetForwardedNodesBetween2Nodes(TPA tpa, Node n0, Node n1) {
		Collection<Node> res = new HashSet<Node>();
		for (var t : GetForwardedTransitionsBetween2Nodes(tpa, n0, n1)) {
			res.add(t.getEndNodes().stream().findFirst().get());
		}
		return res;
	}

	public static Collection<Transition> GetForwardedTransitionsBetween2Nodes(TPA tpa, Node n0, Node n1) {
		Collection<Transition> res = new HashSet<Transition>();
		var transitions = new HashSet<Transition>();
		transitions.addAll(n0.getOutTransitions());
		while (transitions.size() > 0) {
			var t = transitions.stream().findFirst().get();
			if (!res.contains(t)) {
				res.add(t);
				var nf = t.getEndNodes().stream().findFirst().get();
				if (nf != n1) {
					transitions.addAll(nf.getOutTransitions());
				}

			}
			transitions.remove(t);
		}

		return res;
	}

	public static Collection<Node> GetBackwardedGroup(TPA t, Node n0, Collection<Node> Region) {
		Collection<Node> res = Region.stream().filter(n -> IsBackwarded(n0, n)).toList();// t(Collectors.toSet()));

		return res;
	}

	public static Collection<Node> GetForwardedGroup(TPA t, Node n0, Collection<Node> Region) {
		Collection<Node> res = Region.stream().filter(n -> IsForwarded(n0, n)).toList();

		return res;
	}

}
