class Graph {
    constructor() {
        this.adjacencyList = new Map();
    }

    addCity(city) {
        const normalizedCity = this.#normalizeCity(city);
        if (!this.adjacencyList.has(normalizedCity)) {
            this.adjacencyList.set(normalizedCity, []);
        }
    }

    addBidirectionalEdge(source, destination, distance) {
        const normalizedSource = this.#normalizeCity(source);
        const normalizedDestination = this.#normalizeCity(destination);
        const normalizedDistance = Number(distance);

        if (normalizedSource === normalizedDestination) {
            throw new Error("Source and destination cities must be different.");
        }
        if (!Number.isFinite(normalizedDistance) || normalizedDistance <= 0) {
            throw new Error("Distance must be a positive number.");
        }

        this.addCity(normalizedSource);
        this.addCity(normalizedDestination);

        this.#addOrReplaceDirectedEdge(normalizedSource, normalizedDestination, normalizedDistance);
        this.#addOrReplaceDirectedEdge(normalizedDestination, normalizedSource, normalizedDistance);
    }

    containsCity(city) {
        if (!city) {
            return false;
        }
        return this.adjacencyList.has(city.trim());
    }

    getCities() {
        return [...this.adjacencyList.keys()].sort((left, right) => left.localeCompare(right));
    }

    getNeighbors(city) {
        if (!this.containsCity(city)) {
            return [];
        }
        return [...this.adjacencyList.get(city.trim())];
    }

    getUniqueEdges() {
        const uniqueEdges = [];
        const seen = new Set();

        for (const source of this.getCities()) {
            for (const edge of this.getNeighbors(source)) {
                const key = [source, edge.destination].sort().join("::");
                if (!seen.has(key)) {
                    seen.add(key);
                    uniqueEdges.push({
                        source,
                        destination: edge.destination,
                        distance: edge.distance
                    });
                }
            }
        }

        return uniqueEdges;
    }

    copy() {
        const clone = new Graph();
        for (const city of this.getCities()) {
            clone.addCity(city);
            for (const edge of this.getNeighbors(city)) {
                clone.#addOrReplaceDirectedEdge(city, edge.destination, edge.distance);
            }
        }
        return clone;
    }

    #addOrReplaceDirectedEdge(source, destination, distance) {
        const neighbors = this.adjacencyList.get(source);
        const existingIndex = neighbors.findIndex((edge) => edge.destination === destination);
        const nextEdge = { destination, distance };

        if (existingIndex >= 0) {
            neighbors[existingIndex] = nextEdge;
            return;
        }

        neighbors.push(nextEdge);
    }

    #normalizeCity(city) {
        if (typeof city !== "string" || !city.trim()) {
            throw new Error("City name cannot be empty.");
        }
        return city.trim();
    }
}

class LongestPathAlgorithm {
    findLongestPath(graph, source, destination) {
        if (!(graph instanceof Graph)) {
            throw new Error("A valid graph instance is required.");
        }
        if (!source || !destination) {
            return this.#failure("Please select both source and destination cities.");
        }

        const normalizedSource = source.trim();
        const normalizedDestination = destination.trim();

        if (!graph.containsCity(normalizedSource) || !graph.containsCity(normalizedDestination)) {
            return this.#failure("Selected city does not exist in the graph.");
        }
        if (normalizedSource === normalizedDestination) {
            return this.#success([normalizedSource], 0);
        }

        const bestPath = {
            path: [],
            totalDistance: Number.NEGATIVE_INFINITY
        };

        this.#search(graph, normalizedSource, normalizedDestination, new Set([normalizedSource]),
            [normalizedSource], 0, bestPath);

        if (bestPath.path.length === 0) {
            return this.#failure(`No path exists between ${normalizedSource} and ${normalizedDestination}.`);
        }

        return this.#success(bestPath.path, bestPath.totalDistance);
    }

    #search(graph, currentCity, destination, visited, path, distance, bestPath) {
        if (currentCity === destination && distance > bestPath.totalDistance) {
            bestPath.path = [...path];
            bestPath.totalDistance = distance;
            return;
        }

        for (const edge of graph.getNeighbors(currentCity)) {
            if (visited.has(edge.destination)) {
                continue;
            }

            visited.add(edge.destination);
            path.push(edge.destination);

            this.#search(graph, edge.destination, destination, visited, path,
                distance + edge.distance, bestPath);

            path.pop();
            visited.delete(edge.destination);
        }
    }

    #success(path, totalDistance) {
        return {
            pathExists: true,
            path,
            totalDistance,
            message: "Longest simple path found."
        };
    }

    #failure(message) {
        return {
            pathExists: false,
            path: [],
            totalDistance: null,
            message
        };
    }
}

