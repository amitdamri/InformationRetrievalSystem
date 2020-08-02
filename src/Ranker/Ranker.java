package Ranker;

import Files.Configurations;
import Files.CorpusDocument;
import Indexer.Indexer;
import Parse.Parse;

import java.util.*;
import java.util.stream.Collectors;

public class Ranker {

    private boolean isSemanticModel;
    private boolean isClickStream;
    private Indexer indexer;

    /**
     * Ctor - receive indexer in order to use the dictionaries while calculating the doc rank
     * @param indexer - include dictionary and docDictionary
     */
    public Ranker(Indexer indexer) {
        isSemanticModel = Configurations.getSemanticProp();
        isClickStream = Configurations.getClickStreamProp();
        this.indexer = indexer;
    }

    /**
     * Ranks every relevant document for the query.
     * receive two arrays - the first one the title terms and the second one is the description terms.
     * first, calculates docs ranks dor each of the title and desc, then combine them, and adds semantic score and/or clickstream score
     * according to the UI request
     * @param title
     * @param desc
     * @return list of docs and their ranks
     */
    public LinkedHashMap<String, Double> rankQuery(String[] title, String[] desc) {
        LinkedHashMap<String, Double> titleRank;
        LinkedHashMap<String, Double> descRank;
        LinkedHashMap<String, Double> queriesRank;
        //ranks title
        BM25 bmModel = new BM25(title, indexer.getTermsDictionary(), indexer.getDocsDictionary());
        titleRank = bmModel.bm25Algorithm();
        //ranks description
        bmModel = new BM25(desc, indexer.getTermsDictionary(), indexer.getDocsDictionary());
        descRank = bmModel.bm25Algorithm();
        //combine both ranks
        if (titleRank != null && descRank != null)
            queriesRank = mergeTwoRanks(titleRank, 0.05, descRank, 0.95, 0.5);
        else if (titleRank != null)
            queriesRank = titleRank;
        else
            queriesRank = descRank;
        //combine both queries to one array in order to use it with semantic/clickstream rank
        String[] totalQuery = new String[title.length + desc.length];
        System.arraycopy(title, 0, totalQuery, 0, title.length);
        System.arraycopy(desc, 0, totalQuery, title.length, desc.length);
        if (isSemanticModel) {
            queriesRank = rankSemanticAndMerge(totalQuery, queriesRank);
        }
        if (isClickStream) {
            queriesRank = rankClickStreamAndMerge(totalQuery, queriesRank);
        }
        return queriesRank;
    }

    /**
     * Ranks single query - has only one array.
     * first calculates its bm25 ranks and then if needed computes semantic and clickstream ranks
     * @param query
     * @return lis of docs rank
     */
    public LinkedHashMap<String, Double> rankQuery(String[] query) {
        LinkedHashMap<String, Double> queryRank;
        BM25 bmModel = new BM25(query, indexer.getTermsDictionary(), indexer.getDocsDictionary());
        queryRank = bmModel.bm25Algorithm();
        if (isSemanticModel) {
            queryRank = rankSemanticAndMerge(query, queryRank);
        }
        if (isClickStream) {
            queryRank = rankClickStreamAndMerge(query, queryRank);
        }
        return queryRank;
    }

    /**
     * receives two ranks and combine them according to the percent of each rank
     * @param firstRank
     * @param firstPercent
     * @param secondRank
     * @param secondPercent
     * @param ifNotExistsPercent
     * @return combined Map of doc scores
     */
    private LinkedHashMap<String, Double> mergeTwoRanks(LinkedHashMap<String, Double> firstRank, double firstPercent, LinkedHashMap<String, Double> secondRank, double secondPercent, double ifNotExistsPercent) {
        if (firstRank != null && secondRank != null) {
            for (Map.Entry<String, Double> termRank : secondRank.entrySet()) {
                if (firstRank.containsKey(termRank.getKey()))
                    firstRank.replace(termRank.getKey(), termRank.getValue() * secondPercent + firstRank.get(termRank.getKey()) * firstPercent);
                else
                    firstRank.put(termRank.getKey(), termRank.getValue() * ifNotExistsPercent);
            }
            //sort queryRank
            firstRank = firstRank.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            return firstRank;
        }
        return null;
    }

