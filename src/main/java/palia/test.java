package palia;

import java.io.File;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import palia.algorithm.Palia;
import palia.graphviz.exporter.GraphExporter;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;
import palia.utils.TraceFitPath;

public class test {

	private static XFactory factory = new XFactoryNaiveImpl();

	public static void main(String[] args) throws Exception {

		String fn = "output/pdc_2020_0000000.xes";
		var tpa = mine();
		GraphExporter.exportSVG(tpa, new File("output/out.svg"));
		System.out.println("done");
	}

	public static TPA getTPA() {
		TPA tpa = new TPA();

		Node A = tpa.createNode("A");
		Node B2 = tpa.createNode("B2");
		Node C1 = tpa.createNode("C1");
		Node C2 = tpa.createNode("C2");
		Node D = tpa.createNode("D");
		Node E = tpa.createNode("E");
		Node F = tpa.createNode("F");
		Node G = tpa.createNode("G");
		Node H = tpa.createNode("H");
		Node I = tpa.createNode("I");
		Node J = tpa.createNode("J");

		A.setStartingNode(true);
		J.setFinalNode(true);

		tpa.createTransition(A).addEnd(D);
		tpa.createTransition(A).addEnd(C1);
		tpa.createTransition(A).addEnd(B2);
		tpa.createTransition(B2).addEnd(D);
		tpa.createTransition(C1).addEnd(C2);
		tpa.createTransition(C2).addEnd(D);
		tpa.createTransition(D).addEnd(E, F);
		tpa.createTransition(E, F).addEnd(G, H);
		tpa.createTransition(G, H).addEnd(I);
		tpa.createTransition(I).addEnd(J);

		return tpa;
	}

	public static TPA getTPA2() {
		TPA tpa = new TPA();

		Node A = tpa.createNode("A");
		A.setStartingNode(true);
		Node B = tpa.createNode("B");
		Node C = tpa.createNode("C");
		Node D = tpa.createNode("D");
		Node E = tpa.createNode("E");
		E.setFinalNode(true);

		tpa.createTransition(A).addEnd(B, C);
		tpa.createTransition(A).addEnd(D);
		tpa.createTransition(B, C).addEnd(E);
		tpa.createTransition(D).addEnd(E);

		return tpa;
	}

	public static TPA mine(String filename) throws Exception {
		Palia p = new Palia();
		// XParser parser = new XesXmlGZIPParser();
		XParser parser = new XesXmlParser();
		XLog log = parser.parse(new File(filename)).get(0);
		return p.mine(log);
	}

	public static TPA mine() {
		Palia p = new Palia();

		// var log = getLogFile("C:/Carlos/Proyectos/DatosPalia/XES/Training
		// Logs/pdc_2020_0101000.xes");
		// var log = getLog9();

		var log = getLog3();

		var res = p.mine(log);

		return res;
	}

