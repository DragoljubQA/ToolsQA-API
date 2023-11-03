package Tests;

import PojoClasses.AccountPayload;
import PojoClasses.BookstorePayload;
import PojoClasses.Isbn;
import PojoClasses.Variables;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.Random;

import static io.restassured.RestAssured.given;

public class ApiTests {
    Variables variables;
    String accountParam;
    String bookstoreParam;
    String authorizedParam;
    String generateTokenParam;
    String userParam;
    String singleBookParam;
    String multipleBooksParam;
    Isbn isbn;
    BookstorePayload bookstorePayload;
    AccountPayload accountPayload;
    Isbn bookID1;
    Isbn bookID2;

    @BeforeClass
    public void setUp() {
        variables = new Variables();
        isbn = new Isbn();
        bookstorePayload = new BookstorePayload();
        accountPayload = new AccountPayload();
        RestAssured.baseURI = "https://demoqa.com/";
        accountParam = "Account/v1/";
        bookstoreParam = "BookStore/v1/";
        authorizedParam = "Authorized";
        generateTokenParam = "GenerateToken";
        userParam = "User";
        singleBookParam = "Book";
        multipleBooksParam = "Books";

        bookID1 = new Isbn();
        bookID2 = new Isbn();
    }

    @AfterMethod
    public void log(ITestResult result) {
        System.out.println("TEST: " + result.getName() + " FINISHED");
        switch (result.getStatus()) {
            case 1:
                System.out.println("RESULT: SUCCESS");
                break;
            case 2:
                System.out.println("RESULT: FAILED");
                break;
            case 3:
                System.out.println("RESULT: SKIPPED");
                break;
            default:
                System.out.println("RESULT: UNKNOWN");
        }
    }

    @AfterClass
    public void tearDown() {
        removeUser();
    }

    public boolean userIsAuthorized() {
        AccountPayload body = new AccountPayload();
        body.setUserName(accountPayload.getUserName());
        body.setPassword(accountPayload.getPassword());
        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post(accountParam+authorizedParam)
                .then()
                .log().all()
                .extract().response().asString();

        switch (response) {
            case "true":
                return true;

            case "false":
                return false;

            default:
                System.out.println("Unknown response");
                return false;
        }
    }

    public String getUser(int expectCode) {
        return  given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .get(accountParam+userParam+"/"+accountPayload.getUserId())
                .then()
                .statusCode(expectCode)
                .extract().response().asString();
    }

