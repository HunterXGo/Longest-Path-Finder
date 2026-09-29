package com.example.shortestpathfinder.ui;

import com.example.shortestpathfinder.algorithm.LongestPathAlgorithm;
import com.example.shortestpathfinder.algorithm.PathResult;
import com.example.shortestpathfinder.model.Graph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Swing UI for finding and visualizing the longest simple path in a city graph.
 */
public class PathFinderFrame extends JFrame {
    private static final int MAP_WIDTH = 760;
    private static final int MAP_HEIGHT = 460;

    private final Graph graph;
    private final LongestPathAlgorithm longestPathAlgorithm;
    private final Object graphLock = new Object();

    private final JComboBox<String> sourceComboBox = new JComboBox<>();
    private final JComboBox<String> destinationComboBox = new JComboBox<>();
    private final JComboBox<String> connectToComboBox = new JComboBox<>();
    private final JButton findButton = new JButton("Find Longest Path");
    private final JButton addCityButton = new JButton("Add City");
    private final JLabel pathLabel = new JLabel("Longest Path: -");
    private final JLabel distanceLabel = new JLabel("Total Distance: -");
    private final JLabel statusLabel = new JLabel("Choose two cities, then click Find Longest Path.");
    private final JTextArea routesArea = new JTextArea(9, 30);
    private final JTextField newCityField = new JTextField();
    private final JTextField distanceField = new JTextField();
    private final GraphPanel graphPanel = new GraphPanel();

    public PathFinderFrame(Graph graph, LongestPathAlgorithm longestPathAlgorithm) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null.");
        this.longestPathAlgorithm = Objects.requireNonNull(longestPathAlgorithm, "LongestPathAlgorithm cannot be null.");

        setTitle("Longest Path Finder using Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 760));

        initializeView();
        refreshCitySelectors();
        updateRoutesSummary();
        graphPanel.setGraph(graph, List.of());
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeView() {
        JPanel rootPanel = new JPanel(new BorderLayout(18, 18));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        rootPanel.setBackground(new Color(238, 243, 247));

        rootPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        rootPanel.add(buildContentPanel(), BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Longest Path Finder");
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 26));

        JLabel subtitleLabel = new JLabel("DFS/backtracking longest simple path with Swing components");
        subtitleLabel.setForeground(new Color(72, 97, 116));

        JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel buildContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(18, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(buildControlsPanel(), BorderLayout.WEST);
        contentPanel.add(buildRightPanel(), BorderLayout.CENTER);

        return contentPanel;
    }

    private JPanel buildControlsPanel() {
        JPanel controlsPanel = createCardPanel(new GridBagLayout());
        controlsPanel.setPreferredSize(new Dimension(340, 620));

        GridBagConstraints constraints = baseConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        controlsPanel.add(createSectionTitle("Route Search"), constraints);

        addLabeledControl(controlsPanel, "Source City", sourceComboBox, 1);
        addLabeledControl(controlsPanel, "Destination City", destinationComboBox, 3);

        constraints.gridy = 5;
        findButton.addActionListener(event -> handleFindLongestPath());
        controlsPanel.add(findButton, constraints);

        constraints.gridy = 6;
        JPanel resultPanel = createCardPanel(new GridBagLayout());
        resultPanel.setBackground(new Color(255, 250, 241));
        addResultLabels(resultPanel);
        controlsPanel.add(resultPanel, constraints);

        constraints.gridy = 7;
        controlsPanel.add(createSectionTitle("Add New City"), constraints);

        addLabeledControl(controlsPanel, "City Name", newCityField, 8);
        addLabeledControl(controlsPanel, "Connect To", connectToComboBox, 10);
        addLabeledControl(controlsPanel, "Distance", distanceField, 12);

        constraints.gridy = 14;
        addCityButton.addActionListener(event -> handleAddCity());
        controlsPanel.add(addCityButton, constraints);

        return controlsPanel;
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 16));
        rightPanel.setOpaque(false);

        JPanel mapCard = createCardPanel(new BorderLayout(0, 12));
        mapCard.add(createSectionTitle("City Map"), BorderLayout.NORTH);
        mapCard.add(graphPanel, BorderLayout.CENTER);

        routesArea.setEditable(false);
        routesArea.setLineWrap(true);
        routesArea.setWrapStyleWord(true);
        routesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JPanel routesCard = createCardPanel(new BorderLayout(0, 12));
        routesCard.add(createSectionTitle("Available Routes"), BorderLayout.NORTH);
        routesCard.add(new JScrollPane(routesArea), BorderLayout.CENTER);

