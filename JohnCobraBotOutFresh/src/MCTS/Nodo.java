package MCTS;

import java.util.ArrayList;
import java.util.LinkedList;
import enumerate.Action;
import struct.FrameData;

public class Nodo {

	// CONSTANTS

	
	// LINK
	public FrameData fd;
	public LinkedList<Action> myActions;

	public Nodo parent;
	public ArrayList<Nodo> children;
	public int depth; 

	//PROPERTIES
	public int t;
	public int n; 
	public double ucb1; 
	
	//root
	public Nodo(FrameData fd) {
		myActions=new LinkedList<Action>();
		this.fd=fd;
		parent = null;		
		children = new ArrayList<Nodo>();
		depth = 0;
		n = 0;
		t = 0;
	}
	
	public Nodo(Nodo parent, Action action, FrameData fd) {
		this(fd);
		this.parent=parent;
		myActions.addAll(this.parent.myActions);
		myActions.addLast(action);
		depth=parent.depth+1;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}
	
	@Override
	public String toString() {
		String result = "("+t+"/"+n+") "+depth+" UCB1: "+ ucb1+" //		"+myActions.toString() ;
		return result;
	}

}
