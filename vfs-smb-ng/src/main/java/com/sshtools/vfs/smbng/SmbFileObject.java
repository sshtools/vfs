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
package com.sshtools.vfs.smbng;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeHasNoContentException;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.CloseableIterator;
import jcifs.Credentials;
import jcifs.SmbRandomAccess;
import jcifs.SmbResource;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;

/**
 * A file in an SMB file system.
 */
public class SmbFileObject
    extends AbstractFileObject<SmbFileSystem>
{
    // private final String fileName;
    private SmbResource file;

    protected SmbFileObject(final AbstractFileName name,
                            final SmbFileSystem fileSystem) throws FileSystemException
    {
        super(name, fileSystem);
        // this.fileName = UriParser.decode(name.getURI());
    }

    /**
     * Attaches this file object to its file resource.
     */
    @Override
    protected void doAttach() throws Exception
    {
        // Defer creation of the SmbFile to here
        if (file == null)
        {
            file = createSmbFile(getName());
        }
    }

    @Override
    protected void doDetach() throws Exception
    {
        // file closed through content-streams
        file = null;
    }

    private SmbResource createSmbFile(final FileName fileName)
        throws MalformedURLException, FileSystemException, CIFSException
    {
        final SmbFileName smbFileName = (SmbFileName) fileName;

        final String path = smbFileName.getUriWithoutAuth();

        UserAuthenticationData authData = null;
        SmbResource file;
        try
        {
            authData = UserAuthenticatorUtils.authenticate(
                           getFileSystem().getFileSystemOptions(),
                           SmbFileProvider.AUTHENTICATOR_TYPES);
            if(authData == null)
        		throw new IllegalStateException("Authentication cancelled.");
            
            CIFSContext ctx = SmbFileSystemConfigBuilder.getInstance().getContext(getFileSystem().getFileSystemOptions());

            	Credentials c;
            	ctx = ctx.withCredentials(new NtlmPasswordAuthentication(ctx, 
                    UserAuthenticatorUtils.toString(
                        UserAuthenticatorUtils.getData(authData, UserAuthenticationData.DOMAIN,
                            UserAuthenticatorUtils.toChar(smbFileName.getDomain()))),
                    UserAuthenticatorUtils.toString(
                        UserAuthenticatorUtils.getData(authData, UserAuthenticationData.USERNAME,
                            UserAuthenticatorUtils.toChar(smbFileName.getUserName()))),
                    UserAuthenticatorUtils.toString(
                        UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD,
                            UserAuthenticatorUtils.toChar(smbFileName.getPassword())))));
            	

            // if auth == null SmbFile uses default credentials
            // ("jcifs.smb.client.domain", "?"), ("jcifs.smb.client.username", "GUEST"),
            // ("jcifs.smb.client.password", BLANK);
            // ANONYMOUS=("","","")
            file = ctx.get(path);

            if (file.isDirectory() && !file.toString().endsWith("/"))
            {
            	// TODO really?
                file =  ctx.get(path + "/");
            }
            return file;
        }
        finally
        {
            UserAuthenticatorUtils.cleanup(authData); // might be null
        }
    }

    /**
     * Determines the type of the file, returns null if the file does not
     * exist.
     */
    @Override
    protected FileType doGetType() throws Exception
    {
        if (!file.exists())
        {
            return FileType.IMAGINARY;
        }
        else if (file.isDirectory())
        {
            return FileType.FOLDER;
        }
        else if (file.isFile())
        {
            return FileType.FILE;
        }

        throw new FileSystemException("vfs.provider.smb/get-type.error", getName());
    }

    /**
     * Lists the children of the file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    @Override
    protected String[] doListChildren() throws Exception
    {
        // VFS-210: do not try to get listing for anything else than directories
        if (!file.isDirectory())
        {
            return null;
        }
        
        List<String> l = new ArrayList<>();
        try(CloseableIterator<SmbResource> s = file.children()) {
        	while(s.hasNext()) {
            	l.add(s.next().getName());	
        	}
        }

        return UriParser.encode(l.toArray(new String[0]));
    }

    /**
     * Determines if this file is hidden.
     */
    @Override
    protected boolean doIsHidden() throws Exception
    {
        return file.isHidden();
    }

    /**
     * Deletes the file.
     */
    @Override
    protected void doDelete() throws Exception
    {
        file.delete();
    }

    @Override
    protected void doRename(final FileObject newfile) throws Exception
    {
        file.renameTo(createSmbFile(newfile.getName()));
    }

    /**
     * Creates this file as a folder.
     */
    @Override
    protected void doCreateFolder() throws Exception
    {
        file.mkdir();
        file = createSmbFile(getName());
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws Exception
    {
        return file.length();
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime()
        throws Exception
    {
        return file.lastModified();
    }

    /**
     * Creates an input stream to read the file content from.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception
    {
        try
        {
            return file.openInputStream();
        }
        catch (final SmbException e)
        {
            if (e.getNtStatus() == SmbException.NT_STATUS_NO_SUCH_FILE)
            {
                throw new org.apache.commons.vfs2.FileNotFoundException(getName());
            }
            else if (file.isDirectory())
            {
                throw new FileTypeHasNoContentException(getName());
            }

            throw e;
        }
    }

    /**
     * Creates an output stream to write the file content to.
     */
    @Override
    protected OutputStream doGetOutputStream(final boolean bAppend) throws Exception
    {
        return file.openOutputStream(bAppend);
    }

    /**
     * random access
     */
    @Override
    protected RandomAccessContent doGetRandomAccessContent(final RandomAccessMode mode) throws Exception
    {
    	final SmbRandomAccess smb = openRandom(mode);
		return new RandomAccessContent() {
			@Override
			public int skipBytes(int n) throws IOException {
				return smb.skipBytes(n);
			}
			
			@Override
			public int readUnsignedShort() throws IOException {
				return smb.readUnsignedShort();
			}
			
			@Override
			public int readUnsignedByte() throws IOException {
				return smb.readUnsignedByte();
			}
			
			@Override
			public String readUTF() throws IOException {
				return smb.readUTF();
			}
			
			@Override
			public short readShort() throws IOException {
				return smb.readShort();
			}
			
			@Override
			public long readLong() throws IOException {
				return smb.readLong();
			}
			
			@Override
			public String readLine() throws IOException {
				return smb.readLine();
			}
			
			@Override
			public int readInt() throws IOException {
				return smb.readInt();
			}
			
			@Override
			public void readFully(byte[] b, int off, int len) throws IOException {
				smb.readFully(b, off, len);
			}
			
			@Override
			public void readFully(byte[] b) throws IOException {
				smb.readFully(b);			}
			
			@Override
			public float readFloat() throws IOException {
				return smb.readFloat();
			}
			
			@Override
			public double readDouble() throws IOException {
				return smb.readDouble();
			}
			
			@Override
			public char readChar() throws IOException {
				return smb.readChar();
			}
			
			@Override
			public byte readByte() throws IOException {
				return smb.readByte();
			}
			
			@Override
			public boolean readBoolean() throws IOException {
				return smb.readBoolean();
			}
			
			@Override
			public void writeUTF(String s) throws IOException {
				smb.writeUTF(s);
			}
			
			@Override
			public void writeShort(int v) throws IOException {
				smb.writeShort(v);
			}
			
			@Override
			public void writeLong(long v) throws IOException {
				smb.writeLong(v);
			}
			
			@Override
			public void writeInt(int v) throws IOException {
				smb.writeInt(v);
			}
			
			@Override
			public void writeFloat(float v) throws IOException {
				smb.writeFloat(v);
			}
			
			@Override
			public void writeDouble(double v) throws IOException {
				smb.writeDouble(v);
			}
			
			@Override
			public void writeChars(String s) throws IOException {
				smb.writeChars(s);
			}
			
			@Override
			public void writeChar(int v) throws IOException {
				smb.writeChar(v);
			}
			
			@Override
			public void writeBytes(String s) throws IOException {
				smb.writeBytes(s);
			}
			
			@Override
			public void writeByte(int v) throws IOException {
				smb.writeByte(v);
			}
			
			@Override
			public void writeBoolean(boolean v) throws IOException {
				smb.writeBoolean(v);
			}
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				smb.write(b, off, len);
			}
			
			@Override
			public void write(byte[] b) throws IOException {
				smb.write(b);
			}
			
			@Override
			public void write(int b) throws IOException {
				smb.write(b);
			}
			
			@Override
			public void setLength(long newLength) throws IOException {
				smb.setLength(newLength);
			}
			
			@Override
			public void seek(long pos) throws IOException {
				smb.seek(pos);
			}
			
			@Override
			public long length() throws IOException {
				return smb.length();
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public long getFilePointer() throws IOException {
				return smb.getFilePointer();
			}
			
			@Override
			public void close() throws IOException {
				smb.close();
			}
		};
    }

	private SmbRandomAccess openRandom(final RandomAccessMode mode) throws CIFSException {
		switch(mode) {
    	case READWRITE:
        return file.openRandomAccess("rw");
    	default:
            return file.openRandomAccess("r");
    	}
	}

    @Override
    protected boolean doSetLastModifiedTime(final long modtime) throws Exception
    {
        file.setLastModified(modtime);
        return true;
    }
}
