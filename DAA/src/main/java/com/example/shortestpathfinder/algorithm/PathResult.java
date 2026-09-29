package com.example.shortestpathfinder.algorithm;

import java.util.Collections;
import java.util.List;

/**
 * Holds the outcome of a path search.
 */
public class PathResult {
    private final boolean pathExists;
    private final List<String> path;
    private final long totalDistance;
    private final String message;

    private PathResult(boolean pathExists, List<String> path, long totalDistance, String message) {
        this.pathExists = pathExists;
        this.path = List.copyOf(path);
        this.totalDistance = totalDistance;
        this.message = message;
    }

    public static PathResult success(List<String> path, long totalDistance) {
        return new PathResult(true, path, totalDistance, "Path found.");
    }

    public static PathResult success(List<String> path, long totalDistance, String message) {
        return new PathResult(true, path, totalDistance, message);
    }

    public static PathResult failure(String message) {
        return new PathResult(false, Collections.emptyList(), -1, message);
    }

    public boolean pathExists() {
        return pathExists;
    }

    public List<String> getPath() {
        return path;
    }

    public long getTotalDistance() {
        return totalDistance;
    }

    public String getMessage() {
        return message;
    }
}
