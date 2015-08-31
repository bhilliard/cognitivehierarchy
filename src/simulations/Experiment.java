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

import networking.common.GridGameWorldLoader;
import simulations.ExperimentRunner.Level0Type;
import simulations.ExperimentRunner.RewardCalculatorType;
import behavior.SpecifyNoopCostRewardFunction;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.ValueFunctionInitialization.ConstantValueFunctionInitialization;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.CachedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
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
import burlap.behavior.stochasticgame.saconversion.SelfishRewardCalculator;
import burlap.behavior.stochasticgame.saconversion.SingleToMultiPolicy;
import burlap.behavior.stochasticgame.solvers.CorrelatedEquilibriumSolver.CorrelatedEquilibriumObjective;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.SingleAction;
import burlap.oomdp.stochasticgames.World;
import burlap.oomdp.stochasticgames.common.ConstantSGStateGenerator;
import burlap.oomdp.visualizer.Visualizer;

/***
 * ExperimentRunner handles learning policies of agents from level-0 to
 * specified level k. The policies are created in order allowing the next level
 * to learn.
 * 
 * @author betsy hilliard betsy@cs.brown.edu
 *
 */
public class Experiment {

	// Agent parameters
	// Map from agent name (specific to location in game) to level to policy
	// solved for
	private Map<String, Map<Integer, Policy>> solvedAgentPolicies;
	private int kLevel;
	boolean runValueIteration, runStochasticPolicyPlanner;

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

	private String metaText = "";

	private final double DISCOUNT_FACTOR = 0.99, LEARNING_RATE = 0.01;
	private final int TIMEOUT = 100;
	private RewardCalculator rewardCalc;
	
	private Map<String, RewardCalculator> rewardCalcMap;

	public Experiment(String gameFile, int kLevel, double stepCost,
			boolean incurCostOnNoOp, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, RewardCalculatorType rewardCalcType,
			double coopParam, double defenseParam, String outDirRoot) {
		this(gameFile, kLevel, stepCost, incurCostOnNoOp, noopCost, reward,
				tau, runValueIteration, runStochasticPolicyPlanner, numTrials,
				noopAllowed, rewardCalcType, coopParam, defenseParam);
		this.outDirRoot = outDirRoot;
	}

	public Experiment(String gameFile, int kLevel, double stepCost,
			boolean incurCostOnNoOp, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed, RewardCalculatorType rewardType,
			double cooperateParam, double defenseParam) {

		this.gameFile = gameFile;
		this.noopAllowed = noopAllowed;
		this.stepCost = stepCost;
		this.noopCost = noopCost;
		this.reward = reward;
		this.tau = tau;
		this.incurCostOnNoop = incurCostOnNoOp;
		this.runValueIteration = runValueIteration;
		this.runStochasticPolicyPlanner = runStochasticPolicyPlanner;
		this.numTrials = numTrials;
		this.kLevel = kLevel;
		this.gridGame = new GridGame();
		if (noopAllowed) {

			this.domain = (SGDomain) gridGame.generateDomain();
		} else {
			this.domain = (SGDomain) gridGame.generateDomainWithoutNoops();
		}
		this.solvedAgentPolicies = new HashMap<String, Map<Integer, Policy>>();

		switch (rewardType) {

		case SELFISH:
			this.rewardCalc = new SelfishRewardCalculator();
			break;
		case OTHER_REGARDING:
			this.rewardCalc = new OtherRegardingRewardCalculator(
					cooperateParam, defenseParam);
			break;

		}

	}

