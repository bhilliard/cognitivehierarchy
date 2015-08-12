package simulations;

import java.util.List;
import java.util.Map;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.stochasticgames.gridgame.GridGame;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.SGDomain;

public class SimpleCooperativeStrategy extends Agent {



	public SimpleCooperativeStrategy(SGDomain domain) {
		this.domain = domain;
	}

	@Override
	public void gameStarting() {
		

	}

	@Override
	public GroundedSingleAction getAction(State s) {
		String an = GridWorldDomain.CLASSAGENT;


		//find what agent we are
		int agentNum = 0;
		int otherAgentNum = 1;
		if(getAgentName().contains("1")){
			agentNum = 1;
			otherAgentNum = 0;
		}

		ObjectInstance thisAgent = null;
		ObjectInstance otherAgent = null;
		List<ObjectInstance> agentList  = s.getObjectsOfClass(an);
		for (ObjectInstance o : agentList){
			if(o.getStringValForAttribute(GridGame.ATTPN).contains(Integer.toString(agentNum))){
				thisAgent = o;
			}else{
				otherAgent = o;
			}
		}

		int ax=0;
		int ay=0;
		int ox=0;
		int oy=0;
		if(thisAgent != null){
			//get agent x,y position
			ax = thisAgent.getIntValForAttribute(GridWorldDomain.ATTX);
			ay = thisAgent.getIntValForAttribute(GridWorldDomain.ATTY);

		}
		if(otherAgent != null){
			//get agent x,y position
			ox = otherAgent.getIntValForAttribute(GridWorldDomain.ATTX);
			oy = otherAgent.getIntValForAttribute(GridWorldDomain.ATTY);

		}


		//get goal locations
		//loop over all goal objects and find the closest to the agent
		List<ObjectInstance> objects = s.getObjectsOfClass(GridGame.CLASSGOAL);
		int agx=0;
		int agy=0;
		int ogx=0;
		int ogy=0;
		for(ObjectInstance oi : objects){

			//System.out.println("AgentNum: "+agentNum+" GT: "+oi.getIntValForAttribute("gt"));
			if(oi.getIntValForAttribute("gt")==agentNum+1){

				agx = oi.getIntValForAttribute(GridWorldDomain.ATTX);
				agy = oi.getIntValForAttribute(GridWorldDomain.ATTY);

			}else if(oi.getIntValForAttribute("gt")==otherAgentNum+1){

				ogx = oi.getIntValForAttribute(GridWorldDomain.ATTX);
				ogy = oi.getIntValForAttribute(GridWorldDomain.ATTY);

			}

		}

		GroundedSingleAction action;


		//above goal
		if(ax==agx && ay-1==agy){
			//go down
			action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONSOUTH), "");
		}else if(ax==agx && ay+1==agy) {//below goal
			//go up
			action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONNORTH), "");
		}else if(ax==ogx && ay==ogy){//at start spot
			if((ox==agx && oy==agy)||(oy-1==agy && (ox+2==agx||ox-2==agx))){//if other agent in start spot or above and toward center
				//down
				//System.out.println(domain);
				
				action = new GroundedSingleAction(getAgentName(), 
						domain.
						getSingleAction(GridGame.ACTIONSOUTH),
						"");
			}else{
				//stay
				action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONNOOP), "");
			}
		}else if(ax==ogx && ay+1==ogy){//below start spot
			if(ox==agx && oy-1==agy){//other agent above our goal
				//go towards goal
				if(agx<ogx){
					//go left
					action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONWEST), "");
				}else{
					//go right
					action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONEAST), "");
				}
			}else{
				//go up
				action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONNORTH), "");
			}
		}else{
			//go towards goal
			if(agx<ogx){
				//go left
				action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONWEST), "");
			}else{
				//go right
				action = new GroundedSingleAction(getAgentName(), domain.getSingleAction(GridGame.ACTIONEAST), "");
			}
		}
		return action;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction,
			Map<String, Double> jointReward, State sprime, boolean isTerminal) {
		

	}

	@Override
	public void gameTerminated() {
		

	}


}
