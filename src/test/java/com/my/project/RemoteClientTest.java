package com.my.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class RemoteClientTest<S, C extends RemoteClient<?>> extends BaseTest {

	/** FTP/SFTP用户名 */
	public static final String USERNAME = "username";
	/** FTP/SFTP密码 */
	public static final String PASSWORD = "password";
	/** FTP/SFTP主机名 */
	public static final String HOSTNAME = "localhost";
	/** 连接超时时间(milliseconds) */
	public static final int TIMEOUT = 60 * 1000;

	/** Log Appender */
	public final TestAppender loggerAppender = new TestAppender();
	/** Exception */
	@Rule public final ExpectedException exception = ExpectedException.none();

	/** FTP/SFTP服务监听端口 */
	public Integer localPort = -1;
	/** FTP/SFTP服务端 */
	public S server = null;
	/** FTP/SFTP客户端 */
	public C client = null;

	/**
	 * 启动测试FTP/SFTP服务
	 */
	public abstract void startServer() throws IOException;
	/**
	 * 关闭测试FTP/SFTP服务
	 */
	public abstract void stopServer() throws IOException;

	@Before
	public void before() throws IOException {
		Logger.getRootLogger().addAppender(loggerAppender);
		this.startServer();
	}

	@After
	public void after() throws IOException {
		this.stopServer();
		Logger.getRootLogger().removeAppender(loggerAppender);
		loggerAppender.clear();
	}

	@Test
	public void testLsNull1() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls(null);
	}

	@Test
	public void testLsEmpty1() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("");
	}
	
	@Test
	public void testLsBlank1() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("  ");
	}
	
	@Test
	public void testLsNull2() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls(null, true);
	}
	
	@Test
	public void testLsEmpty2() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("", true);
	}
	
	@Test
	public void testLsBlank2() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("  ", true);
	}
	
	@Test
	public void testLsNull3() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls(null, false);
	}
	
	@Test
	public void testLsEmpty3() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("", false);
	}
	
	@Test
	public void testLsBlank3() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.ls("  ", false);
	}
	
	@Test
	public void testLs() throws IOException {
		remote("hello.txt", "Hello World");
		remote(".hidden.txt", "Hidden File");
		remoteFolder("new");

		assertEquals(1, client.ls("/hello.txt").size());
		assertEquals(0, client.ls("/new").size());
		assertEquals(2, client.ls("/").size());
		assertEquals(0, client.ls(".hidden.txt", true).size());
		assertEquals(1, client.ls(".hidden.txt", false).size());
		assertEquals(3, client.ls("/", false).size());
		assertEquals(0, client.ls("/a").size());
		assertEquals(0, client.ls("/a/b.txt").size());
	}

	@Test
	public void testMkdirNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdir(null);
	}

	@Test
	public void testMkdirEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdir("");
	}
	
	@Test
	public void testMkdirBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdir("  ");
	}
	
	@Test
	public void testMkdirRecursiveNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdirRecursive(null);
	}
	
	@Test
	public void testMkdirRecursiveEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdirRecursive("");
	}
	
	@Test
	public void testMkdirRecursiveBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mkdirRecursive("  ");
	}

	@Test
	public void testMkdir() throws IOException {
		remote("hello.txt", "Hello World");

		client.mkdir("hello.txt");
		assertTrue(loggerAppender.contains("hello.txt already exists"));
		client.mkdir("/new");
		assertTrue(remoteGet("/new").isDirectory());
		client.mkdir("/new");
		assertTrue(loggerAppender.contains("/new already exists"));
		client.mkdir("/");
		assertTrue(loggerAppender.contains("/ already exists"));
	}

	@Test
	public void testMkdirRecursive() throws IOException {
		client.mkdirRecursive("/a/b/c");
		assertTrue(remoteGet("/a/b/c").isDirectory());
		client.mkdirRecursive("/a/b/c");
		assertTrue(loggerAppender.contains("/a/b/c already exists"));
		client.mkdirRecursive("/");
		assertTrue(loggerAppender.contains("/ already exists"));
	}

	@Test
	public void testGetNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.get(null, null);
	}

	@Test
	public void testGetEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.get("", null);
	}

	@Test
	public void testGetBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.get("  ", null);
	}
	
	@Test
	public void testGetLocalNull() throws IOException {
		remote("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_CAN_NOT_BE_NULL);
		client.get("/hello.txt", null);
	}

	@Test
	public void testGetRemotePathIsDir() throws IOException {
		remoteFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_FILE, "/a/b/c"));
		client.get("/a/b/c", localGet("c"));
	}

	@Test
	public void testGet() throws IOException {
		remote("hello.txt", "Hello World");

		client.get("/hello.txt", localGet("hello.txt"));
		assertTrue(localGet("/hello.txt").isFile());
		assertEquals("Hello World", content(localGet("/hello.txt")));

		client.get("/no.txt", localGet("no.txt"));
		assertTrue(loggerAppender.contains("/no.txt does not exists"));
	}

	@Test
	public void testMgetNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mget(null, null);
	}

	@Test
	public void testMgetEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mget("", null);
	}
	
	@Test
	public void testMgetBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mget("  ", null);
	}
	
	@Test
	public void testMgetLocalNull() throws IOException {
		remoteFolder("new");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_CAN_NOT_BE_NULL);
		client.mget("/new", null);
	}
	
	@Test
	public void testMgetLocalIsFile() throws IOException {
		remoteFolder("new");
		File local = local("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_A_DIRECTORY, local.getAbsolutePath()));
		client.mget("/new", local);
	}
	
	@Test
	public void testMgetLocalAlreadyExistsFile() throws IOException {
		remoteFolder("new", "path");
		remote("/new/path/newfile.txt", "This is new file");
		localFolder("download", "new");
		local("/download/new/path", "This is path file");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_A_DIRECTORY, localGet("/download/new/path").getAbsolutePath()));
		client.mget("/new", localGet("/download/"));
	}

	@Test
	public void testMgetRemoteIsRoot() throws IOException {
		File local = localFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_ROOT_PATH_IS_NOT_ALLOWED);
		client.mget("/", local);
	}
	
	@Test
	public void testMgetRemoteIsFile() throws IOException {
		remote("hello.txt", "Hello World");
		File local = localFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY, "/hello.txt"));
		client.mget("/hello.txt", local);
	}

	@Test
	public void testMget() throws IOException {
		remoteFolder("new", "a", "b", "c");
		remote("/new/a/a.txt", "This is a file");
		remote("/new/a/b/b.txt", "This is b file");
		remote("/new/a/b/c/c.txt", "This is c file");
		File local = localGet("download");

		client.mget("/new", local);

		assertTrue(localGet("/download/new").exists());
		assertTrue(localGet("/download/new").isDirectory());

		assertTrue(localGet("/download/new/a").exists());
		assertTrue(localGet("/download/new/a").isDirectory());

		assertTrue(localGet("/download/new/a/a.txt").exists());
		assertEquals("This is a file", content(localGet("/download/new/a/a.txt")));

		assertTrue(localGet("/download/new/a/b").exists());
		assertTrue(localGet("/download/new/a/b").isDirectory());

		assertTrue(localGet("/download/new/a/b/b.txt").exists());
		assertEquals("This is b file", content(localGet("/download/new/a/b/b.txt")));

		assertTrue(localGet("/download/new/a/b/c").exists());
		assertTrue(localGet("/download/new/a/b/c").isDirectory());

		assertTrue(localGet("/download/new/a/b/c/c.txt").exists());
		assertEquals("This is c file", content(localGet("/download/new/a/b/c/c.txt")));
	}

	@Test
	public void testPutNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.put(null, null);
	}

	@Test
	public void testPutEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.put(null, "");
	}

	@Test
	public void testPutBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.put(null, "  ");
	}
	
	@Test
	public void testPutLocalNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_CAN_NOT_BE_NULL);
		client.put(null, "/new/");
	}

	@Test
	public void testPutLocalNotExists() throws IOException {
		File local = localGet("no.txt");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_EXISTS, local.getAbsolutePath()));
		client.put(local, "/new/");
	}

	@Test
	public void testPutLocalIsDir() throws IOException {
		File local = localFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_A_FILE, local.getAbsolutePath()));
		client.put(local, "/new/");
	}

	@Test
	public void testPutRemotePathIsFile() throws IOException {
		File local = local("newfile.txt", "Hello New File");
		remote("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY, "/hello.txt"));
		client.put(local, "/hello.txt");
	}

	@Test
	public void testPut() throws IOException {
		File local = local("newfile.txt", "Hello New File");
		client.put(local, "/new/");
		assertTrue(remoteGet("/new/newfile.txt").isFile());
		assertEquals("Hello New File", content(remoteGet("/new/newfile.txt")));
	}

	@Test
	public void testMputNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mput(null, null);
	}

	@Test
	public void testMputEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mput(null, "");
	}
	
	@Test
	public void testMputBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.mput(null, "  ");
	}
	
	@Test
	public void testMputLocalNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_CAN_NOT_BE_NULL);
		client.mput(null, "/upload");
	}
	
	@Test
	public void testMputRemoteIsFile() throws IOException {
		remote("upload", "This is upload file");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY, "/upload"));
		client.mput(null, "/upload");
	}

	@Test
	public void testMputRemoteAlreadyExistsFile() throws IOException {
		localFolder("new", "path");
		local("/new/path/newfile.txt", "This is new file");
		remoteFolder("upload", "new");
		remote("/upload/new/path", "This is path file");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY, "/upload/new/path"));
		client.mput(localGet("/new"), "/upload");
	}

	@Test
	public void testMputLocalNotExists() throws IOException {
		File local = localGet("/new/a/b/c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_EXISTS, local.getAbsolutePath()));
		client.mput(local, "/upload");
	}
	
	@Test
	public void testMputLocalIsFile() throws IOException {
		File local = local("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.LOCAL_PATH_MUST_BE_A_DIRECTORY, local.getAbsolutePath()));
		client.mput(local, "/upload");
	}

	@Test
	public void testMput() throws IOException {
		localFolder("new", "a", "b", "c");
		local("/new/a/a.txt", "This is a file");
		local("/new/a/b/b.txt", "This is b file");
		local("/new/a/b/c/c.txt", "This is c file");

		client.mput(localGet("/new"), "/upload");

		assertTrue(remoteGet("/upload/new").exists());
		assertTrue(remoteGet("/upload/new").isDirectory());

		assertTrue(remoteGet("/upload/new/a").exists());
		assertTrue(remoteGet("/upload/new/a").isDirectory());

		assertTrue(remoteGet("/upload/new/a/a.txt").exists());
		assertEquals("This is a file", content(remoteGet("/upload/new/a/a.txt")));

		assertTrue(remoteGet("/upload/new/a/b").exists());
		assertTrue(remoteGet("/upload/new/a/b").isDirectory());

		assertTrue(remoteGet("/upload/new/a/b/b.txt").exists());
		assertEquals("This is b file", content(remoteGet("/upload/new/a/b/b.txt")));

		assertTrue(remoteGet("/upload/new/a/b/c").exists());
		assertTrue(remoteGet("/upload/new/a/b/c").isDirectory());

		assertTrue(remoteGet("/upload/new/a/b/c/c.txt").exists());
		assertEquals("This is c file", content(remoteGet("/upload/new/a/b/c/c.txt")));
	}

	@Test
	public void testRmNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rm(null);
	}

	@Test
	public void testRmEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rm("");
	}

	@Test
	public void testRmBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rm("  ");
	}
	
	@Test
	public void testRmdirNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmdir(null);
	}
	
	@Test
	public void testRmdirEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmdir("");
	}
	
	@Test
	public void testRmdirBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmdir("  ");
	}
	
	@Test
	public void testRmRoot() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
		client.rm("/");
	}

	@Test
	public void testRmdirRoot() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
		client.rmdir("/");
	}
	
	@Test
	public void testRmRemoteIsDir() throws IOException {
		remoteFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_FILE, "/a/b/c"));
		client.rm("/a/b/c");
	}

	@Test
	public void testRmdirRemoteIsFile() throws IOException {
		remote("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(String.format(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY, "/hello.txt"));
		client.rmdir("/hello.txt");
	}

	@Test
	public void testRm() throws IOException {
		remote("hello.txt", "Hello World");

		client.rm("/hello.txt");
		assertFalse(remoteGet("/hello.txt").exists());
		client.rm("/hello.txt");
		assertTrue(loggerAppender.contains("/hello.txt does not exists"));
	}

	@Test
	public void testRmdir() throws IOException {
		remoteFolder("a", "b", "c");

		client.rmdir("/a/b/c");
		assertFalse(remoteGet("/a/b/c").exists());
		assertTrue(remoteGet("/a/b").exists());
		client.rmdir("/a/b/c");
		assertTrue(loggerAppender.contains("/a/b/c does not exists"));
	}

	@Test
	public void testRmRecursiveNull() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmRecursive(null);
	}

	@Test
	public void testRmRecursiveEmpty() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmRecursive("");
	}

	@Test
	public void testRmRecursiveBlank() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_CAN_NOT_BE_NULL_OR_BLANK);
		client.rmRecursive("  ");
	}

	@Test
	public void testRmRecursiveRoot() throws IOException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_ROOT_PATH_CAN_NOT_BE_REMOVED);
		client.rmRecursive("/");
	}

	@Test
	public void testRmRecursiveRemoteNotExists() throws IOException {
		client.rmRecursive("/a/b/c");
		assertTrue(loggerAppender.contains("/a/b/c does not exists"));
	}

	@Test
	public void testRmRecursive() throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		remote("/a/b/c/newfile.txt", "Hello New File");

		assertTrue(remoteGet("/hello.txt").exists());
		assertTrue(remoteGet("/a/b/c/newfile.txt").exists());

		client.rmRecursive("/hello.txt");
		assertFalse(remoteGet("/hello.txt").exists());
		client.rmRecursive("/a");
		assertFalse(remoteGet("/a/b/c/newfile.txt").exists());
		assertFalse(remoteGet("/a").exists());
	}

	@Test
	public void testExistsNull() {
		assertFalse(client.exists(null));
		assertFalse(client.exists(""));
		assertFalse(client.exists("  "));
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

	@Test
	public void testStatNull() {
		assertNull(client.stat(null));
		assertNull(client.stat(""));
		assertNull(client.stat("  "));
	}

	/**
	 * 测试过程中收集Log4j打印的日志内容
	 */
	private static class TestAppender extends AppenderSkeleton {

		private List<String> logs = new ArrayList<String>();

		/**
		 * 日志内容是否包含指定内容
		 * @param log 指定日志内容
		 * @return 如果包含返回true，否则返回false
		 */
		public boolean contains(String log) {
			return logs.contains(log);
		}

		/**
		 * 清空收集的所有日志内容
		 */
		public void clear() {
			logs.clear();
		}

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

	}

}
