package simulations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import networking.common.GridGameWorldLoader;
import simulations.ExperimentRunner.Level0Type;
import simulations.ExperimentRunner.RewardCalculatorType;
import behavior.SpecifyNoopCostRewardFunction;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.ValueFunctionInitialization.ConstantValueFunctionInitialization;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.CachedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.agents.BRDPlanThenCombinePoliciesAgent;
import burlap.behavior.stochasticgame.agents.BestResponseToDistributionAgent;
import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.behavior.stochasticgame.agents.RewardCalculatingJointRewardFunction;
import burlap.behavior.stochasticgame.agents.SetStrategyAgent;
import burlap.behavior.stochasticgame.agents.TransparentSetStrategyAgent;
import burlap.behavior.stochasticgame.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.behavior.stochasticgame.mavaluefunction.SGBackupOperator;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.CoCoQ;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.CorrelatedQ;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.MaxQ;
import burlap.behavior.stochasticgame.mavaluefunction.backupOperators.MinMaxQ;
import burlap.behavior.stochasticgame.saconversion.OtherRegardingRewardCalculator;
import burlap.behavior.stochasticgame.saconversion.RandomSingleAgentPolicy;
import burlap.behavior.stochasticgame.saconversion.RewardCalculator;
import burlap.behavior.stochasticgame.saconversion.SGStateReachability;
import burlap.behavior.stochasticgame.saconversion.SelfishRewardCalculator;
import burlap.behavior.stochasticgame.saconversion.SingleToMultiPolicy;
import burlap.behavior.stochasticgame.solvers.CorrelatedEquilibriumSolver.CorrelatedEquilibriumObjective;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.RandomStartStateGenerator;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.SingleAction;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.stochasticgames.explorers.SGVisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

/***
 * ExperimentRunner handles learning policies of agents from level-0 to
 * specified level k. The policies are created in order allowing the next level
 * to learn.
 * 
 * @author betsy hilliard betsy@cs.brown.edu, carl trimbach 
 *
 */
public class Experiment {

	// Agent parameters
	// Map from agent name (specific to location in game) to level to policy
	// solved for
	private Map<String, Map<Integer, Policy>> solvedAgentPolicies;
	private int kLevel;
	boolean runValueIteration, runStochasticPolicyPlanner;
	private boolean runWithRandomStartStates = false;
	private double optimisticValue;
	private Policy agent1Policy = null;
	private Policy agent0Policy = null;
	private RewardCalculator rewardCalc;
	private Map<String, RewardCalculator> rewardCalcMap;

	// Game parameters
	private GridGame gridGame;
	private String gameFile, outFile;
	private SGDomain domain;
	private double stepCost, noopCost, reward, tau;
	private boolean incurCostOnNoop = true;
	private boolean noopAllowed;
	private World gameWorld = null;

	// Experiment parameters
	private int numTrials, numLearningEpisodes;
	private double[][][] scores;
	private boolean optimisticInit, boltzmannExplore, saveLearning;
	private double temp;
	private String outDirRoot = "../";
	private String experimentName = "";
	private String bashLoopIndex = null;

	private String metaText = "";
	private String csvText = "";

	private final double DISCOUNT_FACTOR = 0.99, LEARNING_RATE = 0.01;
	private final int TIMEOUT = 100;

	private String[][][][] convergence;

	/**
	 * Basic constructor
	 * 
	 * @param gameFile
	 * @param kLevel
	 * @param stepCost
	 * @param incurCostOnNoop
	 * @param noopCost
	 * @param reward
	 * @param tau
	 * @param runValueIteration
	 * @param runStochasticPolicyPlanner
	 * @param numTrials
	 * @param noopAllowed
	 */
	public Experiment(String gameFile, String experimentName, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, boolean saveLearning, boolean runWithRandomStartStates) {
		this.runWithRandomStartStates = runWithRandomStartStates;
		this.experimentName  = experimentName;
		this.gameFile = gameFile;
		this.noopAllowed = noopAllowed;
		this.saveLearning = saveLearning;
		this.stepCost = stepCost;
		this.noopCost = noopCost;
		this.reward = reward;
		this.tau = tau;
		this.incurCostOnNoop = incurCostOnNoop;
		this.runValueIteration = runValueIteration;
		this.runStochasticPolicyPlanner = runStochasticPolicyPlanner;
		this.numTrials = numTrials;
		this.kLevel = kLevel;
		this.gridGame = new GridGame();
		this.rewardCalcMap = new HashMap<String, RewardCalculator>();
		if (noopAllowed) {

			this.domain = (SGDomain) gridGame.generateDomain();
		} else {
			this.domain = (SGDomain) gridGame.generateDomainWithoutNoops();
		}
		this.solvedAgentPolicies = new HashMap<String, Map<Integer, Policy>>();
	}

	/**
	 * Called if not using types for other regarding, can create agents of
	 * different types (Selfish, simple other regarding)
	 * 
	 * @param gameFile
	 * @param kLevel
	 * @param stepCost
	 * @param incurCostOnNoop
	 * @param noopCost
	 * @param reward
	 * @param tau
	 * @param runValueIteration
	 * @param runStochasticPolicyPlanner
	 * @param numTrials
	 * @param noopAllowed
	 * @param rewardCalcTypeMap
	 * @param coopParam
	 * @param defendParam
	 */
	public Experiment(String gameFile, String experimentName, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, boolean saveLearning,boolean runWithRandomStartStates,
			Map<String, RewardCalculatorType> rewardCalcTypeMap,
			double coopParam, double defendParam) {

		this(gameFile, experimentName, kLevel, stepCost, incurCostOnNoop, noopCost, reward,
				tau, runValueIteration, runStochasticPolicyPlanner, numTrials,
				noopAllowed, saveLearning, runWithRandomStartStates);
		rewardCalcMap = new HashMap<String, RewardCalculator>();

		for (String name : rewardCalcTypeMap.keySet()) {
			switch (rewardCalcTypeMap.get(name)) {

			case SELFISH:
				rewardCalcMap.put(name, new SelfishRewardCalculator());
				break;
			case OTHER_REGARDING:
				rewardCalcMap.put(name, new OtherRegardingRewardCalculator(
						coopParam, defendParam));
				break;

			}
		}
	}

	/**
	 * called when running from Bash script
	 * 
	 * @param gameFile
	 * @param kLevel
	 * @param stepCost
	 * @param incurCostOnNoop
	 * @param noopCost
	 * @param reward
	 * @param tau
	 * @param runValueIteration
	 * @param runStochasticPolicyPlanner
	 * @param numTrials
	 * @param noopAllowed
	 * @param rewardCalcTypeMap
	 * @param bashLoopIndex
	 */
	public Experiment(String gameFile, String experimentName, int klevel, 
			double stepCost, boolean incurCostOnNoop, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, boolean saveLearning, boolean runWithRandomStartStates,String bashLoopIndex) {

		this(gameFile, experimentName, klevel, stepCost, incurCostOnNoop, noopCost, reward,
				tau, runValueIteration, runStochasticPolicyPlanner, numTrials,
				noopAllowed, saveLearning, runWithRandomStartStates);
		this.bashLoopIndex = bashLoopIndex;
	}

	/**
	 * Called when running other regarding, not for ESS
	 * 
	 * @param gameFile
	 * @param kLevel
	 * @param stepCost
	 * @param incurCostOnNoop
	 * @param noopCost
	 * @param reward
	 * @param tau
	 * @param runValueIteration
	 * @param runStochasticPolicyPlanner
	 * @param numTrials
	 * @param noopAllowed
	 * @param rewardCalcTypeMap
	 * @param parameterTypes
	 */
	public Experiment(String gameFile, String experimentName, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, boolean saveLearning, boolean runWithRandomStartStates,
			Map<String, RewardCalculatorType> rewardCalcTypeMap,
			Map<String, String> parameterTypes) {

		this(gameFile, experimentName, kLevel, stepCost, incurCostOnNoop, noopCost, reward,
				tau, runValueIteration, runStochasticPolicyPlanner, numTrials,
				noopAllowed, saveLearning, runWithRandomStartStates);
		rewardCalcMap = new HashMap<String, RewardCalculator>();

		for (String name : rewardCalcTypeMap.keySet()) {
			switch (rewardCalcTypeMap.get(name)) {

			case SELFISH:
				rewardCalcMap.put(name, new SelfishRewardCalculator());
				break;

			case OTHER_REGARDING_NINE:
				// System.out.println("PTN "+parameterTypes.get(name));
				rewardCalcMap.put(name, new NineAgentOtherRegarding(
						parameterTypes.get(name)));
				break;
			}
		}
	}

