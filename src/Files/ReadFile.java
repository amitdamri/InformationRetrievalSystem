package Files;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the files from the corpus path and separates the documents
 */
public class ReadFile {

    private List<CorpusDocument> l_corpusDocuments;
    private File corpus;

    public ReadFile() {
        l_corpusDocuments = new ArrayList<>();
        corpus = null;
    }

    public ReadFile(String corpusPath) {
        l_corpusDocuments = new ArrayList<>();
        corpus = new File(corpusPath);
        readAllFiles(corpus);
    }

    /**
     * call to readAllFilesInCorpus Function with the corpus path in order to read all of the files
     * @param file
     */
    private void readAllFiles(File file) {
        for (File specificFile : file.listFiles()) {
            if (specificFile.isDirectory()) {
                readAllFiles(specificFile);
            } else {
                readDocuments(specificFile);
            }
        }
    }

    /**
     * returns list of the corpus documents
     * @return
     */
    public List<CorpusDocument> getL_corpusDocuments() {
        return l_corpusDocuments;
    }

    /**
     * separates documents in a file
     */
    private void readDocuments(File specificFile) {
        String line, docNO, text, date, title;
        text = docNO = title = date = "";
        try {
            Document doc = Jsoup.parse(specificFile, "UTF-8");
            Elements content = doc.getElementsByTag("DOC");
            for (Element document : content) {
                docNO = document.getElementsByTag("DOCNO").text();
                text = document.getElementsByTag("TEXT").text();
                date = document.getElementsByTag("DATE1").text();
                title = document.getElementsByTag("TI").text();
                if (docNO.equals("")) docNO = "UnknownDoc";
                if (!text.equals("")) {
                    CorpusDocument corpusDoc = new CorpusDocument(docNO, text, date, title);
                    //adds the document to the documents list
                    l_corpusDocuments.add(corpusDoc);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * clears the document list
     */
    public void clear(){
        l_corpusDocuments.clear();
    }

}
