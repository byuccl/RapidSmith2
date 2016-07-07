package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.TileClusterCoordinatesFactory;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This placer is a very simple implementation of simulated annealing. At the beginning of the
 * anneal, most moves are accepted even if they increase the system cost. As it cools, fewer moves
 * are accepted.
 */
public class SimulatedAnnealingPlacer<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends BasicPlacer<CTYPE, CSITE>
{

	PlacerEffortLevel placeEffort;

	/** Create a new placer with a fixed seed. */
	public SimulatedAnnealingPlacer(TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory,
	                                long seed, Map<CTYPE, Rectangle> trMap,
	                                PlacerEffortLevel effortLevel
	) {
		super(tscFactory, seed, trMap);
		this.placeEffort = effortLevel;
		this.initPlacer = new DisplacementRandomInitialPlacer<>(rng);
	}

	/**
	 * Create a new placer with a set of area constraints. The constraints
	 * are a map between a Type and a Rectangle indicating the locations where
	 * the type need to be constrained.
	 */
	public SimulatedAnnealingPlacer(TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory,
	                                Map<CTYPE, Rectangle> trMap,
	                                PlacerEffortLevel effortLevel
	) {
		super(tscFactory, trMap);
		this.placeEffort = effortLevel;
		this.initPlacer = new DisplacementRandomInitialPlacer<>(rng);
	}

	public static Rectangle getRectangleFromString(String areaConstraint) {
		//String regex = "(\\d+,\\d+)";
		//String regex = "\\(.*\\)";
		//Pattern p = Pattern.compile(regex);
		//Matcher m = p.matcher(areaConstraint);
		String c[] = areaConstraint.split(",");
		if (c.length == 4) {
			int x = Integer.parseInt(c[0]);
			int y = Integer.parseInt(c[1]);
			int width = Integer.parseInt(c[2]);
			int height = Integer.parseInt(c[3]);
			return new Rectangle(x, y, width, height);
		}
		return null;
	}

	/**
	 * Creates a mapping between a primitive type and the rectangle constraint from a string.
	 * <p>
	 * The string is given in x,y,width,height and dictates the x,y location of the constraint
	 * and the width and height. This is done for SLICEL and SLICEM types.
	 */
	public static Map<PrimitiveType, Rectangle> createSliceLMAreaConstraint(String areaConstraint) {
		// TODO change this to CLBLL and CLBLM
		Map<PrimitiveType, Rectangle> typeConstraintMap;
		Rectangle rect = getRectangleFromString(areaConstraint);
		if (rect != null) {
			typeConstraintMap = new HashMap<>();
			typeConstraintMap.put(PrimitiveType.SLICEL, rect);
			typeConstraintMap.put(PrimitiveType.SLICEM, rect);
			System.out.println("Using area constraint of (" + rect.x + "," + rect.y + ") with width=" + rect.width +
					" and height=" + rect.height + " for SLICEL and SLICEM primitives");
			return typeConstraintMap;
		} else {
			System.out.println("Bad area constraint - " + areaConstraint);
		}
		return null;
	}

	/**
	 * Main
	 */
	public static void main(String args[]) {
		if (args.length < 3) {
			System.err.println("<xdl filename> <outputfilename> <effort level> [x,y,width,height]");
			System.exit(1);
		}

		String xdlInputFileName = args[0];
		String xdlOutputFileName = args[1];
		PlacerEffortLevel effortLevel = null;
		try {
			effortLevel = PlacerEffortLevel.valueOf(args[2]);
		} catch (IllegalArgumentException e) {
			System.out.println("Illegal Placeement Effort Level - " + args[2]);
			System.exit(1);
		}
		String areaConstraint = null;
		Map<PrimitiveType, Rectangle> typeConstraintMap = null;
		//
		if (args.length >= 4) {
//			areaConstraint = args[3];
//			typeConstraintMap = createSliceLMAreaConstraint(areaConstraint);
			System.out.println("Area constraints not yet supported");
		} else {
			System.out.println("No area constraints given");
		}
		if (args.length > 4) {
			System.out.println("Warning: extra arguments " + args.length);
		}

		long startTime = System.currentTimeMillis();
		Design design = new Design(xdlInputFileName);
		Device device = design.getDevice();
//		SimulatedAnnealingPlacer placer = new SimulatedAnnealingPlacer(new TileClusterCoordinatesFactory(), );
//		boolean successfulPlace = placer.place(design, device);
//		if (!successfulPlace)
//			System.out.println("Warning: place did not finish");
		design.saveXDLFile(xdlOutputFileName);

		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		double seconds = elapsedTime / 1.0E03;
		System.out.println("Elapsed Time = " + seconds + " seconds");

		System.out.println("=========================================");
	}

