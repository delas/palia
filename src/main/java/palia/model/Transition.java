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
	@Getter
	private int frequency;

	public Transition(TPA owner) {
		this.id = UUID.randomUUID();
		this.sourceNodes = new HashSet<>();
		this.endNodes = new HashSet<>();
		this.frequency = 0;

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

	public int incrementFrequency() {
		return incrementFrequency(1);
	}

	public int incrementFrequency(int amount) {
		frequency += amount;
		return frequency;
	}

	public int decrementFrequency() {
		return decrementFrequency(1);
	}

	public int decrementFrequency(int amount) {
		frequency -= amount;
		return frequency;
	}

	@Override
	public String toString() {
		String source = String.join(", ", getSourceNodes().stream().map(n -> n.getName()).sorted().toList());
		String target = String.join(", ", getEndNodes().stream().map(n -> n.getName()).sorted().toList());
		return "[" + source + "] => [" + target + "]";
	}
}
