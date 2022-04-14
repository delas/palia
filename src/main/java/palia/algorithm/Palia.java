package palia.algorithm;

import java.util.List;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;
import palia.utils.Utils;

public class Palia {

	public TPA mine(XLog log) {
		TPA res = SimpleAcceptorTree(log);
		res = ConsecutiveMerge(res);
		
		return res;
	}
	
	private TPA SimpleAcceptorTree(XLog log) {
		TPA res = new TPA();
		for (XTrace t : log) {
			res = UpdateAcceptorTree(res, t);
		}
		return res;
	}

	private TPA UpdateAcceptorTree(TPA tpa, XTrace t) {
		if (t.size() > 0) {
			Node s0 = UpdateStartingNode(tpa, t);
			for (int i = 1; i < t.size(); i++) {
				XEvent e = t.get(i);
				s0 = UpdateNextNode(tpa, s0, e);
			}
			s0.setFinalNode(true);
		}
		return tpa;
	}

	private Node UpdateNextNode(TPA tpa, Node current, XEvent e) {
		Set<Transition> trans = tpa.getExclusiveTransitionsfromSourceNodes(List.of(current));
		for (Transition t : trans) {
			for (Node n : t.getEndNodes()) {
				if (n.getName().equals(XConceptExtension.instance().extractName(e))) {
					return n;
				}
			}
		}
		
		Node res = new Node(tpa, e);
		Transition nt = new Transition(tpa);
		nt.getSourceNodes().add(current);
		nt.getEndNodes().add(res);
		return res;
	}

	private Node UpdateStartingNode(TPA tpa, XTrace t) {
		XEvent e0 = t.get(0);
		for (Node n : tpa.getStartingNodes()) {
			if (n.getName().equals(XConceptExtension.instance().extractName(e0))) {
				return n;
			}
		}
		Node n = new Node(tpa, e0);
		n.setStartingNode(true);
		return n;
	}
	
	public TPA ConsecutiveMerge(TPA tpa) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for(Transition t : tpa.getTransitions()) {
				Node n0 = t.getSourceNodes().iterator().next();
				Node n1 = t.getEndNodes().iterator().next();
				if (!n0.getId().equals(n1.getId()) && Utils.IsEquivalent(n0, n1)) {
					FuseNodes(tpa, n0, n1);
					changed = true;
				}
			}
		}
		return tpa;
	}
	
	private void FuseNodes(TPA tpa, Node n0, Node n1) {
		for(Transition it : n1.getInTransitions()) {
			it.getEndNodes().remove(n1);
			it.getEndNodes().add(n0);
		}
		
		for (Transition ot : n1.getOutTransitions()) {
			ot.getSourceNodes().remove(n1);
			ot.getSourceNodes().add(n0);
		}
		
		tpa.getNodes().remove(n1);
	}
}
