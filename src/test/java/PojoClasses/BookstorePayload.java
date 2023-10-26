package PojoClasses;

import java.util.ArrayList;

public class BookstorePayload {
    private String userId;
    private ArrayList<Object> collectionOfIsbns;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<Object> getCollectionOfIsbns() {
        return collectionOfIsbns;
    }

    public void setCollectionOfIsbns(ArrayList<Object> collectionOfIsbns) {
        this.collectionOfIsbns = collectionOfIsbns;
    }
}
