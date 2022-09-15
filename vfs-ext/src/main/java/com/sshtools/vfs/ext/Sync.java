package com.sshtools.vfs.ext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;

public class Sync {
	final static Log LOG = LogFactory.getLog(Sync.class);

	public enum Result {
		SKIP, UPDATE, ABORT
	}

	public interface Checker {
		Result check(FileObject incoming, FileObject existing) throws FileSystemException;

		void tag(Result result, FileObject file, FileObject existing) throws FileSystemException;
	}
	
	public static class AlwaysCopyChecker implements Checker {
		@Override
		public Result check(FileObject incoming, FileObject existing) throws FileSystemException {
			return Result.UPDATE;
		}

		@Override
		public void tag(Result result, FileObject file, FileObject existing) throws FileSystemException {
		}
		
	}

	public static class LastModifiedChecker implements Checker {
		@Override
		public Result check(FileObject incoming, FileObject existing) throws FileSystemException {
			long m1 = incoming.getContent().getLastModifiedTime();
			if (!existing.exists()) {
				LOG.info(String.format("%s doesn't exist, so updating from incoming %s", existing, incoming));
				return Result.UPDATE;
			}
			else {
				long m2 = existing.getContent().getLastModifiedTime();
				if(m1 > m2) {
					LOG.info(String.format("The incoming %s is newer than the existing %s, updating", incoming, existing));
					return Result.UPDATE;
				}
				else {
					LOG.info(String.format("The existing %s is newer or identical to %s, skipping", existing, incoming));
					return Result.SKIP;
				}
			}
		}

		@Override
		public void tag(Result result, FileObject incoming, FileObject existing) throws FileSystemException {
			FileContent content = incoming == null ? null : incoming.getContent();
			long tm = content == null ? System.currentTimeMillis() : content.getLastModifiedTime();
			LOG.info(String.format("Setting last modified of %s to %s from %s", existing, tm, incoming));
			existing.getContent().setLastModifiedTime(tm);
		}
	}

	private List<FileObject> sources = new ArrayList<>();
	private FileObject destination;
	private boolean recursive = true;
	private boolean preserveAttributes = true;
	private boolean deleteRemoved = true;
	private Checker checker = new LastModifiedChecker();

	public Sync() {
	}

	public Sync(FileObject destination, FileObject... sources) {
		sources(sources);
		destination(destination);
	}

	public Sync sources(FileObject... sources) {
		this.sources.clear();
		this.sources.addAll(Arrays.asList(sources));
		return this;
	}

	public Sync addSource(FileObject source) {
		sources.add(source);
		return this;
	}

	public List<FileObject> sources() {
		return sources;
	}

	public Sync destination(FileObject destination) {
		this.destination = destination;
		return this;
	}

	public FileObject destination() {
		return destination;
	}
	
	public Sync deleteRemoved(boolean deleteRemoved) {
		this.deleteRemoved = deleteRemoved;
		return this;
	}
	
	public boolean deleteRemoved() {
		return deleteRemoved;
	}

	public Sync recursive(boolean recursive) {
		this.recursive = recursive;
		return this;
	}

	public boolean recursive() {
		return recursive;
	}

	public Sync preserveAttributes(boolean preserveAttributes) {
		this.preserveAttributes = preserveAttributes;
		return this;
	}

	public boolean preserveAttributes() {
		return preserveAttributes;
	}

	public Checker checker() {
		return checker;
	}

	public Sync checker(Checker checker) {
		this.checker = checker;
		return this;
	}

	public void sync() throws IOException {
		/*
		 * If the destination doesn't exist, then only allow the copy if the
		 * parent of the destination exists and we are copying a single file
		 */
		if (!destination.exists()) {
			if (destination.getParent() == null || !destination.getParent().exists()
					|| destination.getParent().getType() != FileType.FOLDER || sources.size() > 1)
				throw new FileNotFoundException("Destination doesn't exist.");
		}
		for (FileObject f : sources) {
			sync(f, destination, 0);
		}
	}

	protected void sync(FileObject from, FileObject to, int depth) throws IOException {
		if (from.getType() == FileType.FOLDER) {
			if (depth > 0 && !recursive)
				return;
			if (to.getType() == FileType.FOLDER
					|| (to.getParent() != null && to.getParent().exists() && to.getParent().getType() == FileType.FOLDER)) {
				
				List<FileObject> exist = new LinkedList<>();
				if(deleteRemoved && to.exists()) {
					exist.addAll(Arrays.asList(to.getChildren()));
				}
				
				if(!to.exists())
					to.createFolder();
				
				for (FileObject c : from.getChildren()) {
					FileObject target = to.resolveFile(c.getName().getBaseName());
					sync(c, target, depth + 1);
					exist.remove(target);
				}
				
				for(FileObject c : exist) {
					LOG.info(String.format("Removing %s as it was removing from the source directory %s", c, to));
					c.deleteAll();
				}
			} else if (to.getType() == FileType.FILE) {
				throw new IOException(String.format("Could not synchronize %s. Target file %s is a directory", from, to));
			} else {
				throw new IOException(String.format("Could not synchronize %s. Target file %s is imaginary", from, to));
			}
		} else if (from.getType() == FileType.FILE) {
			if (to.getType() == FileType.FOLDER) {
				syncFile(from, to.resolveFile(from.getName().getBaseName()));
			} else if (to.getType() == FileType.FILE
					|| (to.getParent() != null && to.getParent().exists() && to.getParent().getType() == FileType.FOLDER)) {
				syncFile(from, to);
			} else {
				throw new IOException(String.format("Could not synchronize %s. Target file %s is imaginary", from, to));
			}
		} else
			throw new IOException(String.format("Source file %s is imaginary", from));
	}

	protected void syncFile(FileObject from, FileObject to) throws IOException {
		Result result = Result.UPDATE;
		if (checker != null) {
			result = checker.check(from, to);
			if (result == Result.ABORT)
				throw new IOException("Synchronize aborted.");
		}
		if (result == Result.SKIP)
			LOG.info(String.format("Skipping %s to %s", from, to));
		else {
			LOG.info(String.format("Copying %s to %s", from, to));
			to.copyFrom(from, new AllFileSelector());
			if (checker != null)
				checker.tag(result, from, to);
			if (preserveAttributes) {
				to.setReadable(from.isReadable(), true);
				to.setWritable(from.isWriteable(), true);
				to.setExecutable(from.isExecutable(), true);
			}
			LOG.info(String.format("Copied %s to %s", from, to));
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2)
			throw new IllegalArgumentException("Expects at least a source URI and a target URI.");
		FileSystemManager vfs = VFS.getManager();
		Sync sync = new Sync();
		int i;
		for (i = 0; i < args.length - 1; i++)
			sync.addSource(vfs.resolveFile(args[i]));
		sync.destination(vfs.resolveFile(args[i]));
		sync.sync();
	}
}
