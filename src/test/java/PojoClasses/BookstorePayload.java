package PojoClasses;

public class BookstorePayload {
    private String userId;
    private Object collectionOfIsbns;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Object getCollectionOfIsbns() {
        return collectionOfIsbns;
    }

    public void setCollectionOfIsbns(Object collectionOfIsbns) {
        this.collectionOfIsbns = collectionOfIsbns;
    }
}
