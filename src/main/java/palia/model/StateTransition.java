package palia.model;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;

public class StateTransition {

	@Getter
	@EqualsAndHashCode.Include
	private UUID id;

	public State source;
	public State end;
	public Transition associatedTransition;

	public StateTransition() {

		this.id = UUID.randomUUID();
	}

	public String getName() {
		String res = "[";
		if (source != null) {
			res = source.getName();
		}
		res = res + "]->[";
		if (end != null) {
			res = end.getName();
		}

		return res + "]";
	}
}
