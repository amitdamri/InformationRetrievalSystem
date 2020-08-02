package IRSystem;

import Files.CorpusDocument;
import Indexer.Indexer;
import Parse.Parse;
import java.util.List;

public class SystemThread implements Runnable {
    List<CorpusDocument> corpusDocuments;
    Parse parser;
    Indexer indexer = new Indexer();
    boolean isIndexer = false;

    /**
     * Constructor, create new SystemThread
     * @param documentsList - documents list to parse and index
     */
    public SystemThread(List<CorpusDocument> documentsList) {
        this.corpusDocuments = documentsList;
        parser = new Parse();
    }
    /**
     * Run the program through the thread was created
     */
    public void run() {
        int i;
        for (i = 0; i < corpusDocuments.size(); i++) {
            isIndexer = false;
            parser.parsing(corpusDocuments.get(i));
            if (i % 475 == 0 && i != 0) {
                indexer.startIndexer(parser.getCorpusTerms(), parser.getEntitiesMap(), parser.getDocsMap());
                parser.resetParse();
                isIndexer = true;
            }
        }
        if (!isIndexer) {
            indexer.startIndexer(parser.getCorpusTerms(), parser.getEntitiesMap(), parser.getDocsMap());
        }
    }
}
