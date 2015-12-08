package org.springframework.data.elasticsearch.repositories.geo;

import org.springframework.data.elasticsearch.entities.GeoEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Created by akonczak on 22/11/2015.
 */
public interface SpringDataGeoRepository extends ElasticsearchRepository<GeoEntity, String> {

}
