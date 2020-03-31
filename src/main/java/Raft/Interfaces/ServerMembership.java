package Raft.Interfaces;

import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public interface ServerMembership extends Remote {

    /**
     * List of peers connected to this peer
     */
    HashMap<String, HashMap<String, String>> peers = new HashMap<String, HashMap<String, String>>();

    /**
     * List of super peers that are managing this network
     */
    HashMap<String, HashMap<String, String>> superPeers = new HashMap<String, HashMap<String, String>>();

    /**
     * Track peer fails to communicate
     */
    HashMap<String, Integer> peerFailedCheck = new HashMap<String, Integer>();

    /**
     * Peers that where blocked and can't be super peers anymore during this session
     */
    HashSet<String> blockedPeers = new HashSet<String>();

    /**
     * Max response fails that a peer can have before starting super peer removal
     */
    int MAX_SUPER_PEER_NETWORK_FAILS = 20;

    /**
     * Request to add new peer to the network
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @throws RemoteException RMI
     */
    void requestNewPeerEntry(String id, String host, int port, double metric) throws RemoteException;

    /**
     * Discovers the best super peer to add this peer
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @throws RemoteException RMI
     */
    void discoverNewPeerController(String id, String host, int port, double metric) throws RemoteException;

    /**
     * Returns the metric value for the new peer that this peer has associated with this new peer
     *
     * @param id   - peer id
     * @param host - peer host
     * @param port - peer port
     * @return SPeer metric related to new peer
     * @throws RemoteException RMI
     */
    double getNewPeerMetric(String id, String host, int port) throws RemoteException;

    /**
     * Adds new peer to this Peer management network
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @return map of the peer added
     * @throws RemoteException RMI
     */
    HashMap<String, String> addNewPeer(String id, String host, int port, double metric) throws RemoteException;

    /**
     * Removes peer from this Peer management network
     *
     * @param id - peer id
     * @return map of the peer removed
     * @throws RemoteException RMI
     */
    HashMap<String, String> removePeer(String id) throws RemoteException;

    /**
     * Adds new super peer to this Peer tracking list of super peers
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @return map of the peer added
     * @throws RemoteException RMI
     */
    HashMap<String, String> addSuperPeer(String id, String host, int port, double metric) throws RemoteException;

    /**
     * Removes super peer from this Peer tracking list of super peers
     *
     * @param id - peer id
     * @return map of the peer removed
     * @throws RemoteException RMI
     */
    HashMap<String, String> removeSuperPeer(String id) throws RemoteException;

    /**
     * Blocks peer not allowing to appear on proposed super peer nodes
     *
     * @param id - peer id
     * @return true if blocked, false if not
     * @throws RemoteException RMI
     */
    boolean blockPeer(String id) throws RemoteException;

    /**
     * Unblocks peer allowing to appear on proposed super peer nodes
     *
     * @param id - peer id
     * @return true if unblocked, false if not
     * @throws RemoteException RMI
     */
    boolean unblockPeer(String id) throws RemoteException;

    /**
     * Sets the supper peers hashmap of the currer super peer network
     *
     * @param superPeers current super peer network
     * @throws RemoteException RMI
     */
    void setSuperPeers(HashMap<String, HashMap<String, String>> superPeers) throws RemoteException;

    /**
     * Returns the proposal for a new super nodes based on this peers managed peers that are not blocked
     *
     * @return proposed peer or null if none
     * @throws RemoteException RMI
     */
    Pair<String, HashMap<String, String>> getSuperPeerProposal() throws RemoteException;

}
