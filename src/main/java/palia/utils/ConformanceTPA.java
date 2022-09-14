package palia.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;

import palia.model.Node;
import palia.model.State;
import palia.model.StateTransition;
import palia.model.TPA;
import palia.model.Transition;

public class ConformanceTPA {

	public static boolean TraceFit(TPA tpa, XTrace trace) {

		List<String> t = trace.stream().map(e -> XConceptExtension.instance().extractName(e)).toList();

		State actual = null;

		List<String> eventBag = new ArrayList<>();

		for (var ev : t) {
			if (actual == null) {
				for (var a : tpa.getInitialStates()) {

					if (a.IterateNodes().stream().findFirst().get().getName().equals(ev)) {
						actual = a;
					}

				}

			} else {
				eventBag.add(ev);
				int ebag = 0;
				do {
					ebag = eventBag.size();
					actual = NextState(actual, eventBag);
				} while (ebag != eventBag.size());
			}

			if (actual == null)
				return false;
		}

		if (actual != null && actual.IsFinal() && eventBag.size() == 0)
			return true;
		return false;
	}

	static State NextState(State actual, List<String> eventBag) {
		var post = actual.getOutTransitions();
		for (var t : post) {
			var tk = getTokensfromTransition(t.associatedTransition, eventBag);
			if (tk != null) {
				for (var s : tk) {
					eventBag.remove(s);
				}
				return t.end;
			}
		}
		return actual;
	}

	static Collection<String> getTokensfromTransition(Transition t, List<String> eventBag) {

		List<String> tokens = new ArrayList<>();

		for (var end : t.getEndNodes()) {
			var tk = getEquivalent(end, eventBag);
			if (tk == null)
				return null;
			tokens.add(tk);
		}

		return tokens;
	}

	static String getEquivalent(Node n0, List<String> eventbag) {
		for (var eb : eventbag) {
			if (IsEquivalent(n0, eb))
				return eb;
		}
		return null;
	}

	static boolean IsEquivalent(Node n0, String n) {
		return n0.getName().equals(n);
	}

	public static void CreateStates(TPA tpa) {
		tpa.ClearStates();

		ReindexParallelTranstions(tpa);

		List<State> newstates = new ArrayList<>();

		for (var n : tpa.getStartingNodes()) {
			var al = new HashSet<Node>();
			al.add(n);
			State actual = State.CreateFromNodes(al);
			tpa.AddState(actual);// add starting state
			newstates.add(actual);
		}

		// State actual = State.CreateFromNodes(tpa.getStartingNodes());

		while (newstates.size() > 0)// while new states are created iterate
		{
			State actual = newstates.get(0);
			newstates.remove(0); // pull the state

			Collection<Transition> nodetrans = getpossibleTransitionsFromState(actual);

			for (Transition NT : nodetrans) {
				var ns = getNextNodesFromNodeTransition(NT, actual);// siguiente estado

				State nState = FindStatebyNodes(tpa, ns);
				if (nState == null) {// si no existe el estado lo creamos y lo añadimos a los nuevos
					nState = State.CreateFromNodes(ns);
					tpa.AddState(nState);
					newstates.add(nState);
				}
				if (!ExistsTransition(tpa, actual, nState, NT)) {
					var st = new StateTransition();
					st.source = actual;
					st.end = nState;
					st.associatedTransition = NT;
					tpa.AddStateTransition(st);
					actual.Output.add(st);
					nState.Input.add(st);

				}

			}

		}
	}

	static boolean ExistsTransition(TPA tpa, State origin, State dest, Transition nt) {
		for (StateTransition t : tpa.IterateStateTransitions()) {
			if (t.source == origin && t.end == dest && t.associatedTransition == nt) {
				return true;
			}
		}
		return false;
	}

	public static Collection<Transition> getpossibleTransitionsFromState(State s) {

		// TODO Asegurarse que en el estado estan todos los nodos de la transicion
		Collection<Transition> res = new HashSet<>();

		for (var n : s.IterateNodes()) {
			for (var t : n.getOutTransitions(false)) {
				res.add(t);
			}
		}

		return res;
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

	public static Collection<Node> getNextNodesFromNodeTransition(Transition nt, State actual) {
		Collection<Node> res = new HashSet<Node>();
		res.addAll(actual.IterateNodes());
		for (var n : nt.getSourceNodes()) {
			res.remove(n);
		}
		for (var n : nt.getEndNodes()) {
			res.add(n);
		}

		return res;
	}

	public static State FindStatebyNodes(TPA tpa, Collection<Node> n) {
		for (State s : tpa.IterateStates()) {

			if (IsSubSet(n, s.IterateNodes()) && IsSubSet(s.IterateNodes(), n))
				return s;
		}
		return null;
	}

	static boolean IsSubSet(Collection<Node> SUP, Collection<Node> SUB) {
		for (var x : SUB) {
			if (!SUP.contains(x))
				return false;
		}
		return true;
	}

}
