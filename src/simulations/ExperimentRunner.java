package simulations;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.glass.ui.Window.Level;

import networking.common.GridGameWorldLoader;
import behavior.SpecifyNoopCostRewardFunction;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.ValueFunctionInitialization.ConstantValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.agents.BestResponseToDistributionAgent;
import burlap.behavior.stochasticgame.agents.BRDPlanThenCombinePoliciesAgent;
import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.behavior.stochasticgame.agents.SetStrategyAgent;
import burlap.behavior.stochasticgame.agents.TransparentSetStrategyAgent;
import burlap.behavior.stochasticgame.agents.naiveq.SGNaiveQLAgent;
import burlap.behavior.stochasticgame.saconversion.RandomSingleAgentPolicy;
import burlap.behavior.stochasticgame.saconversion.SingleToMultiPolicy;
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
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.CachedPolicy;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;

/***
 * ExperimentRunner handles learning policies of agents from level-0 to
 * specified level k. The policies are created in order allowing the next level
 * to learn.
 * 
 * @author betsy hilliard betsy@cs.brown.edu
 *
 */
public class ExperimentRunner {

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
	private World gameWorld;

	// Experiment parameters
	private int numTrials, numLearningEpisodes;
	private double[][][] scores;
	private boolean optimisticInit, boltzmannExplore, saveLearning;
	private double temp;

	private final double DISCOUNT_FACTOR = 0.99, LEARNING_RATE = 0.01;
	private final int TIMEOUT = 100;

	public enum Level0Type {
		RANDOM, Q, NASH_CD, NASH_B
	}

	public ExperimentRunner(String gameFile, int kLevel, double stepCost,
			boolean incurCostOnNoOp, double noopCost, double reward,
			double tau, boolean runValueIteration,
			boolean runStochasticPolicyPlanner, int numTrials,
			boolean noopAllowed) {

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
	}
	public List<GameAnalysis> runKLevelExperiment(Level0Type level0Type, int level0LearningEpisodes) {
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

		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String outDir = "../" + ft.format(date) + "/";
		
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
						runValueIteration);

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
					lowerPolicy = generateLevel0Policy(level0Type, opponentName, outDir);
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
					levelMap.put(lev,
							solvedAgentPolicies.get(opponentName).get(lev));
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

		this.outFile = runCompetition(solvedAgentPolicies, kLevel, numTrials, outDir);
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

	private Policy generateLevel0Policy(Level0Type type, String opponentName, String outDir) {
		switch (type) {
		case RANDOM:
			return new RandomSingleAgentPolicy(opponentName,
					domain.getSingleActions());
		case Q:
			Map<String, Policy> policyMap = runLearning(this.numLearningEpisodes, outDir);
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

		this.boltzmannExplore = false;

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

		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String outDir = "../" + ft.format(date) + "/";

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

			ga = this.gameWorld.runGame(TIMEOUT);

			gas.add(ga);

			// List<Map<String, Double>> jointRewards = ga.getJointRewards();
			//
			// for (Map<String, Double> rewards : jointRewards) {
			// for (String agentKey : rewards.keySet()) {
			// if (agentReward.containsKey(agentKey)) {
			// agentReward.put(agentKey, agentReward.get(agentKey)
			// + rewards.get(agentKey));
			// } else {
			// agentReward.put(agentKey, rewards.get(agentKey));
			// }
			// }
			// }
			// for (String keyName : agentReward.keySet()) {
			// agentReward.put(keyName, agentReward.get(keyName)
			// / (1.0 * this.numTrials));
			// }

			System.out.println(ga.getJointRewards());

			String outFile = outDir + "Green_Q" + "_Blue_Q" + "/" + "GQ" + "BQ"
					+ "_Trial_" + i;

			ga.writeToFile(outFile, sp);

		}

		this.outFile = outDir;
		return outDir;
	}

	public String runQVsCooperator(int numEpisodes) {
		this.numLearningEpisodes = numEpisodes;
		SGNaiveQLAgent opponent;
		StateHashFactory hashFactory = new DiscreteStateHashFactory();
		// Map<String, Double> agentReward = new HashMap<String, Double>();
		ArrayList<GameAnalysis> gas = new ArrayList<GameAnalysis>();

		SimpleCooperativeStrategy agent = new SimpleCooperativeStrategy(domain);
		opponent = new SGNaiveQLAgent(domain, this.DISCOUNT_FACTOR,
				this.LEARNING_RATE, hashFactory);

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
			this.gameWorld.runGame(TIMEOUT);

		}

		Date date = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		String outDir = "../" + ft.format(date) + "/";

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

			GameAnalysis ga = this.gameWorld.runGame(TIMEOUT);

			gas.add(ga);

			// List<Map<String, Double>> jointRewards = ga.getJointRewards();
			//
			// for (Map<String, Double> rewards : jointRewards) {
			// for (String agentKey : rewards.keySet()) {
			// if (agentReward.containsKey(agentKey)) {
			// agentReward.put(agentKey, agentReward.get(agentKey)
			// + rewards.get(agentKey));
			// } else {
			// agentReward.put(agentKey, rewards.get(agentKey));
			// }
			// }
			// }
			// for (String keyName : agentReward.keySet()) {
			// agentReward.put(keyName, agentReward.get(keyName)
			// / (1.0 * this.numTrials));
			// }

			StateParser sp = new StateJSONParser(domain);
			System.out.println(ga.getJointRewards());
			String outFile = outDir + "Green_Q" + "_Blue_Q" + "/" + "GQ" + "BQ"
					+ "_Trial_" + i;

