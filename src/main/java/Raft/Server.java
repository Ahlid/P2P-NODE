package Raft;

import Peer.Peer;
import Raft.Interfaces.ServerRMI;
import Raft.State.State;
import Raft.State.StateType;
import javafx.util.Pair;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * All Servers:
 * • If commitIndex > lastApplied: increment lastApplied, apply
 * log[lastApplied] to state machine (§5.3)
 * • If RPC request or response contains term T > currentTerm:
 * set currentTerm = T, convert to follower (§5.1)
 * Followers (§5.2):
 * • Respond to RPCs from candidates and leaders
 * • If election timeout elapses without receiving AppendEntries
 * RPC from current leader or granting vote to candidate:
 * convert to candidate
 * Candidates (§5.2):
 * • On conversion to candidate, start election:
 * • Increment currentTerm
 * • Vote for self
 * • Reset election timer
 * • Send RequestVote RPCs to all other servers
 * • If votes received from majority of servers: become leader
 * • If AppendEntries RPC received from new leader: convert to
 * follower
 * • If election timeout elapses: start new election
 * Leaders:
 * • Upon election: send initial empty AppendEntries RPCs
 * (heartbeat) to each server; repeat during idle periods to
 * prevent election timeouts (§5.2)
 * • If command received from client: append entry to local log,
 * respond after entry applied to state machine (§5.3)
 * • If last log index ≥ nextIndex for a follower: send
 * AppendEntries RPC with log entries starting at nextIndex
 * • If successful: update nextIndex and matchIndex for
 * follower (§5.3)
 * • If AppendEntries fails because of log inconsistency:
 * decrement nextIndex and retry (§5.3)
 * • If there exists an N such that N > commitIndex, a majority
 * of matchIndex[i] ≥ N, and log[N].term == currentTerm:
 * set commitIndex = N (§5.3, §5.4).
 */
public class Server implements ServerRMI {

    /**
     * Persistent state on all servers:
     * (Updated on stable storage before responding to RPCs)
     * currentTerm latest term server has seen (initialized to 0
     * on first boot, increases monotonically)
     * votedFor candidateId that received vote in current
     * term (or null if none)
     * log[] log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1)
     * Volatile state on all servers:
     * commitIndex index of highest log entry known to be
     * committed (initialized to 0, increases
     * monotonically)
     * lastApplied index of highest log entry applied to state
     * machine (initialized to 0, increases
     * monotonically)
     * Volatile state on leaders:
     * (Reinitialized after election)
     * nextIndex[] for each server, index of the next log entry
     * to send to that server (initialized to leader
     * last log index + 1)
     * matchIndex[] for each server, index of highest log entry
     * known to be replicated on server
     * (initialized to 0, increases monotonically)
     */
    private State state;

    /**
     * Candidate timeout to start new election
     */
    private final long CANDIDATE_TIMEOUT = 600;

    /**
     * Leader timeout to send append entries RPC
     */
    private final long LEADER_TIMEOUT = 200;

    /**
     * Leader timeout to send append entries RPC
     */
    private final long PEER_TIMEOUT = 5000;

    /**
     * Candidate timer to control new election
     */
    private Timer candidateTimer;

    /**
     * Leader timer to control send append RPC
     */
    private Timer leaderTimer;

    /**
     * Leader timer to control send append RPC
     */
    private Timer peerTimer;

    /**
     * Port where we are running
     */
    private int serverPort;

    /**
     * Logger to debug
     */
    private Logger logger;


    /**
     * Max super network size of the super peers
     */
    int MAX_SUPER_PEER_NETWORK_SIZE = 0;

    /**
     * Current Peer
     */
    private Peer peer;


    /**
     * Creates new instance of the server
     *
     * @param port running
     * @throws IOException socket exception to connect
     */
    public Server(int port) throws IOException {
        this.state = new State(); // create new state
        this.serverPort = port;

        // create logger to file
        this.logger = Logger.getLogger("server-" + port);
        FileHandler fh = new FileHandler("logs/" + port + ".log");
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        logger.setUseParentHandlers(false);
    }

    /**
     * Sets a state received from the market
     *
     * @param state
     */
    @Override
    public void setStateFromMarket(StateType state) {
        this.state.setStateType(state);
    }

