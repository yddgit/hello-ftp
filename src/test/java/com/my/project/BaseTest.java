package com.my.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public abstract class BaseTest<S, C extends RemoteClient<?>> {

	/** FTP/SFTP用户名 */
	public static final String USERNAME = "username";
	/** FTP/SFTP密码 */
	public static final String PASSWORD = "password";
	/** FTP/SFTP主机名 */
	public static final String HOSTNAME = "localhost";

	private TestAppender loggerAppender = new TestAppender();

	/** Exception */
	@Rule public final ExpectedException exception = ExpectedException.none();
	/** 本地根目录 */
	@Rule public final TemporaryFolder userRoot = new TemporaryFolder();
	/** FTP/SFTP根目录 */
	@Rule public final TemporaryFolder serverRoot = new TemporaryFolder();
	/** FTP/SFTP服务监听端口 */
	public Integer localPort = -1;
	/** FTP/SFTP服务端 */
	public S server = null;
	/** FTP/SFTP客户端 */
	public C client = null;

	@Before
	public void before() throws IOException {
		Logger.getRootLogger().addAppender(loggerAppender);
		this.startServer();
	}

	public abstract void startServer() throws IOException;

	@After
	public void after() throws IOException {
		this.stopServer();
		Logger.getRootLogger().removeAppender(loggerAppender);
		loggerAppender.clear();
	}

	public abstract void stopServer() throws IOException;

	@Test
	public void testLsNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_EMPTY_MESSAGE);
		client.ls(null);
		client.ls("");
		client.ls("  ");
		client.ls(null, true);
		client.ls("", true);
		client.ls("  ", true);
		client.ls(null, false);
		client.ls("", false);
		client.ls("  ", false);
	}

	@Test
	public void testLs() throws IOException {
		remote("hello.txt", "Hello World");
		remote(".hidden.txt", "Hidden File");
		client.mkdir("/new");
		assertEquals(1, client.ls("/hello.txt").size());
		assertEquals(0, client.ls("/new").size());
		assertEquals(2, client.ls("/").size());
		assertEquals(0, client.ls(".hidden.txt", true).size());
		assertEquals(1, client.ls(".hidden.txt", false).size());
		assertEquals(3, client.ls("/", false).size());
		if(client instanceof SftpClient) {
			exception.expect(IOException.class);
			exception.expectMessage("No such file or directory");
		}
		int notExistsFolder = client.ls("/a").size();
		int notExistsFile = client.ls("/a/b.txt").size();
		if(client instanceof FtpClient) {
			assertEquals(0, notExistsFolder);
			assertEquals(0, notExistsFile);
		}
	}

	@Test
	public void testMkdirNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_EMPTY_MESSAGE);
		client.mkdir(null);
		client.mkdir("");
		client.mkdir("  ");
		client.mkdirRecursive(null);
		client.mkdirRecursive("");
		client.mkdirRecursive("  ");
	}

	@Test
	public void testMkdir() throws IOException {
		remote("hello.txt", "Hello World");
		client.mkdir("hello.txt");
		assertTrue(loggerAppender.contains("hello.txt already exists"));

		client.mkdir("/new");
		client.mkdirRecursive("/a/b/c");
		assertTrue(remoteGet("/new").isDirectory());
		assertTrue(remoteGet("/a/b/c").isDirectory());

		client.mkdirRecursive("/a/b/c");
		assertTrue(loggerAppender.contains("/a/b/c already exists"));

		client.mkdir("/");
		client.mkdirRecursive("/");
		assertTrue(loggerAppender.contains("/ already exists"));
	}
	
	@Test
	public void testGet() throws IOException {
		remote("hello.txt", "Hello World");
		client.get("/hello.txt", localGet("hello.txt"));
		assertTrue(localGet("/hello.txt").isFile());
		assertEquals("Hello World", content(localGet("/hello.txt")));
	}

	@Test
	public void testPut() throws IOException {
		File local = local("newfile.txt", "Hello New File");
		client.put(local, "/new/");
		assertTrue(remoteGet("/new/newfile.txt").isFile());
		assertEquals("Hello New File", content(remoteGet("/new/newfile.txt")));
	}

	@Test
	public void testRm() throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		client.rm("/hello.txt");
		client.rmdir("/a/b/c");
		assertFalse(client.exists("/hello.txt"));
		assertFalse(client.exists("/a/b/c"));
	}

	@Test
	public void testRmRecursiveNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_EMPTY_MESSAGE);
		client.rmRecursive(null);
		client.rmRecursive("");
		client.rmRecursive("  ");
	}

	@Test
	public void testRmRecursive() throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		remote("/a/b/c/newfile.txt", "Hello New File");

		client.rmRecursive("/hello.txt");
		assertNull(client.stat("/hello.txt"));
		client.rmRecursive("/a");
		assertNull(client.stat("/a"));

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
		client.rm("/");

		client.rm("/hello.txt");
		assertTrue(loggerAppender.contains("hello.txt already exists"));
	}

	@Test
	public void testStat() throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		if(client instanceof SftpClient) {
			LsEntry f = null;
			f = (LsEntry) client.stat("/hello.txt");
			assertFalse(f.getAttrs().isDir());
			assertEquals("hello.txt", f.getFilename());
			f = (LsEntry) client.stat("/a/b/c");
			assertTrue(f.getAttrs().isDir());
			assertEquals("c", f.getFilename());
			f = (LsEntry) client.stat("/no.txt");
			assertNull(f);
			f = (LsEntry) client.stat("/d/e/f");
			assertNull(f);
		}

		if(client instanceof FtpClient) {
			FTPFile f = null;
			f = (FTPFile) client.stat("/hello.txt");
			assertFalse(f.isDirectory());
			assertEquals("hello.txt", f.getName());
			f = (FTPFile) client.stat("/a/b/c");
			assertTrue(f.isDirectory());
			assertEquals("c", f.getName());
			f = (FTPFile) client.stat("/no.txt");
			assertNull(f);
			f = (FTPFile) client.stat("/d/e/f");
			assertNull(f);
		}
	}

	@Test
	public void testExists() throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		assertTrue(client.exists("/hello.txt"));
		assertTrue(client.exists("/a/b/c"));
		assertFalse(client.exists("/no.txt"));
		assertFalse(client.exists("/d/e/f"));
	}

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

	private static class TestAppender extends AppenderSkeleton {

		private List<String> logs = new ArrayList<String>();

		@Override
		public boolean requiresLayout() { return false; }
		@Override
		public void close() { }
		@Override
		protected void append(LoggingEvent event) {
			StringBuffer buffer = new StringBuffer(128);
			buffer.append(event.getRenderedMessage()).append(Layout.LINE_SEP);
			String[] s = event.getThrowableStrRep();
			if (s != null) {
				int len = s.length;
				for (int i = 0; i < len; ++i) {
					buffer.append(s[i]).append(Layout.LINE_SEP);
				}
			}
			logs.add(buffer.toString().trim());
		}

		public boolean contains(String log) {
			return logs.contains(log);
		}

		public void clear() {
			logs.clear();
		}

	}
}
