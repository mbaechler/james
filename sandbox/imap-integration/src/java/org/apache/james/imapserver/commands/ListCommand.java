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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.ListResultImpl;

/**
 * Handles processeing for the LIST imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 109034 $
 */
class ListCommand extends AuthenticatedStateCommand
{
    public static final String NAME = "LIST";
    public static final String ARGS = "<reference-name> <mailbox-name-with-wildcards>";

    private ListCommandParser parser = new ListCommandParser();

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException, MailboxException
    {
        String referenceName = parser.mailbox( request );
        String mailboxPattern = parser.listMailbox( request );
        parser.endLine( request );

        // Should the #user.userName section be removed from names returned?
        boolean removeUserPrefix;

        ListResult[] listResults;
        
        String personalNamespace = USER_NAMESPACE + HIERARCHY_DELIMITER_CHAR +
        session.getUser().getUserName();
        
        if ( mailboxPattern.length() == 0 ) {
            // An empty mailboxPattern signifies a request for the hierarchy delimiter
            // and root name of the referenceName argument

            String referenceRoot;
            if ( referenceName.startsWith( NAMESPACE_PREFIX ) )
            {
                // A qualified reference name - get the first element,
                // and don't remove the user prefix
                removeUserPrefix = false;
                int firstDelimiter = referenceName.indexOf( HIERARCHY_DELIMITER_CHAR );
                if ( firstDelimiter == -1 ) {
                    referenceRoot = referenceName;
                }
                else {
                    referenceRoot = referenceName.substring(0, firstDelimiter );
                }
            }
            else {
                // A relative reference name - need to remove user prefix from results.
                referenceRoot = "";
                removeUserPrefix = true;
                
            }

            // Get the mailbox for the reference name.
            listResults = new ListResult[1];
            listResults[0]=new ListResultImpl(referenceRoot,HIERARCHY_DELIMITER);
        }
        else {

            // If the mailboxPattern is fully qualified, ignore the
            // reference name.
            if ( mailboxPattern.charAt( 0 ) == NAMESPACE_PREFIX_CHAR ) {
                referenceName="";
            }

            // If the search pattern is relative, need to remove user prefix from results.
            removeUserPrefix = ( (referenceName+mailboxPattern).charAt(0) != NAMESPACE_PREFIX_CHAR );

            if (removeUserPrefix) {
                referenceName=personalNamespace+"."+referenceName;
            }
            
            listResults = doList( session, referenceName, mailboxPattern );
        }


        int prefixLength = personalNamespace.length();

       for (int i = 0; i < listResults.length; i++) {
            StringBuffer message = new StringBuffer( "(" );
            String[] attrs=listResults[i].getAttributes();
            for (int j = 0; j < attrs.length; j++) {
                if (j > 0) {
                    message.append(' ');
                }
                message.append( attrs[j] );
            }
            message.append( ") \"" );
            message.append( listResults[i].getHierarchyDelimiter() );
            message.append( "\" " );

            String mailboxName = listResults[i].getName();
            if ( removeUserPrefix ) {
                if ( mailboxName.length() <= prefixLength ) {
                    mailboxName = "";
                }
                else {
                    mailboxName = mailboxName.substring( prefixLength + 1 );
                }
            }

            // TODO: need to check if the mailbox name needs quoting.
            if ( mailboxName.length() == 0 ) {
                message.append("\"\"");
            }
            else {
                message.append( mailboxName );
            }

            response.commandResponse( this, message.toString() );
        }

        session.unsolicitedResponses( response, false );
        response.commandComplete( this );
    }

    protected ListResult[] doList( ImapSession session, String base, String pattern ) throws MailboxException {
        return doList(  session,  base,  pattern, false);
    }
    
    
    protected ListResult[] doList( ImapSession session, String base, String pattern, boolean subscribed ) throws MailboxException
    {
        try {
            return session.getMailboxManager().list(base,pattern,false);
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);  
        }
    }


    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }

    private class ListCommandParser extends CommandParser
    {
        /**
         * Reads an argument of type "list_mailbox" from the request, which is
         * the second argument for a LIST or LSUB command. Valid values are a "string"
         * argument, an "atom" with wildcard characters.
         * @return An argument of type "list_mailbox"
         */
        public String listMailbox( ImapRequestLineReader request ) throws ProtocolException
        {
            char next = request.nextWordChar();
            switch ( next ) {
                case '"':
                    return consumeQuoted( request );
                case '{':
                    return consumeLiteral( request );
                default:
                    return consumeWord( request, new ListCharValidator() );
            }
        }

        private class ListCharValidator extends ATOM_CHARValidator
        {
            public boolean isValid( char chr )
            {
                if ( isListWildcard( chr ) ) {
                    return true;
                }
                return super.isValid( chr );
            }
        }
    }
}

