package palia.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

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
	private ParallelIdentificationMode ParallelIdentificationPolicy = ParallelIdentificationMode.SplitConcurrence;

	public TPA mine(XLog log) {
		TPA res = SimpleAcceptorTree(log);
		res = ConsecutiveMerge(res);

		RemoveRepeatedTransitions(res);
		FuseEndNodes(res);
		// TODO: MIlestones is for Interactive Palia
		// (Alert: This implementation said always J for milestone)
		// FuseMilestones(res);

		TransitionsMergeMode transmode = TransitionsMergeMode.Inline;

		int nodesnumber = Integer.MAX_VALUE;
		while (nodesnumber > res.getNodes().size()) {
			nodesnumber = res.getNodes().size();
			res = BackwardMerge(res, transmode);
			res = ForwardMerge(res, transmode);
		}

		res = ParallelForwardMerge(res);

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
			for (Transition t : tpa.getTransitions()) {
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
		for (Transition it : n1.getInTransitions()) {
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
			for (Node n0 : f) {
				Collection<Node> f0 = tpa.getFinalNodes();
				for (Node n1 : f0) {
					// As the collection is modified in fusing it is necessary to test if TPA
					// Contains n0
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
					// As the collection is modified in fusing it is necessary to test if TPA
					// Contains n0
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

						switch (mode) {
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

						switch (mode) {
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

	private TPA ParallelForwardMerge(TPA tpa) {
		// Set<Node> nodes = new HashSet<>(tpa.getNodes());
		Collection<Node> nodes = GetForwardOrderedNodes(tpa);
		for (Node n : nodes) {
			GetParallelHypothesis(tpa, n);
		}
		return tpa;
	}

	private Collection<Node> GetForwardOrderedNodes(TPA tpa) {// for having nodes always in forward order
		Collection<Node> res = new ArrayList<Node>();
		for (var s0 : tpa.getStartingNodes()) {
			res.add(s0);
			Collection<Node> all = GetParalleForwardAccesibleNodes(tpa, s0);
			for (var a : all) {
				if (!res.contains(a))
					res.add(a);
			}
		}
		return res;
	}

	private void GetParallelHypothesis(TPA tpa, Node n0) {
		Collection<Node> acc0 = GetParallelFollowingNodes(tpa, n0);
		Collection<Node> region = GetParalleForwardAccesibleNodes(tpa, n0);

		Collection<Node[]> groups = CreateParallelGroups(tpa, acc0, region);

		for (var g : groups) {
			if (g.length > 1) {
				var sync = GetForwardSyncronizingNode(tpa, n0, g);
				if (sync != null) {
					var mp = MoreParallels(tpa, sync.Region, g);// detec if there are more parallels on the region
					if (mp.size() == 0) {
//	                    ApplyParallel(tpa, n0, g, sync.Value.Region, sync.Value.Sync);
					} else {
//	                    ApplyParallel(tpa, n0, g.Union(mp).ToArray(), sync.Value.Region, sync.Value.Sync);
					}
				}
			}
		}
	}

	Collection<Node> MoreParallels(TPA tpa, Collection<Node> _region, Node[] parallels) {// TODO: Look for Parallels
		Collection<Node> res = new HashSet<Node>();
		/*
		 * Boolean changed = true; var region = UnionNodes(_region, parallels); while
		 * (changed) { Collection<Node> npars = new Collection<Node>(); var ppars =
		 * UnionNodes(npars,parallels); changed = false; for(Node node)
		 * 
		 * foreach (Node node in region.Where(r => !ppars.Contains(r) &&
		 * !ppars.Any(p=>PMLogHelper.IsEquivalent(r,p)))) { if (IsParallel(tpa, region,
		 * ppars.Union(new TPATemplate.Node[] {node}).ToArray())) { res.Add(node);
		 * break; } } }
		 */
		return res;
	}

	void ApplyParallel(TPA tpa, Node prev, Node[] parallels, Collection<Node> region, Node post) {

		/*
		 * var rests = SplitSequencesinsideParallel(tpa, region, parallels); if (rests
		 * != null) { TPATemplate.Node[][] Sequences = rests.Values.Select(v =>
		 * v.ToArray()).ToArray(); RemoveInterSplitTransitions(tpa, region, Sequences,
		 * parallels, post); FuseParallelEquivalentNodes(tpa, region, parallels);
		 * ForwardMerge(tpa, TransitionsMergeMode.Inline);
		 * RemoveRepeatedTransitions(tpa); RemoveParallelSelfloops(tpa, parallels);
		 * //Creating transitions tpa.AddNodeTransition(new Guid[] { prev.Id },
		 * parallels.Select(p => p.Id).ToArray(), "");
		 * prev.getOutTransitions(tpa).Where(nt => nt.EndNodes.Count == 1 &&
		 * parallels.Select(p => p.Id).Contains(nt.EndNodes.First())).ToList()
		 * .ForEach(nt => tpa.NodeTransitions.Remove(nt)); foreach (var fin in
		 * SequenceFinals(tpa, Sequences, post)) { tpa.AddNodeTransition(fin.Select(p =>
		 * p.Id).ToArray(), new Guid[] { post.Id }, ""); foreach (var pt in
		 * post.getInTransitions(tpa).Where(nt => nt.SourceNodes.Count == 1)) { if
		 * (fin.Select(f => f.Id).Contains(pt.SourceNodes.First())) {
		 * tpa.NodeTransitions.Remove(pt); } }
		 * 
		 * 
		 * }
		 * 
		 * 
		 * } else { //remove the region foreach (var n in region) {
		 * CutsHelper.DeleteNode(tpa, n); } foreach (var n in parallels) {
		 * CutsHelper.DeleteNode(tpa, n); } //recreate the region
		 * tpa.Nodes.AddRange(parallels); tpa.NodeTransitions.Add(new
		 * TPATemplate.NodeTransition() { SourceNodes = new List<Guid>() { prev.Id },
		 * EndNodes = parallels.Select(p => p.Id).ToList(), Expression = "" });
		 * tpa.NodeTransitions.Add(new TPATemplate.NodeTransition() { EndNodes = new
		 * List<Guid>() { post.Id }, SourceNodes = parallels.Select(p => p.Id).ToList(),
		 * Expression = "" }); }
		 */

	}

	class SyncNode {
		public Node Sync;
		public Collection<Node> Region;

		public SyncNode(Node s, Collection<Node> reg) {
			Sync = s;
			Region = reg;
		}

	}

	public SyncNode GetForwardSyncronizingNode(TPA tpa, Node prev, Node[] hypo) {

		Collection<Node> region = new HashSet<Node>();
		for (var h : hypo) {
			region.add(h);
		}
		int level = 0;
		do {
			level++;
			Collection<Collection<Node>> h = new HashSet<Collection<Node>>();
			for (var h0 : hypo) {
				h.add(GetParallelFollowingNodes(tpa, h0, level));
			}
			var sy = GetSyncrofromFollowingNodes(tpa, prev, hypo, h);
			if (sy != null) {
				return sy;
			}

		} while (level < 50);// TODO: ElegantWay

		return null;

	}

	private Collection<Node> GetParallelFollowingNodes(TPA tpa, Node n0, int level) {
		Collection<Node> res = new HashSet<Node>(GetParallelFollowingNodes(tpa, n0));
		if (level == 1)
			return res;
		for (var v : res.stream().toList()) {
			for (var nx : GetParallelFollowingNodes(tpa, v, level - 1)) {
				res.add(nx);
			}
		}
		return res;
	}

	private SyncNode GetSyncrofromFollowingNodes(TPA tpa, Node prev, Node[] hypo, Collection<Collection<Node>> n) {

		Collection<Node> res = null;

		for (var h : n) {
			Collection<Node> SX = new HashSet<Node>();
			for (var x : h) {
				Boolean all = true;
				for (var y : hypo) {
					if (Utils.IsEquivalent(x, y)) {
						all = false;
					}
				}
				if (all)
					SX.add(x);
			}
			if (res == null) {
				res = new HashSet<Node>();
				res.addAll(SX);
			} else {

				Collection<Node> SY = new HashSet<Node>();
				for (var x : res) {
					Boolean any = false;
					for (var y : SX) {
						if (x == y) {
							any = true;
						}
					}
					if (any) {
						SY.add(x);
					}

				}
				res = SY;
			}

		}

		for (var r : res) {
			var region = GetIsolatedParallelRegion(tpa, prev, hypo, r);
			if (region != null) {
				return new SyncNode(r, region);
			}
		}

		return null;

	}

	Collection<Node> GetIsolatedParallelRegion(TPA tpa, Node prev, Node[] parallels, Node post) {
		Collection<Node> region = new HashSet<Node>();
		for (var node : parallels) {
			region = UnionNodes(region, CutsHelper.GetForwardedNodesBetween2Nodes(tpa, node, post));
		}
		var middleregion = region.stream().filter(x -> x.getId() != post.getId()).collect(Collectors.toSet());
		var prevregion = CutsHelper.GetBackwardedGroup(tpa, prev, tpa.getNodes());
		var postregion = CutsHelper.GetForwardedGroup(tpa, post, tpa.getNodes());
		var iprev = IntersectNodes(middleregion, prevregion);
		var ipost = IntersectNodes(middleregion, postregion);
		if (iprev.size() == 0 && ipost.size() == 0) {
			return middleregion;
		}
		return null;

	}

	/*
	 * TPATemplate.Node[] GetIsolatedParallelRegion(TPATemplate tpa,
	 * TPATemplate.Node prev, TPATemplate.Node[] parallels, TPATemplate.Node post) {
	 * //Compute region in the middle List<TPATemplate.Node> region = new
	 * List<TPATemplate.Node>(); foreach (TPATemplate.Node node in parallels) {
	 * region = region.Union(CutsHelper.GetForwardedNodesBetween2Nodes(tpa, node,
	 * post)).ToList(); } var middleregion = region.Except(new TPATemplate.Node[] {
	 * post }).ToList();
	 * 
	 * var prevregion = CutsHelper.GetBackwardedGroup(tpa, prev,
	 * tpa.Nodes.ToArray()); var postregion = CutsHelper.GetForwardedGroup(tpa,
	 * post, tpa.Nodes.ToArray()); var iprev =
	 * middleregion.Intersect(prevregion).ToList(); var ipost =
	 * middleregion.Intersect(postregion).ToList(); if (iprev.Count == 0 &&
	 * ipost.Count == 0) { return middleregion.ToArray(); } return null; }
	 * 
	 */

	private Collection<Node[]> CreateParallelGroups(TPA tpa, Collection<Node> nodes, Collection<Node> region) {
		Collection<Node[]> res = new HashSet<Node[]>();
		for (var n0 : nodes) {
			res.add(new Node[] { n0 });
		}
		Boolean changed = true;
		while (changed) {
			changed = false;
			int i0 = res.size();
			res = GroupFusion(tpa, region, res);
			if (i0 != res.size()) {
				changed = true;
			}
		}

		return res;
	}

	private Collection<Node[]> GroupFusion(TPA tpa, Collection<Node> region, Collection<Node[]> groups) {

		Collection<Node[]> res = new HashSet<Node[]>(groups);
		for (var i0 : groups) {
			for (var i1 : groups) {
				if (i0 != i1 && res.contains(i0) && res.contains(i1)) {
					var ix = UnionNodes(i1, i0);
					if (IsParallel(tpa, region, ix)) {
						res.remove(i0);
						res.remove(i1);
						res.add(ix.toArray(new Node[ix.size()]));
					}

				}
			}
		}

		return res;

	}

	private Collection<Node> UnionNodes(Node[] n0, Node[] n1) {
		Collection<Node> res = new HashSet<Node>();
		for (var n : n0) {
			res.add(n);
		}
		for (var n : n1) {
			res.add(n);
		}
		return res;
	}

	private Collection<Node> UnionNodes(Collection<Node> n0, Collection<Node> n1) {
		Collection<Node> res = new HashSet<Node>();
		for (var n : n0) {
			res.add(n);
		}
		for (var n : n1) {
			res.add(n);
		}
		return res;
	}

	private Collection<Node> IntersectNodes(Collection<Node> n0, Collection<Node> n1) {
		Collection<Node> res = new HashSet<Node>();
		for (var n : n0) {
			if (n1.contains(n)) {
				res.add(n);
			}
		}
		return res;
	}

	private boolean IsParallel(TPA tpa, Collection<Node> region, Collection<Node> g) {
		switch (ParallelIdentificationPolicy) {
		case Inductive:
			return InductiveMinerParallelMethod(tpa, region, g);
		default:
			return SplitMinerIsParallelMethod(tpa, region, g);
		}
	}

	private boolean InductiveMinerParallelMethod(TPA t, Collection<Node> region, Collection<Node> g) {
		for (Node n0 : g) {
			for (Node n1 : g) {
				if (n1 == n0) {
					continue;
				}

				Collection<Node> eq0 = GetEquivalentNodes(t, n0, region);
				Collection<Node> eq1 = GetEquivalentNodes(t, n0, region);

				boolean back = eq0.stream().anyMatch(x -> eq1.stream().anyMatch(y -> CutsHelper.IsBackwarded(x, y)));
				boolean fow = eq0.stream().anyMatch(x -> eq1.stream().anyMatch(y -> CutsHelper.IsBackwarded(x, y)));

				if (!back || !fow) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean SplitMinerIsParallelMethod(TPA tpa, Collection<Node> region, Collection<Node> g) {
		return ConcurrentNodesHelper.AreParallelSplitMiner(tpa, region, g);
	}

	private Collection<Node> GetParalleForwardAccesibleNodes(TPA tpa, Node n0) {
		Set<Node> res = new HashSet<>();
		Queue<Node> ProcessNodes = new LinkedList<>();
		ProcessNodes.offer(n0);
		while (ProcessNodes.size() > 0) {
			Node n = ProcessNodes.poll();
			res.add(n);
			for (Node nx : GetParallelFollowingNodes(tpa, n)) {
				if (!res.contains(nx) && !ProcessNodes.contains(nx)) {
					ProcessNodes.offer(nx);
				}
			}
		}

		return res;
	}

	private Collection<Node> GetParallelFollowingNodes(TPA tpa, Node n0) {
		Set<Node> res = new HashSet<>();
		for (Transition nt : n0.getOutTransitions()) {
			res.addAll(nt.getEndNodes());
		}
		return res;
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

	private Collection<Transition> GetNodeTransitionsbyEndNodes(TPA tpa, Collection<Node> n0, boolean allowParallel) { // TODO:
																														// verify
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

	private Collection<Transition> GetNodeTransitionsbyStartingNodes(TPA tpa, Collection<Node> n0,
			boolean allowParallel) { // TODO: verify
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
