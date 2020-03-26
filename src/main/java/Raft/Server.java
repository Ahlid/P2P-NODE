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
    private int MAX_SUPER_PEER_NETWORK_SIZE = 0;

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


                if (state.getStateType() == StateType.CANDIDATE && votes.get() > superPeers.size() / 2) {
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

    public void startLeaderTimer() {

        if (this.leaderTimer != null) {
            this.leaderTimer.cancel();
        }

        TimerTask onLeaderTimeout = new TimerTask() {

            public void sendRPCAppend() {

                if (state.getStateType() != StateType.LEADER) {
                    System.out.println(serverPort + " not leader");
                    startLeaderTimer();
                    return;
                }

                System.out.println(serverPort + " SENDING append RPC ");


                state.setCurrentTerm(state.getCurrentTerm() + 2);
                for (Map.Entry<String, HashMap<String, String>> entry : superPeers.entrySet()) {

                    String key = entry.getKey();
                    HashMap<String, String> value = entry.getValue();
                    int port = Integer.parseInt(value.get("port"));
                    String host = value.get("host"); //todo use host

                    if (port == serverPort) {
                        continue;
                    }

                    new Thread(() -> {
                        try {
                            Registry registry = LocateRegistry.getRegistry(port);
                            ServerRMI stub = (ServerRMI) registry.lookup("Raft.Server" + port);
                            Pair<Long, Boolean> response = stub.appendEntriesRPC(state.getCurrentTerm(), Integer.toString(state.hashCode()), state.getLastApplied(), state.getLog(), state.getCommitIndex());

                            if (!response.getValue()) {
                                state.setStateType(StateType.FOLLOWER);
                            }
                            //   System.out.println(serverPort + " append rpc response: " + response.toString());
                        } catch (Exception e) {
                            System.err.println("Client exception: " + e.toString());
                            e.printStackTrace();
                        }
                    }).start();
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

    public StateType getStateType() {
        return this.state.getStateType();
    }

    public void startServer() {
        startCandidateTimer();
        startLeaderTimer();

        TimerTask log = new TimerTask() {
            @Override
            public void run() {

                System.out.println(state);
                System.out.println(peers);
                System.out.println(superPeers);
                System.out.println(MAX_SUPER_PEER_NETWORK_SIZE);
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

                    Pair<String, HashMap<String, String>> p = this.getSuperPeerProposal();
                    if (p == null && chosenPeer.get() == null) {
                        return;
                    }

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


                } else {
                    //todo: check remove
                    System.out.println("remove check");
                }
            }
        }).start();
    }

    @Override
    public void requestNewPeerEntry(String id, String host, int port, double metric) {
        this.discoverNewPeerController(id, host, port, metric);
    }

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
    public void setSuperPeers(HashMap<String, HashMap<String, String>> superPeers) throws RemoteException {
        synchronized (this.superPeers) {
            this.superPeers.clear();
            this.superPeers.putAll(superPeers);
        }
    }

    public void setMAX_SUPER_PEER_NETWORK_SIZE(int MAX_SUPER_PEER_NETWORK_SIZE) {
        this.MAX_SUPER_PEER_NETWORK_SIZE = MAX_SUPER_PEER_NETWORK_SIZE;
    }

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
}