    /**
     * Invoked by candidates to gather votes (§5.2).
     * Arguments:
     * term candidate’s term
     * candidateId candidate requesting vote
     * lastLogIndex index of candidate’s last log entry (§5.4)
     * lastLogTerm term of candidate’s last log entry (§5.4)
     * Results:
     * term currentTerm, for candidate to update itself
     * voteGranted true means candidate received vote
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     *
     * @param term         candidate's term
     * @param leaderId     candidate's id
     * @param prevLogIndex index of candidate’s last log entry (§5.4)
     * @param entries      - ?
     * @param leaderCommit - ?
     * @return * term currentTerm, for candidate to update itself
     * * voteGranted true means candidate received vote
     * * Receiver implementation:
     * * 1. Reply false if term < currentTerm (§5.1)
     * * 2. If votedFor is null or candidateId, and candidate’s log is at
     * * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     * @throws RemoteException - RMI exception
     */
    @Override
    public Pair<Long, Boolean> appendEntriesRPC(long term, String leaderId, long prevLogIndex, ArrayList<String> entries, long leaderCommit) throws RemoteException {

        logger.log(Level.INFO, "appendEntriesRPC: Received append entry - " + leaderId);

        if (term < this.state.getCurrentTerm()) {
            logger.log(Level.INFO, "appendEntriesRPC: Returned false because term is lower - " + leaderId);
            return new Pair<Long, Boolean>(this.state.getCurrentTerm(), false);
        }
        this.state.setCurrentTerm(term);
        this.state.setStateType(StateType.FOLLOWER);
        this.startCandidateTimer();

        logger.log(Level.INFO, "appendEntriesRPC: Returned true - " + leaderId);

        return new Pair<Long, Boolean>(this.state.getCurrentTerm(), true);

    }

