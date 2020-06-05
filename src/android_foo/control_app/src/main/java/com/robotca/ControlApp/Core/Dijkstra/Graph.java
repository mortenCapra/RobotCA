package com.robotca.ControlApp.Core.Dijkstra;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Class to represent a Graph.
 * @param <T> instances of graphNodes or nodes that extend GraphNodes
 */

public class Graph<T extends GraphNode>{

    private final Set<T> nodes;
    private final Map<String, Set<String>> connections;

    /**
     * constructor of the graph
     * @param nodes that are included in the graph
     * @param connections connections between nodes.
     */
    public Graph(Set<T> nodes, Map<String, Set<String>> connections){
        this.nodes = nodes;
        this.connections = connections;
    }

    /**
     * Get the node with the corresponding id. Throws exception if no node with the id is found
     * @param id of the node to be returned
     * @return the node with the id
     */
    public T getNode(String id){
        for (T node: nodes){
            if (id.equals(node.getId())){
                return node;
            }
        }
        throw new IllegalArgumentException("no node found with ID");
    }

    /**
     * get the set of nodes which are connected to the node given as a parameter
     * @param node from which all connections start
     * @return the set of all nodes that are connected to the node in the parameter
     */
    public Set<T> getConnections(T node) {
        Set<String> connectedNodeIds = new HashSet<>();
        Set<T> connectedNodes = new HashSet<>();
        for(T n: nodes){
            if(node == n){
                connectedNodeIds = connections.get(node.getId());
            }
        }
        for(String id: connectedNodeIds){
            connectedNodes.add(getNode(id));
        }
        return connectedNodes;
    }
}
