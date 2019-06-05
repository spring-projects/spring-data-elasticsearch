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

import java.net.InetSocketAddress;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility to parse endpoints in {@code host:port} format into {@link java.net.InetSocketAddress}.
 *
 * @author Mark Paluch
 * @since 3.2
 */
class InetSocketAddressParser {

	/**
	 * Parse a host and port string into a {@link InetSocketAddress}.
	 *
	 * @param hostPortString Hostname/IP address and port formatted as {@code host:port} or {@code host}.
	 * @param defaultPort default port to apply if {@code hostPostString} does not contain a port.
	 * @return a {@link InetSocketAddress} that is unresolved to avoid DNS lookups.
	 * @see InetSocketAddress#createUnresolved(String, int)
	 */
	static InetSocketAddress parse(String hostPortString, int defaultPort) {

		Assert.notNull(hostPortString, "HostPortString must not be null");
		String host;
		String portString = null;

		if (hostPortString.startsWith("[")) {
			String[] hostAndPort = getHostAndPortFromBracketedHost(hostPortString);
			host = hostAndPort[0];
			portString = hostAndPort[1];
		} else {
			int colonPos = hostPortString.indexOf(':');
			if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
				// Exactly 1 colon. Split into host:port.
				host = hostPortString.substring(0, colonPos);
				portString = hostPortString.substring(colonPos + 1);
			} else {
				// 0 or 2+ colons. Bare hostname or IPv6 literal.
				host = hostPortString;
			}
		}

		int port = defaultPort;
		if (StringUtils.hasText(portString)) {
			// Try to parse the whole port string as a number.
			Assert.isTrue(!portString.startsWith("+"), String.format("Cannot parse port number: %s", hostPortString));
			try {
				port = Integer.parseInt(portString);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(String.format("Cannot parse port number: %s", hostPortString));
			}

			Assert.isTrue(isValidPort(port), String.format("Port number out of range: %s", hostPortString));
		}

		return InetSocketAddress.createUnresolved(host, port);
	}

	/**
	 * Parses a bracketed host-port string, throwing IllegalArgumentException if parsing fails.
	 *
	 * @param hostPortString the full bracketed host-port specification. Post might not be specified.
	 * @return an array with 2 strings: host and port, in that order.
	 * @throws IllegalArgumentException if parsing the bracketed host-port string fails.
	 */
	private static String[] getHostAndPortFromBracketedHost(String hostPortString) {

		Assert.isTrue(hostPortString.charAt(0) == '[',
				String.format("Bracketed host-port string must start with a bracket: %s", hostPortString));

		int colonIndex = hostPortString.indexOf(':');
		int closeBracketIndex = hostPortString.lastIndexOf(']');

		Assert.isTrue(colonIndex > -1 && closeBracketIndex > colonIndex,
				String.format("Invalid bracketed host/port: %s", hostPortString));

		String host = hostPortString.substring(1, closeBracketIndex);
		if (closeBracketIndex + 1 == hostPortString.length()) {
			return new String[] { host, "" };
		} else {

			Assert.isTrue(hostPortString.charAt(closeBracketIndex + 1) == ':',
					"Only a colon may follow a close bracket: " + hostPortString);
			for (int i = closeBracketIndex + 2; i < hostPortString.length(); ++i) {
				Assert.isTrue(Character.isDigit(hostPortString.charAt(i)),
						String.format("Port must be numeric: %s", hostPortString));
			}
			return new String[] { host, hostPortString.substring(closeBracketIndex + 2) };
		}
	}

	/**
	 * @param port the port number
	 * @return {@literal true} for valid port numbers.
	 */
	private static boolean isValidPort(int port) {
		return port >= 0 && port <= 65535;
	}
}