	/**
	 * The placer starts out with a random placement. At the beginning of the
	 * anneal, most moves are accepted even if they increase the system cost.
	 * As it cools, fewer moves are accepted.
	 * <p>
	 * This particular annealing schedule is based heavily on VPR. See
	 * "VPR: A New Packing, Placement and Routing Tool for FPGA Research"
	 * by Betz and Rose.
	 * <p>
	 * <p>
	 * Return
	 */
	@Override
	public boolean place(ClusterDesign<CTYPE, CSITE> design, Device device) {
		this.design = design;
		state = createPlacerState(design, typeRectangleMap);
		cost = new NetRectangleCostFunction<>(design, state);

		// Note for Alex:
		// - Need to set parameters for placement based on effort level.
		// - Parameters that need to be adjusted:
		//   stepsPerTemp
		//   MAX_TEMPERATURES_BELOW_COST_THRESHOLD
		//   COST_THRESHOLD

		// Perform initial placement
		System.out.println("Instances: " + design.getClusters().size());
		ArrayList<PlacementGroup> allGroups =
				new ArrayList<>(state.getPlacementGroups());
		boolean initialPlaceSuccessful = initPlacer.initialPlace(state);

		// Check to see if the initial placer was successful or not
		if (!initialPlaceSuccessful) {
			System.out.println("Unsuccesful initial place");
			return false;
		}

		// Initialize annealing schedule
		float currCost = cost.calcSystemCost();
		float initialCost = currCost;
		float oldTempCost = currCost;
		System.out.println("Initial placement cost: " + initialCost);

		if (DEBUG >= DEBUG_MEDIUM)
			System.out.println("== MAKING MOVES FOR INITIAL TEMPERATURE ==");
		float temperature = findInitialTemperature(allGroups, currCost, cost) * 1.5f;
		float fractionOfMovesAccepted = 0;
		//float alpha = 0;
		//float endTemperature = 0.005f*currCost/NetRectangleCostFunction.getRealNets(design).size();
		int numRealNets = NetRectangleCostFunction.getRealNets(design).size();
		//float endTemperature = .005f*currCost/numRealNets;

		int numMoves = 0;
		// TODO: Use the constraint rather than the device size
		int maxRangeLimit = device.getColumns() + device.getRows();
		int rangeLimit = maxRangeLimit;

		//Default values for Normal Mode
		int MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 5; //was 10
		float percentageThreshold = 1.0f;
		boolean usePercentMode = false;
		// Set steps per temperature for different modes
		float qualityMultiplier = HIGHER_QUALITY_MULTIPLIER;
		if (placeEffort == PlacerEffortLevel.LOW) {
			qualityMultiplier = qualityMultiplier * .4f;
			usePercentMode = true;
			percentageThreshold = 5.0f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 3;
		} else if (placeEffort == PlacerEffortLevel.MEDIUM) {
			qualityMultiplier = qualityMultiplier * .75f;
			usePercentMode = true;
			percentageThreshold = 1.0f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 5;
		} else if (placeEffort == PlacerEffortLevel.HIGH) {
			usePercentMode = true;
			percentageThreshold = 0.5f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 10;
		} else if (placeEffort == PlacerEffortLevel.HIGH_L) {
			qualityMultiplier = qualityMultiplier * .4f;
			usePercentMode = true;
			percentageThreshold = 0.5f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 10;
		} else if (placeEffort == PlacerEffortLevel.HIGH_M) {
			qualityMultiplier = qualityMultiplier * .75f;
			usePercentMode = true;
			percentageThreshold = 0.5f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 10;
		} else if (placeEffort == PlacerEffortLevel.HIGH_H) {
			qualityMultiplier = qualityMultiplier * 1.5f;
			usePercentMode = true;
			percentageThreshold = 0.5f;
			MAX_TEMPERATURES_BELOW_COST_THRESHOLD = 10;
		}

		int stepsPerTemp = (int) (Math.pow(allGroups.size(), 1.33f) * qualityMultiplier);
		System.out.println("Max Range Limit = " + maxRangeLimit + " steps per temp=" + stepsPerTemp);

		// Initialize time counter
		long initTime = System.currentTimeMillis();
		long currTime = initTime;
		long lastTime;
		int lastMoves = 0;

		System.out.println("Initial placement cost: " + currCost + " Initial Temperature: " + temperature);

		// Flag that indicates whether another temperature iteration should proceed
		boolean keepGoing = true;

		//float COST_THRESHOLD = .005f * currCost/numRealNets;
		//float COST_THRESHOLD = .05f * currCost/numRealNets;
		float COST_THRESHOLD = .05f * currCost / numRealNets;
		int numTemperaturesBelowCostThreshold = 0;

		System.out.println(" Cost Threshold = " + COST_THRESHOLD + " num temperatures below cost threshold=" +
				MAX_TEMPERATURES_BELOW_COST_THRESHOLD);
		System.out.println("------------------------------------------------------------------------------");
		if (DEBUG >= DEBUG_MEDIUM)
			System.out.println("== STARTING TO MAKE REAL MOVES ==");

		// Outer annealing loop. This loop will be called once for each temperature.
		while (keepGoing) {
			int numTempMoves = 0;
			int numTempMovesAccepted = 0;

			// This loop will perform a single move. It will be done "stepsPerTemp" times.
			for (int j = 1; j < stepsPerTemp; j++) {

				if (DEBUG >= DEBUG_MEDIUM) {
					System.out.println("= Searching for a move at current cost of " + currCost);
				}

				// Identify a move
				PlacerMove move = null;
				while (move == null) {
					// TODO: this will skip groups that are difficult to place. If the group cannot be
					// placed, it looks at a different group.
					int toSwapIdx = rng.nextInt(allGroups.size());
					PlacementGroup toSwap = allGroups.get(toSwapIdx);
					if (rangeLimit <= maxRangeLimit) {
						move = proposeMove(toSwap, true, rangeLimit);
					} else {
						move = proposeMove(toSwap);
					}
				}
				move.makeMove();
				//float newCost = calcIncrementalCost(move, currCost);
				float newCost = cost.calcIncrementalCost(move);
				float deltaCost = newCost - currCost;
				if (DEBUG >= DEBUG_MEDIUM) {
					System.out.print(" Move Cost=" + newCost + " (delta=" + deltaCost + ")");
				}

				boolean acceptMove;
				if (deltaCost < 0) {
					// if the cost is lowered, always accept the move.
					acceptMove = true;
					if (DEBUG >= DEBUG_MEDIUM) {
						System.out.println(" MOVE ACCEPTED");
					}
				} else {
					// Accept some moves that increase the cost. The higher the increase in
					// cost, the loweifferencePercentage = tempDiffCost/oldTempCost*100fr the probability it will be accepted.
					float r = rng.nextFloat();
					float moveThreshold = (float) Math.exp(-deltaCost / temperature);
					if (DEBUG >= DEBUG_MEDIUM) {
						System.out.print(" Threshold=" + moveThreshold + " rand=" + r);
					}
					if (r < moveThreshold) {
						acceptMove = true;
						if (DEBUG >= DEBUG_MEDIUM)
							System.out.println(" MOVE ACCEPTED");
					} else {
						acceptMove = false;
						if (DEBUG >= DEBUG_MEDIUM)
							System.out.println(" MOVE REJECTED");
					}
				}

				if (acceptMove) {
					currCost = newCost;
					numTempMovesAccepted++;
				}
				//reject the rest of the moves
				else {
					if (DEBUG >= DEBUG_MEDIUM)
						System.out.println("  Undo Move");
					move.undoMove();
					//float oldCost = calcIncrementalCost(newMove, newCost);
					float costIfMoveIsRejected = cost.calcIncrementalCost(move);
					float MAX_DIFFERENCE = 1.0f;
					if (Math.abs(costIfMoveIsRejected - currCost) > MAX_DIFFERENCE) {
						System.out.println("Warning: Undo cost mismatch: initial cost " + currCost + " new cost:" + newCost
								+ " after undo " + costIfMoveIsRejected + " difference=" + (costIfMoveIsRejected - currCost));
						//newMove.debugMove();
						//System.exit(-1);
					}
				}

				numMoves++;
				numTempMoves++;
				
				/*if(numMoves % MOVES_PER_MESSAGE == 0) {
					
					lastTime = currTime;
					currTime = System.currentTimeMillis();
					long dTime = currTime - lastTime;
					float movesPerSecifferencePercentage = tempDiffCost/oldTempCost*100fond = (float) MOVES_PER_MESSAGE / (dTime / 1000);
					int movesPerSecondInt = Math.round(movesPerSecond);
					
					System.out.println("Move: " + numMoves + " Cost: " + currCost + 
							" Temp: " + temperature + " Steps Per Temp: " + stepsPerTemp);
					System.out.println("End Temp: " + endTemperature + 
						" Range Limit: " + rangeLimit + " Last Alpha: " + alpha);
					System.out.println("\tTime: "+dTime/1000+" seconds. Moves per second: "+movesPerSecondInt);
				}
				*/
			}
			/*
			alpha = (float)numTempMovesAccepted/(float)numTempMoves;
			temperature = findNewTemperature(alpha, temperature);
			//endTemperature = 0.005f*currCost/netsForCostFunction.size();
			endTemperature = 0.005f*currCost/NetRectangleCostFunction.getRealNets(design).size();
			rangeLimit = findNewRangeLimit(alpha, rangeLimit, maxRangeLimit);
			*/

			float tempDiffCost = currCost - oldTempCost;
			oldTempCost = currCost;
			float differencePercentage = tempDiffCost / oldTempCost * 100f;
			boolean percentThresholdExceeded = differencePercentage <= 0 && -differencePercentage < percentageThreshold;
			boolean tempThresholdExceeded = tempDiffCost > 0 || (-tempDiffCost) < COST_THRESHOLD;
			if (usePercentMode && percentThresholdExceeded || !usePercentMode && tempThresholdExceeded) {
				numTemperaturesBelowCostThreshold++;
				//System.out.println("Didn't meet threshold "+costThreshold+" #"+numTemperaturesBelowCostThreshold);
				if (numTemperaturesBelowCostThreshold >= MAX_TEMPERATURES_BELOW_COST_THRESHOLD /* && rangeLimit == 1 */) {
					keepGoing = false;
					if (!usePercentMode)
						System.out.println("Did not meet threshold of " + COST_THRESHOLD + " for " + numTemperaturesBelowCostThreshold
								+ " consecutive temperatures");
					else
						System.out.println("The delta cost percent fell below -" + percentageThreshold + "% for " + numTemperaturesBelowCostThreshold
								+ " consecutive times.");
				}
			} else
				numTemperaturesBelowCostThreshold = 0;

			// Compute Time
			lastTime = currTime;
			currTime = System.currentTimeMillis();
			int moves = numMoves - lastMoves;
			long dTime = currTime - lastTime;
			float movesPerMiliSecond = (float) moves / (dTime);
			//int movesPerSecondInt = Math.round(movesPerSecond);
			System.out.println("\tTime: " + (float) dTime / 1000 + " seconds. " + moves + " moves. Moves per second: " +
					(float) movesPerMiliSecond * 1000);
			lastMoves = numMoves;

			// Compute new cost
			fractionOfMovesAccepted = (float) numTempMovesAccepted / (float) numTempMoves;
			temperature = findNewTemperature(fractionOfMovesAccepted, temperature);

			String diffPercent = String.format("%3.3f", tempDiffCost / oldTempCost * 100);
			System.out.println("\tNew cost=" + currCost + " delta cost: " + tempDiffCost + " (" + diffPercent + "%)");
			//endTemperature = 0.005f*currCost/NetRectangleCostFunction.getRealNets(design).size();
			rangeLimit = findNewRangeLimit(fractionOfMovesAccepted, rangeLimit, maxRangeLimit);

			System.out.println("\tRange Limit: " + rangeLimit);


			//work harder in more productive parts of the anneal
			//TODO: make this vary directly with alpha? We want alpha=.44, and 
			//we want to make lots of moves at that alpha.
			if (rangeLimit < maxRangeLimit) {
				stepsPerTemp = (int) (Math.pow(allGroups.size(), 1.33f) * qualityMultiplier);
			}
		}

		// Done. Reached the ending condition.

		if (DEBUG >= DEBUG_LOW) {
			System.out.println("Final Placement:");
			for (PlacementGroup pg : state.getPlacementGroups()) {
				System.out.println(" " + pg + ":" + state.getGroupAnchorSite(pg));
			}
		}

		//System.out.println("Final cost: " + currCost);
		long timeInMiliSeconds = (System.currentTimeMillis() - initTime);
		float movesPerSecond = (float) numMoves / timeInMiliSeconds * 1000;
		System.out.println("Final cost: " + currCost + " (" + (currCost / initialCost) * 100 + "% of initial cost:" +
				initialCost + ")");
		System.out.println(numMoves + " Moves in " + (float) timeInMiliSeconds / 1000 + " seconds (" + movesPerSecond + " moves per second)");
		state.finalizePlacement(design);
		return true;
	}

