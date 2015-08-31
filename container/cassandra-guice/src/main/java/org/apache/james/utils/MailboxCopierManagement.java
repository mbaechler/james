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
import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.copier.MailboxCopier;

import java.util.Map;

public class MailboxCopierManagement implements MailboxCopierManagementMBean {

    private final Injector injector;
    private final MailboxCopier mailboxCopier;

    @Inject
    public MailboxCopierManagement(Injector injector, MailboxCopier mailboxCopier) {
        this.injector = injector;
        this.mailboxCopier = mailboxCopier;
    }

    @Override
    public Map<String, String> getMailboxManagerBeans() {
        throw new NotImplementedException();
    }

    @Override
    public void copy(String srcBean, String dstBean) throws Exception {
        Class srcClass = ClassLoader.getSystemClassLoader().loadClass(srcBean);
        Class dstClass = ClassLoader.getSystemClassLoader().loadClass(dstBean);
        MailboxManager src = injector.<MailboxManager>getInstance(srcClass);
        MailboxManager dst = injector.<MailboxManager>getInstance(dstClass);
        mailboxCopier.copyMailboxes(src, dst);
    }
}
