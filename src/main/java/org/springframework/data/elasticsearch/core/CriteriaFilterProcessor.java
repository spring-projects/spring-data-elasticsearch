/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import org.elasticsearch.index.query.*;
import org.springframework.data.elasticsearch.core.geo.GeoLocation;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.springframework.data.elasticsearch.core.query.Criteria.OperationKey;

/**
 * CriteriaFilterProcessor
 *
 * @author Franck Marchand
 */
class CriteriaFilterProcessor {


    FilterBuilder createFilterFromCriteria(Criteria criteria) {
        List<FilterBuilder> fbList = new LinkedList<FilterBuilder>();
        FilterBuilder filter = null;

        ListIterator<Criteria> chainIterator = criteria.getCriteriaChain().listIterator();

        while (chainIterator.hasNext()) {
            FilterBuilder fb = null;
            Criteria chainedCriteria = chainIterator.next();
            if(chainedCriteria.isOr()){
                fb = orFilter(createFilterFragmentForCriteria(chainedCriteria).toArray(new FilterBuilder[]{ }));
                fbList.add(fb);
            }else if(chainedCriteria.isNegating()){
                fbList.addAll(buildNegationFilter(criteria.getField().getName(), criteria.getQueryCriteriaEntries().iterator()));
            }else{
                fbList.addAll(createFilterFragmentForCriteria(chainedCriteria));
            }
        }

        if(!fbList.isEmpty()) {
            if(fbList.size() == 1) {
                filter =fbList.get(0);
            } else {
                filter = andFilter(fbList.toArray(new FilterBuilder[]{ }));
            }
        }

        return filter;
    }


    private List<FilterBuilder> createFilterFragmentForCriteria(Criteria chainedCriteria) {
        Iterator<Criteria.CriteriaEntry> it = chainedCriteria.getFilterCriteriaEntries().iterator();
        List<FilterBuilder> filterList = new LinkedList<FilterBuilder>();

        String fieldName = chainedCriteria.getField().getName();
        Assert.notNull(fieldName,"Unknown field");
        FilterBuilder filter = null;

        while (it.hasNext()){
            Criteria.CriteriaEntry entry = it.next();
            filter = processCriteriaEntry(entry.getKey(), entry.getValue(), fieldName);
            filterList.add(filter);
        }

        return filterList;
    }


    private FilterBuilder processCriteriaEntry(OperationKey key, Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        FilterBuilder filter = null;

        switch (key){
            case WITHIN:
                filter = geoDistanceFilter(fieldName);

                Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
                Object[] valArray = (Object[]) value;
                Assert.noNullElements(valArray, "Geo distance filter takes 2 not null elements array as parameter.");
                Assert.isTrue(valArray.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
                Assert.isTrue(valArray[0] instanceof GeoLocation, "First element of a geo distance filter must be a GeoLocation");
                Assert.isTrue(valArray[1] instanceof String, "Second element of a geo distance filter must be a String");

                GeoLocation loc = (GeoLocation)valArray[0];
                String dist = (String)valArray[1];

                ((GeoDistanceFilterBuilder)filter).lat(loc.getLatitude()).lon(loc.getLongitude()).distance(dist);
                break;
            }

        return filter;
    }

    private List<FilterBuilder> buildNegationFilter(String fieldName, Iterator<Criteria.CriteriaEntry> it){
        List<FilterBuilder> notFilterList = new LinkedList<FilterBuilder>();

        while (it.hasNext()){
            Criteria.CriteriaEntry criteriaEntry = it.next();
            FilterBuilder notFilter =  notFilter(processCriteriaEntry(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName));
            notFilterList.add(notFilter);
        }

        return notFilterList;
    }
}
