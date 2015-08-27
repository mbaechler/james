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
package org.apache.james.modules.protocols;

import java.net.InetSocketAddress;

import javax.inject.Inject;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.container.spring.filesystem.ResourceLoaderFileSystem;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.IMAPServer;
import org.apache.james.imapserver.netty.IMAPServerMBean;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class IMAPServerModule extends AbstractModule {

    public static final int DEFAULT_IMAP_PORT = 143;
    private static final Logger LOGGER = LoggerFactory.getLogger(IMAPServerModule.class);

    @Override
    protected void configure() {
        bind(IMAPServerMBean.class).toInstance(imapServer());

        bind(ImapProcessor.class).annotatedWith(Names.named(ImapProcessor.COMPONENT_NAME)).toProvider(DefaultImapProcessorProvider.class);

        ImapDecoder imapDecoder = DefaultImapDecoderFactory.createDecoder();
        bind(ImapDecoder.class).annotatedWith(Names.named(ImapDecoder.COMPONENT_NAME)).toInstance(imapDecoder);

        DefaultImapEncoderFactory defaultImapEncoderFactory = new DefaultImapEncoderFactory();
        bind(ImapEncoder.class).annotatedWith(Names.named(ImapEncoder.COMPONENT_NAME)).toInstance(defaultImapEncoderFactory.buildImapEncoder());

        ResourceLoaderFileSystem resourceLoaderFileSystem = new ResourceLoaderFileSystem();
        bind(FileSystem.class).annotatedWith(Names.named(FileSystem.COMPONENT_NAME)).toInstance(resourceLoaderFileSystem);
    }

    protected int imapPort() {
        return DEFAULT_IMAP_PORT;
    }

    private IMAPServer imapServer() {
        try {
            IMAPServer imapServer = new IMAPServer();
            imapServer.setListenAddresses(new InetSocketAddress("0.0.0.0", imapPort()));
            imapServer.setBacklog(200);

            imapServer.doConfigure(new HierarchicalConfiguration());
            imapServer.setLog(LOGGER);
            imapServer.bind();
            return imapServer;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static class DefaultImapProcessorProvider implements Provider<ImapProcessor> {

        private final MailboxManager mailboxManager;
        private final SubscriptionManager subscriptionManager;

        @Inject
        private DefaultImapProcessorProvider(MailboxManager mailboxManager, SubscriptionManager subscriptionManager) {
            this.mailboxManager = mailboxManager;
            this.subscriptionManager = subscriptionManager;
        }

        @Override
        public ImapProcessor get() {
            return DefaultImapProcessorFactory.createXListSupportingProcessor(mailboxManager, subscriptionManager, null, 120, ImmutableSet.of("ACL", "MOVE"));
        }
        
    }
}