	public Experiment(String gameFile, String experimentName, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, boolean saveLearning,
			Map<String, RewardCalculatorType> rewardCalcTypeMap,
			double cooperativeParameter, double defensiveParameter,
			String outDir) {
		this(gameFile, experimentName, kLevel, stepCost, incurCostOnNoop, noopCost, reward,
				tau, runValueIteration, runStochasticPolicyPlanner, numTrials,
				noopAllowed, saveLearning,false, rewardCalcTypeMap,
				cooperativeParameter, defensiveParameter);
		this.outDirRoot = outDir;

	}

	public List<GameAnalysis> runKLevelExperiment(Level0Type level0Type,
			int level0LearningEpisodes) {
		this.numLearningEpisodes = level0LearningEpisodes;
		return runKLevelExperiment(level0Type);
	}

	/**
	 * runExperiment runs
	 * 
	 * @param gameType
	 * @param reward
	 * @param kLevel
	 * @param fileName
	 * @return
	 */
	public List<GameAnalysis> runKLevelExperiment(Level0Type level0Type) {

		List<GameAnalysis> gas = new ArrayList<GameAnalysis>();

		StateHashFactory hashFactory = new DiscreteStateHashFactory();

		String outDir = makeOutDir();

		experimentMetaString();
		gameMetaString();

		// loop over all lower levels to learn their policies
		for (int k = 0; k < kLevel; k++) {

			// loop over join orders so that we learn as both agent0 and agent1
			// we need this info for both agent locations so that we can learn
			// the next level up
			for (int otherFirst = 0; otherFirst <= 1; otherFirst++) {

				createWorld();

				BestResponseToDistributionAgent brAgent;
				brAgent = new BRDPlanThenCombinePoliciesAgent(
						gameWorld.getDomain(), hashFactory, reward,
						runValueIteration, this.rewardCalc);

				// Creates a random opponent.
				// brAgent needs opponent to play a game in order to plan.
				Agent opponent = new RandomAgent();

				joinWorldOrdered(brAgent, opponent, otherFirst);

				// construct the other agent policies

				Policy lowerPolicy;

				String opponentName = opponent.getAgentName();
				String brAgentName = brAgent.getAgentName();

				// if level 0, store policy because it's the random agent
				if (k == 0) {
					lowerPolicy = generateLevel0Policy(level0Type,
							opponentName, outDir);
					Map<Integer, Policy> agentPolicies;
					// if no policies for this name exist, store policy
					if (!solvedAgentPolicies.containsKey(opponentName)) {
						agentPolicies = new HashMap<Integer, Policy>();
						agentPolicies.put(k, lowerPolicy);
					} else {
						agentPolicies = solvedAgentPolicies.get(opponentName);
						agentPolicies.put(k, lowerPolicy);
					}
					solvedAgentPolicies.put(opponentName, agentPolicies);
					printPolicyCollection();

					// otherwise, pull opponent's policy from previous level
				} else {
					lowerPolicy = solvedAgentPolicies.get(opponentName).get(
							k - 1);
				}

				// construct policy map to pass to Best Response agent
				// NOTE: If we have more than two opponents, we would need a
				// loop over opponents here...or maybe earlier
				Map<String, Map<Integer, Policy>> allOtherAgentPolicies = new HashMap<String, Map<Integer, Policy>>();
				HashMap<Integer, Policy> levelMap = new HashMap<Integer, Policy>();
				for (int lev = 0; lev <= k; lev++) {
					levelMap.put(lev, solvedAgentPolicies.get(opponentName)
							.get(lev));
				}

				// this is the policies we want to use for learning for this
				// agent
				allOtherAgentPolicies.put(opponentName, levelMap);

				// Now create a distribution for this agent

				brAgent.setOtherAgentDetails(allOtherAgentPolicies, k);

				GameAnalysis ga = this.gameWorld.runGame();
				CachedPolicy cp = new CachedPolicy(
						new DiscreteStateHashFactory(), brAgent.getPolicy());

				Map<Integer, Policy> agentPolicies;
				if (!solvedAgentPolicies.containsKey(brAgentName)) {
					agentPolicies = new HashMap<Integer, Policy>();

					agentPolicies.put(k + 1, cp);
				} else {
					agentPolicies = solvedAgentPolicies.get(brAgentName);
					agentPolicies.put(k + 1, cp);
				}

				solvedAgentPolicies.put(brAgentName, agentPolicies);
				printPolicyCollection();

				// add the game analysis to the records
				gas.add(ga);
			}
		}

		this.outFile = runCompetition(solvedAgentPolicies, kLevel, numTrials,
				outDir);

		kLevelMetaString();
		writeMetaData();
		return gas;
	}

