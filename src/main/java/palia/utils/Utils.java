package palia.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import palia.graphviz.exporter.GraphExporter;
import palia.model.Node;
import palia.model.TPA;

public class Utils {

	public static List<String> EquivalentExceptions = Arrays.asList("@Start", "@End");

	public static boolean IsEquivalent(Node n0, Node n1) {
		return IsEquivalent(n0, n1, false);
	}

	public static boolean IsEquivalent(Node n0, Node n1, boolean FullEquivalence) { // TODO: verify
		if (!n0.getName().equals(n1.getName()))
			return false;
		if (EquivalentExceptions.contains(n0.getName()))
			return true;
		if (!FullEquivalence)
			return true;
		// TODO: Implement IDKeys
		return n0.getId().equals(n1.getId());
	}

	public static void AuditModel(TPA tpa) {
		for (var n : tpa.getNodes()) {
			if (!n.isStartingNode()) {
				var tx = n.getInTransitions();
				if (tx.size() == 0)
					AuditError(tpa);
			}
			if (!n.isFinalNode()) {
				var tx = n.getOutTransitions();
				if (tx.size() == 0)
					AuditError(tpa);
			}
		}
		if (ExistTransition(tpa, "X", "X2") || ExistTransition(tpa, "Y", "Y2")) {

			AuditError(tpa);
		}
	}

	public static void AuditError(TPA tpa) {
		ShowTPA(tpa, "AuditError");
	}

	public static Boolean ExistTransition(TPA tpa, String x0, String x1) {
		for (var t : tpa.getTransitions()) {
			var s0 = t.getSourceNodes();
			var s1 = t.getEndNodes();
			if (s0.size() == 1 && s1.size() == 1) {
				var n0 = s0.stream().findFirst().get();
				var n1 = s1.stream().findFirst().get();
				if (n0.getName().equals(x0) && n1.getName().equals(x1)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void ShowTPA(TPA tpa, String output) {

		try {
			GraphExporter.exportSVG(tpa, new File("output/" + output + ".svg"));
		} catch (IOException e) { // TODO Auto-generated catch
									// block
									// e.printStackTrace(); }
		}
	}
}
