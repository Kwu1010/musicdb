package Model;
/**
 * Users will be able to create new accounts and access via login. The system must record
the date and time an account is created. It must also stored the date and time an user
access into the application
• Users will be able to create collections of music.
• Users will be able to see the list of all their collections by name in ascending order.
The list must show the following information per collection:
– Collection’s name
– Number of songs in the collection
– Total duration in minutes
• Users will be able to search for songs by name, artist, album, or genre. The resulting
list of songs must show the song’s name, the artist’s name, the album, the length and
the listen count. The list must be sorted alphabetically (ascending) by song’s name
and artist’s name. Users can sort the resulting list by song name, artist’s name, genre,
and released year (ascending and descending).
• Users can add and delete albums, and songs from their collection
• Users can modify the name of a collection. They can also delete an entire collection
• Users can listen to a song individually or it can play an entire collection. You must
record every time a song is played by a user. You do not need to actually be able to
play songs, simply mark them as played
• Users can follow another user. Users can search for new users to follow by email
• The application must also allow an user to unfollow a another user
 * 
 */
public class User {
    private static int uid = 1;
    private int user_id;
    private String username;
    private String password;
    private String first_name;
    private String last_name;
    private String email;
    // TODO: add "last access date" and "creation date"

    public User(
        String un, String pw, String fn, String ln, String em, int id
    ) {
        set_username(un);
        set_password(pw);
        set_first_name(fn);
        set_last_name(ln);
        set_email(em);
        set_id(id);
        // NOTES: we can set "last access date" every time this was called
    }
    
    public User(
        String un, String pw, String fn, String ln, String em
        ) {
        this.User(un, pw, fn, ln, em, get_id()); // DEBUG: not sure why there's error in this line
        // NOTES: we can set "creation date" every time this was called
    }


    private int get_id() {
        return uid++;
    }
    public String get_username() {
        return username;
    }
    public String get_password() {
        return password;
    }

    private void set_username(String username) {
        this.username = username;
    }
    private void set_password(String password) {
        this.password = password;
    }
    private void set_first_name(String first_name) {
        this.first_name = first_name;
    }
    private void set_last_name(String last_name) {
        this.last_name = last_name;
    }
    private void set_email(String email) {
        this.email = email;
    }
    private void set_id(int id) {
        this.user_id = id;
    }
}