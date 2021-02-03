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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An artificial ant.
 */
public class Ant implements Runnable {

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	/** determines the relative importance of pheromone vs distance (used upon choosing next graph node) */
	private static  double B;// = 2;

	/** determines the relative importance of exploitation versus exploration (used upon choosing next graph node) */
	private static  double Q0;// = 0.8;

	/** used upon updating tau (pheromone weight) */
	private static  double R;// = 0.1;

	private final int antId;

	private int[][] pathMatrix;
	private int currentNode;
	private final int startNode;
	private double pathDelta;
	private final AntColony antColony;
	private final List<Integer> pathList = new ArrayList<>();

	private final List<Integer> nodesToVisitList = new ArrayList<>();

	private static int GLOBAL_ID_COUNTER = 0;
	private static PrintStream OUTPUT_STREAM;

	private static List<Integer> BEST_PATH_LIST = null;
	static double BEST_PATH_WEIGHT = Double.MAX_VALUE;
	static int[][] BEST_PATH_MATRIX = null;
	static int BEST_PATH_ITERATION = 0; // the iteration at which the best path was found

	/**
	 * Resets all global ant values.
	 */
	public static void resetGlobalValues() {
		BEST_PATH_WEIGHT = Double.MAX_VALUE;
		BEST_PATH_LIST = null;
		BEST_PATH_MATRIX = null;
		BEST_PATH_ITERATION = 0;
		OUTPUT_STREAM = null;
	}

	/**
	 * @param startNode the ant starting node
	 * @param antColony the ant colony the new ant belongs to
	 */
	public Ant(final int startNode, final AntColony antColony, final double B, final double Q0, final double R) {
		GLOBAL_ID_COUNTER++;
		antId = GLOBAL_ID_COUNTER;
		this.startNode = startNode;
		this.antColony = antColony;
		Ant.B = B;
		Ant.Q0 = Q0;
		Ant.R = R;
	}

	/**
	 * Runs the ant.
	 */
	public void startAnt() {
		initAnt();
		final Thread thread = new Thread(this);
		thread.setName("Ant " + antId);
		thread.start();
	}

	/**
	 * Inits the global output stream and the private members.
	 */
	private void initAnt() {
		if (OUTPUT_STREAM == null) {
			try {
				OUTPUT_STREAM = new PrintStream(new FileOutputStream(antColony.getID()
						+ "_"
						+ antColony.getGraph().getNumberOfNodes()
						+ "x"
						+ antColony.getNumberOfAnts()
						+ "x"
						+ antColony.getMaxIterations()
						+ "_ants.txt"));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		final int numberOfNodes = antColony.getGraph().getNumberOfNodes();
		currentNode = startNode;
		pathMatrix = new int[numberOfNodes][numberOfNodes];
		pathList.add(startNode);
		pathDelta = 0;

		// fill nodesToVisit map
		for (int i = 0; i < numberOfNodes; i++) {
			nodesToVisitList.add(i);
		}
		nodesToVisitList.remove(startNode);
	}

	@Override
	public void run() {
		final AntGraph graph = antColony.getGraph();

		while (!nodesToVisitList.isEmpty()) {
			int nextNode;

			// compute next node and add edge weight to the total path weight
			synchronized (graph) {
				nextNode = computeNextNode(currentNode);
				pathDelta += graph.getDelta(currentNode, nextNode);
			}

			// add the current node the list of visited nodes
			pathList.add(nextNode);
			pathMatrix[currentNode][nextNode] = 1;

			// update pheromones of the edge
			synchronized (graph) {
				updatePherormonesOfEdge(currentNode, nextNode);
			}

			// update the current node
			currentNode = nextNode;
		}

		// update the best path values
		synchronized (graph) {
			if (pathDelta < BEST_PATH_WEIGHT) {
				BEST_PATH_WEIGHT = pathDelta;
				BEST_PATH_MATRIX = pathMatrix;
				BEST_PATH_LIST = pathList;
				BEST_PATH_ITERATION = antColony.getCurrentIteration();

				OUTPUT_STREAM.println("Ant "
						+ antId
						+ ",\tbest path weight = "
						+ BEST_PATH_WEIGHT
						+ ",\tbest path iteration = "
						+ BEST_PATH_ITERATION
						+ ",\tnumber of visited nodes = "
						+ BEST_PATH_LIST.size()
						+ ",\tvisited nodes = "
						+ BEST_PATH_LIST);
			}
		}

		// notify the colony
		antColony.antFinished();

		// close file stream on end
		if (antColony.done()) {
			OUTPUT_STREAM.close();
		}
	}

	/**
	 * Computes the next node.
	 * The state transition rule favors transitions toward nodes connected by short edges and with a large amount of pheromone.
	 * For the sake of adding nondeterministic behavior there are two transition rules chosen from at random.
	 *
	 * @param currentNode the current node
	 * @return the next node
	 */
	private int computeNextNode(final int currentNode) {
		final AntGraph graph = antColony.getGraph();

		// generate a random number
		final double q = RANDOM.nextDouble();

		// Exploitation
		if (q <= Q0) {
			int nextNode = -1;
			double maxPheromoneWeight = 0;

			// search the max of the value as defined in eq. a)
			for (final int node : nodesToVisitList) {

				//get the value
				//getTau Pheromon
				//getEtha 1/delta
				//delta = Distanz
				final double pheromoneWeight = graph.getTau(currentNode, node) * Math.pow(graph.getEtha(currentNode, node), B);

				// check if it is the max
				if (pheromoneWeight > maxPheromoneWeight) {
					maxPheromoneWeight = pheromoneWeight;
					nextNode = node;
				}

			}

			// delete the selected node from the list of node to visit
			nodesToVisitList.remove(nextNode);
			return nextNode;

			// Exploration
		} else {
			double sum = 0;

			// sum up pheromone weights
			for (final int node : nodesToVisitList) {
				sum += graph.getTau(currentNode, node) * Math.pow(graph.getEtha(currentNode, node), B);
			}

			// get the average value
			final double average = sum / nodesToVisitList.size();

			// search the node as defined in eq. b)
			for (final int node : nodesToVisitList) {

				//	// get the value of p as defined in eq. b) ////////////// TODO needed?
				//	final double p = (graph.getTau(currentNode, node) * Math.pow(graph.getEtha(currentNode, node), B)) / sum;

				// if the value of p is greater the the average value the node is good
				if ((graph.getTau(currentNode, node) * Math.pow(graph.getEtha(currentNode, node), B)) > average) {
					// delete the selected node from the list of node to visit
					nodesToVisitList.remove(node);
					return node;
				}
			}
		}

		// if no node can be selected, go to node 0
		return 0;
	}

	/**
	 * Updates the pheromone weight of a given edge defined by a start node and an end node.
	 *
	 * @param startNode start node
	 * @param endNode end node
	 */
	public synchronized void updatePherormonesOfEdge(final int startNode, final int endNode) {
		final AntGraph graph = antColony.getGraph();

		// compute the new pheromone weight as defined in eq. c)
		final double newPheromoneWeight = (1 - R) * graph.getTau(startNode, endNode) + (R * (graph.getTau0()));

		// update tau
		graph.updateTau(startNode, endNode, newPheromoneWeight);
	}

	@Override
	public String toString() {
		return "Ant " + antId + ":" + currentNode;
	}
}
