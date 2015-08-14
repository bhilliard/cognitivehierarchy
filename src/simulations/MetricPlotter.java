package simulations;

import java.awt.Color;
import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.stochasticgames.SGDomain;

public class MetricPlotter {

	protected ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>(),
			gasL = new ArrayList<GameAnalysis>();
	protected String inDir, outDir;
	protected SGDomain domain = (SGDomain) new GridGame().generateDomain();
	protected int queueSize = 1;

	public MetricPlotter(String inDir, String outDir) {
		this.inDir = inDir;
		if (!this.inDir.endsWith("/"))
			this.inDir += "/";
		this.outDir = outDir;
		getGames();
	}

	public MetricPlotter(String inDir, String outDir, int avgWindow) {
		this(inDir, outDir);
		this.queueSize = avgWindow;
	}
	
	protected void getGames() {

		File[] matchFiles = new File(this.inDir).listFiles();
		StateParser sp = new StateJSONParser(this.domain);

		for (File match : matchFiles) {
			if (match.isDirectory()) {
				for (File trial : match.listFiles()) {
					GameAnalysis ga = GameAnalysis.parseFileIntoGA(this.inDir
							+ match.getName() + "/" + trial.getName(),
							this.domain, sp);
					if (match.getName().contains("earning"))
						this.gasL.add(ga);
					else
						this.gas.add(ga);
				}
			}
		}
	}
	
	public void plotLearningReward(){
		plotReward(gasL, "Learning");
	}
	
	public void plotTrialReward(){
		plotReward(gas, "Trial");
	}

	protected void plotReward(ArrayList<GameAnalysis> gas, String title) {
		XYPlot plot = new XYPlot();

		title = "Reward-"+title;

		XYSeriesCollection dataset0 = new XYSeriesCollection();
		XYSeriesCollection dataset1 = new XYSeriesCollection();

		XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();

		NumberAxis domain = new NumberAxis("x");
		NumberAxis range = new NumberAxis("y");

		XYSeries agent0 = new XYSeries("Agent0");
		XYSeries agent1 = new XYSeries("Agent1");

		double agent0Sum, agent1Sum, agent0Avg, agent1Avg;
		LinkedList<Double> agent0q = new LinkedList<Double>(), agent1q = new LinkedList<Double>();
		for (int i = 0; i < gas.size(); i++) {
			List<Map<String, Double>> rewards = gas.get(i)
					.getJointRewards();
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

		ChartFrame frame = new ChartFrame("", chart);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {

		MetricPlotter plot = new MetricPlotter("../2015_08_12_03_19_52", "");
		plot.plotTrialReward();
		plot.plotLearningReward();

	}
}
