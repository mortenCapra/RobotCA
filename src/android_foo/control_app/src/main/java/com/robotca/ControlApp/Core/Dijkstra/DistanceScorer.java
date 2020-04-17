package com.robotca.ControlApp.Core.Dijkstra;

import static com.robotca.ControlApp.Core.Utils2.computeDistanceBetweenTwoPoints;

public class DistanceScorer implements Scorer<GeoPointNode> {

    public DistanceScorer(){}

    @Override
    public double computeCost(GeoPointNode from, GeoPointNode to) {
        return computeDistanceBetweenTwoPoints(from.getGeoPoint(), to.getGeoPoint());
    }
}
