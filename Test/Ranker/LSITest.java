package Ranker;

import Files.Configurations;
import Files.CorpusDocument;
import Files.ReadFile;
import Parse.Parse;
import dkpro.similarity.algorithms.api.SimilarityException;
import dkpro.similarity.algorithms.api.TextSimilarityMeasure;
import dkpro.similarity.algorithms.lexical.string.CosineSimilarity;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LSITest {



    @Test
    public void test(){
        SemanticModel l = new SemanticModel();
        HashSet<String> s= l.querySemantic(new String[]{"FALKLAND", "petroleum", "exploration" , "SOUTH ATLANTIC", "FALKLAND ISLANDS" });

        String p = Arrays.toString(s.toArray());
        Parse pd = new Parse();
        pd.parsing(new CorpusDocument("",p,"",""));
        HashMap<String, LinkedHashMap<CorpusDocument, Integer>> a = pd.getCorpusTerms();
        HashMap<String, LinkedHashMap<CorpusDocument, Integer>> ab = pd.getEntitiesMap();
        for (Map.Entry<String,LinkedHashMap<CorpusDocument,Integer>> f : a.entrySet()){
            System.out.println(f.getKey());
        }
        System.out.println("******************");
        String[] synonymsArray = a.keySet().toArray(new String[a.size()]);
        for (int i=0;i<synonymsArray.length;i++){
            System.out.println(synonymsArray[i]);
        }


    }

    @Test
    public void test4(){
        HashSet d = new HashSet();
        d.add(new Integer(2));
        d.add(new Integer(2));
        d.add(new Integer(3));
        Iterator e = d.iterator();
        while (e.hasNext()){
            Integer i  = (Integer)e.next();
            System.out.println(i);
        }

    }

    @Test
    public void test2(){
        ReadFile r = new ReadFile(Configurations.getCorpusPath() + "\\corpus");
        List<CorpusDocument> l = r.getL_corpusDocuments();
        try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("D:\\Downloads\\sspace.txt")));
            for (CorpusDocument d : l) {
                bw.write(d.getText());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test3() throws SimilarityException {
        TextSimilarityMeasure m = new CosineSimilarity();
String[] s = new String[]{"home"};
String[] s1 = new String[]{"house","home"};
        double score = m.getSimilarity(s1,s);
        System.out.println(score);


    }
}
