package palia.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XLog;

import palia.algorithm.callback.CLIPaliaMinerStatusUpdater;
import palia.algorithm.callback.PaliaMinerStatus;
import palia.algorithm.callback.PaliaMinerStatusUpdater;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;
import palia.utils.Utils;

public class Palia {

	public static List<String> MILESTONES = Arrays.asList("MM");
	private ParallelIdentificationMode ParallelIdentificationPolicy = ParallelIdentificationMode.SplitConcurrence;

	public TPA mine(XLog log) {
		return mine(log, new CLIPaliaMinerStatusUpdater());
	}

	public TPA mine(XLog log, PaliaMinerStatusUpdater updater) {
		updater.update(PaliaMinerStatus.EXTRACT_LOG_VARIANTS, true);
		Map<List<String>, Integer> logToProcess = Utils.extractVariants(log);
		updater.update(PaliaMinerStatus.EXTRACT_LOG_VARIANTS, false);

		updater.update(PaliaMinerStatus.SIMPLE_ACCEPTOR_TREE, true);
		TPA res = SimpleAcceptorTree(logToProcess.keySet(), 0);
		updater.update(PaliaMinerStatus.SIMPLE_ACCEPTOR_TREE, false);

		updater.update(PaliaMinerStatus.CONSECUTIVE_MERGE, true);
		res = ConsecutiveMerge(res);
		updater.update(PaliaMinerStatus.CONSECUTIVE_MERGE, false);

		updater.update(PaliaMinerStatus.REMOVE_REPEATED_TRANSITIONS, true);
		RemoveRepeatedTransitions(res);
		updater.update(PaliaMinerStatus.REMOVE_REPEATED_TRANSITIONS, false);

		updater.update(PaliaMinerStatus.FUSE_END_NODES, true);
		FuseEndNodes(res);
		updater.update(PaliaMinerStatus.FUSE_END_NODES, false);

		// TODO: MIlestones is for Interactive Palia
		// (Alert: This implementation said always J for milestone)
		updater.update(PaliaMinerStatus.FUSE_MILESTONES, true);
		FuseMilestones(res);
		updater.update(PaliaMinerStatus.FUSE_MILESTONES, false);

		updater.update(PaliaMinerStatus.MINE_ONWARD_MERGE_1, true);
		MineOnwardMerge(res, TransitionsMergeMode.Inline);
		updater.update(PaliaMinerStatus.MINE_ONWARD_MERGE_1, false);
		// ShowTPA(res);

		updater.update(PaliaMinerStatus.PARALLEL_FORWARD_MERGE, true);
		res = ParallelForwardMerge(res);
		updater.update(PaliaMinerStatus.PARALLEL_FORWARD_MERGE, false);

		updater.update(PaliaMinerStatus.MINE_ONWARD_MERGE_2, true);
		res = MineOnwardMerge(res, TransitionsMergeMode.Equivalent);
		updater.update(PaliaMinerStatus.MINE_ONWARD_MERGE_2, false);

		updater.update(PaliaMinerStatus.REMOVE_REPEATED_TRANSITIONS, true);
		RemoveRepeatedTransitions(res);
		updater.update(PaliaMinerStatus.REMOVE_REPEATED_TRANSITIONS, false);

//		if (transmode == TransitionsMergeMode.Equivalent) {
//			while (nodesnumber > res.getNodes().size()) {
//				nodesnumber = res.getNodes().size();
//				res = BackwardMerge(res, MergingPolicy);
//				res = ForwardMerge(res, MergingPolicy);
//			}
//		}

		return res;
	}

	private TPA MineOnwardMerge(TPA tpa, TransitionsMergeMode mode) {
		int nodesnumber = Integer.MAX_VALUE;
		while (nodesnumber > tpa.iterateNodes().size()) {
			nodesnumber = tpa.iterateNodes().size();
			tpa = BackwardMerge(tpa, TransitionsMergeMode.Extrict);
			tpa = ForwardMerge(tpa, TransitionsMergeMode.Extrict);
			tpa = BackwardMerge(tpa, mode);
			tpa = ForwardMerge(tpa, mode);
		}
		return tpa;
	}