	/**
	 * Has two players join a game world in a specified order. If otherFirst is 1
	 * then the second player listed joins before the player listed first. If 0 then the 
	 * player listed first joins first.
	 * 
	 * @param player
	 * @param opponent
	 * @param otherFirst
	 */
	private void joinWorldOrdered(Agent player, Agent opponent, int otherFirst) {
		if (otherFirst == 1) {
			opponent.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, domain
							.getObjectClass(GridGame.CLASSAGENT), domain
							.getSingleActions()));
			player.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, domain
							.getObjectClass(GridGame.CLASSAGENT), domain
							.getSingleActions()));
		} else {
			player.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, domain
							.getObjectClass(GridGame.CLASSAGENT), domain
							.getSingleActions()));
			opponent.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, domain
							.getObjectClass(GridGame.CLASSAGENT), domain
							.getSingleActions()));
		}
	}

	private Policy generateLevel0Policy(Level0Type type, String opponentName,
			String outDir) {
		switch (type) {
		case RANDOM:
			return new RandomSingleAgentPolicy(opponentName,
					domain.getSingleActions());
		case Q:
			qMetaString();
			Map<String, Policy> policyMap = runLearning(
					this.numLearningEpisodes, outDir);
			return policyMap.get(opponentName);
		case NASH_B:
			break;
		case NASH_CD:
			break;
		}
		return null;
	}

	private void printPolicyCollection() {
		for (String a : solvedAgentPolicies.keySet()) {
			System.out.println("Agent " + a + ": ");
			for (Integer i : solvedAgentPolicies.get(a).keySet()) {
				System.out.println("lev: " + i + ": "
						+ solvedAgentPolicies.get(a).get(i));
			}
		}
		System.out.println();
	}

	private SGDomain getDomain() {
		return domain;
	}

	private String runCompetition(Map<String, Map<Integer, Policy>> policyMap,
			int numLevels, int numRuns, String outDir) {

		double[][][] rewardMatrix = new double[numLevels + 1][numLevels + 1][2];
		Map<Integer, Policy> rowAgent, colAgent;

		rowAgent = policyMap.get("agent0");
		colAgent = policyMap.get("agent1");

		for (int t = 0; t < numRuns; t++) {
			for (int i = 0; i < rowAgent.size(); i++) {
				for (int j = 0; j < colAgent.size(); j++) {

					createWorld();

					SetStrategyAgent rowPlayer = new TransparentSetStrategyAgent(
							this.domain, new SingleToMultiPolicy(
									rowAgent.get(i), this.domain, "agent0"));
					SetStrategyAgent colPlayer = new TransparentSetStrategyAgent(
							this.domain, new SingleToMultiPolicy(
									colAgent.get(j), this.domain, "agent1"));

					rowPlayer.joinWorld(
							this.gameWorld,
							new AgentType(GridGame.CLASSAGENT, this.domain
									.getObjectClass(GridGame.CLASSAGENT),
									this.domain.getSingleActions()));
					colPlayer.joinWorld(
							this.gameWorld,
							new AgentType(GridGame.CLASSAGENT, this.domain
									.getObjectClass(GridGame.CLASSAGENT),
									this.domain.getSingleActions()));

					GameAnalysis ga = this.gameWorld.runGame(TIMEOUT);
					List<Map<String, Double>> jointRewards = ga
							.getJointRewards();
					Map<String, Double> agentReward = new HashMap<String, Double>();

					for (Map<String, Double> rewards : jointRewards) {
						for (String agentKey : rewards.keySet()) {
							if (agentReward.containsKey(agentKey)) {
								agentReward.put(
										agentKey,
										agentReward.get(agentKey)
										+ rewards.get(agentKey));
							} else {
								agentReward
								.put(agentKey, rewards.get(agentKey));
							}
						}
					}

					rewardMatrix[i][j][0] += agentReward.get("agent0");
					rewardMatrix[j][i][1] += agentReward.get("agent1");
					StateParser sp = new StateJSONParser(domain);

					String outFile = outDir + "Green_" + i + "_Blue_" + j + "/"
							+ "G" + i + "B" + j + "_Trial_" + t;

					ga.writeToFile(outFile, sp);

					if (t == numRuns - 1) {
						rewardMatrix[i][j][0] /= numRuns;
						rewardMatrix[j][i][1] /= numRuns;
					}
				}
			}
		}

		this.scores = rewardMatrix;
		return outDir;
	}

	/**
	 * Runs a series of games to test for an ESS among a set of agent strategies.
	 * 
	 * Note, it's really only set up to run two agents against each other right now.
	 * We should probably pull this apart some at some point.
	 * 
	 * It's set up to run from the command line at the moment.
	 * 
	 * @param numLearningEpisodes
	 * @param numGameTrials
	 * @param numAttempts
	 * @param agentPrefTypes
	 * @param boltzmannExplore
	 * @param temp
	 * @param optimisticInit
	 * @param optimisticValue
	 * @param numToVisualize
	 * @return
	 */
	private String runQESS(int numLearningEpisodes, int numGameTrials,
			int numAttempts, String[] agentPrefTypes, boolean boltzmannExplore,
			double temp, boolean optimisticInit, double optimisticValue,
			double numToVisualize) {
		this.optimisticInit = optimisticInit;
		this.optimisticValue = optimisticValue;
		this.boltzmannExplore = boltzmannExplore;
		this.temp = temp;

		int numAgentTypes = agentPrefTypes.length;
		double[][][] rewardMatrix = new double[numAgentTypes][numAgentTypes][2];
		String[][][][] convergenceTestMatrix = new String[numAgentTypes][numAgentTypes][2][numAttempts];

		String outDir = makeOutDir();

		this.numLearningEpisodes = numLearningEpisodes;

		experimentMetaString();
		gameMetaString();

		//these loops should change for the correct ESS
		for (int a = 0; a < numAttempts; a++) {
			for (int t0 = 0; t0 < numAgentTypes; t0++) {
				for (int t1 = t0 + 1; t1 < numAgentTypes; t1++) {

					SGNaiveQLAgent agent0, agent1;
					StateHashFactory hashFactory = new DiscreteStateHashFactory();
					StateParser sp = new StateJSONParser(domain);

					GameAnalysis ga;

					agent0 = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
							this.LEARNING_RATE, hashFactory);

					agent1 = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
							this.LEARNING_RATE, hashFactory);

					if (optimisticInit) {
						this.optimisticValue = this.reward - 10;
						ValueFunctionInitialization initValue = (ValueFunctionInitialization) new ConstantValueFunctionInitialization(
								this.optimisticValue);
						agent0.setQValueInitializer(initValue);
						agent1.setQValueInitializer(initValue);
					}

					if (this.boltzmannExplore) {

						agent0.setStrategy(new BoltzmannQPolicy(agent0,
								this.temp));
						agent1.setStrategy(new BoltzmannQPolicy(agent1,
								this.temp));
					}


					// Learning Phase
					List<Map<StateHashTuple, List<QValue>>> greenQMaps = new ArrayList<Map<StateHashTuple, List<QValue>>>();
					List<Map<StateHashTuple, List<QValue>>> blueQMaps = new ArrayList<Map<StateHashTuple, List<QValue>>>();
					for (int i = 0; i < numLearningEpisodes + numGameTrials; i++) {


						Map<String, AgentType> agentTypes = new HashMap<String,AgentType>();
						agentTypes.put("agent0", agent0.getAgentType());
						agentTypes.put("agent1", agent1.getAgentType());

						//this controls what start state the agents see
						//note, we have to have them see the true start state first
						//so that names and types for the agents exist 
						if(i!=0){
							createWorld(runWithRandomStartStates,agentTypes);
						}else{
							createWorld();
						}

						//agents join the world
						agent0.joinWorld(
								this.gameWorld,
								new AgentType(GridGame.CLASSAGENT, this.domain
										.getObjectClass(GridGame.CLASSAGENT),
										this.domain.getSingleActions()));
						agent1.joinWorld(
								this.gameWorld,
								new AgentType(GridGame.CLASSAGENT, this.domain
										.getObjectClass(GridGame.CLASSAGENT),
										this.domain.getSingleActions()));

						//set the reward calculator types for both agents
						rewardCalcMap.put(agent0.getAgentName(),
								new NineAgentOtherRegarding(agentPrefTypes[t0]));
						rewardCalcMap.put(agent1.getAgentName(),
								new NineAgentOtherRegarding(agentPrefTypes[t1]));
						agent0.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
								rewardCalcMap, gameWorld.getRewardModel(),
								agent0.getAgentName()));
						agent1.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
								rewardCalcMap, gameWorld.getRewardModel(),
								agent1.getAgentName()));

						ga = this.gameWorld.runGame(TIMEOUT);

						if (saveLearning) {
							String outFile = outDir + "Green_Q_"
									+ agentPrefTypes[t0] + "_Blue_Q_"
									+ agentPrefTypes[t1] + "_Attempt_" + a + "/"
									+ "G_" + agentPrefTypes[t0] + "_B_"
									+ agentPrefTypes[t1] + "_Learning_Trial_" + i;
							ga.writeToFile(outFile, sp);
						}

						List<Map<String, Double>> jointRewards = ga
								.getJointRewards();

						Map<String, Double> agentReward = new HashMap<String, Double>();

						for (Map<String, Double> rewards : jointRewards) {
							if(!rewards.containsKey("agent0")){
								@SuppressWarnings("unused")
								int x = 1;
							}
							for (String agentKey : rewards.keySet()) {
								
								if (agentReward.containsKey(agentKey)) {
									agentReward.put(
											agentKey,
											agentReward.get(agentKey)
											+ rewards.get(agentKey));
								} else {
									agentReward.put(agentKey,
											rewards.get(agentKey));
								}
							}
						}

						if (i > numLearningEpisodes) {
							rewardMatrix[t0][t1][0] += agentReward
									.get("agent0");
							rewardMatrix[t1][t0][1] += agentReward
									.get("agent1");
						}

						int convergeWindow = 0;

						if (i >= numLearningEpisodes + numGameTrials
								- convergeWindow - 1) {
							greenQMaps.add(agent0.getQMapCopy());
							blueQMaps.add(agent1.getQMapCopy());

							if (i == numLearningEpisodes + numGameTrials - 1) {
								if (isConverged(greenQMaps, 0.01))
									convergenceTestMatrix[t0][t1][0][a] = " C ";
								else
									convergenceTestMatrix[t0][t1][0][a] = " X ";
								if (isConverged(blueQMaps, 0.01))
									convergenceTestMatrix[t1][t0][1][a] = " C ";
								else
									convergenceTestMatrix[t1][t0][1][a] = " X ";
								if (a == numAttempts - 1) {
									// System.out.println("DIVIDING MATRIX by "+numAttempts*numGameTrials+" __________________%*^(*#^*(#*$%^)(#$)%(^)#%*(^)#*$)");
									// System.out.println("0: "+rewardMatrix[t0][t1][0]);
									// System.out.println("1: "+rewardMatrix[t1][t0][1]);
									rewardMatrix[t0][t1][0] /= (numAttempts * numGameTrials);
									rewardMatrix[t1][t0][1] /= (numAttempts * numGameTrials);

								}
							}
						}

					}

					agent0Policy = new GreedyQPolicy(
							(QComputablePlanner) agent0);
					agent1Policy = new GreedyQPolicy(
							(QComputablePlanner) agent1);



					TransparentSetStrategyAgent agentSet = new TransparentSetStrategyAgent(
							this.domain, agent0Policy);

					TransparentSetStrategyAgent opponentSet = new TransparentSetStrategyAgent(
							this.domain, agent1Policy);



					for (int j = 0; j < numToVisualize; j++) {
						createWorld();

						agentSet.joinWorld(
								this.gameWorld,
								new AgentType(GridGame.CLASSAGENT, this.domain
										.getObjectClass(GridGame.CLASSAGENT),
										this.domain.getSingleActions()));
						opponentSet.joinWorld(
								this.gameWorld,
								new AgentType(GridGame.CLASSAGENT, this.domain
										.getObjectClass(GridGame.CLASSAGENT),
										this.domain.getSingleActions()));

						agentSet.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
								rewardCalcMap, gameWorld.getRewardModel(),
								agentSet.getAgentName()));
						opponentSet.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
								rewardCalcMap, gameWorld
								.getRewardModel(), opponentSet.getAgentName()));

						ga = this.gameWorld.runGame(TIMEOUT);
						String outFile = outDir + "Green_Q_" + agentPrefTypes[t0]
								+ "_Blue_Q_" + agentPrefTypes[t1] + "_Attempt_" + a
								+ "/" + "G_" + agentPrefTypes[t0] + "_B_"
								+ agentPrefTypes[t1] + "_Fixed_" + j;

						ga.writeToFile(outFile, sp);

					}
					// check for C, D, or CD

					int isCagent = isPolicyC(outDir, agentSet);
					System.out.println("Is "+agentSet.getAgentName()+" C:"+isCagent);
					int isDagent = isPolicyD(outDir, agentSet);
					System.out.println("Is "+agentSet.getAgentName()+" D:"+isDagent);
					int isCopp = isPolicyC(outDir, opponentSet);
					System.out.println("Is "+opponentSet.getAgentName()+" C:"+isCopp);
					int isDopp = isPolicyD(outDir, opponentSet);
					System.out.println("Is "+opponentSet.getAgentName()+" D:"+isDopp);

					this.csvText += ((NineAgentOtherRegarding)rewardCalcMap.get(agentSet.getAgentName())).getAgentType()
							+ "," + ((NineAgentOtherRegarding)rewardCalcMap.get(opponentSet.getAgentName())).getAgentType() + ","
							+ isCagent + "," + isDagent + "," + isCopp + ","
							+ isDopp + "," + a+"\n";
				}
			}
		}

		// System.out.println(numAgentTypes);

		this.scores = rewardMatrix;
		this.convergence = convergenceTestMatrix;
		qMetaString();
		eSSMetaString();
		this.outFile = outDir;
		writeMetaData();
		writeCSV();
		return outDir;
	}



	private boolean isConverged(List<Map<StateHashTuple, List<QValue>>> qMaps,
			double threshold) {

		if (qMaps.size() < 2) {
			return false;
		}

		Map<StateHashTuple, List<QValue>> previousMap = qMaps.get(0);
		Map<StateHashTuple, List<QValue>> currentMap;

		Map<StateHashTuple, Map<AbstractGroundedAction, List<Double>>> differences = new HashMap<StateHashTuple, Map<AbstractGroundedAction, List<Double>>>();

		double defaultQ = 0;
		if (this.optimisticInit)
			defaultQ = this.optimisticValue;

		for (int i = 1; i < qMaps.size(); i++) {
			currentMap = qMaps.get(i);
			for (StateHashTuple s : currentMap.keySet()) {
				if (previousMap.containsKey(s)) {
					// Get difference in Q-values for all actions.
					for (QValue currentQVal : currentMap.get(s)) {
						Double difference = Math.abs(currentQVal.q - defaultQ);
						for (QValue prevQVal : previousMap.get(s)) {
							if (currentQVal.a.equals(prevQVal.a)) {
								// Get difference in Q value for this action.
								difference = Math.abs(currentQVal.q
										- prevQVal.q);
							}
						}
						// Set the difference.
						Map<AbstractGroundedAction, List<Double>> M;
						List<Double> L;
						if (differences.containsKey(s)) {
							if (differences.get(s).containsKey(currentQVal.a)) {
								M = differences.get(s);
								L = M.get(currentQVal.a);
							} else {
								M = differences.get(s);
								L = new ArrayList<Double>();
							}
						} else {
							M = new HashMap<AbstractGroundedAction, List<Double>>();
							L = new ArrayList<Double>();
						}
						L.add(difference);
						M.put(currentQVal.a, L);
						differences.put(s, M);
					}
				} else {
					// Difference is value of current for all actions.
					for (QValue currentQVal : currentMap.get(s)) {
						HashMap<AbstractGroundedAction, List<Double>> M = new HashMap<AbstractGroundedAction, List<Double>>();
						ArrayList<Double> L = new ArrayList<Double>();
						L.add(Math.abs(currentQVal.q - defaultQ));
						M.put(currentQVal.a, L);
						differences.put(s, M);
					}
				}
			}
			previousMap = currentMap;
		}

		// Calculate the average differences. If any is above threshold, return
		// false.
		for (StateHashTuple s : differences.keySet()) {
			for (AbstractGroundedAction a : differences.get(s).keySet()) {
				double avg = 0.0;
				for (int i = 0; i < differences.get(s).get(a).size(); i++) {
					avg += differences.get(s).get(a).get(i);
				}
				avg /= (qMaps.size() - 1); // Number of differences is one less
				// than number of maps.
				System.out.println("Computed Avg.:" + avg);
				if (avg > threshold)
					return false;
			}
		}

		return true;
	}

	protected Map<String, Policy> runLearning(int numEpisodes, String outDir) {
		this.numLearningEpisodes = numEpisodes;
		SGNaiveQLAgent agent, opponent;
		StateHashFactory hashFactory = new DiscreteStateHashFactory();
		StateParser sp = new StateJSONParser(domain);

		GameAnalysis ga;

		String[] convergenceTestMatrix = new String[2];
		List<Map<StateHashTuple, List<QValue>>> greenQMaps = new ArrayList<Map<StateHashTuple, List<QValue>>>();
		List<Map<StateHashTuple, List<QValue>>> blueQMaps = new ArrayList<Map<StateHashTuple, List<QValue>>>();

		agent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);
		opponent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);

		if (this.optimisticInit) {

			ValueFunctionInitialization initValue = (ValueFunctionInitialization) new ConstantValueFunctionInitialization(
					this.optimisticValue);
			agent.setQValueInitializer(initValue);
			opponent.setQValueInitializer(initValue);
		}

		if (this.boltzmannExplore) {

			agent.setStrategy(new BoltzmannQPolicy(agent, this.temp));
			opponent.setStrategy(new BoltzmannQPolicy(opponent, this.temp));
		}
		// Learning Phase
		for (int i = 0; i < numEpisodes; i++) {
			createWorld();
			agent.joinWorld(this.gameWorld, new AgentType(GridGame.CLASSAGENT,
					this.domain.getObjectClass(GridGame.CLASSAGENT),
					this.domain.getSingleActions()));
			opponent.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));

			agent.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
					rewardCalcMap, gameWorld.getRewardModel(), agent
					.getAgentName()));
			opponent.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
					rewardCalcMap, gameWorld.getRewardModel(), opponent
					.getAgentName()));
			ga = this.gameWorld.runGame(TIMEOUT);

			if (saveLearning) {
				String outFile = outDir + "Green_Q" + "_Blue_Q" + "Learning/"
						+ "GQ" + "BQ" + "_Trial_" + i;
				ga.writeToFile(outFile, sp);
			}

			// check for convergence

			int convergeWindow = 0;// (int) Math.ceil(numLearningEpisodes*.05);

			if (i >= numLearningEpisodes - convergeWindow - 1) {
				greenQMaps.add(agent.getQMapCopy());
				blueQMaps.add(opponent.getQMapCopy());

				if (i == numLearningEpisodes - 1) {
					if (isConverged(greenQMaps, 0.01))
						convergenceTestMatrix[0] = " C ";
					else
						convergenceTestMatrix[0] = " X ";
					if (isConverged(blueQMaps, 0.01))
						convergenceTestMatrix[1] = " C ";
					else
						convergenceTestMatrix[1] = " X ";

				}
			}

		}

		Map<String, Policy> policyMap = new HashMap<String, Policy>();
		policyMap.put(agent.getAgentName(), new GreedyQPolicy(
				(QComputablePlanner) agent));
		policyMap.put(opponent.getAgentName(), new GreedyQPolicy(
				(QComputablePlanner) opponent));

		return policyMap;
	}

	public String runQLearners(int numEpisodes, boolean boltzmannExplore,
			double temp, boolean optimisticInit, double optimisticValue) {
		this.optimisticInit = optimisticInit;
		this.optimisticValue = optimisticValue;
		this.boltzmannExplore = boltzmannExplore;
		this.temp = temp;

		// Map<String, Double> agentReward = new HashMap<String, Double>();
		ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>();
		StateParser sp = new StateJSONParser(domain);
		GameAnalysis ga;
		this.numLearningEpisodes = numEpisodes;
		String outDir = makeOutDir();

		experimentMetaString();
		gameMetaString();

		Map<String, Policy> policyMap = runLearning(numEpisodes, outDir);

		// Execution Phase
		SetStrategyAgent agentSet = new SetStrategyAgent(domain,
				policyMap.get("agent0"));
		SetStrategyAgent opponentSet = new SetStrategyAgent(domain,
				policyMap.get("agent1"));

		agent0Policy = policyMap.get("agent0");
		agent1Policy = policyMap.get("agent1");

		for (int i = 0; i < this.numTrials; i++) {

			createWorld();

			agentSet.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));
			opponentSet.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));
			agentSet.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
					rewardCalcMap, gameWorld.getRewardModel(), agentSet
					.getAgentName()));
			opponentSet
			.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(
					rewardCalcMap, gameWorld.getRewardModel(),
					opponentSet.getAgentName()));

			ga = this.gameWorld.runGame(TIMEOUT);

			gas.add(ga);

			// System.out.println(ga.getJointRewards());

			String outFile = outDir + "Green_Q" + "_Blue_Q" + "/" + "GQ" + "BQ"
					+ "_Trial_" + i;

			ga.writeToFile(outFile, sp);

		}

		qMetaString();
		this.outFile = outDir;
		writeMetaData();
		return outDir;
	}

	public String runMALearners(int numEpisodes, String operatorType) {
		// Map<String, Double> agentReward = new HashMap<String, Double>();

		SGBackupOperator operator;
		// System.out.println("Creating World");
		// createWorld();
		if (operatorType.compareToIgnoreCase("coco") == 0) {
			System.out.println("____Running Coco");
			operator = new CoCoQ();
		} else if (operatorType.compareToIgnoreCase("max") == 0) {
			System.out.println("____Running Max");
			operator = new MaxQ();
		} else if (operatorType.compareToIgnoreCase("minMax") == 0) {
			System.out.println("____Running MinMax");
			operator = new MinMaxQ();
		} else if (operatorType.compareToIgnoreCase("correlated_egalitarian") == 0) {
			System.out.println("____Running corr egal");
			operator = new CorrelatedQ(
					CorrelatedEquilibriumObjective.EGALITARIAN);
		} else if (operatorType.compareToIgnoreCase("correlated_libertarian") == 0) {
			System.out.println("____Running corr liber");
			operator = new CorrelatedQ(
					CorrelatedEquilibriumObjective.LIBERTARIAN);
		} else if (operatorType.compareToIgnoreCase("correlated_republican") == 0) {
			System.out.println("____Running corr repub");
			operator = new CorrelatedQ(
					CorrelatedEquilibriumObjective.REPUBLICAN);
		} else if (operatorType.compareToIgnoreCase("correlated_utilitarian") == 0) {
			System.out.println("____Running corr util");
			operator = new CorrelatedQ(
					CorrelatedEquilibriumObjective.UTILITARIAN);
		} else {
			operator = new MaxQ();
		}

		ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>();
		StateParser sp = new StateJSONParser(domain);
		GameAnalysis ga;

		String outDir = makeOutDir();

		MultiAgentQLearning maQLearning1 = new MultiAgentQLearning(domain,
				DISCOUNT_FACTOR, LEARNING_RATE, new DiscreteStateHashFactory(),
				0.0, operator, false);

		MultiAgentQLearning maQLearning2 = new MultiAgentQLearning(domain,
				DISCOUNT_FACTOR, LEARNING_RATE, new DiscreteStateHashFactory(),
				0.0, new MaxQ(), false);

		// new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
		// this.LEARNING_RATE, new DiscreteStateHashFactory());

		// Learning Phase
		for (int i = 0; i < numEpisodes; i++) {
			createWorld();
			maQLearning1.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));

			maQLearning2.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));

			ga = this.gameWorld.runGame(TIMEOUT);
			gas.add(ga);
			if (saveLearning) {
				String outFile = outDir + "MALearning/" + "_Trial_" + i;
				ga.writeToFile(outFile, sp);
			}

		}

		System.out.println("The MA one: " + maQLearning1.getAgentName());
		this.outFile = outDir;
		return outDir;
	}

	public String runQVsCooperator(int numEpisodes) {

		this.numLearningEpisodes = numEpisodes;

		SGNaiveQLAgent opponent;
		StateHashFactory hashFactory = new DiscreteStateHashFactory();
		// Map<String, Double> agentReward = new HashMap<String, Double>();
		ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>();

		// SimpleCooperativeStrategy agent = new
		// SimpleCooperativeStrategy(domain);
		BelligerentAgent agent = new BelligerentAgent(domain);
		opponent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);

		StateParser sp = new StateJSONParser(domain);
		GameAnalysis ga;

		String outDir = makeOutDir();

		experimentMetaString();
		gameMetaString();
		qMetaString();

		// Learning Phase
		for (int i = 0; i < numEpisodes; i++) {

			createWorld();
			agent.joinWorld(this.gameWorld, new AgentType(GridGame.CLASSAGENT,
					this.domain.getObjectClass(GridGame.CLASSAGENT),
					this.domain.getSingleActions()));
			opponent.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));
			// System.out.println(gameWorld);
			// System.out.flush();
			ga = this.gameWorld.runGame(TIMEOUT);

			if (saveLearning) {
				String outFile = outDir + "Green_CD" + "_Blue_Q" + "_Learning/"
						+ "GCD" + "BQ" + "_Episode_" + i;
				ga.writeToFile(outFile, sp);
			}

		}

		// Execution Phase
		BelligerentAgent agentSet = new BelligerentAgent(
				domain);
		SetStrategyAgent opponentSet = new SetStrategyAgent(domain,
				new GreedyQPolicy((QComputablePlanner) opponent));

		for (int i = 0; i < this.numTrials; i++) {

			createWorld();

			agentSet.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));
			opponentSet.joinWorld(
					this.gameWorld,
					new AgentType(GridGame.CLASSAGENT, this.domain
							.getObjectClass(GridGame.CLASSAGENT), this.domain
							.getSingleActions()));

			ga = this.gameWorld.runGame(TIMEOUT);

			gas.add(ga);

			System.out.println(ga.getJointRewards());
			String outFile = outDir + "Green_CD" + "_Blue_Q" + "/" + "GCD"
					+ "BQ" + "_Trial_" + i;

			ga.writeToFile(outFile, sp);

		}
		this.outFile = outDir;
		writeMetaData();
		return outDir;
	}

	public HashMap<String, Double> testPolicyAgainstType(String outDir, TransparentSetStrategyAgent fixedPolicyAgent, 
			String oppPreference) {

		// create a VI that has given preference
		StateHashFactory hashFactory = new DiscreteStateHashFactory();

		int otherFirst = 1;
		if (fixedPolicyAgent.getAgentName().compareToIgnoreCase("agent0") == 0) {
			otherFirst = 0;
		}

		createWorld();

		RewardCalculator calc = new NineAgentOtherRegarding(oppPreference);

		BestResponseToDistributionAgent viAgent;

		viAgent = new BRDPlanThenCombinePoliciesAgent(gameWorld.getDomain(), 
				hashFactory, reward, runValueIteration, calc);

		Agent fixedPolicy = fixedPolicyAgent;
		joinWorldOrdered(fixedPolicy, viAgent, otherFirst);

		// construct the other agent policies
		Policy lowerPolicy;

		String fixedPolicyName = fixedPolicy.getAgentName();
		String viAgentName = viAgent.getAgentName();


		// if level 0, store policy because it's the random agent
		lowerPolicy = fixedPolicyAgent.getPolicy();
		Map<Integer, Policy> agentPolicies;

		// if no policies for this name exist, store policy

		if (!solvedAgentPolicies.containsKey(fixedPolicyName)) {
			agentPolicies = new HashMap<Integer, Policy>();
			agentPolicies.put(0, lowerPolicy);

		} else {
			agentPolicies = solvedAgentPolicies.get(fixedPolicyName);
			agentPolicies.put(0, lowerPolicy);

		}

		solvedAgentPolicies.put(fixedPolicyName, agentPolicies);

		// otherwise, pull opponent's policy from previous level
		// construct policy map to pass to Best Response agent
		// NOTE: If we have more than two opponents, we would need a
		// loop over opponents here...or maybe earlier

		Map<String, Map<Integer, Policy>> allOtherAgentPolicies = new HashMap<String, Map<Integer, Policy>>();

		HashMap<Integer, Policy> levelMap = new HashMap<Integer, Policy>();
		levelMap.put(0, solvedAgentPolicies.get(fixedPolicyName).get(0));
		// this is the policies we want to use for learning for this
		// agent
		allOtherAgentPolicies.put(fixedPolicyName, levelMap);
		// Now create a distribution for this agent
		viAgent.setOtherAgentDetails(allOtherAgentPolicies, 0);

		GameAnalysis ga = this.gameWorld.runGame(TIMEOUT);
		StateParser sp = new StateJSONParser(domain);
		
		String outFile;
		if(otherFirst==0){
			outFile = outDir + "Green_FxdPi_Blue_VIw"
					+ oppPreference + "_/G_FxdPi_B_VIw"+oppPreference;
		}else{
			outFile = outDir + "Green_VIw"+oppPreference+"_Blue_FxdPi"
					+ "_/G_VIw"+oppPreference+"_B_FxdPi";
		}
		ga.writeToFile(outFile, sp);

		List<Map<String, Double>> rewards = ga.getJointRewards();

		double VIReward = 0.0;
		double policyReward = 0.0;
		for (Map<String, Double> reward : rewards) {
			VIReward += reward.get(viAgentName);
			policyReward += reward.get(fixedPolicyName);

		}
		System.out.println("VI R: "+VIReward+" Policy R: "+policyReward);

		HashMap<String, Double> rewardVals = new HashMap<String, Double>();
		rewardVals.put(fixedPolicyName, policyReward);
		rewardVals.put(viAgentName, VIReward);
		return rewardVals;



	}



	public int isPolicyC(String outDir, TransparentSetStrategyAgent fixedPolicyAgent) {
		HashMap<String, Double> rewardVals = testPolicyAgainstType(outDir, fixedPolicyAgent, "AxBCxD");
		for (String name : rewardVals.keySet()) {
			System.out.println("Name for R: "+name+" R: "+rewardVals.get(name));
			if (name.compareToIgnoreCase(fixedPolicyAgent.getAgentName()) != 0) {
				if (rewardVals.get(name) > 0) {
					return 1;
				}
				return 0;
			}
		}
		return -1;

	}



	public int isPolicyD(String outDir, TransparentSetStrategyAgent fixedPolicyAgent) {

		HashMap<String, Double> rewardVals = testPolicyAgainstType(outDir, fixedPolicyAgent, "BADC");
		double policyScore = Double.NaN, oppScore = Double.NaN;

		for (String name : rewardVals.keySet()) {

			if (name.compareToIgnoreCase(fixedPolicyAgent.getAgentName()) == 0) {
				policyScore = rewardVals.get(name);
			} else {
				oppScore = rewardVals.get(name);
			}
		}

		if (policyScore == Double.NaN || oppScore == Double.NaN) {
			return -1;
		} else if (policyScore >= oppScore) {
			return 1;
		} else {
			return 0;
		}

	}

	public void writeCSV() {
		PrintWriter writer;
		try {
			writer = new PrintWriter(outFile + "policyClasses.csv", "UTF-8");
			writer.print(this.csvText);
			writer.close();
			this.csvText = null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void writeMetaData() {
		PrintWriter writer;
		try {
			writer = new PrintWriter(outFile + "meta.txt", "UTF-8");
			writer.print(this.metaText);
			writer.close();
			this.metaText = null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	protected void gameMetaString() {
		String noop;

		if (this.incurCostOnNoop)
			noop = Double.toString(this.noopCost);
		else
			noop = "0.0";
		this.metaText += "GAME DATA \n -----------------------------------\n";
		this.metaText += "Game File/Type: " + this.gameFile + '\n';
		this.metaText += "Reward Value: " + this.reward + '\n';
		this.metaText += "Step Cost: " + this.stepCost + '\n';
		this.metaText += "Noop Allowed: " + this.noopAllowed + '\n';
		if (this.noopAllowed)
			this.metaText += "Noop Cost: " + noop + '\n';
		this.metaText += "Game timeout: " + this.TIMEOUT + " moves" + '\n';
		this.metaText += '\n';
	}

	protected void experimentMetaString() {
		this.metaText += "EXPERIMENT DATA \n-----------------------------------\n";
		this.metaText += "Number of trials: " + this.numTrials + '\n';
		this.metaText += '\n';
	}

	protected void kLevelMetaString() {
		PrintWriter current, writerBlue, writerGreen;
		this.metaText += "K-LEVEL DATA \n-----------------------------------\n";

		try {
			writerBlue = new PrintWriter(outFile + "scoresBlue", "UTF-8");
			writerGreen = new PrintWriter(outFile + "scoresGreen", "UTF-8");

			this.metaText += "k-Level: " + this.kLevel + '\n';
			this.metaText += "Tau: " + this.tau + '\n';
			this.metaText += "Using VI: " + this.runValueIteration + '\n';
			this.metaText += "Using Stochastic Policies: "
					+ this.runStochasticPolicyPlanner + '\n';
			this.metaText += "Reward Calculator: "
					+ this.rewardCalc.functionToString() + "\n";
			this.metaText += "Score Matrices:" + '\n';
			for (int m = 0; m < 2; m++) {
				if (m == 0) {
					this.metaText += "Green Agent:" + '\n';
					current = writerGreen;
				} else {
					this.metaText += "Blue Agent:" + '\n';
					current = writerBlue;
				}
				for (int i = 0; i < this.scores.length; i++) {
					this.metaText += i + ": ";
					for (int j = 0; j < this.scores.length; j++) {
						this.metaText += this.scores[i][j][m] + " ";
						current.print(this.scores[i][j][m] + " ");
					}
					this.metaText += '\n';
				}
				this.metaText += '\n';
			}
			writerBlue.close();
			writerGreen.close();
			this.metaText += '\n';

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	protected void eSSMetaString() {
		PrintWriter current, writerBlue, writerGreen;
		this.metaText += "ESS Scores \n-----------------------------------\n";

		try {
			writerBlue = new PrintWriter(outFile + "scoresBlue", "UTF-8");
			writerGreen = new PrintWriter(outFile + "scoresGreen", "UTF-8");

			this.metaText += "Score Matrices:" + '\n';
			for (int m = 0; m < 2; m++) {
				if (m == 0) {
					this.metaText += "Green Agent Scores:" + '\n';
					current = writerGreen;
				} else {
					this.metaText += "Blue Agent Scores:" + '\n';
					current = writerBlue;
				}
				for (int i = 0; i < this.scores.length; i++) {
					this.metaText += i + ": ";
					for (int j = 0; j < this.scores.length; j++) {
						this.metaText += this.scores[i][j][m] + " ";
						current.print(this.scores[i][j][m] + " ");
					}
					this.metaText += '\n';
				}
				this.metaText += '\n';
			}

			this.metaText += "Convergence Matrices:" + '\n';
			for (int a = 0; a < convergence[0][0][0].length; a++) {
				this.metaText += "Attempt:" + a + '\n';
				for (int m = 0; m < 2; m++) {
					if (m == 0) {
						this.metaText += "Green Agent Convergence Attempt " + a
								+ ":" + '\n';
						current = writerGreen;
					} else {
						this.metaText += "Blue Agent Convergence Attempt " + a
								+ ":" + '\n';
						current = writerBlue;
					}
					for (int i = 0; i < this.convergence.length; i++) {
						this.metaText += i + ": ";
						for (int j = 0; j < this.convergence.length; j++) {
							this.metaText += this.convergence[i][j][m][a] + " ";
							current.print(this.convergence[i][j][m][a] + " ");
						}
						this.metaText += '\n';
					}
					this.metaText += '\n';
				}
			}
			writerBlue.close();
			writerGreen.close();
			this.metaText += '\n';

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	protected void qMetaString() {
		this.metaText += "Q-LEARNING DATA \n-----------------------------------\n";
		this.metaText += "Number of learning episodes: "
				+ this.numLearningEpisodes + '\n';
		this.metaText += "Discount factor: " + this.DISCOUNT_FACTOR + '\n';
		this.metaText += "Learning rate: " + this.LEARNING_RATE + '\n';
		this.metaText += "Optimistic Initialization: " + this.optimisticInit
				+ '\n';
		if (this.optimisticInit)
			this.metaText += "Initialization Value: " + this.optimisticValue
			+ '\n';
		this.metaText += "Boltzmann Exploration: " + this.boltzmannExplore
				+ '\n';
		if (this.boltzmannExplore)
			this.metaText += "Boltzmann Temperature: " + this.temp + '\n';
		if (this.rewardCalc != null) {
			this.metaText += "Reward Calculator: "
					+ this.rewardCalc.functionToString() + "\n";
		}

		this.metaText += '\n';
	}

	private void createWorld(boolean random, Map<String, AgentType> agentTypes) {
		if(!random){
			createWorld();
		}else{
			if (!this.gameFile.endsWith(".json")) {

				State s = GridGame.getCorrdinationGameInitialState(this.domain);
				if (this.gameFile.equals("turkey")) {
					s = GridGame.getTurkeyInitialState(this.domain);
				} else if (this.gameFile.equals("coordination")) {
					s = GridGame.getCorrdinationGameInitialState(this.domain);
				} else if (this.gameFile.equals("prisonersdilemma")
						|| this.gameFile.equals("pd")) {
					s = GridGame.getPrisonersDilemmaInitialState(this.domain);
				}

				// create the Joint Action Model and add to the domain d
				JointActionModel jam = new GridGameStandardMechanics(this.domain);
				this.domain.setJointActionModel(jam);

				// create a Joint Reward Function (orignal burlap, no noop specific
				// cost)
				// JointReward jr = new GridGame.GGJointRewardFunction(d,
				// stepCost, reward, reward, incurCostOnNoOp);

				// create a Joint Reward Function
				JointReward jr = new SpecifyNoopCostRewardFunction(this.domain,
						this.stepCost, this.reward, this.reward,
						this.incurCostOnNoop, this.noopCost);
				TerminalFunction tf = new GridGame.GGTerminalFunction(this.domain);

				List<State> reachableStates = SGStateReachability.getReachableNonTerminalStates(s, this.domain, 
						agentTypes, new DiscreteStateHashFactory(), tf);
				Random rand = new Random();
				State randState = reachableStates.get(rand.nextInt(reachableStates.size()));

				SGStateGenerator sg = new ConstantSGStateGenerator(randState);

				this.gameWorld = new World(this.domain, jr, tf, sg);


			} else {
				// if(this.gameWorld==null){
				// System.out.println(" WE ARE HERE LOADING "+this.gameFile);
				this.gameWorld = GridGameWorldLoader.loadWorld(this.gameFile,
						this.stepCost, this.reward, this.incurCostOnNoop,
						this.noopCost);
				TerminalFunction tfHere = gameWorld.getTF();
				// }
				this.domain = this.gameWorld.getDomain();
				System.out.println("ATS size: "+agentTypes.size());

				List<State> reachableStates = SGStateReachability.getReachableNonTerminalStates(gameWorld.startingState(), this.domain, 
						agentTypes, new DiscreteStateHashFactory(), tfHere);
				System.out.println("Reachable states Sz: "+reachableStates.size());
				Random rand = new Random();
				State randState = reachableStates.get(rand.nextInt(reachableStates.size()));
				System.out.println("___________RandStartState: "+randState.getStateDescription());
				SGStateGenerator sg = new ConstantSGStateGenerator(randState);
				this.gameWorld = new World(this.domain, gameWorld.getRewardModel(), tfHere, sg);
			}
		}

	}

	public void createWorld() {
		// if we didn't specify a file, run a hardcoded game based on
		// the name we gave
		if (!this.gameFile.endsWith(".json")) {

			State s = GridGame.getCorrdinationGameInitialState(this.domain);
			if (this.gameFile.equals("turkey")) {
				s = GridGame.getTurkeyInitialState(this.domain);
			} else if (this.gameFile.equals("coordination")) {
				s = GridGame.getCorrdinationGameInitialState(this.domain);
			} else if (this.gameFile.equals("prisonersdilemma")
					|| this.gameFile.equals("pd")) {
				s = GridGame.getPrisonersDilemmaInitialState(this.domain);
			}

			// create the Joint Action Model and add to the domain d
			JointActionModel jam = new GridGameStandardMechanics(this.domain);
			this.domain.setJointActionModel(jam);

			// create a Joint Reward Function (orignal burlap, no noop specific
			// cost)
			// JointReward jr = new GridGame.GGJointRewardFunction(d,
			// stepCost, reward, reward, incurCostOnNoOp);

			// create a Joint Reward Function
			JointReward jr = new SpecifyNoopCostRewardFunction(this.domain,
					this.stepCost, this.reward, this.reward,
					this.incurCostOnNoop, this.noopCost);
			TerminalFunction tf = new GridGame.GGTerminalFunction(this.domain);
			SGStateGenerator sg = new ConstantSGStateGenerator(s);

			this.gameWorld = new World(this.domain, jr, tf, sg);
		} else {
			// if(this.gameWorld==null){
			// System.out.println(" WE ARE HERE LOADING "+this.gameFile);
			this.gameWorld = GridGameWorldLoader.loadWorld(this.gameFile,
					this.stepCost, this.reward, this.incurCostOnNoop,
					this.noopCost);
			// }
			this.domain = this.gameWorld.getDomain();
		}
	}

	private String makeOutDir() {
		Date date = new Date();
		SimpleDateFormat ft;
		String outDir;
		String gameName = this.gameFile.split("\\.")[2];
		gameName = gameName.substring(gameName.lastIndexOf("/")+1);
		if (bashLoopIndex != null) {
			ft = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
			outDir = "/data/people/betsy/"+experimentName+"_"+gameName+"/" +bashLoopIndex + "_"
					+ ft.format(date) + "/";
		} else {
			ft = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
			outDir = this.outDirRoot +gameName+"/"+ft.format(date) + "/";
		}

		File dirFile = new File(outDir);
		dirFile.mkdir();
		return outDir;
	}

	public static void main(String[] args) {

		// World Parameters
		String homeFile = System.getProperty("user.dir");
		homeFile += "/../MultiAgentGames/resources/worlds/";
		String[] gameFile = new String[] {
				homeFile + "TwoAgentsTwoGoals0.json", // 0
				homeFile + "TwoAgentsTwoGoals1.json", // 1
				homeFile + "TwoAgentsTwoGoals2.json", // 2
				homeFile + "LavaPits.json", // 3
				homeFile + "TwoAgentsTunnels", // 4
				homeFile + "TwoAgentsHall_3by5_2Walls.json", // 5
				homeFile + "TwoAgentsHall_3by5_noWalls.json", // 6
				homeFile + "TwoAgentsLongHall_1by7.json", // 7
				homeFile + "TwoAgentsIntersection_3by5.json", // 8
				homeFile + "TwoAgentsDoor_5by5.json", // 9
				homeFile + "TwoAgentsDoor_3by5.json", // 10
				homeFile + "TwoAgentsManners_5by5.json", // 11
				homeFile + "TwoAgentsNoManners_5by5.json", // 12
				homeFile + "TwoAgentsCompromise_2by5.json", //13
				homeFile + "TwoAgentsNoCompromise_2by5.json", //14
				"turkey", "coordination", "prisonersdilemma" }; // 14,15,16

		//TODO: start editing here (THIS IS JUST SO I CAN FIND THE SPOT!!!)
		// Choose from a json game file or built-in option from the list above.
		String file = gameFile[6];
		if(args.length>3){
			file = gameFile[Integer.valueOf(args[3].split("_")[0])];
		}

		double reward = 50.0; // Set this to set the rewards for goals.
		double stepCost = 0.0;
		double noopCost = 0.0;
		boolean incurCostOnNoop = true;
		boolean noopAllowed = true;

		// ///Experiment Parameters/////
		// determine what experiments to run
		String experimentName = "2015_09_16_testing";
		boolean runKLevel = false;
		boolean runTwoQLearners_TypesOptional = false;
		boolean runESS = true;

		int numTrials = 100;
		int numLearningEpisodes = 15000;
		int attempts = 1;

		int numToVisualize = 1;

		// pick a visualizer IF <=1 args true
		boolean showPolicyExplorer = false;
		boolean showGameReplays = true;
		boolean saveLearning = showGameReplays || true;

		// ///////Agent Parameters//////
		// K LEVEL PARAMETERS
		boolean runValueIteration = true; // set to false to run the

		// BoundedRTDP agent instead on ValueIteration
		// handles when policies are stochastically combined
		boolean runStochasticPolicyPlanner = true;
		// This is the level the "smartest" agent will be.
		int kLevel = 5;
		// defines the dist. over lower levels that the agent assumes.
		int tau = 3;

		// Q PARAMETERS
		boolean boltzmannExplore = true;
		double temp = 0.5;
		boolean optimisticInit = true;
		double optimisticValue = reward - 10.0;

		// Set these if running two Q learners
		// only one of these can be true...
		boolean runNormalQLearners = false;
		boolean runOtherRegardingOrderedPref = true;
		boolean runSimpleOtherRegarding = false;

		boolean runWithRandomStartStates = true;
		//set this to control how learning runs
		if(args.length>4){
			if (args[4].compareToIgnoreCase("true")==0){
				runWithRandomStartStates = true;
				numLearningEpisodes = 15000;
			}else{
				runWithRandomStartStates = false;
			}
		}

		// set if runSimpleOtherRegarding && runTwoQLearners_TypesOptional
		double coopParam = 0.1;
		double defendParam = 0.5;

		// set if runOtherRegardingOrderedPref && runTwoQLearners_TypesOptional
		String agent0OrderType = "ABDC";
		String agent1OrderType = "BADC";

		// set this if running ESS
		String[] agentTypes = { "AxBCxD", "AxBCxD" };
		// "ABCD", "ABDC", "BACD", "BADC", "ABCxD", "BACxD", "AxBCD", "AxBDC",
		// "AxBCxD"

		////////////DON'T CHANGE ANYTHING BELOW HERE TO EDIT PARAMETERS///////////

		// Execution timer
		long startTime = System.currentTimeMillis();

		Map<String, RewardCalculatorType> rewardCalcTypeMap = new HashMap<String, RewardCalculatorType>();
		Map<String, String> parameterTypes = null;

		if (runNormalQLearners) {
			rewardCalcTypeMap.put("agent0", RewardCalculatorType.SELFISH);
			rewardCalcTypeMap.put("agent1", RewardCalculatorType.SELFISH);

		} else if (runOtherRegardingOrderedPref || runESS) {

			rewardCalcTypeMap.put("agent0",
					RewardCalculatorType.OTHER_REGARDING_NINE);
			rewardCalcTypeMap.put("agent1",
					RewardCalculatorType.OTHER_REGARDING_NINE);

			if (runOtherRegardingOrderedPref) {
				parameterTypes = new HashMap<String, String>();
				parameterTypes.put("agent0", agent0OrderType);
				parameterTypes.put("agent1", agent1OrderType);
			}
		} else if (runSimpleOtherRegarding) {
			rewardCalcTypeMap.put("agent0",
					RewardCalculatorType.OTHER_REGARDING);
			rewardCalcTypeMap.put("agent1",
					RewardCalculatorType.OTHER_REGARDING);

		}

		Experiment runner;
		// run from a script at the command line
		if (args.length > 2) {
			// System.out.println("Args "+args[2]);
			runner = new Experiment(file, experimentName, kLevel, stepCost, incurCostOnNoop,
					noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					saveLearning,runWithRandomStartStates, args[2]);

		} else if (runESS) {
			runner = new Experiment(file, experimentName, kLevel, stepCost, incurCostOnNoop,
					noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					saveLearning, runWithRandomStartStates);
		} else if (runOtherRegardingOrderedPref) {

			runner = new Experiment(file, experimentName, kLevel, stepCost, incurCostOnNoop,
					noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					saveLearning,runWithRandomStartStates, rewardCalcTypeMap, parameterTypes);

			// runSimpleOtherRegarding
		} else {
			runner = new Experiment(file, experimentName, kLevel, stepCost, incurCostOnNoop,
					noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					saveLearning, runWithRandomStartStates, rewardCalcTypeMap, coopParam, defendParam);
		}

		// Run ESS from script
		if (args.length > 2) {
			agentTypes = new String[2];
			agentTypes[0] = args[0];
			agentTypes[1] = args[1];
			attempts = 1;
			runner.runQESS(numLearningEpisodes, numTrials, attempts,
					agentTypes, boltzmannExplore, temp, optimisticInit,
					optimisticValue, numToVisualize);

		} else if (runKLevel) {
			runner.runKLevelExperiment(Level0Type.RANDOM, numLearningEpisodes);
		} else if (runESS) {

			runner.runQESS(numLearningEpisodes, numTrials, attempts,
					agentTypes, boltzmannExplore, temp, optimisticInit,
					optimisticValue, numToVisualize);

		} else if (runTwoQLearners_TypesOptional) {
			runner.runQLearners(numLearningEpisodes, boltzmannExplore, temp,
					optimisticInit, optimisticValue);
		}

		// Maybe visualize Results
		if (args.length <= 2) {
			Visualizer v = GGVisualizer.getVisualizer(6, 6);
			if (showPolicyExplorer) {
				if (runner.agent0Policy == null) {
					v.addSpecificObjectPainter("agent0",
							new AgentPolicyObjectPainter(
									runner.solvedAgentPolicies.get("agent0")
									.get(kLevel), "agent0"));
					v.addSpecificObjectPainter("agent1",
							new AgentPolicyObjectPainter(
									runner.solvedAgentPolicies.get("agent1")
									.get(kLevel), "agent1"));
				} else {
					v.addSpecificObjectPainter("agent0",
							new AgentPolicyObjectPainter(runner.agent0Policy,
									"agent0"));
					v.addSpecificObjectPainter("agent1",
							new AgentPolicyObjectPainter(runner.agent1Policy,
									"agent1"));
				}

				SGVisualExplorer sgve = new SGVisualExplorer(runner.domain, v,
						runner.gameWorld.startingState());
				sgve.addKeyAction("w", "agent0:north");
				sgve.addKeyAction("a", "agent0:west");
				sgve.addKeyAction("s", "agent0:noop");
				sgve.addKeyAction("d", "agent0:east");
				sgve.addKeyAction("x", "agent0:south");
				sgve.addKeyAction("i", "agent1:north");
				sgve.addKeyAction("j", "agent1:west");
				sgve.addKeyAction("k", "agent1:noop");
				sgve.addKeyAction("l", "agent1:east");
				sgve.addKeyAction(",", "agent1:south");
				sgve.initGUI();
			} else if (showGameReplays) {
				new ExperimentVisualizer(v, runner.getDomain(), runner.outFile);
			}
		}

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total time:  " + totalTime / 1000.0 + " s");
	}

}