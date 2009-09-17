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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.mailet.Mail;

/**
  * Handles DELE command
  */
public class DeleCmdHandler implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doDELE(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a DELE command.
     * This command deletes a particular mail message from the
     * mailbox.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doDELE(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument);
            } catch (Exception e) {
                responseString = POP3Handler.ERR_RESPONSE + " Usage: DELE [mail number]";
                session.writeResponse(responseString);
                return;
            }
            try {
                Mail mc = session.getUserMailbox().get(num);
                if (mc == POP3Handler.DELETED) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    session.writeResponse(responseString);
                } else {
                    session.getUserMailbox().set(num, POP3Handler.DELETED);
                    // we are replacing our reference with "DELETED", so we have
                    // to dispose the no-more-referenced mail object.
                    ContainerUtil.dispose(mc);
                    session.writeResponse(POP3Handler.OK_RESPONSE + " Message deleted");
                }
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder responseBuffer =
                    new StringBuilder(64)
                            .append(POP3Handler.ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
                session.writeResponse(responseString);
            }
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
            session.writeResponse(responseString);
        }
    }


}
