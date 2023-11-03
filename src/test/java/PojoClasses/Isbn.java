package PojoClasses;

import com.github.javafaker.Faker;

import java.util.ArrayList;

public class Isbn {
    private ArrayList<String> isbns;
    private String isbn;

    static Faker faker = new Faker();

    public ArrayList<String> getIsbn() {
        return isbns;
    }

    public void setIsbn(ArrayList<String> isbn) {
        this.isbns = isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getOneIsbn() {
        return isbn;
    }

    public String setRandomISBN() {
        String ISBN = faker.regexify("([8-9]){1}([0-9]){11}");
        return ("97" + ISBN);
    }
}