    /**
     * Invoked by leader to replicate log entries (§5.3); also used as
     * heartbeat (§5.2).
     * Arguments:
     * term leader’s term
     * leaderId so follower can redirect clients
     * prevLogIndex index of log entry immediately preceding
     * new ones
     * prevLogTerm term of prevLogIndex entry
     * entries[] log entries to store (empty for heartbeat;
     * may send more than one for efficiency)
     * leaderCommit leader’s commitIndex
     * Results:
     * term currentTerm, for leader to update itself
     * success true if follower contained entry matching
     * prevLogIndex and prevLogTerm
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     * whose term matches prevLogTerm (§5.3)
     * 3. If an existing entry conflicts with a new one (same index
     * but different terms), delete the existing entry and all that
     * follow it (§5.3)
     * 4. Append any new entries not already in the log
     * 5. If leaderCommit > commitIndex, set commitIndex =
     * min(leaderCommit, index of last new entry)
     *
     * @param term         leader’s term
     * @param candidateId  so follower can redirect clients
     * @param lastLogIndex index of log entry immediately preceding new ones
     * @param lastLogTerm  term of prevLogIndex entry
     * @return 1. Reply false if term < currentTerm (§5.1)
     * * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     * * whose term matches prevLogTerm (§5.3)
     * * 3. If an existing entry conflicts with a new one (same index
     * * but different terms), delete the existing entry and all that
     * * follow it (§5.3)
     * * 4. Append any new entries not already in the log
     * * 5. If leaderCommit > commitIndex, set commitIndex =
     * * min(leaderCommit, index of last new entry)
     * @throws RemoteException
     */
    @Override
    public Pair<Long, Boolean> requestVoteRPC(long term, String candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException {

        System.out.println(serverPort + " received vote request" + candidateId + " term: " + term + " and mine is " + this.state.getCurrentTerm());
        if (term < this.state.getCurrentTerm()) {
            return new Pair<Long, Boolean>(this.state.getCurrentTerm(), false);
        }


        if (term > this.state.getCurrentTerm()) {
            this.state.setVotedFor(null);
            this.state.setCurrentTerm(term);
        }

        this.state.setStateType(StateType.FOLLOWER);
        this.startCandidateTimer();


        //todo: check log
        if (this.state.getVotedFor() == null || this.state.getVotedFor().equals(candidateId)) {
            this.state.setVotedFor(candidateId);
            return new Pair<Long, Boolean>(this.state.getCurrentTerm(), true);
        }

        return new Pair<Long, Boolean>(this.state.getCurrentTerm(), false);
    }

    /**
     * Request to add new peer to the network
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @throws RemoteException RMI
     */
    @Override
    public void requestNewPeerEntry(String id, String host, int port, double metric) {
        this.discoverNewPeerController(id, host, port, metric);
    }

    /**
     * Discovers the best super peer to add this peer
     *
     * @param id     - peer id
     * @param host   - peer host
     * @param port   - peer port
     * @param metric - peer metric
     * @throws RemoteException RMI
     */
    public void discoverNewPeerController(String id, String host, int port, double metric) {

        new Thread(() -> {
            AtomicReference<String> raftChosenPeer = new AtomicReference<>(null);
            AtomicReference<Double> raftChosenMetric = new AtomicReference<>((double) 0);
            ArrayList<Thread> threads = new ArrayList<Thread>();


            for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                String key = entry.getKey();
                HashMap<String, String> value = entry.getValue();
                int raftPeerPort = Integer.parseInt(value.get("port"));
                String raftPeerHost = value.get("host"); //todo use host

                Thread t = new Thread(() -> {
                    try {
                        Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                        ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                        double response = stub.getNewPeerMetric(id, host, port);
                        System.out.println(serverPort + " discover: " + response);
                        if (response > raftChosenMetric.get()) {
                            raftChosenPeer.set(key);
                            raftChosenMetric.set(response);
                        }

                    } catch (Exception e) {
                        System.err.println("Client exception: " + e.toString());
                        e.printStackTrace();
                    }
                });

                threads.add(t);
                t.start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }


            double thisNodeMetric = this.getNewPeerMetric(id, host, port);
            if (thisNodeMetric > raftChosenMetric.get() || raftChosenPeer.get() == null) {
                System.out.println("NEW PEER ADDED TO ME");
                this.addNewPeer(id, host, port, metric);
            } else {
                try {
                    HashMap<String, String> value = this.superPeers.get(raftChosenPeer.get());
                    int raftPeerPort = Integer.parseInt(value.get("port"));
                    String raftPeerHost = value.get("host"); //todo use host
                    Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                    ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                    HashMap<String, String> response = stub.addNewPeer(id, host, port, metric);
                    System.out.println("NEW PEER ADDED TO OTHER NODE");
                } catch (Exception e) {
                    System.err.println("Client exception: " + e.toString());
                    e.printStackTrace();
                    this.addNewPeer(id, host, port, metric);
                }
            }

        }).start();
    }

    /**
     * Returns the metric value for the new peer that this peer has associated with this new peer
     *
     * @param id   - peer id
     * @param host - peer host
     * @param port - peer port
     * @return SPeer metric related to new peer
     * @throws RemoteException RMI
     */
    @Override
    public double getNewPeerMetric(String id, String host, int port) {
        return Math.random();
    }

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
    @Override
    public HashMap<String, String> addNewPeer(String id, String host, int port, double metric) {
        return this.peers.put(id, mountPeerInfo(id, host, port, metric));
    }

    /**
     * Removes peer from this Peer management network
     *
     * @param id - peer id
     * @return map of the peer removed
     * @throws RemoteException RMI
     */
    @Override
    public HashMap<String, String> removePeer(String id) {
        return this.peers.remove(id);
    }

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
    @Override
    public HashMap<String, String> addSuperPeer(String id, String host, int port, double metric) {
        return this.superPeers.put(id, mountPeerInfo(id, host, port, metric));
    }

    /**
     * Removes super peer from this Peer tracking list of super peers
     *
     * @param id - peer id
     * @return map of the peer removed
     * @throws RemoteException RMI
     */
    @Override
    public HashMap<String, String> removeSuperPeer(String id) {
        return this.superPeers.remove(id);
    }

    /**
     * Blocks peer not allowing to appear on proposed super peer nodes
     *
     * @param id - peer id
     * @return true if blocked, false if not
     * @throws RemoteException RMI
     */
    @Override
    public boolean blockPeer(String id) {
        return this.blockedPeers.add(id);
    }

