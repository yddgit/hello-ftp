package com.my.project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RemoteClient<T> extends Closeable {

	public static final String REMOTE_PATH_EMPTY_MESSAGE = "remote path can not be null or blank";
	public static final String REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED = "remote root path can not be removed";
	public static final Logger logger = LoggerFactory.getLogger("remote-client");

	/**
	 * 列出指定目录下的文件
	 * @param remotePath 远程目录
	 * @return 文件列表
	 */
	default List<T> ls(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_EMPTY_MESSAGE);
		return this.ls(remotePath, true);
	}

	/**
	 * 列出指定目录下的文件
	 * @param remotePath 远程目录
	 * @param filterHiddenFile 是否过滤隐藏文件
	 * @return 文件列表
	 */
	List<T> ls(String remotePath, boolean filterHiddenFile) throws IOException;

	/**
	 * 创建远程目录，如果父目录不存在则自动创建
	 * @param remotePath 远程目录
	 */
	default void mkdirRecursive(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_EMPTY_MESSAGE);
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
	void mkdir(String remotePath) throws IOException;

	/**
	 * 下载文件
	 * @param remotePath 远程文件路径
	 * @param local 本地文件
	 */
	void get(String remotePath, File local) throws IOException;

	/**
	 * 上传文件
	 * @param local 本地文件
	 * @param remotePath 远程文件路径
	 */
	void put(File local, String remotePath) throws IOException;

	/**
	 * 删除远程文件
	 * @param remotePath 远程文件路径
	 * @return 成功返回true，失败返回false
	 */
	void rm(String remotePath) throws IOException;

	/**
	 * 删除远程目录
	 * @param remotePath 远程目录
	 * @return 成功返回true，失败返回false
	 */
	void rmdir(String remotePath) throws IOException;

	/**
	 * 删除远程目录（递归删除）
	 * @param remotePath 远程路径
	 */
	default void rmRecursive(String remotePath) throws IOException {
		assertNotBlank(remotePath, REMOTE_PATH_EMPTY_MESSAGE);
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
	 * 递归删除远程文件（仅限内部调用）
	 * @param entry 指定文件
	 * @param path 文件路径
	 */
	default void delete(T entry, String path) throws IOException {
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
	boolean exists(String remotePath);

	/**
	 * 查看文件或目录状态
	 * @param remotePath 文件或目录路径
	 * @return 文件或目录状态信息
	 */
	T stat(String remotePath);

	/**
	 * 是否为目录
	 * @param entry 指定文件
	 * @return 是目录返回true，否则返回false
	 */
	boolean isDir(T entry);

	/**
	 * 获取文件名
	 * @param entry 指定文件
	 * @return 文件名
	 */
	String getFileName(T entry);

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
	public static void assertIsTrue(boolean expression, String message) {
		if (!expression) {
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

}