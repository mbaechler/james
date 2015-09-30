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

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;

import org.apache.james.jmap.AuthenticationFilter;
import org.apache.james.jmap.AuthenticationServlet;
import org.apache.james.jmap.BypassOnPostFilter;
import org.apache.james.jmap.api.AccessTokenManager;
import org.apache.james.jmap.api.ContinuationTokenManager;
import org.apache.james.jmap.crypto.AccessTokenManagerImpl;
import org.apache.james.jmap.crypto.JamesSignatureHandler;
import org.apache.james.jmap.crypto.SignatureHandler;
import org.apache.james.jmap.crypto.SignedContinuationTokenManager;
import org.apache.james.jmap.memory.access.MemoryAccessTokenRepository;
import org.apache.james.jmap.utils.DefaultZonedDateTimeProvider;
import org.apache.james.jmap.utils.ZonedDateTimeProvider;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.ConfigurationProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

public class JMAPServerModule extends AbstractModule {

    private static final long ACCESS_TOKEN_TTL_MS = TimeUnit.HOURS.toMillis(1);
    
    @Override
    protected void configure() {
        bind(SignatureHandler.class).to(JamesSignatureHandler.class);
        bind(ZonedDateTimeProvider.class).to(DefaultZonedDateTimeProvider.class);
        bind(ContinuationTokenManager.class).to(SignedContinuationTokenManager.class);
        bind(AccessTokenManager.class).toInstance(new AccessTokenManagerImpl(new MemoryAccessTokenRepository(ACCESS_TOKEN_TTL_MS)));
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(JMAPModuleConfigurationPerformer.class);
        install(new RoutesModule());
    }

    public static class RoutesModule extends ServletModule {

        @Override
        protected void configureServlets() {
            filter("/auth").through(EverythingButPostAuthenticationFilter.class);
            filter("/").through(AuthenticationFilter.class);
            serve("/auth").with(AuthenticationServlet.class);
        }
        
        @Singleton
        private static class EverythingButPostAuthenticationFilter extends BypassOnPostFilter {
            @Inject
            public EverythingButPostAuthenticationFilter(AuthenticationFilter authenticationFilter) {
                super(authenticationFilter);
            }
        }
    }
    
    @Singleton
    public static class JMAPModuleConfigurationPerformer implements ConfigurationPerformer {

        private final ConfigurationProvider configurationProvider;
        private JamesSignatureHandler signatureHandler;
        private final GuiceFilter guiceFilter;

        @Inject
        public JMAPModuleConfigurationPerformer(
                ConfigurationProvider configurationProvider,
                JamesSignatureHandler signatureHandler,
                GuiceFilter guiceFilter) {
            
            this.configurationProvider = configurationProvider;
            this.signatureHandler = signatureHandler;
            this.guiceFilter = guiceFilter;
        }

        @Override
        public void initModule() throws Exception {
            Server server = new Server(8123);
            
            ServletHandler handler = new ServletHandler();
            FilterHolder filter = new FilterHolder(guiceFilter);
            server.setHandler(handler);
            ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/");
            servletContextHandler.setContextPath("/");
            servletContextHandler.addFilter(filter, "/*", EnumSet.of(DispatcherType.REQUEST));
            signatureHandler.configure(configurationProvider.getConfiguration("tls"));
            signatureHandler.init();
            server.start();
        }
    }
    
}