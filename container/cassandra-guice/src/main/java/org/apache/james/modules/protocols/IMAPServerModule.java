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

import javax.inject.Named;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.IMAPServerFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.utils.ClassPathConfigurationProvider;
import org.apache.james.utils.ConfigurationPerformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;

public class IMAPServerModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(IMAPServerModule.class);

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(IMAPModuleConfigurationPerformer.class);
    }

    @Provides
    @Singleton
    @Named(ImapProcessor.COMPONENT_NAME)
    ImapProcessor provideImapProcessor(@Named(MailboxManager.COMPONENT_NAME) MailboxManager mailboxManager, SubscriptionManager subscriptionManager) {
        return DefaultImapProcessorFactory.createXListSupportingProcessor(mailboxManager, subscriptionManager, null, 120, ImmutableSet.of("ACL", "MOVE"));
    }

    @Provides
    @Singleton
    @Named(ImapDecoder.COMPONENT_NAME)
    ImapDecoder provideImapDecoder() {
        return DefaultImapDecoderFactory.createDecoder();
    }

    @Provides
    @Singleton
    @Named(ImapEncoder.COMPONENT_NAME)
    ImapEncoder provideImapEncoder() {
        return new DefaultImapEncoderFactory().buildImapEncoder();
    }

    @Provides
    @Singleton
    IMAPServerFactory imapServerFactory(@Named(FileSystem.COMPONENT_NAME)FileSystem fileSystem,
                                        @Named(ImapDecoder.COMPONENT_NAME) ImapDecoder imapDecoder,
                                        @Named(ImapEncoder.COMPONENT_NAME) ImapEncoder imapEncoder,
                                        @Named(ImapProcessor.COMPONENT_NAME) ImapProcessor imapProcessor) {
        IMAPServerFactory imapServerFactory = new IMAPServerFactory();
        imapServerFactory.setLog(LOGGER);
        imapServerFactory.setImapDecoder(imapDecoder);
        imapServerFactory.setImapEncoder(imapEncoder);
        imapServerFactory.setImapProcessor(imapProcessor);
        imapServerFactory.setFileSystem(fileSystem);
        return imapServerFactory;
    }

    @Singleton
    public static class IMAPModuleConfigurationPerformer implements ConfigurationPerformer {

        private final ClassPathConfigurationProvider classPathConfigurationProvider;
        private final IMAPServerFactory imapServerFactory;

        @Inject
        public IMAPModuleConfigurationPerformer(ClassPathConfigurationProvider classPathConfigurationProvider, IMAPServerFactory imapServerFactory) {
            this.classPathConfigurationProvider = classPathConfigurationProvider;
            this.imapServerFactory = imapServerFactory;
        }

        @Override
        public void initModule() throws Exception {
            imapServerFactory.setLog(LOGGER);
            imapServerFactory.configure(classPathConfigurationProvider.getConfiguration("imapserver"));
            imapServerFactory.init();
        }
    }
}