package PojoClasses;

import java.util.ArrayList;

public class Isbn {
    private ArrayList<String> isbns;
    private String isbn;

    public ArrayList<String> getIsbn() {
        return isbns;
    }

    public void setIsbn(ArrayList<String> isbn) {
        this.isbns = isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
}
