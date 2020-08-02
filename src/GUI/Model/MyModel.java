package GUI.Model;


import Files.Configurations;
import Indexer.Indexer;
import IRSystem.IRSystem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;

import Files.Query;

/**
 * The model class - responsible of the connection between the gui and the functionality
 */
public class MyModel extends Observable implements IModel {

    private Indexer indexer;

    /**
     * reset the configuration file
     */
    public MyModel() {
        Configurations.setPostingsFilesPath(System.getProperty("user.dir"));
        Configurations.setCorpusPath(System.getProperty("user.dir"));
        Configurations.setClickStream("false");
        Configurations.setStemming("false");
        Configurations.setSemantic("false");
    }


    /**
     * starts the system - reset the previous files and calls for the start function
     */
    public void startSystem() {
        long start = System.nanoTime();
        if (indexer != null)
            indexer.resetIndexer();
        indexer = IRSystem.start();
        long end = System.nanoTime();
        double total = (end - start) / 1000000000.0;
        double[] indexInfo = new double[3];
        indexInfo[0] = indexer.getDocsDictionary().size(); //number of docs with text in corpus
        indexInfo[1] = indexer.getTermsDictionary().size(); // number of exclusive terms
        indexInfo[2] = total; // total time of the process
        setChanged();
        notifyObservers(indexInfo); //Wave the flag so the observers will notice
    }

    /**
     * close the system
     */
    public void closeSystem() {
        IRSystem.close();
        indexer = null;
    }

