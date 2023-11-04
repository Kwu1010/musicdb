package Model;

import java.util.ArrayList;
import java.util.List;

public class Collection {
    private static int cid = 1;
    private int collection_id;
    private String collection_name;
    private int user_id;
    private List<Song> allSongs;
    private List<Album> allAlbums;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[");
        sb.append(collection_id + ", ");
        sb.append(collection_name + ", ");
        sb.append(user_id);
        sb.append("]");

        return sb.toString();
    }

    public Collection(String cn, int cid, int uid) {
        set_id(cid);
        set_uid(uid);
        set_collectionname(cn);
        this.allSongs = new ArrayList<>();
    }

    public Collection(String cn, int uid) {
        this(cn, -1, uid);
        int cid = get_new_id();
        set_id(cid);
    }

    public int get_new_id() {
        return cid++;
    }

    public int get_id() {
        return collection_id;
    }

    public int get_userid() {
        return user_id;
    }

    public String get_collectionname() {
        return collection_name;
    }
    
    public void addSongToCollection(Song song) {
        allSongs.add(song);
    }

    public void addAlbumToCollection(Album album){
        allAlbums.add(album);
    }

    public void get_collection_songs() {
        System.out.println("Collection: " + collection_name);
        for (Song song : allSongs) {
            System.out.println(song.get_title() + " by " + song.get_artist());
        }
    }

    public void get_collection_albums(){
        System.out.println("Collection: " + collection_name);
        for (Album album : allAlbums){
            System.out.println(album.get_albumname() + " ");
        }
    }

    private void set_id(int id) {
        this.collection_id = id;
    }

    private void set_uid(int id) {
        this.user_id = id;
    }

    private void set_collectionname(String name) {
        this.collection_name = name;
    }
}
