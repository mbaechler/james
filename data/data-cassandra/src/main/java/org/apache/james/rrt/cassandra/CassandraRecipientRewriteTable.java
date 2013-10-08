/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.rrt.cassandra;


import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

public class CassandraRecipientRewriteTable implements RecipientRewriteTable {
	
	enum Type {
		REGEX_TYPE,
		ERROR_TYPE, 
		ADDRESS_TYPE, 
		ALIASDOMAIN_TYPE;
	
		private static Map<String, Type> typeIndex;
        
        static {
        	typeIndex = Maps.uniqueIndex(Arrays.asList(values()), new Function<Type, String>() {
        		@Override
        		public String apply(Type input) {
        			return input.type();
        		}
			});
        }

		
		private Type() {
		}

		public String type() {
			return name();
		}
		
		public static Type fromType(String type) {
			return typeIndex.get(type);
		}
	}
	
	private static final String ENTRY = "entry";
	private static final String TYPE = "type";
	private static final String DOMAIN = "domain";
	private static final String USER = "user";
	private static final String RRT = "rrt";
	private Session session;

	public CassandraRecipientRewriteTable(Session session) {
		this.session = session;
	}

	@Override
	public void addAddressMapping(String user, String domain, String address) throws RecipientRewriteTableException {
		addMapping(user, domain, address, Type.ADDRESS_TYPE);
	}
	
	@Override
	public void addErrorMapping(String user, String domain, String error) throws RecipientRewriteTableException {
		addMapping(user, domain, error, Type.ERROR_TYPE);
	}

