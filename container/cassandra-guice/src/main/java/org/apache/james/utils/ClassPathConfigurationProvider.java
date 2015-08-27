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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class ClassPathConfigurationProvider {

    private static final String CONFIGURATION_FILE_SUFFIX = ".xml";

    public HierarchicalConfiguration getConfiguration(String component) throws ConfigurationException {
        int delimiterPosition = component.indexOf(".");
        HierarchicalConfiguration config = getConfig(retrieveConfigInputStream(component, delimiterPosition));
        return selectHierarchicalConfigPart(component, delimiterPosition, config);
    }

    private InputStream retrieveConfigInputStream(String component, int delimiterPosition) throws ConfigurationException {
        String resourceName = computeResourceName(component, delimiterPosition);
        return Optional.ofNullable(ClassLoader.getSystemResourceAsStream(resourceName + CONFIGURATION_FILE_SUFFIX))
            .orElseThrow(() -> new ConfigurationException("Unable to locate configuration file " + resourceName + CONFIGURATION_FILE_SUFFIX + " for component " + component));
    }

    private XMLConfiguration getConfig(InputStream configStream) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.setAttributeSplittingDisabled(true);
        config.load(configStream);
        return config;
    }


    private HierarchicalConfiguration selectHierarchicalConfigPart(String component, int delimiterPosition, HierarchicalConfiguration config) {
        Optional<String> configPart = computeConfigPart(component, delimiterPosition);
        if (configPart.isPresent()) {
            return config.configurationAt(configPart.get());
        } else {
            return config;
        }
    }


    private Optional<String> computeConfigPart(String component, int delimiterPosition) {
        if (delimiterPosition > -1) {
            return Optional.of(component.substring(delimiterPosition + 1));
        }
        return Optional.empty();
    }

    private String computeResourceName(String component, int delimiterPosition) {
        if (delimiterPosition >= 0) {
            return component.substring(0, delimiterPosition);
        } else {
            return component;
        }
    }
}
