package palia.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

public class State {

	@Getter
	@EqualsAndHashCode.Include
	private UUID id;

	private Set<Node> nodes;

	@Getter
	@Setter
	public Set<StateTransition> Input;
	@Getter
	@Setter
	public Set<StateTransition> Output;

	public State() {
		nodes = new HashSet<>();
		Input = new HashSet<>();
		Output = new HashSet<>();
		this.id = UUID.randomUUID();
	}

	public String getName() {
		List<String> names = new ArrayList<>();
		for (var s : nodes) {
			names.add(s.getName());
		}
		return String.join("_", names);
	}

	public boolean IsInitial() {
		for (var n : nodes) {
			if (n.getInTransitions(false).size() > 0)
				return false;
		}
		return true;
	}

	public boolean IsFinal() {
		for (var n : nodes) {
			if (n.getOutTransitions(false).size() > 0)
				return false;
		}
		return true;
	}

	public Collection<StateTransition> getInTransitions() {
		return Input;
	}

	public Collection<StateTransition> getOutTransitions() {
		return Output;
	}

	public void addNode(Node n) {
		nodes.add(n);
	}

	public Collection<Node> IterateNodes() {
		return nodes;
	}

	public static State CreateFromNodes(Collection<Node> nodes) {
		State res = new State();
		for (var n : nodes) {
			res.addNode(n);
		}
		return res;
	}
}
