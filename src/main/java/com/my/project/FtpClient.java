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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient implements RemoteClient<FTPFile> {

	private FTPClient client;

	public FtpClient(String hostname, Integer port, String username, String password) throws SocketException, IOException {
		this.client = new FTPClient();
		this.client.setListHiddenFiles(true);
		this.client.connect(hostname, port);
		this.client.login(username, password);
		this.client.setFileType(FTP.BINARY_FILE_TYPE);
		this.client.setBufferSize(100 * 1024);
		this.client.setUseEPSVwithIPv4(true);
		this.client.enterLocalPassiveMode();
		this.client.changeWorkingDirectory("/");
		this.client.setConnectTimeout(2 * 60 * 60 * 1000);
		this.client.setDataTimeout(2 * 60 * 60 * 1000);
		this.client.setDefaultTimeout(2 * 60 * 60 * 1000);
	}

	@Override
	public List<FTPFile> ls(String remotePath, boolean filterHiddenFile) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);

		if(!this.exists(remotePath)) {
			return Collections.emptyList();
		}

		List<FTPFile> list = new ArrayList<FTPFile>();
		FTPFileFilter filter = (ftpFile) -> {
			String name = ftpFile.getName();
			if(!".".equals(name) && !"..".equals(name)) {
				if(filterHiddenFile) {
					if(!name.startsWith(".")) {
						return true;
					}
				} else {
					return true;
				}
			}
			return false;
		};
		for(FTPFile ftpFile : client.listFiles(remotePath, filter)) {
			list.add(ftpFile);
		}
		return list;
	}

	@Override
	public void mkdir(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(!this.exists(remotePath)) {
			client.makeDirectory(remotePath);
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
				client.retrieveFile(remotePath, output);
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
			client.storeFile(remotePath + (remotePath.endsWith("/") ? "" : "/") + local.getName(), input);
		}
	}
	
	@Override
	public void rm(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
			client.deleteFile(remotePath);
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}
	
	@Override
	public void rmdir(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
			client.removeDirectory(remotePath);
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	@Override
	public boolean exists(String remotePath) {
		if(StringUtils.isBlank(remotePath)) {
			return false;
		}
		try {
			client.getStatus(remotePath);
			return FTPReply.isPositiveCompletion(client.getReplyCode());
		} catch (IOException e) {
			logger.warn(e.getMessage());
			return false;
		}
	}

	@Override
	public FTPFile stat(String remotePath) {
		if(StringUtils.isBlank(remotePath)) {
			return null;
		}
		try {
			return client.mlistFile(remotePath);
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
		return null;
	}
	
	@Override
	public boolean isDir(FTPFile entry) {
		return entry != null && entry.isDirectory();
	}
	
	@Override
	public String getFileName(FTPFile entry) {
		return entry != null ? entry.getName() : null;
	}

	@Override
	public void close() throws IOException {
		if(client != null) {
			client.disconnect();
			client = null;
		}
	}

}
