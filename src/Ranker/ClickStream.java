package Ranker;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.*;

import java.io.*;
import java.util.LinkedHashMap;

/**
 * This class was written by the help of this post https://stackoverflow.com/questions/36300485/how-to-resolve-the-difference-between-the-values-attained-in-the-web-api-and-the
 * Checks the similarity between each word in the queries according to wuPalmer algorithm - considers the depths of the two synsets
 * in the WordNet taxonomies, along with the depth of the LCS (Least Common Subsumer). and finally calculates the average score for whole of the sentence.
 * this score is the similarity score between both of the queries - if the similarity is high there is most probability that the user will click on the same doc.
 */

public class ClickStream {

    private static ILexicalDatabase db = new NictWordNet();

    /**
     * returns the matrix similarity after semantic rank
     * @param words1 first query
     * @param words2 second query - from clickstream file
     * @param rc Relatedness Calculator
     * @return similarity matrix
     */
    private double[][] getSimilarityMatrix(String[] words1, String[] words2, RelatednessCalculator rc) {
        double[][] result = new double[words1.length][words2.length];
        for (int i = 0; i < words1.length; i++) {
            for (int j = 0; j < words2.length; j++) {
                double score = rc.calcRelatednessOfWords(words1[i], words2[j]); // rank each word
                result[i][j] = score;
            }
        }
        return result;
    }

    /**
     * computes the total score after getting the similarity matrix according to wuPalmer formula.
     * finally takes the max rank for each word sums it and the score is the average rank
     * @param words1
     * @param words2
     * @return
     */
    private double compute(String[] words1, String[] words2) {

        double score = 0, count = 0, maxScorePerLine;
        RelatednessCalculator rc1 = new WuPalmer(db);
        double[][] s1 = getSimilarityMatrix(words1, words2, rc1);

        for (int i = 0; i < words1.length; i++) {
            maxScorePerLine = 0;
            for (int j = 0; j < words2.length; j++) {
                if (s1[i][j] > 1) {
                    maxScorePerLine = 1;
                    break;
                } else if (maxScorePerLine < s1[i][j])
                    maxScorePerLine = s1[i][j];
            }
            score += maxScorePerLine;
            ++count;
        }
        return score / count;
    }

    /**
     * clickStream rank - get query from user, and for each line on clickstream file calculates the similarity between the query and the query in the file that the user entered
     * the doc rank is according to this computes similarity
     * @param query
     * @return
     */
    public LinkedHashMap<String, Double> rankClickStream(String[] query) {
        LinkedHashMap<String, Double> clickStreamRank = new LinkedHashMap<>();
        String line;
        String[] splitLine, splitClickUserQuery;
        double docScore;
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(System.getProperty("user.dir") + "\\resources\\clickstream.txt")));
            line = br.readLine(); // first line.
            while ((line = br.readLine()) != null) {
                splitLine = line.split("\\,");
                splitClickUserQuery = splitLine[splitLine.length - 1].split(" ");
                docScore = compute(splitClickUserQuery, query);
                if (clickStreamRank.containsKey(splitLine[splitLine.length - 2])) {
                    clickStreamRank.replace(splitLine[splitLine.length - 2],clickStreamRank.get(splitLine[splitLine.length - 2])+docScore); // sums two scores of the same document
                } else
                    clickStreamRank.put(splitLine[splitLine.length - 2], docScore);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return clickStreamRank;
    }

}
