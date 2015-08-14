package simulations;

import java.util.Map;

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

	// Game parameters
	private String gameFile, outFile;
	private double stepCost, noopCost, reward, tau;
	private boolean incurCostOnNoop = true;
	private boolean noopAllowed;

	// Experiment parameters
	private int numTrials, numLearningEpisodes;
	private boolean optimisticInit, boltzmannExplore, saveLearning;
	private double temp;

	private final double DISCOUNT_FACTOR = 0.99, LEARNING_RATE = 0.01;
	private final int TIMEOUT = 100;

	public enum Level0Type {
		RANDOM, Q, NASH_CD, NASH_B
	}

	public ExperimentRunner(String configFile){
		
	}
	
	private void config(String configFile){
		
	}
}
