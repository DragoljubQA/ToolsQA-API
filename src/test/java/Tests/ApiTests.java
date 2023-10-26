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
                .log().all()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + variables.getToken())
                .get(accountParam+userParam+"/"+accountPayload.getUserId())
                .then()
                .log().all()
                .statusCode(expectCode)
                .extract().response().asString();
    }

    public void removeUser() {
        if (accountPayload.getUserId() != null && variables.getToken() != null) {
            String response = given()
                    .log().all()
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + variables.getToken())
                    .delete(accountParam+userParam+"/"+accountPayload.getUserId())
                    .then()
                    .log().all()
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
        Isbn bookID = new Isbn();
        ArrayList list = new ArrayList<>();
        bookID.setIsbn(isbn.getIsbn().get(new Random().nextInt(isbn.getIsbn().size())));
        list.add(bookID);
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
    }

    @Test(priority = 70)
    public void addMultipleBooksToUser() {
        createUser();
        createToken();
        getAllBooks();

        BookstorePayload payload = new BookstorePayload();
        Isbn bookID1 = new Isbn();
        Isbn bookID2 = new Isbn();
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
    }





}