	public Experiment(String gameFile, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			int tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed,
			Map<String, RewardCalculatorType> rewardCalcTypeMap,
			double coopParam, double defendParam) {

		this.gameFile = gameFile;
		this.noopAllowed = noopAllowed;
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
		if (noopAllowed) {

			this.domain = (SGDomain) gridGame.generateDomain();
		} else {
			this.domain = (SGDomain) gridGame.generateDomainWithoutNoops();
		}
		this.solvedAgentPolicies = new HashMap<String, Map<Integer, Policy>>();

		rewardCalcMap = new HashMap<String, RewardCalculator>();

		for(String name : rewardCalcTypeMap.keySet()){
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

	public Experiment(String gameFile, int kLevel, double stepCost,
			boolean incurCostOnNoop, double noopCost, double reward,
			int tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed,
			Map<String, RewardCalculatorType> rewardCalcTypeMap,
			Map<String,String> parameterTypes) {

		this.gameFile = gameFile;
		this.noopAllowed = noopAllowed;
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
		if (noopAllowed) {

			this.domain = (SGDomain) gridGame.generateDomain();
		} else {
			this.domain = (SGDomain) gridGame.generateDomainWithoutNoops();
		}
		this.solvedAgentPolicies = new HashMap<String, Map<Integer, Policy>>();

		rewardCalcMap = new HashMap<String, RewardCalculator>();


		for(String name : rewardCalcTypeMap.keySet()){
			switch (rewardCalcTypeMap.get(name)) {

			case SELFISH:
				rewardCalcMap.put(name, new SelfishRewardCalculator());
				break;

			case OTHER_REGARDING_NINE:
				//System.out.println("PTN "+parameterTypes.get(name));
				rewardCalcMap.put(name, new NineAgentOtherRegarding(parameterTypes.get(name)));
				break;
			}
		}
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

				List<SingleAction> actions = domain.getSingleActions();

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



	private String runQESS(int numLearningEpisodes, int numGameTrials, int numAttempts, String[] agentTypes) {

		int numAgentTypes = agentTypes.length;
		double[][][] rewardMatrix = new double[numAgentTypes][numAgentTypes][2];

		String outDir = makeOutDir();

		for (int a = 1; a <= numAttempts; a++) {
			for (int t0 = 0; t0 < numAgentTypes; t0++) {
				for (int t1 = 0; t1 < numAgentTypes; t1++) {

					SGNaiveQLAgent agent0, agent1;
					StateHashFactory hashFactory = new DiscreteStateHashFactory();
					StateParser sp = new StateJSONParser(domain);
					this.saveLearning = true;

					GameAnalysis ga;

					agent0 = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
							this.LEARNING_RATE, hashFactory);
					agent1 = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
							this.LEARNING_RATE, hashFactory);

					this.optimisticInit = true;

					if (optimisticInit) {
						ValueFunctionInitialization initValue = (ValueFunctionInitialization) 
								new ConstantValueFunctionInitialization(
										this.reward - 10);
						agent0.setQValueInitializer(initValue);
						agent1.setQValueInitializer(initValue);
					}

					this.boltzmannExplore = true;

					if (this.boltzmannExplore) {
						this.temp = 0.5;
						agent0.setStrategy(new BoltzmannQPolicy(agent0, this.temp));
						agent1.setStrategy(new BoltzmannQPolicy(agent1, this.temp));
					}
					// Learning Phase
					for (int i = 0; i < numLearningEpisodes+numGameTrials; i++) {
						createWorld();
						agent0.joinWorld(this.gameWorld, new AgentType(GridGame.CLASSAGENT,
								this.domain.getObjectClass(GridGame.CLASSAGENT),
								this.domain.getSingleActions()));
						agent1.joinWorld(
								this.gameWorld,
								new AgentType(GridGame.CLASSAGENT, this.domain
										.getObjectClass(GridGame.CLASSAGENT), this.domain
										.getSingleActions()));



						rewardCalcMap.put(agent0.getAgentName(), new NineAgentOtherRegarding(agentTypes[t0]));
						rewardCalcMap.put(agent1.getAgentName(), new NineAgentOtherRegarding(agentTypes[t1]));
						agent0.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), agent0.getAgentName()));
						agent1.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), agent1.getAgentName()));

						ga = this.gameWorld.runGame(TIMEOUT);

						if (saveLearning) {
							String outFile = outDir + "Green_Q_"+ agentTypes[t0] +"_Blue_Q_"+ agentTypes[t0] + "_Attempt_"+a+"/"
									+ "G_" + agentTypes[t0] + "_B_" +agentTypes[t1]+"_Trial_" + i;
							ga.writeToFile(outFile, sp);
						}

