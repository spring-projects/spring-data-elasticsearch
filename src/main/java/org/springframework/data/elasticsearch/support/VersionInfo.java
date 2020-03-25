/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.support;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.elasticsearch.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * This class is used to log the versions of Spring Data Elasticsearch, the Elasticsearch client libs used to built, the
 * Elasticsearch client libs currently used and of the Elasticsearch cluster. If differences greater than a patchlevel
 * are detected, these are logged as warnings.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 */
public final class VersionInfo {

	private static final Logger LOG = LoggerFactory.getLogger(VersionInfo.class);
	private static final AtomicBoolean initialized = new AtomicBoolean(false);
	private static String VERSION_PROPERTIES = "versions.properties";

	/**
	 * logs the relevant version info the first time it is called. Does nothing after the first call
	 * 
	 * @param clusterVersion the version of the cluster
	 */
	public static void logVersions(@Nullable String clusterVersion) {
		if (!initialized.getAndSet(true)) {
			try {
				InputStream resource = VersionInfo.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES);
				if (resource != null) {
					Properties properties = new Properties();
					properties.load(resource);

					String versionSpringDataElasticsearch = properties.getProperty("version.spring-data-elasticsearch");
					Version versionESBuilt = Version.fromString(properties.getProperty("version.elasticsearch-client"));
					Version versionESUsed = Version.CURRENT;
					Version versionESCluster = clusterVersion != null ? Version.fromString(clusterVersion) : null;

					LOG.info("Version Spring Data Elasticsearch: {}", versionSpringDataElasticsearch.toString());
					LOG.info("Version Elasticsearch Client in build: {}", versionESBuilt.toString());
					LOG.info("Version Elasticsearch Client used: {}", versionESUsed.toString());

					if (differInMajorOrMinor(versionESBuilt, versionESUsed)) {
						LOG.warn("Version mismatch in between Elasticsearch Clients build/use: {} - {}", versionESBuilt,
								versionESUsed);
					}

					if (versionESCluster != null) {
						LOG.info("Version Elasticsearch cluster: {}", versionESCluster.toString());

						if (differInMajorOrMinor(versionESUsed, versionESCluster)) {
							LOG.warn("Version mismatch in between Elasticsearch Client and Cluster: {} - {}", versionESUsed,
									versionESCluster);
						}
					}
				} else {
					LOG.warn("cannot load {}", VERSION_PROPERTIES);
				}
			} catch (Exception e) {
				LOG.warn("Could not log version info: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			}

		}
	}

	private static boolean differInMajorOrMinor(Version version1, Version version2) {
		return version1.major != version2.major || version1.minor != version2.minor;
	}

	private VersionInfo() {}
}