	private TPA SimpleAcceptorTree(Set<List<String>> log, int offset) {
		TPA res = new TPA();
		int index = 0;
		for (List<String> t : log) {
			res = UpdateAcceptorTree(res, t);
			if (offset > 0 && index > offset)
				return res;
			index++;
		}
		return res;
	}

	private TPA UpdateAcceptorTree(TPA tpa, List<String> t) {
		if (t.size() > 0) {
			Node s0 = UpdateStartingNode(tpa, t);
			for (int i = 1; i < t.size(); i++) {
				String e = t.get(i);
				s0 = UpdateNextNode(tpa, s0, e);
			}
			s0.setFinalNode(true);
		}
		return tpa;
	}

	private Node UpdateNextNode(TPA tpa, Node current, String e) {
		Set<Transition> trans = tpa.getExclusiveTransitionsfromSourceNodes(List.of(current));
		for (Transition t : trans) {
			for (Node n : t.getEndNodes()) {
				if (n.getName().equals(e)) {
					return n;
				}
			}
		}

		Node res = new Node(tpa, e);
		Transition nt = new Transition(tpa);
		nt.addSource(current);
		nt.addEnd(res);
		return res;
	}

	private Node UpdateStartingNode(TPA tpa, List<String> t) {
		String e0 = t.get(0);
		for (Node n : tpa.getStartingNodes()) {
			if (n.getName().equals(e0)) {
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
			for (Transition t : tpa.iterateTransitions()) {
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
			n0.Input.add(it);
		}

		for (Transition ot : n1.getOutTransitions()) {
			ot.getSourceNodes().remove(n1);
			ot.getSourceNodes().add(n0);
			n0.Output.add(ot);
		}

		tpa.removeNode(n1);
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
		return tpa.iterateNodes().stream().filter(n -> MILESTONES.contains(n.getName())).toList(); // TODO: verify
	}

	private void RemoveRepeatedTransitions(TPA tpa) {
		RemoveRepeatedTransitions(tpa, null);
	}

	private void RemoveRepeatedTransitions(TPA tpa, Collection<Node> region) {
		if (region == null) {
			region = tpa.iterateNodes();
		}

		for (Node n0 : region) {
			Set<Node> nodes = new HashSet<>();
			for (Transition ot : n0.getOutTransitions()) {
				Node endNode = ot.getEndNodes().iterator().next();
				if (!nodes.contains(endNode)) {
					nodes.add(endNode);
				} else {
					tpa.removeTransition(ot);
				}
			}
			nodes.clear();
			for (Transition it : n0.getInTransitions()) {
				Node sourceNode = it.getSourceNodes().iterator().next();
				if (!nodes.contains(sourceNode)) {
					nodes.add(sourceNode);
				} else {
					tpa.removeTransition(it);
				}
			}
		}
	}

	private TPA ForwardMerge(TPA tpa, TransitionsMergeMode mode) {
		boolean changed = true;
		while (changed) {
			changed = false;
			Set<Node> dincol = new HashSet<>(tpa.iterateNodes());
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
						if (nt0 != nt1) {
							Node n0 = nt0.getSourceNodes().iterator().next();
							Node n1 = nt1.getSourceNodes().iterator().next();
							Node f0 = nt0.getEndNodes().iterator().next();
							Node f1 = nt1.getEndNodes().iterator().next();

							switch (mode) {
							case Extrict:
								if (f0 != f1 && n0.getId().equals(n1.getId()) && Utils.IsEquivalent(f0, f1)) {
									FuseNodes(tpa, f0, f1);
									tpa.removeTransition(nt1);
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
									tpa.removeTransition(nt1);
									changed = true;
								}
								break;
							case None:
								break;
							}
						}
					}
				}
				RemoveRepeatedTransitions(tpa, nodes);
			}
		}
		return tpa;
	}

	private TPA BackwardMerge(TPA tpa, TransitionsMergeMode mode) {
		boolean changed = true;
		while (changed) {
			changed = false;
			Set<Node> dincol = new HashSet<>(tpa.iterateNodes());
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
								tpa.removeTransition(nt1);
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
								tpa.removeTransition(nt0);
								changed = true;
							}
							break;
						case None:
							break;
						}
					}
				}
				RemoveRepeatedTransitions(tpa, nodes);
			}
		}
		return tpa;
	}

	private TPA ParallelForwardMerge(TPA tpa) {
		// Set<Node> nodes = new HashSet<>(tpa.getNodes());
		Collection<Node> nodes = GetForwardOrderedNodes(tpa);
		for (Node n : nodes) {
			if (tpa.hasNode(n)) {
				// As TPA is merging the nodes could be removed in iterations
				GetParallelHypothesis(tpa, n);
			}
		}
		return tpa;
	}

	private Collection<Node> GetForwardOrderedNodes(TPA tpa) {// for having nodes always in forward order
		Collection<Node> res = new ArrayList<Node>();
		for (var s0 : tpa.getStartingNodes()) {
			res.add(s0);
			// Collection<Node> all = GetParalleForwardAccesibleNodes(tpa, s0);
			Collection<Node> all = ConcurrentNodesHelper.GetForwardNodes(s0);
			for (var a : all) {
				if (!res.contains(a))
					res.add(a);
			}
		}
		return res;
	}

	public Collection<Node> GetForwardOrderedNodes(TPA tpa, Collection<Node> region) {
		Collection<Node> res = new ArrayList<Node>();
		for (var n : GetForwardOrderedNodes(tpa)) {
			if (region.contains(n)) {
				res.add(n);
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
						ApplyParallel(tpa, n0, Arrays.asList(g), sync.Region, sync.Sync);

					} else {
						ApplyParallel(tpa, n0, UnionNodes(Arrays.asList(g), mp), sync.Region, sync.Sync);

					}
				}
			}
		}
	}

	Collection<Node> MoreParallels(TPA tpa, Collection<Node> _region, Node[] aparallels) {// TODO: Look for Parallels
		Collection<Node> parallels = Arrays.asList(aparallels);
		Collection<Node> res = new HashSet<Node>();
		Boolean changed = true;
		var region = UnionNodes(_region, parallels);
		while (changed) {
			Collection<Node> npars = new HashSet<Node>();
			var ppars = UnionNodes(npars, parallels);
			changed = false;
			// region.Where(r => !ppars.Contains(r) &&
			// !ppars.Any(p=>PMLogHelper.IsEquivalent(r,p)))
			Collection<Node> nreg = new HashSet<Node>();
			for (Node node : region) {
				var ng = new HashSet<Node>(ppars);
				ng.add(node);
				if (!ppars.contains(node) && !ppars.stream().anyMatch(p -> Utils.IsEquivalent(node, p))
						&& IsParallel(tpa, _region, ng)) {
					res.add(node);
					break;
				}
			}

			/*
			 * foreach (Node node in region.Where(r => !ppars.Contains(r) &&
			 * !ppars.Any(p=>PMLogHelper.IsEquivalent(r,p)))) { if (IsParallel(tpa, region,
			 * ppars.Union(new TPATemplate.Node[] {node}).ToArray())) { res.Add(node);
			 * break; } }
			 */
		}
		return res;
	}

	/// APLY PARALLEL REGION

	void ApplyParallel(TPA tpa, Node prev, Collection<Node> parallels, Collection<Node> region, Node post) {

		var rests = SplitSequencesinsideParallel(tpa, region, parallels);

		if (rests != null) {
			Utils.ShowTPA(tpa, "audit0");
			Collection<Collection<Node>> Sequences = rests.values(); // Values.Select(v => v.ToArray()).ToArray();
			RemoveInterSplitTransitions(tpa, region, Sequences, parallels, prev, post);
			Utils.ShowTPA(tpa, "audit1");
			RemoveRepeatedTransitions(tpa);
			// Utils.ShowTPA(tpa, "audit1");
			FuseParallelEquivalentNodes(tpa, region, parallels);
			// Utils.ShowTPA(tpa, "audit1");
			RemoveRepeatedTransitions(tpa);
			// Utils.ShowTPA(tpa, "audit1");
			MineOnwardMerge(tpa, TransitionsMergeMode.Inline);
			// Utils.ShowTPA(tpa, "audit1");
			RemoveRepeatedTransitions(tpa);
			// Utils.ShowTPA(tpa, "audit1");
			RemoveParallelSelfloops(tpa, parallels);
			// Utils.ShowTPA(tpa, "audit1");
			Transition tpre = new Transition(tpa);
			tpre.getSourceNodes().add(prev);
			tpre.getEndNodes().addAll(parallels);
			// Utils.ShowTPA(tpa, "audit1");

			// prev.getOutTransitions().Where(nt => nt.EndNodes.Count == 1 &&
			// parallels.Select(p => p.Id).Contains(nt.EndNodes.First())).ToList()
			// .ForEach(nt => tpa.NodeTransitions.Remove(nt));
			for (var trx : prev.getOutTransitions().stream().filter(nt -> nt.getEndNodes().size() == 1
					&& parallels.contains(nt.getEndNodes().stream().findFirst().get())).toList()) {
				RemoveTransition(tpa, trx);
				// tpa.removeTransition(trx);
			}

			for (var fin : SequenceFinals(tpa, Sequences, post)) {

				// tpa.AddNodeTransition(fin.Select(p => p.Id).ToArray(), new Guid[] { post.Id
				// }, "");
				Transition tx = new Transition(tpa);
				tx.getSourceNodes().addAll(fin);
				tx.getEndNodes().add(post);

				// foreach (var pt in post.getInTransitions(tpa).Where(nt =>
				// nt.SourceNodes.Count == 1)) { if (fin.Select(f =>
				// f.Id).Contains(pt.SourceNodes.First())) { tpa.NodeTransitions.Remove(pt); } }

				for (var pt : post.getInTransitions()) {
					if (pt.getSourceNodes().size() == 1
							&& fin.contains(pt.getSourceNodes().stream().findFirst().get())) {
						tpa.removeTransition(pt);
					}
				}

			}

		} else {
			// remove the region
			for (var n : region) {
				CutsHelper.DeleteNode(tpa, n);
			}
			for (var n : parallels) {
				CutsHelper.DeleteNode(tpa, n);
			}
			// recreate the region
			tpa.registerNode((Node[]) parallels.toArray());
			var st = new Transition(tpa);
			st.addSource(prev);
			st.addEnd((Node[]) parallels.toArray());
			tpa.registerTransition(st);
			var ot = new Transition(tpa);
			ot.addSource((Node[]) parallels.toArray());
			ot.addEnd(post);
			tpa.registerTransition(ot);

		}

		// Utils.ShowTPA(tpa, "audit1");

	}

	void RemoveParallelSelfloops(TPA tpa, Collection<Node> parallels) {
		for (var p : parallels) {
			// p.getOutTransitions(tpa).Where(nt => nt.EndNodes.Count == 1 &&
			// nt.EndNodes.First() == p.Id).ToList().ForEach(nt =>
			// tpa.NodeTransitions.Remove(nt));
			for (var loop : p.getOutTransitions().stream()
					.filter(nt -> nt.getEndNodes().size() == 1 && nt.getEndNodes().stream().findFirst().get() == p)
					.toList()) {
				tpa.removeTransition(loop);
			}
		}
	}

	List<List<Node>> SequenceFinals(TPA tpa, Collection<Collection<Node>> Sequences, Node post) {
		List<List<Node>> finals = new ArrayList<List<Node>>();
		for (Collection<Node> seq : Sequences) {
			// var x0 = post.getInTransitions()
			// .Where(nt => nt.SourceNodes.Count == 1).Select(nt =>
			// nt.getSourceNodes(tpa).First())
			// .Where(n => seq.Contains(n)).ToArray();
			List<Node> x0 = post.getInTransitions().stream().filter(nt -> nt.getSourceNodes().size() == 1)
					.map(nt -> nt.getSourceNodes().stream().findFirst().get()).filter(n -> seq.contains(n)).toList();

			finals.add(x0);

		}
		// var res = new List<Collection<Node>>();
		// var res = CartesianProduct<TPATemplate.Node>(finals).Select(x =>
		// x.ToArray()).ToArray();

		List<List<Node>> res = cartesianProduct(finals);
		return res;

	}

	public static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
		// check if incoming data is not null
		if (lists == null)
			return Collections.emptyList();
		// Cartesian product, intermediate result
		List<List<T>> cp = Collections.singletonList(Collections.emptyList());
		// iterate through incoming lists
		for (List<T> list : lists) {
			// non-null and non-empty lists
			if (list == null || list.size() == 0)
				continue;
			// intermediate result for next iteration
			List<List<T>> next = new ArrayList<>();
			// rows of current intermediate result
			for (List<T> row : cp) {
				// elements of current list
				for (T el : list) {
					// new row for next intermediate result
					List<T> nRow = new ArrayList<>(row);
					nRow.add(el);
					next.add(nRow);
				}
			}
			// pass to next iteration
			cp = next;
		}
		// Cartesian product, final result
		return cp;
	}

	HashMap<Node, Collection<Node>> SplitSequencesinsideParallel(TPA tpa, Collection<Node> region,
			Collection<Node> parallels) {

		Collection<Node> regionrests = new ArrayList();

		for (var r : region) {
			if (!parallels.stream().anyMatch(h -> Utils.IsEquivalent(h, r))) {
				regionrests.add(r);
			}
		}
		if (regionrests.size() > 0) {
			HashMap<Node, Collection<Node>> dict = new HashMap<Node, Collection<Node>>();
			// var dict = parallels.ToDictionary(p => p, p => new List<TPATemplate.Node>() {
			// p });
			for (var p : parallels) {
				dict.put(p, new ArrayList<Node>() {
				});
				dict.get(p).add(p);
			}
			for (var rr : regionrests) {
				for (var p : parallels) {
					Collection<Node> np = new ArrayList();
					np.add(p);
					np.add(rr);
					if (!IsParallel(tpa, region, np)) {
						dict.get(p).add(rr);
						break;
					}
				}
			}
			return dict;

		}

		return null;
	}

	void FuseParallelEquivalentNodes(TPA tpa, Collection<Node> region, Collection<Node> parallels) {
		for (var p : parallels) {
			// var eq = region.Where(r => PMLogHelper.IsEquivalent(r, p)).Except(new
			// TPATemplate.Node[] { p }).ToArray();
			var eq = region.stream().filter(r -> Utils.IsEquivalent(r, p)).toList();
			for (var e : eq) {
				if (!parallels.contains(e)) {
					FuseNodes(tpa, p, e);
				}
			}
		}
	}

	void RemoveInterSplitTransitions(TPA tpa, Collection<Node> region, Collection<Collection<Node>> Sequences,
			Collection<Node> parallels, Node pre, Node post) {
		Collection<Transition> res = new ArrayList<Transition>();
		for (Collection<Node> s0 : Sequences) {
			for (Collection<Node> s1 : Sequences) {
				if (s0 != s1) {
					res.addAll(GetInterSplitTransitions(tpa, region, s0, s1, parallels, pre, post));

				}
			}
		}

		// Utils.ShowTPA(tpa, "audit0");
		// res.ForEach(nt => tpa.NodeTransitions.Remove(nt));
		for (var nt : res) {
			// Utils.removeifnotbreakingtransition(tpa, nt);
			tpa.removeTransition(nt);
		}
		// Utils.ShowTPA(tpa, "audit1");
		// Utils.CheckAuditError(tpa, "audit1");

	}

	Collection<Transition> GetInterSplitTransitions(TPA tpa, Collection<Node> region, Collection<Node> _s0,
			Collection<Node> _s1, Collection<Node> parallels, Node pre, Node post) {
		Collection<Transition> res = new ArrayList<Transition>();
		// var s = region.Where(x => _s0.Any(y => PMLogHelper.IsEquivalent(x,
		// y))).Union(_s0).ToArray();
		// s0 are all the _s0 that are in the region
		List<Node> s0 = new ArrayList(_s0);
		for (var x : region) {
			if (_s0.stream().anyMatch(y -> Utils.IsEquivalent(x, y))) {
				s0.add(x);
			}
		}

		// var s1 = region.Where(x => _s1.Any(y => PMLogHelper.IsEquivalent(x,
		// y))).Union(_s1).ToArray();
		// s1 are all the _s1 that are in the region
		List<Node> s1 = new ArrayList(_s1);
		for (var x : region) {
			if (_s1.stream().anyMatch(y -> Utils.IsEquivalent(x, y))) {
				s1.add(x);
			}
		}
		s0 = GetForwardOrderedNodes(tpa, s0).stream().toList();
		s1 = GetForwardOrderedNodes(tpa, s1).stream().toList();

		boolean haschanges = true;
		while (haschanges) {
			haschanges = false;
			for (var n0 : s0) {
				for (var n1 : s1) {
					var outcon = getSimpleTransition(tpa, n0, n1);
					// Utils.ShowTPA(tpa, "out1");
					if (outcon.size() > 0) {// we find transition that are inter sequences

						// Utils.ShowTPA(tpa, "audit0");
						SolveInterSequence(tpa, n0, n1, s0, s1, pre, post, parallels, outcon);
						// Utils.ShowTPA(tpa, "audit1");
						// res.add(outcon.stream().findFirst().get());
						haschanges = true;
					}
				}
			}
		}
		// ShowTPA(tpa, "out10");
		return res;
	}

	private void SolveInterSequence(TPA tpa, Node n0, Node n1, Collection<Node> s0, Collection<Node> s1, Node pre,
			Node post, Collection<Node> parallels, Collection<Transition> outcon) {

		var intertrans = outcon.stream().findFirst().get();
		// ANALYZE N0->N1->*
		// var db = GetForwardNodes(tpa, n1).ToArray();
		// var fn = GetForwardNodes(tpa, n1).Where(n => s0.Any(nx =>
		// PMLogHelper.IsEquivalent(n, nx))).ToArray();
		var fwd = ConcurrentNodesHelper.GetForwardNodes(n1);
		List<Node> fn = new ArrayList();
		for (var x : ConcurrentNodesHelper.GetForwardNodes(n1)) {
			if (s0.stream().anyMatch(y -> Utils.IsEquivalent(x, y))) {
				fn.add(x);
			}
		}

		if (fn.size() > 0) {
			// tpa.AddNodeTransition(new Guid[] { n0.Id }, new Guid[] { x0.Id }, "");
			var nx = fn.get(0);
			CreateTransition(tpa, n0, nx);
			RemoveTransition(tpa, intertrans);
			// Utils.ShowTPA(tpa, "audit1");

		} else {

			// tpa.AddNodeTransition(new Guid[] { n0.Id }, new Guid[] { post.Id }, "");
			CreateTransition(tpa, n0, post);
			RemoveTransition(tpa, intertrans);
			// Utils.ShowTPA(tpa, "audit1");

		}
		if (!parallels.contains(n0))// N0 is not initial
		{
			// Analyze *->N0->N1
			// var bn = GetBackwardNodes(tpa, n0).Where(n => s1.Any(nx =>
			// PMLogHelper.IsEquivalent(n, nx))).ToArray();
			List<Node> bn = new ArrayList();
			var bck = ConcurrentNodesHelper.GetBackwardNodes(n0);
			for (var x : ConcurrentNodesHelper.GetBackwardNodes(n0)) {
				if (s1.stream().anyMatch(y -> Utils.IsEquivalent(x, y))) {
					bn.add(x);
				}
			}

			// if (bn.FirstOrDefault() is TPATemplate.Node x1)
			if (bn.size() > 0) {
				var x1 = bn.get(0);
				if (x1 != n1) {
					// tpa.AddNodeTransition(new Guid[] { x1.Id }, new Guid[] { n1.Id }, "");
					CreateTransition(tpa, x1, n1);
					// Utils.ShowTPA(tpa, "audit1");
				}
			} else {
				var nprev = s1.stream().findFirst().get();
				if (nprev != n1) {
					// tpa.AddNodeTransition(new Guid[] { prev.Id }, new Guid[] { n1.Id }, "");
					if (!Utils.IsEquivalent(n1, nprev)) {
						CreateTransition(tpa, nprev, n1);
						// Utils.ShowTPA(tpa, "audit1");
					} else {
						// FuseNodes(tpa, prev, n1);
						CreateTransition(tpa, pre, n1);
						// Utils.ShowTPA(tpa, "audit1");
					}

				}
			}
		} else {
			CreateTransition(tpa, s1.stream().findFirst().get(), n1);
			// Utils.ShowTPA(tpa, "audit1");
		}
		// Utils.ShowTPA(tpa, "audit1");
	}

	private Collection<Transition> getSimpleTransition(TPA tpa, Node n0, Node n1) {
		var res = n0.getOutTransitions().stream().filter(
				nt -> nt.getEndNodes().size() == 1 && nt.getEndNodes().stream().findFirst().get().getId() == n1.getId())
				.toList();
		return res;
	}

	private void CreateTransition(TPA tpa, Node n0, Node n1) {
		/*
		 * var tx = new Transition(tpa); tx.getSourceNodes().add(n0);
		 * tx.getEndNodes().add(n1);
		 */
		var t = tpa.createTransition(n0).addEnd(n1);
		tpa.registerTransition(t);
	}

	private void CreateTransition(TPA tpa, Collection<Node> n0, Collection<Node> n1) {
		/*
		 * var tx = new Transition(tpa); tx.getSourceNodes().add(n0);
		 * tx.getEndNodes().add(n1);
		 */
		var t = tpa.createTransition(n0).addEnd(n1);
		tpa.registerTransition(t);
	}

	private void RemoveTransition(TPA tpa, Transition nt) {
		/*
		 * var tx = new Transition(tpa); tx.getSourceNodes().add(n0);
		 * tx.getEndNodes().add(n1);
		 */
		tpa.removeTransition(nt);
	}

	// END APPLY PARALLEL REGION

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
		var prevregion = CutsHelper.GetBackwardedGroup(tpa, prev, tpa.iterateNodes());
		var postregion = CutsHelper.GetForwardedGroup(tpa, post, tpa.iterateNodes());
		var iprev = IntersectNodes(middleregion, prevregion);
		var ipost = IntersectNodes(middleregion, postregion);
		if (iprev.size() == 0 && ipost.size() == 0) {
			return middleregion;
		}
		return null;

	}

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
			var nod = nt.getEndNodes();
			res.addAll(nt.getEndNodes());
		}
		return res;
	}

	private Collection<Transition> GetAccesibleTransitions(TPA tpa, Transition nt0, boolean backward) {
		Set<Transition> res = new HashSet<>();
		if (backward) {
			res.addAll(ConcurrentNodesHelper.GetBackwardTransitions(nt0.getEndNodes().iterator().next()));
		} else {
			res.addAll(ConcurrentNodesHelper.GetForwardTransitions(nt0.getSourceNodes().iterator().next()));
		}
		return res;
	}

	/*
	 * private Collection<Transition> GetForwardTransitions(TPA tpa, Node n0) {
	 * Set<Transition> res = new HashSet<>(); Set<Transition> transitions = new
	 * HashSet<>(); transitions.addAll(n0.getOutTransitions()); while
	 * (transitions.size() > 0) { Transition t = transitions.iterator().next(); if
	 * (!res.contains(t)) { res.add(t); Node nf = t.getEndNodes().iterator().next();
	 * transitions.addAll(nf.getOutTransitions()); } transitions.remove(t); }
	 * 
	 * return res; }
	 * 
	 * private Collection<Transition> GetBackwardTransitions(TPA tpa, Node n0) {
	 * Set<Transition> res = new HashSet<>(); Set<Transition> transitions = new
	 * HashSet<>(); transitions.addAll(n0.getInTransitions()); while
	 * (transitions.size() > 0) { Transition t = transitions.iterator().next(); if
	 * (!res.contains(t)) { res.add(t); Node nf =
	 * t.getSourceNodes().iterator().next();
	 * transitions.addAll(nf.getInTransitions()); } transitions.remove(t); }
	 * 
	 * return res; }
	 */

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
			region = tpa.iterateNodes();
		}
		return region.stream().filter(n -> Utils.IsEquivalent(n0, n)).toList();
	}
}
