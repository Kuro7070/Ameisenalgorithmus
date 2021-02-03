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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Affero GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package colony;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

/**
 * An ant colony.
 */
public class AntColony {

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	/** the pheromone decay parameter (used upon updating all pheromone weights) */
	private static double A; // = 0.1;

	private PrintStream outputStream;

	private final AntGraph antGraph;
	private Ant[] ants;
	private final int numberOfAnts;
	private int numberOfFinishedAnts;
	private int iterationCounter;
	private final int maxIterations;

	private final int colonyId;

	private static int GLOBAL_ID_COUNTER = 0;


	private   double B;// = 2;
	private   double Q0;// = 0.8;
	private   double R;// = 0.1;

	/**
	 * @param antGraph the graph
	 * @param numberOfAnts the number of ants in the new colony
	 * @param maxIterations max number of iterations to run ants in
	 */
	public AntColony(final AntGraph antGraph, final int numberOfAnts, final int maxIterations, final double A, double B, double Q0, double R) {
		this.antGraph = antGraph;
		this.numberOfAnts = numberOfAnts;
		this.maxIterations = maxIterations;
		GLOBAL_ID_COUNTER++;
		colonyId = GLOBAL_ID_COUNTER;

		AntColony.A = A;
		this.B = B;
		this.Q0 = Q0;
		this.R = R;

	}

	/**
	 * Starts the ant colony.
	 */
	public synchronized void start() {
		// creates all ants
		ants = createAnts();

		iterationCounter = 0;
		try {
			outputStream = new PrintStream(new FileOutputStream(
					colonyId + "_" + antGraph.getNumberOfNodes() + "x" + ants.length + "x" + maxIterations + "_colony.txt"));
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// loop for all iterations
		while (iterationCounter < maxIterations) {
			// run an iteration
			doIteration();
			try {
				// wait for all ants to finish
				wait();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (antGraph) {
				// update pheromone weights of all edges
				updatePheromonesOfAllEdges();
			}
		}

		if (iterationCounter == maxIterations) {
			outputStream.close();
		}
	}

	/**
	 * Do perform the iteration.
	 */
	private void doIteration() {
		numberOfFinishedAnts = 0;
		iterationCounter++;
		outputStream.print("iteration " + iterationCounter);
		for (int i = 0; i < ants.length; i++) {
			ants[i].startAnt();
		}
	}

	/**
	 * @return the graph
	 */
	public AntGraph getGraph() {
		return antGraph;
	}

	/**
	 * @return the number of ants
	 */
	public int getNumberOfAnts() {
		return numberOfAnts;
	}

	/**
	 * @return the iteration limit
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * @return the current iteration
	 */
	public int getCurrentIteration() {
		return iterationCounter;
	}

	/**
	 * @return the ID of the colony
	 */
	public int getID() {
		return colonyId;
	}

	/**
	 * Notifies that an ant finished.
	 */
	public synchronized void antFinished() {
		numberOfFinishedAnts++;

		if (numberOfFinishedAnts == numberOfAnts) {
			outputStream.println("\tbest path weight = " + Ant.BEST_PATH_WEIGHT + "\taverage tau = " + antGraph.getAverageTau());
			notify();

		}
	}

	/**
	 * @return the overall best path weight.
	 */
	public double getBestPathValue() {
		return Ant.BEST_PATH_WEIGHT;
	}

	/**
	 * @return the iteration at which the best path was found.
	 */
	public int getLastBestPathIteration() {
		return Ant.BEST_PATH_ITERATION;
	}

	/**
	 * @return <code>true</code> is iteration limit is reached.
	 */
	public boolean done() {
		return iterationCounter == maxIterations;
	}

	/**
	 * Creates an array of ants.
	 *
	 * @return a new array of ants
	 */
	private Ant[] createAnts() {
		Ant.resetGlobalValues();
		final Ant[] ants = new Ant[numberOfAnts];
		for (int i = 0; i < numberOfAnts; i++) {
			ants[i] = new Ant((int) (antGraph.getNumberOfNodes() * RANDOM.nextDouble()), this,B,Q0,R); // start at a different node
		}

		return ants;
	}

	/**
	 * Provides a greater amount of pheromone to shorter tours.Equation (4) dictates that only those edges belonging to
	 * the globally best tour will receive reinforcement.
	 */
	private void updatePheromonesOfAllEdges() {
		double evaporation = 0;
		double deposition = 0;

		for (int r = 0; r < antGraph.getNumberOfNodes(); r++) {
			for (int s = 0; s < antGraph.getNumberOfNodes(); s++) {
				if (r != s) {
					// get the value for delta tau
					final double deltaTau = (1 / Ant.BEST_PATH_WEIGHT) * Ant.BEST_PATH_MATRIX[r][s];

					// get the value for pheromone evaporation as defined in eq. d)
					evaporation = (1 - A) * antGraph.getTau(r, s);
					// get the value for pheromone deposition as defined in eq. d)
					deposition = A * deltaTau;

					// update tau
					antGraph.updateTau(r, s, evaporation + deposition);
				}
			}
		}
	}
}
