package files;

import com.google.gson.Gson;
import command.communicationToUI.CommandWord;
import command.communicationToUI.NetworkInterface;
import command.communicationToUI.OperationFinishedEvent;
import taskbuilder.communicationToClient.TaskListener;
import taskbuilder.fileManagement.PathManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by HalfLeif on 2014-03-04.
 *
 * Abstract class for resolving task file dependencies.
 *
 * Uses TaskListener to report error information.
 */
abstract class AbstractFileMaster{

    protected final TaskMeta taskMeta;
    protected final PathManager pathManager;
    protected final NetworkInterface client;

    private final TaskListener taskListener;
    private final CommandWord expectedOperation;

    private final Lock lock = new ReentrantLock();
    private final Condition allDependenciesComplete = lock.newCondition();
    private final Map<String, FileDep> unresolvedFiles = new HashMap<>();

    private volatile boolean operationFailed = false;
    private volatile boolean stillStartingUp = true;

    /**
     * Creates FileMaster object that reads meta-file for a task. Run {@link AbstractFileMaster#runAndAwait()} for
     * solving the dependencies.
     *
     *
     * @param taskMeta Dependencies to be solved
     * @param client Client for downloading files from network (DHT)
     * @param taskListener Listener to learn about failures such as unresolved dependencies.
     * @param expectedOperation
     * @param pathManager PathManager to correct directory
     * @throws FileNotFoundException if meta-file is not found. Path to search on is derived from projectName and taskName.
     */
    public AbstractFileMaster(TaskMeta taskMeta, NetworkInterface client, TaskListener taskListener,
                              CommandWord expectedOperation, PathManager pathManager) throws TaskMetaDataException {

        this.taskMeta = taskMeta;
        this.client = client;
        this.taskListener = taskListener;
        this.expectedOperation = expectedOperation;
        this.pathManager = pathManager;

        client.addListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt instanceof OperationFinishedEvent) {
                    operationReturned((OperationFinishedEvent) evt);
                }
            }
        });

        //TODO do locally
