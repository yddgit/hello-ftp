package com.my.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.io.BuiltinIoServiceFactoryFactories;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class SftpClientTest extends RemoteClientTest<SshServer, SftpClient> {

	private static final String PRIVATE_KEY_FILE_NAME = "id_rsa";
	private static final String PASSPHRASE = "123456";
	private static final String PUBLIC_KEY_FILE_NAME = "id_rsa.pub";

	public SftpClient keyAuthClient;

	@Test
	public void testStat() throws IOException {
		this.testStat(this.client);
	}

	@Test
	public void testStatByKeyAuthClient() throws IOException, SftpException, JSchException {
		try {
			String privateKey = inputStreamToString(SftpClientTest.class.getClassLoader().getResourceAsStream(PRIVATE_KEY_FILE_NAME));
			this.keyAuthClient = new SftpClient(HOSTNAME, this.localPort, USERNAME, null, privateKey, PASSPHRASE, TIMEOUT, null, null);
			this.testStat(keyAuthClient);
		} finally {
			if(this.keyAuthClient != null) {
				this.keyAuthClient.close();
				this.keyAuthClient = null;
			}
		}
	}

	private void testStat(SftpClient client) throws IOException {
		remote("hello.txt", "Hello World");
		remoteFolder("a", "b", "c");
		LsEntry f = null;
		f = client.stat("/hello.txt");
		assertFalse(f.getAttrs().isDir());
		assertEquals("hello.txt", f.getFilename());
		f = client.stat("/a/b/c");
		assertTrue(f.getAttrs().isDir());
		assertEquals("c", f.getFilename());
		f = client.stat("/");
		assertTrue(f.getAttrs().isDir());
		assertEquals("/", f.getFilename());

		assertNull(client.stat("/no.txt"));
		assertNull(client.stat("/d/e/f"));
	}

	@Override
	public void startServer() throws IOException {
		this.server = SshServer.setUpDefaultServer();
		this.server.setIoServiceFactoryFactory(BuiltinIoServiceFactoryFactories.NETTY.create());
		this.server.setPort(0);
		VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory(this.serverRoot.getRoot().toPath());
		fileSystemFactory.setUserHomeDir(USERNAME, this.serverRoot.getRoot().toPath());
		this.server.setFileSystemFactory(fileSystemFactory);
		this.server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory.Builder().build()));
		this.server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
		userAuthFactories.add(UserAuthPublicKeyFactory.INSTANCE);
		userAuthFactories.add(UserAuthPasswordFactory.INSTANCE);
		this.server.setUserAuthFactories(userAuthFactories);

		this.server.setCommandFactory(new ScpCommandFactory());

		try {
			Path publicKeyFile = Paths.get(SftpClientTest.class.getClassLoader().getResource(PUBLIC_KEY_FILE_NAME).toURI());
			this.server.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(publicKeyFile) {
				@Override
				protected boolean isValidUsername(String username, ServerSession session) {
					return USERNAME.equals(username);
				}
			});
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		this.server.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session)
					throws PasswordChangeRequiredException, AsyncAuthException {
				return USERNAME.equals(username) && PASSWORD.equals(password);
			}
		});
		this.server.start();
		this.localPort = this.server.getPort();
		try {
			this.client = new SftpClient(HOSTNAME, this.localPort, USERNAME, PASSWORD, null, null, TIMEOUT, null, null);
		} catch (JSchException | SftpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void stopServer() throws IOException {
		if(this.client != null) {
			this.client.close();
			this.client = null;
		}
		if(this.server != null) {
			this.server.close();
			this.server = null;
		}
	}

	private String inputStreamToString(InputStream input) throws IOException {
		try(
			InputStream in = input;
			ByteArrayOutputStream output = new ByteArrayOutputStream()
		) {
			byte[] buffer = new byte[1024];
			int len = 0;
			while((len = in.read(buffer)) != -1) {
				output.write(buffer, 0, len);
			}
			return output.toString(StandardCharsets.UTF_8.name());
		}
	}

}
