package Test;

import Files.*;
import Indexer.*;
import Parse.Parse;
import org.apache.commons.io.FileUtils;


import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

public class ReadFileTest {

    public static void main(String[] args) throws InterruptedException, IOException {
        try {
            //test9();
            //test8();
            test43();
            //test8();
            //test1();
            //test2();
            //test3();
            //test6();
            //test();
            //para3();
            //para2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void test43(){
        try {
            HashSet<String> a = new HashSet<>();
            HashSet<String> b = new HashSet<>();
            BufferedReader r = new BufferedReader(new FileReader(new File("d:\\documents\\users\\amitdamr\\Downloads\\Postings\\fullPosting1.txt")));
            String line;
            while ((line = r.readLine())!=null){
                a.add(line.split("#")[0]);
            }
            r.close();
            r = new BufferedReader(new FileReader(new File("d:\\documents\\users\\amitdamr\\Downloads\\Postings\\stem\\fullPosting1.txt")));
            while ((line = r.readLine())!=null){
                b.add(line.split("#")[0]);
            }
            System.out.println("a : " + a.size());
            System.out.println("b : " + b.size());
            for (String s : b) {
                if (!a.contains(s))
                    System.out.println(s);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void test7() throws IOException {
        long start = System.nanoTime();
        Indexer i = new Indexer();
        MergeFiles.mergePostings();
        i.getTermsDictionary().size();
        long end = System.nanoTime();
        double total = (end - start) / 1000000000.0;
        System.out.println(total);
        System.out.println(i.getTermsDictionary().size());
        System.out.println(i.getDocsDictionary().size());
    }

    public static void test8() throws IOException {
        String s = "D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus";
        ReadFile r = new ReadFile(s);
        List<CorpusDocument> l = r.getL_corpusDocuments();
        System.out.println(l.size());
        Indexer i = new Indexer();
        i.buildDocsFromPosting();
        for (CorpusDocument t : l) {
            if (!i.getDocsDictionary().containsKey(t.getDocNO())) {
                System.out.println(t.getDocNO());
            }
        }
    }

    public static void test1() throws InterruptedException {
        long start = System.nanoTime();
        Indexer i = new Indexer();
        MergeFiles.mergePostings();
        long end = System.nanoTime();
        long total = end - start;
        System.out.println(total / 1000000000.0);
        System.out.println(i.getTermsDictionary().size());
    }

    public static void test2() throws InterruptedException, IOException {
        //FB396009
        FileUtils.deleteDirectory(new File("Postings"));

        long start = System.nanoTime();
        String s = "D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\FB396001";
        ReadFile r = new ReadFile(s);
        List<CorpusDocument> l = r.getL_corpusDocuments();
        //Parse p = new Parse("D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\stop-words.txt");

        for (int i = 0; i < l.size(); i = i + 5000) {
            lala(l, i, i + 5000);
        }

        long start2 = System.nanoTime();
        MergeFiles.mergePostings();

        long end2 = System.nanoTime();
        double total2 = (end2 - start2) / 1000000000.0;
        System.out.println("Merge:" + total2);


        /*for (int i = 0; i < l.size(); i++) {
            if (i % 5000 == 0) {
                System.out.println(i);
                p = new Parse("D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\stop-words.txt");
            }
            p.parsing(l.get(i));

        }*/
        //long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        //System.out.println(afterUsedMem-beforeUsedMem);
        /*for (Map.Entry<Term, LinkedHashMap<CorpusDocument,Integer>> e : p.getEntitiesMap().entrySet()) {
            System.out.println(e.getKey().getTermName());
        }*/
        long end = System.nanoTime();
        //System.out.println("total:" + Parse.getTime());
        // System.out.println("avg:" + Parse.avg());

        double total = (end - start) / 1000000000.0;
        System.out.println(total);

    }

    public static void test() throws IOException {
        FileUtils.deleteDirectory(new File("Postings"));
        long start = System.nanoTime();
        String s = "D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus";
        long start2 = System.nanoTime();
        ReadFile r = new ReadFile(s);
        long end2 = System.nanoTime();
        double total2 = (end2 - start2) / 1000000000.0;
        List<CorpusDocument> l = r.getL_corpusDocuments();
        System.out.println("list: " + total2 + " Size: " + l.size());

        int NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        check t;
        int i =0;

        while (i+47500<l.size()){
            t = new check(l.subList(i, i + 47500));
            exec.execute(t);
            i=i+47500;
        }
        t = new check(l.subList(i, l.size()));
        exec.execute(t);

        exec.shutdown();
        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Doneeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
       // MergeFiles.mergePostings();
       // Indexer in = new Indexer();
       // in.buildDocsFromPosting();
       // in.buildDictionaryFromPosting();
       // System.out.println(in.getDocsDictionary().size());
       // System.out.println(in.getTermsDictionary().size());
        //System.out.println(i.dictionarySize().get("01-03")[0]);
        //System.out.println(i.dictionarySize().get("01-03")[1]);
        //System.out.println(i.dictionarySize().get("01-03")[2]);
        long end = System.nanoTime();
        double total = (end - start) / 1000000000.0;
        System.out.println(total);
    }

    public static void lala(List<CorpusDocument> l, int index, int index2) {
        //Parse p = new Parse("D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\stop-words.txt");
        if (index2 > l.size())
            index2 = l.size();
        for (int i = index; i < index2; i++) {
            //p.parsing(l.get(i));
        }
        Indexer i = new Indexer();
       // i.startIndexer(p.getCorpusTerms(), p.getEntitiesMap(), p.getDocsMap());
        //System.out.println("size: " + p.getCorpusTerms().size());
        //System.out.println("size: " + p.getCorpusTerms().size());
        //System.out.println("total:" + Parse.getTime());
        //System.out.println("avg:" + Parse.avg());
        //System.out.println("Done : " + index + "-" + index2);
    }

}


class check implements Runnable {
    List<CorpusDocument> t;
    Parse p;
    Indexer x = new Indexer();
    boolean isIndexer = false;

    public check(List<CorpusDocument> f) {
        t = new ArrayList<>();
        this.t = f;
        //p = new Parse("D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\stop-words.txt");
    }

    public void run() {
        int i;
        for (i = 0; i < t.size(); i++) {
            isIndexer = false;
            p.parsing(t.get(i));
            if (i % 475 == 0 && i != 0) {
                x.startIndexer(p.getCorpusTerms(), p.getEntitiesMap(), p.getDocsMap());
                //p = new Parse("D:\\Downloads\\BenGUrion\\סמסטר ה\\אחזור מידע\\פרוייקט\\corpus\\stop-words.txt");
                System.out.println("Done" + i);
                isIndexer = true;
            }
        }
        if (!isIndexer) {
            x.startIndexer(p.getCorpusTerms(), p.getEntitiesMap(), p.getDocsMap());
            System.out.println("Done" + i);
        }
        //System.out.println("start:" + Parse.getTime());
        //System.out.println("avg:" + Parse.avg());
    }
}





