package org.apache.james;

import org.apache.james.utils.ConfigurationProvider;
import org.apache.james.utils.FileConfigurationProvider;

import com.google.inject.AbstractModule;

public class CommonServicesModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ConfigurationProvider.class).to(FileConfigurationProvider.class);
    }

}