class GraphVisualizer {
    constructor(containerSelector) {
        this.container = d3.select(containerSelector);
        this.width = 920;
        this.height = 620;
        this.svg = this.container.append("svg")
            .attr("class", "graph-svg")
            .attr("viewBox", `0 0 ${this.width} ${this.height}`)
            .attr("role", "img")
            .attr("aria-label", "Longest path graph visualization");

        this.defs = this.svg.append("defs");
        this.defs.append("filter")
            .attr("id", "node-shadow")
            .html(`
                <feDropShadow dx="0" dy="8" stdDeviation="8" flood-color="rgba(34, 25, 17, 0.28)"></feDropShadow>
            `);

        this.edgeLayer = this.svg.append("g");
        this.distanceLayer = this.svg.append("g");
        this.pathLayer = this.svg.append("g");
        this.nodeLayer = this.svg.append("g");
        this.labelLayer = this.svg.append("g");
        this.currentAnimationToken = 0;
    }

    render(graph, highlightedPath = []) {
        const cities = graph.getCities();
        const nodes = cities.map((city, index) => ({
            id: city,
            ...this.#computePosition(index, cities.length)
        }));
        const nodeMap = new Map(nodes.map((node) => [node.id, node]));
        const edges = graph.getUniqueEdges().map((edge) => ({
            ...edge,
            sourceNode: nodeMap.get(edge.source),
            destinationNode: nodeMap.get(edge.destination)
        }));

        this.currentEdges = edges;
        this.currentNodes = nodes;
        this.#renderEdges(edges, highlightedPath);
        this.#renderNodes(nodes, highlightedPath);
    }

    async animatePath(path) {
        this.currentAnimationToken += 1;
        const animationToken = this.currentAnimationToken;
        this.pathLayer.selectAll("*").remove();

        if (!Array.isArray(path) || path.length < 2) {
            return;
        }

        for (let index = 0; index < path.length - 1; index += 1) {
            if (animationToken !== this.currentAnimationToken) {
                return;
            }

            const source = path[index];
            const destination = path[index + 1];
            const edge = this.currentEdges.find((candidate) =>
                (candidate.source === source && candidate.destination === destination)
                || (candidate.source === destination && candidate.destination === source)
            );

            if (!edge) {
                continue;
            }

            await this.#drawAnimatedSegment(edge, animationToken);
            this.#pulseNode(destination, animationToken);
            await this.#wait(380);
        }
    }

    cancelAnimation() {
        this.currentAnimationToken += 1;
        this.pathLayer.selectAll("*").remove();
    }

    #renderEdges(edges, highlightedPath) {
        const edgeSelection = this.edgeLayer.selectAll("line")
            .data(edges, (edge) => `${edge.source}-${edge.destination}`);

        edgeSelection.join(
            (enter) => enter.append("line"),
            (update) => update,
            (exit) => exit.remove()
        )
            .attr("x1", (edge) => edge.sourceNode.x)
            .attr("y1", (edge) => edge.sourceNode.y)
            .attr("x2", (edge) => edge.destinationNode.x)
            .attr("y2", (edge) => edge.destinationNode.y)
            .attr("stroke", (edge) => this.#isEdgeInPath(edge, highlightedPath) ? "#db6f2f" : "#b2b2b2")
            .attr("stroke-opacity", (edge) => this.#isEdgeInPath(edge, highlightedPath) ? 0.48 : 0.8)
            .attr("stroke-width", (edge) => this.#isEdgeInPath(edge, highlightedPath) ? 4 : 2.4);

        const labels = this.distanceLayer.selectAll("text")
            .data(edges, (edge) => `${edge.source}-${edge.destination}`);

        labels.join(
            (enter) => enter.append("text").attr("class", "distance-label"),
            (update) => update,
            (exit) => exit.remove()
        )
            .attr("x", (edge) => (edge.sourceNode.x + edge.destinationNode.x) / 2)
            .attr("y", (edge) => (edge.sourceNode.y + edge.destinationNode.y) / 2 - 8)
            .text((edge) => `${edge.distance} km`);
    }

    #renderNodes(nodes, highlightedPath) {
        const nodeSelection = this.nodeLayer.selectAll("circle")
            .data(nodes, (node) => node.id);

        nodeSelection.join(
            (enter) => enter.append("circle"),
            (update) => update,
            (exit) => exit.remove()
        )
            .attr("cx", (node) => node.x)
            .attr("cy", (node) => node.y)
            .attr("r", 28)
            .attr("fill", (node) => highlightedPath.includes(node.id) ? "#db6f2f" : "#0e6385")
            .attr("stroke", "#fff8eb")
            .attr("stroke-width", 4)
            .attr("filter", "url(#node-shadow)");

        const labels = this.labelLayer.selectAll("text")
            .data(nodes, (node) => node.id);

        labels.join(
            (enter) => enter.append("text").attr("class", "node-label"),
            (update) => update,
            (exit) => exit.remove()
        )
            .attr("x", (node) => node.x)
            .attr("y", (node) => node.y)
            .text((node) => node.id);
    }

    #computePosition(index, totalCities) {
        const angle = ((Math.PI * 2) / Math.max(totalCities, 1)) * index - Math.PI / 2;
        const orbitX = this.width * 0.35;
        const orbitY = this.height * 0.28;
        return {
            x: this.width / 2 + orbitX * Math.cos(angle),
            y: this.height / 2 + orbitY * Math.sin(angle)
        };
    }

