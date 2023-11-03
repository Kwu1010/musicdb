package Model;

public class Genre {
    private static int gid = 1;
    private int genre_id;
    private String type;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        sb.append(genre_id + ", ");
        sb.append(type);
        sb.append("]");

        return sb.toString();
    }

    public Genre(String type, int id) {
        set_id(id);
        set_genre(type);
    }

    public Genre(String type) {
        this(type, -1);
        int id = get_new_id();
        set_id(id);
    }

    private int get_new_id() {
        return gid++;
    }

    public int get_id() {
        return genre_id;
    }

    public String get_genre() {
        return type;
    }
    
    private void set_id(int id) {
        this.genre_id = id;
    }

    private void set_genre(String type) {
        this.type = type;
    }   
}
