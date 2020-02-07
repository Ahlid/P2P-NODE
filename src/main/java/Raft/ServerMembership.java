package Raft;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;

public interface ServerMembership {

    HashMap<String, HashMap<String, String>> peers = new HashMap<String, HashMap<String, String>>();
    HashMap<String, HashMap<String, String>> superPeers = new HashMap<String, HashMap<String, String>>();
    HashSet<String> blockedPeers = new HashSet<String>();

    void requestNewPeerEntry(String id, String host, int port, double metric);

    void discoverNewPeerController(String id, String host, int port, double metric);

    double getNewPeerMetric(String id, String host, int port);

    HashMap<String, String> addNewPeer(String id, String host, int port, double metric);

    HashMap<String, String> removePeer(String id);

    HashMap<String, String> addSuperPeer(String id, String host, int port, double metric);

    HashMap<String, String> removeSuperPeer(String id);

    boolean blockPeer(String id);

    boolean unblockPeer(String id);

    Pair<String, HashMap<String, String>> getSuperPeerProposal();
}
