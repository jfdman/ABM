package org.sandag.abm.active;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class DijkstraOOShortestPath<N extends Node,E extends Edge<N>,T extends Traversal<E>> implements ShortestPath<N> {
	private final Network<N,E,T> network;
	private final TraversalEvaluator<T> traversalEvaluator;
	
	public DijkstraOOShortestPath(Network<N,E,T> network, TraversalEvaluator<T> traversalEvaluator) {
		this.network = network;
		this.traversalEvaluator = traversalEvaluator;
	}
	
	private class TraversedEdge implements Comparable<TraversedEdge> {
		private final E edge;
		private final double cost;
		private final Path<N> path;
		
		private TraversedEdge(E edge, double cost, Path<N> path) {
			this.edge = edge;
			this.cost = cost;
			this.path = path;
		}
		
		public int compareTo(TraversedEdge other) {
			return (int) Math.signum(cost - other.cost);
		}
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
		ShortestPathResultsContainer<N> spResults = new BasicShortestPathResults<>();
		for (N originNode : originNodes)
			spResults.addAll(getShortestPaths(originNode,destinationNodes,maxCost));
		return spResults;
	}
	
	private ShortestPathResults<N> getShortestPaths(N originNode, Set<N> destinationNodes, double maxCost) {
		//List<Node> orderedNodes = getOrderedNodeList();
		Map<N,Path<N>> paths = new HashMap<>();         //zone node paths
		Map<N,Double> costs = new HashMap<>();          //zone node costs
		Map<E,Double> finalCosts = new HashMap<>();     //cost to (and including) edge
		
		PriorityQueue<TraversedEdge> traversalQueue = new PriorityQueue<>();
		
		Set<N> targets = new HashSet<>(destinationNodes);
		Path<N> basePath = new Path<>(originNode);
		if (targets.contains(originNode)) {
			targets.remove(originNode);
			costs.put(originNode,0.0);
			paths.put(originNode,basePath);
		}
		
		//initialize traversalQueue and costs
		for (N successor : network.getSuccessors(originNode)) {
			T traversal = network.getNullTraversal(network.getEdge(originNode,successor));
			double traversalCost = evaluateTraversal(traversal);
			if (traversalCost <= maxCost) {
				TraversedEdge traversedEdge = new TraversedEdge(traversal.getToEdge(),traversalCost,basePath.extendPath(successor));
				traversalQueue.add(traversedEdge);
			}
		}
		
		//dijkstra
		while (!traversalQueue.isEmpty() && !targets.isEmpty()) {
			TraversedEdge traversedEdge = traversalQueue.poll();
			E edge = traversedEdge.edge;
			
			if (finalCosts.containsKey(edge)) //already considered
				continue;
			Path<N> path = traversedEdge.path;
			double cost = traversedEdge.cost;
			
			finalCosts.put(edge,cost);
			N toNode = edge.getToNode();
			if (targets.remove(toNode)) {
				paths.put(toNode,path);
				costs.put(toNode,cost);
			}
			
			for (N successor : network.getSuccessors(toNode)) {
				T traversal = network.getTraversal(traversedEdge.edge,network.getEdge(toNode,successor));
				double traversalCost = cost + evaluateTraversal(traversal);
				if (traversalCost <= maxCost) {
					traversedEdge = new TraversedEdge(traversal.getToEdge(),traversalCost,path.extendPath(successor));
					traversalQueue.add(traversedEdge);
				}
			}
		}
		
		BasicShortestPathResults<N> spResults = new BasicShortestPathResults<>();
		for (N destinationNode : destinationNodes) {
			boolean pathDefined = paths.containsKey(destinationNode);
			Path<N> path = pathDefined ? paths.get(destinationNode) : null;
			double cost = pathDefined ? costs.get(destinationNode) : Double.POSITIVE_INFINITY;
			spResults.addResult(new NodePair<N>(originNode,destinationNode),path,cost);
		}
		
		return spResults;
	}
	
	protected double evaluateTraversal(T traversal) {
		return traversalEvaluator.evaluate(traversal);
	}

}