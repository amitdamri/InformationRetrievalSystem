package GUI.View;

import GUI.ViewModel.MyViewModel;
import edu.cmu.lti.ws4j.impl.Lin;
import javafx.beans.binding.MapExpression;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * the controller of the gui - responsible for the program window
 */
public class MyViewController implements Observer, IView {

    private static MyViewModel viewModel;
    @FXML
    private TextArea corpusPath;
    @FXML
    private TextArea postingsPath;
    @FXML
    private TextField queriesFilePath;
    @FXML
    private TextField query;
    @FXML
    private RadioButton stemmingButton;
    @FXML
    private TableColumn Terms;
    @FXML
    private TableColumn Number;
    @FXML
    private TableView table;
    @FXML
    private Label lbl_docNum;
    @FXML
    private Label lbl_exTerms;
    @FXML
    private Label lbl_totalTime;
    @FXML
    private TableView queryResultsTable;
    @FXML
    private Label lbl_totalRelevantDocs;
    @FXML
    private TableColumn docno;
    @FXML
    private TableColumn queryNum;
    @FXML
    private Button searchEntities;
    @FXML
    private TextField docnoEntities;
    @FXML
    private TableView entitiesTable;
    @FXML
    private TableColumn entities;
    @FXML
    private TableColumn ranking;
    @FXML
    private RadioButton semanticButton;
    @FXML
    private RadioButton clickStreamButton;

    //Stages
    private static Stage mainStage;
    private static Stage subStage;
    private static Stage subStageEntities;
    private static Scene mainScene;

    //Data structures
    private static LinkedHashMap<String, LinkedHashMap<String, Double>> queriesResults;

    /**
     * init the stages and listen for changes in the corpus/postings path
     *
     * @param viewModel
     * @param mainStage
     * @param mainScene
     */
    public void initialize(MyViewModel viewModel, Stage mainStage, Scene mainScene) {
        this.viewModel = viewModel;
        this.mainScene = mainScene;
        this.mainStage = mainStage;
        setCorpusPathListener();
        setPostingsPathListener();
        setQueriesFilePathLisenter();
    }

    /**
     * get updates from the model - and reacts according to the given object
     *
     * @param o   from view model
     * @param arg value from the model
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o == viewModel) {
            if (arg instanceof double[]) {
                showResult((double[]) arg);
            } else if (arg instanceof Object[]) {
                Object[] file = (Object[]) arg;
                switch ((int) file[1]) {
                    case 1:
                        corpusPath.setText(((File) file[0]).getAbsolutePath());
                        break;
                    case 2:
                        postingsPath.setText(((File) file[0]).getAbsolutePath());
                        break;
                    case 3:
                        queriesFilePath.setText(((File) file[0]).getAbsolutePath());
                        break;
                    case 4:
                        LinkedHashMap<String, int[]> dictionaryTerm = (LinkedHashMap<String, int[]>) file[0];
                        showDictionaryInTable(dictionaryTerm);
                        break;
                    case 5:
                        LinkedHashMap<String, Double> temp = (LinkedHashMap<String, Double>) file[0];
                        LinkedHashMap<String, LinkedHashMap<String, Double>> queryRank = new LinkedHashMap<>();
                        queryRank.put("1025", temp);
                        queriesResults = queryRank;
                        showQueryResult(queryRank);
                        break;
                    case 6:
                        LinkedHashMap<String, LinkedHashMap<String, Double>> queriesRank = (LinkedHashMap<String, LinkedHashMap<String, Double>>) file[0];
                        queriesResults = queriesRank;
                        showQueryResult(queriesRank);
                        break;
                    case 7:
                        LinkedHashMap<String, Double> entities = (LinkedHashMap<String, Double>)file[0];
                        showTop5Entities(entities);
                    default:
                        break;
                }
            } else if (arg instanceof String) {
                showError((String) arg, "ERROR: NO Dictionary Found or Not Valid Input");
            }
        }
    }



    /**
     * function that runs when start button is clicked - must have paths in the text area, and reset stemming according to the check box
     */
    public void startSystem() {
        if (corpusPath.getText().equals("") || postingsPath.getText().equals("")) {
            showError("Please insert Corpus and Posting path.", "Path is missing");
        } else if (!(new File(corpusPath.getText()).exists()) || !(new File(postingsPath.getText()).exists())) {
            showError("Corpus or Postings File doesn't exist. Please choose other directory.", "Path doesn't exist");
        } else {
            enableStemming();
            waitingScreen();
            viewModel.startSystem();
        }
    }

