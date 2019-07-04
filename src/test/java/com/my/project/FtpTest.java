package com.my.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import com.my.project.FtpClient;

public class FtpTest extends BaseTest<FtpServer, FtpClient> {

	@Override
	public void startServer() throws IOException {
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();
		factory.setServerAddress(HOSTNAME);
		factory.setPort(0);
		Listener listener = factory.createListener();
		serverFactory.addListener("default", listener);
		serverFactory.setUserManager(new MyUserManager(USERNAME, PASSWORD, serverRoot.getRoot()));
		this.server = serverFactory.createServer();
		try {
			this.server.start();
		} catch (FtpException e) {
			throw new IOException(e);
		}
		this.localPort = listener.getPort();
		this.client = new FtpClient(HOSTNAME, this.localPort, USERNAME, PASSWORD);
	}

	@Override
	public void stopServer() throws IOException {
		if(this.client != null) {
			this.client.close();
			this.client = null;
		}
		if(this.server != null) {
			this.server.stop();
			this.server = null;
		}
	}

	private static class MyUserManager implements UserManager {

		private final User user;

		public MyUserManager(String username, String password, File home) {
			BaseUser u = new BaseUser();
	        u.setName(username);
	        u.setPassword(password);
	        u.setEnabled(true);
	        u.setHomeDirectory(home.toPath().toString());

	        List<Authority> authorities = new ArrayList<Authority>();
	        authorities.add(new WritePermission());
	        authorities.add(new ConcurrentLoginPermission(0, 0));
	        authorities.add(new TransferRatePermission(0, 0));
	        u.setAuthorities(authorities);
	        u.setMaxIdleTime(0);
	        this.user = u;
		}

		@Override
		public User getUserByName(String username) throws FtpException {
			if(user.getName().equals(username)) {
				return user;
			}
			return null;
		}

		@Override
		public String[] getAllUserNames() throws FtpException {
			return new String[] { user.getName() };
		}

		@Override
		public void delete(String username) throws FtpException { }

		@Override
		public void save(User user) throws FtpException { }

		@Override
		public boolean doesExist(String username) throws FtpException {
			return user.getName().equals(username);
		}

		@Override
		public User authenticate(Authentication authentication) throws AuthenticationFailedException {
			if (authentication instanceof UsernamePasswordAuthentication) {
				UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;

				String username = upauth.getUsername();
				String password = upauth.getPassword();

				if (user.getName().equals(username) && user.getPassword().equals(password)) {
					return user;
				} else {
					throw new AuthenticationFailedException("Authentication failed");
				}
			} else if (authentication instanceof AnonymousAuthentication) {

				throw new AuthenticationFailedException("Authentication failed");
			} else {
				throw new IllegalArgumentException("Authentication not supported by this user manager");
			}
		}

		@Override
		public String getAdminName() throws FtpException {
			return user.getName();
		}

		@Override
		public boolean isAdmin(String username) throws FtpException {
			return user.getName().equals(username);
		}

	}

}
