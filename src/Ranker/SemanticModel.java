package Ranker;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

import java.util.Arrays;
import java.util.HashSet;

public class SemanticModel {

    /**
     * loads dictionary from file
     */
    public SemanticModel() {
        System.setProperty("wordnet.database.dir", System.getProperty("user.dir") + "\\dict");
    }

    /**
     * find the synonyms for each word i the query and returns set of all synonyms
     * @param query
     * @return set of synonyms of the query terms
     */
    public HashSet<String> querySemantic(String[] query) {
        HashSet<String> querySet = new HashSet<>(Arrays.asList(query));
        HashSet<String> querySynonyms = new HashSet<>();
        for (String term : query) {
            //  Get the synsets containing the word term
            WordNetDatabase database = WordNetDatabase.getFileInstance();
            Synset[] synsets = database.getSynsets(term);

            //  adds the word forms for synsets retrieved to the hashSet
            if (synsets.length > 0) {
                for (int i = 0; i < synsets.length; i++) {
                    String[] wordForms = synsets[i].getWordForms();
                    //adds only synonyms without the original terms from the query
                    for (int j = 0; j < wordForms.length; j++) {
                        if (!querySet.contains(wordForms[j]))
                            querySynonyms.add(wordForms[j]);
                    }
                }
            }
        }
        return querySynonyms;
    }

}


