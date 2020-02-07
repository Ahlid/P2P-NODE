package Raft;

import javafx.util.Pair;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Server implements ServerRMI {

    private State state;
    private final long CANDIDATE_TIMEOUT = 600;
    private final long LEADER_TIMEOUT = 200;
    private Timer candidateTimer;
    private Timer leaderTimer;
    final ArrayList<Integer> portsServers; //todo delete this
    private int serverPort;

    public Server(ArrayList<Integer> portsServers, int port) {
        this.state = new State();
        this.portsServers = portsServers;
        this.serverPort = port;
    }

    @Override
    public Pair<Long, Boolean> appendEntriesRPC(long term, String leaderId, long prevLogIndex, ArrayList<String> entries, long leaderCommit) throws RemoteException {

        System.out.println(serverPort + " received appennd entrie request " + serverPort);
        if (term < this.state.getCurrentTerm()) {
            return new Pair<Long, Boolean>(this.state.getCurrentTerm(), false);
        }
        this.state.setCurrentTerm(term);
        this.state.setStateType(StateType.FOLLOWER);
        this.startCandidateTimer();
        //todo log stuff and commit

        return new Pair<Long, Boolean>(this.state.getCurrentTerm(), true);

    }

    @Override
    public void setStateFromMarket(StateType state) {
        this.state.setStateType(state);
    }

    @Override
    public Pair<Long, Boolean> requestVoteRPC(long term, String candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException {

        System.out.println(serverPort + " received vote request" + serverPort);
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

    public void startCandidateTimer() {

        if (this.candidateTimer != null) {
            this.candidateTimer.cancel();
        }

        TimerTask onCandidateTimeout = new TimerTask() {

            public void sendRPCVote() {
                System.out.println(serverPort + " RPC code");

                AtomicInteger votes = new AtomicInteger(1);
                ArrayList<Thread> threads = new ArrayList<Thread>();

                for (Integer port : portsServers) {

                    if (port == serverPort) {
                        continue;
                    }

                    Thread t = new Thread(() -> {
                        try {
                            System.out.println(serverPort + " Request vote to " + port);
                            Registry registry = LocateRegistry.getRegistry(port);
                            ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + port);
                            Pair<Long, Boolean> response = stub.requestVoteRPC(state.getCurrentTerm(), Integer.toString(state.hashCode()), state.getLastApplied(), state.getCurrentTerm());
                            System.out.println(serverPort + "response: " + response.toString());

                            if (response.getKey() > state.getCurrentTerm()) {
                                state.setCurrentTerm(response.getKey());
                                state.setStateType(StateType.FOLLOWER);
                                startCandidateTimer();
                            }

                            if (response.getValue()) {
                                votes.getAndIncrement();
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


                if (state.getStateType() == StateType.CANDIDATE && votes.get() > portsServers.size() / 2) {
                    state.setStateType(StateType.LEADER);
                    System.out.println(serverPort + " SERVER: " + serverPort + " leader");
                    //   startLeaderTimer();
                }
            }

            public void run() {

                if (state.getStateType() == StateType.NODE) {
                    startCandidateTimer();
                    return;
                }

                if (state.getStateType() == StateType.LEADER) {
                    System.out.println(serverPort + " I'm leader");
                    // startCandidateTimer();
                    return;
                }

                System.out.println(serverPort + " Candidate Timeout");
                state.setCurrentTerm(state.getCurrentTerm() + 1);
                state.setStateType(StateType.CANDIDATE);
                state.setVotedFor(Integer.toString(state.hashCode()));
                startCandidateTimer();
                sendRPCVote();
            }
        };


        candidateTimer = new Timer();
        try {
            candidateTimer.schedule(onCandidateTimeout, CANDIDATE_TIMEOUT + (int) Math.random() * CANDIDATE_TIMEOUT);
        } catch (Exception ignored) {

        }

    }

    public void startLeaderTimer() {

        if (this.leaderTimer != null) {
            this.leaderTimer.cancel();
        }

        TimerTask onLeaderTimeout = new TimerTask() {

            public void sendRPCAppend() {

                if (state.getStateType() == StateType.NODE) {
                    startLeaderTimer();
                    return;
                }

                if (state.getStateType() != StateType.LEADER) {
                    System.out.println(serverPort + " not leader");
                    startLeaderTimer();
                    return;
                }

                System.out.println(serverPort + " SENDING append RPC ");


                state.setCurrentTerm(state.getCurrentTerm() + 1);
                for (Integer port : portsServers) {

                    if (port == serverPort) {
                        continue;
                    }

                    new Thread(() -> {
                        try {
                            Registry registry = LocateRegistry.getRegistry(port);
                            ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + port);
                            Pair<Long, Boolean> response = stub.appendEntriesRPC(state.getCurrentTerm(), Integer.toString(state.hashCode()), state.getLastApplied(), state.getLog(), state.getCommitIndex());
                            System.out.println(serverPort + " append rpc response: " + response.toString());
                        } catch (Exception e) {
                            System.err.println("Client exception: " + e.toString());
                            e.printStackTrace();
                        }
                    }).start();
                }

                startLeaderTimer();
            }

            public void run() {
                sendRPCAppend();
            }
        };


        leaderTimer = new Timer();
        leaderTimer.schedule(onLeaderTimeout, LEADER_TIMEOUT);

    }

    public StateType getStateType() {
        return this.state.getStateType();
    }

    public void addPeer(int port) {
        synchronized (this.portsServers) {
            if (this.portsServers.size() < 3) {
                this.portsServers.add(port);
            }
        }
    }

    public void startServer() {
        startCandidateTimer();
        startLeaderTimer();

        TimerTask log = new TimerTask() {
            @Override
            public void run() {
                System.out.println(state);
            }
        };

        Timer t = new Timer();
        t.schedule(log, 0, 5000);
    }

    private HashMap<String, String> mountPeerInfo(String id, String host, int port, double metric) {
        HashMap<String, String> peerInfo = new HashMap<String, String>();
        peerInfo.put("host", host);
        peerInfo.put("port", String.valueOf(port));
        peerInfo.put("metric", String.valueOf(metric));
        return peerInfo;
    }

    @Override
    public void requestNewPeerEntry(String id, String host, int port, double metric) {
        this.discoverNewPeerController(id, host, port, metric);
    }

    public void discoverNewPeerController(String id, String host, int port, double metric) {

        new Thread(() -> {
            AtomicReference<String> raftChosenPeer = new AtomicReference<>(null);
            double raftChosenMetric = 0;
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
                        if (response > raftChosenMetric) {
                            raftChosenPeer.set(key);
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
            if (thisNodeMetric > raftChosenMetric) {
                this.addNewPeer(id, host, port, metric);
            } else {
                try {
                    HashMap<String, String> value = this.superPeers.get(raftChosenPeer);
                    int raftPeerPort = Integer.parseInt(value.get("port"));
                    String raftPeerHost = value.get("host"); //todo use host
                    Registry registry = LocateRegistry.getRegistry(raftPeerPort);
                    ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + raftPeerPort);
                    double response = stub.getNewPeerMetric(id, host, port);
                } catch (Exception e) {
                    System.err.println("Client exception: " + e.toString());
                    e.printStackTrace();
                    this.addNewPeer(id, host, port, metric);
                }
            }

        }).start();
    }

    @Override
    public double getNewPeerMetric(String id, String host, int port) {
        return Math.random();
    }

    @Override
    public HashMap<String, String> addNewPeer(String id, String host, int port, double metric) {
        return this.peers.put(id, mountPeerInfo(id, host, port, metric));
    }

    @Override
    public HashMap<String, String> removePeer(String id) {
        return this.peers.remove(id);
    }

    @Override
    public HashMap<String, String> addSuperPeer(String id, String host, int port, double metric) {
        return this.superPeers.put(id, mountPeerInfo(id, host, port, metric));
    }

    @Override
    public HashMap<String, String> removeSuperPeer(String id) {
        return this.superPeers.remove(id);
    }

    @Override
    public boolean blockPeer(String id) {
        return this.blockedPeers.add(id);
    }

    @Override
    public boolean unblockPeer(String id) {
        return this.blockedPeers.remove(id);
    }

    @Override
    public Pair<String, HashMap<String, String>> getSuperPeerProposal() {

        Pair<String, HashMap<String, String>> chosenPeer = null;
        double chosenPeerMetricValue = 0;

        for (Map.Entry<String, HashMap<String, String>> entry : this.peers.entrySet()) {
            String key = entry.getKey();
            HashMap<String, String> value = entry.getValue();

            if (this.blockedPeers.contains(key)) {
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
}
