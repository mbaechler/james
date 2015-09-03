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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

public class JavaFileSystemTest {

    private JavaFileSystem javaFileSystem;

    @Before
    public void setUp() {
        javaFileSystem = new JavaFileSystem();
    }

    @Test
    public void test1() throws Exception {
        assertThat(IOUtils.toString(javaFileSystem.getResource("cassandra-guice/src/test/resources/test.text"))).isEqualTo("content");
    }

    @Test(expected = FileNotFoundException.class)
    public void test2() throws Exception {
        javaFileSystem.getResource("cassandra-guice/src/test/resources/no.text");
    }

    @Test
    public void test3() throws Exception {
        assertThat(IOUtils.toString(javaFileSystem.getResource("file://cassandra-guice/src/test/resources/test.text"))).isEqualTo("content");
    }

    @Test(expected = FileNotFoundException.class)
    public void test4() throws Exception {
        javaFileSystem.getResource("file://cassandra-guice/src/test/resources/no.text");
    }

    @Test
    public void test5() throws Exception {
        assertThat(FileUtils.readFileToString(javaFileSystem.getFile("cassandra-guice/src/test/resources/test.text"))).isEqualTo("content");
    }

    @Test(expected = FileNotFoundException.class)
    public void test6() throws Exception {
        javaFileSystem.getFile("cassandra-guice/src/test/resources/no.text");
    }

    @Test
    public void test7() throws Exception {
        assertThat(FileUtils.readFileToString(javaFileSystem.getFile("file://cassandra-guice/src/test/resources/test.text"))).isEqualTo("content");
    }

    @Test(expected = FileNotFoundException.class)
    public void test8() throws Exception {
        javaFileSystem.getFile("file://cassandra-guice/src/test/resources/no.text");
    }

}
