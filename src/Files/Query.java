package Files;

/**
 * This class represents the query
 */
public class Query {

    String num;
    String title;
    String description;
    String narrative;

    /**Ctor**/
    public Query(String num, String title, String description, String narrative) {
        this.num = num;
        this.title = title;
        this.description = description;
        this.narrative = narrative;
    }

    /**Getters**/
    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getNum(){return num;}
}