			ga.writeToFile(outFile, sp);

		}

		this.outFile = outDir;
		return outDir;
	}

	public void writeMetaData() {
		PrintWriter writer, writerGreen, writerBlue, current;
		try {
			writer = new PrintWriter(outFile + "meta.txt", "UTF-8");
			writerGreen = new PrintWriter(outFile + "scoresGreen", "UTF-8");
			writerBlue = new PrintWriter(outFile + "scoresBlue", "UTF-8");
			String noop;
			if (this.incurCostOnNoop)
				noop = Double.toString(this.noopCost);
			else
				noop = "0.0";

			writer.println("Game File/Type: " + this.gameFile);
			writer.println("k-Level: " + this.kLevel);
			writer.println("Tau: " + this.tau);
			writer.println("Reward Value: " + this.reward);
			writer.println("Using VI: " + this.runValueIteration);
			writer.println("Using Stochastic Policies: "
					+ this.runStochasticPolicyPlanner);
			writer.println("Step Cost: " + this.stepCost);
			writer.println("Noop: " + this.noopAllowed);
			if (this.noopAllowed)
				writer.println("Noop Cost: " + noop);
			writer.println("Number of trials: " + this.numTrials);
			writer.println("Game timeout: " + this.TIMEOUT + " moves");
			writer.println("Score Matrices:");
			for (int m = 0; m < 2; m++) {
				if (m == 0) {
					writer.println("Green Agent:");
					current = writerGreen;
				} else {
					writer.println("Blue Agent:");
					current = writerBlue;
				}
				for (int i = 0; i < this.scores.length; i++) {
					writer.print(i + ": ");
					for (int j = 0; j < this.scores.length; j++) {
						writer.print(this.scores[i][j][m] + " ");
						current.print(this.scores[i][j][m] + " ");
					}
					writer.println();
				}
				writer.println();
			}
			writer.close();
			writerGreen.close();
			writerBlue.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void writeMetaDataForQLearners() {
		PrintWriter writer;
		try {
			writer = new PrintWriter(outFile + "meta.txt", "UTF-8");
			String noop;
			if (this.incurCostOnNoop)
				noop = Double.toString(this.noopCost);
			else
				noop = "0.0";

			writer.println("Game File/Type: " + this.gameFile);
			writer.println("Reward Value: " + this.reward);
			writer.println("Step Cost: " + this.stepCost);
			writer.println("Noop: " + this.noopAllowed);
			if (this.noopAllowed)
				writer.println("Noop Cost: " + noop);
			writer.println("Number of learning episodes: "
					+ this.numLearningEpisodes);
			writer.println("Number of execution trials: " + this.numTrials);
			writer.println("Discount factor: " + this.DISCOUNT_FACTOR);
			writer.println("Learning rate: " + this.LEARNING_RATE);
			writer.println("Optimistic: " + this.optimisticInit);
			writer.println("Boltzmann Exploration: " + this.boltzmannExplore);
			if (this.boltzmannExplore)
				writer.println("Boltzmann Temperature: " + this.temp);
			writer.println("Game timeout: " + this.TIMEOUT + " moves");

			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
			this.gameWorld = GridGameWorldLoader.loadWorld(this.gameFile,
					this.stepCost, this.reward, this.incurCostOnNoop,
					this.noopCost);
			this.domain = this.gameWorld.getDomain();
		}
	}

	public static void main(String[] args) {

		double reward = 50.0; // Set this to set the rewards for goals.
		// This should maybe be set by a config file
		// called by the BR2D agent later.
		double stepCost = -1.0;
		double noopCost = -0.75;

		boolean incurCostOnNoop = true;
		boolean noopAllowed = true;
		int kLevel = 5; // This is the level the "smartest" agent will be.
		int tau = 2; // Parameter defines the distribution over lower levels
		// that the agent assumes.

		boolean runValueIteration = false; // set to false to run the
		// BoundedRTDP
		// agent instead on ValueIteration
		boolean runStochasticPolicyPlanner = true; // handles when policies are
		// stochastically combined

		boolean runKNotQTests = true;

		int numTrials = 100;
		int numLearningEpisodes = 10000;

		String[] gameFile = new String[] {
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals0.json",
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals1.json",
				"../MultiAgentGames/resources/worlds/TwoAgentsTwoGoals2.json",
				"../MultiAgentGames/resources/worlds/LavaPits.json",
				"../MultiAgentGames/resources/worlds/TwoAgentsTunnels",
				"../MultiAgentGames/resources/worlds/TwoAgentsHall_3by5_2Walls.json",
				"../MultiAgentGames/resources/worlds/TwoAgentsHall_3by5_noWalls.json",
				"turkey", "coordination", "prisonersdilemma" };

		// Choose from a json game file or built-in option from the list above.
		String file = gameFile[6];

		// Execution timer
		long startTime = System.currentTimeMillis();

		ExperimentRunner runner = new ExperimentRunner(file, kLevel, stepCost,
				incurCostOnNoop, noopCost, reward, tau, runValueIteration,
				runStochasticPolicyPlanner, numTrials, noopAllowed);

		// Run k-Level
		if (runKNotQTests) {
			runner.runKLevelExperiment(Level0Type.RANDOM, numLearningEpisodes);
			runner.writeMetaData();
		} else {
			// Run Q-Learners
			// runner.runQVsCooperator(numLearningEpisodes);
			runner.runQLearners(numLearningEpisodes);
			runner.writeMetaDataForQLearners();
		}

		// Visualize Results
		Visualizer v = GGVisualizer.getVisualizer(6, 6);
		new ExperimentVisualizer(v, runner.getDomain(), runner.outFile);

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total time:  " + totalTime / 1000.0 + " s");
	}
}