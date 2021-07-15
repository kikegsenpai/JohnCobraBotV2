import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import struct.AttackData;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.MotionData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import MCTS.MonteCarloTS;
import MCTS.Nodo;
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;

/**
 * @author Enrique Garrido Sánchez
 **/
public class JoestarBot implements AIInterface {

	/*
	 * 
	 * TO DO: SET ENEMY ACTION POOL EFICIENTLY ESCAPE CORNERS VIGILAR BOLA DE FUEGO
	 * LEVANTARSE SIN DAÑO ESQUINA CROUCH_KICK MEDIR DISTANCIAS MEJOR TIME LIMIT TO
	 * EXECUTION VV
	 * 
	 */

	// PARÁMETROS DE LANZAMIENTO
	// -n 10 --c1 ZEN --c2 ZEN --a1 JohnCobraBot --a2 SimpleAI --fastmode --mute
	// -n 10 --c1 ZEN --c2 ZEN --a1 JohnCobraBot --a2 SimpleAI --fastmode --mute
	// --disable-window --grey-bg --inverted-player 1

	// ALGORITMO
	MonteCarloTS mcts;

	// VARIABLES
	private Key inputKey;
	private boolean player;
	private FrameData fd;
	private CommandCenter cc;
	private GameData gd;
	private Simulator sim;
	private FrameData simFd;

	LinkedList<Action> myActs; // Action waiting to be executed

	private CharacterData myCharacterData;
	private CharacterData opponentData;
	private ArrayList<MotionData> myCharacterMotion;
	private ArrayList<MotionData> opponentMotion;

	File dir;
	File file;

	private LinkedList<Action> ground;

	private LinkedList<Action> air;

	// MÉTODOS
	@Override
	public void close() {
	}

	@Override
	public void getInformation(FrameData fd, boolean isControl) {
		this.fd = fd;
		this.cc.setFrameData(this.fd, this.player);
		myCharacterData = fd.getCharacter(player);
		opponentData = fd.getCharacter(!player);
	}

	@Override
	public int initialize(GameData gd, boolean player) {

		this.gd = gd;
		sim = gd.getSimulator();
		this.player = player;
		this.inputKey = new Key();
		this.cc = new CommandCenter();
		this.fd = new FrameData();

		air = new LinkedList<Action>();
		ground = new LinkedList<Action>();
		orderMoves();
		mcts = new MonteCarloTS(player, gd);

		myCharacterMotion = gd.getMotionData(player);
		opponentMotion = gd.getMotionData(!player);

		initWriter();

		return 0;
	}

	@Override
	public Key input() {
		return this.inputKey;
	}

	@Override
	public void processing() { // DISTANCIA ATAQUE CLOSE 100 MAX 600-700

		if (canProcess()) {
			if (cc.getSkillFlag()) {
				inputKey = cc.getSkillKey();
			} else {
				inputKey.empty();
				cc.skillCancel();

				preparation();

				mcts.execute();
				cc.commandCall(mcts.getBestUCB1Child(false).name());

			}
		}

	}

	private void preparation() {
		simFd = this.sim.simulate(fd, player, (Deque) null, (Deque) null, 14);
		mcts.fd = simFd;
		mcts.root = new Nodo(simFd);
		mcts.oppActions = selectEnemyMoves();
		mcts.myActions = selectMyMovesEspecific();

	}

