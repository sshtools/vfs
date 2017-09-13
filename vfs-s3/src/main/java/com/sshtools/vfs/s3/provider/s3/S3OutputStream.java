/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * https://github.com/spring-cloud/spring-cloud-aws/blob/master/spring-cloud-aws-core/src/main/java/org/springframework/cloud/aws/core/io/s3/SimpleStorageResource.java
 */
package com.sshtools.vfs.s3.provider.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.BinaryUtils;

public class S3OutputStream extends OutputStream {

		static Log log = LogFactory.getLog(S3OutputStream.class);
		
        // The minimum size for a multi part is 5 MB, hence the buffer size of 5 MB
        private static final int BUFFER_SIZE = 1024 * 1024 * 5;
        private ByteArrayOutputStream currentOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        private final Object monitor = new Object();
        private int partNumberCounter = 1;
        private InitiateMultipartUploadResult multiPartUploadResult;
        private AmazonS3 amazonS3;
        String bucketName;
        String objectName;

//        private final CompletionService<UploadPartResult> completionService;
//        Executor taskExeutor;
        List<PartETag> results = new ArrayList<>();
        
        S3OutputStream(AmazonS3 amazonS3, String bucketName, String objectName) {
        	this.amazonS3 = amazonS3;
        	this.bucketName = bucketName;
        	this.objectName = objectName.startsWith("/") ? objectName.substring(1) : objectName;
//        	this.taskExeutor = taskExecutor;
//        	this.completionService = new ExecutorCompletionService<>(taskExeutor);
           }

        @Override
        public void write(int b) throws IOException {
        	write(new byte[] {(byte)b});
        }
        
        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            synchronized (this.monitor) {
            	while(len > 0) {
            		if(this.currentOutputStream.size() < BUFFER_SIZE) {
            			int count = Math.min(len, BUFFER_SIZE - this.currentOutputStream.size());
            			this.currentOutputStream.write(buf, off, count);
            			off += count;
            			len -= count;
            		}
	                if (this.currentOutputStream.size() == BUFFER_SIZE) {
	                    initiateMultiPartIfNeeded();
	                    try {
							results.add(
							        new UploadPartResultCallable(amazonS3, this.currentOutputStream.toByteArray(), 
							        		this.currentOutputStream.size(), bucketName, objectName, this.multiPartUploadResult.getUploadId(), 
							        			this.partNumberCounter++, false).call().getPartETag());
						} catch (Exception e) {
							if(isMultiPartUpload()) {
								abortMultiPartUpload();
							}
							throw new IOException(e.getMessage(), e);
						} finally {
							this.currentOutputStream.reset();
						}
	                    
	                }
            	}
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (this.monitor) {
                if (this.currentOutputStream == null) {
                    return;
                }

                if (isMultiPartUpload()) {
                    finishMultiPartUpload();
                } else {
                    finishSimpleUpload();
                }
            }
        }

        private boolean isMultiPartUpload() {
            return this.multiPartUploadResult != null;
        }

        private void finishSimpleUpload() {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(this.currentOutputStream.size());

            byte[] content = this.currentOutputStream.toByteArray();
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                String md5Digest = BinaryUtils.toBase64(messageDigest.digest(content));
                objectMetadata.setContentMD5(md5Digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MessageDigest could not be initialized because it uses an unknown algorithm", e);
            }

            amazonS3.putObject(bucketName, objectName,
                    new ByteArrayInputStream(content), objectMetadata);

            //Release the memory early
            this.currentOutputStream = null;
        }

//        private List<PartETag> getMultiPartsUploadResults() throws ExecutionException, InterruptedException {
//            List<PartETag> result = new ArrayList<>(this.partNumberCounter);
//            for (int i = 0; i < this.partNumberCounter; i++) {
//                Future<UploadPartResult> uploadPartResultFuture = this.completionService.take();
//                result.add(uploadPartResultFuture.get().getPartETag());
//            }
//            return result;
//        }
        
        private void finishMultiPartUpload() throws IOException {

            try {
                results.add(new UploadPartResultCallable(amazonS3, this.currentOutputStream.toByteArray(),
                		this.currentOutputStream.size(), bucketName, objectName, 
                		this.multiPartUploadResult.getUploadId(), this.partNumberCounter, true).call().getPartETag());
            	log.info("Finishing upload");
//                List<PartETag> partETags = getMultiPartsUploadResults();
                amazonS3.completeMultipartUpload(new CompleteMultipartUploadRequest(this.multiPartUploadResult.getBucketName(),
                        this.multiPartUploadResult.getKey(), this.multiPartUploadResult.getUploadId(), results));
                log.info("Finished upload");
            } catch (Exception e) {
                abortMultiPartUpload();
                throw new IOException("Multi part upload failed ", e.getCause());
            } finally {
                this.currentOutputStream = null;
            }
        }


		private void initiateMultiPartIfNeeded() {
            if (this.multiPartUploadResult == null) {
                this.multiPartUploadResult = amazonS3.initiateMultipartUpload(
                        new InitiateMultipartUploadRequest(bucketName, objectName));
            }
        }

        private void abortMultiPartUpload() {
            if (isMultiPartUpload()) {
                amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(this.multiPartUploadResult.getBucketName(),
                        this.multiPartUploadResult.getKey(), this.multiPartUploadResult.getUploadId()));
            }
        }

        private class UploadPartResultCallable implements Callable<UploadPartResult> {

            private final AmazonS3 amazonS3;
            private final int contentLength;
            private final int partNumber;
            private final boolean last;
            private final String bucketName;
            private final String key;
            private final String uploadId;
            private byte[] content;

            private UploadPartResultCallable(AmazonS3 amazon, byte[] content, int writtenDataSize, String bucketName, String key, String uploadId, int partNumber, boolean last) {
                this.amazonS3 = amazon;
                this.content = content;
                this.contentLength = writtenDataSize;
                this.partNumber = partNumber;
                this.last = last;
                this.bucketName = bucketName;
                this.key = key;
                this.uploadId = uploadId;
            }

            @Override
            public UploadPartResult call() throws Exception {
                try {
                	log.info(String.format("Uploading part %s", uploadId));
                    return this.amazonS3.uploadPart(new UploadPartRequest().withBucketName(this.bucketName).
                            withKey(this.key).
                            withUploadId(this.uploadId).
                            withInputStream(new ByteArrayInputStream(this.content)).
                            withPartNumber(this.partNumber).
                            withLastPart(this.last).
                            withPartSize(this.contentLength));
                    
                } finally {
                    //Release the memory, as the callable may still live inside the CompletionService which would cause
                    // an exhaustive memory usage
                    this.content = null;
                    log.info(String.format("Finished iploading part %s", uploadId));
                }
                
            }
        }
    }