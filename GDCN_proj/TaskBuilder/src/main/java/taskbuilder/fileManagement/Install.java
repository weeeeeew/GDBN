package taskbuilder.fileManagement;

import org.apache.commons.io.IOUtils;
import taskbuilder.ExitFailureException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by HalfLeif on 2014-03-04.
 *
 * Creates directories and files for application data in ~/.gdcn
 */
public class Install {

    private static final String SEPARATOR = File.separator;

    public static final String APPDATA = System.getProperty("user.home") + SEPARATOR + ".gdcn" + SEPARATOR;
    public static final String PATH_DATA = APPDATA + "pathdata.prop";
    public static final String HEADER_NAME = "Header.hs";

    public static final String LIB_DIR = APPDATA + "lib";
    public static final String HDB_DIR = APPDATA + "hdb.conf.d";

    /**
     * Simply runs {@link Install#install()}
     * @param args
     */
    public static void main(String[] args){
        install();
    }

    /**
     * Creates directory for application data. Creates file containing important paths used by application.
     * Must be run from GDCN_proj/ directory.
     */
    public static void install(){
        File rootPath = new File(APPDATA);
        rootPath.mkdirs();

        File pathDataFile = new File(PATH_DATA);

        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(pathDataFile));

            Properties pathData = paths();
            pathData.store(outputStream, " -- Paths for GDCN --");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        installHaskellLibraries();
    }

    /**
     * Deletes entire directory for application data.
     *
     * @return true iff uninstalled correctly
     */
    public static boolean uninstall(){
        File rootPath = new File(APPDATA);
        return deleteContents(rootPath);
    }

    /**
     * Deletes entire directory with contents. Use with care!
     * Is currently used in {@link Install#uninstall()} and {@link PathManager#deleteBinaries()} etc.
     *
     * http://stackoverflow.com/questions/7768071/how-to-delete-folder-content-in-java
     * @param directory Folder to delete
     * @return
     */
    static boolean deleteContents(File directory){
        if(!directory.exists()){
            return false;
        }
        File[] files = directory.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteContents(f);
                } else {
                    f.delete();
                }
            }
        }
        return directory.delete();
    }

    /**
     * Creates Property object containing paths critical for the program to work on tasks
     * @return
     */
    private static Properties paths(){
        Properties props = new Properties();

        String subHeaderPath = "TaskBuilder" + SEPARATOR + "src" + SEPARATOR + "main" + SEPARATOR + "haskell" + SEPARATOR;

        props.put("bin_path", System.getProperty("user.dir") + SEPARATOR + subHeaderPath);
        props.put("data_path", APPDATA + "data" + File.separator);

        return props;
    }

    private static void installHaskellLibraries() {
        String[] dbCmd = {"ghc-pkg", "init", HDB_DIR};
        String[] libCmd = {"runhaskell", "Setup", LIB_DIR, HDB_DIR};

        try {
            Process makeDb = new ProcessBuilder(dbCmd).start();

            if (makeDb.waitFor() != 0) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(makeDb.getErrorStream(), writer, null);
                throw new ExitFailureException(writer.toString());
            }

            //TODO Use BIN_PATH from pathdata.prop
            File buildDir = new File(System.getProperty("user.dir"), "TaskBuilder/resources/gdcn-trusted");
            Process makeLib = new ProcessBuilder(libCmd).directory(buildDir).start();

            if (makeLib.waitFor() != 0) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(makeDb.getErrorStream(), writer, null);
                throw new ExitFailureException(writer.toString());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExitFailureException e) {
            e.printStackTrace();
        }
    }
}
