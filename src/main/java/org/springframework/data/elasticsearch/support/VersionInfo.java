/*
 * Copyright 2020-2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log LOGGER = LogFactory.getLog(VersionInfo.class);

	private static final String VERSION_PROPERTIES = "versions.properties";

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
			LOGGER.error("Could not load " + VERSION_PROPERTIES, e);
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
	public static void logVersions(String vendor, String runtimeLibraryVersion, @Nullable String clusterVersion) {
		try {

			String versionSpringDataElasticsearch = versionProperties.getProperty(VERSION_SPRING_DATA_ELASTICSEARCH);
			Version versionBuiltLibraryES = Version.fromString(versionProperties.getProperty(VERSION_ELASTICSEARCH_CLIENT));
			Version versionRuntimeLibrary = Version.fromString(runtimeLibraryVersion);
			Version versionCluster = clusterVersion != null ? Version.fromString(clusterVersion) : null;

			LOGGER.info(String.format("Version Spring Data Elasticsearch: %s", versionSpringDataElasticsearch));
			LOGGER.info(String.format("Version Elasticsearch client in build: %s", versionBuiltLibraryES));
			LOGGER.info(String.format("Version runtime client used: %s - %s", vendor, versionRuntimeLibrary));

			if (differInMajorOrMinor(versionBuiltLibraryES, versionRuntimeLibrary)) {
				LOGGER.warn(String.format("Version mismatch in between Elasticsearch Clients build/use: %s - %s",
						versionBuiltLibraryES, versionRuntimeLibrary));
			}

			if (versionCluster != null) {
				LOGGER.info(String.format("Version cluster: %s - %s", vendor, versionCluster));

				if (differInMajorOrMinor(versionRuntimeLibrary, versionCluster)) {
					LOGGER.warn(String.format("Version mismatch in between  Client and Cluster: %s - %s - %s", vendor,
							versionRuntimeLibrary, versionCluster));
				}
			}
		} catch (Exception e) {
			LOGGER.warn(String.format("Could not log version info: %s - %s", e.getClass().getSimpleName(), e.getMessage()));
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
		return version1.major() != version2.major() || version1.minor() != version2.minor();
	}

	private VersionInfo() {}
}
