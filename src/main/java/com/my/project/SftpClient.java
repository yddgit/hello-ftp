package com.my.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
import com.jcraft.jsch.SocketFactory;
import com.jcraft.jsch.UserInfo;

public class SftpClient extends RemoteClient<LsEntry> {

	private Session session;
	private ChannelSftp channel;
	private File privateKeyFile;

	/**
	 * 创建一个SFTP连接
	 * @param hostname SFTP主机
	 * @param port SFTP端口
	 * @param username SFTP用户名
	 * @param password SFTP用户密码
	 * @param timeout 连接超时时间(ms)
	 * @param proxyHost SOCK5代理主机
	 * @param proxyPort SOCK5代理端口
	 */
	public SftpClient(String hostname, Integer port, String username, String password, String privateKey, String passphrase, int timeout, String proxyHost, Integer proxyPort) throws JSchException, SftpException, IOException {
		JSch jsch = new JSch();
		if(StringUtils.isNotBlank(privateKey)) {
			privateKeyFile = new File(System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + ".key");
			Files.write(privateKeyFile.toPath(), privateKey.getBytes());
			jsch.addIdentity(privateKeyFile.getCanonicalPath(), passphrase);
		}
		this.session = jsch.getSession(username, hostname, port);
		this.session.setConfig("StrictHostKeyChecking", "no");
		this.session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
		InnerUserInfo userInfo = () -> password;
		this.session.setUserInfo(userInfo);
		this.session.setPassword(password);
		this.session.setTimeout(timeout);
		if(StringUtils.isNotBlank(proxyHost) && isValidTCPPort(proxyPort)) {
			this.session.setSocketFactory(new SocketFactory() {
				@Override
				public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
					Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort)));
					socket.connect(new InetSocketAddress(host, port), timeout);
					return socket;
				}
				@Override
				public InputStream getInputStream(Socket socket) throws IOException {
					return socket.getInputStream();
				}
				@Override
				public OutputStream getOutputStream(Socket socket) throws IOException {
					return socket.getOutputStream();
				}
			});
		}
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
	public void get(String remotePath, File localFile) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		assertNotNull(localFile, LOCAL_PATH_CAN_NOT_BE_NULL);
		if(this.exists(remotePath)) {
			assertFalse(isDir(this.stat(remotePath)), String.format(REMOTE_PATH_MUST_BE_A_FILE, remotePath));
			try (OutputStream output = new FileOutputStream(localFile)) {			
				channel.get(remotePath, output);
			} catch (SftpException e) {
				throw new IOException(e);
			}
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	@Override
	public void put(File localFile, String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(!this.exists(remotePath)) {
			this.mkdirRecursive(remotePath);
		} else {
			assertTrue(isDir(this.stat(remotePath)), String.format(REMOTE_PATH_MUST_BE_A_DIRECTORY, remotePath));
		}
		assertNotNull(localFile, LOCAL_PATH_CAN_NOT_BE_NULL);
		assertTrue(localFile.exists(), String.format(LOCAL_PATH_MUST_BE_EXISTS, localFile.getAbsolutePath()));
		assertTrue(localFile.isFile(), String.format(LOCAL_PATH_MUST_BE_A_FILE, localFile.getAbsolutePath()));
		try (InputStream input = new FileInputStream(localFile)) {
			channel.put(input, remotePath + (remotePath.endsWith("/") ? "" : "/") + localFile.getName(), ChannelSftp.OVERWRITE);
		} catch (SftpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rm(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
			assertFalse(isDir(this.stat(remotePath)), String.format(REMOTE_PATH_MUST_BE_A_FILE, remotePath));
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
			assertTrue(isDir(this.stat(remotePath)), String.format(REMOTE_PATH_MUST_BE_A_DIRECTORY, remotePath));
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
					String filename = getFileNameFromRemotePath(remotePath);
					channel.ls(remotePath, (e) -> {
						if(".".equals(e.getFilename())) {
							try {
								Method setFilename = LsEntry.class.getDeclaredMethod("setFilename", String.class);
								setFilename.setAccessible(true);
								setFilename.invoke(e, filename);
							} catch (Exception ex) {
								logger.warn(ex.getMessage());
							}
							entry.add(e);
							return LsEntrySelector.BREAK;
						} else {
							return LsEntrySelector.CONTINUE;
						}
					});
					if(entry.isEmpty()) {
						Constructor<LsEntry> constructor = LsEntry.class.getDeclaredConstructor(ChannelSftp.class, String.class, String.class, SftpATTRS.class);
						constructor.setAccessible(true);
						String longname = attr.toString() + " " + filename;
						LsEntry e = constructor.newInstance(channel, filename, longname, attr);
						entry.add(e);
					}
				}
				if(entry.size() > 0) {
					return entry.get(0);
				}
			}
		} catch (Exception e) {
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
	public long getModificationTime(LsEntry entry) {
		// getMTime() method return seconds, but we need return milliseconds
		return entry != null ? entry.getAttrs().getMTime() * 1000L : 0L;
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
		if(privateKeyFile != null && privateKeyFile.exists()) {
			privateKeyFile.delete();
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
