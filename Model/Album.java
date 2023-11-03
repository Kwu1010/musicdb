package Model;

import java.util.ArrayList;

public class Album {
    private static int aid = 1;
    private int album_id;
    private String album_name;
    private String release_date;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        sb.append(album_id + ", ");
        sb.append(album_name + ", ");
        sb.append(release_date);
        sb.append("]");

        return sb.toString();
    }

    public Album(String an, String rd, int id) {
        set_id(id);
        set_albumname(an);
        set_releasedate(rd);
    }

    public Album(String an, String rd) {
        this(an, rd, -1);
        int id = get_new_id();
        set_id(id);
    }

    private int get_new_id() {
        return aid++;
    }

    public int get_id() {
        return album_id;
    }

    public String get_albumname() {
        return album_name;
    }

    public String get_releasedate() {
        return release_date;
    }
    
    private void set_id(int id) {
        this.album_id = id;
    }

    public void set_albumname(String name) {
        this.album_name = name;
    }

    public void set_releasedate(String date) {
        this.release_date = date;
    }   
}
