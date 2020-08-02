package Files;

import java.io.*;
import java.util.Properties;

/**
 * Configurations of the system - CorpusPath, PostingsPath, Stemming or not
 */
public class Configurations {

    private static Properties prop = new Properties();

    private Configurations(){}

    /**
     * sets the corpus path according to the user request
     * @param corpusPath
     */
    public static void setCorpusPath(String corpusPath) {
        prop.setProperty("CorpusPath", corpusPath);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  return the corpus path
     * @return cirpus path
     */
    public static String getCorpusPath(){
        String path="";
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                path = prop.getProperty("CorpusPath");
            }
            input.close();
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * sets the posting files path
     * @param postingsFilesPath
     */
    public static void setPostingsFilesPath(String postingsFilesPath) {
        prop.setProperty("PostingsPath", postingsFilesPath);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return the posting files path
     * @return posting files path
     */
    public static String getPostingsFilePath(){
        String path="";
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                path = prop.getProperty("PostingsPath");
            }
            input.close();
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * sets the stemming option - true or false
     * @param stemming
     */
    public static void setStemming(String stemming){
        prop.setProperty("Stemming",stemming);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return the stemming option - true or false
     * @return stemmig option
     */
    public static boolean getStemmingProp(){
        boolean stemming = false;
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                if (prop.getProperty("Stemming").equals("false")){
                    stemming = false;
                } else
                    stemming = true;
            }
            input.close();
            return stemming;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * sets the semantic option - true or false
     * @param semantic
     */
    public static void setSemantic(String semantic){
        prop.setProperty("Semantic",semantic);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return the semantic option - true or false
     * @return semantic option
     */
    public static boolean getSemanticProp(){
        boolean semantic = false;
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                if (prop.getProperty("Semantic").equals("false")){
                    semantic = false;
                } else
                    semantic = true;
            }
            input.close();
            return semantic;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * sets the clickStream option - true or false
     * @param clickStream
     */
    public static void setClickStream(String clickStream){
        prop.setProperty("ClickStream",clickStream);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return the clickStream option - true or false
     * @return clickStream option
     */
    public static boolean getClickStreamProp(){
        boolean click = false;
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                if (prop.getProperty("ClickStream").equals("false")){
                    click = false;
                } else
                    click = true;
            }
            input.close();
            return click;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * sets the queries file path
     * @param queriesFilePath - path to load queries file from
     */
    public static void setQueriesFilePath(String queriesFilePath) {
        prop.setProperty("QueriesFilePath", queriesFilePath);
        try {
            File userHome = new File(System.getProperty("user.dir"));
            File propertiesFile = new File(userHome, "config.properties");
            prop.store(new FileOutputStream(propertiesFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the queries file path
     */
    public static String getQueriesFilePath(){
        String path="";
        try {
            InputStream input = new FileInputStream(System.getProperty("user.dir") + "\\config.properties");
            prop.load(input);
            if (!prop.isEmpty()) {
                path = prop.getProperty("QueriesFilePath");
            }
            input.close();
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
}
