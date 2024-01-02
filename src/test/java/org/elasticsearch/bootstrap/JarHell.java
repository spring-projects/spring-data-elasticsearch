/*
 * Copyright 2018-2024 the original author or authors.
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
package org.elasticsearch.bootstrap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/**
 * No words – No words can describe this piece of code and why we cannot opt-in/opt-out from JarHell check.
 * <p/>
 * Elasticsearch wants to raise awareness if there are two classes with the exact same name (class name and package
 * name) to avoid downstream issues. Turns out, in some case, such as Java 9 module descriptors, it's perfectly fine to
 * have exactly same class names (such as {@code module-info.class}) yet JarHell goes awry and prevents startup.
 * <p>
 * This class is here to be loaded before ES's JarHell class and to anyone that wants to survive JarHell, leave it here
 * or you will die a slow and painful death.
 * <p>
 * Oh, by the way: If Elasticsearch decides to upgrade JarHell with new method signatures, we should adapt to these.
 *
 * @author Mark Paluch
 */
public class JarHell {

	private JarHell() {}

	/**
	 * Empty stub. Leave it here or you will die a slow and painful death.
	 *
	 * @param output
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void checkJarHell(Consumer<String> output) throws IOException, URISyntaxException {}

	/**
	 * Empty stub. Leave it here or you will die a slow and painful death.
	 *
	 * @return
	 */
	public static Set<URL> parseClassPath() {
		return Collections.emptySet();
	}

	/**
	 * Empty stub. Leave it here or you will die a slow and painful death.
	 *
	 * @param urls
	 * @param output
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void checkJarHell(Set<URL> urls, Consumer<String> output) throws URISyntaxException, IOException {}

	/**
	 * Empty stub. Leave it here or you will die a slow and painful death.
	 *
	 * @param targetVersion
	 */
	public static void checkVersionFormat(String targetVersion) {}

	/**
	 * Empty stub. Leave it here or you will die a slow and painful death.
	 *
	 * @param resource
	 * @param targetVersion
	 */
	public static void checkJavaVersion(String resource, String targetVersion) {}
}