        rightPanel.add(mapCard, BorderLayout.CENTER);
        rightPanel.add(routesCard, BorderLayout.SOUTH);
        return rightPanel;
    }

    private void addResultLabels(JPanel resultPanel) {
        GridBagConstraints constraints = baseConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);

        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.BOLD));
        distanceLabel.setFont(distanceLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setForeground(new Color(72, 97, 116));

        constraints.gridy = 0;
        resultPanel.add(pathLabel, constraints);
        constraints.gridy = 1;
        resultPanel.add(distanceLabel, constraints);
        constraints.gridy = 2;
        resultPanel.add(statusLabel, constraints);
    }

    private void addLabeledControl(JPanel panel, String labelText, java.awt.Component component, int startRow) {
        GridBagConstraints constraints = baseConstraints();
        constraints.gridy = startRow;
        panel.add(new JLabel(labelText), constraints);

        constraints.gridy = startRow + 1;
        panel.add(component, constraints);
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Verdana", Font.BOLD, 16));
        return label;
    }

    private JPanel createCardPanel(java.awt.LayoutManager layoutManager) {
        JPanel panel = new JPanel(layoutManager);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 228, 237)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(6, 0, 6, 0);
        return constraints;
    }

    private void handleFindLongestPath() {
        String source = (String) sourceComboBox.getSelectedItem();
        String destination = (String) destinationComboBox.getSelectedItem();
        setSearchInProgress(true);

        SwingWorker<PathResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PathResult doInBackground() {
                Graph graphSnapshot;
                synchronized (graphLock) {
                    graphSnapshot = graph.copy();
                }
                return longestPathAlgorithm.findLongestPath(graphSnapshot, source, destination);
            }

            @Override
            protected void done() {
                try {
                    applyPathResult(get());
                } catch (Exception exception) {
                    showError("Unable to compute the longest path.", exception.getMessage());
                } finally {
                    setSearchInProgress(false);
                }
            }
        };

        worker.execute();
    }

    private void handleAddCity() {
        try {
            String newCity = newCityField.getText() == null ? "" : newCityField.getText().trim();
            String connectTo = (String) connectToComboBox.getSelectedItem();
            String distanceText = distanceField.getText() == null ? "" : distanceField.getText().trim();

            if (newCity.isEmpty() || connectTo == null || connectTo.isBlank() || distanceText.isEmpty()) {
                throw new IllegalArgumentException("Please enter a city name, a city to connect to, and a valid distance.");
            }
            if (graph.containsCity(newCity)) {
                throw new IllegalArgumentException("That city already exists.");
            }

            int distance = Integer.parseInt(distanceText);
            synchronized (graphLock) {
                graph.addBidirectionalEdge(newCity, connectTo, distance);
            }

            newCityField.setText("");
            distanceField.setText("");
            refreshCitySelectors();
            updateRoutesSummary();
            graphPanel.setGraph(graph, List.of());
            statusLabel.setForeground(new Color(22, 101, 52));
            statusLabel.setText("Added city " + newCity + " and connected it to " + connectTo + ".");
        } catch (NumberFormatException exception) {
            showError("Invalid Distance", "Distance must be a whole number.");
        } catch (IllegalArgumentException exception) {
            showError("Cannot Add City", exception.getMessage());
        }
    }

    private void refreshCitySelectors() {
        String currentSource = (String) sourceComboBox.getSelectedItem();
        String currentDestination = (String) destinationComboBox.getSelectedItem();
        String currentConnectTo = (String) connectToComboBox.getSelectedItem();
        List<String> cities = graph.getCities();

        resetComboBox(sourceComboBox, cities, currentSource != null ? currentSource : firstCity(cities));
        resetComboBox(destinationComboBox, cities,
                currentDestination != null ? currentDestination : (cities.size() > 1 ? cities.get(1) : firstCity(cities)));
        resetComboBox(connectToComboBox, cities, currentConnectTo != null ? currentConnectTo : firstCity(cities));
    }

    private void resetComboBox(JComboBox<String> comboBox, List<String> cities, String preferredValue) {
        comboBox.removeAllItems();
        for (String city : cities) {
            comboBox.addItem(city);
        }
        if (preferredValue != null && cities.contains(preferredValue)) {
            comboBox.setSelectedItem(preferredValue);
        } else if (!cities.isEmpty()) {
            comboBox.setSelectedIndex(0);
        }
    }

    private String firstCity(List<String> cities) {
        return cities.isEmpty() ? null : cities.get(0);
    }

    private void updateRoutesSummary() {
        StringBuilder summary = new StringBuilder();
        for (String city : graph.getCities()) {
            String routes = graph.getNeighbors(city).stream()
                    .map(edge -> edge.destination() + " (" + edge.distance() + " km)")
                    .collect(Collectors.joining(", "));
            summary.append(city).append(" -> ").append(routes).append(System.lineSeparator());
        }
        routesArea.setText(summary.toString());
    }

    private void applyPathResult(PathResult result) {
        if (result.pathExists()) {
            pathLabel.setText("Longest Path: " + String.join(" -> ", result.getPath()));
            distanceLabel.setText("Total Distance: " + result.getTotalDistance() + " km");
            statusLabel.setForeground(new Color(22, 101, 52));
            statusLabel.setText(result.getMessage());
            graphPanel.setGraph(graph, result.getPath());
            return;
        }

        pathLabel.setText("Longest Path: -");
        distanceLabel.setText("Total Distance: -");
        statusLabel.setForeground(new Color(185, 28, 28));
        statusLabel.setText(result.getMessage());
        graphPanel.setGraph(graph, List.of());
    }

    private void setSearchInProgress(boolean inProgress) {
        findButton.setEnabled(!inProgress);
        addCityButton.setEnabled(!inProgress);
        sourceComboBox.setEnabled(!inProgress);
        destinationComboBox.setEnabled(!inProgress);
        connectToComboBox.setEnabled(!inProgress);
        newCityField.setEnabled(!inProgress);
        distanceField.setEnabled(!inProgress);

        if (inProgress) {
            statusLabel.setForeground(new Color(15, 23, 42));
            statusLabel.setText("Searching for the longest simple path...");
        }
    }

    private void showError(String title, String message) {
        statusLabel.setForeground(new Color(185, 28, 28));
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private class GraphPanel extends JPanel {
        private final Map<String, Point> cityPositions = new HashMap<>();
        private Graph graph;
        private List<String> highlightedPath = List.of();

        private GraphPanel() {
            setPreferredSize(new Dimension(MAP_WIDTH, MAP_HEIGHT));
            setBackground(new Color(251, 253, 255));
            setBorder(BorderFactory.createLineBorder(new Color(215, 228, 237)));
        }

        private void setGraph(Graph graph, List<String> highlightedPath) {
            this.graph = graph;
            this.highlightedPath = List.copyOf(highlightedPath);
            assignCityPositions();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (graph == null) {
                return;
            }

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawEdges(graphics2D);
            drawCities(graphics2D);
            graphics2D.dispose();
        }

        private void assignCityPositions() {
            if (graph == null) {
                return;
            }

            List<String> cities = graph.getCities();
            cityPositions.keySet().retainAll(cities);

            for (int index = 0; index < cities.size(); index++) {
                String city = cities.get(index);
                double angle = (2 * Math.PI * index) / Math.max(cities.size(), 1) - Math.PI / 2;
                int centerX = MAP_WIDTH / 2;
                int centerY = MAP_HEIGHT / 2;
                int radiusX = 275;
                int radiusY = 160;
                int x = (int) Math.round(centerX + radiusX * Math.cos(angle));
                int y = (int) Math.round(centerY + radiusY * Math.sin(angle));
                cityPositions.put(city, new Point(x, y));
            }
        }

        private void drawEdges(Graphics2D graphics2D) {
            List<String> drawnEdges = new ArrayList<>();

            for (String source : graph.getCities()) {
                for (Graph.Edge edge : graph.getNeighbors(source)) {
                    String key = createEdgeKey(source, edge.destination());
                    if (drawnEdges.contains(key)) {
                        continue;
                    }

                    Point sourcePoint = cityPositions.get(source);
                    Point destinationPoint = cityPositions.get(edge.destination());
                    boolean highlighted = isEdgeInPath(source, edge.destination());

                    graphics2D.setColor(highlighted ? new Color(239, 68, 68) : new Color(148, 163, 184));
                    graphics2D.setStroke(new BasicStroke(highlighted ? 4f : 2f));
                    graphics2D.drawLine(sourcePoint.x, sourcePoint.y, destinationPoint.x, destinationPoint.y);

                    graphics2D.setColor(new Color(72, 97, 116));
                    graphics2D.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    int labelX = (sourcePoint.x + destinationPoint.x) / 2;
                    int labelY = (sourcePoint.y + destinationPoint.y) / 2;
                    graphics2D.drawString(edge.distance() + " km", labelX, labelY);

                    drawnEdges.add(key);
                }
            }
        }

        private void drawCities(Graphics2D graphics2D) {
            for (String city : graph.getCities()) {
                Point point = cityPositions.get(city);
                boolean highlighted = highlightedPath.contains(city);

                graphics2D.setColor(highlighted ? new Color(249, 115, 22) : new Color(14, 165, 233));
                graphics2D.fillOval(point.x - 24, point.y - 24, 48, 48);
                graphics2D.setColor(Color.WHITE);
                graphics2D.setStroke(new BasicStroke(3f));
                graphics2D.drawOval(point.x - 24, point.y - 24, 48, 48);

                graphics2D.setFont(new Font("Verdana", Font.BOLD, 12));
                FontMetrics metrics = graphics2D.getFontMetrics();
                int textX = point.x - metrics.stringWidth(city) / 2;
                int textY = point.y + metrics.getAscent() / 2 - 2;
                graphics2D.drawString(city, textX, textY);
            }
        }

        private boolean isEdgeInPath(String source, String destination) {
            for (int index = 0; index < highlightedPath.size() - 1; index++) {
                String current = highlightedPath.get(index);
                String next = highlightedPath.get(index + 1);
                if ((current.equals(source) && next.equals(destination))
                        || (current.equals(destination) && next.equals(source))) {
                    return true;
                }
            }
            return false;
        }

        private String createEdgeKey(String cityOne, String cityTwo) {
            return cityOne.compareTo(cityTwo) <= 0 ? cityOne + "-" + cityTwo : cityTwo + "-" + cityOne;
        }
    }
}
