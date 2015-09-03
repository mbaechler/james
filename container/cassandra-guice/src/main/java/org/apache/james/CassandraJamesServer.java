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

import com.google.inject.Injector;
import org.apache.james.modules.mailbox.CassandraMailboxModule;
import org.apache.james.modules.mailbox.CassandraSessionModule;
import org.apache.james.modules.mailbox.ElasticSearchMailboxModule;
import org.apache.james.modules.protocols.LMTPServerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.POP3ServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.server.ActiveMQQueueModule;
import org.apache.james.modules.server.CamelMailetContainerModule;
import org.apache.james.modules.server.CassandraDataModule;
import org.apache.james.modules.server.DNSServiceModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.modules.server.MailStoreRepositoryModule;
import org.apache.james.modules.server.SieveModule;

import com.google.inject.Guice;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.onami.lifecycle.jsr250.PreDestroyModule;

public class CassandraJamesServer {

    private final ElasticSearchMailboxModule elasticSearchMailboxModule;
    private final PreDestroyModule preDestroyModule;

    public CassandraJamesServer(ElasticSearchMailboxModule elasticSearchMailboxModule) {
        this.elasticSearchMailboxModule = elasticSearchMailboxModule;
        this.preDestroyModule = new PreDestroyModule();
    }

    public CassandraJamesServer() {
        this(new ElasticSearchMailboxModule());
    }

    public void start() throws Exception {
        Injector injector = Guice.createInjector(new CassandraMailboxModule(),
            new CassandraSessionModule(),
            elasticSearchMailboxModule,
            new CassandraDataModule(),
            new DNSServiceModule(),
            new IMAPServerModule(),
            new ProtocolHandlerModule(),
            new MailStoreRepositoryModule(),
            new POP3ServerModule(),
            new SMTPServerModule(),
            new LMTPServerModule(),
            new ActiveMQQueueModule(),
            new SieveModule(),
            new CamelMailetContainerModule(),
            preDestroyModule,
            new JMXServerModule()
        );
        injector.getInstance(ConfigurationsPerformer.class).initModules();
    }

    public void stop() {
        preDestroyModule.getStager().stage();
    }

}
