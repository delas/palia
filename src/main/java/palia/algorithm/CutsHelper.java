package palia.algorithm;

import palia.model.Node;
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
}
