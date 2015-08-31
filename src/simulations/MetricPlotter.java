package simulations;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.stochasticgames.SGDomain;

public class MetricPlotter {

	protected TreeMap<Integer, GameAnalysis> gas = new TreeMap<Integer, GameAnalysis>(),
			gasL = new TreeMap<Integer, GameAnalysis>();
	protected String inDir, outDir;
	protected SGDomain domain = (SGDomain) new GridGame().generateDomain();
	protected int queueSize = 1;

	public MetricPlotter(String inDir, String outDir) {
		this.inDir = inDir;
		if (!this.inDir.endsWith("/"))
			this.inDir += "/";
		this.outDir = outDir;
	}

	public MetricPlotter(String inDir, String outDir, int avgWindow) {
		this(inDir, outDir);
		this.queueSize = avgWindow;
	}

	public void plotTauExperiment() {

		Map<Set<Integer>, Map<Double, Double>> tauData = getTauData();
		XYPlot plot = new XYPlot();

		String title = "Cooperation vs Tau";

		NumberAxis domain = new NumberAxis("Tau");
		NumberAxis range = new NumberAxis("Percantage Cooperative Games (%)");
		plot.setDomainAxis(0, domain);
		plot.setRangeAxis(0, range);

		int counter = 0;
		for (Set<Integer> set : tauData.keySet()) {
			XYSeries series = new XYSeries(set.toString());
			for (Double tau : tauData.get(set).keySet()) {
				series.add(tau, tauData.get(set).get(tau));
			}
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(series);

			plot.setDataset(counter, dataset);

			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			plot.setRenderer(counter, renderer);
			renderer.setSeriesPaint(counter, Color.CYAN);
			renderer.setBaseShapesVisible(true);

			plot.mapDatasetToDomainAxis(counter, 0);
			plot.mapDatasetToRangeAxis(counter, 0);

			counter++;
		}

		showPlot(plot, title);

	}

