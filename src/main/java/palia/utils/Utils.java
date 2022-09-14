package palia.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import palia.graphviz.exporter.GraphExporter;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

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

	public static Boolean AuditModel(TPA tpa) {
		for (var n : tpa.iterateNodes()) {
			if (!n.isStartingNode()) {
				var tx = n.getInTransitions();
				if (tx.size() == 0) {
					// AuditError(tpa);

					return false;
				}

			}
			if (!n.isFinalNode()) {
				var tx = n.getOutTransitions();
				if (tx.size() == 0) {
					// AuditError(tpa);
					return false;
				}
			}
		}

		return true;
	}

	public static void AuditError(TPA tpa) {
		ShowTPA(tpa, "AuditError");
	}

	public static void CheckAuditError(TPA tpa, String message) {
		if (!Utils.AuditModel(tpa)) {
			Utils.ShowTPA(tpa, message);
		}
	}

	public static Boolean ExistTransition(TPA tpa, String x0, String x1) {
		for (var t : tpa.iterateTransitions()) {
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

	static Integer index0 = 0;
	static boolean EnabledShowTPA = false;

	public static void ShowTPATrace(TPA tpa, String output) {

		index0++;
		var num = String.format("%05d", index0) + output;
		ShowTPA(tpa, num);
	}

	public static void ShowTPA(TPA tpa, String output) {

		if (EnabledShowTPA) {
			try {
				GraphExporter.exportSVG(tpa, new File("output/" + output + ".svg"));
			} catch (IOException e) { // TODO Auto-generated catch // block //
				e.printStackTrace();
			}
		}
	}

	public static Map<List<String>, Integer> extractVariants(XLog log) {
		Map<List<String>, Integer> res = new HashMap<>();

		for (XTrace trace : log) {
			List<String> t = trace.stream().map(e -> XConceptExtension.instance().extractName(e)).toList();
			int frequency = 1;
			if (res.containsKey(t)) {
				frequency += res.get(t);
			}
			res.put(t, frequency);

		}
		return res;
	}

	public static void removeifnotbreakingtransition(TPA tpa, Transition nt) {

		tpa.removeTransition(nt);
		if (!AuditModel(tpa)) {
			tpa.registerTransition(nt);
		}
	}

}
