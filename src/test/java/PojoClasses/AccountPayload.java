package PojoClasses;

import com.github.javafaker.Faker;

public class AccountPayload {
    private String userName;
    private String password;
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    static Faker faker = new Faker();

    public Object accountPayload() {
        AccountPayload payload = new AccountPayload();
        String username = "DragoTest " + faker.name().username();
        String password = faker.regexify("([a-z]){2}([A-Z]){2}([0-9]){2}([@$!%*&]){2}([0-9a-zA-Z]){3}?");
        payload.setUserName(username);
        payload.setPassword(password);
        this.setUserName(username);
        this.setPassword(password);
        return payload;
    }

}
