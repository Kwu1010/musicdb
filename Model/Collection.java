package Model;

public class Collection {
    private static int cid = 1;
    private int collection_id;
    private String collection_name;
    private int user_id;

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
    }

    public Collection(String cn, int uid) {
        this(cn, uid, -1);
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
    
    private void set_id(int id) {
        this.collection_id = id;
    }

    private void set_uid(int id) {
        this.collection_id = id;
    }

    private void set_collectionname(String name) {
        this.collection_name = name;
    }
}
