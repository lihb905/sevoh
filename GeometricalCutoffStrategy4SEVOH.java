package org.sevoh;

import org.chocosolver.cutoffseq.GeometricalCutoffStrategy;

/**
 * This is the restart strategy used by SEVOH, which is built with a
 * GeometricalCutoffStrategy.
 * 
 * @author Hongbo Li and Yaling Wu
 *
 */
public class GeometricalCutoffStrategy4SEVOH extends GeometricalCutoffStrategy {

	private double probePhaseTotalRestarts;
	private int probeRestartTimes;
	private int probePhaseCutoff;

	/**
	 * The geometrical cutoff strategy for SEVOH.
	 * 
	 * @param s    and g are those used in GeometricalCutoffStrategy
	 * @param pct  the unique cutoff in the probe phase.
	 * @param sptr the total number of restarts in the probe phase, which equals to
	 *             roundLimit times the number of candidates used in SEVOH.
	 */
	public GeometricalCutoffStrategy4SEVOH(int pct, long sptr, long s, double g) {
		super(s, g);
		this.probePhaseTotalRestarts = sptr;
		probePhaseCutoff = pct;
		probeRestartTimes = 0;
	}

	@Override
	public long getNextCutoff() {
		if (probeRestartTimes < probePhaseTotalRestarts) {
			probeRestartTimes++;
			return probePhaseCutoff;
		} else {
			return super.getNextCutoff();
		}
	}
}
