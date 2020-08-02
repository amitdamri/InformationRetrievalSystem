package GUI.ViewModel;

import GUI.Model.IModel;
import javafx.stage.Stage;

import Files.Query;

import java.util.*;

/**
 * connection between model and view
 */
public class MyViewModel extends Observable implements Observer {

    private IModel model;

    public MyViewModel(IModel model){
        this.model = model;
    }

    /**
     * sends update to the view layer
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o==model){
            //Notify my observer (View) that I have changed
            setChanged();
            notifyObservers(arg);
        }
    }

    /**
     * all functions just call to the model functions according to the view requests
     */

    public void startSystem(){
        model.startSystem();
    }

    public void loadDictionaryFromPosting(){
        model.loadDictionary();
    }

    public void resetSystem(){
        model.resetSystem();
    }

    //view Model Functionality
    public void chooseCorpusDirBrowse(Stage systemStage){
        model.chooseCorpusPathBrowse(systemStage);
    }

    public void choosePostingFilesDirBrowse(Stage systemStage){
        model.choosePostingPathBrowse(systemStage);
    }

    public void chooseCorpusDir(Stage systemStage,String path){
        model.chooseCorpusPath(systemStage,path);
    }

    public void choosePostingFilesDir(Stage systemStage,String path){
        model.choosePostingPath(systemStage,path);
    }

    public void enableStemming(boolean enable){
        model.enableStemming(enable);
    }

    public void showDictionary(){
        model.showDictionary();
    }

    public void chooseQueriesFilesDir(Stage systemStage,String path) { model.chooseQueriesFilePath(systemStage,path); }

    public void chooseQueriesFileDirBrowse(Stage systemStage) { model.chooseQueriesFilePathBrowse(systemStage); }

    public void insertQuery(String query) { model.insertQuery(query); }

    public void insertQueryFile(String queryFilePath) { model.insertQueryFile(queryFilePath); }

    public void showTop5EntitiesAction(String docno) { model.showTop5EntitiesAction(docno); }

    public void enableSemantic(boolean enable){
        model.enableSemantic(enable);
    }

    public void enableClickStream(boolean enable){
        model.enableClickStream(enable);
    }

    public void saveQueryResult(Stage systemStage, LinkedHashMap<String, LinkedHashMap<String, Double>> queriesRank) { model.saveQueryResult(systemStage,queriesRank);}
}
