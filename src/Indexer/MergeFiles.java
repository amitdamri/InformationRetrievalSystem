package Indexer;

import Files.Configurations;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.NumericUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class MergeFiles {


    private static HashMap<String, HashMap<String, Integer>> mostFrequentEntities = new HashMap<>(); // 5 (or less) most frequent entities in document and number of appearance of each entity in the document

    /**
     * Start merging as follow: clear old content from directories (if exists),
     sort new files, merge entities files, merge term files, merge all together
     */
    public static void mergePostings() {
        removePreviousFiles();

        sortFiles();

        mergeEntities();

        mergeTerms();

        mergeTermsWithEntities();

        // Dvir made changes here
        updateDocumentPostingWithEntities();


    }

    /**
     * Sort temporary posting files
     */
    private static void sortFiles() {
        File postings = new File(Configurations.getPostingsFilePath() + "\\Postings\\" + Indexer.isStem);
        BufferedWriter fwPostings;
        BufferedReader brPostings;
        ArrayList<String> fileToSort;
        String line, lineToWrite = "";
        String[] firstLine, secondLine;
        try {
            if (postings.exists()) {
                for (File file : postings.listFiles()) {
                    fileToSort = new ArrayList();
                    brPostings = new BufferedReader(new FileReader(file));
                    while ((line = brPostings.readLine()) != null) {
                        fileToSort.add(line);
                    }
                    brPostings.close();

                    List<String> sortedFile = fileToSort.parallelStream().sorted(new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            return o1.split("#")[0].toLowerCase().compareTo(o2.split("#")[0].toLowerCase());
                        }
                    }).collect(Collectors.toList());

                    fwPostings = new BufferedWriter(new FileWriter(file));
                    for (int i = 0; i < sortedFile.size() - 1; i++) {
                        lineToWrite = sortedFile.get(i);
                        firstLine = lineToWrite.split("#");
                        secondLine = sortedFile.get(i + 1).split("#");
                        while (firstLine[0].toLowerCase().equals(secondLine[0].toLowerCase())) {
                            lineToWrite = mergeDocumentListOfTheSameTerm(lineToWrite.split("#"), secondLine);
                            if (i + 2 < sortedFile.size()) {
                                ++i;
                                secondLine = sortedFile.get(i + 1).split("#");
                            } else
                                break;
                        }
                        fwPostings.write(lineToWrite);
                        fwPostings.write(System.getProperty("line.separator"));
                        if (i + 2 == sortedFile.size() && !firstLine[0].equals(secondLine[0])) { // last line doesnt equals to the previous line
                            fwPostings.write(sortedFile.get(i + 1));
                            fwPostings.write(System.getProperty("line.separator"));
                        }
                    }
                    fwPostings.flush();
                    fwPostings.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Merge all entities files into one
     */
    private static void mergeEntities() {
        mergePostingFiles("Entity");
    }

    /**
     * @param entityLineFromPosting - array of entities
     * @return true if the term is legal (for example entity has been seen in two
    or more documents), or false otherwise
     */
    private static boolean isLegalTerm(String[] entityLineFromPosting) {
        if (entityLineFromPosting[0].contains(" ")) {
            return entityLineFromPosting[1].split("\\?").length > 2; // One Document = DocID and docFrequency (2 values), therefore need more than 2 boxes
        }
        return true;
    }

    /**
     * Invoke merge *terms* posting files method
     */
    private static void mergeTerms() {
        mergePostingFiles("Terms");
    }

    /**
     * Invoke merge *entities* posting files method
     */
    private static void mergeTermsWithEntities() {
        mergePostingFiles("");
    }

    /**
     * Merge posting files by required type - terms or entities
     * @param typeOfMerge - term or entity
     */
    private static void mergePostingFiles(String typeOfMerge) {
        File postingsDir = new File(Configurations.getPostingsFilePath() + "\\Postings\\" + Indexer.isStem);
        File[] postingsFiles = postingsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (!name.equals("")) {
                    if (!typeOfMerge.equals("")) return name.startsWith(typeOfMerge);
                    else return name.contains("Terms") || name.contains("Entity");
                }
                return false;
            }
        }); // all the posting files
        if (postingsFiles.length > 1) {
            chooseFilesToReadAndWriteForPostingFiles(postingsFiles, typeOfMerge);
        }
    }

    /**
     * Choose the file to merge into, out of current two postings are being merged
     * @param postingsFiles - array of all temporary posting files in directory
     * @param typeOfMerge - terms or entities
     */
    private static void chooseFilesToReadAndWriteForPostingFiles(File[] postingsFiles, String typeOfMerge) {
        BufferedReader brFirstPosting, brSecondPosting; //reads the posting file
        File firstPostingFileMerged, secondPostingFileMerged; // the full Posting file - two in order to merge them with the rest. in the end only one file exists
        firstPostingFileMerged = secondPostingFileMerged = null;
        BufferedWriter tempFileWriter;
        int i;
        try {
            for (i = 1; i < postingsFiles.length; i++) {
                brFirstPosting = new BufferedReader(new FileReader(postingsFiles[i]));
                //merge all with the full posting file
                if (i > 1 && i % 2 == 0) {
                    brSecondPosting = new BufferedReader(new FileReader(firstPostingFileMerged));
                    if (secondPostingFileMerged == null)
                        secondPostingFileMerged = new File(Configurations.getPostingsFilePath() + "\\Postings\\" + Indexer.isStem + "\\fullPosting2" + typeOfMerge + ".txt");
                    tempFileWriter = new BufferedWriter(new FileWriter(secondPostingFileMerged));
                } else if (i > 1 && i % 2 == 1) {
                    brSecondPosting = new BufferedReader(new FileReader(secondPostingFileMerged));
                    tempFileWriter = new BufferedWriter(new FileWriter(firstPostingFileMerged));
                }
                //the first and second files still doesnt have the future full file
                else {
                    brSecondPosting = new BufferedReader(new FileReader(postingsFiles[i - 1]));
                    firstPostingFileMerged = new File(Configurations.getPostingsFilePath() + "\\Postings\\" + Indexer.isStem + "\\fullPosting1" + typeOfMerge + ".txt");
                    tempFileWriter = new BufferedWriter(new FileWriter(firstPostingFileMerged));
                }
                readWriteLinesFromPostingFiles(brFirstPosting, brSecondPosting, tempFileWriter, typeOfMerge);
                //remove unnecessary files
                brFirstPosting.close();
                brSecondPosting.close();
                tempFileWriter.flush();
                tempFileWriter.close();
                Files.delete(postingsFiles[i].toPath());
                if (i == 1)
                    Files.delete(postingsFiles[0].toPath());
            }
            if (firstPostingFileMerged != null && secondPostingFileMerged != null && firstPostingFileMerged.exists() && secondPostingFileMerged.exists()) {
                if (firstPostingFileMerged.lastModified() > secondPostingFileMerged.lastModified())
                    secondPostingFileMerged.delete();
                else
                    firstPostingFileMerged.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read and Write lines from and to required posting file
     * @param brFirstPosting - buffered reader for posting file number 1
     * @param brSecondPosting - buffered reader for posting file number 2
     * @param tempFileWriter - file writer
     * @param typeOfMerge - terms or entities
     */
    private static void readWriteLinesFromPostingFiles(BufferedReader brFirstPosting, BufferedReader brSecondPosting, BufferedWriter tempFileWriter, String typeOfMerge) {
        String lineFirstPosting, lineSecondPosting; //the lines in the posting file - got it from the buffer reader
        boolean termEntityMerge = false;
        String entityRegex = "[A-Z]+[\\s*[A-Z]]*";

        if (typeOfMerge.equals(""))
            termEntityMerge = true; // merge terms and entities - the reason is to check how many docs the entity has
        try {
            lineFirstPosting = brFirstPosting.readLine();
            lineSecondPosting = brSecondPosting.readLine();

            //runs over all the lines in the posting files
            while (lineFirstPosting != null && lineSecondPosting != null) {
                String[] firstPostingTerm = lineFirstPosting.split("#");
                String[] secondPostingTerm = lineSecondPosting.split("#");


                //write first term
                if (firstPostingTerm[0].toLowerCase().compareTo(secondPostingTerm[0].toLowerCase()) < 0) {
                    if (termEntityMerge && !isLegalTerm(firstPostingTerm)) { // checks if the term is legal while merge with entities
                        lineFirstPosting = brFirstPosting.readLine();
                        continue;
                    }
                    if (firstPostingTerm[0].matches(entityRegex))
                        addToDocsAndEntitiesMap(lineFirstPosting);
                    tempFileWriter.write(lineFirstPosting);
                    tempFileWriter.write(System.getProperty("line.separator"));
                    lineFirstPosting = brFirstPosting.readLine();
                }

                //write second term
                else if (firstPostingTerm[0].toLowerCase().compareTo(secondPostingTerm[0].toLowerCase()) > 0) {
                    if (termEntityMerge && !isLegalTerm(secondPostingTerm)) { // checks if the term is legal while merge with entities
                        lineSecondPosting = brSecondPosting.readLine();
                        continue;
                    }
                    if (secondPostingTerm[0].matches(entityRegex))
                        addToDocsAndEntitiesMap(lineSecondPosting);
                    tempFileWriter.write(lineSecondPosting);
                    tempFileWriter.write(System.getProperty("line.separator"));
                    lineSecondPosting = brSecondPosting.readLine();

                }

                //equals merge docs list amd write
                else {
                    String mergeLine = mergeDocumentListOfTheSameTerm(firstPostingTerm, secondPostingTerm);
                    String entity = mergeLine.split("#")[0];
                    if (entity.matches(entityRegex))
                        addToDocsAndEntitiesMap(mergeLine);
                    tempFileWriter.write(mergeLine);//writes one line for two duplicates
                    tempFileWriter.write(System.getProperty("line.separator"));
                    lineFirstPosting = brFirstPosting.readLine();
                    lineSecondPosting = brSecondPosting.readLine();
                }
            }
            //still lines in the first file
            while (lineFirstPosting != null) {
                if (termEntityMerge && !isLegalTerm(lineFirstPosting.split("#"))) {
                    lineFirstPosting = brFirstPosting.readLine();
                    continue;
                }
                String entity = lineFirstPosting.split("#")[0];
                if (entity.matches(entityRegex))
                    addToDocsAndEntitiesMap(lineFirstPosting);
                tempFileWriter.write(lineFirstPosting);
                tempFileWriter.write(System.getProperty("line.separator"));
                lineFirstPosting = brFirstPosting.readLine();
            }
            //still lines in the second file
            while (lineSecondPosting != null) {
                if (termEntityMerge && !isLegalTerm(lineSecondPosting.split("#"))) {
                    lineSecondPosting = brSecondPosting.readLine();
                    continue;
                }
                String entity = lineSecondPosting.split("#")[0];
                if (entity.matches(entityRegex))
                    addToDocsAndEntitiesMap(lineSecondPosting);
                tempFileWriter.write(lineSecondPosting);
                tempFileWriter.write(System.getProperty("line.separator"));
                lineSecondPosting = brSecondPosting.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add new entity to 5 most frequent entities of each document
     *
     * @param entityDescription
     */
    private static void addToDocsAndEntitiesMap(String entityDescription) {

        String entity = entityDescription.substring(0, entityDescription.indexOf("#"));
        String[] docsList = entityDescription.substring(entityDescription.indexOf("#") + 1).split("\\?");

        for (int i = 0; i < docsList.length; i += 2) { // iterate through docs identifiers
            String docIdentifier = docsList[i];
            if (!mostFrequentEntities.containsKey(docIdentifier)) {
                mostFrequentEntities.put(docIdentifier, new HashMap<>());
            }
            if (mostFrequentEntities.get(docIdentifier).size() < 5) {
                mostFrequentEntities.get(docIdentifier).put(entity, Integer.parseInt(docsList[i + 1]));
            }
            else {
                update5MostFrequentEntitiesListInDoc(docIdentifier, entity, Integer.parseInt(docsList[i + 1]));
            }
        }
    }

    /**
     * Add new entity if it has more appearances in current document than the minimum entity
     *
     * @param docIdentifier            - current document
     * @param entity                   - new entity
     * @param newEntityNumOfAppearance - new entity number of appearances in the document
     */
    private static void update5MostFrequentEntitiesListInDoc(String docIdentifier, String entity, int newEntityNumOfAppearance) {

        Map.Entry<String, Integer> entry = mostFrequentEntities.get(docIdentifier).entrySet().iterator().next();
        int minNumOfAppearance = entry.getValue(); // get random first minNumOfAppearance
        String entityMinNumOfAppearance = entry.getKey();
        for (Map.Entry entityAndFrequency : mostFrequentEntities.get(docIdentifier).entrySet()) {
            if (minNumOfAppearance > (Integer) entityAndFrequency.getValue()) {
                minNumOfAppearance = (Integer) entityAndFrequency.getValue();
                entityMinNumOfAppearance = (String) entityAndFrequency.getKey();
            }
        }
        if (newEntityNumOfAppearance > minNumOfAppearance) {
            mostFrequentEntities.get(docIdentifier).remove(entityMinNumOfAppearance);
            mostFrequentEntities.get(docIdentifier).put(entity, newEntityNumOfAppearance);
        }
    }

    /**
     * Merging documents list of two terms, who discovered as the same term while
     merging posting files
     * @param firstPostingTerm
     * @param secondPostingTerm
     * @return
     */
    private static String mergeDocumentListOfTheSameTerm(String[] firstPostingTerm, String[] secondPostingTerm) {
        String termLine;
        if (firstPostingTerm[0].equals(firstPostingTerm[0].toLowerCase())) { // if lower case save it like this
            termLine = firstPostingTerm[0] + "#";
        } else { // doesnt matter what is the format of the term
            termLine = secondPostingTerm[0] + "#";
        }
        termLine += firstPostingTerm[1] + "?" + secondPostingTerm[1];
        return termLine;
    }

    /**
     * Clear old content from directories before begging
     */
    private static void removePreviousFiles() {
        File removeFirstPreviousFiles = new File("Postings\\fullPosting1.txt");
        File removeSecondPreviousFiles = new File("Postings\\fullPosting2.txt");
        if (removeFirstPreviousFiles.exists())
            removeFirstPreviousFiles.delete();
        if (removeSecondPreviousFiles.exists())
            removeSecondPreviousFiles.delete();
    }

    /**
     * Update documents posting file with 5 most frequent entities
     */
    private static void updateDocumentPostingWithEntities() {

        try {
            // Make copy of old file in order to read it
            File docsFile = new File(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\docs" + Indexer.isStem +".txt");
            File tempDocsFile = new File(Configurations.getPostingsFilePath() + "\\Postings\\Docs\\tempDocs.txt");
            FileUtils.copyFile(docsFile, tempDocsFile);

            // Override content of documents posting file
            BufferedWriter docFileWriter = new BufferedWriter(new FileWriter(docsFile, false));
            docFileWriter.write("");
            docFileWriter.close();

            // Create a new file writer for new file and a file reader for old content
            docFileWriter = new BufferedWriter(new FileWriter(docsFile, true));
            BufferedReader bufferedReader = new BufferedReader(new FileReader(tempDocsFile));

            String line = "";
            String newLine = "";
            int docIdentifier = 1;
            while ((line = bufferedReader.readLine()) != null) {
                if (mostFrequentEntities.containsKey(docIdentifier + "")) {
                    newLine += line;
                    for (Map.Entry entityAndFrequency : mostFrequentEntities.get(docIdentifier + "").entrySet())
                        newLine += entityAndFrequency.getKey() + "?" + entityAndFrequency.getValue() + "?";
                } else
                    newLine += line;

                docFileWriter.write(newLine);
                docFileWriter.write(System.getProperty("line.separator"));
                newLine = "";
                docIdentifier++;
            }

            bufferedReader.close();
            docFileWriter.flush();
            docFileWriter.close();

            tempDocsFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * clears the data structures of this class
     */
    public static void clear(){
        mostFrequentEntities.clear();
    }
}