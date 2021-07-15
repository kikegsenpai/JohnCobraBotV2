package MCTS;

import java.util.ArrayList;
import java.util.LinkedList;
import enumerate.Action;
import struct.FrameData;

public class Node {

	// CONSTANTS

	
	// LINK
	public FrameData fd;
	public LinkedList<Action> myActions;

	public Node parent;
	public ArrayList<Node> children;
	public int depth; 

	//PROPERTIES
	public int t;
	public int n; 
	public double ucb1; 
	
	//root
	public Node(FrameData fd) {
		myActions=new LinkedList<Action>();
		this.fd=fd;
		parent = null;		
		children = new ArrayList<Node>();
		depth = 0;
		n = 0;
		t = 0;
	}
	
	public Node(Action action, FrameData fd) {
		this(fd);
		myActions.addAll(parent.myActions);
		myActions.addLast(action);
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}
	
	@Override
	public String toString() {
		String result = "("+t+"/"+n+") "+depth+" UCB1: "+ ucb1+" //		+action.name()" ;
		return result;
	}

}
