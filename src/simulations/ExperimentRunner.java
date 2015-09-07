package simulations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import burlap.behavior.singleagent.Policy;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;

public class ExperimentRunner {
	// Agent parameters
	// Map from agent name (specific to location in game) to level to policy
	// solved for
	private int kLevel;
	boolean runValueIteration, runStochasticPolicyPlanner;
	Map<String, RewardCalculatorType> rewardCalcTypeMap;
	double cooperativeParameter, defensiveParameter;

	// Game parameters
	private String gameFile, outDir;
	private double stepCost, noopCost, reward, tau;
	private boolean incurCostOnNoop = true;
	private boolean noopAllowed;

	// Experiment parameters
	private int numTrials, numLearningEpisodes;
	private boolean optimisticInit, boltzmannExplore, saveLearning;
	private double temp;
	private double optimisticValue;
	
	private final double DISCOUNT_FACTOR = 0.99, LEARNING_RATE = 0.01;
	private final int TIMEOUT = 100;
	private double tauMin;
	private double tauMax;
	private double tauStep;
	

	public enum Level0Type {
		RANDOM, Q, NASH_CD, NASH_B
	}

	public ExperimentRunner(String configFile) {
		config(configFile);
	};

	public enum RewardCalculatorType {
		SELFISH, OTHER_REGARDING, OTHER_REGARDING_NINE	
	};

	public void runExperiment() {

		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		this.outDir = "../Experiments_" + ft.format(date) + "/";
		File dirFile = new File(outDir);
		dirFile.mkdir();

		for (double tau = this.tauMin; tau <= this.tauMax; tau += this.tauStep) {
			Experiment exp = new Experiment(this.gameFile, this.kLevel,
					this.stepCost, this.incurCostOnNoop, this.noopCost,
					this.reward, tau, this.runValueIteration,
					this.runStochasticPolicyPlanner, this.numTrials,
					this.noopAllowed,true, this.rewardCalcTypeMap,
					this.cooperativeParameter, this.defensiveParameter, this.outDir);
			exp.runKLevelExperiment(Level0Type.RANDOM);
		}
	}

	private void config(String configFile) {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(configFile);

			// load a properties file
			prop.load(input);
			this.kLevel = Integer.valueOf(prop.getProperty("kLevel"));
			this.runValueIteration = Boolean.parseBoolean(prop
					.getProperty("runValueIteration"));
			this.runStochasticPolicyPlanner = Boolean.parseBoolean(prop
					.getProperty("runStochasticPolicyPlanner"));
			this.gameFile = prop.getProperty("gameFile");
			this.outDir = prop.getProperty("outFile");
			this.stepCost = Double.valueOf(prop.getProperty("stepCost"));
			this.noopCost = Double.valueOf(prop.getProperty("noopCost"));
			this.reward = Double.valueOf(prop.getProperty("reward"));
			this.tau = Double.valueOf(prop.getProperty("tau"));
			this.tauMin = Double.valueOf(prop.getProperty("tauMin"));
			this.tauMax = Double.valueOf(prop.getProperty("tauMax"));
			this.tauStep = Double.valueOf(prop.getProperty("tauStep"));
			this.incurCostOnNoop = Boolean.parseBoolean(prop
					.getProperty("incurCostOnNoop"));
			this.noopAllowed = Boolean.parseBoolean(prop
					.getProperty("noopAllowed"));
			this.numTrials = Integer.valueOf(prop.getProperty("numTrials"));
			this.numLearningEpisodes = Integer.valueOf(prop
					.getProperty("numLearningEpisodes"));
			this.optimisticInit = Boolean.parseBoolean(prop
					.getProperty("optimisticInit"));
			this.boltzmannExplore = Boolean.parseBoolean(prop
					.getProperty("boltzmannExplore"));
			this.temp = Double.valueOf(prop.getProperty("temp"));
			this.saveLearning = Boolean.parseBoolean(prop
					.getProperty("saveLearning"));
			String rewardCalcTypeString = prop.getProperty("rewardCalc");
			if(rewardCalcTypeString.equals("selfish")){
				this.rewardCalcTypeMap.put("agent0", RewardCalculatorType.SELFISH);
				this.rewardCalcTypeMap.put("agent1", RewardCalculatorType.SELFISH);
			}else if (rewardCalcTypeString.equals("other_regarding")){
				this.rewardCalcTypeMap.put("agent0", RewardCalculatorType.OTHER_REGARDING);
				this.rewardCalcTypeMap.put("agent1", RewardCalculatorType.OTHER_REGARDING);
			}
			this.cooperativeParameter = Double.valueOf(prop.getProperty("cooperativeParameter"));
			this.defensiveParameter = Double.valueOf(prop.getProperty("defensiveParameter"));

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();

		ExperimentRunner expRun = new ExperimentRunner("./TauConfig.properties");
		expRun.runExperiment();

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total time:  " + totalTime / 1000.0 + " s");
	}
}
