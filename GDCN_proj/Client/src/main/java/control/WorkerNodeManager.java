package control;

import network.WorkerID;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Leif on 2014-03-31.
 *
 * Class to keep track of what peers are registered as workers.
 *
 * //TODO save this object to file on 'exit' and load it from file on startup
 */
public class WorkerNodeManager implements Serializable{

    public static enum DisciplinaryAction{
        REMOVE,
        DEMOTE
    }

    private final DisciplinaryAction standardAction;
    private final int removeSoManyPoints;

    private final Map<WorkerID, Integer> registeredWorkers = new HashMap<>();

    public WorkerNodeManager(DisciplinaryAction standardAction, int removeSoManyPoints) {
        this.standardAction = standardAction;
        if(standardAction==DisciplinaryAction.REMOVE && removeSoManyPoints <= 0){
            throw new IllegalArgumentException("If use REMOVE, one must subtract points, not give points, on failure.");
        }
        this.removeSoManyPoints = removeSoManyPoints;
    }

    /**
     * Registers worker as a worker node to remember
     * @param worker Worker node
     * @return true if worker was added, false if existed already
     */
    public boolean registerWorker(WorkerID worker){
        if(registeredWorkers.keySet().contains(worker)){
            return false;
        }
        registeredWorkers.put(worker, 0);
        return true;
    }

    /**
     *
     * @param worker Worker node
     * @return true if and only if worker is registered
     */
    public boolean isWorkerRegistered(WorkerID worker){
        return registeredWorkers.keySet().contains(worker);
    }

    /**
     * Worker misbehaved. Perhaps it is a Sybil node? Apply standard action.
     * @param worker Worker node
     */
    public void reportWorker(WorkerID worker){
        reportWorker(worker, standardAction);
    }

    /**
     * Worker misbehaved. Perhaps it is a Sybil node?
     * @param worker Worker node
     * @param action Action to take on misbehavior
     */
    public void reportWorker(WorkerID worker, DisciplinaryAction action){
        switch (action){
            case REMOVE:
                registeredWorkers.remove(worker);
                break;
            case DEMOTE:
                Integer reputation = registeredWorkers.get(worker);
                registeredWorkers.put(worker, reputation-removeSoManyPoints);
                break;
        }
    }

    /**
     * Worker finished some task nicely. Promote the worker.
     * @param worker Worker node
     */
    public void promoteWorker(WorkerID worker){
        Integer reputation = registeredWorkers.get(worker);
        registeredWorkers.put(worker, reputation+1);
    }

}