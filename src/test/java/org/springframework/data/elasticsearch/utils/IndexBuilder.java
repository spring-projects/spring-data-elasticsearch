package org.springframework.data.elasticsearch.utils;

import java.lang.reflect.Field;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

/**
 * Created by akonczak on 02/12/2015.
 *
 * @author Peter-Josef Meisch
 */
public class IndexBuilder {

	public static IndexQuery buildIndex(Object object) {
		for (Field f : object.getClass().getDeclaredFields()) {

			if (AnnotationUtils.findAnnotation(f, Id.class) != null) {
				try {
					f.setAccessible(true);
					IndexQuery indexQuery = new IndexQuery();
					indexQuery.setId((String) f.get(object));
					indexQuery.setObject(object);
					return indexQuery;
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		throw new RuntimeException("Missing @Id field");
	}
}
