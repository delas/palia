package palia.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;
import palia.utils.Utils;

public class ConcurrentNodesHelper {

	public static boolean AreParallelSplitMiner(TPA t, Collection<Node> region, Collection<Node> nodes) {
		Map<Node, Collection<Node>> equivalents = GetEquivalent(t, region);
		for (Node n : nodes) {
			if (!equivalents.containsKey(n)) {
				equivalents.put(n, equivalents.entrySet().stream().filter(e -> Utils.IsEquivalent(e.getKey(), n))
						.map(x -> x.getValue()).findFirst().orElse(new HashSet<>()));
			}
		}
		// Discover if Each pair (A,B) is Fully Accessible
		for (Entry<Node, Collection<Node>> n0 : equivalents.entrySet().stream().filter(e -> nodes.contains(e.getKey()))
				.toList()) {
			for (Entry<Node, Collection<Node>> n1 : equivalents.entrySet().stream()
					.filter(e -> nodes.contains(e.getKey())).toList()) {
				if ((!n0.getKey().equals(n1.getKey())) && (!n0.getValue().stream()
						.filter(n0i -> n1.getValue().stream().anyMatch(n1i -> IsAccessible(n0i, n1i, false))).findAny()
						.isPresent())) {
					// Exist A->B not accessible
					return false;
				}

				if (n0.getValue().stream()
						.filter(n0x -> n0.getValue().stream().anyMatch(n0y -> IsAccessible(n0x, n0y, false))).findAny()
						.isPresent()) {
					// There Is a Short Loop A>>A'
					return false;
				}
			}
		}
		return true;
	}

	public static Map<Node, Collection<Node>> GetEquivalent(TPA t, Collection<Node> nodes) {
		Map<Node, Collection<Node>> res = new HashMap<>();
		for (Node n : nodes) {
			Optional<Node> eq = res.keySet().stream().filter(k -> Utils.IsEquivalent(k, n)).findFirst();
			if (eq.isPresent()) {
				res.get(eq.get()).add(n);
			} else {
				res.put(n, new HashSet<>(Arrays.asList(n)));
			}
		}
		return res;
	}

	public static boolean IsAccessible(Node n0, Node n1, boolean backward) {
		return GetAccesibleNodes(n0, backward).contains(n1);
	}

	public static Collection<Node> GetAccesibleNodes(Node n0, boolean backward) {
		Set<Node> res = new HashSet<>();
		if (backward) {
			res = GetBackwardTransitions(n0).stream().map(nt -> nt.getEndNodes()).findFirst().orElse(new HashSet<>());
		} else {
			res = GetForwardTransitions(n0).stream().map(nt -> nt.getEndNodes()).findFirst().orElse(new HashSet<>());
		}
		return res;
	}

	public static Collection<Transition> GetBackwardTransitions(Node n0) {
		Set<Transition> res = new HashSet<>();
		Set<Transition> transitions = new HashSet<>();
		transitions.addAll(n0.getInTransitions());
		while (transitions.size() > 0) {
			Transition t = transitions.iterator().next();
			if (!res.contains(t)) {
				res.add(t);
				transitions.addAll(t.getSourceNodes().iterator().next().getInTransitions());
			}
			transitions.remove(t);
		}
		return res;
	}

	public static Collection<Transition> GetForwardTransitions(Node n0) {
		Set<Transition> res = new HashSet<>();
		Set<Transition> transitions = new HashSet<>();
		transitions.addAll(n0.getOutTransitions());
		while (transitions.size() > 0) {
			Transition t = transitions.iterator().next();
			if (!res.contains(t)) {
				res.add(t);
				transitions.addAll(t.getSourceNodes().iterator().next().getOutTransitions());
			}
			transitions.remove(t);
		}
		return res;
	}
}