	public static TPA mineandtest() {
		Palia p = new Palia();
		var log = getLog3();
		var res = p.mine(log);

		String logtest = "A X X1 Y Z Y1 Z1 D H\n" + "A X X1 Y1 Y Z Z1 D H\n" + "A X Z Y X1 Y1 Z1 D H\n"
				+ "A Y Y1 Z Z1 X X1 D H\n" + "A Y X Y1 X1 Z Z1 D H\n" + "A Z Z1 X Y X1 Y1 D H\n"
				+ "A Z Y Z1 Y1 X X1 D H";
		var testlog = getLogfromString(logtest);
		// ConformanceTPA.CreateStates(res);
		TraceFitPath.ReindexParallelTranstions(res);
		for (var trc : testlog) {

			List<String> tstring = trc.stream().map(e -> XConceptExtension.instance().extractName(e)).toList();
			var tracename = String.join(" ", tstring);

			// if (ConformanceTPA.TraceFit(res, trc)) {
			if (TraceFitPath.TraceFit2(res, trc)) {
				System.out.println("Trace [" + tracename + "] Fits");
			} else {
				System.out.println("Trace [" + tracename + "] Not Fits");
			}
		}

		return res;
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

	public static XLog getLog3a() {
		String res = "A X X1 Y Y1 D H\n" + "A X Y X1 Y1 D H\n" + "A Y Y1 X X1 D H\n" + "A Y X Y1 X1 D H\n"
				+ "A X Y X1 Y1 D H\n" + "A Y Y1 X X1 D H";
		return getLogfromString(res);
	}

	public static XLog getLog4() {
		String res = "A C B D G\n" + "A B C D G\n" + "A E F T D H\n" + "A E F T E F T E F T D H\n" + "A W W W A\n"
				+ "A J E F T J A\n" + "A X X1 Y Z Y1 Z1 D H\n" + "A X Z Y X1 Y1 Z1 D H\n" + "A Y Y1 Z Z1 X X1 D H\n"
				+ "A Y X Y1 X1 Z Z1 D H\n" + "A Z Z1 X Y X1 Y1 D H\n" + "A Z Y Z1 Y1 X X1 D H";
		return getLogfromString(res);
	}

	public static XLog getLog5() {
		String res = "A X X1 Y Z X2 Y1 Y2 Z1 Z2A Z3 D H\n" + "A X Z Y X1 Y1 X2 Y2 Z1 Z2B Z3 D H\n"
				+ "A Y Y1 Z Y2 Z1 X Z2A X1 X2 Z3 D H\n" + "A Y X Y1 X1 Y2 Z Z1 X2 Z2B Z3 D H\n"
				+ "A Z Z1 Z2B X Y X1 Z3 Y1 X2 Y2 D H\n" + "A Z Y Z1 Z2A Y1 Y2 Z3 X X1 X2 D H\n"
				+ "A Z Z1 Z2B Z3 Y Y1 Y2 X X1 X2 D H\n" + "A Y Y1 Y2 Z Z1 Z2A Z3 X X1 X2 D H\n"
				+ "A Z Z1 Z2A Z3 Y Y1 Y2 X X1 X2 D H\n" + "A X X1 X2 Y Y1 Y2 Z Z1 Z2B Z3 D H";
		return getLogfromString(res);
	}

	public static XLog getLog6() {
		String res = "A X Y Z MM X1 Y1 Z1 D H\n" + "A X Z Y MM X1 Z1 Y1 D H\n" + "A Y Z X MM Y1 Z1 X1 D H\n"
				+ "A Y X Z MM Y1 X1 Z1 D H\n" + "A Z X Y MM Z1 X1 Y1 D H\n" + "A Z Y X MM Z1 Y1 X1 D H";
		return getLogfromString(res);
	}

	public static XLog getLog7() {
		String res = "A X X1 Y Z Y1 Z1 Z2A Z3 D H\n" + "A X Z Y X1 Y1 Z1 Z2B Z3 D H\n" + "A Y Y1 Z Z1 X Z2A X1 Z3 D H\n"
				+ "A Y X Y1 X1 Z Z1 Z2B Z3 D H\n" + "A Z Z1 Z2B X Y X1 Z3 Y1 D H\n" + "A Z Y Z1 Z2A Y1 Z3 X X1 D H\n"
				+ "A Z Z1 Z2B Z3 Y Y1 X X1 D H\n" + "A Y Y1 Z Z1 Z2A Z3 X X1 D H\n" + "A Z Z1 Z2A Z3 Y Y1 X X1 D H\n"
				+ "A X X1 Y Y1 Z Z1 Z2B Z3 D H";
		return getLogfromString(res);
	}

	public static XLog getLog8() {
		String res = "A B C D E F\n" + "A C B D E G\n" + "A D C B E G";
		return getLogfromString(res);
	}

	public static XLog getLog9() {

		String fn = "output/pdc_2020_0000000.xes";
		XParser parser = new XesXmlParser();
		XLog log;
		try {
			log = parser.parse(new File(fn)).get(0);
			return log;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static XLog getLog10() {
		String res = "A D E F\n" + "A E F\n" + "B D E F\n" + "C D E F";
		return getLogfromString(res);
	}

	public static XLog getLog11() {
		String res = "A C B B1 D D H A\n" + "A B B1 C D H D D H A\n" + "A D H D D H D H A\n";
		return getLogfromString(res);
	}

	public static XLog getLogFile(String fn) {

		XParser parser = new XesXmlParser();
		XLog log;
		try {
			log = parser.parse(new File(fn)).get(0);
			return log;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static XLog getLogfromString(String s) {
		XLog log = factory.createLog();
		String[] st = s.split("\n");
		for (String tline : st) {
			XTrace trc = factory.createTrace();
			// trc.add(createEvent("Start"));
			String[] token = tline.split(" ");
			for (String t : token) {
				trc.add(createEvent(t));
			}
			// trc.add(createEvent("End"));
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
