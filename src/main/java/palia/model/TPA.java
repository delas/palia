package palia.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.deckfour.xes.model.XEvent;

import lombok.Getter;

public class TPA {

	@Getter
	private UUID name;

	private Set<Node> nodes;

	private Set<Transition> transitions;

	public TPA() {
		this.name = UUID.randomUUID();
		this.nodes = new HashSet<>();
		this.transitions = new HashSet<>();
	}

	public Node createNode(String name) {
		return new Node(this, name);
	}

	public Node createNode(XEvent e) {
		return new Node(this, e);
	}

	public Set<Node> iterateNodes() {
		return nodes;
	}

	public Set<Transition> iterateTransitions() {
		return transitions;
	}

	public boolean hasNode(Node n) {
		return nodes.contains(n);
	}

	public boolean hasTransition(Transition n) {
		return transitions.contains(n);
	}

	public void removeNode(Node n) {
		nodes.remove(n);
	}

	public void removeTransition(Transition t) {
		transitions.remove(t);
		for (Node n : t.getSourceNodes()) {
			n.Output.remove(t);
		}
		for (Node n : t.getEndNodes()) {
			n.Input.remove(t);
		}
	}

	public Transition createTransition(Node... sources) {
		var res = new Transition(this).addSource(sources);
		registerTransition(res);
		return res;
	}

	public Transition createTransition(Collection<Node> sources) {
		var res = new Transition(this).addSource(sources);
		registerTransition(res);
		return res;
	}

	public void registerTransition(Transition... T) {
		for (Transition transition : T) {
			if (!transitions.contains(transition)) {
				transitions.add(transition);
				for (Node n : transition.getSourceNodes()) {
					n.Output.add(transition);
				}
				for (Node n : transition.getEndNodes()) {
					n.Input.add(transition);
				}
			}
		}
	}

	public void registerNode(Node... N) {
		for (Node node : N) {
			nodes.add(node);
		}
	}

	public Collection<Node> getStartingNodes() {
		return nodes.stream().filter(n -> n.isStartingNode()).toList();
	}

	public Collection<Node> getFinalNodes() {
		return nodes.stream().filter(n -> n.isFinalNode()).toList();
	}

	public Set<Transition> getExclusiveTransitionsfromSourceNodes(Collection<Node> n) {
		Set<Transition> res = new HashSet<>();
		for (Transition t : transitions) {
			if (areEqual(t.getSourceNodes(), n)) {
				res.add(t);
			}
		}
		return res;
	}

	private boolean areEqual(Collection<Node> set0, Collection<Node> set1) {
		return isNodeSubset(set0, set1) && isNodeSubset(set1, set0);
	}

	private boolean isNodeSubset(Collection<Node> set, Collection<Node> subset) {
		return set.containsAll(subset);
	}
}