    /**
     * loads the dictionary from an existing posting file, and the docs table
     */
    public void loadDictionary() {
        if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings"))) {
            String stem = Configurations.getStemmingProp() == false ? "WithoutStem" : "Stem"; // with stem or without - folder of posting file
            if (new File(Configurations.getPostingsFilePath() + "\\Postings\\" + stem).listFiles().length > 0 &&
                    new File(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docs" + stem + ".txt").exists()) {
                if (indexer != null)
                    indexer.resetIndexer();
                else
                    indexer = new Indexer();
                indexer.buildDocsFromPosting();
                indexer.buildDictionaryFromPosting();
            } else {
                setChanged();
                notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
            }
        } else {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }

    /**
     * returns the dictionary from the indexer in order to show it on the screen
     */
    public void showDictionary() {
        if (indexer != null) {
            setChanged();
            notifyObservers(new Object[]{indexer.getTermsDictionary(), 4}); //Wave the flag so the observers will notice
        } else {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }

    /**
     * resets the system - removes all previous files - stem/without stem files (depends on the stemming checkbox) and docs files
     */
    public void resetSystem() {
        boolean removed = IRSystem.resetSystem();
        indexer = null;
        if (!removed) {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }

    /**
     * sets the corpus path
     *
     * @param systemStage the main stage
     * @param path        of the corpus and stop words file
     */
    public void chooseCorpusPath(Stage systemStage, String path) {
        if (!path.equals("")) {
            Configurations.setCorpusPath(path);
        }
    }

    /**
     * sets the postings file path
     *
     * @param systemStage the main stage
     * @param path        of the postings files path
     */
    public void choosePostingPath(Stage systemStage, String path) {
        if (!path.equals("")) {
            Configurations.setPostingsFilesPath(path);
        }
    }

    /**
     * shows the user browse window in order to choose the corpus path
     *
     * @param systemStage main stage
     */
    public void chooseCorpusPathBrowse(Stage systemStage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Please choose Corpus & Stop words path");
        File defaultDirectory = new File(System.getProperty("user.dir"));
        chooser.setInitialDirectory(defaultDirectory);
        File corpusDir = chooser.showDialog(systemStage);
        if (corpusDir != null) {
            Configurations.setCorpusPath(corpusDir.getAbsolutePath());
            setChanged(); //Raise a flag that I have changed
            Object[] arg = new Object[]{corpusDir, 1};
            notifyObservers(arg); //Wave the flag so the observers will notice
        }
    }


    /**
     * shows the user browse window in order to choose the Postings path
     *
     * @param systemStage main stage
     */
    public void choosePostingPathBrowse(Stage systemStage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Please choose Postings Files path");
        File defaultDirectory = new File(System.getProperty("user.dir"));
        chooser.setInitialDirectory(defaultDirectory);
        File postingsDir = chooser.showDialog(systemStage);
        if (postingsDir != null) {
            Configurations.setPostingsFilesPath(postingsDir.getAbsolutePath());
            setChanged(); //Raise a flag that I have changed
            Object[] arg = new Object[]{postingsDir, 2};
            notifyObservers(arg); //Wave the flag so the observers will notice
        }
    }


    /**
     * sets the stemming option in the configuration file
     *
     * @param value
     */
    public void enableStemming(boolean value) {
        if (value == false)
            Configurations.setStemming("false");
        else
            Configurations.setStemming("true");
    }

    /**
     * sets the queries file path
     *
     * @param systemStage the main stage
     * @param path        of the queries file
     */
    public void chooseQueriesFilePath(Stage systemStage, String path) {
        if (!path.equals("")) {
            Configurations.setQueriesFilePath(path);
        }
    }

    /**
     * shows the user browse window in order to choose the queries file path
     *
     * @param systemStage main stage
     */
    public void chooseQueriesFilePathBrowse(Stage systemStage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        chooser.setTitle("Please choose Queries File");
        File defaultDirectory = new File(System.getProperty("user.dir"));
        chooser.setInitialDirectory(defaultDirectory);
        File queriesFiles = chooser.showOpenDialog(systemStage);
        if (queriesFiles != null) {
            Configurations.setQueriesFilePath(queriesFiles.getAbsolutePath());
            setChanged(); //Raise a flag that I have changed
            Object[] arg = new Object[]{queriesFiles, 3};
            notifyObservers(arg); //Wave the flag so the observers will notice
        }
    }

    /**
     * Get a string query and search for relevant documents
     *
     * @param query - query input from user
     */
    public void insertQuery(String query) {
        if (indexer != null && indexer.getDocsDictionary().size() > 0 && indexer.getTermsDictionary().size() > 0) {
            LinkedHashMap<String, Double> queryRank = IRSystem.runSingleQuery(indexer, new Query(null, query, null, null));
            if (queryRank == null) {
                setChanged();
                notifyObservers("No results for this query.\"");
            } else {
                setChanged();
                notifyObservers(new Object[]{queryRank, 5}); //Wave the flag so the observers will notice
            }
        } else {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }


    /**
     * Get a queries file and search for relevant documents for each query
     *
     * @param queriesFilePath
     */
    public void insertQueryFile(String queriesFilePath) {
        if (indexer != null && indexer.getDocsDictionary().size() > 0 && indexer.getTermsDictionary().size() > 0) {
            File file;
            file = new File(queriesFilePath);
            if (file.exists()) {
                Map<String, Map<String, Double>> queriesRank = IRSystem.runQueriesFile(indexer, file);
                setChanged();
                notifyObservers(new Object[]{queriesRank, 6}); //Wave the flag so the observers will notice
            } else {
                setChanged();
                notifyObservers("Please load a legal path."); //Wave the flag so the observers will notice
            }

        } else {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }

    /**
     * Shows 5 most frequent entities of a given document
     *
     * @param docno
     */
    public void showTop5EntitiesAction(String docno) {
        if (indexer != null && indexer.getDocsDictionary().size() > 0 && indexer.getTermsDictionary().size() > 0) {
            Map<String, Double> top5Entities = IRSystem.documentsEntities(indexer, docno);
            if (top5Entities == null) {
                setChanged();
                notifyObservers("No such document.");
            } else {
                setChanged();
                notifyObservers(new Object[]{top5Entities, 7}); //Wave the flag so the observers will notice
            }
        } else {
            setChanged();
            notifyObservers("No dictionary in memory. Please load a legal dictionary or start the indexer."); //Wave the flag so the observers will notice
        }
    }


    /**
     * sets the Semantic option in the configuration file
     *
     * @param value
     */
    public void enableSemantic(boolean value) {
        if (value == false)
            Configurations.setSemantic("false");
        else
            Configurations.setSemantic("true");
    }

    /**
     * sets the ClickStream option in the configuration file
     *
     * @param value
     */
    public void enableClickStream(boolean value) {
        if (value == false)
            Configurations.setClickStream("false");
        else
            Configurations.setClickStream("true");
    }

    /**
     * sets the query result path and writes all of the docsRank in this file
     *
     * @param systemStage current stage
     * @param queryResult docsRanks for the query
     */
    public void saveQueryResult(Stage systemStage, LinkedHashMap<String, LinkedHashMap<String, Double>> queryResult) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Please choose Queries Results path");
        File defaultDirectory = new File(System.getProperty("user.dir"));
        chooser.setInitialDirectory(defaultDirectory);
        File queriesFilesDir = chooser.showDialog(systemStage);
        if (queriesFilesDir != null) {
            String path = queriesFilesDir.getAbsolutePath();
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path + "\\results.txt"), true));
                for (Map.Entry<String, LinkedHashMap<String, Double>> query : queryResult.entrySet()) {
                    for (Map.Entry<String, Double> docRank : query.getValue().entrySet()) {
                        bw.write(query.getKey() + " 0 " + docRank.getKey() + " 1 " + docRank.getValue() + " mt");
                        bw.newLine();
                    }
                }
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
