package com.my.project;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, SftpException, JSchException {
        if(args == null || args.length < 7) {
            System.out.println("Usage: java -jar ftp.jar" +
                    " --type=FTP" +
                    " --host=example.com" +
                    " --port=21" +
                    " --user=user1" +
                    " --pass=user1pass" +
                    " --remotePath=/" +
                    " --op=ls" +
                    " --proxy=example.proxy.com" +
                    " --proxyPort=8080");
            return;
        }

        String type = args[0].replace("--type=", "").toUpperCase();
        String host = args[1].replace("--host=", "");
        Integer port = Integer.parseInt(args[2].replace("--port=", ""));
        String user = args[3].replace("--user=", "");
        String pass = args[4].replace("--pass=", "");
        String remotePath = args[5].replace("--remotePath=", "");
        String op = args[6].replace("--op=", "");
        String proxy = args.length >= 8 ? args[7].replace("--proxy=", "") : null;
        Integer proxyPort = args.length >= 8 ? Integer.parseInt(args[8].replace("--proxyPort=", "")) : null;

        RemoteClient client = null;
        if("FTP".equals(type.toUpperCase())) {
            client = new FtpClient(host, port, user, pass, 60000, proxy, proxyPort);
        } else if("SFTP".equals(type.toUpperCase())) {
            client = new SftpClient(host, port, user, pass, null, null, 60000, proxy, proxyPort);
        }

        if(client != null) {
            if("ls".equals(op)) {
                for (Object o : client.ls(remotePath)) {
                    System.out.println(o);
                }
            }
            if("download".equals(op)) {
                client.mget(remotePath, new File("download-test"));
            }
            client.close();
        }
    }
}
