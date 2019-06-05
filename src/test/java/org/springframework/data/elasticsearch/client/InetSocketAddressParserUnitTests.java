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
package org.springframework.data.elasticsearch.client;

import static org.assertj.core.api.Assertions.*;

import java.net.InetSocketAddress;

import org.junit.Test;

/**
 * Unit tests for {@link InetSocketAddressParser}.
 *
 * @author Mark Paluch
 */
public class InetSocketAddressParserUnitTests {

	@Test
	public void testFromStringWellFormed() {
		// Well-formed inputs.
		checkFromStringCase("pivotal.io", 80, "pivotal.io", 80, false);
		checkFromStringCase("pivotal.io", 80, "pivotal.io", 80, false);
		checkFromStringCase("192.0.2.1", 82, "192.0.2.1", 82, false);
		checkFromStringCase("[2001::1]", 84, "2001::1", 84, false);
		checkFromStringCase("2001::3", 86, "2001::3", 86, false);
		checkFromStringCase("host:", 80, "host", 80, false);
	}

	@Test
	public void testFromStringBadDefaultPort() {
		// Well-formed strings with bad default ports.
		checkFromStringCase("gmail.com:81", -1, "gmail.com", 81, true);
		checkFromStringCase("192.0.2.2:83", -1, "192.0.2.2", 83, true);
		checkFromStringCase("[2001::2]:85", -1, "2001::2", 85, true);
		checkFromStringCase("goo.gl:65535", 65536, "goo.gl", 65535, true);
		// No port, bad default.
		checkFromStringCase("pivotal.io", -1, null, -1, false);
		checkFromStringCase("192.0.2.1", 65536, null, -1, false);
		checkFromStringCase("[2001::1]", -1, null, -1, false);
		checkFromStringCase("2001::3", 65536, null, -1, false);
	}

	@Test
	public void testFromStringUnusedDefaultPort() {
		// Default port, but unused.
		checkFromStringCase("gmail.com:81", 77, "gmail.com", 81, true);
		checkFromStringCase("192.0.2.2:83", 77, "192.0.2.2", 83, true);
		checkFromStringCase("[2001::2]:85", 77, "2001::2", 85, true);
	}

	@Test
	public void testFromStringBadPort() {
		// Out-of-range ports.
		checkFromStringCase("pivotal.io:65536", 1, null, 99, false);
		checkFromStringCase("pivotal.io:9999999999", 1, null, 99, false);
		// Invalid port parts.
		checkFromStringCase("pivotal.io:port", 1, null, 99, false);
		checkFromStringCase("pivotal.io:-25", 1, null, 99, false);
		checkFromStringCase("pivotal.io:+25", 1, null, 99, false);
		checkFromStringCase("pivotal.io:25  ", 1, null, 99, false);
		checkFromStringCase("pivotal.io:25\t", 1, null, 99, false);
		checkFromStringCase("pivotal.io:0x25 ", 1, null, 99, false);
	}

	@Test
	public void testFromStringUnparseableNonsense() {
		// Some nonsense that causes parse failures.
		checkFromStringCase("[goo.gl]", 1, null, 99, false);
		checkFromStringCase("[goo.gl]:80", 1, null, 99, false);
		checkFromStringCase("[", 1, null, 99, false);
		checkFromStringCase("[]:", 1, null, 99, false);
		checkFromStringCase("[]:80", 1, null, 99, false);
		checkFromStringCase("[]bad", 1, null, 99, false);
	}

	@Test
	public void testFromStringParseableNonsense() {
		// Examples of nonsense that gets through.
		checkFromStringCase("[[:]]", 86, "[:]", 86, false);
		checkFromStringCase("x:y:z", 87, "x:y:z", 87, false);
		checkFromStringCase("", 88, "", 88, false);
		checkFromStringCase(":", 99, "", 99, false);
		checkFromStringCase(":123", -1, "", 123, true);
		checkFromStringCase("\nOMG\t", 89, "\nOMG\t", 89, false);
	}

	private static void checkFromStringCase(String hpString, int defaultPort, String expectHost, int expectPort,
			boolean expectHasExplicitPort) {
		InetSocketAddress hp;

		try {
			hp = InetSocketAddressParser.parse(hpString, defaultPort);
		} catch (IllegalArgumentException e) {
			// Make sure we expected this.
			assertThat(expectHost).isNull();
			return;
		}

		assertThat(expectHost).isNotNull();

		if (expectHasExplicitPort) {
			assertThat(hp.getPort()).isEqualTo(expectPort);
		} else {
			assertThat(hp.getPort()).isEqualTo(defaultPort);
		}

		assertThat(hp.getHostString()).isEqualTo(expectHost);
	}
}
