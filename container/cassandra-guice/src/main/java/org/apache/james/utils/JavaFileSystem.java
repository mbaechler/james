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

import org.apache.james.filesystem.api.FileSystem;

import java.io.*;

public class JavaFileSystem implements FileSystem {

    @Override
    public InputStream getResource(String url) throws IOException {
        return new FileInputStream(getFile(url));
    }

    @Override
    public File getFile(String url) throws FileNotFoundException {
        File file = new File(transformURL(url));
        if (!file.exists()) {
            throw new FileNotFoundException(transformURL(url) + " can not be found");
        }
        return file;
    }

    @Override
    public File getBasedir() throws FileNotFoundException {
        return new File("../");
    }

    private String transformURL(String url) {
        if (url.startsWith(FILE_PROTOCOL_ABSOLUTE)) {
            return "/" + url.substring(FILE_PROTOCOL_ABSOLUTE.length());
        }
        if (url.startsWith(FILE_PROTOCOL)) {
            return "../" + url.substring(FILE_PROTOCOL.length());
        }
        return "../" + url;
    }
}
