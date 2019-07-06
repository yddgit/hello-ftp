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

	private final TestAppender loggerAppender = new TestAppender();

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
		exception.expectMessage(RemoteClient.REMOTE_PATH_MUST_BE_A_FILE);
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
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_MUST_BE_EXISTS);
		client.put(localGet("no.txt"), "/new/");
	}

	@Test
	public void testPutLocalIsDir() throws IOException {
		localFolder("a", "b", "c");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.LOCAL_PATH_MUST_BE_A_FILE);
		client.put(localGet("/a/b/c"), "/new/");
	}

	@Test
	public void testPutRemotePathIsFile() throws IOException {
		File local = local("newfile.txt", "Hello New File");
		remote("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY);
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
		exception.expectMessage(RemoteClient.REMOTE_PATH_MUST_BE_A_FILE);
		client.rm("/a/b/c");
	}

	@Test
	public void testRmdirRemoteIsFile() throws IOException {
		remote("hello.txt", "Hello World");
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RemoteClient.REMOTE_PATH_MUST_BE_A_DIRECTORY);
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
