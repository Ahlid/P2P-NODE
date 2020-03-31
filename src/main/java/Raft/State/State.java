package Raft.State;

import java.util.ArrayList;

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
public class State {

    /**
     *
     */
    private StateType stateType;

    //persisted state on all servers

    /**
     * latest term server has seen (initialized to 0
     * on first boot, increases monotonically)
     */
    private long currentTerm = Long.MIN_VALUE;

    /**
     * candidateId that received vote in current
     * term (or null if none)
     */
    private String votedFor = null;

    /**
     * log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1)
     */
    private ArrayList<String> log;

    //Volatile state on all servers

    /**
     * index of highest log entry known to be
     * committed (initialized to 0, increases
     * monotonically)
     */
    private long commitIndex = Long.MIN_VALUE;

    /**
     * index of highest log entry applied to state
     * machine (initialized to 0, increases
     * monotonically)
     */
    private long lastApplied = Long.MIN_VALUE;

    //Volatile state on leaders
    /**
     * for each server, index of the next log entry
     * to send to that server (initialized to leader
     * last log index + 1)
     */
    private ArrayList<String> nextIndex;

    /**
     * for each server, index of highest log entry
     * known to be replicated on server
     * (initialized to 0, increases monotonically)
     */
    private ArrayList<String> matchIndex;


    public State() {
        this.log = new ArrayList<String>();
        this.nextIndex = new ArrayList<String>();
        this.matchIndex = new ArrayList<String>();
        this.stateType = StateType.NODE;
    }


    /* Gets & sets*/

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
        return "Raft.State.State{" +
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