	/**
	 * Updates the temperature of the anneal based on the last temperature
	 * and the percentage of moves that were accepted during the last
	 * temperature cycle (alpha).
	 *
	 * @param fractionOfMovesAccepted
	 * @param curTemp
	 * @return
	 */
	protected float findNewTemperature(float fractionOfMovesAccepted, float curTemp) {
		float newTemp;
		float alpha;

		if (fractionOfMovesAccepted > 0.96) {
			//alpha = 0.5f;
			alpha = 0.7f;
		} else if (fractionOfMovesAccepted > 0.8) {
			alpha = 0.9f;
		} else if (fractionOfMovesAccepted > 0.25) {
			alpha = 0.95f;
		} else {
			alpha = 0.8f;
		}
		newTemp = alpha * curTemp;
		float fiftyPercentCostAccept = (float) -Math.log(.5) * newTemp;
		System.out.println("New temp=" + newTemp + " " + (int) (fractionOfMovesAccepted * 100) +
				"% accepted Alpha=" + alpha +
				" 50% delta cost accept=" + fiftyPercentCostAccept);
		return newTemp;
	}

	/**
	 * Find the range limit - that is, the maximum distance that any group can be
	 * moved from its existing placement.
	 *
	 * @param fractionOfMovesAccepted
	 * @param oldLimit
	 * @param maxRangeLimit
	 * @return
	 */
	protected int findNewRangeLimit(float fractionOfMovesAccepted, int oldLimit, int maxRangeLimit) {
		float newLimit = oldLimit * (1 - TARGET_ALPHA + fractionOfMovesAccepted);
		if (newLimit < 1)
			newLimit = 1;
		else if (newLimit > maxRangeLimit)
			newLimit = maxRangeLimit;
		else
			System.out.println("\tNew range=" + newLimit + " old range =" + oldLimit);
		return (int) newLimit;
	}

