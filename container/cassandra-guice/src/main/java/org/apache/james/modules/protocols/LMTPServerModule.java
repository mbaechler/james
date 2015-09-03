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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.lmtpserver.netty.LMTPServerFactory;
import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader;
import org.apache.james.utils.ClassPathConfigurationProvider;
import org.apache.james.utils.ConfigurationPerformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

public class LMTPServerModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LMTPServerModule.class);

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(LMTPModuleConfigurationPerformer.class);
    }

    @Provides
    @Singleton
    private LMTPServerFactory lmtpServerFactory(@Named(ProtocolHandlerLoader.COMPONENT_NAME) ProtocolHandlerLoader loader,
                                                @Named(FileSystem.COMPONENT_NAME)FileSystem fileSystem) {
        LMTPServerFactory lmtpServerFactory = new LMTPServerFactory();
        lmtpServerFactory.setLog(LOGGER);
        lmtpServerFactory.setFileSystem(fileSystem);
        lmtpServerFactory.setProtocolHandlerLoader(loader);
        return lmtpServerFactory;
    }

    @Singleton
    public static class LMTPModuleConfigurationPerformer implements ConfigurationPerformer {

        private final ClassPathConfigurationProvider classPathConfigurationProvider;
        private final LMTPServerFactory lmtpServerFactory;

        @Inject
        public LMTPModuleConfigurationPerformer(ClassPathConfigurationProvider classPathConfigurationProvider, LMTPServerFactory lmtpServerFactory) {
            this.classPathConfigurationProvider = classPathConfigurationProvider;
            this.lmtpServerFactory = lmtpServerFactory;
        }

        @Override
        public void initModule() throws Exception {
            lmtpServerFactory.setLog(LOGGER);
            lmtpServerFactory.configure(classPathConfigurationProvider.getConfiguration("lmtpserver"));
            lmtpServerFactory.init();
        }
    }

}
