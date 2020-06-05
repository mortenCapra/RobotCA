package com.robotca.ControlApp.Core.Dijkstra;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Class to find the best routes across the graph
 * @param <T> type of the nodes in the graph
 */
public class RouteFinder<T extends  GraphNode> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    /**
     * constructor of the routefinder
     * @param graph in which the routes are found
     * @param nextNodeScorer calculates the exaxt score for the next node
     * @param targetScorer calculates the estimated score to our destination
     */
    public RouteFinder(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer){
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    /**
     * method to find th best route between from and to
     * @param from node in which the route starts
     * @param to node in which the route ends
     * @return a list of nodes traversed in order to get to end node from start node
     */
    public List<T> findRoute(T from, T to){
        Queue<RouteNode> openSet = new PriorityQueue<>();
        Map<T, RouteNode<T>> allNodes = new HashMap<>();

        RouteNode<T> start = new RouteNode<>(from, null, 0d, targetScorer.computeCost(from, to));
        openSet.add(start);
        allNodes.put(from, start);
        while(!openSet.isEmpty()){
            RouteNode<T> next = openSet.poll();
            if (next.getCurrent().equals(to)){
                List<T> route = new ArrayList<>();
                RouteNode<T> current = next;
                do {
                    route.add(0, current.getCurrent());
                    current = allNodes.get(current.getPrevious());
                } while (current != null);
                return route;
            }

            Set<T> connections = graph.getConnections(next.getCurrent());
            for (T t: connections){
                RouteNode<T> nextNode = allNodes.get(t);
                if (nextNode == null){
                    nextNode = new RouteNode<>(t);
                }
                allNodes.put(t, nextNode);

                double newScore = next.getRouteScore() + nextNodeScorer.computeCost(next.getCurrent(), t);
                if (newScore < nextNode.getRouteScore()){
                    nextNode.setPrevious(next.getCurrent());
                    nextNode.setRouteScore(newScore);
                    nextNode.setEstimatedScore(newScore + targetScorer.computeCost(t, to));
                    openSet.add(nextNode);
                }
            }
        }
        return null;
    }
}