	/**
	 * Returns the initial temperature for the anneal. This is based on the standard
	 * deviation of the cost of a move from the initial placement. This is a little
	 * lower than the 20X standard deviation suggested in the VPR paper; this means
	 * an initial close placement is not completely "blown up" into a random placement.
	 *
	 * @param allGroups
	 * @param initCost
	 * @return
	 */
	protected float findInitialTemperature(ArrayList<PlacementGroup> allGroups, float initCost, PlacerCostFunction cost) {

		ArrayList<Float> allMoveDeltaCosts = new ArrayList<>();
		ArrayList<Float> allMoveCosts = new ArrayList<>();
		for (int i = 0; i < allGroups.size(); i++) {
			PlacerMove move = null;
			while (move == null) {
				int toSwapIdx = rng.nextInt(allGroups.size());
				PlacementGroup toSwap = allGroups.get(toSwapIdx);
				move = proposeMove(toSwap);
			}
			move.makeMove();
			float newCost = cost.calcIncrementalCost(move);
			allMoveDeltaCosts.add(newCost - initCost);
			allMoveCosts.add(newCost);
			move.undoMove();
			float undoCost = cost.calcIncrementalCost(move);

			if ((undoCost - initCost) > 1f) {
				System.out.println("Warning: initial cost " + initCost + " new cost " + newCost + " undocost " + undoCost);
			}

		}
		float stdDev = calcStdDev(allMoveDeltaCosts);
		float temperature = stdDev / 15;

		// Print debug messages regarding the computation of the initial temperature
		System.out.println("Initial temperature = " + temperature + " computed from " + allMoveDeltaCosts.size() + " moves.");
		System.out.println("\tAvg delta cost= " + calcMean(allMoveDeltaCosts) + " std=" + stdDev);
		System.out.println("\tAvg move cost=" + calcMean(allMoveCosts) + " std dev=" + calcStdDev(allMoveCosts) +
				" temp would be " + (20 * calcStdDev(allMoveCosts)));
		return temperature;
	}

