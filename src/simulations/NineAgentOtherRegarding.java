package simulations;

import java.util.Map;

import burlap.behavior.stochasticgame.saconversion.RewardCalculator;

public class NineAgentOtherRegarding extends RewardCalculator {

	private String agentType;
	private RewardParameters params;
	double [] agentsParams;

	public NineAgentOtherRegarding(String agentType) {
		super("NineAgentOtherRegarding");
		this.params = new NineAgentParameters();
		this.agentType = agentType;
		agentsParams = params.getParameters(agentType);
		
	}
	
	/**
	 * [0]*myR + [1]*otherR +[2]*max(myR-otherR, otherR-myR)+[3]*max(myR-otherR, 0)
	 * +[4]*max(0, otherR-myR)
	 */

	@Override
	public double getReward(double myReward, double otherReward) {

		if (otherReward > 0 || myReward > 0) {
			return agentsParams[0]*myReward 
					+ agentsParams[1]*otherReward 
					+ agentsParams[2]*Math.max(myReward-otherReward, otherReward-myReward) 
					+ agentsParams[3]*Math.max(myReward-otherReward, 0.0)
					+ agentsParams[4]*Math.max(0.0, otherReward-myReward) ;
		} else {
			return myReward;
		}

	}

	@Override
	public String functionToString() {
		return "Agent Type: "+agentType+" "+agentsParams[0]+"*myReward + "
				+agentsParams[1]+"*otherReward + "
				+agentsParams[2]+"*Math.max(myReward-otherReward, otherReward-myReward)"
					+ agentsParams[3]+"*Math.max(myReward-otherReward, 0.0)"
					+ agentsParams[4]+" *Math.max(0.0, otherReward-myReward) ";
	}

	@Override
	public double getReward(String agentNameIn,
			Map<String, Double> realRewards) {
		double otherReward =0;
		//System.out.println("RR: "+realRewards);
		//System.out.println("AN: "+agentNameIn);
		double myReward = realRewards.get(agentNameIn);
		for(String oAgentName : realRewards.keySet()){
			
			if(oAgentName!=agentNameIn){
				otherReward+=realRewards.get(oAgentName);
			}
			
		}
		if (otherReward > 0 || myReward > 0) {
			return agentsParams[0]*myReward 
					+ agentsParams[1]*otherReward 
					+ agentsParams[2]*Math.max(myReward-otherReward, otherReward-myReward) 
					+ agentsParams[3]*Math.max(myReward-otherReward, 0.0)
					+ agentsParams[4]*Math.max(0.0, otherReward-myReward) ;
		} else {
			return myReward;
		}
	}

}

