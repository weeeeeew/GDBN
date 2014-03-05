package control;

import command.communicationToUI.ClientInterface;
import taskbuilder.communicationToClient.TaskListener;

import java.io.FileNotFoundException;

/**
 * Created by HalfLeif on 2014-03-05.
 */
public class Downloader extends FileMaster {
    /**
     * Creates FileMaster object that reads meta-file for a task. Run {@link control.FileMaster#runAndAwait()} for
     * solving the dependencies.
     *
     * @param projectName  Name of project
     * @param taskName     Name of task
     * @param client       Client for downloading files from network (DHT)
     * @param taskListener Listener to learn about failures such as unresolved dependencies.
     * @throws java.io.FileNotFoundException if meta-file is not found. Path to search on is derived from projectName and taskName.
     */
    public Downloader(String projectName, String taskName, ClientInterface client, TaskListener taskListener) throws FileNotFoundException, TaskMetaDataException {
        super(projectName, taskName, client, taskListener);
    }
}
