package genyus.com.whichmovie.model;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Created by genyus on 26/11/15.
 */
public class Genre implements Serializable{

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        try {
            return new String(name.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return name;
        } catch (NullPointerException e) {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }
}
