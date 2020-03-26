package Raft;

import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

public interface ServerMembership extends Remote {

    HashMap<String, HashMap<String, String>> peers = new HashMap<String, HashMap<String, String>>();
    HashMap<String, HashMap<String, String>> superPeers = new HashMap<String, HashMap<String, String>>();
    HashSet<String> blockedPeers = new HashSet<String>();


    void requestNewPeerEntry(String id, String host, int port, double metric) throws RemoteException;

    void discoverNewPeerController(String id, String host, int port, double metric) throws RemoteException;

    double getNewPeerMetric(String id, String host, int port) throws RemoteException;

    HashMap<String, String> addNewPeer(String id, String host, int port, double metric) throws RemoteException;

    HashMap<String, String> removePeer(String id) throws RemoteException;

    HashMap<String, String> addSuperPeer(String id, String host, int port, double metric) throws RemoteException;

    HashMap<String, String> removeSuperPeer(String id) throws RemoteException;

    boolean blockPeer(String id) throws RemoteException;

    boolean unblockPeer(String id) throws RemoteException;

    void setSuperPeers(HashMap<String, HashMap<String, String>> superPeers) throws RemoteException;

    Pair<String, HashMap<String, String>> getSuperPeerProposal() throws RemoteException;
}
