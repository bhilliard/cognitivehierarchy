package simulations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.GameSequenceVisualizer;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.visualizer.Visualizer;

public class ExperimentVisualizer extends GameSequenceVisualizer {

	String dirName;

	public ExperimentVisualizer(Visualizer vis, SGDomain domain, String dirName) {
		super(vis, domain, getGames(domain, dirName));
		this.dirName = dirName;
		setEpisodeNames();
		this.initGUI();
	}

	protected static ArrayList<GameAnalysis> getGames(SGDomain domain,
			String dirName) {

		File[] matchFiles = new File(dirName).listFiles();
		StateParser sp = new StateJSONParser(domain);
		ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>();

		for (File match : matchFiles) {
			if (match.isDirectory()) {
				for (File trial : match.listFiles()) {
					GameAnalysis ga = GameAnalysis.parseFileIntoGA(dirName
							+ match.getName() + "/" + trial.getName(), domain,
							sp);
					gas.add(ga);
				}
			}
		}
		return gas;
	}

	@Override
	public void initWithDirectGames(Visualizer v, SGDomain d,
			List<GameAnalysis> games, int w, int h) {

		painter = v;
		domain = d;

		this.directGames = games;

		cWidth = w;
		cHeight = h;
	}

	public void setEpisodeNames() {

		File[] matchFiles = new File(this.dirName).listFiles();

		this.episodesListModel = new DefaultListModel();

		for (File match : matchFiles) {
			if (match.isDirectory()) {
				for (File trial : match.listFiles()) {
					episodesListModel.addElement(trial.getName());
				}
			}
		}
	}

	public static void main(String[] args) {
		String dirName = "../2015_08_06_02_25_43";
		if (!dirName.endsWith("/"))
			dirName += "/";
		new ExperimentVisualizer(GGVisualizer.getVisualizer(6, 6),
				(SGDomain) new GridGame().generateDomain(), dirName);
	}
}
