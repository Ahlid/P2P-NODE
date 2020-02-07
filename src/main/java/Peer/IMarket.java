package Peer;

import org.json.JSONObject;

public interface IMarket {
    void setLeader();

    void peerConnection();

    void leaderHZ();

    void connected();

    void ping();

    void setNodeState();

    void newPeer(JSONObject peer);
}
