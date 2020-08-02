package Files;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadQueriesFile {

    private List<Query> queriesList;

    /**
     * Ctor
     */
    public ReadQueriesFile() {
        queriesList = new ArrayList<>();
    }

    /**
     * Reads query file and separates its content to num , title , description, narrative
     * @param file query file in the required format
     * @return list of queries
     */
    public List<Query> readQueriesFile(File file) {

        String num, title, description, narrative;
        try {
            Document doc = Jsoup.parse(file, "UTF-8");
            Elements content = doc.getElementsByTag("top");

            for (Element document : content) {
                String temp = document.select("num").first().ownText();
                num = temp.substring(temp.indexOf(" ")+1);
                title = document.getElementsByTag("title").text();
                temp = document.select("desc").first().ownText();
                description = temp.substring(temp.indexOf(" ")+1);
                temp = document.getElementsByTag("narr").text();
                narrative = temp.substring(temp.indexOf(" ")+1);
                if (title != null && !title.matches("^\\s*$")) { // check title isn't null or empty
                    Query query = new Query(num, title, description, narrative);
                    //adds the document to the documents list
                    queriesList.add(query);
                }
            }
            return queriesList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
