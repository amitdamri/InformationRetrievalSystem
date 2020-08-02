package Ranker;

import Files.Configurations;
import Stemmer.Stemmer;
import edu.stanford.nlp.pipeline.JSONOutputter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Represents the BM25 algorithm for documents ranking
 */
public class BM25 {

    private static final double K_PARAMETER = 1.3;
    private static final double B_PARAMETER = 0.75;

    private static double avgdl = 0.0;
    private static String postingFilePath = Configurations.getPostingsFilePath();
    private static String isStem;

    private Map<String, int[]> termsDictionary;
    private Map<String, Object[]> docsDictionary;
    private String[] query;
    private LinkedHashMap<String, Double> documentsRank;
    private LinkedHashMap<String, String> requiredTerms; //term and his posting file line that contains documents and numOfOccurrences
    private LinkedHashMap<String, LinkedHashMap<String, Object[]>> relevantDocuments;

    /**
     * Ctor creates bm25 object
     * @param query
     * @param termsDictionary from indexer
     * @param docsDictionary from indexer
     */
    public BM25(String[] query, Map<String, int[]> termsDictionary, Map<String, Object[]> docsDictionary) {

        isStem = Configurations.getStemmingProp() == false ? "WithoutStem" : "Stem";
        documentsRank = new LinkedHashMap<>();
        requiredTerms = new LinkedHashMap<>();
        relevantDocuments = new LinkedHashMap<>();
        this.termsDictionary = termsDictionary;
        this.docsDictionary = docsDictionary;

        //compute average document length according to the number of exclusive terms
        if (avgdl == 0.0) {
            double totalTerms = 0.0;
            for (Map.Entry<String, Object[]> doc : docsDictionary.entrySet()) {
                totalTerms += (Integer) doc.getValue()[2]; //[2] - numOfExclusiveTerms
            }
            avgdl = totalTerms / docsDictionary.size();
        }

        //changes every query
        if (query != null)
            this.query = query;
    }

    /**
     * The algorithm - brings all relevant lines from postings, finds all relevant documents on those lines, and calculates for each doc its query rank
     * @return rank map of docNo and its score for the given query - sorted in descending order
     */
    public LinkedHashMap<String, Double> bm25Algorithm() {

        readAllQueryLinesFromPosting();
        if (requiredTerms.size()>0) {
            getRelevantDocuments();
            rankDocuments();
            //sort ranking list - descending order
            LinkedHashMap sortedRankingList = documentsRank.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            return sortedRankingList;
        }
        return null;
        /*//Threads
        int numOfProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numOfProcessors);

        //executes threads that compute every document rank
        ArrayList<Map.Entry<String, LinkedHashMap<String, Object[]>>> relevantDocumentsArray = new ArrayList<>(relevantDocuments.entrySet());
        BM25 thread;
        int indexOfDocuments = 0;
        while (indexOfDocuments + 500 < relevantDocumentsArray.size()) {
            thread = new BM25(relevantDocumentsArray.subList(indexOfDocuments, indexOfDocuments + 500));
            executorService.execute(thread);
            indexOfDocuments += 500;
        }
        thread = new BM25(relevantDocumentsArray.subList(indexOfDocuments, relevantDocumentsArray.size()));
        executorService.execute(thread);
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }*/
    }

