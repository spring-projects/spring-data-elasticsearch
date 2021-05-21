/*
 * Copyright 2020-2021 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

	protected static final String VERSION_PROPERTIES = "versions.properties";
	public static final String VERSION_SPRING_DATA_ELASTICSEARCH = "version.spring-data-elasticsearch";
	public static final String VERSION_ELASTICSEARCH_CLIENT = "version.elasticsearch-client";

	private static Properties versionProperties;

	public static Properties versionProperties() {
		return versionProperties;
	}

	static {
		try {
			versionProperties = loadVersionProperties();
		} catch (IOException e) {
			LOG.error("Could not load {}", VERSION_PROPERTIES, e);
			versionProperties = new Properties();
			versionProperties.put(VERSION_SPRING_DATA_ELASTICSEARCH, "0.0.0");
			versionProperties.put(VERSION_ELASTICSEARCH_CLIENT, "0.0.0");
		}
	}

	/**
	 * logs the relevant version info.
	 *
	 * @param clusterVersion the version of the cluster
	 */
	public static void logVersions(@Nullable String clusterVersion) {
		try {

			String versionSpringDataElasticsearch = versionProperties.getProperty(VERSION_SPRING_DATA_ELASTICSEARCH);
			Version versionESBuilt = Version.fromString(versionProperties.getProperty(VERSION_ELASTICSEARCH_CLIENT));
			Version versionESUsed = Version.CURRENT;
			Version versionESCluster = clusterVersion != null ? Version.fromString(clusterVersion) : null;

			LOG.info("Version Spring Data Elasticsearch: {}", versionSpringDataElasticsearch.toString());
			LOG.info("Version Elasticsearch Client in build: {}", versionESBuilt.toString());
			LOG.info("Version Elasticsearch Client used: {}", versionESUsed.toString());

			if (differInMajorOrMinor(versionESBuilt, versionESUsed)) {
				LOG.warn("Version mismatch in between Elasticsearch Clients build/use: {} - {}", versionESBuilt, versionESUsed);
			}

			if (versionESCluster != null) {
				LOG.info("Version Elasticsearch cluster: {}", versionESCluster.toString());

				if (differInMajorOrMinor(versionESUsed, versionESCluster)) {
					LOG.warn("Version mismatch in between Elasticsearch Client and Cluster: {} - {}", versionESUsed,
							versionESCluster);
				}
			}
		} catch (Exception e) {
			LOG.warn("Could not log version info: {} - {}", e.getClass().getSimpleName(), e.getMessage());
		}
	}

	/**
	 * gets the version properties from the classpath resource.
	 *
	 * @return version properties
	 * @throws IOException when an error occurs
	 */
	private static Properties loadVersionProperties() throws IOException {
		InputStream resource = VersionInfo.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES);
		if (resource != null) {
			Properties properties = new Properties();
			properties.load(resource);
			return properties;
		} else {
			throw new IllegalStateException("Resource not found");
		}
	}

	private static boolean differInMajorOrMinor(Version version1, Version version2) {
		return version1.major != version2.major || version1.minor != version2.minor;
	}

	private VersionInfo() {}
}
