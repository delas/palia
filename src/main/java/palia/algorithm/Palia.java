package palia.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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

	public static List<String> MILESTONES = Arrays.asList("J");
	
	public TPA mine(XLog log) {
		TPA res = SimpleAcceptorTree(log);
		res = ConsecutiveMerge(res);
		
		RemoveRepeatedTransitions(res);
		FuseEndNodes(res);
		//TODO: MIlestones is for Interactive Palia 
		//(Alert: This implementation said always J for milestone)
		//FuseMilestones(res); 
		
		TransitionsMergeMode transmode = TransitionsMergeMode.Equivalent;
		
		int nodesnumber = Integer.MAX_VALUE;
		while (nodesnumber > res.getNodes().size()) {
			nodesnumber = res.getNodes().size();
			res = BackwardMerge(res, transmode);
			res = ForwardMerge(res, transmode);
		}
		
//		res = ParallelForwardMerge(res);
//		
//		if (transmode == TransitionsMergeMode.Equivalent) {
//			while (nodesnumber > res.getNodes().size()) {
//				nodesnumber = res.getNodes().size();
//				res = BackwardMerge(res, MergingPolicy);
//				res = ForwardMerge(res, MergingPolicy);
//			}
//		}
		
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
	
	private void FuseEndNodes(TPA tpa) {
		boolean changed = true;
		while (changed) {
			Collection<Node> f = tpa.getFinalNodes();
			changed = false;
			for(Node n0 : f) {
				Collection<Node> f0 = tpa.getFinalNodes();
				for(Node n1 : f0) {
					//As the collection is modified in fusing it is necessary to test if TPA Contains n0
					if (n0 != n1 && f0.contains(n0) && Utils.IsEquivalent(n0, n1)) {
						FuseNodes(tpa, n0, n1);
						changed = true;
					}
				}
			}
		}
	}
	
	private void FuseMilestones(TPA tpa) {
		boolean changed = true;
		while (changed) {
			Collection<Node> f = GetMilestones(tpa);
			changed = false;
			for (Node n0 : f) {
				Collection<Node> f0 = GetMilestones(tpa);
				for (Node n1 : f0) {
					//As the collection is modified in fusing it is necessary to test if TPA Contains n0
					if (n0 != n1 && f0.contains(n0) && Utils.IsEquivalent(n0, n1)) {
						FuseNodes(tpa, n0, n1);
						changed = true;
					}
				}
			}
		}
	}
	
	private Collection<Node> GetMilestones(TPA tpa) {
		return tpa.getNodes().stream().filter(n -> MILESTONES.contains(n.getName())).toList(); // TODO: verify
	}
	
	private void RemoveRepeatedTransitions(TPA tpa) {
		RemoveRepeatedTransitions(tpa, null);
	}
	
	private void RemoveRepeatedTransitions(TPA tpa, Collection<Node> region) {
		if (region == null) {
			region = tpa.getNodes();
		}
		
		for (Node n0 : region) {
			Set<Node> nodes = new HashSet<>();
			for (Transition ot : n0.getOutTransitions()) {
				Node endNode = ot.getEndNodes().iterator().next();
				if (!nodes.contains(endNode)) {
					nodes.add(endNode);
				} else {
					tpa.getTransitions().remove(ot);
				}
			}
			nodes.clear();
			for (Transition it : n0.getInTransitions()) {
				Node sourceNode = it.getSourceNodes().iterator().next();
				if (!nodes.contains(sourceNode)) {
					nodes.add(sourceNode);
				} else {
					tpa.getTransitions().remove(it);
				}
			}
		}
	}
	
	private TPA ForwardMerge(TPA tpa, TransitionsMergeMode mode) {
		boolean changed = true;
		while (changed) {
			changed = false;
			Set<Node> dincol = new HashSet<>(tpa.getNodes());
			for (Node n : dincol) {
				Collection<Node> nodes = new HashSet<>();
				nodes.add(n);
				switch (mode) {
				case Extrict:
					break;
				case Equivalent:
					nodes = GetEquivalentNodes(tpa, n);
					break;
				case Inline:
					nodes = GetEquivalentNodes(tpa, n);
					break;
				case None:
					break;
				}
				Collection<Transition> transitions = GetNodeTransitionsbyStartingNodes(tpa, nodes);
				for (Transition nt0 : transitions) {
					Collection<Transition> subtrans = null;
					if (mode != TransitionsMergeMode.Inline) {
						subtrans = transitions.stream().filter(nt -> nt != nt0).toList();
					} else {
						subtrans = GetAccesibleTransitions(tpa, nt0, true);
					}
					for (Transition nt1 : subtrans) {
						Node n0 = nt0.getSourceNodes().iterator().next();
						Node n1 = nt1.getSourceNodes().iterator().next();
						Node f0 = nt0.getEndNodes().iterator().next();
						Node f1 = nt1.getEndNodes().iterator().next();
						
						switch(mode) {
						case Extrict:
							if (f0 != f1 && n0.getId().equals(n1.getId()) && Utils.IsEquivalent(f0, f1)) {
								FuseNodes(tpa, f0, f1);
								tpa.getTransitions().remove(nt1);
								changed = true;
							}
							break;
						case Inline:
						case Equivalent:
							if (f0 != f1 && Utils.IsEquivalent(f0, f1) && Utils.IsEquivalent(n0, n1)) {
								FuseNodes(tpa, f0, f1);
								if (n0 != n1) {
									FuseNodes(tpa, n0, n1);
								}
								tpa.getTransitions().remove(nt1);
								changed = true;
							}
							break;
						case None:
							break;
						}
					}
				}
				RemoveRepeatedTransitions(tpa);
			}
		}
		return tpa;
	}
	
	
	private TPA BackwardMerge(TPA tpa, TransitionsMergeMode mode) {
		boolean changed = true;
		while (changed) {
			changed = false;
			Set<Node> dincol = new HashSet<>(tpa.getNodes());
			for (Node n : dincol) {
				Collection<Node> nodes = new HashSet<>();
				nodes.add(n);
				switch (mode) {
				case Extrict:
					break;
				case Equivalent:
					nodes = GetEquivalentNodes(tpa, n);
					break;
				case Inline:
					nodes = GetEquivalentNodes(tpa, n);
					break;
				case None:
					break;
				}
				Collection<Transition> transitions = GetNodeTransitionsbyEndNodes(tpa, nodes);
				for (Transition nt0 : transitions) {
					Collection<Transition> subtrans = null;
					if (mode != TransitionsMergeMode.Inline) {
						subtrans = transitions.stream().filter(nt -> nt != nt0).toList();
					} else {
						subtrans = GetAccesibleTransitions(tpa, nt0, true);
					}
					for (Transition nt1 : subtrans) {
						Node n0 = nt0.getSourceNodes().iterator().next();
						Node n1 = nt1.getSourceNodes().iterator().next();
						Node f0 = nt0.getEndNodes().iterator().next();
						Node f1 = nt1.getEndNodes().iterator().next();
						
						switch(mode) {
						case Extrict:
							if (n0 != n1 && f0.getId().equals(f1.getId()) && Utils.IsEquivalent(n0, n1)) {
								FuseNodes(tpa, n0, n1);
								tpa.getTransitions().remove(nt1);
								changed = true;
							}
							break;
						case Inline:
						case Equivalent:
							if (n0 != n1 && Utils.IsEquivalent(f0, f1) && Utils.IsEquivalent(n0, n1)) {
								FuseNodes(tpa, n0, n1);
								if (f0 != f1) {
									FuseNodes(tpa, f0, f1);
								}
								tpa.getTransitions().remove(nt1);
								changed = true;
							}
							break;
						case None:
							break;
						}
					}
				}
				RemoveRepeatedTransitions(tpa);
			}
		}
		return tpa;
	}
	
	private Collection<Transition> GetAccesibleTransitions(TPA tpa, Transition nt0, boolean backward) {
		Set<Transition> res = new HashSet<>();
		if (backward) {
			res.addAll(GetBackwardTransitions(tpa, nt0.getEndNodes().iterator().next()));
		} else {
			res.addAll(GetForwardTransitions(tpa, nt0.getSourceNodes().iterator().next()));
		}
		return res;
	}
	
	private Collection<Transition> GetForwardTransitions(TPA tpa, Node n0) {
		Set<Transition> res = new HashSet<>();
		Set<Transition> transitions = new HashSet<>();
		transitions.addAll(n0.getOutTransitions());
		while (transitions.size() > 0) {
			Transition t = transitions.iterator().next();
			if (!res.contains(t)) {
				res.add(t);
				Node nf = t.getEndNodes().iterator().next();
				transitions.addAll(nf.getOutTransitions());
			}
			transitions.remove(t);
		}
		
		return res;
	}

	private Collection<Transition> GetBackwardTransitions(TPA tpa, Node n0) {
		Set<Transition> res = new HashSet<>();
		Set<Transition> transitions = new HashSet<>();
		transitions.addAll(n0.getInTransitions());
		while (transitions.size() > 0) {
			Transition t = transitions.iterator().next();
			if (!res.contains(t)) {
				res.add(t);
				Node nf = t.getSourceNodes().iterator().next();
				transitions.addAll(nf.getInTransitions());
			}
			transitions.remove(t);
		}
		
		return res;
	}

	private Collection<Transition> GetNodeTransitionsbyEndNodes(TPA tpa, Collection<Node> n0) {
		return GetNodeTransitionsbyEndNodes(tpa, n0, false);
	}
	
	private Collection<Transition> GetNodeTransitionsbyEndNodes(TPA tpa, Collection<Node> n0, boolean allowParallel) { // TODO: verify
		Set<Transition> t = new HashSet<>();
		for (Node n : n0) {
			for (Transition nt : n.getInTransitions()) {
				if (allowParallel || !nt.isParallel()) {
					t.add(nt);
				}
			}
		}
		return t;
	}

	private Collection<Transition> GetNodeTransitionsbyStartingNodes(TPA tpa, Collection<Node> n0) {
		return GetNodeTransitionsbyStartingNodes(tpa, n0, false);
	}
	
	private Collection<Transition> GetNodeTransitionsbyStartingNodes(TPA tpa, Collection<Node> n0, boolean allowParallel) { // TODO: verify
		Set<Transition> t = new HashSet<>();
		for (Node n : n0) {
			for (Transition nt : n.getOutTransitions()) {
				if (allowParallel || !nt.isParallel()) {
					t.add(nt);
				}
			}
		}
		return t;
	}
	
	private Collection<Node> GetEquivalentNodes(TPA tpa, Node n0) {
		return GetEquivalentNodes(tpa, n0, null);
	}
	
	private Collection<Node> GetEquivalentNodes(TPA tpa, Node n0, Collection<Node> region) {
		if (region == null) {
			region = tpa.getNodes();
		}
		return region.stream().filter(n -> Utils.IsEquivalent(n0, n)).toList();
	}
}
