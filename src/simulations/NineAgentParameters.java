package simulations;

public class NineAgentParameters extends RewardParameters {

	public NineAgentParameters() {
		super("Other reguarding, nine agents");
		
	}
	
	/**
	 * [0]*myR + [1]*otherR +[2]*max(myR-otherR, otherR-myR)+[3]*max(myR-otherR, 0)
	 * +[4]*max(0, otherR-myR)
	 */

	@Override
	public double[] getParameters(String agentType) {
		
		if(agentType.compareTo("BADC")==0){
			double[] params = {1.0,-.25,0.0,0.0, 0.0};
			return params;
		}else if(agentType.compareTo("BACD")==0){
			double[] params = {1.0,0.0,0.25,0.0, 0.0};
			return params;
		}else if(agentType.compareTo("BACxD")==0){
			double[] params = {1.0,0.0,0.0,.25, 0.0};
			return params;
		}else if(agentType.compareTo("ABDC")==0){
			double[] params = {1.0,0.0,-.25,0.0, 0.0};
			return params;
		}else if(agentType.compareTo("ABCD")==0){
			double[] params = {1.0,.25,0.0,0.0, 0.0};
			return params;
		}else if(agentType.compareTo("ABCxD")==0){
			double[] params = {1.0,0.0,0.0,-.25, 0.0};
			return params;
		}else if(agentType.compareTo("AxBDC")==0){
			double[] params = {1.0,0.0,0.0,0.0,-.25};
			return params;
		}else if(agentType.compareTo("AxBCD")==0){
			double[] params = {1.0,.25,0.0,0.25, 0.0};
			return params;
		}else if(agentType.compareTo("AxBCxD")==0){
			double[] params = {1.0,0.0,0.0,0.0, 0.0};
			return params;
		}else{
			return null;
		}
		
	}

}
