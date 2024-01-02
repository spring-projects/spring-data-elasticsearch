/*
 * Copyright 2021-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.cluster;

/**
 * Information about the cluster health. Contains currently only the top level elements returned from Elasticsearch.
 *
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class ClusterHealth {

	private final String clusterName;
	private final String status;
	private final int numberOfNodes;
	private final int numberOfDataNodes;
	private final int activeShards;
	private final int relocatingShards;
	private final int activePrimaryShards;
	private final int initializingShards;
	private final int unassignedShards;
	private final double activeShardsPercent;
	private final int numberOfPendingTasks;
	private final boolean timedOut;
	private final int numberOfInFlightFetch;
	private final int delayedUnassignedShards;
	private final long taskMaxWaitingTimeMillis;

	private ClusterHealth(String clusterName, String status, int numberOfNodes, int numberOfDataNodes, int activeShards,
			int relocatingShards, int activePrimaryShards, int initializingShards, int unassignedShards,
			double activeShardsPercent, int numberOfPendingTasks, boolean timedOut, int numberOfInFlightFetch,
			int delayedUnassignedShards, long taskMaxWaitingTimeMillis) {
		this.clusterName = clusterName;
		this.status = status;
		this.numberOfNodes = numberOfNodes;
		this.numberOfDataNodes = numberOfDataNodes;
		this.activeShards = activeShards;
		this.relocatingShards = relocatingShards;
		this.activePrimaryShards = activePrimaryShards;
		this.initializingShards = initializingShards;
		this.unassignedShards = unassignedShards;
		this.activeShardsPercent = activeShardsPercent;
		this.numberOfPendingTasks = numberOfPendingTasks;
		this.timedOut = timedOut;
		this.numberOfInFlightFetch = numberOfInFlightFetch;
		this.delayedUnassignedShards = delayedUnassignedShards;
		this.taskMaxWaitingTimeMillis = taskMaxWaitingTimeMillis;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getStatus() {
		return status;
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public int getNumberOfDataNodes() {
		return numberOfDataNodes;
	}

	public int getActiveShards() {
		return activeShards;
	}

	public int getRelocatingShards() {
		return relocatingShards;
	}

	public int getActivePrimaryShards() {
		return activePrimaryShards;
	}

	public int getInitializingShards() {
		return initializingShards;
	}

	public int getUnassignedShards() {
		return unassignedShards;
	}

	public double getActiveShardsPercent() {
		return activeShardsPercent;
	}

	public int getNumberOfPendingTasks() {
		return numberOfPendingTasks;
	}

	public boolean isTimedOut() {
		return timedOut;
	}

	public int getNumberOfInFlightFetch() {
		return numberOfInFlightFetch;
	}

	public int getDelayedUnassignedShards() {
		return delayedUnassignedShards;
	}

	public long getTaskMaxWaitingTimeMillis() {
		return taskMaxWaitingTimeMillis;
	}

	@Override
	public String toString() {
		return "ClusterHealth{" + //
				"clusterName='" + clusterName + '\'' + //
				", status='" + status + '\'' + //
				", numberOfNodes=" + numberOfNodes + //
				", numberOfDataNodes=" + numberOfDataNodes + //
				", activeShards=" + activeShards + //
				", relocatingShards=" + relocatingShards + //
				", activePrimaryShards=" + activePrimaryShards + //
				", initializingShards=" + initializingShards + //
				", unassignedShards=" + unassignedShards + //
				", activeShardsPercent=" + activeShardsPercent + //
				", numberOfPendingTasks=" + numberOfPendingTasks + //
				", timedOut=" + timedOut + //
				", numberOfInFlightFetch=" + numberOfInFlightFetch + //
				", delayedUnassignedShards=" + delayedUnassignedShards + //
				", taskMaxWaitingTimeMillis=" + taskMaxWaitingTimeMillis + //
				'}'; //
	}

	public static ClusterHealthBuilder builder() {
		return new ClusterHealthBuilder();
	}

	public static final class ClusterHealthBuilder {
		private String clusterName = "";
		private String status = "";
		private int numberOfNodes;
		private int numberOfDataNodes;
		private int activeShards;
		private int relocatingShards;
		private int activePrimaryShards;
		private int initializingShards;
		private int unassignedShards;
		private double activeShardsPercent;
		private int numberOfPendingTasks;
		private boolean timedOut;
		private int numberOfInFlightFetch;
		private int delayedUnassignedShards;
		private long taskMaxWaitingTimeMillis;

		private ClusterHealthBuilder() {}

		public ClusterHealthBuilder withClusterName(String clusterName) {
			this.clusterName = clusterName;
			return this;
		}

		public ClusterHealthBuilder withStatus(String status) {
			this.status = status.toUpperCase();
			return this;
		}

		public ClusterHealthBuilder withNumberOfNodes(int numberOfNodes) {
			this.numberOfNodes = numberOfNodes;
			return this;
		}

		public ClusterHealthBuilder withNumberOfDataNodes(int numberOfDataNodes) {
			this.numberOfDataNodes = numberOfDataNodes;
			return this;
		}

		public ClusterHealthBuilder withActiveShards(int activeShards) {
			this.activeShards = activeShards;
			return this;
		}

		public ClusterHealthBuilder withRelocatingShards(int relocatingShards) {
			this.relocatingShards = relocatingShards;
			return this;
		}

		public ClusterHealthBuilder withActivePrimaryShards(int activePrimaryShards) {
			this.activePrimaryShards = activePrimaryShards;
			return this;
		}

		public ClusterHealthBuilder withInitializingShards(int initializingShards) {
			this.initializingShards = initializingShards;
			return this;
		}

		public ClusterHealthBuilder withUnassignedShards(int unassignedShards) {
			this.unassignedShards = unassignedShards;
			return this;
		}

		public ClusterHealthBuilder withActiveShardsPercent(double activeShardsPercent) {
			this.activeShardsPercent = activeShardsPercent;
			return this;
		}

		public ClusterHealthBuilder withNumberOfPendingTasks(int numberOfPendingTasks) {
			this.numberOfPendingTasks = numberOfPendingTasks;
			return this;
		}

		public ClusterHealthBuilder withTimedOut(boolean timedOut) {
			this.timedOut = timedOut;
			return this;
		}

		public ClusterHealthBuilder withNumberOfInFlightFetch(int numberOfInFlightFetch) {
			this.numberOfInFlightFetch = numberOfInFlightFetch;
			return this;
		}

		public ClusterHealthBuilder withDelayedUnassignedShards(int delayedUnassignedShards) {
			this.delayedUnassignedShards = delayedUnassignedShards;
			return this;
		}

		public ClusterHealthBuilder withTaskMaxWaitingTimeMillis(long taskMaxWaitingTimeMillis) {
			this.taskMaxWaitingTimeMillis = taskMaxWaitingTimeMillis;
			return this;
		}

		public ClusterHealth build() {
			return new ClusterHealth(clusterName, status, numberOfNodes, numberOfDataNodes, activeShards, relocatingShards,
					activePrimaryShards, initializingShards, unassignedShards, activeShardsPercent, numberOfPendingTasks,
					timedOut, numberOfInFlightFetch, delayedUnassignedShards, taskMaxWaitingTimeMillis);
		}
	}
}
