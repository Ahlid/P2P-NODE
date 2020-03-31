package Peer;

import org.json.JSONObject;

public interface IMarket {
    void setLeader(JSONObject data);

    void peerConnection();

    void leaderHZ();

    void connected();

    void ping();

    void setNodeState();

    void newPeer(JSONObject peer);

    void setToken(JSONObject peer);

    void requestJobAssign(JSONObject peer);
}
