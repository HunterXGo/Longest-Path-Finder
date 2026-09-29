package com.example.shortestpathfinder.algorithm;

import com.example.shortestpathfinder.model.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the longest simple path between two cities using DFS/backtracking.
 *
 * <p>In an undirected positive-weight graph, a longest path is only finite when
 * cities are not revisited. This class therefore searches for the longest simple
 * path from source to destination.</p>
 */
public class LongestPathAlgorithm {

    public PathResult findLongestPath(Graph graph, String source, String destination) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null.");
        }
        if (source == null || source.trim().isEmpty() || destination == null || destination.trim().isEmpty()) {
            return PathResult.failure("Please select both source and destination cities.");
        }

        String normalizedSource = source.trim();
        String normalizedDestination = destination.trim();

        if (!graph.containsCity(normalizedSource) || !graph.containsCity(normalizedDestination)) {
            return PathResult.failure("Selected city does not exist in the graph.");
        }
        if (normalizedSource.equalsIgnoreCase(normalizedDestination)) {
            return PathResult.success(List.of(normalizedSource), 0, "Longest simple path found.");
        }

        BestPath bestPath = new BestPath();
        Set<String> visited = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        visited.add(normalizedSource);
        currentPath.add(normalizedSource);

        search(graph, normalizedSource, normalizedDestination, visited, currentPath, 0, bestPath);

        if (bestPath.path.isEmpty()) {
            return PathResult.failure("No path exists between " + normalizedSource + " and " + normalizedDestination + ".");
        }

        return PathResult.success(bestPath.path, bestPath.totalDistance, "Longest simple path found.");
    }

    private void search(Graph graph, String currentCity, String destination, Set<String> visited,
                        List<String> currentPath, long distance, BestPath bestPath) {
        if (currentCity.equals(destination)) {
            if (distance > bestPath.totalDistance) {
                bestPath.path = List.copyOf(currentPath);
                bestPath.totalDistance = distance;
            }
            return;
        }

        for (Graph.Edge edge : graph.getNeighbors(currentCity)) {
            if (visited.contains(edge.destination())) {
                continue;
            }

            visited.add(edge.destination());
            currentPath.add(edge.destination());

            search(graph, edge.destination(), destination, visited, currentPath,
                    distance + edge.distance(), bestPath);

            currentPath.remove(currentPath.size() - 1);
            visited.remove(edge.destination());
        }
    }

    private static class BestPath {
        private List<String> path = List.of();
        private long totalDistance = Long.MIN_VALUE;
    }
}
