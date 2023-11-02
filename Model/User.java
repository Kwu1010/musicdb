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
    private String username;
    private String password;

    public User(String username, String password){
        this.username = username;
        this.password = password;
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
}