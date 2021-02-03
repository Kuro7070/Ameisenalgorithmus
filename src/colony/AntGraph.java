/*
 * Copyright (C) 2007-2016 Ugo Chirico
 *
 * This is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package colony;

import java.io.Serializable;

public class AntGraph implements Serializable {

	private static final long serialVersionUID = 5561845194000144163L;

	private final double[][] delta;
	private final double[][] tau;
	private final int numberOfNodes;
	private double tau0;

	public AntGraph(final int numberOfNodes, final double[][] delta) {
		this.numberOfNodes = numberOfNodes;
		this.delta = delta;
		this.tau = new double[numberOfNodes][numberOfNodes];

		resetTau();
	}

	public void resetTau() {
		final double averageDelta = average(delta);

		tau0 = 1 / (numberOfNodes * (0.5 * averageDelta));

		System.out.println("Average delta: " + averageDelta);
		System.out.println("Tau0: " + tau0);

		for (int r = 0; r < numberOfNodes; r++) {
			for (int s = 0; s < numberOfNodes; s++) {
				tau[r][s] = tau0;
			}
		}
	}

	private double average(final double matrix[][]) {
		double sum = 0;
		for (int r = 0; r < numberOfNodes; r++) {
			for (int s = 0; s < numberOfNodes; s++) {
				sum += matrix[r][s];
			}
		}

		return sum / (numberOfNodes * numberOfNodes);
	}

	@Override
	public String toString() {
		final StringBuilder deltaString = new StringBuilder();
		final StringBuilder tauString = new StringBuilder();

		for (int r = 0; r < numberOfNodes; r++) {
			for (int s = 0; s < numberOfNodes; s++) {
				deltaString.append(getDelta(r, s)).append("\t");
				tauString.append(getTau(r, s)).append("\t");
			}

			deltaString.append("\n");
		}

		return deltaString.append("\n\n\n").append(tauString).toString();
	}

	public synchronized int getNumberOfNodes() {
		return numberOfNodes;
	}

	public synchronized double getDelta(final int r, final int s) {
		return delta[r][s];
	}

	public synchronized double getEtha(final int r, final int s) {
		return 1 / delta[r][s];
	}

	public synchronized double getTau(final int r, final int s) {
		return tau[r][s];
	}

	public double getAverageTau() {
		return average(tau);
	}

	public synchronized double getTau0() {
		return tau0;
	}

	public synchronized void updateTau(final int r, final int s, final double value) {
		tau[r][s] = value;
	}

}
