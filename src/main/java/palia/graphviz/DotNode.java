package palia.graphviz;

import java.util.Map;
import java.util.Map.Entry;

/**
 * This code is part of the GraphViz ProM package.
 *
 * @author Sander Leemnans
 * @see {@link http://promtools.org/}
 * @see {@link http://leemans.ch/}
 * @see {@link https://svn.win.tue.nl/repos/prom/Packages/GraphViz/Trunk/src/org/processmining/plugins/graphviz/dot/}
 */
public class DotNode extends AbstractDotElement {

	private static final long serialVersionUID = 7718543258727675772L;

	protected DotNode(String label, Map<String, String> optionsMap) {
		this.setLabel(label);
		if (optionsMap != null) {
			for (Entry<String, String> e : optionsMap.entrySet()) {
				setOption(e.getKey(), e.getValue());
			}
		}
	}

	public boolean equals(Object object) {
		if (!(object instanceof DotNode)) {
			return false;
		}
		return ((DotNode) object).getId().equals(getId());
	}

	public String toString() {
		String result = "\"" + getId() + "\" [label=" + labelToString() + ", id=\"" + getId() + "\"";
		for (String key : getOptionKeySet()) {
			result += "," + key + "=" + escapeString(getOption(key));
		}
		return result + "];";
	}

}