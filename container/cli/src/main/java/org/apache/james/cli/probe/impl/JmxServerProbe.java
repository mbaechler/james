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
package org.apache.james.cli.probe.impl;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.james.cli.probe.ServerProbe;
import org.apache.james.container.spring.mailbox.MailboxCopierManagementMBean;
import org.apache.james.domainlist.api.DomainListManagementMBean;
import org.apache.james.rrt.api.RecipientRewriteTableManagementMBean;
import org.apache.james.rrt.lib.Mappings;
import org.apache.james.user.api.UsersRepositoryManagementMBean;

public class JmxServerProbe implements ServerProbe {

    // TODO: Move this to somewhere else
    private final static String DOMAINLIST_OBJECT_NAME = "org.apache.james:type=component,name=domainlist";
    private final static String VIRTUALUSERTABLE_OBJECT_NAME = "org.apache.james:type=component,name=recipientrewritetable";
    private final static String USERSREPOSITORY_OBJECT_NAME = "org.apache.james:type=component,name=usersrepository";
    private final static String MAILBOXCOPIER_OBJECT_NAME = "org.apache.james:type=component,name=mailboxcopier";

    private JMXConnector jmxc;
    
    private DomainListManagementMBean domainListProcxy;
    private RecipientRewriteTableManagementMBean virtualUserTableProxy;
    private UsersRepositoryManagementMBean usersRepositoryProxy;
    private MailboxCopierManagementMBean mailboxCopierManagement;

    private static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
    private static final int defaultPort = 9999;
    private final String host;
    private final int port;

    /**
     * Creates a ServerProbe using the specified JMX host and port.
     *
     * @param host hostname or IP address of the JMX agent
     * @param port TCP port of the remote JMX agent
     * @throws IOException on connection failures
     */
    public JmxServerProbe(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    /**
     * Creates a NodeProbe using the specified JMX host and default port.
     *
     * @param host hostname or IP address of the JMX agent
     * @throws IOException on connection failures
     */
    public JmxServerProbe(String host) throws IOException {
        this.host = host;
        this.port = defaultPort;
        connect();
    }

    /**
     * Create a connection to the JMX agent and setup the M[X]Bean proxies.
     *
     * @throws IOException on connection failures
     */
    private void connect() throws IOException {
        JMXServiceURL jmxUrl = new JMXServiceURL(String.format(fmtUrl, host, port));
        jmxc = JMXConnectorFactory.connect(jmxUrl, null);
        MBeanServerConnection mbeanServerConn = jmxc.getMBeanServerConnection();
        
        try {
            ObjectName name = new ObjectName(DOMAINLIST_OBJECT_NAME);
            domainListProcxy = MBeanServerInvocationHandler.newProxyInstance(
                    mbeanServerConn, name, DomainListManagementMBean.class, true);
            name = new ObjectName(VIRTUALUSERTABLE_OBJECT_NAME);
            virtualUserTableProxy = MBeanServerInvocationHandler
                    .newProxyInstance(mbeanServerConn, name, RecipientRewriteTableManagementMBean.class, true);
            name = new ObjectName(USERSREPOSITORY_OBJECT_NAME);
            usersRepositoryProxy = MBeanServerInvocationHandler.newProxyInstance(
                    mbeanServerConn, name, UsersRepositoryManagementMBean.class, true);
            name = new ObjectName(MAILBOXCOPIER_OBJECT_NAME);
            mailboxCopierManagement = MBeanServerInvocationHandler.newProxyInstance(
                    mbeanServerConn, name, MailboxCopierManagementMBean.class, true);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid ObjectName? Please report this as a bug.", e);
        }
    }

    @Override
    public void close() throws IOException {
        jmxc.close();
    }
    
    @Override
    public void addUser(String userName, String password) throws Exception {
        usersRepositoryProxy.addUser(userName, password);
    }

    @Override
    public void removeUser(String username) throws Exception {
        usersRepositoryProxy.deleteUser(username);
    }

    @Override
    public String[] listUsers() throws Exception {
        return usersRepositoryProxy.listAllUsers();
    }

    @Override
    public void setPassword(String userName, String password) throws Exception {
        usersRepositoryProxy.setPassword(userName, password);
    }

    @Override
    public boolean containsDomain(String domain) throws Exception {
        return domainListProcxy.containsDomain(domain);
    }

    @Override
    public void addDomain(String domain) throws Exception {
        domainListProcxy.addDomain(domain);
    }

    @Override
    public void removeDomain(String domain) throws Exception {
        domainListProcxy.removeDomain(domain);
    }

    @Override
    public String[] listDomains() throws Exception {
        return domainListProcxy.getDomains();
    }

    @Override
    public Map<String, Mappings> listMappings() throws Exception {
        return virtualUserTableProxy.getAllMappings();
    }

    @Override
    public void addAddressMapping(String user, String domain, String toAddress) throws Exception {
        virtualUserTableProxy.addAddressMapping(user, domain, toAddress);
    }

    @Override
    public void removeAddressMapping(String user, String domain, String fromAddress) throws Exception {
        virtualUserTableProxy.removeAddressMapping(user, domain, fromAddress);
    }

    @Override
    public Mappings listUserDomainMappings(String user, String domain) throws Exception {
        return virtualUserTableProxy.getUserDomainMappings(user, domain);
    }

    @Override
    public void addRegexMapping(String user, String domain, String regex) throws Exception {
        virtualUserTableProxy.addRegexMapping(user, domain, regex);
    }

    @Override
    public void removeRegexMapping(String user, String domain, String regex) throws Exception {
        virtualUserTableProxy.removeRegexMapping(user, domain, regex);
    }

    @Override
    public void copyMailbox(String srcBean, String dstBean) throws Exception {
        mailboxCopierManagement.copy(srcBean, dstBean);
    }

}
