/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.mailet.MailAddress;
import org.apache.mailet.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A Jdbc-backed UserRepository which handles User instances
 * of the <CODE>DefaultJamesUser</CODE> class, or any superclass.
 *
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public class JamesUsersJdbcRepository extends AbstractJdbcUsersRepository
{
    /**
     * Reads properties for a User from an open ResultSet.
     *
     * @param rsUsers A ResultSet with a User record in the current row.
     * @return A User instance
     * @throws SQLException
     *                   if an exception occurs reading from the ResultSet
     */
    protected User readUserFromResultSet(ResultSet rsUsers) throws SQLException
    {
        // Get the column values
        String username = rsUsers.getString(1);
        String pwdHash = rsUsers.getString(2);
        String pwdAlgorithm = rsUsers.getString(3);
        boolean useForwarding = rsUsers.getBoolean(4);
        String forwardingDestination = rsUsers.getString(5);
        boolean useAlias = rsUsers.getBoolean(6);
        String alias = rsUsers.getString(7);

        MailAddress forwardAddress = null;
        if ( forwardingDestination != null ) {
            try {
                forwardAddress = new MailAddress(forwardingDestination);
            }
            catch (javax.mail.internet.ParseException pe) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(256)
                        .append("Invalid mail address in database: ")
                        .append(forwardingDestination)
                        .append(", for user ")
                        .append(username)
                        .append(".");
                throw new RuntimeException(exceptionBuffer.toString());
            }
        }

        // Build a DefaultJamesUser with these values, and add to the list.
        DefaultJamesUser user = new DefaultJamesUser(username, pwdHash, pwdAlgorithm);
        user.setForwarding(useForwarding);
        user.setForwardingDestination(forwardAddress);
        user.setAliasing(useAlias);
        user.setAlias(alias);

        return user;
    }

    /**
     * Set parameters of a PreparedStatement object with
     * property values from a User instance.
     *
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userInsert a PreparedStatement initialised with SQL taken from the "insert" SQL definition.
     * @throws SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected void setUserForInsertStatement(User user,
                                             PreparedStatement userInsert)
        throws SQLException {
        setUserForStatement(user, userInsert, false);
    }

    /**
     * Set parameters of a PreparedStatement object with
     * property values from a User instance.
     *
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userUpdate a PreparedStatement initialised with SQL taken from the "update" SQL definition.
     * @throws SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected void setUserForUpdateStatement(User user,
                                             PreparedStatement userUpdate)
        throws SQLException {
        setUserForStatement(user, userUpdate, true);
    }

    /**
     * Sets the data for the prepared statement to match the information
     * in the user object.
     *
     * @param user the user whose data is to be stored in the PreparedStatement.
     * @param stmt the PreparedStatement to be modified.
     * @param userNameLast whether the user id is the last or the first column
     */
    private void setUserForStatement(User user, PreparedStatement stmt,
                                     boolean userNameLast) throws SQLException {
        // Determine column offsets to use, based on username column pos.
        int nameIndex = 1;
        int colOffset = 1;
        if ( userNameLast ) {
            nameIndex = 7;
            colOffset = 0;
        }

        // Can handle instances of DefaultJamesUser and DefaultUser.
        DefaultJamesUser jamesUser;
        if (user instanceof DefaultJamesUser) {
            jamesUser = (DefaultJamesUser)user;
        }
        else if ( user instanceof DefaultUser ) {
            DefaultUser aUser = (DefaultUser)user;
            jamesUser = new DefaultJamesUser(aUser.getUserName(),
                                             aUser.getHashedPassword(),
                                             aUser.getHashAlgorithm());
        }
        // Can't handle any other implementations.
        else {
            throw new RuntimeException("An unknown implementation of User was " +
                                       "found. This implementation cannot be " +
                                       "persisted to a UsersJDBCRepsitory.");
        }

        // Get the user details to save.
        stmt.setString(nameIndex, jamesUser.getUserName());
        stmt.setString(1 + colOffset, jamesUser.getHashedPassword());
        stmt.setString(2 + colOffset, jamesUser.getHashAlgorithm());
        stmt.setInt(3 + colOffset, (jamesUser.getForwarding() ? 1 : 0));

        MailAddress forwardAddress = jamesUser.getForwardingDestination();
        String forwardDestination = null;
        if ( forwardAddress != null ) {
            forwardDestination = forwardAddress.toString();
        }
        stmt.setString(4 + colOffset, forwardDestination);
        stmt.setInt(5 + colOffset, (jamesUser.getAliasing() ? 1 : 0));
        stmt.setString(6 + colOffset, jamesUser.getAlias());
    }
}
