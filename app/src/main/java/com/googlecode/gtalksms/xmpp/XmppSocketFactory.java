package com.googlecode.gtalksms.xmpp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class XmppSocketFactory extends SocketFactory {
    private static SocketFactory sDefaultFactory = SocketFactory.getDefault();
    private static XmppSocketFactory sInstance;

    private Socket socket;

    public static XmppSocketFactory getInstance() {
        if (sInstance == null) sInstance = new XmppSocketFactory();
        return sInstance;
    }

    @Override
    public Socket createSocket() throws IOException {
        socket = sDefaultFactory.createSocket();
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
        socket = sDefaultFactory.createSocket(arg0, arg1);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        socket = sDefaultFactory.createSocket(host, port);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        socket = sDefaultFactory.createSocket(host, port, localHost, localPort);
        setSockOpt(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                               int localPort) throws IOException {
        socket = sDefaultFactory.createSocket(address, port, localAddress, localPort);
        setSockOpt(socket);
        return socket;
    }

    private static void setSockOpt(Socket socket) throws IOException {
        socket.setKeepAlive(false);
        // Set the Socket timeout to 120 minutes
        socket.setSoTimeout(120 * 60 * 1000);
        socket.setTcpNoDelay(false);
    }
}
