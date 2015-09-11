package org.apache.james.utils;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;

public interface ConfigurationProvider {

    HierarchicalConfiguration getConfiguration(String component)
            throws ConfigurationException;

}