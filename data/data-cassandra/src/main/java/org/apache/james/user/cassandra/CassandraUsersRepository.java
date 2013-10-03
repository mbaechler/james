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

package org.apache.james.user.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.Iterator;

import javax.inject.Inject;

import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.apache.james.user.lib.AbstractUsersRepository;
import org.apache.james.user.lib.model.DefaultUser;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;

public class CassandraUsersRepository extends AbstractUsersRepository {

	private static final String USERS = "users";
	private static final String NAME = "name";
	private static final String REALNAME = "realname";
	private static final String PASSWORD = "password";
	private static final String ALGO = "algorithm";
	private static final String DEFAULT_ALGO_VALUE = "SHA1";
	
	private Session session;

	@Inject
	@VisibleForTesting CassandraUsersRepository(Session session) {
		this.session = session;
	}
	
	@Override
	public User getUserByName(String name) throws UsersRepositoryException {
		ResultSet result = session.execute(select(REALNAME, PASSWORD, ALGO).from(USERS).where(eq(REALNAME, name)));
		Row row = result.one();
		if (row != null) {
			return new DefaultUser(row.getString(REALNAME), row.getString(PASSWORD), row.getString(ALGO));
		}
		return null;
	}

	@Override
	public void updateUser(User user) throws UsersRepositoryException {
		Preconditions.checkArgument(user instanceof DefaultUser);
		DefaultUser defaultUser = (DefaultUser) user;
		if (contains(user.getUserName())) {
			session.execute(update(USERS)
					.where(eq(NAME, defaultUser.getUserName().toLowerCase()))
					.with(set(REALNAME, defaultUser.getUserName()))
					.and(set(PASSWORD, defaultUser.getHashedPassword()))
					.and(set(ALGO, defaultUser.getHashAlgorithm())));
		}
		if (!contains(user.getUserName())) {
			throw new UsersRepositoryException("Unable to update user");
		}
	}

	@Override
	public void removeUser(String name) throws UsersRepositoryException {
		session.execute(delete().from(USERS).where(eq(NAME, name.toLowerCase())));
	}

	@Override
	public boolean contains(String name) throws UsersRepositoryException {
		ResultSet result = session.execute(select().countAll().from(USERS).where(eq(NAME, name.toLowerCase())));
		return result.one().getLong(0) != 0;
	}

	@Override
	public boolean test(String name, String password)	throws UsersRepositoryException {
		User user = getUserByName(name);
		return user != null && user.verifyPassword(password);
	}

	@Override
	public int countUsers() throws UsersRepositoryException {
		ResultSet result = session.execute(select().countAll().from(USERS));
		return Long.valueOf(result.one().getLong(0)).intValue();
	}

	@Override
	public Iterator<String> list() throws UsersRepositoryException {
		ResultSet result = session.execute(select(REALNAME).from(USERS));
		return FluentIterable.from(result)
			.transform(new Function<Row, String>() {
				@Override
				public String apply(Row row) {
					return row.getString(REALNAME);
				}
			})
			.iterator();
	}

	@Override
	protected void doAddUser(String username, String password) throws UsersRepositoryException {
		DefaultUser user = new DefaultUser(username, DEFAULT_ALGO_VALUE);
		user.setPassword(password);
		session.execute(insertInto(USERS)
				.value(NAME, user.getUserName().toLowerCase())
				.value(REALNAME, user.getUserName())
				.value(PASSWORD, user.getHashedPassword())
				.value(ALGO, user.getHashAlgorithm()));
	}


}
