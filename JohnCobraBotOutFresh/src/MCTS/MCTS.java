package MCTS;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

public class MCTS {

	// CONSTANTS
	public static final int N_FRAMES_SIMULATED = 30;
	public static final int DEPTH_LIMIT = 4;
	public static final int N_ITERATIONS = 50;
	public static final int N_RANDOM_ACTIONS = 3;
	public static final double C = Math.sqrt(2);

	// TOOLS
	public Simulator simulator;
	public boolean player;
	public Random random;

	// DATA
	public Node root;

	private LinkedList<Action> myActionPool;
	private LinkedList<Action> enemyActionPool;
	private CharacterData oppOGState;
	private CharacterData playerOGState;
	public FrameData fd; // State
	public GameData gameData; // GameData	

	public MCTS(boolean player, FrameData fd, GameData gameData, LinkedList<Action> myActionPool,
			LinkedList<Action> enemyActionPool) {
		random = new Random();

		this.simulator = gameData.getSimulator();
		this.player = player;
		this.fd = fd;
		this.gameData = gameData;
		this.myActionPool = myActionPool;
		this.enemyActionPool = enemyActionPool;
		
		
		oppOGState = fd.getCharacter(!player);
		playerOGState = fd.getCharacter(player);

	}

	public void expansion(Node node) {
		node.children = new ArrayList<Node>();

		for (int i = 0; i < myActionPool.size(); i++) {
			Node newNode = new Node(node, myActionPool.get(i));
			node.children.add(newNode);
		}

	}

	public Node selection(Node node) {

		Node selectedNode = null;
		double bestUCB1 = -9999;
		for (Node child : node.children) {
			calculateUCB1(child);

			if (bestUCB1 < child.ucb1) {
				selectedNode = child;
				bestUCB1 = child.ucb1;
			}
		}

		return selectedNode;
	}

	public double playout(Node node) {

		LinkedList<Action> myRandomActions = new LinkedList<Action>();
		LinkedList<Action> enemyRandomActions = new LinkedList<Action>();

		myRandomActions.addFirst(node.action);
		
		for (int i = 0; i < N_RANDOM_ACTIONS; i++) {
			enemyRandomActions.add(enemyActionPool.get(random.nextInt(enemyActionPool.size())));
		}

		// SIMULA LA ACCION DEL NODO PARA EL PLAYER Y X ACCIONES DEL ENEMIGO ALEATORIAS
		FrameData result = simulator.simulate(fd, player, myRandomActions, enemyRandomActions, N_FRAMES_SIMULATED);

		return calculateScoreSimple(result);

	}

	public void update(Node node, double score) {
		Node current = node;
		while (current != null) {
			current.n++;
			current.t += score;
			current = current.parent;
		}
	}

	public void executeFull() {
		root = new Node(null);
		expansion(root);

		Node current;
		boolean doneIteration;


		for (int i = 0; i < N_ITERATIONS; i++) {
			
			doneIteration = false;
			current = root;
			
			while (!doneIteration) {
				
				current = selection(current); 										// SELECIONA BEST UCB1
				
				if (current.isLeaf()) { 											// SI ES HOJA -> SE HARÁ UNA SIMULACIÓN
					
					if (current.n == 0 && current.depth < DEPTH_LIMIT) { 			// SI NO HA SIDO VISITADA ANTES -> ADEMÁS SE LE AÑADIRAN LOS HIJOS	
						expansion(current);
						current = selection(current); 								// SELECCIONARÁ UNO PARA LA SIMULACIÓN
					}
					
					double score = playout(current);
					update(current, score); 										// SIMULAMOS Y ACTUALIZAMOS LOS VALORES DE N (VECES VISITADA) Y T
					doneIteration = true; 											// TERMINAMOS LA ITERACIÓN
					
				} 																	// SI NO ES HOJA -> SELECCIONAMOS EL SIGUIENTE
			}		
		}	
		
		//printTree();
	}
	