	public LinkedList<Action> selectMyMovesEspecific() {

		LinkedList<Action> selected = new LinkedList<Action>();

		State myState = myCharacterData.getState();
		State enemyState = opponentData.getState();
		int myEnergy = myCharacterData.getEnergy();

		int x = simFd.getDistanceX();
		Deque<AttackData> projectiles;
		if (player)
			projectiles = simFd.getProjectilesByP2();
		else
			projectiles = simFd.getProjectilesByP1();

		boolean bolazo = false;
		for (AttackData atk : projectiles) {
			if (atk.getHitDamage() > 100) {
				bolazo = true;
			}
		}
		if (bolazo) {

			selected.add(Action.FOR_JUMP);
			selected.add(Action.BACK_JUMP);

		} else if ((simFd.getCharacter(player).getHp() - simFd.getCharacter(!player).getHp()) > 150) {

			if (x > 200) {
				selected.add(Action.STAND_GUARD);
				selected.add(Action.CROUCH_GUARD);
			} else {
				selected.add(Action.FOR_JUMP);
				selected.add(Action.BACK_JUMP);
			}

		} else if (myState == State.STAND && myEnergy >= 150) { // Bolazo de fuego

			selected.add(Action.STAND_D_DF_FC);

		} else if (myCharacterData.getCenterX() < 50) { // Esquina izquierda
			if (!myCharacterData.isFront()) { // ABUSAR

				selected.add(Action.CROUCH_B);
				selected.add(Action.CROUCH_FB);
				if (myEnergy > 55)
					selected.add(Action.STAND_F_D_DFB);
				else
					selected.add(Action.STAND_A);
				selected.add(Action.JUMP);

			} else { // ESCAPAR
				selected.add(Action.FOR_JUMP);
				selected.add(Action.STAND_GUARD);
				selected.add(Action.CROUCH_GUARD);
			}
		} else if (myCharacterData.getCenterX() > 910) { // Esquina derecha

			if (myCharacterData.isFront()) { // ABUSAR

				selected.add(Action.CROUCH_B);
				selected.add(Action.CROUCH_FB);
				if (myEnergy > 55)
					selected.add(Action.STAND_F_D_DFB);
				else
					selected.add(Action.STAND_A);
				selected.add(Action.JUMP);

			} else { // ESCAPAR
				selected.add(Action.FOR_JUMP);
				selected.add(Action.STAND_GUARD);
				selected.add(Action.CROUCH_GUARD);
			}

		} else if (myState == State.AIR && projectiles.isEmpty()) { // En el aire

			if (myEnergy > 50 && x > 300) {
				selected.add(Action.AIR_D_DB_BB);
				selected.add(Action.AIR_D_DF_FB);
			}
			selected.add(Action.AIR_FB);
			selected.add(Action.AIR_DB);
			selected.add(Action.AIR_DA);

		} else if (x < 500 && x > 450 && myState == State.STAND && enemyState == State.STAND) { // Distancia Inicial,
																								// ambos en el suelo

			selected.add(Action.STAND_FB);
			selected.add(Action.CROUCH_FB);
			selected.add(Action.FOR_JUMP);
			if (myEnergy > 55)
				selected.add(Action.STAND_F_D_DFB);
			else
				selected.add(Action.STAND_F_D_DFA);

		} else if (x < 100) { // Close Combat
			selected.add(Action.CROUCH_A);
			selected.add(Action.THROW_A);
			selected.add(Action.STAND_B);
			selected.add(Action.BACK_JUMP);
		} else if (x > 600) { // Full Screen
			selected.add(Action.DASH);
			selected.add(Action.FOR_JUMP);
			selected.add(Action.STAND_D_DB_BA);
		} else {

			selected.add(Action.FOR_JUMP);

		}
		return selected;

	}

	public LinkedList<Action> selectEnemyMoves() {
		LinkedList<Action> selected = new LinkedList<Action>();

		State enemyState = opponentData.getState();
		int enemyEnergy = opponentData.getEnergy();

		if (enemyState == State.AIR) {

			for (int i = 0; i < air.size(); i++) {
				if (Math.abs(opponentMotion.get(Action.valueOf(air.get(i).name()).ordinal())
						.getAttackStartAddEnergy()) <= enemyEnergy) {
					selected.add(air.get(i));
				}
			}
		} else {

			for (int i = 0; i < ground.size(); i++) {
				if (Math.abs(opponentMotion.get(Action.valueOf(ground.get(i).name()).ordinal())
						.getAttackStartAddEnergy()) <= enemyEnergy) {
					selected.add(ground.get(i));
				}
			}
		}

		return selected;

	}

