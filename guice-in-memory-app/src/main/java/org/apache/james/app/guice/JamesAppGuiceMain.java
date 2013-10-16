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
package org.apache.james.app.guice;

import java.util.Calendar;
import java.util.UUID;

import javax.inject.Named;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.dnsjava.DNSJavaService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainList;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.mock.MockFileSystem;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.IMAPServerFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.cassandra.CassandraMailboxSessionMapperFactory;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.user.CassandraSubscriptionMapper;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.user.SubscriptionMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapperFactory;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Bootstraps James using a Spring container.
 */
public class JamesAppGuiceMain implements Daemon {

    private static final Logger log = LoggerFactory.getLogger("MAIN");

    public static void main(String[] args) throws Exception {

        long start = Calendar.getInstance().getTimeInMillis();

        JamesAppGuiceMain main = new JamesAppGuiceMain();
        main.init(null);

        long end = Calendar.getInstance().getTimeInMillis();

        log.info("Apache James Server is successfully started in " + (end - start) + " milliseconds.");

    }

    private static class CassandraModule extends AbstractModule {
    	@Override
    	protected void configure() {
			bind(new TypeLiteral<MailboxSessionMapperFactory<UUID>>(){/*empty*/}).to(CassandraMailboxSessionMapperFactory.class);
			bind(new TypeLiteral<ModSeqProvider<UUID>>(){/*empty*/}).to(CassandraModSeqProvider.class);
			bind(new TypeLiteral<UidProvider<UUID>>(){/*empty*/}).to(CassandraUidProvider.class);
			bind(SubscriptionMapper.class).to(CassandraSubscriptionMapper.class);
			bind(SubscriptionMapperFactory.class).to(CassandraMailboxSessionMapperFactory.class);    		
    	}
    	
		@Provides @Singleton
		public MailboxManager mailboxManager(
				MailboxSessionMapperFactory<UUID> mailboxSessionMapperFactory, Authenticator authenticator,
				MailboxPathLocker mailboxPathLocker, MailboxACLResolver aclResolver, 
				final GroupMembershipResolver groupMembershipResolver) throws MailboxException {
			
			StoreMailboxManager<UUID> storeMailboxManager = 
					new StoreMailboxManager<UUID>(mailboxSessionMapperFactory, authenticator, mailboxPathLocker, aclResolver, groupMembershipResolver);
			storeMailboxManager.init();
			return storeMailboxManager;
		}
    }

    private static class MemoryModule extends AbstractModule {
    	@Override
    	protected void configure() {
    		InMemoryMailboxSessionMapperFactory inMemoryMailboxSessionMapperFactory = new InMemoryMailboxSessionMapperFactory();
    		bind(new TypeLiteral<MailboxSessionMapperFactory<Long>>(){}).toInstance(inMemoryMailboxSessionMapperFactory);
    		bind(SubscriptionMapperFactory.class).toInstance(inMemoryMailboxSessionMapperFactory);
    	}
    	
		@Provides @Singleton
		public MailboxManager mailboxManager(
				MailboxSessionMapperFactory<Long> mailboxSessionMapperFactory, Authenticator authenticator,
				MailboxPathLocker mailboxPathLocker, MailboxACLResolver aclResolver, 
				final GroupMembershipResolver groupMembershipResolver) throws MailboxException {
			
			StoreMailboxManager<Long> storeMailboxManager = 
					new StoreMailboxManager<Long>(mailboxSessionMapperFactory, authenticator, mailboxPathLocker, aclResolver, groupMembershipResolver);
			storeMailboxManager.init();
			return storeMailboxManager;
		}
    }
    
    @Override
    public void init(DaemonContext ignored) throws Exception {
    	Injector injector = Guice.createInjector(new AbstractModule() {
			
			@Override
			protected void configure() {
				//install(new CassandraModule());
				install(new MemoryModule());
				bind(Authenticator.class).to(UserRepositoryAuthenticator.class).in(Scopes.SINGLETON);
				bind(MailboxPathLocker.class).to(JVMMailboxPathLocker.class).in(Scopes.SINGLETON);
				bind(MailboxACLResolver.class).to(UnionMailboxACLResolver.class).in(Scopes.SINGLETON);
				bind(GroupMembershipResolver.class).to(SimpleGroupMembershipResolver.class).in(Scopes.SINGLETON);
				bind(ImapDecoder.class).annotatedWith(Names.named("imapDecoder")).toInstance(DefaultImapDecoderFactory.createDecoder());
				bind(ImapEncoder.class).annotatedWith(Names.named("imapEncoder")).toInstance(new DefaultImapEncoderFactory().buildImapEncoder());
				bind(UsersRepository.class).annotatedWith(Names.named("usersrepository")).to(CassandraUsersRepository.class).in(Scopes.SINGLETON);
				bind(DomainList.class).annotatedWith(Names.named("domainlist")).to(CassandraDomainList.class).in(Scopes.SINGLETON);
				bind(FileSystem.class).annotatedWith(Names.named("filesystem")).to(MockFileSystem.class).in(Scopes.SINGLETON);
				bind(Session.class).toInstance(Cluster.builder().addContactPoint("127.0.0.1").build().connect("james2"));
				bind(DNSService.class).annotatedWith(Names.named("dnsservice")).to(DNSJavaService.class).in(Scopes.SINGLETON);
			}
			
			@Provides @Singleton @Named("imapProcessor")
			public ImapProcessor imapProcessor(MailboxManager mailboxManager, SubscriptionManager subscriptionManager) {
				return DefaultImapProcessorFactory
						.createXListSupportingProcessor(mailboxManager, subscriptionManager, null, 120, ImmutableSet.of("ACL", "MOVE"));
			}
			
			@Provides @Singleton
			public SubscriptionManager subscriptionManager(SubscriptionMapperFactory subscriptionMapperFactory) {
				return new StoreSubscriptionManager(subscriptionMapperFactory);
			}
			
		});
    	UsersRepository users = injector.getInstance(Key.get(UsersRepository.class, Names.named("usersrepository")));
    	if (!users.contains("test")) {
    		users.addUser("test", "pass");
    	}
    	IMAPServerFactory imapServerFactory = injector.getInstance(IMAPServerFactory.class);
    	imapServerFactory.setLog(log);
    	XMLConfiguration configuration = new XMLConfiguration(Resources.getResource("imapserver.xml"));
    	imapServerFactory.configure(configuration);
    	imapServerFactory.init();
    }

    @Override
    public void start() throws Exception {
    	//empty
    }

    @Override
    public void stop() throws Exception {
    	//empty
    }

    @Override
    public void destroy() {
    	//empty
    }

}