    public void removeUser() {
        if (accountPayload.getUserId() != null && variables.getToken() != null) {
            String response = given()
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + variables.getToken())
                    .delete(accountParam+userParam+"/"+accountPayload.getUserId())
                    .then()
                    .statusCode(204)
                    .extract().response().asString();

            Assert.assertTrue(response.isEmpty());

            // Verify user can't be found after removal

            JsonPath jp = new JsonPath(getUser(401));
            String message = jp.get("message");
            Assert.assertEquals(message, "User not found!");
        }
    }

    public String getISBN() {
        return isbn.getIsbn().get(new Random().nextInt(isbn.getIsbn().size()));
    }
    @Test(priority = 10)
    public void createUser() {
        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(accountPayload.accountPayload())
                .when()
                .post(accountParam+userParam)
                .then()
                .log().all()
                .statusCode(201)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        accountPayload.setUserId(jp.get("userID"));
        accountPayload.setUserName(jp.get("username"));

        String books = jp.get("books[0]");
        Assert.assertNull(books);
        Assert.assertFalse(userIsAuthorized());
    }
    @Test(priority = 20)
    public void createToken() {
        createUser();
        AccountPayload body = new AccountPayload();
        body.setUserName(accountPayload.getUserName());
        body.setPassword(accountPayload.getPassword());
        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post(accountParam+generateTokenParam)
                .then()
                .statusCode(200)
                .log().all()
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        variables.setToken(jp.get("token"));
        String status = jp.get("status");
        String result = jp.get("result");
        Assert.assertTrue(userIsAuthorized());
        Assert.assertEquals(status, "Success");
        Assert.assertEquals(result, "User authorized successfully.");
    }

    @Test(priority = 30)
    public void getUserInfo() {
        createUser();
        createToken();
        JsonPath jp = new JsonPath(getUser(200));
        String userID = jp.get("userId");
        String username = jp.get("username");
        Assert.assertEquals(userID, accountPayload.getUserId());
        Assert.assertEquals(username, accountPayload.getUserName());
    }

    @Test(priority = 40)
    public void getAllBooks() {
        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .when()
                .get(bookstoreParam+multipleBooksParam)
                .then()
                .log().all()
                .statusCode(200)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        int size = jp.get("books.size()");
        String body = jp.get("books[0].isbn");

        Assert.assertFalse(body.isEmpty());
        Assert.assertTrue(size > 0);

        ArrayList<String> lista = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            lista.add(jp.get("books["+i+"].isbn"));
        }
        isbn.setIsbn(lista);
    }

    @Test(priority = 50)
    public void getOneBook() {
        getAllBooks();

        String bookID = isbn.getIsbn().get(new Random().nextInt(isbn.getIsbn().size()));
        String response = given()
                .queryParam("ISBN", bookID)
                .log().all()
                .header("Content-Type", "application/json")
                .when()
                .get(bookstoreParam+singleBookParam)
                .then()
                .log().all()
                .statusCode(200)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        String isbn = jp.get("isbn");
        String title = jp.get("title");
        String subTitle = jp.get("subTitle");
        String author = jp.get("author");
        String publish_date = jp.get("publish_date");
        String publisher = jp.get("publisher");
        String pages = jp.get("pages").toString();
        String description = jp.get("description");
        String website = jp.get("website");
        Assert.assertFalse(isbn.isEmpty());
        Assert.assertFalse(title.isEmpty());
        Assert.assertFalse(subTitle.isEmpty());
        Assert.assertFalse(author.isEmpty());
        Assert.assertFalse(publish_date.isEmpty());
        Assert.assertFalse(publisher.isEmpty());
        Assert.assertFalse(pages.isEmpty());
        Assert.assertFalse(description.isEmpty());
        Assert.assertFalse(website.isEmpty());
    }

    @Test(priority = 60)
    public void addBookToUser() {
        createUser();
        createToken();
        getAllBooks();

        BookstorePayload payload = new BookstorePayload();
        ArrayList list = new ArrayList<>();
        bookID1.setIsbn(isbn.getIsbn().get(new Random().nextInt(isbn.getIsbn().size())));
        list.add(bookID1);
        payload.setUserId(accountPayload.getUserId());
        payload.setCollectionOfIsbns(list);

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .post(bookstoreParam+multipleBooksParam)
                .then()
                .log().all()
                .statusCode(201)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        String responseISBN = jp.get("books[0].isbn");
        Assert.assertEquals(responseISBN, bookID1.getOneIsbn());
    }

    @Test(priority = 70)
    public void addMultipleBooksToUser() {
        createUser();
        createToken();
        getAllBooks();

        BookstorePayload payload = new BookstorePayload();

        ArrayList list = new ArrayList<>();
        String book1 = getISBN();
        String book2;
        while(true) {
            book2 = getISBN();
            if (!book2.equals(book1)) {
                break;
            }
        }
        bookID1.setIsbn(book1);
        list.add(bookID1);
        bookID2.setIsbn(book2);
        list.add(bookID2);
        payload.setUserId(accountPayload.getUserId());
        payload.setCollectionOfIsbns(list);

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .post(bookstoreParam+multipleBooksParam)
                .then()
                .log().all()
                .statusCode(201)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        String firstBook = jp.getString("books[0].isbn");
        String secondBook = jp.getString("books[1].isbn");
        Assert.assertEquals(firstBook, bookID1.getOneIsbn());
        Assert.assertEquals(secondBook, bookID2.getOneIsbn());

    }

    @Test(priority = 80)
    public void removeAllBooksFromUser() {
        addMultipleBooksToUser();

        JsonPath jp = new JsonPath(getUser(200));
        String books = jp.get("books[0].isbn");
        Assert.assertNotNull(books);

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .when()
                .delete(bookstoreParam+multipleBooksParam+"?UserId="+accountPayload.getUserId())
                .then()
                .statusCode(204)
                .log().all()
                .extract().response().asString();

        jp = new JsonPath(getUser(200));
        books = jp.get("books[0].isbn");
        Assert.assertNull(books);
    }

    @Test(priority = 90)
    public void removeOneBookFromUser() {
        addMultipleBooksToUser();

        JsonPath jp = new JsonPath(getUser(200));
        String book1 = jp.get("books[0].isbn");
        String book2 = jp.get("books[1].isbn");
        Assert.assertEquals(book1, bookID1.getOneIsbn());
        Assert.assertEquals(book2, bookID2.getOneIsbn());

        BookstorePayload payload = new BookstorePayload();
        payload.setUserId(accountPayload.getUserId());
        payload.setIsbn(bookID1.getOneIsbn());

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .delete(bookstoreParam+singleBookParam)
                .then()
                .log().all()
                .statusCode(204);

        jp = new JsonPath(getUser(200));
        book1 = jp.get("books[0].isbn");
        book2 = jp.get("books[1].isbn");
        Assert.assertEquals(book1, bookID2.getOneIsbn());
        Assert.assertNull(book2);
    }

    @Test(priority = 100)
    public void switchBooksOnUser() {
        addBookToUser();

        String book1 = bookID1.getOneIsbn();
        String book2;
        while(true) {
            book2 = getISBN();
            if (!book2.equals(book1)) {
                break;
            }
        }

        JsonPath jp = new JsonPath(getUser(200));
        String firstBook = jp.get("books[0].isbn");
        String secondBook = jp.get("books[1].isbn");
        Assert.assertEquals(firstBook, book1);
        Assert.assertNull(secondBook);

        BookstorePayload payload = new BookstorePayload();
        payload.setUserId(accountPayload.getUserId());
        payload.setIsbn(book2);

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .put(bookstoreParam+multipleBooksParam+"/"+book1)
                .then()
                .statusCode(200)
                .log().all();

        jp = new JsonPath(getUser(200));
        firstBook = jp.get("books[0].isbn");
        secondBook = jp.get("books[1].isbn");
        Assert.assertEquals(firstBook, book2);
        Assert.assertNull(secondBook);

    }

    @Test(priority = 110)
    public void cannotCreateUserWithNullFields() {
        AccountPayload payload = new AccountPayload();

        for(int i = 0; i < 2; i++) {
            payload.setUserName(accountPayload.setRandomUsername());
            payload.setPassword(accountPayload.setRandomPassword());

            if(i == 0) {
                payload.setUserName(null);
            } else {
                payload.setPassword(null);
            }
            String response = given()
                    .log().all()
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .when()
                    .post(accountParam+userParam)
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().response().asString();

            JsonPath jp = new JsonPath(response);
            Assert.assertEquals(jp.get("message"), "UserName and Password required.");
            System.out.println("RESPONSE: "+response);
        }


    }

    @Test(priority = 120)
    public void cannotCreateUserWithEmptyFields() {
        AccountPayload payload = new AccountPayload();

        for(int i = 0; i < 2; i++) {
            payload.setUserName(accountPayload.setRandomUsername());
            payload.setPassword(accountPayload.setRandomPassword());

            if(i == 0) {
                payload.setUserName("");
            } else {
                payload.setPassword("");
            }
            String response = given()
                    .log().all()
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .when()
                    .post(accountParam+userParam)
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().response().asString();

            JsonPath jp = new JsonPath(response);
            Assert.assertEquals(jp.get("message"), "UserName and Password required.");
        }


    }

    @Test(priority = 130)
    public void passwordValidation() {
        AccountPayload payload = new AccountPayload();
        ArrayList<String> password = new ArrayList<>();
        password.add("123!@#456");
        password.add("Qwerty!@#!@#");
        password.add("Qwerty123456");
        password.add("qwerty123!@#");
        password.add("QWERTY123!@#");
        password.add("Qwe12!@");

        for (int i = 0; i < password.size(); i++) {
            payload.setUserName(payload.setRandomUsername());
            payload.setPassword(password.get(0));
            String response = given()
                    .log().all()
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .when()
                    .post(accountParam+userParam)
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().response().asString();

            JsonPath jp = new JsonPath(response);
            Assert.assertEquals(jp.get("message"), "Passwords must have at least one non alphanumeric character, one digit ('0'-'9'), one uppercase ('A'-'Z'), one lowercase ('a'-'z'), one special character and Password must be eight characters or longer.");
        }


    }

    @Test(priority = 140)
    public void cannotCreatUserWithExistingUser() {
        createUser();
        AccountPayload payload = new AccountPayload();
        payload.setUserName(accountPayload.getUserName());
        payload.setPassword(accountPayload.getPassword());

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(payload)
                .when()
                .post(accountParam+userParam)
                .then()
                .log().all()
                .statusCode(406)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertEquals(jp.get("message"), "User exists!");
    }

    @Test(priority = 150)
    public void createUserWithExistingUsernameButDifferentPassword() {
        createUser();
        AccountPayload payload = new AccountPayload();
        payload.setUserName(accountPayload.getUserName());
        payload.setPassword(accountPayload.setRandomPassword());

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(payload)
                .when()
                .post(accountParam+userParam)
                .then()
                .log().all()
                .statusCode(201)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertNull(jp.get("message"));
    }

    @Test(priority = 160)
    public void cannotAddNonexistingISBN() {
        createToken();

        BookstorePayload payload = new BookstorePayload();
        ArrayList list = new ArrayList<>();
        bookID1.setIsbn(isbn.setRandomISBN());
        list.add(bookID1);
        payload.setUserId(accountPayload.getUserId());
        payload.setCollectionOfIsbns(list);

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .post(bookstoreParam+multipleBooksParam)
                .then()
                .log().all()
                .statusCode(400)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertEquals(jp.get("message"), "ISBN supplied is not available in Books Collection!");

    }

    @Test(priority = 170)
    public void cannotAddISBNToNonexistingUser() {
        BookstorePayload payload = new BookstorePayload();
        ArrayList list = new ArrayList<>();
        bookID1.setIsbn(isbn.setRandomISBN());
        list.add(bookID1);
        payload.setUserId(accountPayload.setRandomUserId());
        payload.setCollectionOfIsbns(list);

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .post(bookstoreParam+multipleBooksParam)
                .then()
                .log().all()
                .statusCode(401)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertEquals(jp.get("message"), "User Id not correct!");
    }

    @Test(priority = 180)
    public void cannotRemoveISBNThatsNotInCollection() {
        createToken();
        getAllBooks();

        BookstorePayload payload = new BookstorePayload();
        payload.setUserId(accountPayload.getUserId());
        payload.setIsbn(getISBN());

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .delete(bookstoreParam+singleBookParam)
                .then()
                .log().all()
                .statusCode(400)
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertEquals(jp.get("message"), "ISBN supplied is not available in User's Collection!");

    }

    @Test(priority = 190)
    public void cannotReplaceWithNonexistingISBN() {
        addBookToUser();

        String book1 = bookID1.getOneIsbn();

        BookstorePayload payload = new BookstorePayload();
        payload.setUserId(accountPayload.getUserId());
        payload.setIsbn(isbn.setRandomISBN());

        String response = given()
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .body(payload)
                .when()
                .put(bookstoreParam+multipleBooksParam+"/"+book1)
                .then()
                .statusCode(400)
                .log().all()
                .extract().response().asString();

        JsonPath jp = new JsonPath(response);
        Assert.assertEquals(jp.get("message"), "ISBN supplied is not available in Books Collection!");

        jp = new JsonPath(getUser(200));
        Assert.assertEquals(jp.get("books[0].isbn"), book1);


    }

}
