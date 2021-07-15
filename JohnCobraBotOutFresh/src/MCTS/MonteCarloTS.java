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

	public static final int PROCESSING_TIME = 160 * 100000;
	public static final int N_FRAMES_SIMULATED = 60;
	public static final int DEPTH_LIMIT = 3;
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
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= PROCESSING_TIME;) {
			executeOneIteration(root);
		}
	}

	private void executeOneIteration(Nodo node) {
		Nodo current=node;
		double score=-9999;
		if(current.isLeaf()) {
			if(current.n==0) {
				score = playout(current);
			} else {
				expansion(current);
				score = playout(current.children.get(0));
			}
			update(current,score);
		}else {
			current=selection(current);
			executeOneIteration(current);
		}
	}	
	
	private Nodo selection(Nodo node) {
		Nodo selectedNode = null;
		double bestUCB1 = -9999;
		for (Nodo child : node.children) {
			calculateUCB1(child);

			if (bestUCB1 < child.ucb1) {
				selectedNode = child;
				bestUCB1 = child.ucb1;
			}
		}

		return selectedNode;
	}
	
	private void expansion(Nodo node) {
		for(Action action : myActions) {
			Nodo newNode= new Nodo(action,fd);
			node.children.add(newNode);
		}
	}
	
	private double playout(Nodo node) {
		//Mis Acciones simuladas
		myActionsSim=new LinkedList<Action>();
		myActionsSim.addAll(node.myActions);
		while(myActionsSim.size()<N_SIM_ACTIONS) {
			myActionsSim.add(myActions.get(random.nextInt(myActions.size())));
		}
		//Acciones simuladas del oponente
		oppActionsSim=new LinkedList<Action>();
		while(oppActionsSim.size()<N_SIM_ACTIONS) {
			oppActionsSim.add(oppActions.get(random.nextInt(oppActions.size())));
		}
		System.out.println(myActionsSim==null);
		System.out.println(oppActionsSim==null);
		System.out.println(fd==null);
		System.out.println(player);


		//Simulación
		FrameData simulated =simulator.simulate(fd, player, myActionsSim , oppActionsSim, N_FRAMES_SIMULATED);
		
		return calculateScoreSimple(simulated);
	}
	
	private void update(Nodo node, Double score) {
		Nodo current=node;
		while(current!=null) {
			current.n++;
			current.t+=score;
			current=current.parent;
		}
	}
	
	public double calculateScoreSimple (FrameData fd) { // ADD MULTIPLOS SITUACIONES FAVORABLES COMO STUN
		
		
		State myState = fd.getCharacter(player).getState();
		State opponentState = fd.getCharacter(!player).getState();
		
		double result = fd.getCharacter(player).getHp() - fd.getCharacter(!player).getHp();
		//PONDERACION PARA PRIORIZAR LA VIDA NANTE LA DISTANCIA
		double pond= fd.getDistanceX()/1000;
		result += result*pond ;
		
		if(myState==State.DOWN)
			result*=0.9;
		if(opponentState==State.DOWN)
			result*=1.1;
		
		return result;	
	}
	
	public double calculateUCB1(Nodo node) {
		double result;
		if (node.n == 0)
			result = 9999 + random.nextInt(50);
		else
			result = node.t / node.n + C * Math.sqrt(Math.log(node.parent.n) / node.n);
		node.ucb1 = result;
		return result;
	}
	
	public Action getBestUCB1Child() {
		// TODO Auto-generated method stub
		return null;
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
