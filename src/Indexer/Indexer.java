package Indexer;

import Files.CorpusDocument;
import Files.Configurations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class Indexer {

    private Map<String, int[]> termsDictionary;
    private Map<String, Object[]> docsDictionary;
    private static final Object lock = new Object();
    private static Integer postingIndex = 0;
    private static Integer docLineNumber = 1;
    private int index;
    protected static String isStem;
    private static String postingFilePath = Configurations.getPostingsFilePath();


    /**
     * Constructor - initialize required data structures (docs list and dictionary)
     * Check if stemming is required by user&#39;s click in GUI
     */
    public Indexer() {
        docsDictionary = new LinkedHashMap<>();
        termsDictionary = new LinkedHashMap<>();
        isStem = Configurations.getStemmingProp() == false ? "WithoutStem" : "Stem";
        createPostingsDir();

        synchronized (postingIndex) {
            ++postingIndex;
            index = postingIndex;
        }
    }

    /**
     * Launch indexing operation, create each file required
     * @param termList - terms list as received from parser
     * @param entitiesList - separate list for entities. Insert entity as term only
    if entity has been watched in 2 docs or more
     * @param docsMap - parsed documents list
     */
    public void startIndexer(Map<String, LinkedHashMap<CorpusDocument, Integer>> termList, Map<String, LinkedHashMap<CorpusDocument, Integer>> entitiesList, LinkedHashMap<CorpusDocument, LinkedHashMap<String, Integer>> docsMap) {

        if (Files.exists(Paths.get(postingFilePath + "\\Postings"))) {
            createDocPostingFile(docsMap);
            createPostingFileTerms(termList);
            createPostingFileEntities(entitiesList);
        } else
            System.err.println("No posting files");

    }

    /**
     * Calls to create posting file by type - terms
     * @param termHashMap
     */
    private void createPostingFileTerms(Map<String, LinkedHashMap<CorpusDocument, Integer>> termHashMap) {
        createPostingFile(termHashMap, "Terms");
    }

    /**
     * Calls to create posting file by type - entities
     * @param entitiesMap
     */
    private void createPostingFileEntities(Map<String, LinkedHashMap<CorpusDocument, Integer>> entitiesMap) {
        createPostingFile(entitiesMap, "Entity");
    }

    /**
     * Creates posting file by given type - terms or entities
     * @param termHashMap
     * @param fileType
     */
    private void createPostingFile(Map<String, LinkedHashMap<CorpusDocument, Integer>> termHashMap, String fileType) {
        File postingFile = new File(postingFilePath + "\\Postings\\" + isStem + "\\" + fileType + index + ".txt");
        String termEntry = "";
        BufferedWriter fileWriter;
        try {
            if (postingFile.exists())
                fileWriter = new BufferedWriter(new FileWriter(postingFile.getAbsoluteFile(), true));
            else
                fileWriter = new BufferedWriter(new FileWriter(postingFile));
            for (Map.Entry<String, LinkedHashMap<CorpusDocument, Integer>> term : termHashMap.entrySet()) {
                for (Map.Entry<CorpusDocument, Integer> document : term.getValue().entrySet()) {
                    if (termEntry.length() > 0)
                        termEntry += "?";
                    termEntry += docsDictionary.get(document.getKey().getDocNO())[0] + "?" + document.getValue(); // documents line number + document frequency;
                }
                termEntry = term.getKey() + "#" + termEntry;
                fileWriter.write(termEntry);
                fileWriter.write(System.getProperty("line.separator"));
                termEntry = "";
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates "documents posting file" in the following pattern - DOCNO#number of
     most frequent term?number of exclusive terms?title
     * @param temp
     * */
    private void createDocPostingFile(HashMap<CorpusDocument, LinkedHashMap<String, Integer>> temp) {
        String docInfo = "";
        File docsFile = new File(postingFilePath + "\\Postings\\Docs\\docs" + isStem + ".txt");
        try {
            synchronized (lock) {
                BufferedWriter docFileWriter = new BufferedWriter(new FileWriter(docsFile, true));
                for (Map.Entry<CorpusDocument, LinkedHashMap<String, Integer>> doc : temp.entrySet()) {
                    docInfo = doc.getKey().getDocNO() + "#" + doc.getValue().entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue() + "?" + doc.getValue().size() + "?";
                    Object[] data = new Object[13];
                    data[0] = docLineNumber;
                    docsDictionary.put(doc.getKey().getDocNO(), data);
                    ++docLineNumber;
                    docFileWriter.write(docInfo);
                    docFileWriter.write(System.getProperty("line.separator"));
                    docInfo = "";
                }
                docFileWriter.flush();
                docFileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Creates dictionary out of finite posting final into &quot;termDictionary&quot; data
     structure
     */
    public void buildDictionaryFromPosting() {
        try {
            File postingFileDirectory = new File(postingFilePath + "\\Postings\\" + isStem); // only one file the full posting file
            if (postingFileDirectory.exists() && postingFileDirectory.listFiles().length > 0) {
                postingFileDirectory = postingFileDirectory.listFiles()[0];
                BufferedReader brPostingFile = new BufferedReader(new FileReader(postingFileDirectory));
                String line;
                String[] termContent;
                String[] docContent;
                int[] termInfo; //first - doc Frequency, Second - line number in posting file, Third - total term frequency in corpus
                int lineNumber = 1;
                while ((line = brPostingFile.readLine()) != null) {
                    termInfo = new int[3];
                    termContent = line.split("#");
                    termInfo[0] = (termContent[1].split("\\?").length) / 2;
                    termInfo[1] = lineNumber;
                    docContent = termContent[1].split("\\?");
                    for (int i = 1; i < docContent.length; i = i + 2) {
                        termInfo[2] += Integer.parseInt(docContent[i]);
                    }
                    termsDictionary.put(termContent[0], termInfo);
                    ++lineNumber;
                }
                brPostingFile.close();
            } else {
                throw new Exception("PostingFile doesn't exists! Please be sure you have created the inverted file, and try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Uploads documents posting file to RAM in run-time
     */    public void buildDocsFromPosting() {
        try {
            File postingFileDirectory = new File(postingFilePath + "\\Postings\\Docs\\docs" + isStem + ".txt"); // only one file the full posting file
            if (postingFileDirectory.exists()) {
                BufferedReader brPostingFile = new BufferedReader(new FileReader(postingFileDirectory));
                String line;
                int lineNumber = 1;
                while ((line = brPostingFile.readLine()) != null) {
                    String docNO = line.split("#")[0];
                    Object[] data = new Object[13];
                    data[0] = lineNumber;

                    String[] restData = line.substring(line.indexOf("#") + 1).split("\\?");
                    data[1] = Integer.parseInt(restData[0]); //maxTerm
                    data[2] = Integer.parseInt(restData[1]); //numOfExclusiveTerms
                    for (int i = 2; i < restData.length; i++) {
                        data[i + 1] = restData[i]; //entities
                    }

                    docsDictionary.put(docNO, data);
                    ++lineNumber;
                }
                brPostingFile.close();
            } else {
                throw new Exception("PostingFile doesn't exists! Please be sure you have created the inverted file, and try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates 3 different directories - directory for posting file with stem,
     without stem and docs directory
     */
    private void createPostingsDir() {
        if (!Files.exists(Paths.get(postingFilePath + "\\Postings"))) {
            File postingFileDir = new File(postingFilePath + "\\Postings");
            boolean created = postingFileDir.mkdir();
            if (!created) {
                System.err.println("ERROR WHEN CREATING POSTINGS FILE");
            }
            File stemDir = new File(postingFilePath + "\\Postings\\Stem");
            created = stemDir.mkdir();
            if (!created) {
                System.err.println("ERROR WHEN CREATING STEM FILE");
            }
            File withoutStemDir = new File(postingFilePath + "\\Postings\\WithoutStem");
            created = withoutStemDir.mkdir();
            if (!created) {
                System.err.println("ERROR WHEN CREATING WithoutStem FILE");
            }
            File docsDir = new File(postingFilePath + "\\Postings\\Docs");
            created = docsDir.mkdir();
            if (!created) {
                System.err.println("ERROR WHEN CREATING DOCS FILE");
            }
        }
    }

    /**
     * @return term dictionary was build out of finite posting file
     */
    public Map<String, int[]> getTermsDictionary() {
        return termsDictionary;
    }

    /**
     * @return documents parsed list
     */    public Map<String, Object[]> getDocsDictionary() {
        return docsDictionary;
    }

    /**
     * Clear all data structure
     */
    public void resetIndexer() {
        termsDictionary.clear();
        docsDictionary.clear();
        postingIndex = 0;
        docLineNumber = 1;
        postingFilePath = Configurations.getPostingsFilePath();
        isStem = Configurations.getStemmingProp() == false ? "WithoutStem" : "Stem";
        MergeFiles.clear();
    }


}