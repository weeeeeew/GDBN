package files;

import command.communicationToUI.ClientInterface;
import command.communicationToUI.CommandWord;
import net.tomp2p.storage.Data;
import taskbuilder.communicationToClient.TaskListener;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by HalfLeif on 2014-03-05.
 */
public class Uploader extends AbstractFileMaster{
    /**
     * Creates FileMaster object that reads meta-file for a task. Run {@link FileMaster#runAndAwait()} for
     * solving the dependencies.
     *
     * @param projectName  Name of project
     * @param taskName     Name of task
     * @param client       Client for downloading files from network (DHT)
     * @param taskListener Listener to learn about failures such as unresolved dependencies.
     * @throws java.io.FileNotFoundException if meta-file is not found. Path to search on is derived from projectName and taskName.
     */
    public Uploader(String projectName, List<String> taskName, ClientInterface client, TaskListener taskListener) throws FileNotFoundException, TaskMetaDataException {
        super(projectName, taskName, client, taskListener, CommandWord.PUT);
    }

    @Override
    protected void ifFileExist(FileDep fileDep) {
        File file = super.pathTo(fileDep);

        if(!file.exists() || file.isDirectory()){
            //TODO better output?
            System.out.println("Didn't find file " + pathTo(fileDep));
            return;
        }

        try {
            client.put(fileDep.getKey(), new Data(fromFile(file)));
        } catch (IOException e) {
            e.printStackTrace();
            //TODO better output?
            System.out.println("Failed to put " + pathTo(fileDep) + "\n"+e.getMessage());
        }
    }

    @Override
    protected void ifFileDoNotExist(FileDep fileDep) {
        //TODO better output?
        System.out.println("Didn't find file " + pathTo(fileDep));
    }

    @Override
    protected void operationForDependentFileSuccess(FileDep fileDep, Object result) {
        super.fileDependencyResolved(fileDep);
    }

    public static byte[] fromFile(File file) throws IOException {

        InputStream inputStream = null;

        try {
            //TODO Possibly use MD5 instead, if SHA-1 is too slow
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            inputStream = new BufferedInputStream(new FileInputStream(file));
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);

            byte[] data = new byte[(int) file.length()];

            //TODO actually use digest
            digestInputStream.read(data);
            byte[] digestArray = digest.digest();

            return data;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return null;
    }

}
