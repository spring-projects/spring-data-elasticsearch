/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * ElasticsearchTemplate
 *
 * @author Simon Schneider
 */
@Slf4j
public class ClassPathFileUtils {

    public static String readFileFromClasspath(String url) {
        StringBuilder stringBuilder = new StringBuilder();

        BufferedReader bufferedReader = null;

        try {
            ClassPathResource classPathResource = new ClassPathResource(url);
            InputStreamReader inputStreamReader = new InputStreamReader(classPathResource.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;

            String lineSeparator = System.getProperty("line.separator");
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(lineSeparator);
            }
        } catch (Exception e) {
            log.debug(String.format("Failed to load file from url: %s: %s", url, e.getMessage()));
            return null;
        } finally {
            if (bufferedReader != null)
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.debug(String.format("Unable to close buffered reader.. %s", e.getMessage()));
                }
        }

        return stringBuilder.toString();
    }
}
