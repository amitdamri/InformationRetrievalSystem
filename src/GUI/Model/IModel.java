package GUI.Model;

import Files.Configurations;
import Files.Query;
import Indexer.Indexer;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interface defines required methods for model layer
 */
public interface IModel {

    void startSystem();

    void closeSystem();

    void loadDictionary();

    void showDictionary();

    void resetSystem();

    void enableStemming(boolean value);

    void chooseCorpusPathBrowse(Stage systemStage);

    void chooseCorpusPath(Stage systemStage,String path);

    void choosePostingPathBrowse(Stage systemStage);

    void choosePostingPath(Stage systemStage,String path);

    void chooseQueriesFilePathBrowse(Stage systemStage);

    void chooseQueriesFilePath(Stage systemStage, String path);

    void insertQuery(String query);

    void insertQueryFile(String queryFilePath);

    void showTop5EntitiesAction(String docno);

    void enableSemantic(boolean value);

    void enableClickStream(boolean value);

    void saveQueryResult(Stage systemStage, LinkedHashMap<String, LinkedHashMap<String, Double>> queryResult);
}