    /**
     * Unblocks peer allowing to appear on proposed super peer nodes
     *
     * @param id - peer id
     * @return true if unblocked, false if not
     * @throws RemoteException RMI
     */
    @Override
    public boolean unblockPeer(String id) {
        return this.blockedPeers.remove(id);
    }

    /**
     * Sets the supper peers hashmap of the currer super peer network
     *
     * @param superPeers current super peer network
     * @throws RemoteException RMI
     */
    @Override
    public void setSuperPeers(HashMap<String, HashMap<String, String>> superPeers) throws RemoteException {
        synchronized (this.superPeers) {
            this.superPeers.clear();
            this.superPeers.putAll(superPeers);
        }
    }

    /**
     * Returns the proposal for a new super nodes based on this peers managed peers that are not blocked
     *
     * @return proposed peer or null if none
     * @throws RemoteException RMI
     */
    @Override
    public Pair<String, HashMap<String, String>> getSuperPeerProposal() {

        Pair<String, HashMap<String, String>> chosenPeer = null;
        double chosenPeerMetricValue = 0;

        for (Map.Entry<String, HashMap<String, String>> entry : this.peers.entrySet()) {
            String key = entry.getKey();
            HashMap<String, String> value = entry.getValue();

            if (this.blockedPeers.contains(key) || this.superPeers.containsKey(key)) {
                continue;
            }

            double metric = Double.parseDouble(value.get("metric"));
            if (metric > chosenPeerMetricValue) {
                chosenPeerMetricValue = metric;
                chosenPeer = new Pair<String, HashMap<String, String>>(key, value);
            }
        }

        return chosenPeer;
    }

    public List<HashMap<String, String>> sendNodeCheck() {


        List<HashMap<String, String>> configs = new ArrayList<HashMap<String, String>>();
        List<Thread> threads = new ArrayList<>();

        for (Map.Entry<String, HashMap<String, String>> entry : this.peers.entrySet()) {


            String key = entry.getKey();
            HashMap<String, String> value = entry.getValue();
            int raftPeerPort = Integer.parseInt(value.get("port"));
            String raftPeerHost = value.get("host"); //todo use host

            Thread t = new Thread(() -> {
                try {
                    Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                    ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                    configs.add(stub.requestNodeCheck());
                } catch (Exception e) {
                    System.err.println("Client exception: " + e.toString());
                    e.printStackTrace();
                }
            });

            threads.add(t);
            t.start();

        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }

        return configs;
    }

    @Override
    public HashMap<String, String> requestNodeCheck() throws RemoteException {

        startPeerTimer();

        Random r = new Random();
        HashMap<String, String> config = new HashMap();

        config.put("host", "localhost");
        config.put("port", this.serverPort + "");
        config.put("credits", r.nextInt(50) + "");
        config.put("cpu", r.nextInt(1000000000) + "");
        config.put("ram", r.nextInt(26400000) + "");
        config.put("disk", r.nextInt(26400000) + "");

        return config;
    }

    /**
     * Returns the current state type
     *
     * @return State type, one of the enum @StateType
     */
    public StateType getStateType() {
        return this.state.getStateType();
    }

    /**
     * Sets the mas super peer network size received from the marked
     *
     * @param MAX_SUPER_PEER_NETWORK_SIZE
     */
    public void setMAX_SUPER_PEER_NETWORK_SIZE(int MAX_SUPER_PEER_NETWORK_SIZE) {
        this.MAX_SUPER_PEER_NETWORK_SIZE = MAX_SUPER_PEER_NETWORK_SIZE;
    }

