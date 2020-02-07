package Peer;

import Peer.Events.*;
import Raft.Server;
import io.socket.client.Socket;

public abstract class SocketIO implements IMarket {

    protected Socket socket;
    protected int port;
    protected String host;
    protected Server server;

    void onConnect() {
        this.socket.on(Socket.EVENT_CONNECT, new OnConnect(this));
    }

    void onDisconnect() {
        this.socket.on(Socket.EVENT_CONNECT, new OnDisconnect(this));
    }

    void onHello() {
        this.socket.on(PeerEndpoints.HELLO.toString(), new OnHello(this));
    }

    void onMarketPing() {
        this.socket.on(PeerEndpoints.MARKET_PING.toString(), new OnMarketPing(this));
    }

    void onNode() {
        this.socket.on(PeerEndpoints.SET_NODE.toString(), new OnNode(this));
    }

    void onPing() {
        this.socket.on(PeerEndpoints.PING.toString(), new OnPing(this));
    }
    void onNewPeer() {
        this.socket.on(PeerEndpoints.NEW_PEER.toString(), new OnNewPeer(this));
    }

    void onSetLeader() {
        this.socket.on(PeerEndpoints.SET_LEADER.toString(), new OnSetAsLeader(this));
    }

    public Socket getSocket() {
        return socket;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Server getServer() {
        return server;
    }

}
