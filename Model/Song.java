package Model;

public class Song {
    private static int sid = 1;
    private int song_id;
    private String title;
    private int length;

    public Song(
        String title, int length, int id
    ) {
        
    }

    public Song(
        String title, int length
    ) {
        this(title, length, 1);
    }
}
