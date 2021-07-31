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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import enumerate.Action;
import enumerate.State;
import simulator.Simulator;

/**
 * @author Enrique Garrido Sánchez
 **/
public class BrandoBot implements AIInterface {

	// PARÁMETROS DE LANZAMIENTO
	// -n 10 --c1 ZEN --c2 ZEN --a1 BrandoBot --a2 SimpleAI --fastmode --mute
	// -n 10 --c1 ZEN --c2 ZEN --a1 BrandoBot --a2 SimpleAI --fastmode --mute
	// --disable-window --grey-bg --inverted-player 1

	
	private Key inputKey;
	private boolean player;
	private FrameData fd;
	private CommandCenter cc;
	private GameData gd;
	private Simulator sim;
	private CharacterData myCharacterData;
	private CharacterData opponentData;
	private ArrayList<MotionData> myCharacterMotion;
	private ArrayList<MotionData> opponentMotion;

	File dir;
	File file;

	

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

				

			}
		}

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
		dir = new File("C:/Users/Kike/Desktop/Logs/BrandoBot");
		String currentDate = java.time.LocalDate.now().toString();
		file = new File("C:/Users/Kike/Desktop/Logs/BrandoBot/" + currentDate + ".txt");

		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}