	public LinkedList<Action> selectMyMoves() {
		LinkedList<Action> selected = new LinkedList<Action>();

		State state = myCharacterData.getState();
		int energy = myCharacterData.getEnergy();

		if (state == State.AIR) {

			for (int i = 0; i < air.size(); i++) {
				if (Math.abs(myCharacterMotion.get(Action.valueOf(air.get(i).name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					selected.add(air.get(i));
				}
			}
		} else {

			for (int i = 0; i < ground.size(); i++) {
				if (Math.abs(myCharacterMotion.get(Action.valueOf(ground.get(i).name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					selected.add(ground.get(i));
				}
			}
		}

		return selected;

	}

	public void orderMoves() {

		air.add(Action.AIR_A);
		air.add(Action.AIR_B);
		air.add(Action.AIR_DA);
		air.add(Action.AIR_DB);
		air.add(Action.AIR_FA);
		air.add(Action.AIR_FB);
		air.add(Action.AIR_UA);
		air.add(Action.AIR_UB);

		air.add(Action.AIR_D_DF_FA); // -5

		air.add(Action.AIR_F_D_DFA); // Puñetazo ligero con inercia hacia delante (-10)
		air.add(Action.AIR_D_DB_BA); // Puñetazo pesado con inercia hacia delante (-10)

		air.add(Action.AIR_D_DF_FB); // -20

		air.add(Action.AIR_F_D_DFB); // Patada ligera con inercia hacia delante (-40)

		air.add(Action.AIR_D_DB_BB); // Patada pesada con inercia hacia delante (-50)

		ground.add(Action.FORWARD_WALK);
		ground.add(Action.DASH);
		ground.add(Action.BACK_STEP);
		ground.add(Action.CROUCH);
		ground.add(Action.JUMP);
		ground.add(Action.FOR_JUMP);
		ground.add(Action.BACK_JUMP);
		ground.add(Action.STAND_GUARD);
		ground.add(Action.CROUCH_GUARD);
		ground.add(Action.STAND_A);
		ground.add(Action.STAND_B);
		ground.add(Action.CROUCH_A);
		ground.add(Action.CROUCH_B);
		ground.add(Action.STAND_FA);
		ground.add(Action.STAND_FB);
		ground.add(Action.CROUCH_FA);
		ground.add(Action.CROUCH_FB);
		ground.add(Action.CROUCH_FA);
		ground.add(Action.STAND_F_D_DFA);
		ground.add(Action.STAND_D_DB_BA);

		ground.add(Action.THROW_A);
		ground.add(Action.STAND_D_DF_FA);

		ground.add(Action.THROW_B);

		ground.add(Action.STAND_D_DF_FB);

		ground.add(Action.STAND_D_DB_BB);

		ground.add(Action.STAND_F_D_DFB);

		ground.add(Action.STAND_D_DF_FC);

	}

	public boolean canProcess() {
		return !fd.getEmptyFlag() && fd.getRemainingFramesNumber() > 0;
	}

	@Override
	public void roundEnd(int p1Hp, int p2Hp, int frames) {
		String data = "\n" + "iswin: " + (myCharacterData.getHp() > opponentData.getHp()) + "	|| HP1:" + p1Hp
				+ "	|| HP2:" + p2Hp + "	|| DIFF:" + Math.abs(Math.abs(p1Hp) - Math.abs(p2Hp)) + "	|| P1:"
				+ gd.getAiName(player) + "	|| P2:" + gd.getAiName(!player);
		writeData(file.getPath(), data);
	}

	// --------------------RECORD LOGS----------------------------

	private void writeData(String filePath, String data) {
		try {
			final FileWriter writer = new FileWriter(filePath, true);
			final BufferedWriter buffer = new BufferedWriter(writer);
			buffer.write(data);
			buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initWriter() {
		dir = new File("C:/Users/Kike/git/JohnCobraBotV2/JohnCobraBot/statistics");
		String currentDate = java.time.LocalDate.now().toString();
		file = new File("C:/Users/Kike/git/JohnCobraBotV2/JohnCobraBot/statistics/" + currentDate + ".txt");

		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}
