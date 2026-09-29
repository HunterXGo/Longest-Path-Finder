package com.example.shortestpathfinder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an undirected weighted graph using an adjacency list.
 */
public class Graph {
    private final Map<String, List<Edge>> adjacencyList = new LinkedHashMap<>();

    /**
     * Creates a deep copy of the graph so algorithms can read a stable snapshot.
     *
     * @return copied graph
     */
    public Graph copy() {
        Graph copy = new Graph();
        for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
            List<Edge> edges = new ArrayList<>();
            for (Edge edge : entry.getValue()) {
                edges.add(new Edge(edge.destination(), edge.distance()));
            }
            copy.adjacencyList.put(entry.getKey(), edges);
        }
        return copy;
    }

    /**
     * Adds a city if it does not already exist.
     *
     * @param city city name
     */
    public void addCity(String city) {
        validateCityName(city);
        adjacencyList.putIfAbsent(city.trim(), new ArrayList<>());
    }

    /**
     * Adds an undirected edge between two cities.
     *
     * @param source   source city
     * @param target   destination city
     * @param distance positive distance between cities
     */
    public void addBidirectionalEdge(String source, String target, int distance) {
        validateCityName(source);
        validateCityName(target);

        if (source.trim().equalsIgnoreCase(target.trim())) {
            throw new IllegalArgumentException("Source and destination cities must be different.");
        }
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero.");
        }

        String normalizedSource = source.trim();
        String normalizedTarget = target.trim();

        addCity(normalizedSource);
        addCity(normalizedTarget);

        addOrReplaceDirectedEdge(normalizedSource, normalizedTarget, distance);
        addOrReplaceDirectedEdge(normalizedTarget, normalizedSource, distance);
    }

    public boolean containsCity(String city) {
        return city != null && adjacencyList.containsKey(city.trim());
    }

    public List<String> getCities() {
        List<String> cities = new ArrayList<>(adjacencyList.keySet());
        cities.sort(Comparator.naturalOrder());
        return Collections.unmodifiableList(cities);
    }

    public List<Edge> getNeighbors(String city) {
        if (!containsCity(city)) {
            return List.of();
        }
        return Collections.unmodifiableList(adjacencyList.get(city.trim()));
    }

    private void addOrReplaceDirectedEdge(String source, String target, int distance) {
        List<Edge> neighbors = adjacencyList.get(source);
        neighbors.removeIf(edge -> edge.destination().equalsIgnoreCase(target));
        neighbors.add(new Edge(target, distance));
    }

    private void validateCityName(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be empty.");
        }
    }

    /**
     * Immutable edge record for an adjacency list entry.
     *
     * @param destination neighboring city
     * @param distance    edge weight
     */
    public record Edge(String destination, int distance) {
        public Edge {
            Objects.requireNonNull(destination, "Destination city cannot be null.");
            if (destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination city cannot be empty.");
            }
            if (distance <= 0) {
                throw new IllegalArgumentException("Distance must be greater than zero.");
            }
        }
    }
}
