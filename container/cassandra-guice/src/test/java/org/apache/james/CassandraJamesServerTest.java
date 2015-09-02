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
package org.apache.james;

import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;

import org.apache.james.mailbox.cassandra.CassandraClusterSingleton;
import com.google.inject.util.Modules;
import org.apache.james.mailbox.elasticsearch.EmbeddedElasticSearch;
import org.apache.james.modules.TestElasticSearchModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public class CassandraJamesServerTest {

    private static final CassandraClusterSingleton CASSANDRA = CassandraClusterSingleton.build();
    private static final int IMAP_PORT = 1143; // You need to be root (superuser) to bind to ports under 1024.
    private static final int IMAP_PORT_SSL = 1993;
    private static final int POP3_PORT = 1110;
    private static final int SMTP_PORT = 1025;
    private static final int LMTP_PORT = 1024;

    private CassandraJamesServer server;
    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch = new EmbeddedElasticSearch(temporaryFolder);

    @Rule
    public RuleChain chain = RuleChain.outerRule(temporaryFolder).around(embeddedElasticSearch);

    @Before
    public void setup() throws Exception {
        CASSANDRA.ensureAllTables();

        server = new CassandraJamesServer(Modules.override(CassandraJamesServerMain.defaultModule).with(new TestElasticSearchModule(embeddedElasticSearch)));
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        CASSANDRA.clearAllTables();
    }

    @Test (expected = AuthenticationFailedException.class)
    public void connectIMAPServerShouldThrowWhenNoCredentials() throws Exception {
        IMAPstore(IMAP_PORT).connect();
    }

    @Test (expected = AuthenticationFailedException.class)
    public void connectOnSecondaryIMAPServerIMAPServerShouldThrowWhenNoCredentials() throws Exception {
        IMAPstore(IMAP_PORT_SSL).connect();
    }

    @Test (expected = AuthenticationFailedException.class)
    public void connectPOP3ServerShouldThrowWhenNoCredentials() throws Exception {
        POP3store().connect();
    }

    @Test
    public void connectSMTPServerShouldNotThrowWhenNoCredentials() throws Exception {
        SMTPTransport().connect();
    }

    @Test(expected = MessagingException.class)
    public void connectLMTPServerShouldNotThrowWhenNoCredentials() throws Exception {
        LMTPTransport().connect();
    }

    private Store IMAPstore(int port) throws NoSuchProviderException {
        Properties properties = new Properties();
        properties.put("mail.imap.host", "localhost");
        properties.put("mail.imap.port", String.valueOf(port));
        Session session = Session.getDefaultInstance(properties);
        session.setDebug(true);
        return session.getStore("imap");
    }

    private Store POP3store() throws NoSuchProviderException {
        Properties properties = new Properties();
        properties.put("mail.pop3.host", "localhost");
        properties.put("mail.pop3.port", String.valueOf(POP3_PORT));
        Session session = Session.getDefaultInstance(properties);
        session.setDebug(true);
        return session.getStore("pop3");
    }

    private Transport SMTPTransport() throws NoSuchProviderException {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "localhost");
        properties.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        Session session = Session.getDefaultInstance(properties);
        session.setDebug(true);
        return session.getTransport("smtp");
    }

    private Transport LMTPTransport() throws NoSuchProviderException {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "localhost");
        properties.put("mail.smtp.port", String.valueOf(LMTP_PORT));
        Session session = Session.getDefaultInstance(properties);
        session.setDebug(true);
        return session.getTransport("smtp");
    }
}
