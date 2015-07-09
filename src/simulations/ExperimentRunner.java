package simulations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import networking.common.GridGameWorldLoader;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.stochasticgame.GameAnalysis;
import burlap.behavior.stochasticgame.GameSequenceVisualizer;
import burlap.behavior.stochasticgame.agents.BestResponseToDistributionAgent;
import burlap.behavior.stochasticgame.agents.RandomAgent;
import burlap.behavior.stochasticgame.saconversion.RandomSingleAgentPolicy;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.domain.stochasticgames.gridgame.GridGameStandardMechanics;
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



public class ExperimentRunner {

	Map<String,Map<Integer, Policy>> solvedAgentPolicies;

	GridGame gg = new GridGame();
	
	Agent oponent;
	Policy previousPolicy;

	SGDomain d;

	public ExperimentRunner(){
		oponent = new RandomAgent();
		previousPolicy = null;
		d = (SGDomain)gg.generateDomain();
		solvedAgentPolicies = new HashMap<String,Map<Integer, Policy>>();

	}

	/**
	 * runExperiment runs 
	 * @param gameType
	 * @param reward
	 * @param maxLevel
	 * @param fileName
	 * @return
	 */
	public List<GameAnalysis> runExperiment(String gameType, int reward, int maxLevel, double tau, String fileName){
		List<GameAnalysis> gas = new ArrayList<GameAnalysis>();
		
		World gameWorld;

		StateHashFactory hashFactory = new DiscreteStateHashFactory();

		for(int k = 0;k<=maxLevel;k++){
			for(int otherFirst = 0;otherFirst<=1;otherFirst++){
				System.out.println("LEVEL: "+k+" OTHERFIRST: "+otherFirst);
				
				if(fileName.compareToIgnoreCase("NOFILE")==0){

					State s = GridGame.getCorrdinationGameInitialState(d); 
					if(gameType.compareToIgnoreCase("turkey")==0){
						s = GridGame.getTurkeyInitialState(d);
					}else if(gameType.compareToIgnoreCase("coordination")==0){
						s = GridGame.getCorrdinationGameInitialState(d);
					}else if(gameType.compareToIgnoreCase("prisonersdilemma")==0 || gameType.compareToIgnoreCase("pd")==0){
						s = GridGame.getPrisonersDilemmaInitialState(d);
					}

					JointActionModel jam = new GridGameStandardMechanics(d);
					d.setJointActionModel(jam);

					JointReward jr = new GridGame.GGJointRewardFunction(d, -1, 50.0, 50.0, false);
					TerminalFunction tf = new GridGame.GGTerminalFunction(d);
					SGStateGenerator sg = new ConstantSGStateGenerator(s);

					gameWorld = new World(d, jr, tf, sg);
				}else{
					gameWorld = GridGameWorldLoader.loadWorld(fileName);

					d = gameWorld.getDomain();

				}



				BestResponseToDistributionAgent brAgent = new BestResponseToDistributionAgent(d, hashFactory);
				if(k>0){
					//oponent = new BestResponseToDistributionAgent(d, hashFactory);
					oponent= new RandomAgent();
				}


				if(otherFirst==1){
					oponent.joinWorld(gameWorld, new AgentType(GridGame.CLASSAGENT, d.getObjectClass(GridGame.CLASSAGENT), d.getSingleActions()));
					brAgent.joinWorld(gameWorld, new AgentType(GridGame.CLASSAGENT, d.getObjectClass(GridGame.CLASSAGENT), d.getSingleActions()));
				}else{
					brAgent.joinWorld(gameWorld, new AgentType(GridGame.CLASSAGENT, d.getObjectClass(GridGame.CLASSAGENT), d.getSingleActions()));
					oponent.joinWorld(gameWorld, new AgentType(GridGame.CLASSAGENT, d.getObjectClass(GridGame.CLASSAGENT), d.getSingleActions()));
				}

				//construct the other agent policies


				List<SingleAction> actions = d.getSingleActions();

				Policy lowerPolicy;

				String oponentName = oponent.getAgentName();
				System.out.println("Oponent's name: "+oponentName);
				String brAgentName = brAgent.getAgentName();
				System.out.println("BR's name: "+brAgentName);
				//if level 0, store policy
				if(k==0){
					lowerPolicy = new RandomSingleAgentPolicy(oponentName, actions);
					Map<Integer,Policy> agentPolicies;
					//if no policies for this name exist, store policy
					if(!solvedAgentPolicies.containsKey(oponentName)){
						agentPolicies = new HashMap<Integer,Policy>();
						agentPolicies.put(k, lowerPolicy);
					}else{
						
						agentPolicies = solvedAgentPolicies.get(oponentName);
						System.out.println("seen this before: agentPolicies keys: "+agentPolicies.keySet());
						agentPolicies.put(k, lowerPolicy);
						System.out.println("seen this after: agentPolicies keys: "+agentPolicies.keySet());
					}
					solvedAgentPolicies.put(oponentName, agentPolicies);
					printPolicyCollection();
					//otherwise, pull opponent's policy from previous level
				}else{
					lowerPolicy = solvedAgentPolicies.get(oponentName).get(k-1);
					System.out.println("Pulling lower Policy: "+ lowerPolicy);
				}

				for(String agentName : solvedAgentPolicies.keySet()){
					System.out.println("Num levels for "+agentName+": "+solvedAgentPolicies.get(agentName).size());
				}


				//construct policy map to pass to Best Response agent
				Map<String, Map<Integer,Policy>> allOtherAgentPolicies = new HashMap<String, Map<Integer,Policy>>();
				HashMap<Integer, Policy> levelMap = new HashMap<Integer, Policy>();
				for(int lev = 0;lev<=k;lev++){
					levelMap.put(lev, solvedAgentPolicies.get(oponentName).get(lev));
					System.out.println("getting: "+oponentName+", level: "+lev);
				}
				//				for(int lev = k;lev>=k;lev--){
				//					System.out.println("Adding: "+solvedAgentPolicies.get(oponentName).get(lev)+" opp name: "+oponentName+" lev: "+lev);
				//					levelMap.put(lev, solvedAgentPolicies.get(oponentName).get(lev));
				//				}
				allOtherAgentPolicies.put(oponentName, levelMap);


				Map<String, Map<Integer,Double>> distributionOverAllOtherAgentPolicies  = new HashMap<String, Map<Integer,Double>>();
				HashMap<Integer,Double> distribution = new HashMap<Integer,Double>();
				double facSoFar = 1;
				for(int lev = 0;lev<=k;lev++){
					//System.out.println("dist value: "+(double)allOtherAgentPolicies.get(oponentName).size());
					//the distribution should be based on lev and maxLevel and parameter
					//
					double f_k = 1;
					
					if(lev>0){
						facSoFar*=lev;
						f_k = (Math.pow(Math.E, tau*-1)*Math.pow(tau, lev))/facSoFar;
					}
					System.out.println("Dist: "+lev+" weight: "+f_k);
					//distribution.put(lev, 1.0/(double)allOtherAgentPolicies.get(oponentName).size());
					distribution.put(lev, f_k);
					
				}
				
				distributionOverAllOtherAgentPolicies.put(oponentName,distribution);

				brAgent.setOtherAgentPolicyMap(allOtherAgentPolicies, distributionOverAllOtherAgentPolicies);

				//gameWorld.addWorldObserver(ob);
				System.out.println("running game");
				GameAnalysis ga = gameWorld.runGame();
				



				Map<Integer,Policy> agentPolicies;
				if(!solvedAgentPolicies.containsKey(brAgentName)){
					agentPolicies = new HashMap<Integer,Policy>();
					agentPolicies.put(k+1, brAgent.getPolicy());
				}else{
					agentPolicies = solvedAgentPolicies.get(brAgentName);
					agentPolicies.put(k+1, brAgent.getPolicy());
				}
				
				System.out.println("Added: "+brAgent.getPolicy()+" br name: "+brAgent.getAgentName()+" lev: "+(k+1));
				solvedAgentPolicies.put(brAgentName, agentPolicies);
				printPolicyCollection();
				
				
				//System.out.println("SAP size: "+solvedAgentPolicies.size());
				for(String agentName : solvedAgentPolicies.keySet()){
					//System.out.println("Num levels for "+agentName+": "+solvedAgentPolicies.get(agentName).size());
				}
				

				gas.add(ga);

				System.out.println("Level: "+k+" BR Agent Name: "+brAgent.getAgentName());

				//oponent = brAgent;
				//previousPolicy = brAgent.getPolicy();



			}
		}

		return gas;
	}
	
	private void printPolicyCollection(){
		for(String a : solvedAgentPolicies.keySet()){
			System.out.println("Agent "+a+": ");
			for(Integer i : solvedAgentPolicies.get(a).keySet()){
				System.out.println("lev: "+i+": "+solvedAgentPolicies.get(a).get(i));
			}
		}
		System.out.println();
	}

	private SGDomain getDomain() {
		return d;
	}

	public static void main(String[] args) {

		int reward = 100;
		int maxLevelOthers = 0;
		String gameType = "pd";
		String file = "/Users/betsy/grid_games/worlds/TwoAgentsHall_3by5_2Walls.json"; //"NOFILE";
		
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals0.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals1.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals2.json"
		//"/Users/betsy/grid_games/worlds/LavaPits.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTunnels"
		//


		ExperimentRunner runner = new ExperimentRunner();
		List<GameAnalysis> gas = runner.runExperiment(gameType, reward, maxLevelOthers,2,file);

		Visualizer v = GGVisualizer.getVisualizer(6,6);
		GameSequenceVisualizer gsv = new GameSequenceVisualizer(v,runner.getDomain(),gas);

	}

}
