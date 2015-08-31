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
package org.apache.james.jmap;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class JMAPAuthenticationTest {
	
	private static final int RANDOM_PORT = 0;
	private Server server;
	
	private UsersRepository mockedUsersRepository;

	@Before
	public void setup() throws Exception {
        server = new Server(RANDOM_PORT);
 
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        
        mockedUsersRepository = mock(UsersRepository.class);
        AuthenticationServlet authenticationServlet = new AuthenticationServlet();
        authenticationServlet.setUsersRepository(mockedUsersRepository);
        ServletHolder servletHolder = new ServletHolder(authenticationServlet);
        handler.addServletWithMapping(servletHolder, "/*");
 
        server.start();

        int localPort = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        RestAssured.port = localPort;
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
        
	}
	
	@Test
	public void mustReturnMalformedRequestWhenContentTypeIsMissing() {
		given()
			.accept(ContentType.JSON)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenContentTypeIsNotJson() {
		given()
			.contentType(ContentType.XML)
			.accept(ContentType.JSON)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenAcceptIsMissing() {
		given()
			.contentType(ContentType.JSON)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenAcceptIsNotJson() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.XML)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenCharsetIsNotUTF8() {
		given()
			.contentType("application/json; charset=ISO-8859-1")
			.accept(ContentType.JSON)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenBodyIsEmpty() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnMalformedRequestWhenBodyIsNotAcceptable() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"badAttributeName\": \"value\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(400);
	}
	
	@Test
	public void mustReturnJsonResponse() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(200)
			.contentType(ContentType.JSON);
	}
	
	@Test
	public void methodShouldContainPasswordWhenValidResquest() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(200)
			.body("methods", hasItem("password"));
	}
	
	@Test
	public void mustReturnContinuationTokenWhenValidResquest() {
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(200)
			.body("continuationToken", isA(String.class));
	}
	
	@Test
	public void mustReturnAuthenticationFailedWhenBadPassword() {
		String continuationToken =
		with()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.post("/authentication")
			.body()
			.path("continuationToken")
			.toString();
		
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"badpassword\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(401);
	}
	
	@Test
	public void mustReturnAuthenticationFailedWhenUsersRepositoryException() throws Exception {
		String continuationToken =
		with()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.post("/authentication")
			.body()
			.path("continuationToken")
			.toString();
		
		when(mockedUsersRepository.test("username", "password"))
			.thenThrow(new UsersRepositoryException("test"));
	
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"password\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(401);
	}
	
	@Test
	public void mustReturnCreatedWhenGoodPassword() throws Exception {
		String continuationToken =
		with()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.post("/authentication")
			.body()
			.path("continuationToken")
			.toString();
		
		when(mockedUsersRepository.test("username", "password"))
			.thenReturn(true);
		
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"password\"}")
		.when()
			.post("/authentication")
		.then()
			.statusCode(201);
	}
	
	@Test
	public void mustSendJsonContainingAccessTokenWhenGoodPassword() throws Exception {
		String continuationToken =
		with()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"username\": \"user@domain.tld\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
		.post("/authentication")
			.body()
			.path("continuationToken")
			.toString();
		
		when(mockedUsersRepository.test("username", "password"))
			.thenReturn(true);
		
		given()
			.contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"password\"}")
		.when()
			.post("/authentication")
		.then()
			.contentType(ContentType.JSON)
			.body("accessToken", isA(String.class));
	}
	
	@After
	public void teardown() throws Exception {
		server.stop();
	}

}
