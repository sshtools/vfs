package com.sshtools.vfs.s3.provider.s3;

import com.amazonaws.services.s3.AmazonS3;

public interface S3Op<T> {
	T exec(AmazonS3 service);
}
