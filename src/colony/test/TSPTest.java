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
package colony.test;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Random;

import colony.AntColony;
import colony.AntGraph;

public class TSPTest {
    private static Random s_ran = new Random(System.currentTimeMillis());

    public static void main(final String[] args) {
        // Print application prompt to console.
        System.out.println("AntColonySystem for TSP");

/*
* 	B: Wichtigkeit der Pheromonen gegenüber der Distanz
	Q0: Wichtigkeit der Ausbeute gegenüber der Erkundung schnellerer Wege
	R: Anreicherung des Pheromon Gewichts
	A: Setz die Pheromonen Werte nicht genutzter Pfade herab
* */

        //Parameter
        int nAnts = 30;
        int nNodes = 50;
        int nIterations = 2500;
        int nRepetitions = 1;


        final double d[][] = new double[nNodes][nNodes];
        //        double t[][] = new double[nNodes][nNodes];

        for (int i = 0; i < nNodes; i++) {
            for (int j = i + 1; j < nNodes; j++) {
                d[i][j] = s_ran.nextDouble();
                d[j][i] = d[i][j];
                //                t[i][j] = 1; //(double)1 / (double)(nNodes * 10);
                //                t[j][i] = t[i][j];
            }
        }

        final AntGraph graph = new AntGraph(nNodes, d);

        try {
            final ObjectOutputStream outs = new ObjectOutputStream(new FileOutputStream("" + nNodes + "_antgraph.bin"));
            outs.writeObject(graph);
            outs.close();

            //            ObjectInputStream ins = new ObjectInputStream(new FileInputStream("c:\\temp\\" + nNodes + "_antgraph.bin"));
            //            graph = (AntGraph)ins.readObject();
            //            ins.close();

            final FileOutputStream outs1 = new FileOutputStream("" + nNodes + "_antgraph.txt");

            for (int i = 0; i < nNodes; i++) {
                for (int j = 0; j < nNodes; j++) {
                    outs1.write((graph.getDelta(i, j) + ",").getBytes());
                }
                outs1.write('\n');
            }

            outs1.close();

            final PrintStream outs2 = new PrintStream(
                    new FileOutputStream("" + nNodes + "x" + nAnts + "x" + nIterations + "_results.txt"));


            /*	  Opti	Low		High
             * A:  0.1	0,05	0,9
             * B:  2	0,5		3
             * Q0: 0.8	0,1		0,95
             * R:  0.1	0,05	0,9
             * */

            /*for (int i = 0; i < nRepetitions; i++) {
                graph.resetTau();
                final AntColony antColony = new AntColony(graph, nAnts, nIterations, );
                antColony.start();
                outs2.println(i + "," + antColony.getBestPathValue() + "," + antColony.getLastBestPathIteration());
            }*/

            //Aenderung A - Low, Opti, High
			startRoutine(0.05,2,0.8,0.1,graph,outs2,"Änderung A: Low", nAnts, nIterations);
			startRoutine(0.1,2,0.8,0.1,graph,outs2,"Änderung A: Opti", nAnts, nIterations);
			startRoutine(0.3,2,0.8,0.1,graph,outs2,"Änderung A: High", nAnts, nIterations);

			//Aenderung B
			startRoutine(0.1,0.5,0.8,0.1,graph,outs2,"Änderung B: Low", nAnts, nIterations);
			startRoutine(0.1,2,0.8,0.1,graph,outs2,"Änderung B: Opti", nAnts, nIterations);
			startRoutine(0.1,3,0.8,0.1,graph,outs2,"Änderung B: High", nAnts, nIterations);

			//Aenderung Q0
			startRoutine(0.1,2,0.1,0.1,graph,outs2,"Änderung Q0: Low", nAnts, nIterations);
			startRoutine(0.1,2,0.8,0.1,graph,outs2,"Änderung Q0: Opti", nAnts, nIterations);
			startRoutine(0.1,2,0.95,0.1,graph,outs2,"Änderung Q0: High", nAnts, nIterations);

			//Aenderung R
			startRoutine(0.1,2,0.8,0.05,graph,outs2,"Änderung R: Low", nAnts, nIterations);
			startRoutine(0.1,2,0.8,0.1,graph,outs2,"Änderung R: Opti", nAnts, nIterations);
			startRoutine(0.1,2,0.8,0.9,graph,outs2,"Änderung R: High", nAnts, nIterations);

            outs2.close();
        } catch (final Exception ex) {
        }
    }

	public static void startRoutine(double A, double B, double Q0, double R, AntGraph graph, PrintStream outs2, String name, int nAnts, int nIterations) {
		graph.resetTau();
		final AntColony antColony = new AntColony(graph, nAnts, nIterations,A,B,Q0,R );
		antColony.start();
		outs2.println(name + ", " + antColony.getBestPathValue() + "," + antColony.getLastBestPathIteration());
    }

}