/*
6.3..8.  LIST Command

   Arguments:  reference name
               mailbox name with possible wildcards

   Responses:  untagged responses: LIST

   Result:     OK - list completed
               NO - list failure: can't list that reference or name
               BAD - command unknown or arguments invalid

      The LIST command returns a subset of names from the complete set
      of all names available to the client.  Zero or more untagged LIST
      replies are returned, containing the name attributes, hierarchy
      delimiter, and name; see the description of the LIST reply for
      more detail.

      The LIST command SHOULD return its data quickly, without undue
      delay.  For example, it SHOULD NOT go to excess trouble to
      calculate \Marked or \Unmarked status or perform other processing;
      if each name requires 1 second of processing, then a list of 1200
      names would take 20 minutes!

      An empty ("" string) reference name argument indicates that the
      mailbox name is interpreted as by SELECT. The returned mailbox
      names MUST match the supplied mailbox name pattern.  A non-empty
      reference name argument is the name of a mailbox or a level of
      mailbox hierarchy, and indicates a context in which the mailbox
      name is interpreted in an implementation-defined manner.

      An empty ("" string) mailbox name argument is a special request to
      return the hierarchy delimiter and the root name of the name given
      in the reference.  The value returned as the root MAY be null if
      the reference is non-rooted or is null.  In all cases, the
      hierarchy delimiter is returned.  This permits a client to get the
      hierarchy delimiter even when no mailboxes by that name currently
      exist.

      The reference and mailbox name arguments are interpreted, in an
      implementation-dependent fashion, into a canonical form that
      represents an unambiguous left-to-right hierarchy.  The returned
      mailbox names will be in the interpreted form.

      Any part of the reference argument that is included in the
      interpreted form SHOULD prefix the interpreted form.  It SHOULD
      also be in the same form as the reference name argument.  This
      rule permits the client to determine if the returned mailbox name
      is in the context of the reference argument, or if something about
      the mailbox argument overrode the reference argument.  Without
      this rule, the client would have to have knowledge of the server's
      naming semantics including what characters are "breakouts" that
      override a naming context.

      For example, here are some examples of how references and mailbox
      names might be interpreted on a UNIX-based server:

               Reference     Mailbox Name  Interpretation
               ------------  ------------  --------------
               ~smith/Mail/  foo.*         ~smith/Mail/foo.*
               archive/      %             archive/%
               #news.        comp.mail.*   #news.comp.mail.*
               ~smith/Mail/  /usr/doc/foo  /usr/doc/foo
               archive/      ~fred/Mail/*  ~fred/Mail/*

      The first three examples demonstrate interpretations in the
      context of the reference argument.  Note that "~smith/Mail" SHOULD
      NOT be transformed into something like "/u2/users/smith/Mail", or
      it would be impossible for the client to determine that the
      interpretation was in the context of the reference.

      The character "*" is a wildcard, and matches zero or more
      characters at this position.  The character "%" is similar to "*",
      but it does not match a hierarchy delimiter.  If the "%" wildcard
      is the last character of a mailbox name argument, matching levels
      of hierarchy are also returned.  If these levels of hierarchy are
      not also selectable mailboxes, they are returned with the
      \Noselect mailbox name attribute (see the description of the LIST
      response for more details).

      Server implementations are permitted to "hide" otherwise
      accessible mailboxes from the wildcard characters, by preventing
      certain characters or names from matching a wildcard in certain
      situations.  For example, a UNIX-based server might restrict the
      interpretation of "*" so that an initial "/" character does not
      match.

      The special name INBOX is included in the output from LIST, if
      INBOX is supported by this server for this user and if the
      uppercase string "INBOX" matches the interpreted reference and
      mailbox name arguments with wildcards as described above.  The
      criteria for omitting INBOX is whether SELECT INBOX will return
      failure; it is not relevant whether the user's real INBOX resides
      on this or some other server.

   Example:    C: A101 LIST "" ""
               S: * LIST (\Noselect) "/" ""
               S: A101 OK LIST Completed
               C: A102 LIST #news.comp.mail.misc ""
               S: * LIST (\Noselect) "." #news.
               S: A102 OK LIST Completed
               C: A103 LIST /usr/staff/jones ""
               S: * LIST (\Noselect) "/" /
               S: A103 OK LIST Completed
               C: A202 LIST ~/Mail/ %
               S: * LIST (\Noselect) "/" ~/Mail/foo
               S: * LIST () "/" ~/Mail/meetings
               S: A202 OK LIST completed

7.2.2.  LIST Response

   Contents:   name attributes
               hierarchy delimiter
               name

      The LIST response occurs as a result of a LIST command.  It
      returns a single name that matches the LIST specification.  There
      can be multiple LIST responses for a single LIST command.

      Four name attributes are defined:

      \Noinferiors   It is not possible for any child levels of
                     hierarchy to exist under this name; no child levels
                     exist now and none can be created in the future.

      \Noselect      It is not possible to use this name as a selectable
                     mailbox.

      \Marked        The mailbox has been marked "interesting" by the
                     server; the mailbox probably contains messages that
                     have been added since the last time the mailbox was
                     selected.

      \Unmarked      The mailbox does not contain any additional
                     messages since the last time the mailbox was
                     selected.

      If it is not feasible for the server to determine whether the
      mailbox is "interesting" or not, or if the name is a \Noselect
      name, the server SHOULD NOT send either \Marked or \Unmarked.

      The hierarchy delimiter is a character used to delimit levels of
      hierarchy in a mailbox name.  A client can use it to create child
      mailboxes, and to search higher or lower levels of naming
      hierarchy.  All children of a top-level hierarchy node MUST use
      the same separator character.  A NIL hierarchy delimiter means
      that no hierarchy exists; the name is a "flat" name.

      The name represents an unambiguous left-to-right hierarchy, and
      MUST be valid for use as a reference in LIST and LSUB commands.
      Unless \Noselect is indicated, the name MUST also be valid as an
            argument for commands, such as SELECT, that accept mailbox
      names.

   Example:    S: * LIST (\Noselect) "/" ~/Mail/foo
*/
