package com.my.project;

import static com.my.project.RemoteClient.assertIsTrue;
import static com.my.project.RemoteClient.assertNotBlank;
import static com.my.project.RemoteClient.assertNotNull;
import static com.my.project.RemoteClient.assertRemotePathIsNotRoot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
		this.session.setTimeout(2 * 60 * 60 * 1000);
		this.session.connect();
		
		Channel channel = this.session.openChannel("sftp");
		channel.connect();
		this.channel = (ChannelSftp)channel;
		this.channel.cd("/");
	}

	@Override
	public List<LsEntry> ls(String remotePath, boolean filterHiddenFile) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);

		if(!this.exists(remotePath)) {
			return Collections.emptyList();
		}

		List<LsEntry> list = new ArrayList<LsEntry>();
		LsEntrySelector selector = (entry) -> {
			String name = entry.getFilename();
			if(!".".equals(name) && !"..".equals(name)) {
				if(filterHiddenFile) {
					if(!name.startsWith(".")) {
						list.add(entry);
					}
				} else {
					list.add(entry);
				}
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
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
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
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		assertNotNull(local, LOCAL_PATH_CAN_NOT_BE_NULL);
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
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(!this.exists(remotePath)) {
			this.mkdirRecursive(remotePath);
		}
		assertNotNull(local, LOCAL_PATH_CAN_NOT_BE_NULL);
		assertIsTrue(local.exists(), LOCAL_PATH_MUST_BE_EXISTS);
		assertIsTrue(local.isFile(), LOCAL_PATH_MUST_BE_A_FILE);
		try (InputStream input = new FileInputStream(local)) {
			channel.put(input, remotePath + (remotePath.endsWith("/") ? "" : "/") + local.getName(), ChannelSftp.OVERWRITE);
		} catch (SftpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rm(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
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
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
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
		if(StringUtils.isBlank(remotePath)) {
			return false;
		}
		SftpATTRS attr = null;
		try {
			attr = channel.lstat(remotePath);
		} catch (SftpException e) {
			logger.warn(e.getMessage());
		}
		if(attr != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public LsEntry stat(String remotePath) {
		if(StringUtils.isBlank(remotePath)) {
			return null;
		}
		try {
			SftpATTRS attr = channel.lstat(remotePath);
			if(attr != null) {
				List<LsEntry> entry = new ArrayList<LsEntry>();
				if(!attr.isDir()) {
					entry.add(this.ls(remotePath, false).get(0));
				} else {
					channel.ls(remotePath, (e) -> {
						if(".".equals(e.getFilename())) {
							try {
								Method setFilename = LsEntry.class.getDeclaredMethod("setFilename", String.class);
								setFilename.setAccessible(true);
								setFilename.invoke(e, getFileNameFromRemotePath(remotePath));
							} catch (Exception ex) {
								logger.warn(ex.getMessage());
							}
							entry.add(e);
							return LsEntrySelector.BREAK;
						} else {
							return LsEntrySelector.CONTINUE;
						}
					});
				}
				if(entry.size() > 0) {
					return entry.get(0);
				}
			}
		} catch (SftpException | IOException e) {
			logger.warn(e.getMessage());
		}
		return null;
	}

	private String getFileNameFromRemotePath(String remotePath) {
		String fileName = "";
		if(remotePath.endsWith("/")) {
			remotePath = remotePath.substring(0, remotePath.length() - 1);
		}
		if(StringUtils.isBlank(remotePath)) {
			remotePath = "/";
			fileName = "/";
		} else {
			int index = remotePath.lastIndexOf("/");
			if(index != 1) {
				fileName = remotePath.substring(index + 1);
			} else {
				fileName = remotePath;
			}
		}
		return fileName;
	}

	@Override
	public boolean isDir(LsEntry entry) {
		return entry != null && entry.getAttrs().isDir();
	}
	
	@Override
	public String getFileName(LsEntry entry) {
		return entry != null ? entry.getFilename() : null;
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
