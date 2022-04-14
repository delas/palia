package palia;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import palia.algorithm.Palia;
import palia.graphviz.exporter.GraphExporter;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class test {

	private static XFactory factory = new XFactoryNaiveImpl();
	
	public static void main(String[] args) throws IOException {
		GraphExporter.exportSVG(mine(), new File("C:\\Users\\andbur\\Desktop\\out.svg"));
		System.out.println("done");
	}
	
	public static TPA mine() {
		Palia p = new Palia();
		return p.mine(getLog());
	}
	
	public static XLog getLog() {
		
		XLog log = factory.createLog();
		XTrace t1 = factory.createTrace();
		t1.add(createEvent("A"));
		t1.add(createEvent("B"));
		t1.add(createEvent("C"));
		t1.add(createEvent("E"));
		XTrace t2 = factory.createTrace();
		t2.add(createEvent("K"));
		t2.add(createEvent("B"));
		t2.add(createEvent("C"));
		t2.add(createEvent("E"));
		
		log.add(t1);
		log.add(t2);
		return log;
	}
	
	public static XEvent createEvent(String name) {
		XEvent e = factory.createEvent();
		XConceptExtension.instance().assignName(e, name);
		return e;
	}
	
	public static TPA getSimple() {
		TPA tpa = new TPA();
		Node n1 = new Node(tpa, "n1");
		Node n2 = new Node(tpa, "n2");
		Node n3 = new Node(tpa, "n3");
		Node n4 = new Node(tpa, "n4");
		
		Transition t1 = new Transition(tpa);
		t1.getSourceNodes().add(n1);
		t1.getSourceNodes().add(n3);
		t1.getEndNodes().add(n2);
		return tpa;
	}
}