						List<Map<String, Double>> jointRewards = ga.getJointRewards();
						Map<String, Double> agentReward = new HashMap<String, Double>();

						for (Map<String, Double> rewards : jointRewards) {
							for (String agentKey : rewards.keySet()) {
								if (agentReward.containsKey(agentKey)) {
									agentReward.put(agentKey,agentReward.get(agentKey)
											+ rewards.get(agentKey));
								} else {
									agentReward.put(agentKey, rewards.get(agentKey));
								}
							}
						}

						if(i>numLearningEpisodes){
							rewardMatrix[t0][t1][0] += agentReward.get("agent0");
							rewardMatrix[t1][t0][1] += agentReward.get("agent1");
							if (a == numAttempts && i==numLearningEpisodes+numGameTrials-1) {
								//System.out.println("DIVIDING MATRIX by "+numAttempts*numGameTrials+" __________________%*^(*#^*(#*$%^)(#$)%(^)#%*(^)#*$)");
								//System.out.println("0: "+rewardMatrix[t0][t1][0]);
								//System.out.println("1: "+rewardMatrix[t1][t0][1]);
								rewardMatrix[t0][t1][0] /= (numAttempts*numGameTrials);
								rewardMatrix[t1][t0][1] /= (numAttempts*numGameTrials);
							}
						}


					}



				}

			}
		}


		this.scores = rewardMatrix;

		qMetaString();
		ESSScores();
		this.outFile = outDir;
		writeMetaData();

		return outDir;
	}


	protected Map<String, Policy> runLearning(int numEpisodes, String outDir) {
		this.numLearningEpisodes = numEpisodes;
		SGNaiveQLAgent agent, opponent;
		StateHashFactory hashFactory = new DiscreteStateHashFactory();
		StateParser sp = new StateJSONParser(domain);
		this.saveLearning = true;
		GameAnalysis ga;

		agent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);
		opponent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);



		this.optimisticInit = true;

		if (optimisticInit) {
			ValueFunctionInitialization initValue = (ValueFunctionInitialization) new ConstantValueFunctionInitialization(
					this.reward - 10);
			agent.setQValueInitializer(initValue);
			opponent.setQValueInitializer(initValue);
		}

		this.boltzmannExplore = true;

		if (this.boltzmannExplore) {
			this.temp = 0.5;
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


			agent.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), agent.getAgentName()));
			opponent.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), opponent.getAgentName()));
			ga = this.gameWorld.runGame(TIMEOUT);

			if (saveLearning) {
				String outFile = outDir + "Green_Q" + "_Blue_Q" + "Learning/"
						+ "GQ" + "BQ" + "_Trial_" + i;
				ga.writeToFile(outFile, sp);
			}

		}

		Map<String, Policy> policyMap = new HashMap<String, Policy>();
		policyMap.put(agent.getAgentName(), new GreedyQPolicy(
				(QComputablePlanner) agent));
		policyMap.put(opponent.getAgentName(), new GreedyQPolicy(
				(QComputablePlanner) opponent));

		return policyMap;
	}

	public String runQLearners(int numEpisodes) {
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
			agentSet.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), agentSet.getAgentName()));
			opponentSet.setInternalRewardFunction(new RewardCalculatingJointRewardFunction(rewardCalcMap, gameWorld.getRewardModel(), opponentSet.getAgentName()));

			ga = this.gameWorld.runGame(TIMEOUT);

			gas.add(ga);

			//System.out.println(ga.getJointRewards());

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
		this.saveLearning = true;
		SGBackupOperator operator;
		//System.out.println("Creating World");
		//createWorld();
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
				0.0,  new MaxQ(), false);

		//				new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
		//				this.LEARNING_RATE, new DiscreteStateHashFactory());



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

			ga = this.
					gameWorld.
					runGame(TIMEOUT);
			gas.add(ga);
			if (saveLearning) {
				String outFile = outDir + "MALearning/" + "_Trial_" + i;
				ga.writeToFile(outFile, sp);
			}

		}

		System.out.println("The MA one: "+maQLearning1.getAgentName());
		this.outFile = outDir;
		return outDir;
	}

	public String runQVsCooperator(int numEpisodes) {

		this.numLearningEpisodes = numEpisodes;
		this.saveLearning = true;
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
		SimpleCooperativeStrategy agentSet = new SimpleCooperativeStrategy(
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


	protected void ESSScores() {
		PrintWriter current, writerBlue, writerGreen;
		this.metaText += "ESS Scores \n-----------------------------------\n";

		try {
			writerBlue = new PrintWriter(outFile + "scoresBlue", "UTF-8");
			writerGreen = new PrintWriter(outFile + "scoresGreen", "UTF-8");

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

	protected void qMetaString() {
		this.metaText += "Q-LEARNING DATA \n-----------------------------------\n";
		this.metaText += "Number of learning episodes: "
				+ this.numLearningEpisodes + '\n';
		this.metaText += "Discount factor: " + this.DISCOUNT_FACTOR + '\n';
		this.metaText += "Learning rate: " + this.LEARNING_RATE + '\n';
		this.metaText += "Optimistic Initialization: " + this.optimisticInit
				+ '\n';
		this.metaText += "Boltzmann Exploration: " + this.boltzmannExplore
				+ '\n';
		if (this.boltzmannExplore)
			this.metaText += "Boltzmann Temperature: " + this.temp + '\n';
		if(this.rewardCalc!=null){
			this.metaText += "Reward Calculator: "
					+ this.rewardCalc.functionToString() + "\n";
		}

		this.metaText += '\n';
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
			//if(this.gameWorld==null){
			this.gameWorld = GridGameWorldLoader.loadWorld(this.gameFile,
					this.stepCost, this.reward, this.incurCostOnNoop,
					this.noopCost);
			//}
			this.domain = this.gameWorld.getDomain();
		}
	}

	private String makeOutDir() {
		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String outDir = this.outDirRoot + ft.format(date) + "/";
		File dirFile = new File(outDir);
		dirFile.mkdir();
		return outDir;
	}

	public static void main(String[] args) {

		double reward = 50.0; // Set this to set the rewards for goals.
		// This should maybe be set by a config file
		// called by the BR2D agent later.
		double stepCost = 0.0;
		double noopCost = 0.0;

		boolean incurCostOnNoop = true;
		boolean noopAllowed = true;
		int kLevel = 5; // This is the level the "smartest" agent will be.
		int tau = 3; // Parameter defines the distribution over lower levels
		// that the agent assumes.

		boolean runValueIteration = true; // set to false to run the
		// BoundedRTDP
		// agent instead on ValueIteration
		boolean runStochasticPolicyPlanner = true; // handles when policies are
		// stochastically combined

		boolean runKNotQTests = false;
		
		boolean runNine = true;

		int numTrials = 1;
		int numLearningEpisodes = 10;

		String[] gameFile = new String[] {
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals0.json", // 0
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals1.json", // 1
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals2.json", // 2
				"../MultiAgentGames/resources/worlds/LavaPits.json", // 3
				"../MultiAgentGames/resources/worlds/TwoAgentsTunnels", // 4
				"../MultiAgentGames/resources/worlds/TwoAgentsHall_3by5_2Walls.json", // 5
				"../MultiAgentGames/resources/worlds/TwoAgentsHall_3by5_noWalls.json", // 6
				"../MultiAgentGames/resources/worlds/TwoAgentsNarrowHall_1by10_noWalls.json", // 7
				"../MultiAgentGames/resources/worlds/TwoAgentsCross.json", // 8
				"../MultiAgentGames/resources/worlds/TwoAgentsBox_5by5_2Walls.json", // 9
				"../MultiAgentGames/resources/worlds/TwoAgentsBox_3by5_2Walls.json", // 10
				"../MultiAgentGames/resources/worlds/TwoAgentsFourCorners_5by5_CrossWalls.json", // 11
				"turkey", "coordination", "prisonersdilemma" }; // 12,13,14

		// Choose from a json game file or built-in option from the list above.
		String file = gameFile[6];

		// Execution timer
		long startTime = System.currentTimeMillis();

		//RewardCalculatorType rewardCalcType = RewardCalculatorType.OTHER_REGARDING;
		RewardCalculatorType rewardCalcType = RewardCalculatorType.SELFISH;

		Map<String, RewardCalculatorType> rewardCalcTypeMap = new HashMap<String, RewardCalculatorType>();
		rewardCalcTypeMap.put("agent0",RewardCalculatorType.OTHER_REGARDING_NINE);
		rewardCalcTypeMap.put("agent1",RewardCalculatorType.OTHER_REGARDING_NINE);


		Map<String, String> parameterTypes = null;
		if(runNine){
			parameterTypes= new HashMap<String, String>();

			parameterTypes.put("agent0","BADC");
			parameterTypes.put("agent1","BADC");
		}

		double coopParam = 0.1;
		double defendParam = 0.5;

		Experiment runner;
		if(rewardCalcType==null){
			runner = new Experiment(file, kLevel, stepCost,
					incurCostOnNoop, noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					rewardCalcType, coopParam, defendParam);
		}else if(parameterTypes==null){
			runner = new Experiment(file, kLevel, stepCost,
					incurCostOnNoop, noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					rewardCalcTypeMap, coopParam, defendParam);

		}else{
			runner = new Experiment(file, kLevel, stepCost,
					incurCostOnNoop, noopCost, reward, tau, runValueIteration,
					runStochasticPolicyPlanner, numTrials, noopAllowed,
					rewardCalcTypeMap, parameterTypes);
		}
		// Run k-Level
		if (runKNotQTests) {
			runner.runKLevelExperiment(Level0Type.RANDOM, numLearningEpisodes);
		} else {
			// Run Q-Learners
			// runner.runQVsCooperator(numLearningEpisodes);
			//runner.runMALearners(numLearningEpisodes, "max");
			//runner.runQLearners(numLearningEpisodes);
			String[] agentTypes = {"ABCD", "ABDC", "BACD", "BADC", "ABCd","BACd","AbCD","AbDC","AbCd"};
			runner.runQESS( numLearningEpisodes, numTrials, 1, agentTypes);

		}

		// Visualize Results
		//Visualizer v = GGVisualizer.getVisualizer(6, 6);
		//		v.addSpecificObjectPainter("agent0", new AgentPolicyObjectPainter(
		//				runner.solvedAgentPolicies.get("agent0").get(5), "agent0"));
		//		v.addSpecificObjectPainter("agent1", new AgentPolicyObjectPainter(
		//				runner.solvedAgentPolicies.get("agent1").get(5), "agent1"));

		//		SGVisualExplorer sgve = new SGVisualExplorer(runner.domain, v, runner.gameWorld.startingState());
		//		sgve.addKeyAction("w", "agent0:north");
		//		sgve.addKeyAction("a", "agent0:west");
		//		sgve.addKeyAction("s", "agent0:noop");
		//		sgve.addKeyAction("d", "agent0:east");
		//		sgve.addKeyAction("x", "agent0:south");
		//		sgve.addKeyAction("i", "agent1:north");
		//		sgve.addKeyAction("j", "agent1:west");
		//		sgve.addKeyAction("k", "agent1:noop");
		//		sgve.addKeyAction("l", "agent1:east");
		//		sgve.addKeyAction(",", "agent1:south");
		//		sgve.initGUI();
		//new ExperimentVisualizer(v, runner.getDomain(), runner.outFile);

		//List<State> states = new ArrayList<State>();
		//states.add(runner.gameWorld.startingState());

		// PolicyVisualizer vis = new PolicyVisualizer(states,
		// runner.solvedAgentPolicies.get("agent0").get(0));
		// VisPlorer vis = new VisPlorer(runner.domain, v,
		// runner.gameWorld.startingState(),
		// runner.solvedAgentPolicies.get("agent0").get(1));
		// vis.initGUI();


		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total time:  " + totalTime / 1000.0 + " s");
	}
}