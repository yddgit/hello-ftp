package com.my.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.ChannelSftp.LsEntrySelector;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import static com.my.project.RemoteClient.*;

public class SftpClient implements RemoteClient<LsEntry> {

	private Session session;
	private ChannelSftp channel;

	public SftpClient(String hostname, Integer port, String username, String password) throws JSchException, SftpException {
		JSch jsch = new JSch();
		this.session = jsch.getSession(username, hostname, port);
		this.session.setConfig("StrictHostKeyChecking", "no");
		InnerUserInfo userInfo = () -> password;
		this.session.setUserInfo(userInfo);
		this.session.setPassword(password);
		this.session.connect();
		
		Channel channel = this.session.openChannel("sftp");
		channel.connect();
		this.channel = (ChannelSftp)channel;
		this.channel.cd("/");
	}

	@Override
	public List<LsEntry> ls(String remotePath, boolean filterHiddenFile) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		List<LsEntry> list = new ArrayList<LsEntry>();
		LsEntrySelector selector = (entry) -> {
			String name = entry.getFilename();
			if(filterHiddenFile) {
				if(!".".equals(name) && !"..".equals(name) && !name.startsWith(".")) {
					list.add(entry);
				}
			} else {
				list.add(entry);
			}
			return LsEntrySelector.CONTINUE;
		};
		try {
			channel.ls(remotePath, selector);
		} catch (SftpException e) {
			throw new IOException(e);
		}
		return list;
	}

	@Override
	public void mkdir(String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(!this.exists(remotePath)) {
			try {
				channel.mkdir(remotePath);
			} catch (SftpException e) {
				throw new IOException(e);
			}
		} else {
			logger.warn("{} already exists", remotePath);
		}
	}

	@Override
	public void get(String remotePath, File local) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(this.exists(remotePath)) {
			try (OutputStream output = new FileOutputStream(local)) {			
				channel.get(remotePath, output);
			} catch (SftpException e) {
				throw new IOException(e);
			}
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	@Override
	public void put(File local, String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(!this.exists(remotePath)) {
			this.mkdirRecursive(remotePath);
		}
		assertNotNull(local, "local file can not be null");
		assertIsTrue(local.exists(), "local file must be exists");
		try (InputStream input = new FileInputStream(local)) {
			channel.put(input, remotePath + (remotePath.endsWith("/") ? "" : "/") + local.getName(), ChannelSftp.OVERWRITE);
		} catch (SftpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rm(String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(this.exists(remotePath)) {
			try {
				channel.rm(remotePath);
			} catch (SftpException e) {
				throw new IOException(e);
			}
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}
	
	@Override
	public void rmdir(String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(this.exists(remotePath)) {
			try {
				channel.rmdir(remotePath);
			} catch (SftpException e) {
				throw new IOException(e);
			}
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}
	
	@Override
	public boolean exists(String remotePath) {
		SftpATTRS attr = null;
		try {
			attr = channel.stat(remotePath);
		} catch (SftpException e) {
			logger.warn(e.getMessage());
		}
		if(attr != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public void close() {
		if(channel != null) {
			channel.disconnect();
			channel = null;
		}
		if(session != null) {
			session.disconnect();
			session = null;
		}
	}

	private interface InnerUserInfo extends UserInfo {
		@Override default String getPassphrase() { return null; }
		@Override default boolean promptPassword(String message) { return true; }
		@Override default boolean promptPassphrase(String message) { return false; }
		@Override default boolean promptYesNo(String message) { return true; }
		@Override default void showMessage(String message) { }
	}

}
