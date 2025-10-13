/*
 * Copyright 2021-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class DocumentAdaptersBenchmark {

	private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();

    private List<Hit<EntityAsMap>> mockHits;

    @BeforeEach
    public void setup() {
        mockHits = generateMockHits(100);
    }

    public Object fromLegacy() {
        return mockHits.stream()
                .map(hit -> DocumentAdapters.fromLegacy(hit, jsonpMapper))
                .collect(Collectors.toList());
    }

    public Object from() {
        return mockHits.stream()
                .map(hit -> DocumentAdapters.from(hit, jsonpMapper))
                .collect(Collectors.toList());
    }

    @Test
    public void runner() {
        benchmark("fromLegacy", this::fromLegacy);
        benchmark("from", this::from);
    }

    private void benchmark(String name, Supplier<Object> func) {
        int warmups = 5;
        int runs = 20;

        // Warm-up
        for (int i = 0; i < warmups; i++) {
            func.get();
        }

        long total = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            func.get();
            long end = System.nanoTime();
            total += (end - start);
        }

        double avgMs = total / (runs * 1_000_000.0);
        System.out.printf("%s average time: %.6f ms%n", name, avgMs);
    }

    public static List<Hit<EntityAsMap>> generateMockHits(int count) {
        List<Hit<EntityAsMap>> results = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            EntityAsMap source = new EntityAsMap();
            source.put("field1", 10000 + i);
            source.put("field2", "value2_" + UUID.randomUUID().toString().substring(0, 8));
            source.put("field3", UUID.randomUUID().toString() + ".jpeg");
            source.put("field4", "value4_" + random.nextInt(1000));
            source.put("field5", "value5_" + random.nextInt(10));

            List<Map<String, Object>> subCategories = new ArrayList<>();
            for (int j = 0; j < 50; j++) {
                Map<String, Object> subCat = new LinkedHashMap<>();
                subCat.put("key", "sub_category_" + j);
                subCat.put("value", random.nextInt(5));
                subCategories.add(subCat);
            }

            EntityAsMap innerSource = new EntityAsMap();
            innerSource.put("field1", "inner_source_" + i);
            innerSource.put("field2", random.nextInt(5));
            innerSource.put("field3", subCategories);

            NestedIdentity nested = new NestedIdentity.Builder().field("nested_field").offset(random.nextInt(10)).build();
            Hit<JsonData> innerHit = new Hit.Builder<JsonData>()
                    .index("index")
                    .id(String.valueOf(10000 + i))
                    .nested(nested)
                    .score(random.nextDouble())
                    .source(JsonData.of(innerSource))
                    .build();

            List<Hit<JsonData>> innerHits = List.of(innerHit);

            TotalHits total = new TotalHits.Builder()
                    .value(1)
                    .relation(TotalHitsRelation.Eq)
                    .build();

            HitsMetadata<JsonData> hits = new HitsMetadata.Builder<JsonData>()
                    .total(total)
                    .maxScore(random.nextDouble())
                    .hits(innerHits)
                    .build();

            InnerHitsResult innerHitsResult = new InnerHitsResult.Builder()
                    .hits(hits)
                    .build();

            Hit<EntityAsMap> hit = new Hit.Builder<EntityAsMap>()
                    .index("index")
                    .id(String.valueOf(10000 + i))
                    .score(random.nextDouble())
                    .source(source)
                    .innerHits(Map.of("inner_hit", innerHitsResult))
                    .build();
            results.add(hit);
        }
        return results;
    }
}
