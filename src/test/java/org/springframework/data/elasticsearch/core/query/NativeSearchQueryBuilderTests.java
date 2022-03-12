package org.springframework.data.elasticsearch.core.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

/**
 * @author owen.qq
 */
public class NativeSearchQueryBuilderTests {

    @Test // #2105
    void shouldContainEffectiveSearchAfterValue() {
        Long lastSortValue = 1L;
        List<Object> searchAfter = Lists.newArrayList(lastSortValue);

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(searchAfter);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNotNull();
    }

    @Test // #2105
    void shouldIgnoreNullableSearchAfterValue() {
        List<Object> emptySearchValueByFirstSearch = null;
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNull();
    }

    @Test // #2105
    void shouldIgnoreEmptySearchAfterValue() {
        List<Object> emptySearchValueByFirstSearch = Lists.newArrayList();
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNull();
    }
}