	public Map<Set<Integer>, Map<Double, Double>> getTauData() {
		Map<Set<Integer>, Map<Double, Double>> data = new HashMap<Set<Integer>, Map<Double, Double>>();
		try {
			for (File expFile : new File(this.inDir).listFiles()) {
				if (expFile.isDirectory()) {

					// Get tau form meta.txt
					String metaFile = expFile.getAbsolutePath() + "/meta.txt";
					BufferedReader reader;
					reader = new BufferedReader(new FileReader(metaFile));
					String line = reader.readLine();
					while (!line.contains("Tau"))
						line = reader.readLine();
					reader.close();
					Double tau = Double.valueOf(line.split(": ")[1]);
					double percent;
					for (File match : expFile.listFiles()) {
						if (match.isDirectory()) {
							Integer agent0 = Integer.valueOf(match.getName()
									.split("_")[3]);
							Integer agent1 = Integer.valueOf(match.getName()
									.split("_")[1]);
							Set<Integer> playerSet = new HashSet<Integer>();
							playerSet.add(agent0);
							playerSet.add(agent1);

							percent = getPercentCooperate(match);

							if (data.containsKey(playerSet)) {
								Map<Double, Double> M = data.get(playerSet);
								if (M.containsKey(tau)) {
									M.put(tau, (M.get(tau) + percent) / 2);
								} else {
									M.put(tau, percent);
								}
							} else {
								Map<Double, Double> M = new HashMap<Double, Double>();
								M.put(tau, percent);
								data.put(playerSet, M);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return data;
	}

	private double getPercentCooperate(File match) {
		int coopCount = 0, totalCount = 0;
		StateParser sp = new StateJSONParser(this.domain);

		for (File trial : match.listFiles()) {
			if (trial.isFile()) {
				GameAnalysis ga = GameAnalysis.parseFileIntoGA(
						trial.getAbsolutePath(), this.domain, sp);
				boolean isCooperative = true;
				for (double reward : ga.getJointReward(ga.numTimeSteps() - 1)
						.values()) {
					if (reward <= 0)
						isCooperative = false;
				}
				if (isCooperative)
					coopCount++;
				totalCount++;
			}
		}

		return (coopCount * 100.0) / totalCount;
	}

	protected void getGames() {

		File[] matchFiles = new File(this.inDir).listFiles();
		StateParser sp = new StateJSONParser(this.domain);
		int index;
		for (File match : matchFiles) {
			if (match.isDirectory()) {
				for (File trial : match.listFiles()) {
					GameAnalysis ga = GameAnalysis.parseFileIntoGA(this.inDir
							+ match.getName() + "/" + trial.getName(),
							this.domain, sp);
					String[] split = trial.getName().split("_");
					index = Integer.valueOf(split[split.length - 1].split("\\.")[0]);
					if (match.getName().contains("earning"))
						this.gasL.put(index, ga);
					else
						this.gas.put(index, ga);
				}
			}
		}
	}

	public void plotLearningReward() {
		getGames();
		plotReward(gasL, "Learning");
	}

	public void plotTrialReward() {
		getGames();
		plotReward(gas, "Trial");
	}

	protected void plotReward(TreeMap<Integer, GameAnalysis> gas, String title) {
		XYPlot plot = new XYPlot();

		title = "Reward-" + title;

		XYSeriesCollection dataset0 = new XYSeriesCollection();
		XYSeriesCollection dataset1 = new XYSeriesCollection();

		XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();

		NumberAxis domain = new NumberAxis("Learning Episode");
		NumberAxis range = new NumberAxis("Average Reward");

		XYSeries agent0 = new XYSeries("Agent0");
		XYSeries agent1 = new XYSeries("Agent1");

		double agent0Sum, agent1Sum, agent0Avg, agent1Avg;
		LinkedList<Double> agent0q = new LinkedList<Double>(), agent1q = new LinkedList<Double>();
		for (int i = 0; i < gas.size(); i++) {
			List<Map<String, Double>> rewards = gas.get(i).getJointRewards();
			agent0Sum = 0;
			agent1Sum = 0;

			for (Map<String, Double> reward : rewards) {
				agent0Sum += reward.get("agent0");
				agent1Sum += reward.get("agent1");
			}

			if (agent0q.size() == this.queueSize) {
				agent1q.removeFirst();
				agent0q.removeFirst();
			}

			agent0q.addLast(agent0Sum);
			agent1q.addLast(agent1Sum);

			if (agent0q.size() == queueSize) {
				agent0Avg = 0;
				agent1Avg = 0;
				for (int j = 0; j < queueSize; j++) {
					agent0Avg += agent0q.get(j);
					agent1Avg += agent1q.get(j);
				}
				agent0Avg /= queueSize;
				agent1Avg /= queueSize;
				agent0.add(i, agent0Avg);
				agent1.add(i, agent1Avg);
			}
		}

		dataset0.addSeries(agent0);
		dataset1.addSeries(agent1);

		plot.setDataset(0, dataset0);
		plot.setDataset(1, dataset1);

		plot.setRenderer(0, renderer0);
		plot.setRenderer(1, renderer1);

		renderer0.setSeriesPaint(0, Color.GREEN);
		renderer1.setSeriesPaint(0, Color.BLUE);
		renderer0.setBaseShapesVisible(false);
		renderer1.setBaseShapesVisible(false);

		plot.setDomainAxis(0, domain);
		plot.setRangeAxis(0, range);

		plot.mapDatasetToDomainAxis(0, 0);
		plot.mapDatasetToDomainAxis(1, 0);

		plot.mapDatasetToRangeAxis(0, 0);
		plot.mapDatasetToRangeAxis(1, 0);

		showPlot(plot, title);
	}

	public void plotNumSteps() {
		XYPlot plot = new XYPlot();

		String title = "Average Number of Steps in Game";

		XYSeriesCollection dataset0 = new XYSeriesCollection();

		XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();

		NumberAxis domain = new NumberAxis("x");
		NumberAxis range = new NumberAxis("y");

		XYSeries steps = new XYSeries("Num Steps");

		double avgNumTimeSteps;
		double numTimeSteps;
		LinkedList<Double> q = new LinkedList<Double>();
		for (int i = 0; i < gas.size(); i++) {

			numTimeSteps = this.gas.get(i).numTimeSteps();

			if (q.size() == this.queueSize) {
				q.removeFirst();
			}

			q.addLast(numTimeSteps);

			if (q.size() == queueSize) {
				avgNumTimeSteps = 0;
				for (int j = 0; j < queueSize; j++) {
					avgNumTimeSteps += q.get(j);
				}
				avgNumTimeSteps /= queueSize;

				steps.add(i, avgNumTimeSteps);

			}
		}

		dataset0.addSeries(steps);

		plot.setDataset(0, dataset0);

		plot.setRenderer(0, renderer0);

		renderer0.setSeriesPaint(0, Color.CYAN);

		renderer0.setBaseShapesVisible(false);

		plot.setDomainAxis(0, domain);
		plot.setRangeAxis(0, range);

		plot.mapDatasetToDomainAxis(0, 0);
		plot.mapDatasetToDomainAxis(1, 0);

		plot.mapDatasetToRangeAxis(0, 0);
		plot.mapDatasetToRangeAxis(1, 0);

		showPlot(plot, title);
	}

	public void showPlot(XYPlot plot, String title) {

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
				plot, true);
		saveToFile(chart, title);
		ChartFrame frame = new ChartFrame(inDir, chart);
		frame.pack();
		frame.setVisible(true);

	}
	
	public void saveToFile(JFreeChart chart, String fileName){
		saveToFile(chart, fileName, 700, 500);
	}
	
	public void saveToFile(JFreeChart chart, String fileName, int width, int height){
		try {
			File outFile = new File(inDir+fileName+".jpeg");
			ChartUtilities.saveChartAsJPEG(outFile, chart, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		};
	}

	protected void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public static void main(String[] args) {

		MetricPlotter plot = new MetricPlotter(
				"../2015_08_31_16_14_11/", "",150);
		plot.plotLearningReward();
		plot.plotTrialReward();
		
		// MetricPlotter plot = new MetricPlotter(
		// "../2015_08_18_12_04_28", "");
//		plot.plotTauExperiment();
	}
}
