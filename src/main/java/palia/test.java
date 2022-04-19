package palia;

import java.io.File;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
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

	public static void main(String[] args) throws Exception {
		GraphExporter.exportSVG(mine(), new File("output/out.svg"));
		System.out.println("done");
	}

	public static TPA getTPA() {
		TPA tpa = new TPA();

		Node A = new Node(tpa, "A");
		Node B = new Node(tpa, "B");
		Node C = new Node(tpa, "C");
		Node D = new Node(tpa, "D");
		Node E = new Node(tpa, "E");
		Node F = new Node(tpa, "F");
		Node G = new Node(tpa, "G");
		Node H = new Node(tpa, "H");
		Node I = new Node(tpa, "I");

		A.setStartingNode(true);
		I.setFinalNode(true);

		Transition t1 = new Transition(tpa);
		Transition t2 = new Transition(tpa);
		Transition t3 = new Transition(tpa);
		Transition t4 = new Transition(tpa);
		Transition t5 = new Transition(tpa);
		Transition t6 = new Transition(tpa);
		Transition t7 = new Transition(tpa);

		t1.addSource(A).addEnd(B);
		t2.addSource(A).addEnd(C);
		t3.addSource(B).addEnd(D);
		t4.addSource(C).addEnd(D);
		t5.addSource(D).addEnd(E, F);
		t6.addSource(E, F).addEnd(G, H);
		t7.addSource(G, H).addEnd(I);

		return tpa;
	}

	public static TPA mine(String filename) throws Exception {
		Palia p = new Palia();
		XParser parser = new XesXmlGZIPParser();
		XLog log = parser.parse(new File(filename)).get(0);
		return p.mine(log);
	}

	public static TPA mine() {
		Palia p = new Palia();
		return p.mine(getLog2());
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
		String res = "J E F T J A";
		return getLogfromString(res);
	}

	public static XLog getLog0() {
		String res = "A B1 C E G\n" + "A B2 C E F\n" + "A B C F";
		return getLogfromString(res);
	}

	public static XLog getLog1() {
		String res = "A X Y Z D H\n" + "A X Z Y D H\n" + "A Y Z X D H\n" + "A Y X Z D H\n" + "A Z X Y D H\n"
				+ "A Z Y X D H";
		return getLogfromString(res);
	}

	public static XLog getLog2() {
		String res = "A C B D G\n" + "A B C D G\n" + "A E F T D H\n" + "A E F T E F T E F T D H\n" + "A W W W A\n"
				+ "A P E F T P A\n" + "A X Y Z D H\n" + "A X Z Y D H\n" + "A Y Z X D H\n" + "A Y X Z D H\n"
				+ "A Z X Y D H\n" + "A Z Y X D H";
		return getLogfromString(res);
	}

	public static XLog getLog3() {
		String res = "A X X1 Y Z Y1 Z1 D H\n" + "A X Z Y X1 Y1 Z1 D H\n" + "A Y Y1 Z Z1 X X1 D H\n"
				+ "A Y X Y1 X1 Z Z1 D H\n" + "A Z Z1 X Y X1 Y1 D H\n" + "A Z Y Z1 Y1 X X1 D H";
		return getLogfromString(res);
	}

	public static XLog getLogfromString(String s) {
		XLog log = factory.createLog();
		String[] st = s.split("\n");
		for (String tline : st) {
			XTrace trc = factory.createTrace();
			String[] token = tline.split(" ");
			for (String t : token) {
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
