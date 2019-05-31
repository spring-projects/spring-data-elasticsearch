/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Version;

/**
 * @author Christoph Strobl
 */
public class ElasticsearchVersionRule implements TestRule {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchVersionRule.class);

	private static final Version ANY = new Version(9999, 9999, 9999);
	private static final Version DEFAULT_HIGH = ANY;
	private static final Version DEFAULT_LOW = new Version(0, 0, 0);

	private final static AtomicReference<Version> currentVersion = new AtomicReference<>(null);
	private final Version minVersion;
	private final Version maxVersion;

	public ElasticsearchVersionRule(Version min, Version max) {

		this.minVersion = min;
		this.maxVersion = max;
	}

	public static ElasticsearchVersionRule any() {
		return new ElasticsearchVersionRule(ANY, ANY);
	}

	public static ElasticsearchVersionRule atLeast(Version minVersion) {
		return new ElasticsearchVersionRule(minVersion, DEFAULT_HIGH);
	}

	public static ElasticsearchVersionRule atMost(Version maxVersion) {
		return new ElasticsearchVersionRule(DEFAULT_LOW, maxVersion);
	}

	@Override
	public Statement apply(Statement base, Description description) {

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {

				if (!getCurrentVersion().equals(ANY)) {

					Version minVersion = ElasticsearchVersionRule.this.minVersion.equals(ANY) ? DEFAULT_LOW
							: ElasticsearchVersionRule.this.minVersion;
					Version maxVersion = ElasticsearchVersionRule.this.maxVersion.equals(ANY) ? DEFAULT_HIGH
							: ElasticsearchVersionRule.this.maxVersion;

					if (description.getAnnotation(ElasticsearchVersion.class) != null) {
						ElasticsearchVersion version = description.getAnnotation(ElasticsearchVersion.class);
						if (version != null) {

							Version expectedMinVersion = Version.parse(version.asOf());
							if (!expectedMinVersion.equals(ANY) && !expectedMinVersion.equals(DEFAULT_LOW)) {
								minVersion = expectedMinVersion;
							}

							Version expectedMaxVersion = Version.parse(version.until());
							if (!expectedMaxVersion.equals(ANY) && !expectedMaxVersion.equals(DEFAULT_HIGH)) {
								maxVersion = expectedMaxVersion;
							}
						}
					}

					validateVersion(minVersion, maxVersion);
				}

				base.evaluate();
			}
		};
	}

	private void validateVersion(Version min, Version max) {

		if (getCurrentVersion().isLessThan(min) || getCurrentVersion().isGreaterThanOrEqualTo(max)) {

			throw new AssumptionViolatedException(String
					.format("Expected Elasticsearch server to be in range (%s, %s] but found %s", min, max, currentVersion));
		}

	}

	private Version getCurrentVersion() {

		if (currentVersion.get() == null) {

			Version current = fetchCurrentVersion();
			if (currentVersion.compareAndSet(null, current)) {
				logger.info("Running Elasticsearch " + current);
			}
		}

		return currentVersion.get();
	}

	private Version fetchCurrentVersion() {
		return TestUtils.serverVersion();
	}

	@Override
	public String toString() {
		return getCurrentVersion().toString();
	}
}
