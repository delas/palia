package palia.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;

import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class TraceFitPath {

	public TPA tpa;
	public List<Node> active = new ArrayList<>();
	public List<Node> waiting = new ArrayList<>();

	public TraceFitPath(TPA t) {
		tpa = t;
	}

	public static boolean TraceFit2(TPA tpa, XTrace trace) {

		List<String> t = trace.stream().map(e -> XConceptExtension.instance().extractName(e)).toList();

		List<TraceFitPath> hypothesis = null;

		for (var ev : t) {
			if (hypothesis == null) {

				var tf = new TraceFitPath(tpa);
				if (tf.Starting(ev)) {
					hypothesis = new ArrayList<TraceFitPath>();
					hypothesis.add(tf);

				} else {
					return false;
				}

			} else {

				for (var h : hypothesis.stream().toList()) {
					hypothesis.remove(h);
					var newhypo = h.Next(ev);
					hypothesis.addAll(newhypo);
				}
			}

			if (hypothesis.size() == 0)
				return false;
		}

		if (hypothesis != null && hypothesis.stream().anyMatch(h -> h.isFinal()))
			return true;
		return false;

	}

	public Collection<TraceFitPath> Next(String token) {

		List<TraceFitPath> res = new ArrayList<>();
		// waiting nodes
		var wn = waiting.stream().filter(n -> n.getName().equals(token)).findFirst();
		if (wn.isPresent()) {
			waiting.remove(wn.get());
			active.add(wn.get());
			res.add(this);
			return res;

		}

		for (var t : GetAllCombinedTransition(active)) {
			var matchnode = t.getEndNodes().stream().filter(n -> n.getName().equals(token)).findFirst();
			if (matchnode.isPresent()) {
				var hypo = Clone();
				if (t.isParallel()) {
					hypo.active.removeAll(t.getSourceNodes());
					hypo.waiting.addAll(t.getEndNodes());
					res.addAll(hypo.Next(token));
				} else {
					hypo.active.remove(t.getSourceNodes().stream().findFirst().get());
					hypo.active.add(t.getEndNodes().stream().findFirst().get());
					res.add(hypo);
				}

			}

		}
		return res;

	}

	public Collection<Transition> GetAllCombinedTransition(Collection<Node> active) {
		HashSet<Transition> res = new HashSet<>();
		for (var node : active) {
			for (var t : node.getOutTransitions(false).stream().toList()) {
				res.add(t);
			}
		}

		return res;
	}

	public boolean isFinal() {
		if (waiting.size() > 0)
			return false;
		if (active.stream().allMatch(a -> a.isFinalNode()))
			return true;
		return false;
	}

	public TraceFitPath Clone() {
		TraceFitPath res = new TraceFitPath(tpa);
		res.active.addAll(active);
		res.waiting.addAll(waiting);
		return res;
	}

	public boolean Starting(String token) {

		if (active == null) {
			active = new ArrayList<Node>();
			waiting = new ArrayList<Node>();
		}

		var sn = tpa.getStartingNodes().stream().filter(n -> n.getName().equals(token)).findFirst();

		if (sn.isPresent()) {
			active.add(sn.get());
			return true;
		} else {
			return false;
		}

	}

	public static void ReindexParallelTranstions(TPA tpa) {
		for (var t : tpa.iterateTransitions()) {
			for (var source : t.getSourceNodes()) {
				if (!source.getOutTransitions(false).contains(t)) {
					source.Output.add(t);
				}
			}
			for (var end : t.getEndNodes()) {
				if (!end.getInTransitions(false).contains(t)) {
					end.Input.add(t);
				}
			}
		}
	}

}
