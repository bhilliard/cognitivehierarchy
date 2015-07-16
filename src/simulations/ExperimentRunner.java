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


/***
 * ExperimentRunner handles learning policies of agents from level-0 to specified 
 * level k. The policies are created in order allowing the next level to learn.
 * 
 * @author betsy hilliard betsy@cs.brown.edu
 *
 */
public class ExperimentRunner {

	//Map from agent name (specific to location in game) to level to policy solved for
	private Map<String,Map<Integer, Policy>> solvedAgentPolicies;

	private GridGame gg = new GridGame();
	
	private Agent oponent;

	private SGDomain d;
	
	private double stepCost = -1.0;

	public ExperimentRunner(){
		oponent = new RandomAgent();
		
		d = (SGDomain)gg.generateDomain();
		solvedAgentPolicies = new HashMap<String,Map<Integer, Policy>>();

	}

	/**
	 * runExperiment runs 
	 * @param gameType
	 * @param reward
	 * @param kLevel
	 * @param fileName
	 * @return
	 */
	public List<GameAnalysis> runExperiment(String gameType, double reward, int kLevel, double tau, String fileName, boolean runValueItteration){
		List<GameAnalysis> gas = new ArrayList<GameAnalysis>();
		
		World gameWorld;

		StateHashFactory hashFactory = new DiscreteStateHashFactory();

		//loop over all lower levels to learn their policies
		for(int k = 0;k<kLevel;k++){
			
			//loop over join orders so that we learn as both agent0 and agent1
			//we need this info for both agent locations so that we can learn the next level up
			for(int otherFirst = 0;otherFirst<=1;otherFirst++){
				System.out.println("LEVEL: "+k+" OTHERFIRST: "+otherFirst);
				
				//if we didn't specify a file, run a hardcoded game based on the name we gave
				if(fileName.compareToIgnoreCase("NOFILE")==0){

					State s = GridGame.getCorrdinationGameInitialState(d); 
					if(gameType.compareToIgnoreCase("turkey")==0){
						s = GridGame.getTurkeyInitialState(d);
					}else if(gameType.compareToIgnoreCase("coordination")==0){
						s = GridGame.getCorrdinationGameInitialState(d);
					}else if(gameType.compareToIgnoreCase("prisonersdilemma")==0 || gameType.compareToIgnoreCase("pd")==0){
						s = GridGame.getPrisonersDilemmaInitialState(d);
					}

					//create the Joint Action Model and add to the domain d
					JointActionModel jam = new GridGameStandardMechanics(d);
					d.setJointActionModel(jam);

					//create a Joint Reward Function and 
					JointReward jr = new GridGame.GGJointRewardFunction(d, stepCost, reward, reward, false);
					TerminalFunction tf = new GridGame.GGTerminalFunction(d);
					SGStateGenerator sg = new ConstantSGStateGenerator(s);

					gameWorld = new World(d, jr, tf, sg);
				}else{
					gameWorld = GridGameWorldLoader.loadWorld(fileName);

					d = gameWorld.getDomain();

				}



				BestResponseToDistributionAgent brAgent = new BestResponseToDistributionAgent(d, hashFactory, reward, runValueItteration);
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
				String brAgentName = brAgent.getAgentName();
				System.out.println("Oponent's name: "+oponentName+ " BR2D's name: "+brAgentName);
				
				//if level 0, store policy because it's the random agent
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

				//This is for debugging
//				for(String agentName : solvedAgentPolicies.keySet()){
//					System.out.println("Num levels for "+agentName+": "+solvedAgentPolicies.get(agentName).size());
//				}


				//construct policy map to pass to Best Response agent
				// NOTE: If we have more than two opponents, we would need a loop over opponents here...or maybe earlier
				Map<String, Map<Integer,Policy>> allOtherAgentPolicies = new HashMap<String, Map<Integer,Policy>>();
				HashMap<Integer, Policy> levelMap = new HashMap<Integer, Policy>();
				for(int lev = 0;lev<=k;lev++){
					levelMap.put(lev, solvedAgentPolicies.get(oponentName).get(lev));
					System.out.println("getting: "+oponentName+", level: "+lev);
				}
				
				
				//this is the policies we want to use for learning for this agent
				allOtherAgentPolicies.put(oponentName, levelMap);

				//Now create a distribution for this agent
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

				brAgent.setOtherAgentPolicyMaps(allOtherAgentPolicies, distributionOverAllOtherAgentPolicies);

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
				

				//add the game analysis to the records 
				gas.add(ga);

				System.out.println("Level: "+k+" BR Agent Name: "+brAgent.getAgentName());

			
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

		double reward = 50.0; //Set this to set the rewards for goals. 
							  //This should maybe be set by a config file called by the BR2D agent later.
		int kLevel = 1; //This is the level the "smartest" agent will be.
		int tau = 2; //Parameter defines the distribution over lower levels that the agent assumes.
		
		boolean runValueIteration = true; // set to false to run the BoundedRTDP agent instead on ValueIteration
		
		String file = "/Users/betsy/grid_games/worlds/TwoAgentsHall_3by5_2Walls.json"; //"NOFILE";
		
		//if file is set to "NOFILE" you can specify a common, built-in game
		String gameType = "turkey"; // Built in options: "turkey", "prisonersdilema" "coordination"
		
		//OTHER FILES
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals0.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals1.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTwoGoals2.json"
		//"/Users/betsy/grid_games/worlds/LavaPits.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsTunnels"
		//"/Users/betsy/grid_games/worlds/TwoAgentsHall_3by5_2Walls.json"
		//"/Users/betsy/grid_games/worlds/TwoAgentsHall_3by5_noWalls.json"
		

		ExperimentRunner runner = new ExperimentRunner();
		List<GameAnalysis> gas = runner.runExperiment(gameType, reward, kLevel,tau,file, runValueIteration);

		//runs the visualizer for all agent games
		Visualizer v = GGVisualizer.getVisualizer(6,6);
		GameSequenceVisualizer gsv = new GameSequenceVisualizer(v,runner.getDomain(),gas);

	}

}
