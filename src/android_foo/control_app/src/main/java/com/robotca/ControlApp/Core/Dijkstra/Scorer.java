package com.robotca.ControlApp.Core.Dijkstra;

/**
 * Scorer interface with a single method
 * @param <T> type of nodes to score
 */

public interface Scorer<T extends GraphNode> {
    /**
     * method to compute cost
     * @param from start node
     * @param to end node
     * @return score
     */
    double computeCost(T from, T to);
}
