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
		GraphExporter.exportSVG(mine(), new File("output/out.svg"));
		System.out.println("done");
	}
	
	public static TPA mine() {
		Palia p = new Palia();
		return p.mine(getLog1());
	}
	
	public static XLog getLog() {
		
		XLog log = factory.createLog();
		XTrace t1 = factory.createTrace();
		t1.add(createEvent("A"));
		t1.add(createEvent("B1"));
		t1.add(createEvent("C"));
		t1.add(createEvent("E"));
		t1.add(createEvent("G"));
		XTrace t2 = factory.createTrace();
		t2.add(createEvent("A"));
		t2.add(createEvent("B2"));
		t2.add(createEvent("C"));
		t2.add(createEvent("E"));
		t2.add(createEvent("F"));
		XTrace t3 = factory.createTrace();
		t3.add(createEvent("A"));
		t3.add(createEvent("B"));
		t3.add(createEvent("C"));
		t3.add(createEvent("F"));
		
		log.add(t1);
		log.add(t2);
		log.add(t3);
		return log;
	}
	
	public static XEvent createEvent(String name) {
		XEvent e = factory.createEvent();
		XConceptExtension.instance().assignName(e, name);
		return e;
	}
	
	
	public static XLog getLogbug() {
		String res ="J E F T J A";
		return getLogfromString(res);
	}
	
	public static XLog getLog0() {
		String res = "A B1 C E G\n"
				+ "A B2 C E F\n"
				+ "A B C F";
		return getLogfromString(res);
	}
	
	
	public static XLog getLog1() {
		String res = "A C B D G\n"
				+ "A B C D G\n"
				+ "A E F T D H\n"
				+ "A E F T E F T E F T D H\n"
				+ "A W W W A\n"
				+ "A P E F T P A\n"
				+ "A X Y Z D H\n"
				+ "A X Z Y D H\n"
				+ "A Y Z X D H\n"
				+ "A Y X Z D H\n"
				+ "A Z X Y D H\n"
				+ "A Z Y X D H";
		return getLogfromString(res);
	}
	
	public static XLog getLogfromString(String s) 
	{
		XLog log = factory.createLog();
		String[] st = s.split("\n");
		for(String tline : st) 
		{
			XTrace trc = factory.createTrace();
			String [] token = tline.split(" ");
			for(String t : token) 
			{
				trc.add(createEvent(t));
			}
			log.add(trc);
		}
		
		return log;
		
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
