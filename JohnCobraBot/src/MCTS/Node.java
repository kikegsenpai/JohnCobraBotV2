package MCTS;

import java.util.ArrayList;
import java.util.LinkedList;

import aiinterface.CommandCenter;
import enumerate.Action;
import struct.FrameData;
import struct.GameData;

public class Node {

	// CONSTANTS

	// LINK
	public Action action;
	public Node parent;
	public ArrayList<Node> children;
	public int depth; // Profundidad

	//PROPERTIES
	public int t; // Total Score of Node
	public int n; // Times Visited
	public double ucb1; // Value of select criteria

	public Node(Action action) {
		this.action = action;
		parent = null;
		
		children = null;
		
		depth = 0;
		n = 0;
		t = 0;
	}

	public Node(Node parent, Action action) {
		this.action = action;
		this.parent = parent;
		
		children = null;

		depth = parent.depth + 1;
		n = 0;
		t = 0;
	}

	public boolean isLeaf() {
		return children==null || children.isEmpty();
	}
	
	@Override
	public String toString() {
		String result;
		if(action==null) //ROOT
			result= "("+t+"/"+n+") "+depth+" UCB1: "+ ucb1;
		else
			result= "("+t+"/"+n+") "+depth+" UCB1: "+ ucb1+" //		"+action.name() ;
		return result;
	}

}
