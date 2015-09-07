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


package org.apache.james.modules.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.CassandraDataDataModel;
import org.apache.james.backends.cassandra.components.CassandraDataModel;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainList;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.file.XMLRecipientRewriteTable;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepository;
import org.apache.james.utils.ClassPathConfigurationProvider;
import org.apache.james.utils.ConfigurationPerformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraDataModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraDataModule.class);

    @Override
    protected void configure() {
        bind(DomainList.class).to(CassandraDomainList.class);
        bind(UsersRepository.class).to(CassandraUsersRepository.class);
        bind(RecipientRewriteTable.class).to(XMLRecipientRewriteTable.class);
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(CassandraDataConfigurationPerformer.class);
        Multibinder.newSetBinder(binder(), CassandraDataModel.class).addBinding().to(CassandraDataDataModel.class);
    }

    @Singleton
    public static class CassandraDataConfigurationPerformer implements ConfigurationPerformer {

        private final ClassPathConfigurationProvider classPathConfigurationProvider;
        private final CassandraDomainList cassandraDomainList;
        private final CassandraUsersRepository cassandraUsersRepository;

        @Inject
        public CassandraDataConfigurationPerformer(ClassPathConfigurationProvider classPathConfigurationProvider,
                                                   CassandraDomainList cassandraDomainList,
                                                   CassandraUsersRepository cassandraUsersRepository) {
            this.classPathConfigurationProvider = classPathConfigurationProvider;
            this.cassandraDomainList = cassandraDomainList;
            this.cassandraUsersRepository = cassandraUsersRepository;
        }

        public void initModule() throws ConfigurationException {
            cassandraDomainList.setLog(LOGGER);
            cassandraDomainList.configure(classPathConfigurationProvider.getConfiguration("domainlist"));
            cassandraUsersRepository.setLog(LOGGER);
            cassandraUsersRepository.configure(classPathConfigurationProvider.getConfiguration("usersrepository"));
        }
    }

}
