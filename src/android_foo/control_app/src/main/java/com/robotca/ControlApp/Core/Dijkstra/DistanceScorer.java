package com.robotca.ControlApp.Core.Dijkstra;

import static com.robotca.ControlApp.Core.Utils2.computeDistanceBetweenTwoPoints;

/**
 * Implementation of the scorer interface. Gives a score corresponding to the distance between two points
 */

public class DistanceScorer implements Scorer<GeoPointNode> {

    public DistanceScorer(){}

    /**
     * Overridden method from scorer interface. To generate a
     * @param from the start node
     * @param to the end node
     * @return the score of the route
     */
    @Override
    public double computeCost(GeoPointNode from, GeoPointNode to) {
        return computeDistanceBetweenTwoPoints(from.getGeoPoint(), to.getGeoPoint());
    }
}
