package Model;

public class Song {
    private static int sid = 1;
    private int song_id;
    private String title;
    private String artist;
    private int length;
    private String release_date;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(song_id + ", ");
        sb.append(title + ", ");
        sb.append(artist + ", ");
        sb.append(length + ", ");
        sb.append(release_date);
        sb.append("]");
        return sb.toString();
    }

    public Song(String title, String artist, int length, String rd, int id) {
        set_title(title);
        set_artist(artist);
        set_length(length);
        set_releasedate(rd);
        set_id(id);
    }

    public Song(String title, String artist, int length, String rd) {
        this(title, artist, length, rd, -1);
        int id = get_new_id();
        set_id(id);
    }

    public int get_new_id() {
        return sid++;
    }

    public int get_id() {
        return song_id;
    }

    public String get_title() {
        return title;
    }

    public String get_artist() {
        return artist;
    }
    private void set_title(String title){
        this.title = title;
    }

    private void set_artist(String artist) {
        this.artist = artist;
    }

    private void set_releasedate(String release_date) {
        this.release_date = release_date;
    }

    private void set_length(int length) {
        this.length = length;
    }
}
