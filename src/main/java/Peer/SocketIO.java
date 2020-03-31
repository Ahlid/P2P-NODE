package Peer;

import Peer.CommunicationInterfaces.PeerEndpoints;
import Peer.Events.*;
import Raft.Server;
import io.socket.client.Socket;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Abstract Class to make binding os socket.io and market events
 */
public abstract class SocketIO implements IMarket {

    /**
     * The socket io instance that allows us to communicate with the server
     * - Check MarketEndpoints to understand the interfaces of communication with the market
     */
    protected Socket socket;

    /**
     * The port where this Peer should be running the Server
     */
    protected int port;

    /**
     * The host of this Peer
     */
    protected String host;
    /**
     * The port where this Peer should be running the Server
     */
    protected Server server;

    /**
     * Session token, allows to authenticate the Market endpoint calls,
     * market will send this using a PeerEndpoint interface called SEND_SESSION_TOKEN
     */
    protected String token;

    /**
     * The event to be ran when we connect to the server
     */
    void onConnect() {
        this.socket.on(Socket.EVENT_CONNECT, new OnConnect(this));
    }


    /**
     * The event to be ran when we disconnect to the server
     */
    void onDisconnect() {
        this.socket.on(Socket.EVENT_CONNECT, new OnDisconnect(this));
    }


    /**
     * Simple test event
     */
    void onHello() {
        this.socket.on(PeerEndpoints.HELLO.toString(), new OnHello(this));
    }

    /**
     * Event to be ran when the market pings this peer
     */
    void onMarketPing() {
        this.socket.on(PeerEndpoints.MARKET_PING.toString(), new OnMarketPing(this));
    }

    /**
     * Event to be ran when the peer should change to Node state
     */
    void onNode() {
        this.socket.on(PeerEndpoints.SET_NODE.toString(), new OnNode(this));
    }

    /**
     * Event to be ran when it receives a ping (ping pong to keep connection with market)
     */
    void onPing() {
        this.socket.on(PeerEndpoints.PING.toString(), new OnPing(this));
    }

    /**
     * Event to be ran when it should add a new peer to the market
     */
    void onNewPeer() {
        this.socket.on(PeerEndpoints.NEW_PEER.toString(), new OnNewPeer(this));
    }

    /**
     * Event to be ran when market promotes peer to Leader
     */
    void onSetLeader() {
        this.socket.on(PeerEndpoints.SET_LEADER.toString(), new OnSetAsLeader(this));
    }

    /**
     * Bind receive of the session token
     */
    void onSessionToken() {
        this.socket.on(PeerEndpoints.SEND_SESSION_TOKEN.toString(), new OnSessionToken(this));
    }

    /**
     * Bind receive of the session token
     */
    void onNewJobAssignRequest() {
        throw new NotImplementedException();
        //this.socket.on(PeerEndpoints.SEND_SESSION_TOKEN.toString(), new OnNewJobAssignRequest(this));
    }

    /* gets */

    /**
     * Gets the socket instance
     *
     * @return socket Instance of io.socket.client.Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Returns the port where this peer is running
     *
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host string (IP like or localhost) where this peer is running
     *
     * @return host string
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the server that this peer is serving
     *
     * @return server object instance of Raft.Server
     */
    public Server getServer() {
        return server;
    }

}