//            if(! taskName.equals(taskMeta.taskName)){
//                throw new TaskMetaDataException("Must be error in metaFile: taskName doesn't conform with filename!");
//            }

        for(FileDep fileDep : taskMeta.dependencies){
            unresolvedFiles.put(fileDep.key, fileDep);
        }
        if(taskMeta.module != null){
            //is currently null when coming from Uploader class
            unresolvedFiles.put(taskMeta.module.key, taskMeta.module);
        }
    }

    /**
     * Parses file for MetaData of Task
     *
     * @param file Path to meta data file
     * @return Representation of meta data content
     * @throws FileNotFoundException if file isn't found
     */
    protected static TaskMeta readMetaFile(File file) throws FileNotFoundException {

        Reader reader = null;
        try {
            reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)));

            Gson gson = new Gson();
            return gson.fromJson(reader, TaskMeta.class);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Just runs {@link AbstractFileMaster#run()} and {@link AbstractFileMaster#await()}
     * @return result of {@link AbstractFileMaster#await()}
     */
    public boolean runAndAwait() throws TaskMetaDataException {
        run();
        return await();
    }

    /**
     * Attempts to resolve the dependencies found in meta-file.
     */
    private void run() throws TaskMetaDataException {
        if(taskMeta != null){
            resolveDependencies();
        } else {
            throw new TaskMetaDataException("Meta data file wasn't found (or parsed correctly)!");
        }
    }

    /**
     * Blocks current thread until all dependencies for the specified task are resolved.
     *
     * @return true if file has been properly downloaded, false if one of the dependencies couldn't be resolved.
     */
    private boolean await(){
        if(operationFailed){
            // This code is necessary to avoid deadlock if await() is called after a GET operation has failed
            // since there is no guarantee for another signal (it might have been the last file to be resolved)

            System.out.println("Operation failed before enter loop, return FALSE");
            return false;
        }

        while(stillStartingUp || unresolvedFiles.size()>0){
            try {
                lock.lock();
                allDependenciesComplete.await();
                lock.unlock();
            } catch (InterruptedException e) {
                System.out.println("Caught interruption: "+e.getMessage());
                continue;
            }
            if(operationFailed){
                return false;
            }
            System.out.println("Test monitor condition before exit loop...");
        }

        //Alternatively, ignore await() model and use TaskListener instead...
        return true;
    }

    /**
     * Attempt to solve dependecies that was found
     * @throws TaskMetaDataException if dependent File exist locally but is a directory
     */
    private void resolveDependencies() throws TaskMetaDataException {
        //gets ConcurrentModificationException if reads directly from unresolvedFiles.
        Set<FileDep> deps = new HashSet<>(unresolvedFiles.values());

        for(FileDep fileDep : deps){
            File file = pathTo(fileDep);
            if(file.exists()){
                if(file.isDirectory()){
                    throw new TaskMetaDataException("Files in dependencies should not be directories! File: "+file);
                }
                ifFileExist(fileDep);
            } else {
                ifFileDoNotExist(fileDep);
            }
        }

        lock.lock();
        stillStartingUp = false;
        allDependenciesComplete.signalAll();
        lock.unlock();
    }

    /**
     * This file dependency was found locally. What to do?
     *
     * Called in resolve dependencies
     *
     * @param fileDep file
     */
    protected abstract void ifFileExist(FileDep fileDep);

    /**
     * This file dependency wasn't found locally. What to do?
     *
     * Called in resolve dependencies
     *
     * @param fileDep file
     */
    protected abstract void ifFileDoNotExist(FileDep fileDep);

    /**
     * Call to set a file dependency as resolved.
     * @param fileDep file
     */
    protected final void fileDependencyResolved(FileDep fileDep){
        lock.lock();
        unresolvedFiles.remove(fileDep.key);
        allDependenciesComplete.signalAll();
        lock.unlock();
    }

    /**
     * Operation successful with respect to this file
     * @param fileDep file
     * @param result Result of OperationEvent, may be null
     */
    protected abstract void operationForDependentFileSuccess(FileDep fileDep, Object result);

    /**
     * Handles returns of Get operation requested earlier.
     * @param event OperationFinishedEvent
     */
    private void operationReturned(OperationFinishedEvent event) {
        if(event.getCommandWord() != expectedOperation){
            return;
        }

        String key = event.getOperation().getKey();

        if(!unresolvedFiles.containsKey(key)){
            //TODO redirect output?
            System.out.println("FileDep with key ("+key+") wasn't found in Map for task");
            //Might be from other request unrelated with this FileMaster
            return;
        }

        lock.lock();
        FileDep fileDep = unresolvedFiles.remove(key);

        if(event.getOperation().isSuccess()){
            operationForDependentFileSuccess(fileDep, event.getOperation().getResult());

        } else {
            operationFailed = true;
            taskListener.taskFailed(taskMeta.taskName, "Failed to resolve file with name " +  fileDep.fileName);
            allDependenciesComplete.signalAll();
        }

        if(unresolvedFiles.size()==0){
            allDependenciesComplete.signalAll();
        }
        lock.unlock();
    }


    /**
     *
     * @param fileDep file
     * @return Absolute path to file
     */
    protected File pathTo(FileDep fileDep){
        return new File(pathManager.projectDir() + fileDep.location + File.separator + fileDep.fileName);
    }

    /**
     *
     * @param fileDep file
     * @return Absolute path to file
     */
    protected static File pathTo(PathManager pathManager, FileDep fileDep){
        return new File(pathManager.projectDir() + fileDep.location + File.separator + fileDep.fileName);
    }

    /**
     *
     * @return List of paths to all resource files mentioned in taskmetas
     */
    protected List<String> getResourceFiles() {
        List<String> resources = new ArrayList<>();

        for(FileDep fileDep : taskMeta.dependencies){
            resources.add(pathTo(fileDep).getAbsolutePath());
        }

        return resources;
    }

    /**
     * Serialized data-class to Json
     *
     * Represents contents in one MetaTask file
     */
    protected static class TaskMeta implements Serializable{
        private String projectName;
        private String taskName;

        private FileDep module;
        private List<FileDep> dependencies;

        protected TaskMeta(String projectName, String taskName, FileDep module, List<FileDep> dependencies) {
            this.projectName = projectName;
            this.taskName = taskName;
            this.module = module;
            this.dependencies = dependencies;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getTaskName() {
            return taskName;
        }

        public FileDep getModule() {
            return module;
        }

        public List<FileDep> getDependencies() {
            return dependencies;
        }
    }

    /**
     * Serialized data-class to Json
     *
     * Represent one file that has to be resolved for Task to compile or run
     */
    protected static class FileDep implements Serializable{
        private String fileName;
        private String location;
        private String key;

        private boolean sticky = false;
        //TODO put checksum elsewhere
        private int checkSum;

        protected FileDep(String fileName, String location, String key, boolean sticky, int checkSum) {
            this.fileName = fileName;
            this.location = location;
            this.key = key;
            this.sticky = sticky;
            this.checkSum = checkSum;
        }

        @Override
        public int hashCode(){
            return key.hashCode();
        }

        @Override
        public boolean equals(Object o){
            if(o == null){
                return false;
            }
            if(!(o instanceof FileDep)){
                return false;
            }
            return key.equals(((FileDep) o).key);
        }

        public String getFileName() {
            return fileName;
        }

        public String getLocation() {
            return location;
        }

        public String getKey() {
            return key;
        }

        public boolean isSticky() {
            return sticky;
        }

        public int getCheckSum() {
            return checkSum;
        }
    }

    /**
     * Generates a suitable json-String to put in a file, used for debugging
     * @param args
     */
    public static void main(String[] args){
        FileDep rawIndata = new FileDep("2_2000.raw", "resources", "Primes_2_2000", false, 25);
        List<FileDep> deps = new ArrayList<>();
        deps.add(rawIndata);

        FileDep algorithm = new FileDep("Prime.hs", "code", "Primes_algorithms", true, 500);

        TaskMeta taskMetaTest = new TaskMeta("Primes", "PrimeTask_01", algorithm, deps);
        System.out.println( new Gson().toJson(taskMetaTest));
    }
}