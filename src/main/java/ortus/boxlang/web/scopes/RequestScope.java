/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
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
package ortus.boxlang.web.scopes;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.web.exchange.IBoxHTTPExchange;

/**
 * Web request scope implementation in BoxLang. I look like the request scope from the BL core,
 * but I also make the request attributes avaialble as if they were in the scope.
 */
public class RequestScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key		name	= Key.of( "request" );

	/**
	 * The Linked Exchange
	 */
	protected IBoxHTTPExchange	exchange;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public RequestScope( IBoxHTTPExchange exchange ) {
		super( RequestScope.name );
		this.exchange = exchange;
	}

	/**
	 * Returns the number of key-value mappings in this map. If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size() {
		return wrapped.size() + exchange.getRequestAttributeMap().size();
	}

	/**
	 * Returns {@code true} if this map contains no key-value mappings.
	 */
	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty() && exchange.getRequestAttributeMap().isEmpty();
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified {@code Key}
	 *
	 * @param key key whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( Key key ) {
		return wrapped.containsKey( key ) || exchange.getRequestAttributeMap().containsKey( key.getName() );
	}

	/**
	 * Returns {@code true} if this map maps has the specified value
	 *
	 * @param value value whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified value
	 */
	@Override
	public boolean containsValue( Object value ) {
		return wrapped.containsValue( value ) || exchange.getRequestAttributeMap().containsValue( value );
	}

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a NullValue still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a NullValue object, null means the key didn't exist *
	 */
	public Object getRaw( Key key ) {
		// Look in wrapped map first
		Object value = wrapped.get( key );
		if ( value != null ) {
			return value;
		}

		// Then look in the request attributes
		var requestAttributes = exchange.getRequestAttributeMap();
		if ( requestAttributes.containsKey( key.getName() ) ) {
			value = requestAttributes.get( key.getName() );
			if ( value != null ) {
				return value;
			} else {
				return new NullValue();
			}
		}

		// Not found
		return null;

	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 */
	@Override
	public Set<Key> keySet() {
		var keys = wrapped.keySet();
		keys.addAll( exchange.getRequestAttributeMap().keySet().stream().map( Key::of ).collect( Collectors.toSet() ) );
		return keys;
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 */
	@Override
	public Collection<Object> values() {
		var vals = wrapped.values().stream()
		    .map( entry -> unWrapNull( entry ) )
		    .collect( Collectors.toList() );
		vals.addAll( exchange.getRequestAttributeMap().values() );
		return vals;
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 */
	@Override
	public Set<Entry<Key, Object>> entrySet() {
		Set<Entry<Key, Object>> entries = wrapped.entrySet().stream()
		    .map( entry -> new SimpleEntry<>( entry.getKey(), unWrapNull( entry.getValue() ) ) )
		    .collect( Collectors.toCollection( LinkedHashSet::new ) );

		exchange.getRequestAttributeMap().entrySet().stream()
		    .map( entry -> new SimpleEntry<>( Key.of( entry.getKey() ), entry.getValue() ) )
		    .forEach( entries::add );

		return entries;
	}

}
