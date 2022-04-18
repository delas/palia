package palia.algorithm;

import java.util.Collection;

import palia.model.Node;

public class PaliaGrouper {

	private Boolean IsNodeGroup(Node[] grouping) 
	{
		
		
		return null;
		
	}
	
	public static Collection<Node[]> CreateGroups(Collection<Node> g)
	{
		return null;
	}
	
//	public static IEnumerable<T[]> CreateGroups(IEnumerable<T> g ,Func<T[], bool> isgroupfunction)
//    {
//        List<T[]> res = g.Select(x=>new T[]{x}).ToList();
//        var dc = new DynamicCollection<T[]>(res);
//        bool changed = true;
//        while (changed)
//        {
//            var x = SingleGroupFusion(res, isgroupfunction);
//            changed = x.changed;
//            res = x.groups.ToList();
//        }
//
//        return res;
//    }

//    static (IEnumerable<T[]> groups, bool changed) SingleGroupFusion(IEnumerable<T[]> groups, Func<T[], bool> isgroupfunction)
//    {
//
//        foreach (var i0 in groups.ToArray())
//        {
//            foreach (var i1 in groups.ToArray())
//            {
//                if (i0 != i1)
//                {
//                    var ix = i0.Union(i1).ToArray();
//                    if (isgroupfunction(ix))
//                    {
//                        var res = groups.Except(new T[][] { i0, i1 }).ToList(); 
//                        res.Add(ix);
//                        return (res,true);
//                    }
//
//                }
//
//            }
//
//        }
//        return (groups,false);
//    }
//}
}
