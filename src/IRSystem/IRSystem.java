package IRSystem;

import Files.*;
import Indexer.*;
import Parse.Parse;
import Searcher.Searcher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IRSystem {

    /**
     * Start &quot;behind the scene&quot; - i.e. clear directories (if needed), start file
     reading, parsing and indexing
     * @return
     */
    public static Indexer start() {
        startReset();

        ReadFile r = new ReadFile(Configurations.getCorpusPath() + "\\corpus");
        List<CorpusDocument> documentsList = r.getL_corpusDocuments();

        int numOfProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(numOfProcessors);
        SystemThread thread;

        int indexOfDocuments = 0;

        //load stop words
        Parse.createStopWordList(Configurations.getCorpusPath() + "\\stop_words.txt");
        Parse.setStemming();
        while (indexOfDocuments + 47500 < documentsList.size()) {
            thread = new SystemThread(documentsList.subList(indexOfDocuments, indexOfDocuments + 47500));
            exec.execute(thread);
            indexOfDocuments += 47500;
        }
        thread = new SystemThread(documentsList.subList(indexOfDocuments, documentsList.size()));
        exec.execute(thread);
        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            exec.shutdownNow();
        }

        documentsList.clear();
        r.clear();

        MergeFiles m = new MergeFiles();
        m.mergePostings();
        Indexer indexer = new Indexer();
        indexer.buildDocsFromPosting();
        indexer.buildDictionaryFromPosting();

        return indexer;
    }

    /**
     * Clear directories, prepare for program closing
     * @return true when the program closes appropriately
     */
    public static boolean close() {
        resetSystem();
        return true;
    }

    /**
     * Clear directories
     * @return true if the program closes appropriately
     */
    public static boolean resetSystem() {
        try {
            if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings"))) {
                FileUtils.deleteDirectory(new File(Configurations.getPostingsFilePath() + "\\Postings"));
                return true;
            } else
                return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * When re-start program, clear old content and prepare for new running
     */
    private static void startReset() {
        if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings"))) {
            if (Configurations.getStemmingProp()) {
                resetStemFiles();
                resetStemDocs();
            } else {
                resetWithoutStemFiles();
                resetWithoutStemDocs();
            }
        }
    }

    /**
     * When re-start program, clear old content of stem files and prepare for new running
     */
    private static void resetStemDocs() {
        try {
            if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docsStem.txt")))
                FileUtils.forceDelete(new File(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docsStem.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When re-start program, clear old content of without stem files and prepare for new running
     */
    private static void resetWithoutStemDocs() {
        try {
            if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docsWithoutStem.txt")))
                FileUtils.forceDelete(new File(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docsWithoutStem.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When re-start program, clear old content and prepare for new running
     */
    private static void resetStemFiles() {
        try {
            if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings\\Stem")))
                FileUtils.cleanDirectory(new File(Configurations.getPostingsFilePath() + "\\Postings\\Stem"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When re-start program, clear old content and prepare for new running
     */
    private static void resetWithoutStemFiles() {
        try {
            if (Files.exists(Paths.get(Configurations.getPostingsFilePath() + "\\Postings\\WithoutStem")))
                FileUtils.cleanDirectory(new File(Configurations.getPostingsFilePath() + "\\Postings\\WithoutStem"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * runs single query and returns list of documents and their ranks about this query
     * @param indexer
     * @param query
     * @return list of documents ranks
     */
    public static LinkedHashMap<String,Double> runSingleQuery(Indexer indexer, Query query){
        Searcher searcher = new Searcher(indexer);
        LinkedHashMap<String, Double> res = searcher.search(query);
        return res;
    }

    /**
     * runs query File with the help of runSingleQuery function and returns list of documents and their ranks about each query
     * @param indexer
     * @param query
     * @return Map of documents rank while key is queryNum and value contains docNO and Score
     */
    public static Map<String, Map<String, Double>> runQueriesFile(Indexer indexer, File queriesFile) {

        Map<String, Map<String, Double>> results = new LinkedHashMap<>();

        ReadQueriesFile readQueriesFile = new ReadQueriesFile();
        List<Query> queries = readQueriesFile.readQueriesFile(queriesFile);
        for (Query query : queries) {
            Map<String, Double> curr_query_res = new LinkedHashMap<>();
            curr_query_res.putAll(runSingleQuery(indexer, query));
            results.put(query.getNum(), curr_query_res);
        }

        return results;

    }

    /**
     * get entities for specific doc
     * @param indexer
     * @param docno to retrieve for it entities
     * @return Map of entities and their rank
     */
    public static Map<String, Double> documentsEntities(Indexer indexer, String docno) {

        if (!indexer.getDocsDictionary().containsKey(docno))
            return null;

        Searcher searcher = new Searcher(indexer);
        return searcher.getTopFiveEntities(docno);
    }
}