    async #drawAnimatedSegment(edge, animationToken) {
        const path = this.pathLayer.append("line")
            .attr("x1", edge.sourceNode.x)
            .attr("y1", edge.sourceNode.y)
            .attr("x2", edge.sourceNode.x)
            .attr("y2", edge.sourceNode.y)
            .attr("stroke", "#ff8b3d")
            .attr("stroke-linecap", "round")
            .attr("stroke-width", 7)
            .attr("opacity", 0.96);

        await new Promise((resolve) => {
            path.transition()
                .duration(650)
                .ease(d3.easeCubicInOut)
                .attr("x2", edge.destinationNode.x)
                .attr("y2", edge.destinationNode.y)
                .on("end", resolve);
        });

        if (animationToken !== this.currentAnimationToken) {
            path.remove();
        }
    }

    #pulseNode(city, animationToken) {
        const node = this.nodeLayer.selectAll("circle")
            .filter((candidate) => candidate.id === city);

        node.raise()
            .transition()
            .duration(220)
            .attr("r", 33)
            .transition()
            .duration(220)
            .attr("r", 28)
            .on("end", () => {
                if (animationToken !== this.currentAnimationToken) {
                    node.interrupt();
                }
            });
    }

    #isEdgeInPath(edge, path) {
        for (let index = 0; index < path.length - 1; index += 1) {
            const from = path[index];
            const to = path[index + 1];
            if ((edge.source === from && edge.destination === to)
                || (edge.source === to && edge.destination === from)) {
                return true;
            }
        }
        return false;
    }

    #wait(duration) {
        return new Promise((resolve) => {
            window.setTimeout(resolve, duration);
        });
    }
}

class AppController {
    constructor() {
        this.graph = this.#createSampleGraph();
        this.algorithm = new LongestPathAlgorithm();
        this.visualizer = new GraphVisualizer("#graph-container");

        this.sourceSelect = document.querySelector("#source-city");
        this.destinationSelect = document.querySelector("#destination-city");
        this.connectSelect = document.querySelector("#connect-city");
        this.pathOutput = document.querySelector("#path-output");
        this.distanceOutput = document.querySelector("#distance-output");
        this.statusOutput = document.querySelector("#status-output");
        this.routesList = document.querySelector("#routes-list");
        this.findButton = document.querySelector("#find-path-button");
        this.addButton = document.querySelector("#add-city-button");
        this.newCityInput = document.querySelector("#new-city-name");
        this.newDistanceInput = document.querySelector("#new-city-distance");

        this.isBusy = false;
        this.#bindEvents();
        this.#refreshControls();
        this.#renderGraph();
        this.#renderRoutes();
    }