    /**
     * Creates the timer for the candidate election timeout
     */
    public void startCandidateTimer() {

        if (this.candidateTimer != null) {
            this.candidateTimer.cancel();
        }

        Random ran = new Random();

        TimerTask onCandidateTimeout = new TimerTask() {

            public void sendRPCVote() {
                System.out.println(serverPort + " RPC code");

                AtomicInteger votes = new AtomicInteger(1);
                ArrayList<Thread> threads = new ArrayList<Thread>();

                for (Map.Entry<String, HashMap<String, String>> entry : superPeers.entrySet()) {

                    String key = entry.getKey();
                    HashMap<String, String> value = entry.getValue();
                    int port = Integer.parseInt(value.get("port"));
                    String host = value.get("host"); //todo use host

                    if (port == serverPort) {
                        continue;
                    }

                    Thread t = sendRequestForVote(key, port, host, votes);
                    threads.add(t);
                    t.start();

                }

                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }


                if (state.getStateType() == StateType.CANDIDATE && votes.get() > superPeers.size() / 2) {
                    state.setStateType(StateType.LEADER);
                    logger.log(Level.SEVERE, serverPort + " SERVER: " + serverPort + " leader");
                    //todo: notify server?
                }
            }

            public void run() {

                if (state.getStateType() == StateType.NODE) {
                    startCandidateTimer();
                    return;
                }

                if (state.getStateType() == StateType.LEADER) {
                    // System.out.println(serverPort + " I'm leader");
                    // startCandidateTimer();
                    return;
                }

                logger.log(Level.WARNING, " Candidate Timeout: " + serverPort);
                state.setCurrentTerm(state.getCurrentTerm() + ran.nextInt(2));
                state.setStateType(StateType.CANDIDATE);
                state.setVotedFor(Integer.toString(state.hashCode()));
                startCandidateTimer();
                sendRPCVote();
            }
        };