    /**
     * rank the query according to semantic model - get all its synonyms and check their bm25 ranks
     * @param query
     * @return map of docs and their semantic score
     */
    private LinkedHashMap<String, Double> rankSemantic(String[] query) {
        HashSet<String> synonyms;
        LinkedHashMap<String, Double> rankedSynonyms = new LinkedHashMap<>();
        HashMap<String, LinkedHashMap<CorpusDocument, Integer>> termsMapFromParser = new HashMap<>();
        SemanticModel semanticModel = new SemanticModel();
        //get synonyms
        synonyms = semanticModel.querySemantic(query);

        if (synonyms.size() > 0) {
            //parse synonyms
            Parse parser = new Parse();
            String textSynonyms = Arrays.toString(synonyms.toArray());
            parser.parsing(new CorpusDocument("", textSynonyms, "", ""));

            //get parsed terms
            termsMapFromParser.putAll(parser.getCorpusTerms());

            //get only keys = terms from Map
            String[] synonymsArray = termsMapFromParser.keySet().toArray(new String[termsMapFromParser.size()]);

            //rank synonyms
            BM25 bmModel = new BM25(synonymsArray, indexer.getTermsDictionary(), indexer.getDocsDictionary());
            rankedSynonyms = bmModel.bm25Algorithm();
        }
        return rankedSynonyms;
    }

    /**
     * ranks entities - for each entity compute its numOfFrequencies in doc and divide it with the maxFreqEntity
     * @param entitiesAndFreq array of entities and their frequencies
     * @return Map of entities and their rank
     */
    public LinkedHashMap<String, Double> rankEntities(Object[] entitiesAndFreq) {
        LinkedHashMap<String, Double> entitiesRank = new LinkedHashMap<>();
        int maxFreq = 0, currFreq;
        double rank;
        //finds the max freq
        for (int i = 1; i < entitiesAndFreq.length; i += 2) {
            if (entitiesAndFreq[i] != null) {
                currFreq = Integer.parseInt((String) entitiesAndFreq[i]);
                if (currFreq > maxFreq)
                    maxFreq = currFreq;
            }
        }
        //calculates score by dividing with max freq
        for (int i = 0; i < entitiesAndFreq.length; i += 2) {
            if (entitiesAndFreq[i] != null) {
                rank = Double.parseDouble((String) entitiesAndFreq[i + 1]) / maxFreq;
                entitiesRank.put((String) entitiesAndFreq[i], rank);
            }
        }
        //sorts the array
        if (entitiesRank.size() > 1)
        entitiesRank = entitiesRank.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return entitiesRank;

    }

    /**
     * ranks query with clickstream rank
     * @param query
     * @param queriesRank - the doc ranks till now
     * @return combined map of doc ranks with the clickstream rank and the previous rank
     */
    private LinkedHashMap<String, Double> rankClickStreamAndMerge(String[] query, LinkedHashMap<String, Double> queriesRank) {
        LinkedHashMap<String, Double> clickStreamRank;
        //calculates ClickStream rank and merges between two ranks - 50% query rank + 50% ClickStream rank
        ClickStream click = new ClickStream();
        clickStreamRank = click.rankClickStream(query);
        clickStreamRank = mergeTwoRanks(queriesRank, 0.8, clickStreamRank, 0.2, 0.15);
        return clickStreamRank;
    }

    /**
     * ranks query with Semantic rank
     * @param query
     * @param queriesRank - the doc ranks till now
     * @return combined map of doc ranks with the semantic rank and the previous rank
     */
    private LinkedHashMap<String, Double> rankSemanticAndMerge(String[] query, LinkedHashMap<String, Double> queriesRank) {
        LinkedHashMap<String, Double> semanticRank;
        //calculates semantic rank and merges between two ranks - 75% query rank + 25% semantic rank
        semanticRank = rankSemantic(query);
        semanticRank = mergeTwoRanks(queriesRank, 0.85, semanticRank, 0.15, 0.15);
        return semanticRank;
    }

}
