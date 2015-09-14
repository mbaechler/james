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

package org.apache.james.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Singleton;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.elasticsearch.common.base.Throwables;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

@Singleton
public class FileConfigurationProvider implements ConfigurationProvider {

    private static final String CONFIGURATION_FILE_SUFFIX = ".xml";
    
    private final Path configuration;

    @Inject
    public FileConfigurationProvider() throws MissingArgumentException {
        String configurationDirectory = Optional
                .ofNullable(System.getProperty("configuration.directory"))
                .orElseThrow(() -> new MissingArgumentException("Server needs a configuration.directory env entry"));
        configuration = new File(configurationDirectory).toPath();
    }
    
    @Override
    public HierarchicalConfiguration getConfiguration(String component) throws ConfigurationException {
        List<String> configPathParts = Arrays.asList(component.split("\\."));
        HierarchicalConfiguration config = getConfig(retrieveConfigInputStream(configPathParts.get(0)));
        return selectHierarchicalConfigPart(config, configPathParts.subList(1, configPathParts.size()));
    }

    private InputStream retrieveConfigInputStream(String configurationFileWithoutExtension) throws ConfigurationException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(configurationFileWithoutExtension), "The configuration file name should not be empty or null");
        return Optional.ofNullable(configuration.resolve(configurationFileWithoutExtension + CONFIGURATION_FILE_SUFFIX).toFile())
                .map(x -> {
                    try {
                        return new FileInputStream(x);
                    } catch (FileNotFoundException e) {
                        throw Throwables.propagate(e);
                    }
                })
            .orElseThrow(() -> new ConfigurationException("Unable to locate configuration file " + configurationFileWithoutExtension + CONFIGURATION_FILE_SUFFIX));
    }

    private XMLConfiguration getConfig(InputStream configStream) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.setAttributeSplittingDisabled(true);
        config.load(configStream);
        return config;
    }

    private HierarchicalConfiguration selectHierarchicalConfigPart(HierarchicalConfiguration config, List<String> configsPathParts) {
        HierarchicalConfiguration currentConfig = config;
        for (String nextPathPart : configsPathParts) {
            currentConfig = currentConfig.configurationAt(nextPathPart);
        }
        return currentConfig;
    }

}
