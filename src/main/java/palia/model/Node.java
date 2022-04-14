package palia.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node {

	@Getter
	@EqualsAndHashCode.Include
	private UUID id;
	private TPA owner;
	@Getter
	private String name;
	@Getter
	private String idKey;
	@Getter @Setter
	private boolean isStartingNode;
	@Getter @Setter
	private boolean isFinalNode;
	
	public Node(TPA owner, XEvent e) {
		this(owner, XConceptExtension.instance().extractName(e));
	}
	
	public Node(TPA owner, String name) {
		this.id = UUID.randomUUID();
		this.owner = owner;
		this.isStartingNode = false;
		this.isFinalNode = false;
		this.name = name;
		
		this.owner.registerNode(this);
	}
	
	public Set<Transition> getOutTransitions() {
		return getOutTransitions(true);
	}
	
	public Set<Transition> getOutTransitions(boolean exclusive) {
		Set<Transition> toReturn = new HashSet<>();
		for (Transition t : owner.getTransitions()) {
			if (t.getSourceNodes().contains(this)) {
				if (exclusive) {
					if (t.getSourceNodes().size() == 1) {
						toReturn.add(t);
					}
				} else {
					toReturn.add(t);
				}
			}
		}
		return toReturn;
	}
	
	public Set<Transition> getInTransitions() {
		return getInTransitions(true);
	}
	
	public Set<Transition> getInTransitions(boolean exclusive) {
		Set<Transition> toReturn = new HashSet<>();
		for (Transition t : owner.getTransitions()) {
			if (t.getEndNodes().contains(this)) {
				if (exclusive) {
					if (t.getEndNodes().size() == 1) {
						toReturn.add(t);
					}
				} else {
					toReturn.add(t);
				}
			}
		}
		return toReturn;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
