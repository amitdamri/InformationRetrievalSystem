package GUI.Main;

import GUI.Model.MyModel;
import GUI.View.MyViewController;
import GUI.ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Main class - program starts here with launching GUI
 */
public class Main extends Application {

    /**
     * start method - starts the whole application, launches GUI
     * @param primaryStage - primary screen (engine screen)
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        //ViewModel -> Model
        MyModel model = new MyModel();

        MyViewModel viewModel = new MyViewModel(model);
        model.addObserver(viewModel);


        //Loading Main.Main Windows
        primaryStage.setTitle("A.D Engine - Information Retrieval Project");
        FXMLLoader fxmlLoader = new FXMLLoader();
        primaryStage.setMinWidth(1120);
        primaryStage.setMinHeight(700);
        primaryStage.setMaxWidth(1120);
        primaryStage.setMaxHeight(700);
        Parent root = fxmlLoader.load(getClass().getResource("/GUIProject.fxml").openStream());
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        //View -> ViewModel
        MyViewController view = fxmlLoader.getController();
        view.initialize(viewModel,primaryStage,scene);
        viewModel.addObserver(view);
        //--------------
        setStageCloseEvent(primaryStage, model);
        //
        //Show the Main.Main Window
        primaryStage.resizableProperty().setValue(Boolean.FALSE);
        primaryStage.show();

    }

    /**
     * popping a window exit and wait for users decision. If user want to quit - closes program, otherwise, go back to main screen
     * @param primaryStage
     * @param model
     */
    private void setStageCloseEvent(Stage primaryStage, MyModel model) {
        primaryStage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want to exit?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
                // Close the program properly
                model.closeSystem();
            } else {
                // ... user chose CANCEL or closed the dialog
                event.consume();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
