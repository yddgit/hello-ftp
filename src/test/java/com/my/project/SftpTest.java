package com.my.project;

import java.io.IOException;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.io.BuiltinIoServiceFactoryFactories;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class SftpTest extends BaseTest<SshServer, SftpClient> {

	@Override
	public void startServer() throws IOException {
		this.server = SshServer.setUpDefaultServer();
		this.server.setIoServiceFactoryFactory(BuiltinIoServiceFactoryFactories.MINA.create());
		this.server.setPort(0);
		VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory(this.serverRoot.getRoot().toPath());
		fileSystemFactory.setUserHomeDir(USERNAME, this.serverRoot.getRoot().toPath());
		this.server.setFileSystemFactory(fileSystemFactory);
		this.server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory.Builder().build()));
		this.server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
		this.server.setUserAuthFactories(Collections.singletonList(new UserAuthPasswordFactory()));
		this.server.setCommandFactory(new ScpCommandFactory());
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
			this.client = new SftpClient(HOSTNAME, this.localPort, USERNAME, PASSWORD);
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

}
