package Raft;

import java.util.ArrayList;

public class State {

    private StateType stateType;
    //persisted state on all servers
    private long currentTerm = Long.MIN_VALUE;
    private String votedFor = null;
    private ArrayList<String> log;

    //Volatile state on all servers
    private long commitIndex = Long.MIN_VALUE;
    private long lastApplied = Long.MIN_VALUE;

    //Volatile state on leaders
    private ArrayList<String> nextIndex;
    private ArrayList<String> matchIndex;


    public State() {
        this.log = new ArrayList<String>();
        this.nextIndex = new ArrayList<String>();
        this.matchIndex = new ArrayList<String>();
        this.stateType = StateType.NODE;
    }


    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public ArrayList<String> getLog() {
        return log;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public void incrementCommitIndex() {
        this.commitIndex++;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    public ArrayList<String> getNextIndex() {
        return nextIndex;
    }

    public ArrayList<String> getMatchIndex() {
        return matchIndex;
    }

    public StateType getStateType() {
        return stateType;
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
    }

    @Override
    public synchronized String toString() {
        return "Raft.State{" +
                "stateType=" + stateType +
                ", currentTerm=" + currentTerm +
                ", votedFor='" + votedFor + '\'' +
                ", log=" + log +
                ", commitIndex=" + commitIndex +
                ", lastApplied=" + lastApplied +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                '}';
    }
}
