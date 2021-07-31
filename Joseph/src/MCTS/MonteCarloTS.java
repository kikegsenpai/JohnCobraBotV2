package MCTS;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;
import struct.FrameData;
import struct.GameData;

public class MonteCarloTS {

	public static final int PROCESSING_TIME = 16500000;
	public static final int N_FRAMES_SIMULATED = 60;
	public static final int DEPTH_LIMIT = 5;
	public static final int N_SIM_ACTIONS = 5;
	public static final double C = Math.sqrt(2);

	public Nodo root;
    public FrameData fd;
	public Simulator simulator;

	public LinkedList<Action> myActions;
	public LinkedList<Action> oppActions;
    
	public Deque<Action> myActionsSim;
	public Deque<Action> oppActionsSim;
	
    
    boolean player;
    GameData gd;


	private Random random;


	public MonteCarloTS(boolean player,GameData gd) {
		this.random=new Random();
		this.player=player;
		this.gd=gd;
		this.simulator=gd.getSimulator();
	}
	
	public void execute() {
		int n=0;
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= PROCESSING_TIME;) {
			executeOneIteration(root);
			n++;
		}
		System.out.println("N: "+n);
	}

	private void executeOneIteration(Nodo node) {
		Nodo current=node;
		double score=-9999;
		if(current.isLeaf()) {
			if(current.parent!=null && current.n==0) {
				score = playout(current);
			} else {
				if(current.depth<DEPTH_LIMIT) {
					expansion(current);
					current=current.children.get(0);
				}
				score = playout(current);
			}
			update(current,score);
		}else {
			current=selection(current);
			executeOneIteration(current);
		}
	}	
	
	private Nodo selection(Nodo node) {
		Nodo selectedNode = null;
		double maximum = -9999;
		double current;
		for (Nodo child : node.children) {
			
			current = selectionUCB1(child);
			current = selectionBestScore(child);
			
			if (maximum < current) {
				selectedNode = child;
				maximum = current;
			}
		}

		return selectedNode;
	}

	private void expansion(Nodo node) {
		for(Action action : myActions) {
			Nodo newNode= new Nodo(node,action);
			node.children.add(newNode);
		}
	}
	
	private double playout(Nodo node) {
		//Mis Acciones simuladas
		myActionsSim=new LinkedList<Action>();
		myActionsSim.addAll(node.actionList);
		while(myActionsSim.size()<N_SIM_ACTIONS) {
			myActionsSim.add(myActions.get(random.nextInt(myActions.size())));
		}
		//Acciones simuladas del oponente
		oppActionsSim=new LinkedList<Action>();
		while(oppActionsSim.size()<N_SIM_ACTIONS) {
			oppActionsSim.add(oppActions.get(random.nextInt(oppActions.size())));
		}
		//Simulación
		FrameData simulated =simulator.simulate(fd, player, myActionsSim , oppActionsSim, N_FRAMES_SIMULATED);
		
		return evaluationPond(simulated);
	}
	
	private void update(Nodo node, Double score) {
		Nodo current=node;
		while(current!=null) {
			current.n++;
			current.s+=score;
			current=current.parent;
		}
	}
	
	//SELECTION POLITICS
	public double selectionBestScore(Nodo node) {
		double result;
		if (node.n == 0)
			result = random.nextInt(10);
		else
			result=node.s;
		return result;
	}
	
		public double selectionUCB1(Nodo node) {
			double result;
			if (node.n == 0)
				result = 9999 + random.nextInt(50);
			else
				result = node.s / node.n + C * Math.sqrt(Math.log(node.parent.n) / node.n);
			return result;
		}
		
		public double selectionBestMean(Nodo node) {
			double result;
			if (node.n == 0)
				result = random.nextInt(10);
			else
				result=node.s/node.n;
			return result;
		}
		
	//EVALUATION POLITICS
		public double evaluationSimple (FrameData fd) { 
			double result = fd.getCharacter(player).getHp() - fd.getCharacter(!player).getHp();
			return result;	
		}
		
		public double evaluationPond (FrameData fd) {
		State myState = fd.getCharacter(player).getState();
		State opponentState = fd.getCharacter(!player).getState();
		double result = fd.getCharacter(player).getHp() - fd.getCharacter(!player).getHp();
		double pond= fd.getDistanceX()/1000;
		result += result*pond ;
		
		if(myState==State.DOWN)
			result*=0.9;
		if(opponentState==State.DOWN)
			result*=1.1;
		
		return result;	
	}
	
	
	//FINAL SELECTIONS
	public Action getMostVisitedChild(boolean print) {
		double best = -999999;
		Action bestAction = Action.BACK_STEP;

		for (int i = 0; i < root.children.size(); i++) {
			if (root.children.get(i).n > best) {
				best = root.children.get(i).n;
				bestAction = root.children.get(i).actionList.getFirst();
			}
		}
		if(print)
			printTree();
		System.out.println("ELECCION: "+bestAction.name());

		return bestAction;
	}

	public Action getBestScoreChild(boolean print) {
		double best = -999999;
		Action bestAction = Action.BACK_STEP;

		for (int i = 0; i < root.children.size(); i++) {
			if (root.children.get(i).s > best) {
				best = root.children.get(i).s;
				bestAction = root.children.get(i).actionList.getFirst();
			}
		}
		if(print)
			printTree();
		System.out.println("ELECCION: "+bestAction.name());
		System.out.println("=============================================================");

		return bestAction;
	}
	
	public void printTree() {
		printNode(root);
	}

	public void printNode(Nodo node) {
		for (int i = 0; i < node.depth; i++)
			System.out.print("    ");
		System.out.println(node);
		if (node.children != null) {
			for (int i = 0; i < node.children.size(); i++)
				printNode(node.children.get(i));
		}
	}

	
}