        candidateTimer = new Timer();
        try {
            candidateTimer.schedule(onCandidateTimeout, CANDIDATE_TIMEOUT + (long) (Math.random() * (CANDIDATE_TIMEOUT)));
        } catch (Exception ignored) {

        }

    }

    /**
     * Creates the timer for the leader append entries RPC
     */
    public void startLeaderTimer() {

        if (this.leaderTimer != null) {
            this.leaderTimer.cancel();
        }

        TimerTask onLeaderTimeout = new TimerTask() {

            public void sendRPCAppend() {

                if (state.getStateType() != StateType.LEADER) {
                    startLeaderTimer();
                    return;
                }

                logger.log(Level.INFO, "sendRPCAppend: SENDING append RPC ");

                state.setCurrentTerm(state.getCurrentTerm() + 2);
                for (Map.Entry<String, HashMap<String, String>> entry : superPeers.entrySet()) {

                    String key = entry.getKey();
                    HashMap<String, String> value = entry.getValue();
                    int port = Integer.parseInt(value.get("port"));
                    String host = value.get("host"); //todo use host

                    if (port == serverPort) {
                        continue;
                    }

                    sendAppendEntries(key, port, host).start();
                }

                startLeaderTimer();
                checkMembership();
            }

            public void run() {
                sendRPCAppend();
            }
        };


        leaderTimer = new Timer();
        leaderTimer.schedule(onLeaderTimeout, LEADER_TIMEOUT);

    }

    public void startPeerTimer() {

        if (this.peerTimer != null) {
            this.peerTimer.cancel();
        }

        TimerTask onPeerTimeout = new TimerTask() {

            public void run() {
                try {
                    if (state.getStateType() == StateType.NODE) {
                        restart();
                    }
                    startPeerTimer();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    startPeerTimer();
                }
            }
        };


        peerTimer = new Timer();
        peerTimer.schedule(onPeerTimeout, PEER_TIMEOUT, PEER_TIMEOUT);

    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }


    public void restart() throws URISyntaxException {
        this.state = new State();
        this.peers.clear();
        this.superPeers.clear();
        this.blockedPeers.clear();
        this.peerFailedCheck.clear();
        this.peer.start();
        this.startServer();
    }

    /**
     * Starts the server
     */
    public void startServer() {
        startCandidateTimer();
        startLeaderTimer();
        startPeerTimer();

        TimerTask log = new TimerTask() {
            @Override
            public void run() {

                System.out.println("=========== STATE ===========");
                System.out.println(state);
                System.out.println(peers);
                System.out.println(superPeers);
                System.out.println(blockedPeers);
                System.out.println(peerFailedCheck);
                System.out.println(MAX_SUPER_PEER_NETWORK_SIZE);
                System.out.println("=========== STATE ===========");
            }
        };

        Timer t = new Timer();
        t.schedule(log, 0, 5000);
    }

    /**
     * Check the membership TODO: send this to the management peer interface
     */
    public void checkMembership() {

        AtomicReference<Pair<String, HashMap<String, String>>> chosenPeer = new AtomicReference<>(null);
        AtomicReference<Double> chosenPeerMetric = new AtomicReference<>((double) 0);
        ArrayList<Thread> threads = new ArrayList<Thread>();

        new Thread(() -> {
            synchronized (peers) {
                if (superPeers.size() < MAX_SUPER_PEER_NETWORK_SIZE) {
                    for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                        String key = entry.getKey();
                        HashMap<String, String> value = entry.getValue();
                        int raftPeerPort = Integer.parseInt(value.get("port"));
                        String raftPeerHost = value.get("host"); //todo use host

                        Thread t = new Thread(() -> {
                            try {
                                Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                                ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                                Pair<String, HashMap<String, String>> response = stub.getSuperPeerProposal();

                                if (response == null) {
                                    return;
                                }

                                System.out.println("PROPOSED: " + response.getKey());
                                double metric = Double.parseDouble(response.getValue().get("metric"));

                                if (metric > chosenPeerMetric.get()) {
                                    chosenPeer.set(response);
                                    chosenPeerMetric.set(metric);
                                }

                            } catch (Exception e) {
                            }
                        });

                        threads.add(t);
                        t.start();
                    }

                    for (Thread thread : threads) {
                        try {
                            thread.join();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    Pair<String, HashMap<String, String>> p = this.getSuperPeerProposal();
                    if (!(p == null && chosenPeer.get() == null)) {


                        if ((chosenPeer.get() == null) || (p != null && chosenPeerMetric.get() < Double.parseDouble(p.getValue().get("metric")))) {
                            String host = (String) p.getValue().get("host");
                            String id = (String) p.getKey();
                            int port = Integer.parseInt(p.getValue().get("port"));
                            double metric = Double.parseDouble(p.getValue().get("metric"));
                            this.addSuperPeer(id, host, port, metric);

                            for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                                String key = entry.getKey();
                                HashMap<String, String> value = entry.getValue();
                                int raftPeerPort = Integer.parseInt(value.get("port"));
                                String raftPeerHost = value.get("host");

                                new Thread(() -> {

                                    try {
                                        Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                                        ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                                        HashMap<String, String> response = stub.addSuperPeer(id, host, port, metric);


                                    } catch (Exception e) {
                                        System.err.println("Client exception: " + e.toString());
                                        e.printStackTrace();
                                    }
                                }).run();

                            }

                        } else {
                            String host = (String) chosenPeer.get().getValue().get("host");
                            String id = (String) chosenPeer.get().getKey();
                            int port = Integer.parseInt(chosenPeer.get().getValue().get("port"));
                            double metric = Double.parseDouble(chosenPeer.get().getValue().get("metric"));
                            this.addSuperPeer(id, host, port, metric);

                            for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                                String key = entry.getKey();
                                HashMap<String, String> value = entry.getValue();
                                int raftPeerPort = Integer.parseInt(value.get("port"));
                                String raftPeerHost = value.get("host");

                                new Thread(() -> {

                                    try {
                                        Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                                        ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                                        HashMap<String, String> response = stub.addSuperPeer(id, host, port, metric);
                                    } catch (Exception e) {
                                        System.err.println("Client exception: " + e.toString());
                                        e.printStackTrace();
                                    }
                                }).run();
                            }
                        }

                        for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                            String key = entry.getKey();
                            HashMap<String, String> value = entry.getValue();
                            int raftPeerPort = Integer.parseInt(value.get("port"));
                            String raftPeerHost = value.get("host");

                            new Thread(() -> {

                                try {
                                    Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                                    ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                                    stub.setSuperPeers(superPeers);


                                } catch (Exception e) {
                                    System.err.println("Client exception: " + e.toString());
                                    e.printStackTrace();
                                }
                            }).run();

                        }
                    }


                }


                for (Map.Entry<String, Integer> node : peerFailedCheck.entrySet()) {
                    String id = node.getKey();
                    Integer numberOfFails = node.getValue();
                    System.out.println(numberOfFails);

                    //did pass the limit
                    if (numberOfFails > this.MAX_SUPER_PEER_NETWORK_FAILS) {

                        removeSuperPeer(id);
                        blockPeer(id);

                        for (Map.Entry<String, HashMap<String, String>> entry : this.superPeers.entrySet()) {

                            String key = entry.getKey();
                            HashMap<String, String> value = entry.getValue();
                            int raftPeerPort = Integer.parseInt(value.get("port"));
                            String raftPeerHost = value.get("host");

                            if (key == id)
                                continue;

                            new Thread(() -> {

                                try {
                                    ServerRMI stub = getRemoteServer(raftPeerHost, raftPeerPort);
                                    stub.removeSuperPeer(id);
                                    stub.blockPeer(id);


                                } catch (Exception e) {
                                    System.err.println("Client exception: " + e.toString());
                                    e.printStackTrace();
                                }
                            }).run();

                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Mounts the info object of the peer
     *
     * @param id     peer id
     * @param host   peer host
     * @param port   peer port
     * @param metric peer metric
     * @return hashmap of the info
     */
    private HashMap<String, String> mountPeerInfo(String id, String host, int port, double metric) {
        HashMap<String, String> peerInfo = new HashMap<String, String>();
        peerInfo.put("host", host);
        peerInfo.put("port", String.valueOf(port));
        peerInfo.put("metric", String.valueOf(metric));
        return peerInfo;
    }


    //thread runs

    /**
     * Gets the remover peer Server object tho invoke RMI calls
     *
     * @param host peer host
     * @param port peer port
     * @return ServerRMI instance
     * @throws RemoteException   RMI
     * @throws NotBoundException RMI
     */
    private ServerRMI getRemoteServer(String host, int port) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(port);
        ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + port);
        return stub;
    }

    /**
     * Thread that sends the append entries of the leader to the peer
     *
     * @param id   - peer id
     * @param port - peer port
     * @param host - peer host
     * @return Thread ready to send append entries
     */
    private Thread sendAppendEntries(String id, int port, String host) {
        return new Thread(() -> {
            try {

                ServerRMI remoteServer = this.getRemoteServer(host, port);
                Pair<Long, Boolean> response = remoteServer.appendEntriesRPC(state.getCurrentTerm(), Integer.toString(state.hashCode()), state.getLastApplied(), state.getLog(), state.getCommitIndex());

                if (!response.getValue()) {
                    state.setStateType(StateType.FOLLOWER);
                }

                //reset failed answers
                peerFailedCheck.put(id, 0);

            } catch (Exception e) {

                //add to failed
                int previousValue = 0;
                if (peerFailedCheck.containsKey(id)) {
                    previousValue = peerFailedCheck.get(id);
                }
                peerFailedCheck.put(id, 1 + previousValue);

            }
        });
    }

    /**
     * Thread that sends the request for votes of the candidade to the peer
     *
     * @param id    - peer id
     * @param port  - peer port
     * @param host  - peer host
     * @param votes - atomic integer reference of the current received votes
     * @return Thread ready to send request for vote
     */
    private Thread sendRequestForVote(String id, int port, String host, AtomicInteger votes) {
        return new Thread(() -> {
            try {
                logger.log(Level.WARNING, " Request vote to " + port);
                ServerRMI server = getRemoteServer(host, port);
                Pair<Long, Boolean> response = server.requestVoteRPC(state.getCurrentTerm(), Integer.toString(state.hashCode()), state.getLastApplied(), state.getCurrentTerm());
                logger.log(Level.WARNING, "response from " + port + ": " + response.toString());

                if (response.getKey() > state.getCurrentTerm()) {
                    logger.log(Level.WARNING, "back to follower because term received was higher");
                    state.setCurrentTerm(response.getKey());
                    state.setStateType(StateType.FOLLOWER);
                    startCandidateTimer();
                }

                if (response.getValue()) {
                    votes.getAndIncrement();
                    logger.log(Level.WARNING, "Got vote, current votes: " + votes);
                }

            } catch (Exception e) {
                logger.log(Level.INFO, "Node " + port + " not reachable.");
            }
        });
    }


}
