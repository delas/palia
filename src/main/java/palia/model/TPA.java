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
	@Getter
	private Set<Node> nodes;
	@Getter
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

	public Transition createTransition(Node... sources) {
		return new Transition(this).addSource(sources);
	}

	public void registerTransition(Transition transition) {
		transitions.add(transition);
	}

	public void registerNode(Node node) {
		nodes.add(node);
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