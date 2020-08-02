package GUI.View;

/**
 * Interface defines required methods for View layer
 */
public interface IView {
    void enableStemming();
    void choosePostingsFilesPathBrowse();
    void chooseCorpusPathBrowse();
    void getDictionary();
    void loadDictionary();
    void resetSystem();
    void startSystem();
}
