package MCTS;

import java.util.ArrayList;
import java.util.LinkedList;
import enumerate.Action;

public class Nodo {

	//DATA
	public LinkedList<Action> actionList;

	//LINKED
	public Nodo parent;
	public ArrayList<Nodo> children;
	public int depth; 

	//STATS
	public int s;
	public int n; 
	
	//ROOT
	public Nodo() {
		actionList=new LinkedList<Action>();
		parent = null;		
		children = new ArrayList<Nodo>();
		depth = 0;
		n = 0;
		s = 0;
	}
	
	//CHILD
	public Nodo(Nodo parent, Action action) {
		this();
		this.parent=parent;
		actionList.addAll(this.parent.actionList);
		actionList.addLast(action);
		depth=parent.depth+1;
	}

	public boolean isLeaf() {
		return children.isEmpty();
	}
	
	@Override
	public String toString() {
		String result = "("+s+"/"+n+") "+depth+" //		"+actionList.toString() ;
		return result;
	}

}