    /**
     * reads all relevant lines from posting and store it in requiredTerms object - lines of the query terms
     */
    private void readAllQueryLinesFromPosting() {
        LinkedHashSet<Integer> sortedLineNumbers = new LinkedHashSet<>();
        String line;
        String[] splitLine;
        int lineNumber = 1;
        for (String term : query) {
            if (termsDictionary.containsKey(term) && !sortedLineNumbers.contains(termsDictionary.get(term)[1]))
                sortedLineNumbers.add(termsDictionary.get(term)[1]); //line number in posting
        }
        TreeSet<Integer> sortedLineNumbersTree = new TreeSet<>(sortedLineNumbers);
        if (sortedLineNumbers.size()>0) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(postingFilePath + "\\Postings\\" + isStem + "\\fullPosting1.txt")));
                while (lineNumber <= sortedLineNumbersTree.last() && (line = br.readLine()) != null) { // last line
                    splitLine = line.split("\\#");
                    if (sortedLineNumbers.contains(lineNumber))
                        requiredTerms.put(splitLine[0], splitLine[1]);
                    ++lineNumber;
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * runs over each requiredTerm line and breaks it to docs and their numOfFrequencies of the specific term in query
     * stores the relevant docs in relevantDocuments object - contains docNo - key, term, termFreq, maxTermFrequency, numOfExclusiveTerm
     */
    private void getRelevantDocuments() {
        String[] docsOfTerm;
        Map.Entry<String, Object[]> doc;
        ArrayList<Map.Entry<String, Object[]>> docs = new ArrayList<>(docsDictionary.entrySet()); //in order to use indexes
        for (Map.Entry<String, String> termLine : requiredTerms.entrySet()) {
            docsOfTerm = termLine.getValue().split("\\?"); //separate docs
            //get docs according to their line that was saved in the posting file
            for (int i = 0; i < docsOfTerm.length; i += 2) {
                doc = docs.get(Integer.parseInt(docsOfTerm[i]) - 1); // sub 1 because starts from 0
                if (!relevantDocuments.containsKey(doc.getKey())) {
                    LinkedHashMap<String, Object[]> termsList = new LinkedHashMap<>();
                    termsList.put(termLine.getKey(), new Object[]{Double.parseDouble(docsOfTerm[i + 1]), doc.getValue()[1], doc.getValue()[2]}); //[0] - termFreq, [1]- maxTerm, [2] numOfExclusive
                    relevantDocuments.put(doc.getKey(), termsList);
                } else {
                    relevantDocuments.get(doc.getKey()).put(termLine.getKey(), new Object[]{Double.parseDouble(docsOfTerm[i + 1]), doc.getValue()[1], doc.getValue()[2]});
                }
            }
        }
    }

    /**
     * clears the data structures
     */
    public void clear() {
        documentsRank.clear();
        requiredTerms.clear();
        relevantDocuments.clear();
    }

    /**
     * ranks the relevant documents according to bm25 formula
     */
    private void rankDocuments() {
        double rank = 0.0;
        double idf = 0, denominator = 0, numerator = 0, termFrequency = 0;
        double maxRank = 0; // maxRank in order to normalize between 0-1
        String[] termDocs;

        for (Map.Entry<String, LinkedHashMap<String, Object[]>> doc : relevantDocuments.entrySet()) { //runs over all relevant documents for the query
            // runs over all the query terms that exists in the doc - object[] contains = [0] = termFrequency,[1]= maxFreq, [2] = numOfExclusive

            for (Map.Entry<String, Object[]> term : doc.getValue().entrySet()) {
                if (termsDictionary.containsKey(term.getKey())) {

                    idf = Math.log((docsDictionary.size() - termsDictionary.get(term.getKey())[0] + 0.5) / (termsDictionary.get(term.getKey())[0] + 0.5)); //[0] docFrequency
                    termFrequency = (Double) term.getValue()[0]; //termFrequency in document
                    //termFrequency = termFrequency / (Integer) term.getValue()[1]; // [1] = maxFreq

                    numerator = termFrequency * (K_PARAMETER + 1);
                    denominator = termFrequency + K_PARAMETER * (1 - B_PARAMETER + B_PARAMETER * ((Integer) term.getValue()[2] / avgdl)); //document[2] = numOfExclusiveTerms in doc

                    if (numerator != 0 && denominator != 0 && idf != 0) {
                        rank += (idf * numerator / denominator);
                        termFrequency = 0;
                    }
                }
            }
            if (maxRank < rank)
                maxRank = rank;
            documentsRank.put(doc.getKey(), rank); // normalize rank
            idf = denominator = numerator = termFrequency = rank = 0;

        }
        //normalize
        for (Map.Entry<String, Double> docRank : documentsRank.entrySet()) {
            // normalize with maxRank and reward for num of equals words between query
            documentsRank.replace(docRank.getKey(), docRank.getValue() / maxRank /*+ relevantDocuments.get(docRank.getKey()).size()/requiredTerms.size()*/);
        }
    }
}

    /*public void getRelevantDocuments() {
        String[] docsOfTerm;
        Map.Entry<String, Object[]> doc;
        ArrayList<Map.Entry<String, Object[]>> docs = new ArrayList<>(docsDictionary.entrySet()); //in order to use indexes
        for (String termLine : requiredTerms.values()) {
            docsOfTerm = termLine.split("\\?"); //separate docs
            //get docs according to their line that was saved in the posting file
            for (int i = 0; i < docsOfTerm.length; i += 2) {
                doc = docs.get(Integer.parseInt(docsOfTerm[i]) - 1); // sub 1 because starts from 0
                if (!relevantDocuments.containsKey(doc.getKey()))
                    relevantDocuments.put(doc.getKey(), doc.getValue());
            }
        }
    }*/

    /* public void check() {
        double rank = 0.0;
        double idf = 0, denominator = 0, numerator = 0, termFrequency = 0;
        String[] termDocs;

        for (Map.Entry<String, LinkedHashMap<String,Object[]>> doc : relevantDocuments.entrySet()) {
            for (String term : query) {
                if (termsDictionary.containsKey(term)) {

                    idf = Math.log((docsDictionary.size() - termsDictionary.get(term)[0] + 0.5) / (termsDictionary.get(term)[0] + 0.5)); //[0] docFrequency
                    termDocs = requiredTerms.get(term).split("\\?"); // separates docs and frequencies
                    for (int i = 0; i < termDocs.length; i = i + 2) {
                        if (termDocs[i].equals(String.valueOf(doc.getValue()[0]))) { //checks if it has the same document line number
                            termFrequency = Double.parseDouble(termDocs[i + 1]); //termFrequency in document
                            termFrequency = termFrequency / (Integer) doc.getValue()[1]; // [1] = maxFreq
                            break;
                        }
                    }
                    numerator = termFrequency * (K_PARAMETER + 1);
                    denominator = termFrequency + K_PARAMETER * (1 - B_PARAMETER + B_PARAMETER * ((Integer) doc.getValue()[2] / avgdl)); //document[2] = numOfExclusiveTerms in doc

                    if (numerator != 0 && denominator != 0 && idf != 0) {
                        rank += (idf * numerator / denominator);
                        termFrequency = 0;
                    }
                }
            }
            documentsRank.put(doc.getKey(), rank);
            idf = denominator = numerator = termFrequency = rank = 0;
        }
    }*/

    /*    @Override
    public void run() {
        double rank = 0.0;
        double idf = 0, denominator = 0, numerator = 0, termFrequency = 0;
        String[] termDocs;

        for (Map.Entry<String, LinkedHashMap<String, Object[]>> doc : document) {
            for (Map.Entry<String, Object[]> term : doc.getValue().entrySet()) {
                if (termsDictionary.containsKey(term.getKey())) {

                    idf = Math.log((docsDictionary.size() - termsDictionary.get(term.getKey())[0] + 0.5) / (termsDictionary.get(term.getKey())[0] + 0.5)); //[0] docFrequency
                    termFrequency = (Double) term.getValue()[0]; //termFrequency in document
                    termFrequency = termFrequency / (Integer) term.getValue()[1]; // [1] = maxFreq

                    numerator = termFrequency * (K_PARAMETER + 1);
                    denominator = termFrequency + K_PARAMETER * (1 - B_PARAMETER + B_PARAMETER * ((Integer) term.getValue()[2] / avgdl)); //document[2] = numOfExclusiveTerms in doc

                    if (numerator != 0 && denominator != 0 && idf != 0) {
                        rank += (idf * numerator / denominator);
                        termFrequency = 0;
                    }
                }
            }
            synchronized (lockRankingList) {
                documentsRank.put(doc.getKey(), rank);
                idf = denominator = numerator = termFrequency = rank = 0;
            }
        }
    }*/

    /*    //Ctor for thread
    private BM25(List<Map.Entry<String, LinkedHashMap<String, Object[]>>> document) {
        this.document = document;
    }*/