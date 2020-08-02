package Searcher;
import Files.CorpusDocument;
import Files.Query;
import Files.ReadQueriesFile;
import Parse.*;
import Indexer.*;
import Ranker.Ranker;
import java.io.File;
import java.util.*;

public class Searcher {

    private Indexer indexer;
    private Ranker ranker;

    /**
     * Constructor - init Searcher Object (consists of a Parser)
     */
    public Searcher(Indexer indexer) {
        Parse.createStopWordList(System.getProperty("user.dir")+"\\resources\\stop_words.txt");
        Parse.setStemming();
        this.indexer = indexer;
        ranker = new Ranker(indexer);
    }


    /**
     * Invoke method of searching a single query
     * @param query - string of query
     * @return all query terms as string array
     */
    public LinkedHashMap<String, Double> search(Query query) {

        LinkedHashMap<String, Double> rankingResults = null;

        String[] queryTerms_title = null;
        String[] queryTerms_desc = null;

        if (query.getTitle() != null) {
            queryTerms_title = searchQueryTitle(query);
        }
        if (query.getDescription() != null) {
            queryTerms_desc = searchQueryDescription(query);
        }

        if (queryTerms_title != null && queryTerms_desc != null)
            rankingResults = ranker.rankQuery(queryTerms_title, queryTerms_desc);

        else if (queryTerms_title != null)
            rankingResults = ranker.rankQuery(queryTerms_title);


        if (rankingResults == null)
            return null;

        rankingResults = getTopFiftyDocs(rankingResults);

        return rankingResults;
    }

    /**
     * Searching a single query
     * @param query - string of query
     * @return all query terms as string array
     */
    private String[] searchQueryTitle(Query query) {
        Parse parse = new Parse();

        CorpusDocument queryAsCorpusDoc_title = null;

        queryAsCorpusDoc_title = new CorpusDocument(null, query.getTitle(), null, null);
        parse.parsing(queryAsCorpusDoc_title);

        Set<String> queryTerms = new HashSet<>();
        queryTerms.addAll(parse.getCorpusTerms().keySet());
        queryTerms.addAll(parse.getEntitiesMap().keySet());
        String[] termsArr = new String[queryTerms.size()];
        Iterator it = queryTerms.iterator();
        for (int i = 0; i < termsArr.length && it.hasNext(); i++)
            termsArr[i] = (String)it.next();

        return termsArr;
    }

    /**
     * Searching a single query
     * @param query - string of query
     * @return all query terms as string array
     */
    private String[] searchQueryDescription(Query query) {
        Parse parse = new Parse();

        CorpusDocument queryAsCorpusDoc_desc = null;

        queryAsCorpusDoc_desc = new CorpusDocument(null, query.getDescription(), null, null);
        parse.parsing(queryAsCorpusDoc_desc);

        Set<String> queryTerms = new HashSet<>();
        queryTerms.addAll(parse.getCorpusTerms().keySet());
        queryTerms.addAll(parse.getEntitiesMap().keySet());
        String[] termsArr = new String[queryTerms.size()];
        Iterator it = queryTerms.iterator();
        for (int i = 0; i < termsArr.length && it.hasNext(); i++)
            termsArr[i] = (String)it.next();

        return termsArr;
    }


    /**
     * @param rankingResults - list of all relevant documents and their ranking
     * @return list of top fifty high-ranked documents
     */
    private LinkedHashMap<String, Double> getTopFiftyDocs(LinkedHashMap<String, Double> rankingResults) {

        if (rankingResults.size() <= 50)
            return rankingResults;

        LinkedHashMap<String, Double> result = new LinkedHashMap<>();

        int index = 0;
        Iterator<Map.Entry<String, Double>> it = rankingResults.entrySet().iterator();
        Map.Entry<String,Double> doc;
        while (it.hasNext() && index<50){
            doc = it.next();
            result.put(doc.getKey(),doc.getValue());
            ++index;
        }

        return result;
    }

    /**
     * @param docNO - document number to search in
     * @return - list of top 5 high-ranked entities in the document
     */
    public Map<String, Double> getTopFiveEntities(String docNO) {

        Object[] docData = indexer.getDocsDictionary().get(docNO);
        Object[] temp = Arrays.copyOfRange(docData, 3, 13);

        return ranker.rankEntities(temp);
    }

}