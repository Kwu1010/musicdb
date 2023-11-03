package Model;

public class Artist {
    private static int rid = 1;
    private int artist_id;
    private String artist_name;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(artist_id + ", ");
        sb.append(artist_name);
        sb.append("]");
        return sb.toString();
    }

    public Artist(String name, int id) {
        set_artist(name);
        set_id(id);
    }

    public Artist(String name) {
        this(name, -1);
        int id = get_new_id();
        set_id(id);
    }

    public int get_new_id() {
        return rid++;
    }

    public int get_id() {
        return artist_id;
    }

    public String get_artist() {
        return artist_name;
    }
    
    private void set_id(int id){
        this.artist_id = id;
    }

    private void set_artist(String name){
        this.artist_name = name;
    }
}
