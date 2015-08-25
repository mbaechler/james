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

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import org.apache.james.adapter.mailbox.MailboxManagerManagement;
import org.apache.james.cli.probe.impl.JmxServerProbe;
import org.apache.james.domainlist.lib.DomainListManagement;
import org.apache.james.mailbox.copier.MailboxCopier;
import org.apache.james.rrt.lib.RecipientRewriteTableManagement;
import org.apache.james.user.lib.UsersRepositoryManagement;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class MBeanRegistery {

    private final UsersRepositoryManagement usersRepositoryManagement;
    private final DomainListManagement domainListManagement;
    private final RecipientRewriteTableManagement recipientRewriteTableManagement;
    private final MailboxManagerManagement mailboxManagerManagement;
    private final MailboxCopier mailboxCopier;

    @Inject
    public MBeanRegistery(UsersRepositoryManagement usersRepositoryManagement,
                          DomainListManagement domainListManagement,
                          RecipientRewriteTableManagement recipientRewriteTableManagement,
                          MailboxManagerManagement mailboxManagerManagement,
                          MailboxCopier mailboxCopier) {
        this.usersRepositoryManagement = usersRepositoryManagement;
        this.domainListManagement = domainListManagement;
        this.recipientRewriteTableManagement = recipientRewriteTableManagement;
        this.mailboxManagerManagement = mailboxManagerManagement;
        this.mailboxCopier = mailboxCopier;
    }


    public void performRegistration() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            mBeanServer.registerMBean(usersRepositoryManagement, new ObjectName(JmxServerProbe.USERSREPOSITORY_OBJECT_NAME));
            mBeanServer.registerMBean(domainListManagement, new ObjectName(JmxServerProbe.DOMAINLIST_OBJECT_NAME));
            mBeanServer.registerMBean(recipientRewriteTableManagement, new ObjectName(JmxServerProbe.VIRTUALUSERTABLE_OBJECT_NAME));
            mBeanServer.registerMBean(mailboxManagerManagement, new ObjectName(JmxServerProbe.MAILBOXMANAGER_OBJECT_NAME));
       //     mBeanServer.registerMBean(mailboxCopier, new ObjectName(JmxServerProbe.MAILBOXCOPIER_OBJECT_NAME));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
