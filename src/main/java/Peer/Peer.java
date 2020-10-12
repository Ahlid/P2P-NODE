package Peer;

import Peer.CommunicationInterfaces.MarketEndpoints;
import Raft.Server;
import Raft.State.StateType;
import io.socket.client.IO;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Peer extends SocketIO {


    private boolean stopped = false;

    public static void main(String[] args) {
        System.out.println("POG");
    }


    private static final long PEER_TIMEOUT = 1000;
    /**
     * peer password
     */
    private String password;

    /**
     * peer username
     */
    private String username;

    private String machineId;
    private String machineToken = null;
    private String token = null;
    /**
     * Market uri to communicate using socket io
     */
    private String uri;
    private Timer peerTimer;


    /**
     * Creates new instance of a Peer that communicates with the market
     *
     * @param uri      - market url
     * @param host     - peer host
     * @param port     - peer port
     * @param server   - peer server
     * @param username - peer username
     * @param password - peer password
     */
    public Peer(String uri, String host, int port, Server server, String username, String password, String machineId) {

        this.uri = uri;
        this.host = host;
        this.port = port;
        this.server = server;
        this.username = username;
        this.password = password;
        this.machineId = machineId;


        this.server.setPeer(this);
    }

    public Peer(String uri , Server server, String host, int port,  String token, String machineToken, String machineId) {

        this.uri = uri;
        this.host = host;
        this.port = port;
        this.server = server;
        this.token = token;
        this.machineToken = machineToken;
        this.machineId = machineId;


        this.server.setPeer(this);
    }

    /**
     * Here we bind the current peer interfaces to the correspondent events
     * - connect
     * - disconnect
     * - hello teste
     * - market ping
     * - market set peer Leader state
     * - on ping (ping pong keep alive)
     * - market set the peer Node state
     * - market send session token after validate username and password
     */
    private void bindInterface() {
        this.onConnect();
        this.onDisconnect();
        this.onHello();
        this.onMarketPing();
        this.onSetLeader();
        this.onPing();
        this.onNode();
        this.onNewPeer();
        this.onSessionToken();
    }

    /**
     * Starts the peer
     *
     * @throws URISyntaxException
     */
    public void start() throws URISyntaxException {
        System.err.println(this.uri);
        this.socket = IO.socket(this.uri); //bind socket
        this.bindInterface();
        this.socket.connect();
        startPeerTimer();
    }

    /**
     * When peer gets connected
     */
    public void connected() {
        //this.server.setStateFromMarket(StateType.NODE);
        // this.socket.emit(MarketEndpoints.PONG.toString());
    }

    /**
     * When market pings, we send pong back
     */
    @Override
    public void ping() {
        this.socket.emit(MarketEndpoints.PONG.toString());
    }

    /**
     * When market sends us to mark Node state, we mark the Raft.Server as a Node
     */
    @Override
    public void setNodeState() {
        this.server.setStateFromMarket(StateType.NODE);
    }

    /**
     * When market asks us to add a new peer to the network
     *
     * @param peer - the new peer info: host, id, port, metric
     */
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

    /**
     * When market authenticates us and send us the auth token
     *
     * @param data
     */
    @Override
    public void setToken(JSONObject data) {
        this.token = data.getString("token");
        this.machineToken = data.getString("machineToken");
    }


    /**
     * When market elects leader
     *
     * @param data - ?
     */
    @Override
    public void setLeader(JSONObject data) {
        int MAX_SUPER_PEER_NETWORK_SIZE = data.getInt("superNodeSize");
        if (this.server.getStateType() != StateType.LEADER) {
            this.server.setStateFromMarket(StateType.FOLLOWER);
            this.server.setMAX_SUPER_PEER_NETWORK_SIZE(MAX_SUPER_PEER_NETWORK_SIZE);
        }
        this.socket.emit(MarketEndpoints.SET_LEADER.toString());
    }

    /**
     * Market authentication username and password
     */
    @Override
    public void peerConnection() {
        JSONObject obj2 = new JSONObject();
        obj2.put("port", this.port);
        obj2.put("host", this.host);
        obj2.put("username", this.username);
        obj2.put("password", this.password);
        obj2.put("machineId", this.machineId);
        obj2.put("token", this.token);
        obj2.put("machineToken", this.machineToken);
        obj2.put("mess", "mess");
        this.socket.emit(MarketEndpoints.PEER_CONNECTION.toString(), obj2);
    }

    /**
     * Market asks leader to send pulse back
     */
    @Override
    public void leaderHZ() {
        socket.emit(MarketEndpoints.LEADER_HZ.toString());
    }

    public void startPeerTimer() {

        if (this.peerTimer != null) {
            this.peerTimer.cancel();
        }

        if (this.stopped) {
            return;
        }

        TimerTask onPeerTimeout = new TimerTask() {

            public void run() {
                List<HashMap<String, String>> configs = server.sendNodeCheck();
                if (configs.size() > 0) {
                    JSONObject response = new JSONObject();
                    JSONArray configsResponse = new JSONArray(configs);
                    response.put("id", machineId);
                    response.put("peersData", configsResponse);
                    socket.emit(MarketEndpoints.SEND_PEERS_STATE.toString(), response);
                }

                startPeerTimer();
            }
        };


        peerTimer = new Timer();
        peerTimer.schedule(onPeerTimeout, PEER_TIMEOUT);

    }

    public String getMachineId() {
        return machineId;
    }

    public void stop() throws URISyntaxException {
        this.socket.disconnect();
        this.socket.close();
        this.server.stop();
        this.server.setPeer(null);
        this.peerTimer.cancel();
        this.server = null;
        this.stopped = true;
    }
}
