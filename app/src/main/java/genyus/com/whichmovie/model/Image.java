package genyus.com.whichmovie.model;

import java.io.Serializable;

/**
 * Created by genyus on 13/12/15.
 */
public class Image implements Serializable {

    public String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if(((Image)o).getPath().equals(path)){
            return true;
        }
        return false;
    }
}
