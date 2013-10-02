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
package org.apache.james.domainlist.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.lib.AbstractDomainList;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

public class CassandraDomainList extends AbstractDomainList {

	private Session session;

	@Inject
	@VisibleForTesting CassandraDomainList(Session session) {
		this.session = session;
	}
	
    @Override
    protected List<String> getDomainListInternal() throws DomainListException {
    	ResultSet results = session.execute(select("domain").from("domains"));
    	return	FluentIterable.from(results)
    			.transform(new Function<Row, String>() {
    				@Override
    				public String apply(Row row) {
    					return row.getString("domain");
    				}
    			})
    			.toImmutableList();
    }

    @Override
    public boolean containsDomain(String domain) throws DomainListException {
        String lowerCasedDomain = domain.toLowerCase();
    	ResultSet results = session.execute(select("domain").from("domains").where(eq("domain", lowerCasedDomain)));
        return results.one() != null;
    }

    @Override
    public void addDomain(String domain) throws DomainListException {
        String lowerCasedDomain = domain.toLowerCase();
        if (containsDomain(lowerCasedDomain)) {
            throw new DomainListException(lowerCasedDomain + " already exists.");
        }
        session.execute(insertInto("domains").value("domain", lowerCasedDomain));
    }

    @Override
    public void removeDomain(String domain) throws DomainListException {
    	String lowerCasedDomain = domain.toLowerCase();
    	session.execute(delete().from("domains").where(eq("domain", lowerCasedDomain)));
    }

}