	public boolean executeOneIteration() {
		
		boolean doneIteration = false;
		Node current = root;
			
		while (!doneIteration) {
				
			current = selection(current); 										// SELECIONA BEST UCB1
				
			if (current.isLeaf()) { 											// SI ES HOJA -> SE HARÁ UNA SIMULACIÓN
					
				if (current.n == 0 && current.depth < DEPTH_LIMIT) { 			// SI NO HA SIDO VISITADA ANTES -> ADEMÁS SE LE AÑADIRAN LOS HIJOS	
					expansion(current);
					current = selection(current); 								// SELECCIONARÁ UNO PARA LA SIMULACIÓN
				}
					
				double score = playout(current);
				update(current, score); 										// SIMULAMOS Y ACTUALIZAMOS LOS VALORES DE N (VECES VISITADA) Y T
				doneIteration = true; 											// TERMINAMOS LA ITERACIÓN
					
			} 																	// SI NO ES HOJA -> SELECCIONAMOS EL SIGUIENTE
		}		
		
		
		//printTree();
		return doneIteration;


	}

	public Action getBestUCB1Child() {
		double best = 0;
		Action bestAction = Action.BACK_STEP;

		for (int i = 0; i < root.children.size(); i++) {
			if (root.children.get(i).n > best) {
				best = root.children.get(i).n;
				bestAction = root.children.get(i).action;
			}
		}
		printTree();
		System.out.println("ELECCION: "+bestAction.name());
		System.out.println("=============================================================");

		return bestAction;
	}

	public Action getMostVisitedChild() {
		double mostVisited = 0;
		Action mostVisitedAction = Action.BACK_STEP;

		for (int i = 0; i < root.children.size(); i++) {
			if (root.children.get(i).n > mostVisited) {
				mostVisited = root.children.get(i).n;
				mostVisitedAction = root.children.get(i).action;
			}
		}
		printTree();
		System.out.println("ELECCION: "+mostVisitedAction.name());
		System.out.println("=============================================================");

		return mostVisitedAction;
	}
	public double calculateScoreSimple (FrameData fd) { // ADD MULTIPLOS SITUACIONES FAVORABLES COMO STUN
		
		
		State myState = fd.getCharacter(player).getState();
		State opponentState = fd.getCharacter(!player).getState();
		
		double result = fd.getCharacter(player).getHp() - fd.getCharacter(!player).getHp();
		//PONDERACION PARA PRIORIZAR LA VIDA ANTE LA DISTANCIA
		double pond= fd.getDistanceX()/1000;
		result += result*pond ;
		
		if(myState==State.DOWN)
			result*=0.9;
		if(opponentState==State.DOWN)
			result*=1.1;
		if(result==0)
			result += 30 * (1/pond);
		
		return result;
		
		
	}
	/*
	public double calculateScore(FrameData fd) { // ADD MULTIPLOS SITUACIONES FAVORABLES COMO STUN
		
		double myHp = fd.getCharacter(player).getHp();
		double opponentHp = fd.getCharacter(!player).getHp();
		
		
		double myDiffHp = myHp - playerOGState.getHp() ;
		double opponentDiffHp = opponentHp - oppOGState.getHp() ;
		
		int combo = fd.getCharacter(player).getHitCount();
		boolean hits = fd.getCharacter(player).isHitConfirm();
		
		State myState = fd.getCharacter(player).getState();
		State opponentState = fd.getCharacter(!player).getState();
		
		double result = (myDiffHp-opponentDiffHp)*10 + combo*5;

		if(hits)
			result+=10;
		if(myState==State.DOWN)
			result-=50;
		if(opponentState==State.DOWN)
			result+=50;
		
		return result;
		
	}
	*/
	public double calculateUCB1(Node node) {
		double result;
		if (node.n == 0)
			result = 9999 + random.nextInt(30);
		else
			result = node.t / node.n + C * Math.sqrt(Math.log(node.parent.n) / node.n);
		node.ucb1 = result;
		return result;
	}

	public void printTree() {
		printNode(root);
	}

	public void printNode(Node node) {
		for (int i = 0; i < node.depth; i++)
			System.out.print("    ");
		System.out.println(node);
		if (node.children != null) {
			for (int i = 0; i < node.children.size(); i++)
				printNode(node.children.get(i));
		}
	}

}
