package palia.graphviz;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * This code is part of the GraphViz ProM package.
 *
 * @author Sander Leemnans
 * @author Andrea Burattin
 * @see {@link http://promtools.org/}
 * @see {@link http://leemans.ch/}
 * @see {@link https://svn.win.tue.nl/repos/prom/Packages/GraphViz/Trunk/src/org/processmining/plugins/graphviz/dot/}
 */
public abstract class AbstractDotElement implements DotElement {

	private static final long serialVersionUID = -2029055045617878563L;
	private final String id;
	private HashMap<String, String> optionsMap;
	private String label;

	protected AbstractDotElement() {
		id = "e" + UUID.randomUUID().toString();
		label = "";
		optionsMap = new HashMap<>();
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String labelToString() {
		return escapeString(label);
	}

	protected String escapeString(String value) {
		if (value.length() > 2 && value.substring(0, 1).equals("<")
				&& value.substring(value.length() - 1, value.length()).equals(">")) {
			return value;
		} else {
			String value2 = value.replace("\"", "\\\"");
			return "\"" + value2 + "\"";
		}
	}

	@Override
	public void setOption(String key, String value) {
		optionsMap.put(key, value);
	}

	@Override
	public String getOption(String key) {
		if (optionsMap.containsKey(key)) {
			return optionsMap.get(key);
		}
		return null;
	}

	@Override
	public Set<String> getOptionKeySet() {
		return Collections.unmodifiableSet(optionsMap.keySet());
	}
}