	@Override
	public void addRegexMapping(String user, String domain, String regex) throws RecipientRewriteTableException {
		try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new RecipientRewriteTableException("Invalid regex: " + regex, e);
        }
		addMapping(user, domain, regex, Type.REGEX_TYPE);
	}
	
	private void addMapping(String user, String domain, String mapping, Type type) {
		session.execute(insertInto(RRT).value(USER, user).value(DOMAIN, domain).value(ENTRY, mapping).value(TYPE, type.type()));
	}

	@Override
	public void addAliasDomainMapping(String aliasDomain, String realDomain) throws RecipientRewriteTableException {
		session.execute(insertInto(RRT).value(USER, WILDCARD).value(DOMAIN, aliasDomain).value(ENTRY, realDomain).value(TYPE, Type.ALIASDOMAIN_TYPE.type()));
	}
	
	@Override
	public void addMapping(String user, String domain, String mapping) throws RecipientRewriteTableException {
        String map = mapping.toLowerCase();

        if (map.startsWith(RecipientRewriteTable.ERROR_PREFIX)) {
            addErrorMapping(user, domain, map.substring(RecipientRewriteTable.ERROR_PREFIX.length()));
        } else if (map.startsWith(RecipientRewriteTable.REGEX_PREFIX)) {
            addRegexMapping(user, domain, map.substring(RecipientRewriteTable.REGEX_PREFIX.length()));
        } else if (map.startsWith(RecipientRewriteTable.ALIASDOMAIN_PREFIX)) {
            if (user != null) {
                throw new RecipientRewriteTableException("User must be null for aliasDomain mappings");
            }
            addAliasDomainMapping(domain, map.substring(RecipientRewriteTable.ALIASDOMAIN_PREFIX.length()));
        } else {
            addAddressMapping(user, domain, map);
        }
	}
	
	@Override
	public Map<String, Collection<String>> getAllMappings()
			throws RecipientRewriteTableException {
		ResultSet results = session.execute(select(USER, DOMAIN, ENTRY, TYPE).from(RRT));
		if (results.isExhausted()) {
			return null;
		}
		return Multimaps.transformValues(
				Multimaps.index(results, new Function<Row, String>() {
					@Override
					public String apply(Row input) {
						return input.getString(USER) + "@" + input.getString(DOMAIN);
					}
				}), new Function<Row, String>() {
					@Override
					public String apply(Row input) {
						return input.getString(ENTRY);
					}
				}).asMap();
	}

	@Override
	public Collection<String> getMappings(String user, String domain) throws ErrorMappingException, RecipientRewriteTableException {
		List<String> directMapping = getUserMapping(user, domain);
		if (!directMapping.isEmpty()) {
			return directMapping;
		}
		List<String> mappings = ImmutableList.copyOf(
				Iterables.concat(directMapping, getAliasDomainMapping(user, domain), getUserMapping(WILDCARD, domain)));
		if (!mappings.isEmpty()) {
			return mappings;
		}
		return null;
	}

	@Override
	public Collection<String> getUserDomainMappings(String user, String domain) throws RecipientRewriteTableException {
		return null;
	}

	private List<String> getUserMapping(String user, String domain) throws ErrorMappingException {
		ResultSet results = session.execute(
				select(ENTRY, TYPE).from(RRT)
					.where(eq(USER, user))
					.and(eq(DOMAIN, domain)));
		Iterable<Row> filteredResultSet = filterTypes(results, Type.ADDRESS_TYPE, Type.ERROR_TYPE, Type.REGEX_TYPE);
		List<String> mappings = Lists.newArrayList();
		for (Row row: filteredResultSet) {
			checkErrorMapping(row);
			mappings.add(row.getString(ENTRY));
		}
		return mappings;
	}


	private List<String> getAliasDomainMapping(String user, String domain) throws ErrorMappingException {
		ResultSet results = session.execute(
				select(ENTRY, TYPE).from(RRT)
					.where(eq(USER, WILDCARD))
					.and(eq(DOMAIN, domain)));
		Iterable<Row> filteredResultSet = filterTypes(results, Type.ALIASDOMAIN_TYPE, Type.ERROR_TYPE);
		List<String> mappings = Lists.newArrayList();
		for (Row row: filteredResultSet) {
			checkErrorMapping(row);
			mappings.add(user + "@" + row.getString(ENTRY));
		}
		return mappings;
	}
	
	private void checkErrorMapping(Row row) throws ErrorMappingException {
		if (Type.fromType(row.getString(TYPE)).equals(Type.ERROR_TYPE)) {
		    throw new ErrorMappingException(row.getString(ENTRY));
		}
	}

	private FluentIterable<Row> filterTypes(ResultSet results, final Type... types) {
		return FluentIterable.from(results).filter(new Predicate<Row>() {
			@Override
			public boolean apply(Row input) {
				String actualType = input.getString(TYPE);
				for (Type t: types) {
					if (actualType.equals(t.type())) {
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	public void removeAddressMapping(String user, String domain, String address) throws RecipientRewriteTableException {
		removeMapping(user, domain, address, Type.ADDRESS_TYPE);
	}

	@Override
	public void removeErrorMapping(String user, String domain, String error) throws RecipientRewriteTableException {
		removeMapping(user, domain, error, Type.ERROR_TYPE);
	}

	@Override
	public void removeRegexMapping(String user, String domain, String regex) throws RecipientRewriteTableException {
		removeMapping(user, domain, regex, Type.REGEX_TYPE);
	}

	private void removeMapping(String user, String domain, String entry, Type type) {
		session.execute(delete().from(RRT).where(eq(USER, user)).and(eq(DOMAIN, domain)).and(eq(ENTRY, entry)).and(eq(TYPE, type)));
	}

	@Override
	public void removeAliasDomainMapping(String aliasDomain, String realDomain) throws RecipientRewriteTableException {
		removeMapping(WILDCARD, aliasDomain, realDomain, Type.ALIASDOMAIN_TYPE);
	}
	
	@Override
	public void removeMapping(String user, String domain, String mapping) throws RecipientRewriteTableException {
		if (mapping.startsWith(RecipientRewriteTable.ERROR_PREFIX)) {
            removeErrorMapping(user, domain, mapping.substring(RecipientRewriteTable.ERROR_PREFIX.length()));
        } else if (mapping.startsWith(RecipientRewriteTable.REGEX_PREFIX)) {
        	removeRegexMapping(user, domain, mapping.substring(RecipientRewriteTable.REGEX_PREFIX.length()));
        } else if (mapping.startsWith(RecipientRewriteTable.ALIASDOMAIN_PREFIX)) {
            if (user != null) {
                throw new RecipientRewriteTableException("User must be null for aliasDomain mappings");
            }
            removeAliasDomainMapping(domain, mapping.substring(RecipientRewriteTable.ALIASDOMAIN_PREFIX.length()));
        } else {
        	removeAddressMapping(user, domain, mapping);
        }
	}

	@Override
	public void setRecursiveMapping(boolean enable) {
		throw new UnsupportedOperationException();
	}
	
}
