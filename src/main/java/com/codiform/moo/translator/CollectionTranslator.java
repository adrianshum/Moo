package com.codiform.moo.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.codiform.moo.TranslationException;
import com.codiform.moo.annotation.TranslateCollection;
import com.codiform.moo.configuration.Configuration;
import com.codiform.moo.source.TranslationSource;

/**
 * A translator for handling issues specific to collections. In particular, this
 * handles defensive copying of collections that don't need to be transformed,
 * but also deals with making a copy of a collection that matches the original
 * collection type (set, list, etc.) but where each item in the collection has
 * been translated.
 * 
 * <p>
 * This approach has limitations; it can't change the type of the collection,
 * and it can't currently handle sorted sets with translation (where does the
 * comparator for the translated objects come from?), maps, or allow you to
 * select the implementation class for the collection (LinkedList vs.
 * ArrayList).
 * </p>
 */
public class CollectionTranslator {

	private Configuration configuration;

	/**
	 * Create a collection translator with a known configuration.
	 * 
	 * @param configuration
	 */
	public CollectionTranslator(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Translate a collection. Make a defensive copy if need be. If the element
	 * is annotated with the TranslateCollection element, then create an
	 * entirely new collection and translate each element of the contents of the
	 * source collection.
	 * 
	 * @param value
	 *            the source collection
	 * @param annotation
	 *            the {@link TranslateCollection} annotation, if present
	 * @param cache
	 *            the translation cache of previously-translated elements
	 * @return the translated collection
	 */
	public Object translate(Object value, TranslateCollection annotation,
			TranslationSource cache) {
		if( value instanceof SortedSet<?> ) {
			return copySortedSet( (SortedSet<?>) value, annotation, cache );
		} else if( value instanceof Set<?> ) {
			return copySet( (Set<?>) value, annotation, cache );
		} else if( value instanceof SortedMap ) {
			return copySortedMap( (SortedMap<?,?>) value, annotation );
		} else if( value instanceof Map ) {
			return copyMap( (Map<?,?>) value, annotation );
		} else if( value instanceof List ) {
			return (List<?>) copyList( (List<?>) value, annotation, cache );
		} else {
			return copyCollection( (Collection<?>) value, annotation, cache );
		}
	}

	private Collection<?> copyCollection(Collection<?> value,
			TranslateCollection annotation, TranslationSource translationSource) {
		if( annotation == null ) {
			return configuration.isPerformingDefensiveCopies() ? new ArrayList<Object>(
					value ) : value;
		} else {
			return translationSource.getEachTranslation( value, annotation
					.value() );
		}
	}

	private List<?> copyList(List<?> value, TranslateCollection annotation,
			TranslationSource translationSource) {
		if( annotation == null ) {
			return configuration.isPerformingDefensiveCopies() ? new ArrayList<Object>(
					value ) : value;
		} else {
			return translationSource.getEachTranslation( value, annotation
					.value() );
		}
	}

	private Map<?,?> copyMap(Map<?,?> value, TranslateCollection annotation) {
		if( annotation == null ) {
			return configuration.isPerformingDefensiveCopies() ? new HashMap<Object,Object>(
					value ) : value;
		} else {
			throw new TranslationException(
					"Support for translated maps not yet built." );
		}
	}

	private SortedMap<?,?> copySortedMap(SortedMap<?,?> value,
			TranslateCollection annotation) {
		if( annotation == null ) {
			if( configuration.isPerformingDefensiveCopies() ) {
				return defensivelyCopySortedMap( value );
			} else {
				return value;
			}
		} else {
			throw new TranslationException(
					"Support for translated sorted maps not yet built" );
		}
	}

	private <A, B> SortedMap<A, B> defensivelyCopySortedMap(
			SortedMap<A, B> value) {
		SortedMap<A, B> map = new TreeMap<A, B>( value.comparator() );
		map.putAll( value );
		return map;
	}

	private Set<?> copySet(Set<?> value, TranslateCollection annotation,
			TranslationSource translationSource) {
		if( annotation == null ) {
			return configuration.isPerformingDefensiveCopies() ? new HashSet<Object>(
					value )
					: value;
		} else {
			return translationSource.getEachTranslation( value, annotation
					.value() );
		}
	}

	private SortedSet<?> copySortedSet(SortedSet<?> original,
			TranslateCollection annotation, TranslationSource translationSource) {
		if( annotation == null ) {
			if( configuration.isPerformingDefensiveCopies() ) {
				return defensivelyCopySortedSet( original );
			} else {
				return original;
			}
		} else if( original.comparator() == null ) {
			Class<?> annotationValue = annotation.value();
			if( Comparable.class.isAssignableFrom( annotationValue ) ) {
				return copyAndTranslateSortedSet( original, translationSource,
						annotationValue );
			} else {
				throw new TranslationException(
						"Naturally sorted set cannot be translated into another naturally-sorted set if the destination type is not comparable: "
								+ annotationValue );
			}
		} else {
			throw new TranslationException(
					"Support for translated sorted sets with comparators not yet built" );
		}
	}

	private SortedSet<?> copyAndTranslateSortedSet(SortedSet<?> original,
			TranslationSource translationSource, Class<?> annotationValue) {
		Set<?> translated = translationSource.getEachTranslation(
				original,
				annotationValue );
		SortedSet<?> result = new TreeSet<Object>( translated );
		return result;
	}

	private <Z> SortedSet<Z> defensivelyCopySortedSet(SortedSet<Z> value) {
		SortedSet<Z> result = new TreeSet<Z>( value.comparator() );
		result.addAll( value );
		return result;
	}

}