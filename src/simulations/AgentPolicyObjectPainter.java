package simulations;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.PolicyRenderLayer;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D.PolicyGlyphRenderStyle;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;

public class AgentPolicyObjectPainter implements ObjectPainter {

	PolicyRenderLayer pLayer;
	Policy policy;
	private PolicyGlyphPainter2D spp;

	public AgentPolicyObjectPainter(Policy policy, String agentName) {
		this.policy = policy;

		this.spp = new PolicyGlyphPainter2D();
		spp.setXYAttByObjectReference(agentName, GridWorldDomain.ATTX,
				agentName, GridWorldDomain.ATTY);

		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONNORTH,
				new ColoredArrowActionGlyph(0));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONSOUTH,
				new ColoredArrowActionGlyph(1));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONEAST,
				new ColoredArrowActionGlyph(2));
		spp.setActionNameGlyphPainter(GridWorldDomain.ACTIONWEST,
				new ColoredArrowActionGlyph(3));
		spp.setActionNameGlyphPainter("noop", new ColoredArrowActionGlyph(4));
		 spp.setRenderStyle(PolicyGlyphRenderStyle.DISTSCALED);

		this.pLayer = new PolicyRenderLayer(null, spp, policy);
	}

	@Override
	public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
			float cWidth, float cHeight) {


		List<ActionProb> actionProbs = this.policy
				.getActionDistributionForState(s);

		int actionNum = 0;

		for (ActionProb actionProb : actionProbs) {
			if (actionProb.ga.actionName().equals(GridWorldDomain.ACTIONNORTH)) {
				actionNum = 0;

			} else if (actionProb.ga.actionName().equals(
					GridWorldDomain.ACTIONSOUTH)) {
				actionNum = 1;
			} else if (actionProb.ga.actionName().equals(
					GridWorldDomain.ACTIONEAST)) {
				actionNum = 2;
			} else if (actionProb.ga.actionName().equals(
					GridWorldDomain.ACTIONWEST)) {
				actionNum = 3;
			} else if (actionProb.ga.actionName().equals("noop")) {
				actionNum = 4;
			}

			spp.setActionNameGlyphPainter(actionProb.ga.actionName(),
					new ColoredArrowActionGlyph(actionNum,
							actionProb.pSelection));
			System.out.println(actionProb.ga.actionName()+": "+actionProb.pSelection);

		}

		pLayer.setSpp(spp);

		// Update state.
		List<State> statesToVisualize = new ArrayList<State>();
		statesToVisualize.add(s);
		pLayer.setStateValuesToVisualize(statesToVisualize);

		// Render the policy render layer.
		pLayer.render(g2, cWidth, cHeight);
	}

}
