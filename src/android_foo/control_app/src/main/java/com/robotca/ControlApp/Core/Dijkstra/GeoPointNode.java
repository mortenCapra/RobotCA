package com.robotca.ControlApp.Core.Dijkstra;

import org.osmdroid.util.GeoPoint;

/**
 * Implementation of the GraphNode interface. Each graphNode represents a single geopoint in the route
 */

public class GeoPointNode implements GraphNode {
    private final String id;
    private final GeoPoint geoPoint;

    /**
     * Constructor of the graphnode
     * @param id to identify the node
     * @param geoPoint the geopoint which the node represents
     */
    public GeoPointNode(String id, GeoPoint geoPoint){
        this.id = id;
        this.geoPoint = geoPoint;
    }

    /**
     * Overridden method to get the id of the node
     * @return the id set in the constructor
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * get the gropoint represented by the node
     * @return the geopoint
     */
    public GeoPoint getGeoPoint() {
        return geoPoint;
    }
}