    /**
     * shows a waiting screen when the system is processing
     */
    public void waitingScreen() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Loading...");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("/WaitingScreen.fxml").openStream());
            Scene scene = new Scene(root);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.resizableProperty().setValue(Boolean.FALSE);
            MyViewController view = fxmlLoader.getController();
            stage.setScene(scene);
            subStage = stage;
            subStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * shows the results of the process
     *
     * @param indexInfo - First - document frequency, Second- term total frequency, Third - total time of process
     */
    public void showResult(double[] indexInfo) {
        try {
            if (subStage != null) {
                subStage.close();
            }
            Stage stage = new Stage();
            stage.setTitle("Results");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("/ResultScreen.fxml").openStream());
            Scene scene = new Scene(root);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.resizableProperty().setValue(Boolean.FALSE);
            MyViewController view = fxmlLoader.getController();
            view.lbl_docNum.textProperty().setValue(Double.toString(indexInfo[0]));
            view.lbl_exTerms.textProperty().setValue(Double.toString(indexInfo[1]));
            view.lbl_totalTime.textProperty().setValue(Double.toString(indexInfo[2]));
            stage.setScene(scene);
            subStage = stage;
            subStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * closes the window
     */
    public void closeWindow() {
        if (subStage != null)
            subStage.close();
    }

    /**
     * reset the system - remove the memory and posting files
     */
    public void resetSystem() {
        table.setItems(null);
        viewModel.resetSystem();
    }

    /**
     * loads the dictionary from the posting files
     */
    public void loadDictionary() {
        waitingScreen();
        viewModel.loadDictionaryFromPosting();
        subStage.close();
    }

    /**
     * get the dictionary from the model
     */
    public void getDictionary() {
        viewModel.showDictionary();
    }

    /**
     * shows dictionary inside the table
     *
     * @param termsDictionary
     */
    private void showDictionaryInTable(LinkedHashMap<String, int[]> termsDictionary) {
        //reset table
        table.setItems(null);

        Terms.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String, String>, String>, ObservableValue<String>>) p -> {
            return new SimpleStringProperty(p.getValue().getKey());
        });


