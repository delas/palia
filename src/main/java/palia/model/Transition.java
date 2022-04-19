package palia.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transition {

	@Getter
	@EqualsAndHashCode.Include
	private UUID id;
	@Getter
	private Set<Node> sourceNodes;
	@Getter
	private Set<Node> endNodes;

	public Transition(TPA owner) {
		this.id = UUID.randomUUID();
		this.sourceNodes = new HashSet<>();
		this.endNodes = new HashSet<>();

		owner.registerTransition(this);
	}

	public boolean isParallel() {
		return sourceNodes.size() > 1 || endNodes.size() > 1;
	}

	public Transition addSource(Node... n) {
		for (Node ni : n) {
			sourceNodes.add(ni);
		}
		return this;
	}

	public Transition addEnd(Node... n) {
		for (Node ni : n) {
			endNodes.add(ni);
		}
		return this;
	}

	@Override
	public String toString() {
		String source = String.join(", ", getSourceNodes().stream().map(n -> n.getName()).sorted().toList());
		String target = String.join(", ", getEndNodes().stream().map(n -> n.getName()).sorted().toList());
		return "[" + source + "] => [" + target + "]";
	}
}
