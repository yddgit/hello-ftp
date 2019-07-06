package com.my.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class BaseTest {

	/** 本地根目录 */
	@Rule public final TemporaryFolder userRoot = new TemporaryFolder();
	/** FTP/SFTP根目录 */
	@Rule public final TemporaryFolder serverRoot = new TemporaryFolder();

	/**
	 * 查看文件内容
	 * @param file 要查看的文件
	 * @return 文件内容
	 */
	protected String content(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	/**
	 * 创建一个本地测试文件
	 * @param fileName 文件名
	 * @param content 文件内容
	 * @return 测试文件
	 */
	protected File local(String fileName, String content) throws IOException {
		return newFile(userRoot, fileName, content);
	}

	/**
	 * 创建一个本地测试文件（内容为空）
	 * @param fileName 文件名
	 * @return 测试文件
	 */
	protected File local(String fileName) throws IOException {
		return newFile(userRoot, fileName);
	}

	/**
	 * 创建本地测试目录
	 * @param folders 目录名
	 * @return 目录
	 */
	protected File localFolder(String... folders) throws IOException {
		return newFolder(userRoot, folders);
	}

	/**
	 * 创建一个本地测试文件（文件名随机）
	 * @param content 文件内容
	 * @return 测试文件
	 */
	protected File localRandom(String content) throws IOException {
		return newRandomFile(userRoot, content);
	}

	/**
	 * 创建一个本地测试文件（文件名随机，内容为空）
	 * @return 测试文件
	 */
	protected File localRandom() throws IOException {
		return newRandomFile(userRoot);
	}

	/**
	 * 删除本地测试文件或目录
	 * @param path 文件路径
	 */
	protected void localDelete(String path) {
		delete(userRoot, path);
	}

	/**
	 * 列出本地测试目录下的文件
	 * @param path 路径
	 * @param filterHiddenFile 是否忽略隐藏文件
	 * @return 指定路径下的文件列表
	 */
	protected List<File> localList(String path, boolean filterHiddenFile) {
		return listFile(userRoot, path, filterHiddenFile);
	}
	
	/**
	 * 列出本地测试目录下的文件（忽略隐藏文件）
	 * @param path 路径
	 * @return 指定路径下的文件列表
	 */
	protected List<File> localList(String path) {
		return listFile(userRoot, path, true);
	}
	
	/**
	 * 列出本地测试根目录下的文件
	 * @param filterHiddenFile 是否忽略隐藏文件
	 * @return 根路径下的文件列表
	 */
	protected List<File> localList(boolean filterHiddenFile) {
		return listFile(userRoot, null, filterHiddenFile);
	}

	/**
	 * 列出本地测试根目录下的文件（忽略隐藏文件）
	 * @return 根路径下的文件列表
	 */
	protected List<File> localList() {
		return listFile(userRoot, null, true);
	}
	
	/**
	 * 获取本地测试目录下的文件
	 * @param path 文件路径
	 * @return 指定路径的文件
	 */
	protected File localGet(String path) {
		return new File(userRoot.getRoot(), path);
	}

	/**
	 * 创建一个远程测试文件
	 * @param fileName 文件名
	 * @param content 文件内容
	 * @return 测试文件
	 */
	protected File remote(String fileName, String content) throws IOException {
		return newFile(serverRoot, fileName, content);
	}

	/**
	 * 创建一个远程测试文件（内容为空）
	 * @param fileName 文件名
	 * @return 测试文件
	 */
	protected File remote(String fileName) throws IOException {
		return newFile(serverRoot, fileName);
	}
	
	/**
	 * 创建远程测试目录
	 * @param folders 目录名
	 * @return 目录
	 */
	protected File remoteFolder(String... folders) throws IOException {
		return newFolder(serverRoot, folders);
	}

	/**
	 * 创建一个远程测试文件（文件名随机）
	 * @param content 文件内容
	 * @return 测试文件
	 */
	protected File remoteRandom(String content) throws IOException {
		return newRandomFile(serverRoot, content);
	}
	
	/**
	 * 创建一个远程测试文件（文件名随机，内容为空）
	 * @return 测试文件
	 */
	protected File remoteRandom() throws IOException {
		return newRandomFile(serverRoot);
	}
	
	/**
	 * 删除远程测试文件或目录
	 * @param path 文件路径
	 */
	protected void remoteDelete(String path) {
		delete(serverRoot, path);
	}

	/**
	 * 列出远程测试目录下的文件
	 * @param path 路径
	 * @param filterHiddenFile 是否忽略隐藏文件
	 * @return 指定路径下的文件列表
	 */
	protected List<File> remoteList(String path, boolean filterHiddenFile) {
		return listFile(serverRoot, path, filterHiddenFile);
	}
	
	/**
	 * 列出远程测试目录下的文件（忽略隐藏文件）
	 * @param path 路径
	 * @return 指定路径下的文件列表
	 */
	protected List<File> remoteList(String path) {
		return listFile(serverRoot, path, true);
	}
	
	/**
	 * 列出远程测试根目录下的文件
	 * @param filterHiddenFile 是否忽略隐藏文件
	 * @return 根路径下的文件列表
	 */
	protected List<File> remoteList(boolean filterHiddenFile) {
		return listFile(serverRoot, null, filterHiddenFile);
	}

	/**
	 * 列出远程测试根目录下的文件（忽略隐藏文件）
	 * @return 根路径下的文件列表
	 */
	protected List<File> remoteList() {
		return listFile(serverRoot, null, true);
	}
	
	/**
	 * 获取远程测试目录下的文件
	 * @param path 文件路径
	 * @return 指定路径的文件
	 */
	protected File remoteGet(String path) {
		return new File(serverRoot.getRoot(), path);
	}

	private File newFolder(TemporaryFolder root, String... folders) throws IOException {
		return root.newFolder(folders);
	}

	private File newFile(TemporaryFolder root, String fileName) throws IOException {
		return this.newFile(root, fileName, null);
	}

	private File newFile(TemporaryFolder root, String fileName, String content) throws IOException {
		File newFile = root.newFile(fileName);

		if(content != null) {
			Files.write(newFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);
		}

		return newFile;
	}

	private File newRandomFile(TemporaryFolder root) throws IOException {
		return newRandomFile(root, null);
	}

	private File newRandomFile(TemporaryFolder root, String content) throws IOException {
		File random = root.newFile();
		if(content != null) {
			Files.write(random.toPath(), content.getBytes(), StandardOpenOption.WRITE);
		}
		return random;
	}

	private void delete(TemporaryFolder root, String path) {
		File f = new File(root.getRoot(), path);
		if(f.exists()) {
			delete(f);
		}
	}

	private void delete(File file) {
    	if(file.isDirectory()) {
	        File[] files = file.listFiles();
	        if (files != null) {
	            for (File each : files) {
	            	delete(each);
	            }
	        }
    	} else {
    		file.delete();
    	}
    }

	private List<File> listFile(TemporaryFolder root, String path, boolean filterHiddenFile) {

		File folder = null;
		if(path == null) {
			folder = root.getRoot();
		} else {
			folder = new File(root.getRoot(), path);
		}

		List<File> list = new ArrayList<File>();

		if(folder.isDirectory()) {
			File[] files = folder.listFiles((dir, name) -> {
				if(filterHiddenFile) {
					return !".".equals(name) && !"..".equals(name) && !name.startsWith(".");
				}
				return true;
			});
			for(File file : files) {
				list.add(file);
			}
		} else {
			list.add(folder);
		}

		return list;
	}

}
