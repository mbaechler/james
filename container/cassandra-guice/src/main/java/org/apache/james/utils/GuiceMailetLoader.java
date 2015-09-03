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

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;

import javax.mail.MessagingException;

public class GuiceMailetLoader implements MailetLoader {

    private static final String STANDARD_PACKAGE = "org.apache.james.transport.mailets.";

    private final Injector injector;

    @Inject
    public GuiceMailetLoader(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Mailet getMailet(MailetConfig config) throws MessagingException {
        String mailetName = constructFullName(config.getMailetName());
        final Mailet result;
        try {
            Class classs = ClassLoader.getSystemClassLoader().loadClass(mailetName);
            result = injector.<Mailet>getInstance(classs);
        } catch (ClassNotFoundException e) {
            throw new MessagingException("Can not find mailet " + mailetName, e);
        }
        result.init(config);
        return result;
    }

    private String constructFullName(String name) {
        if (name.indexOf(".") < 1) {
            return STANDARD_PACKAGE + name;
        }
        return name;
    }

}
