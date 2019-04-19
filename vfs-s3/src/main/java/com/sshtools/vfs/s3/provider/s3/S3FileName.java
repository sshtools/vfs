/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.vfs.s3.provider.s3;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;

import com.amazonaws.util.StringUtils;

/**
 * An SMB URI. Adds a share name to the generic URI.
 */
public class S3FileName extends GenericFileName {
	protected S3FileName(final String scheme, final String hostName, final String userName, final String password,
			final String path, final FileType type) {
		super(scheme, hostName, 0, 0, userName, password, path, type);
	}

	/**
	 * Factory method for creating name instances.
	 *
	 * @param path path of file.
	 * @param type file or directory
	 * @return new SmbFileName object, never null.
	 */
	@Override
	public FileName createName(final String path, final FileType type) {
		return new S3FileName(getScheme(), getHostName(), getUserName(), getPassword(), path, type);
	}

	@Override
	public FileName getParent() {
		FileName parent = super.getParent();
		if (parent == null) {
			if (!StringUtils.isNullOrEmpty(getHostName())) {
				return new S3FileName(getScheme(), null, null, getUserName(), null, FileType.FOLDER);
			}
		}
		return parent;
	}

	/**
	 * Returns the base name of the file.
	 * 
	 * @return The base name of the file.
	 */
	@Override
	public String getBaseName() {
		if (getPath().equals("/") && !StringUtils.isNullOrEmpty(getHostName()))
			return getHostName();
		return super.getBaseName();
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
		buffer.append("://");
		appendCredentials(buffer, addPassword);
		if (!StringUtils.isNullOrEmpty(getHostName())) {
			buffer.append(getHostName());
		}
	}
}
