package org.sevoh;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorContradiction;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

/**
 * This is the source code of the approach described in: "A Portfolio-Based
 * Approach to Select Efficient Variable Ordering Heuristics for Constraint
 * Satisfaction Problems", Proc. of CP'22.
 * 
 * Our experiments were run in Choco 4.10.6.
 * 
 * Here is an example to configure your search with SEVOH
  	int failLimit = 100;
	int roundLimit = 100;
	int sptr = roundLimit * 4;
  	AbstractStrategy<IntVar> voh = new SEVOH(dvars, roundLimit, failLimit, 
  					new FailureBased(dvars, seed, 2, new IntDomainMin()),
					new DomOverWDegRef(dvars, seed, new IntDomainMin()),
					new ConflictHistorySearch(dvars, seed, new IntDomainMin()), 
					new ActivityBased(dvars, false, seed));//We did not run the sampling phase of ABS in our experiments.
  	solver.setRestarts(count -> solver.getFailCount() >= count,
					new GeometricalCutoffStrategy4SEVOH(failLimit, sptr, 10, 1.1), Integer.MAX_VALUE);
	solver.setSearch(voh);
 * 
 * @author Hongbo Li and Yaling Wu
 */

public class SEVOH extends AbstractStrategy<IntVar> implements IMonitorRestart, IMonitorContradiction {

	private final AbstractStrategy<IntVar>[] candidateVOHs;
	private final int candidateNum;
	private final int roundLimit;
	private final int failLimit;

	private boolean probing;
	private int currentRoundNum;
	private int indexOfCurrentVOH;// the index of the candidate using in current
									// sample
	private int maxDepth;// recording the max depth in current sample
	private int totalFailDepth;// recording the total failure depth in current
								// sample to calculate the average
								// failure depth of current sample

	private int indexOfBestVOH;// the index of the selected candidate
	private double[] max_min;// the score combining maxD with minFD
	private double[] minFD;
	private int[] maxD;

	public SEVOH(IntVar[] vars, int rl, int fl, AbstractStrategy<IntVar>... strategies) {
		super(vars);
		this.candidateVOHs = strategies;
		candidateNum = candidateVOHs.length;
		probing = true;
		currentRoundNum = 0;
		indexOfCurrentVOH = 0;
		maxDepth = 0;
		totalFailDepth = 0;
		indexOfBestVOH = -1;
		max_min = new double[candidateNum];
		maxD = new int[candidateNum];
		minFD = new double[candidateNum];
		for (int i = 0; i < candidateNum; i++) {
			minFD[i] = Double.MAX_VALUE;
		}
		roundLimit = rl;
		failLimit = fl;
		vars[0].getModel().getSolver().plugMonitor(this);
	}

	@Override
	public boolean init() {
		boolean ok = true;
		for (int i = 0; i < candidateNum; i++) {
			ok &= candidateVOHs[i].init();
		}
		return ok;
	}

	public void afterRestart() {
		if (probing) {
			if (currentRoundNum < roundLimit) {
				if (maxD[indexOfCurrentVOH] < maxDepth) {
					maxD[indexOfCurrentVOH] = maxDepth;
				}
				double aveFD = (double) totalFailDepth / failLimit;
				if (aveFD < minFD[indexOfCurrentVOH]) {
					minFD[indexOfCurrentVOH] = aveFD;
				}
			}
			maxDepth = 0;
			totalFailDepth = 0;

			if (indexOfCurrentVOH < candidateNum - 1) {
				indexOfCurrentVOH++;
			} else {
				indexOfCurrentVOH = 0;
				currentRoundNum++;
			}

			if (currentRoundNum >= roundLimit) {
				probing = false;
				makeSelection(roundLimit);
			}
		}
	}

	public void onContradiction(ContradictionException cex) {
		int depth = 0;
		for (int idx = 0; idx < vars.length; idx++) {
			if (vars[idx].isInstantiated()) {
				depth++;
			}
		}
		totalFailDepth += depth;
	}

	@Override
	public Decision<IntVar> getDecision() {
		if (probing) {
			int currentDepth = 0;
			for (int idx = 0; idx < vars.length; idx++) {
				if (vars[idx].isInstantiated()) {
					currentDepth++;
				}
			}
			if (currentDepth > maxDepth) {
				maxDepth = currentDepth;
			}
			return candidateVOHs[indexOfCurrentVOH].getDecision();
		} else {
			return candidateVOHs[indexOfBestVOH].getDecision();
		}
	}

	protected void makeSelection(int num) {
		System.out.print("Round Number£º  " + num + "\n");
		double max = 0;
		for (int i = 0; i < candidateNum; i++) {
			if (minFD[i] <= 1) {
				max_min[i] = 0;
			} else {
				max_min[i] = maxD[i] / (Math.log10(minFD[i]));
			}

			if (max_min[i] > max) {
				indexOfBestVOH = i;
				max = max_min[i];
			}
			System.out.println("candidate " + i + ":\t maxD " + maxD[i] + ",\t minFD " + String.format("%.4f", minFD[i])
					+ ",\t MaxD/log(MinFD) " + String.format("%.4f", max_min[i]));
		}
		System.out.println("BestCandidate:" + candidateVOHs[indexOfBestVOH].getClass().getName());
		System.out.println("Start searching with the best VOH ...");
	}

}
