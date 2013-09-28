package com.googlecode.gtalksms.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;

public class XmppSocketFactory extends SocketFactory {
    private static final SocketFactory defaultFactory = SocketFactory.getDefault();
    
    private Socket socket;    
    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException {
        socket = defaultFactory.createSocket(arg0, arg1);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        socket = defaultFactory.createSocket(host, port);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
            int localPort) throws IOException {
        socket = defaultFactory.createSocket(host, port, localHost, localPort);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        socket = defaultFactory.createSocket(address, port, localAddress, localPort);
        setSockOpt(socket);
        return socket;
    }

    private static void setSockOpt(Socket socket) throws IOException {
        socket.setKeepAlive(false);
        // Set sockek timeout to2 hours, should be more then the ping interval
        // to avoid Exceptions on read()
        socket.setSoTimeout(120*60*100);
        socket.setTcpNoDelay(false);
    }
}
