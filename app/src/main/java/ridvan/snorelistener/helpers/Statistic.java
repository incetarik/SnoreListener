package ridvan.snorelistener.helpers;

import java.io.Serializable;
import java.util.Date;

public class Statistic implements Serializable {
    private String name;
    private String content;
    private Date   dateTime;

    public Statistic() {
    }

    public Statistic(String name) {

        this.name = name;
    }

    public Statistic(String name, String content) {

        this.name = name;
        this.content = content;
    }

    public Statistic(String name, String content, Date dateTime) {

        this.name = name;
        this.content = content;
        this.dateTime = dateTime;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }
}
