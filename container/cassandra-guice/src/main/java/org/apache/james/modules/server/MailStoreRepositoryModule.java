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
import com.google.inject.name.Names;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.file.FileMailRepository;
import org.apache.james.utils.ClassPathConfigurationProvider;
import org.apache.james.utils.ConfigurationPerformer;
import org.apache.james.utils.JavaMailRepositoryStore;
import org.apache.james.utils.MailRepositoryProvider;

import javax.inject.Named;

public class MailStoreRepositoryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MailRepositoryStore.class).annotatedWith(Names.named(MailRepositoryStore.COMPONENT_NAME)).to(JavaMailRepositoryStore.class);
        Multibinder<MailRepositoryProvider> multibinder = Multibinder.newSetBinder(binder(), MailRepositoryProvider.class);
        multibinder.addBinding().to(FileMailRepositoryProvider.class);
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(MailRepositoryStoreModuleConfigurationPerformer.class);
    }

    public static class FileMailRepositoryProvider implements MailRepositoryProvider {

        private final FileSystem fileSystem;

        @Inject
        public FileMailRepositoryProvider(@Named(FileSystem.COMPONENT_NAME)FileSystem fileSystem) {
            this.fileSystem = fileSystem;
        }

        @Override
        public String canonicalName() {
            return FileMailRepository.class.getCanonicalName();
        }

        @Override
        public MailRepository get() {
            FileMailRepository fileMailRepository = new FileMailRepository();
            fileMailRepository.setFileSystem(fileSystem);
            return fileMailRepository;
        }
    }

    @Singleton
    public static class MailRepositoryStoreModuleConfigurationPerformer implements ConfigurationPerformer {

        private final ClassPathConfigurationProvider classPathConfigurationProvider;
        private final JavaMailRepositoryStore javaMailRepositoryStore;

        @Inject
        public MailRepositoryStoreModuleConfigurationPerformer(ClassPathConfigurationProvider classPathConfigurationProvider,
                                                               JavaMailRepositoryStore javaMailRepositoryStore) {
            this.classPathConfigurationProvider = classPathConfigurationProvider;
            this.javaMailRepositoryStore = javaMailRepositoryStore;
        }

        @Override
        public void initModule() throws Exception {
            javaMailRepositoryStore.configure(classPathConfigurationProvider.getConfiguration("mailrepositorystore"));
            javaMailRepositoryStore.init();
        }
    }

}
