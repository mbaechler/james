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



package org.apache.james.pop3server;


import org.apache.james.socket.shared.CommonCommandHandler;

/**
 * Custom command handlers must implement this interface
 * The command handlers will be Server wide common to all the POP3Handlers,
 * therefore the command handlers must store all the state information
 * in the POP3Session object
 */
 public interface CommandHandler extends CommonCommandHandler{
    /**
     * Handle the command
    **/
    POP3Response onCommand(POP3Session session, String command, String parameters);

}
