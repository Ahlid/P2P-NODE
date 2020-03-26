package Peer;

import Raft.Server;
import Raft.StateType;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;

public class Peer extends SocketIO {

    private HashMap<String, HashMap<String, String>> networkNodes;
    private String uri;

    public Peer(String uri, String host, int port, Server server) {
        this.uri = uri;
        this.host = host;
        this.port = port;
        this.server = server;
    }

    private void bindInterface() {
        this.onConnect();
        this.onDisconnect();
        this.onHello();
        this.onMarketPing();
        this.onSetLeader();
        this.onPing();
        this.onNode();
        this.onNewPeer();
    }

    public void start() throws URISyntaxException {
        this.socket = IO.socket(this.uri);
        this.bindInterface();
        this.socket.connect();
    }

    public void connected() {
        //this.server.setStateFromMarket(StateType.NODE);
        // this.socket.emit(MarketEndpoints.PONG.toString());
    }

    @Override
    public void ping() {
        this.socket.emit(MarketEndpoints.PONG.toString());
    }

    @Override
    public void setNodeState() {
        this.server.setStateFromMarket(StateType.NODE);
    }

    @Override
    public void newPeer(JSONObject peer) {
        System.out.println(peer);
        String host = (String) peer.get("host");
        String id = (String) peer.get("id");
        int port = (int) peer.get("port");
        double metric = (double) peer.get("metric");
        //this.server.addPeer((Integer) peer.get("port"));//todo implement new peer right
        this.server.requestNewPeerEntry(id, host, port, metric);
    }

    @Override
    public void setLeader(JSONObject data) {
        int MAX_SUPER_PEER_NETWORK_SIZE = data.getInt("superNodeSize");
        if (this.server.getStateType() != StateType.LEADER) {
            this.server.setStateFromMarket(StateType.FOLLOWER);
            this.server.setMAX_SUPER_PEER_NETWORK_SIZE(MAX_SUPER_PEER_NETWORK_SIZE);
        }
        this.socket.emit(MarketEndpoints.SET_LEADER.toString());
    }

    @Override
    public void peerConnection() {
        JSONObject obj2 = new JSONObject();
        obj2.put("port", this.port);
        obj2.put("host", this.host);
        this.socket.emit(MarketEndpoints.PEER_CONNECTION.toString(), obj2);
    }

    @Override
    public void leaderHZ() {
        socket.emit(MarketEndpoints.LEADER_HZ.toString());
    }
}
