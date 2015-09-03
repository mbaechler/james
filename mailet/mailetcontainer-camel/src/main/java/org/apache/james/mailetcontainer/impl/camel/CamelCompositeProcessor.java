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

package org.apache.james.mailetcontainer.impl.camel;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.james.mailetcontainer.api.MatcherLoader;
import org.apache.james.mailetcontainer.lib.AbstractStateCompositeProcessor;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetContext;
import org.apache.mailet.Matcher;

/**
 * Build up the Camel Routes by parsing the mailetcontainer.xml configuration
 * file.
 * 
 * It also offer the {@link AbstractStateCompositeProcessor} implementation
 * which allow to inject {@link Mail} into the routes.
 */
public class CamelCompositeProcessor extends AbstractStateCompositeProcessor implements CamelContextAware {

    private CamelContext camelContext;
    private MailetContext mailetContext;
    private MatcherLoader matcherLoader;
    private MailetLoader mailetLoader;

    @Inject
    public void setMatcherLoader(@Named(MatcherLoader.COMPONENT_NAME) MatcherLoader matcherLoader) {
        this.matcherLoader = matcherLoader;
    }

    @Inject
    public void setMailetLoader(@Named(MailetLoader.COMPONENT_NAME) MailetLoader mailetLoader) {
        this.mailetLoader = mailetLoader;
    }

    @Inject
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();

        // Make sure the camel context get started
        // See https://issues.apache.org/jira/browse/JAMES-1069
        if (getCamelContext().getStatus().isStopped()) {
            getCamelContext().start();
        }

    }

    /**
     * @see org.apache.camel.CamelContextAware#getCamelContext()
     */
    public CamelContext getCamelContext() {
        return camelContext;
    }

    /**
     * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
     */
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    /**
     * @see org.apache.james.mailetcontainer.lib.AbstractStateCompositeProcessor
     * #createMailProcessor(java.lang.String, org.apache.commons.configuration.HierarchicalConfiguration)
     */
    protected MailProcessor createMailProcessor(String name, HierarchicalConfiguration config) throws Exception {
        CamelMailetProcessor processor = new CamelMailetProcessor();
        try {
            processor.setLog(logger);
            processor.setCamelContext(camelContext);
            processor.setMailetContext(mailetContext);
            processor.setMailetLoader(mailetLoader);
            processor.setMatcherLoader(matcherLoader);
            processor.configure(config);
            processor.init();
            return processor;
        } catch (Exception e) {
            processor.destroy();

            throw e;
        }
    }

}
