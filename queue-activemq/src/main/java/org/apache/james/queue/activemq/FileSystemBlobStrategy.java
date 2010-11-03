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
package org.apache.james.queue.activemq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jms.JMSException;
import javax.mail.util.SharedFileInputStream;

import org.apache.activemq.BlobMessage;
import org.apache.activemq.blob.BlobDownloadStrategy;
import org.apache.activemq.blob.BlobTransferPolicy;
import org.apache.activemq.blob.BlobUploadStrategy;
import org.apache.activemq.command.ActiveMQBlobMessage;
import org.apache.james.core.NonClosingSharedInputStream;
import org.apache.james.services.FileSystem;

/**
 * {@link BlobUploadStrategy} and {@link BlobDownloadStrategy} implementation which use the {@link FileSystem} to lookup the {@link File} for the {@link BlobMessage}
 *
 */
public class FileSystemBlobStrategy implements BlobUploadStrategy, BlobDownloadStrategy, ActiveMQSupport{

   
    private final FileSystem fs;
    private final BlobTransferPolicy policy;

    public FileSystemBlobStrategy(final BlobTransferPolicy policy, final FileSystem fs) {
        this.fs = fs;
        this.policy = policy;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.activemq.blob.BlobUploadStrategy#uploadFile(org.apache.activemq.command.ActiveMQBlobMessage, java.io.File)
     */
    public URL uploadFile(ActiveMQBlobMessage message, File file) throws JMSException, IOException {
        return uploadStream(message, new FileInputStream(file));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.activemq.blob.BlobUploadStrategy#uploadStream(org.apache.activemq.command.ActiveMQBlobMessage, java.io.InputStream)
     */
    public URL uploadStream(ActiveMQBlobMessage message, InputStream in) throws JMSException, IOException {
        File f = getFile(message);
        FileOutputStream out = new FileOutputStream(f);
        byte[] buffer = new byte[policy.getBufferSize()];
        for (int c = in.read(buffer); c != -1; c = in.read(buffer)) {
            out.write(buffer, 0, c);
            out.flush();
        }
        out.flush();
        out.close();
        return f.toURL();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.activemq.blob.BlobDownloadStrategy#deleteFile(org.apache.activemq.command.ActiveMQBlobMessage)
     */
    public void deleteFile(ActiveMQBlobMessage message) throws IOException, JMSException {
        File f = getFile(message);
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * Returns a {@link SharedFileInputStream} for the give {@link BlobMessage}
     */
    public InputStream getInputStream(ActiveMQBlobMessage message) throws IOException, JMSException {
        // return a NonClosingSharedInputStream to make sure the stream will only get closed on dispose call later
        return new NonClosingSharedInputStream<SharedFileInputStream>(new SharedFileInputStream(getFile(message)));
    }

    
    /**
     * Return the {@link File} for the {@link ActiveMQBlobMessage}. 
     * The {@link File} is lookup via the {@link FileSystem} service
     * 
     * @param message
     * @return file
     * @throws JMSException
     * @throws FileNotFoundException
     */
    protected File getFile(ActiveMQBlobMessage message) throws JMSException, FileNotFoundException {
        String queueName = message.getStringProperty(JAMES_QUEUE_NAME);
        String mailname = message.getStringProperty(JAMES_MAIL_NAME);
        String queueUrl = policy.getUploadUrl() + "/" + queueName + "/";
        File queueF = fs.getFile(queueUrl);
        
        // check if we need to create the queue folder
        if (queueF.exists() == false) {
            queueF.mkdirs();
        }
        return fs.getFile(queueUrl+ "/" + mailname);
        
    }
}
