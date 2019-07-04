package com.my.project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RemoteClient<T> extends Closeable {

	public static final Logger logger = LoggerFactory.getLogger("remote-client");

	/**
	 * 列出指定目录下的文件
	 * @param remotePath 远程目录
	 * @return 文件列表
	 */
	List<T> ls(String remotePath) throws IOException;

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
		if(StringUtils.isNotBlank(remotePath)) {
			String root = "";
			if(remotePath.startsWith("/")) {
				root = "/";
				remotePath = remotePath.substring(1);
			}
			String[] folders = remotePath.split("/");
			folders[0] = root + folders[0];
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
	 * 远程文件是否存在
	 * @param remotePath 远程目录或文件
	 * @return 存在返回true，否则返回false
	 */
	boolean exists(String remotePath) throws IOException;
}