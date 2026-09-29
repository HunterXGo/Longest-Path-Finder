package com.example.shortestpathfinder;

import com.example.shortestpathfinder.algorithm.LongestPathAlgorithm;
import com.example.shortestpathfinder.model.Graph;
import com.example.shortestpathfinder.ui.PathFinderFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Swing Longest Path Finder application.
 */
public class LongestPathFinderApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Graph graph = createSampleGraph();
            PathFinderFrame frame = new PathFinderFrame(graph, new LongestPathAlgorithm());
            frame.setVisible(true);
        });
    }

    private static Graph createSampleGraph() {
        Graph graph = new Graph();

        // Sample dataset with weighted roads.
        graph.addBidirectionalEdge("Delhi", "Mumbai", 1400);
        graph.addBidirectionalEdge("Delhi", "Jaipur", 280);
        graph.addBidirectionalEdge("Jaipur", "Ahmedabad", 660);
        graph.addBidirectionalEdge("Ahmedabad", "Mumbai", 530);
        graph.addBidirectionalEdge("Delhi", "Lucknow", 555);
        graph.addBidirectionalEdge("Lucknow", "Varanasi", 320);
        graph.addBidirectionalEdge("Varanasi", "Kolkata", 680);
        graph.addBidirectionalEdge("Mumbai", "Bengaluru", 980);
        graph.addBidirectionalEdge("Ahmedabad", "Bengaluru", 1490);
        graph.addBidirectionalEdge("Kolkata", "Bengaluru", 1870);

        return graph;
    }
}