        Number.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String, int[]>, Integer>, ObservableValue<Integer>>) p -> {
            return new SimpleIntegerProperty(p.getValue().getValue()[2]).asObject();
        });

        ObservableList<Map.Entry<String, int[]>> items = FXCollections.observableArrayList(termsDictionary.entrySet());
        table.setItems(items);
    }

    /**
     * chooses the path of the corpus with the browse button
     */
    public void chooseCorpusPathBrowse() {
        viewModel.chooseCorpusDirBrowse(mainStage);
    }

    /**
     * chooses the path of the postings with the browse button
     */
    public void choosePostingsFilesPathBrowse() {
        viewModel.choosePostingFilesDirBrowse(mainStage);
    }

    /**
     * chooses the path of the corpus with the text area
     */
    public void setCorpusPathListener() {
        corpusPath.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                viewModel.chooseCorpusDir(mainStage, newValue);
                corpusPath.setText(newValue);
            }
        });
    }

    /**
     * chooses the path of the postings with the text area
     */
    public void setPostingsPathListener() {
        postingsPath.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                viewModel.choosePostingFilesDir(mainStage, newValue);
                postingsPath.setText(newValue);
            }
        });
    }

    /**
     * sets stemming option - off/on
     */
    public void enableStemming() {
        viewModel.enableStemming(stemmingButton.isSelected());
    }

    /**
     * shows error according to the given cause
     *
     * @param alertMessage
     * @param errorType
     */
    private void showError(String alertMessage, String errorType) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(alertMessage);
        alert.setHeaderText(errorType);
        alert.show();
    }

    /**
     * chooses the path of the queries file with the browse button
     */
    public void chooseQueriesFilePathBrowse() {
        viewModel.chooseQueriesFileDirBrowse(mainStage);
    }

    /**
     * chooses the path of the queries file with the text field
     */
    public void setQueriesFilePathLisenter() {
        queriesFilePath.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                viewModel.chooseQueriesFilesDir(mainStage, newValue);
                queriesFilePath.setText(newValue);
            }
        });
    }

    /**
     * search a query
     */
    public void searchQuery() {
        String queryInput = query.getText();
        if (queryInput == null || queryInput.equals("")) {
            showError("Please insert Query.","Empty Query");
        } else {
            viewModel.insertQuery(queryInput);
        }
    }

    /**
     * takes query file path from the user and sends it to the viewModel
     */
    public void searchQueriesFile() {
        String queryFilesPath = queriesFilePath.getText();
        if (queryFilesPath == null || queryFilesPath.equals("")) {
            showError("Please insert Query.","Empty Query");
        } else {
            viewModel.insertQueryFile(queryFilesPath);
        }
    }

    /**
     * shows the query result on new window screen
     * @param queryResult
     */
    public void showQueryResult(LinkedHashMap<String, LinkedHashMap<String, Double>> queryResult){
        try {
            Stage stage = new Stage();
            stage.setTitle("Query Results");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("/QueryResultsScreen.fxml").openStream());
            Scene scene = new Scene(root);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.resizableProperty().setValue(Boolean.FALSE);
            MyViewController view = fxmlLoader.getController();

            //count how many relevant documents found
            int size = 0;
            for (Map.Entry<String, LinkedHashMap<String, Double>> entry : queryResult.entrySet()) {
                size += entry.getValue().size();
            }

            view.lbl_totalRelevantDocs.textProperty().setValue(Integer.toString(size));

            //reset table
            view.queryResultsTable.setItems(null);


            view.docno.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String,String>, String>, ObservableValue<String>>) p -> {
                return new SimpleStringProperty(p.getValue().getKey());
            });

            view.queryNum.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String,String>, String>, ObservableValue<String>>) p -> {
                return new SimpleStringProperty(p.getValue().getValue());
            });

            LinkedHashMap<String, String> docsAndQueryNum = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashMap<String, Double>> linkedHashMap : queryResult.entrySet()) {
                for (Map.Entry<String, Double> entry : linkedHashMap.getValue().entrySet()) {
                    docsAndQueryNum.put(entry.getKey(), linkedHashMap.getKey());
                }
            }

            ObservableList<Map.Entry<String, String>> items = FXCollections.observableArrayList(docsAndQueryNum.entrySet());
            view.queryResultsTable.setItems(items);

            stage.setScene(scene);
            subStage = stage;
            subStage.show();
        }
        catch (IOException e) {
            System.err.println("Error occurred while trying load fxml single query result");
        }

    }

    /**
     * calls the viewModel in order to load entities
     */
    public void showTop5EntitiesAction() {
        String docno = docnoEntities.getText();
        if (docno == null) {
            showError("Please insert valid DOCNO.","Empty DOCNO");
        } else {
            viewModel.showTop5EntitiesAction(docno);
        }
    }

    /**
     * shows on the screen the entities of the required doc
     * @param entities
     */
    public void showTop5Entities(LinkedHashMap<String, Double> entities) {
        try {
            Stage stage = new Stage();
            stage.setTitle("Entities");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("/DocumentEntities.fxml").openStream());
            Scene scene = new Scene(root);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.resizableProperty().setValue(Boolean.FALSE);
            MyViewController view = fxmlLoader.getController();

            //reset table
            view.entitiesTable.setItems(null);


            view.entities.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String,Double>, String>, ObservableValue<String>>) p -> {
                return new SimpleObjectProperty<>(p.getValue().getKey());
            });

            view.ranking.setCellValueFactory((Callback<TableColumn.CellDataFeatures<Map.Entry<String,Double>, Double>, ObservableValue<Double>>) p -> {
                return new SimpleObjectProperty<>(p.getValue().getValue());
            });

            ObservableList<Map.Entry<String,Double>> items = FXCollections.observableArrayList(entities.entrySet());
            view.entitiesTable.setItems(items);

            stage.setScene(scene);
            subStageEntities = stage;
            subStageEntities.show();
        }
        catch (IOException e) {
            System.err.println("Error occurred while trying present entities");
        }

    }

    /**
     * sets Semantic option - off/on
     */
    public void enableSemantic() {
        viewModel.enableSemantic(semanticButton.isSelected());
    }

    /**
     * sets ClickStream option - off/on
     */
    public void enableClickStream() {
        viewModel.enableClickStream(clickStreamButton.isSelected());
    }

    /**
     * save queries file according to TREC_EVAL format
     */
    public void saveQueryResult(){
        if (queriesResults != null)
            viewModel.saveQueryResult(mainStage,queriesResults);
    }

}
