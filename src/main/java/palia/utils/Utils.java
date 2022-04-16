package palia.utils;

import java.util.Arrays;
import java.util.List;

import palia.model.Node;

public class Utils {

	public static List<String> EquivalentExceptions = Arrays.asList("@Start", "@End");

	public static boolean IsEquivalent(Node n0, Node n1) {
		return IsEquivalent(n0, n1, false);
	}

	public static boolean IsEquivalent(Node n0, Node n1, boolean FullEquivalence) { // TODO: verify
		if (!n0.getName().equals(n1.getName())) return false;
		if (EquivalentExceptions.contains(n0.getName())) return true;
		if (!FullEquivalence) return true;
		//TODO: Implement IDKeys
		return n0.getId().equals(n1.getId());
	}
}