	/**
	 * Computes the mean of the floats in the values List.
	 *
	 * @param values
	 * @return
	 */
	protected float calcMean(Collection<Float> values) {
		float total = 0;
		for (Float f : values) {
			total += f;
		}
		return total / values.size();
	}

	/**
	 * Computes the standard deviation of the floats
	 * in the values List.
	 *
	 * @param values
	 * @return
	 */
	protected float calcStdDev(Collection<Float> values) {
		double mean = calcMean(values);
		ArrayList<Float> sqDiffsFromMean = new ArrayList<>();
		for (Float f : values) {
			sqDiffsFromMean.add((float) Math.pow(f - mean, 2));
		}
		return (float) Math.sqrt(calcMean(sqDiffsFromMean));
	}

	public static float round(float f, int decimals) {
		int multiplier = 1;
		for (int i = 0; i < decimals; i++)
			multiplier *= 10;
		float m = Math.round(f * (float) multiplier);
		return m / multiplier;

	}

	/**
	 * A type that indicates the effort level of the placer
	 */
	public enum PlacerEffortLevel {
		LOW, MEDIUM, HIGH, NORMAL, HIGH_L, HIGH_M, HIGH_H
	}

	;

	/**
	 * The percentage of moves made that are actually accepted should be
	 * around 44% for best results. See "Performance of a New Annealing
	 * Schedule" by Lam and Delosme.
	 */
	public static final float TARGET_ALPHA = .15f; //.44f originally

	/**
	 * Determines how many moves are made per temperature. Higher values
	 * of these parameters lead to more moves per temperature, a higher
	 * quality of result, and a longer execution time. The lower quality
	 * version is used at the early stages of the anneal, and the higher
	 * quality version is used later on, once we're close to the target
	 * alpha.
	 */
	public static final float LOWER_QUALITY_MULTIPLIER = 0.15f;
	public static final float HIGHER_QUALITY_MULTIPLIER = 0.5f;


}
