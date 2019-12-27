package com.my.project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RemoteClient<T> implements Closeable {

	public static final String REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK = "remote path can not be null or blank";
	public static final String REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED = "remote root path can not be removed";
	public static final String REMOTE_ROOT_PATH_IS_NOT_ALLOWED = "remote root path is not allowed";
	public static final String REMOTE_PATH_MUST_BE_A_FILE = "remote path must be a file: %s";
	public static final String REMOTE_PATH_MUST_BE_A_DIRECTORY = "remote path must be a directory: %s";
	public static final String LOCAL_PATH_CAN_NOT_BE_NULL = "local path can not be null";
	public static final String LOCAL_PATH_MUST_BE_EXISTS = "local path must be exists: %s";
	public static final String LOCAL_PATH_MUST_BE_A_FILE = "local path must be a file: %s";
	public static final String LOCAL_PATH_MUST_BE_A_DIRECTORY = "local path must be a directory: %s";

	public static final Logger logger = LoggerFactory.getLogger(RemoteClient.class);

	/**
	 * 列出指定目录下的文件
	 * @param remotePath 远程目录
	 * @return 文件列表
	 */
	public List<T> ls(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		return this.ls(remotePath, true);
	}

	/**
	 * 列出指定目录下的文件
	 * @param remotePath 远程目录
	 * @param filterHiddenFile 是否过滤隐藏文件
	 * @return 文件列表
	 */
	public abstract List<T> ls(String remotePath, boolean filterHiddenFile) throws IOException;

	/**
	 * 创建远程目录，如果父目录不存在则自动创建
	 * @param remotePath 远程目录
	 */
	public void mkdirRecursive(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(!this.exists(remotePath)) {
			String prefix = "";
			if(remotePath.startsWith("/")) {
				prefix = "/";
				remotePath = remotePath.substring(1);
			}
			if(remotePath.endsWith("/")) {
				remotePath = remotePath.substring(0, remotePath.length() - 1);
			}
			String[] folders = remotePath.split("/");
			folders[0] = prefix + folders[0];
			// first folder
			StringBuffer path = new StringBuffer(folders[0]);
			this.mkdir(path.toString());
			// sub folders
			int index = 1;
			while(index < folders.length) {
				path.append("/").append(folders[index]);
				this.mkdir(path.toString());
				index ++;
			}
		} else {
			logger.warn("{} already exists", remotePath);
		}
	}

	/**
	 * 创建远程目录
	 * @param remotePath 远程目录
	 */
	public abstract void mkdir(String remotePath) throws IOException;

	/**
	 * 下载文件
	 * @param remotePath 远程文件路径
	 * @param local 本地文件
	 */
	public abstract void get(String remotePath, File localFile) throws IOException;

	/**
	 * 下载文件夹
	 * @param remotePath 远程路径
	 * @param localPath 本地路径
	 */
	public void mget(String remotePath, File localPath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		assertNotNull(localPath, LOCAL_PATH_CAN_NOT_BE_NULL);
		if(!localPath.exists()) {
			localPath.mkdirs();
		} else {
			assertTrue(localPath.isDirectory(), String.format(LOCAL_PATH_MUST_BE_A_DIRECTORY, localPath.getAbsolutePath()));
		}
		remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_IS_NOT_ALLOWED);
		if(this.exists(remotePath)) {
			T entry = this.stat(remotePath);
			assertTrue(isDir(entry), String.format(REMOTE_PATH_MUST_BE_A_DIRECTORY, remotePath));
			download(entry, localPath, remotePath);
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	/**
	 * 递归下载远程文件
	 * @param entry 远程文件
	 * @param localPath 本地路径
	 */
	private void download(T entry, File localPath, String path) throws IOException {
		String fileName = getFileName(entry);
		if(!isDir(entry)) {
			this.get(path, new File(localPath, fileName));
		} else {

			File local = new File(localPath, fileName);
			if(!local.exists()) {
				local.mkdirs();
			}
			assertTrue(local.isDirectory(),  String.format(LOCAL_PATH_MUST_BE_A_DIRECTORY, local.getAbsolutePath()));

			List<T> list = this.ls(path, false);
			for(T e : list) {
				download(e, local, path + (path.endsWith("/") ? "" : "/") + getFileName(e));
			}
		}
	}

	/**
	 * 上传文件
	 * @param local 本地文件
	 * @param remotePath 远程文件路径
	 */
	public abstract void put(File localFile, String remotePath) throws IOException;

	/**
	 * 上传文件夹
	 * @param localPath 本地路径
	 * @param remotePath 远程路径
	 */
	public void mput(File localPath, String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(!this.exists(remotePath)) {
			this.mkdirRecursive(remotePath);
		} else {
			assertTrue(isDir(this.stat(remotePath)), String.format(REMOTE_PATH_MUST_BE_A_DIRECTORY, remotePath));
		}
		assertNotNull(localPath, LOCAL_PATH_CAN_NOT_BE_NULL);
		assertTrue(localPath.exists(), String.format(LOCAL_PATH_MUST_BE_EXISTS, localPath.getAbsolutePath()));
		assertTrue(localPath.isDirectory(), String.format(LOCAL_PATH_MUST_BE_A_DIRECTORY, localPath.getAbsolutePath()));
		upload(localPath, remotePath);
	}

	/**
	 * 递归上传本地文件
	 * @param localPath 本地路径
	 * @param path 远程路径
	 */
	private void upload(File localPath, String path) throws IOException {
		if(localPath.isFile()) {
			this.put(localPath, path);
		} else {
			path = path + (path.endsWith("/") ? "" : "/") + localPath.getName();
			this.mkdirRecursive(path);
			assertTrue(isDir(this.stat(path)), String.format(REMOTE_PATH_MUST_BE_A_DIRECTORY, path));
			File[] files = localPath.listFiles();
			for(File f : files) {
				upload(f, path);
			}
		}
	}

	/**
	 * 删除远程文件
	 * @param remotePath 远程文件路径
	 * @return 成功返回true，失败返回false
	 */
	public abstract void rm(String remotePath) throws IOException;

	/**
	 * 删除远程目录
	 * @param remotePath 远程目录
	 * @return 成功返回true，失败返回false
	 */
	public abstract void rmdir(String remotePath) throws IOException;

	/**
	 * 删除远程目录（递归删除）
	 * @param remotePath 远程路径
	 */
	public void rmRecursive(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		if(this.exists(remotePath)) {
			remotePath = assertRemotePathIsNotRoot(remotePath, REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
			T attr = this.stat(remotePath);
			if(!isDir(attr)) {
				this.rm(remotePath);
			} else {
				for(T e : this.ls(remotePath, false)) {
					delete(e, remotePath + "/" + getFileName(e));
				}
				this.rmdir(remotePath);
			}
		} else {
			logger.warn("{} does not exists", remotePath);
		}
	}

	/**
	 * 递归删除远程文件
	 * @param entry 指定文件
	 * @param path 文件路径
	 */
	private void delete(T entry, String path) throws IOException {
		if(!isDir(entry)) {
			this.rm(path);
		} else {
			for(T e : this.ls(path, false)) {
				delete(e, path + "/" + getFileName(e));
			}
			this.rmdir(path);
		}
	}

	/**
	 * 远程文件是否存在
	 * @param remotePath 远程目录或文件
	 * @return 存在返回true，否则返回false
	 */
	public abstract boolean exists(String remotePath);

	/**
	 * 查看文件或目录状态
	 * @param remotePath 文件或目录路径
	 * @return 文件或目录状态信息
	 */
	public abstract T stat(String remotePath);

	/**
	 * 是否为目录
	 * @param entry 指定文件
	 * @return 是目录返回true，否则返回false
	 */
	public abstract boolean isDir(T entry);

	/**
	 * 获取文件名
	 * @param entry 指定文件
	 * @return 文件名
	 */
	public abstract String getFileName(T entry);

	/**
	 * 获取文件最近修改时间
	 * @param entry 指定文件
	 * @return 上次修改时间对应的timestamp
	 */
	public abstract long getModificationTime(T entry);

	/**
	 * 检查指定对象不能为空
	 * @param object 要检查的对象
	 * @param message 异常message
	 */
	public static void assertNotNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 检查指定字符串不能为空白字符串
	 * @param s 要检查的字符串
	 * @param message 异常message
	 */
	public static void assertNotBlank(String s, String message) {
		if (StringUtils.isBlank(s)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 检查指定表达式为true
	 * @param expression 表达式
	 * @param message 表达式为false时的异常message
	 */
	public static void assertTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 检查指定表达式为false
	 * @param expression 表达式
	 * @param message 表达式为true时的异常message
	 */
	public static void assertFalse(boolean expression, String message) {
		if (expression) {
			throw new IllegalArgumentException(message);
		}
	}
	
	/**
	 * 检查远程路径不能为根目录
	 * @param remotePath 远程路径
	 * @param message 异常message
	 */
	public static String assertRemotePathIsNotRoot(String remotePath, String message) {
		if(remotePath.endsWith("/")) {
			remotePath = remotePath.substring(0, remotePath.length() - 1);
		}
		assertNotBlank(remotePath, message);
		return remotePath;
	}

	/**
	 * 是否为合法的TCP端口
	 * @param port 端口
	 * @return port合法则返回true，否则返回false
	 */
	public static boolean isValidTCPPort(Integer port) {
		if(port == null || port <= 0 || port > 65535) {
			return false;
		}
		return true;
	}
}