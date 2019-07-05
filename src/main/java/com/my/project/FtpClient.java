package com.my.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

import static com.my.project.RemoteClient.*;

public class FtpClient implements RemoteClient<FTPFile> {

	private FTPClient client;

	public FtpClient(String hostname, Integer port, String username, String password) throws SocketException, IOException {
		this.client = new FTPClient();
		this.client.setListHiddenFiles(true);
		this.client.connect(hostname, port);
		this.client.login(username, password);
		this.client.setFileType(FTP.BINARY_FILE_TYPE);
		this.client.enterLocalPassiveMode();
		this.client.changeWorkingDirectory("/");
	}

	@Override
	public List<FTPFile> ls(String remotePath, boolean filterHiddenFile) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		List<FTPFile> list = new ArrayList<FTPFile>();
		FTPFileFilter filter = (ftpFile) -> {
			String name = ftpFile.getName();
			if(filterHiddenFile) {
				if(!".".equals(name) && !"..".equals(name) && !name.startsWith(".")) {
					return true;
				}
			} else {
				return true;
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
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(!this.exists(remotePath)) {
			client.makeDirectory(remotePath);
		} else {
			logger.warn("{} already exists", remotePath);
		}
	}

	@Override
	public void get(String remotePath, File local) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
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
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(!this.exists(remotePath)) {
			this.mkdirRecursive(remotePath);
		}
		assertNotNull(local, "local file can not be null");
		assertIsTrue(local.exists(), "local file must be exists");
		try (InputStream input = new FileInputStream(local)) {
			client.storeFile(remotePath + (remotePath.endsWith("/") ? "" : "/") + local.getName(), input);
		}
	}
	
	@Override
	public void rm(String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(this.exists(remotePath)) {
			client.deleteFile(remotePath);
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}
	
	@Override
	public void rmdir(String remotePath) throws IOException {
		assertNotBlank(remotePath, "remote path can not be null or blank");
		if(this.exists(remotePath)) {
			client.removeDirectory(remotePath);
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	@Override
	public boolean exists(String remotePath) {
		try(InputStream input = client.retrieveFileStream(remotePath)) {
			if(input == null || client.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
				return false;
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void close() throws IOException {
		if(client != null) {
			client.disconnect();
			client = null;
		}
	}

}