    #bindEvents() {
        this.findButton.addEventListener("click", () => this.#handleFindLongestPath());
        this.addButton.addEventListener("click", () => this.#handleAddCity());
    }

    async #handleFindLongestPath() {
        try {
            this.#setBusy(true, "Searching for the longest simple path...");

            const graphSnapshot = this.graph.copy();
            const result = await this.#calculateOnNextFrame(() =>
                this.algorithm.findLongestPath(
                    graphSnapshot,
                    this.sourceSelect.value,
                    this.destinationSelect.value
                )
            );

            this.#applyPathResult(result);
            await this.visualizer.animatePath(result.path);
        } catch (error) {
            this.#setStatus(error.message || "Unable to compute the longest path.", "error");
        } finally {
            this.#setBusy(false);
        }
    }

    #handleAddCity() {
        try {
            const city = this.newCityInput.value.trim();
            const connectTo = this.connectSelect.value;
            const distance = Number(this.newDistanceInput.value);

            if (!city || !connectTo || !Number.isFinite(distance)) {
                throw new Error("Enter a city name, a connection city, and a valid distance.");
            }
            if (this.graph.containsCity(city)) {
                throw new Error("That city already exists.");
            }

            this.graph.addBidirectionalEdge(city, connectTo, distance);
            this.newCityInput.value = "";
            this.newDistanceInput.value = "";

            this.#refreshControls();
            this.#renderGraph();
            this.#renderRoutes();
            this.#setStatus(`Added ${city} and connected it to ${connectTo}.`, "success");
        } catch (error) {
            this.#setStatus(error.message || "Unable to add city.", "error");
        }
    }

    #refreshControls() {
        const cities = this.graph.getCities();
        this.#populateSelect(this.sourceSelect, cities, this.sourceSelect.value || cities[0]);
        this.#populateSelect(this.destinationSelect, cities, this.destinationSelect.value || cities[1] || cities[0]);
        this.#populateSelect(this.connectSelect, cities, this.connectSelect.value || cities[0]);
    }

    #populateSelect(select, cities, preferredValue) {
        const currentValue = cities.includes(preferredValue) ? preferredValue : cities[0];
        select.innerHTML = "";

        for (const city of cities) {
            const option = document.createElement("option");
            option.value = city;
            option.textContent = city;
            select.append(option);
        }

        if (currentValue) {
            select.value = currentValue;
        }
    }

    #renderGraph(highlightedPath = []) {
        this.visualizer.cancelAnimation();
        this.visualizer.render(this.graph, highlightedPath);
    }

    #renderRoutes() {
        this.routesList.innerHTML = "";

        for (const city of this.graph.getCities()) {
            const item = document.createElement("article");
            item.className = "route-item";

            const cityTitle = document.createElement("p");
            cityTitle.className = "route-city";
            cityTitle.textContent = city;

            const neighbors = this.graph.getNeighbors(city)
                .map((edge) => `${edge.destination} (${edge.distance} km)`)
                .join(", ");

            const neighborText = document.createElement("p");
            neighborText.className = "route-neighbors";
            neighborText.textContent = neighbors || "No outgoing routes";

            item.append(cityTitle, neighborText);
            this.routesList.append(item);
        }
    }

    #applyPathResult(result) {
        if (result.pathExists) {
            this.pathOutput.textContent = result.path.join(" → ");
            this.distanceOutput.textContent = `${result.totalDistance} km`;
            this.#setStatus(result.message, "success");
            this.#renderGraph(result.path);
            return;
        }

        this.pathOutput.textContent = "-";
        this.distanceOutput.textContent = "-";
        this.#setStatus(result.message, "error");
        this.#renderGraph([]);
    }

    #setBusy(isBusy, message = "") {
        this.isBusy = isBusy;
        this.findButton.disabled = isBusy;
        this.addButton.disabled = isBusy;
        this.sourceSelect.disabled = isBusy;
        this.destinationSelect.disabled = isBusy;
        this.connectSelect.disabled = isBusy;
        this.newCityInput.disabled = isBusy;
        this.newDistanceInput.disabled = isBusy;

        if (message) {
            this.#setStatus(message, "info");
        }
    }

    #setStatus(message, type = "info") {
        this.statusOutput.textContent = message;
        this.statusOutput.dataset.state = type;
        if (type === "error") {
            this.statusOutput.style.color = "#b5392f";
        } else if (type === "success") {
            this.statusOutput.style.color = "#256048";
        } else {
            this.statusOutput.style.color = "#6b5a47";
        }
    }

    #calculateOnNextFrame(action) {
        return new Promise((resolve, reject) => {
            window.requestAnimationFrame(() => {
                try {
                    resolve(action());
                } catch (error) {
                    reject(error);
                }
            });
        });
    }

    #createSampleGraph() {
        const graph = new Graph();
        graph.addBidirectionalEdge("Delhi", "Mumbai", 1400);
        graph.addBidirectionalEdge("Delhi", "Jaipur", 280);
        graph.addBidirectionalEdge("Delhi", "Lucknow", 555);
        graph.addBidirectionalEdge("Jaipur", "Ahmedabad", 660);
        graph.addBidirectionalEdge("Ahmedabad", "Mumbai", 530);
        graph.addBidirectionalEdge("Lucknow", "Varanasi", 320);
        graph.addBidirectionalEdge("Varanasi", "Kolkata", 680);
        graph.addBidirectionalEdge("Mumbai", "Bengaluru", 980);
        graph.addBidirectionalEdge("Ahmedabad", "Bengaluru", 1490);
        graph.addBidirectionalEdge("Kolkata", "Bengaluru", 1870);
        graph.addBidirectionalEdge("Mumbai", "Hyderabad", 710);
        graph.addBidirectionalEdge("Hyderabad", "Bengaluru", 570);
        return graph;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    new AppController();
});
