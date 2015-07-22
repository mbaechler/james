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
package org.apache.james.user.lib;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test basic behaviors of UsersFileRepository
 */
public abstract class AbstractUsersRepositoryTest {

    /**
     * Users repository
     */
    protected UsersRepository usersRepository;

    /**
     * Create the repository to be tested.
     *
     * @return the user repository
     * @throws Exception
     */
    protected abstract UsersRepository getUsersRepository() throws Exception;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        this.usersRepository = getUsersRepository();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        disposeUsersRepository();
    }

    @Test
    public void givenFreshRepositoryCountUsersShouldReturnZero() throws UsersRepositoryException {
        assertThat(usersRepository.countUsers()).describedAs("users repository should be empty").isEqualTo(0);
    }
    
    @Test
    public void givenFreshRepositoryListShouldBeEmpty() throws UsersRepositoryException {
        assertThat(usersRepository.list()).describedAs("users repository should be empty").isEmpty();
    }
    
    @Test(expected=UsersRepositoryException.class)
    public void addUserShouldNotAllowDuplicateUsers() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        usersRepository.addUser("username", "password2");
    }
    
    @Test
    public void addUserShouldNotAllowSeveralUserCreation() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        usersRepository.addUser("username2", "password2");
        usersRepository.contains("username3");
    }
    
    @Test(expected=UsersRepositoryException.class)
    public void addUserShouldNotAllowUsersNotTakeCaseIntoAccountForDuplicate() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        usersRepository.addUser("uSeRName", "password2");
    }

    @Test
    public void givenAnEmptyRepositoryGetUserByNameShouldReturnNull() throws UsersRepositoryException {
        User user = usersRepository.getUserByName("username");
        assertThat(user).isNull();
    }

    
    @Test
    public void getUserByNameShouldReturnMatchingUser() throws UsersRepositoryException {
        usersRepository.addUser("userNaMe", "password");
        User user = usersRepository.getUserByName("userNaMe");
        assertThat(user).isNotNull();
        assertThat(user.getUserName()).isEqualTo("userNaMe");
    }
    
    @Test
    public void getUserByNameShouldBeCaseSensitive() throws UsersRepositoryException {
        usersRepository.addUser("userNaMe", "password");
        User user = usersRepository.getUserByName("username");
        assertThat(user).isNull();
    }
    
    @Test
    public void givenAnEmptyRepositoryContainsShouldReturnFalse() throws UsersRepositoryException {
        assertThat(usersRepository.contains("username")).isFalse();
    }

    
    @Test
    public void givenExistingUserContainsShouldReturnTrue() throws UsersRepositoryException {
        usersRepository.addUser("userNaMe", "password");
        assertThat(usersRepository.contains("userNaMe")).isTrue();
    }
    
    @Test
    public void containsShouldBeCaseSensitive() throws UsersRepositoryException {
        usersRepository.addUser("userNaMe", "password");
        assertThat(usersRepository.contains("username")).isFalse();
    }

    @Test
    public void testShouldAcceptExactUsernamePassword() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        assertThat(usersRepository.test("username", "password")).isTrue();
    }

    @Test
    public void testShouldNotAcceptWrongPassword() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        assertThat(usersRepository.test("username", "password2")).isFalse();
    }

    @Test
    public void testShouldNotAcceptWrongUsername() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        assertThat(usersRepository.test("username2", "password")).isFalse();
    }
    
    @Test
    public void testShouldNotAcceptWrongCaseOnPassword() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        assertThat(usersRepository.test("username", "Password")).isFalse();
    }

    @Test
    public void testShouldNotAcceptWrongCaseOnUsername() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        assertThat(usersRepository.test("Username", "password")).isFalse();
    }
    
    @Test
    public void listShouldReturnAllUsers() throws UsersRepositoryException {
        usersRepository.addUser("username1", "passwd");
        usersRepository.addUser("username2", "passwd");
        usersRepository.addUser("username3", "passwd");
        assertThat(usersRepository.list()).containsOnly("username1", "username2", "username3");
    }
    
    @Test
    public void updateUserShouldAllowPasswordChange() throws UsersRepositoryException {
        usersRepository.addUser("username1", "passwd");
        User user = usersRepository.getUserByName("username1");
        user.setPassword("newpass");
        usersRepository.updateUser(user);
        assertThat(usersRepository.test("username1", "newpass"));
    }
    

    @Test(expected=UsersRepositoryException.class)
    public void remoteUnknownUserShouldThrow() throws UsersRepositoryException {
        usersRepository.removeUser("username");
    }

    @Test
    public void removeExistingUserShouldWork() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        usersRepository.removeUser("username");
        assertThat(usersRepository.contains("username")).isFalse();
        assertThat(usersRepository.test("username", "password")).isFalse();
        assertThat(usersRepository.list()).isEmpty();
    }
    
    @Test(expected=UsersRepositoryException.class)
    public void updateUnknownUserShouldThrow() throws UsersRepositoryException {
        usersRepository.addUser("username", "password");
        User user = usersRepository.getUserByName("username");
        usersRepository.removeUser("username");
        usersRepository.updateUser(user);
    }
    

    /**
     * Dispose the repository
     *
     * @throws UsersRepositoryException
     */
    protected void disposeUsersRepository() throws UsersRepositoryException {
        if (usersRepository != null) {
            LifecycleUtil.dispose(this.usersRepository);
        }
    }

}
