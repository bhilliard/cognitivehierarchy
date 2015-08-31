package simulations;

public abstract class RewardParameters {
	
	private String functionType;

	public RewardParameters(String functionType) {
		this.functionType = functionType;
		
	}
	
	public abstract double[] getParameters(String agentType);
	
	public String getFuctionType(){
		return functionType;
	}

}
