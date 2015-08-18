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
package org.apache.james;

import org.apache.james.modules.mailbox.CassandraMailboxModule;
import org.apache.james.modules.mailbox.CassandraSessionModule;
import org.apache.james.modules.mailbox.ElasticSearchMailboxModule;
import org.apache.james.modules.server.ActiveMQQueueModule;
import org.apache.james.modules.server.DNSServiceModule;
import org.apache.james.modules.server.JpaDomainListModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.server.JpaUsersRepositoryModule;

import com.google.inject.Guice;

public class CassandraJamesServer {

    private final ElasticSearchMailboxModule elasticSearchMailboxModule;

    public CassandraJamesServer(ElasticSearchMailboxModule elasticSearchMailboxModule) {
        this.elasticSearchMailboxModule = elasticSearchMailboxModule;
    }

    public CassandraJamesServer() {
        this(new ElasticSearchMailboxModule());
    }

    public void start() {
        Guice.createInjector(new CassandraMailboxModule(),
            new CassandraSessionModule(),
            elasticSearchMailboxModule,
            new JpaUsersRepositoryModule(),
            new JpaDomainListModule(),
            new DNSServiceModule(),
            new IMAPServerModule(),
            new ActiveMQQueueModule()
        );
    }

    public void stop() {
    }

}
