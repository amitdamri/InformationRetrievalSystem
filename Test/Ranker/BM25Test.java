package Ranker;

        import Files.Configurations;
        import Files.Query;
        import Files.ReadQueriesFile;
        import Indexer.Indexer;
        import Parse.Parse;
        import Searcher.Searcher;
        import org.junit.Test;

        import java.io.BufferedWriter;
        import java.io.File;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.util.*;


public class BM25Test {

    @Test
    public void test() {
        Indexer indexer = new Indexer();
        indexer.buildDocsFromPosting();
        indexer.buildDictionaryFromPosting();
        System.out.println(indexer.getDocsDictionary().get("FBIS4-33055")[0]);
        System.out.println(indexer.getTermsDictionary().get("extraordinarily")[1]);
        System.out.println(indexer.getDocsDictionary().size());
        System.out.println(indexer.getDocsDictionary().get("FT933-5574")[0]);
        System.out.println(indexer.getDocsDictionary().get("FT933-5574")[1]);
        System.out.println(indexer.getTermsDictionary().get("%%")[0]);
        System.out.println(indexer.getTermsDictionary().get("%%")[1]);
        System.out.println(indexer.getTermsDictionary().get("%%")[2]);
    }

    /*@Test
    public void test2(){
        Indexer indexer = new Indexer();
        indexer.buildDocsFromPosting();
        indexer.buildDictionaryFromPosting();
        String[] query = new String[]{"-0%","-0.0%","-1.7%","ZZZZZZZZZZ"};
        System.out.println("check");
        BM25 bm = new BM25(query,indexer.getTermsDictionary(),indexer.getDocsDictionary());
        bm.readAllQueryLinesFromPosting();
    }*/

    @Test
    public void bm25Test() {
        Indexer i = new Indexer();
        i.buildDocsFromPosting();
        i.buildDictionaryFromPosting();
        System.out.println("FirstQuery");
        String[] query = new String[]{"FALKLAND", "petroleum", "exploration", "SOUTH ATLANTIC", "FALKLAND ISLANDS"};
        BM25 bm = new BM25(query, i.getTermsDictionary(), i.getDocsDictionary());
        LinkedHashMap<String, Double> list = bm.bm25Algorithm();
        Iterator<Map.Entry<String, Double>> j = list.entrySet().iterator();
        int h = 0;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results.txt"), true));
            for (Map.Entry<String, Double> k = j.next(); h < 50 && j.hasNext(); h++, k = j.next()) {
                if (k.getValue() > 0) {
                    bw.write("351 0 " + k.getKey() + " 1 " + k.getValue() + " mt");
                    bw.newLine();
                }
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
  /*      for (Map.Entry<String,Double> k = j.next(); h<50 && j.hasNext(); h++,k=j.next()){
            System.out.println( k.getKey()+ " " + k.getValue());
        }*/
    }

    @Test
    public void test5() {
        Indexer i = new Indexer();
        i.buildDocsFromPosting();
        System.out.println("buildDocs");
        i.buildDictionaryFromPosting();
        System.out.println("buildDic");
        Ranker r = new Ranker(i);
        System.out.println("Rank");

        System.out.println("FirstQuery");
        String[] query = new String[]{"FALKLAND", "petroleum", "exploration", "SOUTH ATLANTIC", "FALKLAND ISLANDS"};
        LinkedHashMap<String, Double> d = r.rankQuery(query);

        Iterator<Map.Entry<String, Double>> j = d.entrySet().iterator();
        int h = 0;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results.txt"), true));
            for (Map.Entry<String, Double> k = j.next(); h < 50 && j.hasNext(); h++, k = j.next()) {
                if (k.getValue() > 0) {
                    bw.write("351 0 " + k.getKey() + " 1 " + k.getValue() + " mt");
                    bw.newLine();
                }
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


   /* @Test
    public void test6() {

        Indexer indexer = new Indexer();
        indexer.buildDocsFromPosting();
        System.out.println("buildDocs");
        indexer.buildDictionaryFromPosting();
        System.out.println("buildDic");
        Ranker r = new Ranker(indexer);
        System.out.println("Rank");
        System.out.println("Stop");

        Searcher searcher = new Searcher(indexer);
        System.out.println("Searcher");
        //File queryFile = new File("D:\\Downloads\\queries.txt");
        Map<String, Double> res = searcher.search("FALKLAND petroleum FALKLAND ISLANDS exploration SOUTH ATLANTIC");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results.txt"), true));
            for (Map.Entry<String, Double> entry : res.entrySet()) {
                bw.write("351 0 " + entry.getKey() + " 1 " + entry.getValue() + " mt");
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Test
    public void test7() {

        Indexer indexer = new Indexer();
        indexer.buildDocsFromPosting();
        System.out.println("buildDocs");
        indexer.buildDictionaryFromPosting();
        System.out.println("buildDic");
        System.out.println("Rank");
        System.out.println("Stop");

        Searcher searcher = new Searcher(indexer);
        System.out.println("Searcher");
        File queryFile = new File("D:\\Downloads\\queries.txt");
        ReadQueriesFile readQueriesFile = new ReadQueriesFile();
        List<Query> queries = readQueriesFile.readQueriesFile(queryFile);
        try {

            for (Query query : queries) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results.txt"), true));
                Map<String, Double> res = searcher.search(query);
                System.out.println("search**********");
                for (Map.Entry<String, Double> entry : res.entrySet()) {
                    bw.write(query.getNum()+" 0 " + entry.getKey() + " 1 " + entry.getValue() + " mt");
                    bw.newLine();
                }
                bw.flush();
                System.out.println("write**************");
                bw.flush();
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test200(){
        LinkedHashMap<String,LinkedHashMap<String,Object[]>> t = new LinkedHashMap<>();
        LinkedHashMap<String,Object[]> f = new LinkedHashMap<>();
        f.put("dvir",new Object[]{"1","2"});
        t.put("amit",f);
        t.get("amit").put("dada",new Object[]{"3","4"});
        System.out.println(t.get("amit").size());
    }
}
