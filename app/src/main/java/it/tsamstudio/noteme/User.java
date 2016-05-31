package it.tsamstudio.noteme;

import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;

import it.tsamstudio.noteme.utils.NoteMeUtils;

/**
 * Created by damiano on 31/05/16.
 */
public class User {

    private static User userInstance;

    public static User getInstance() {
        if (userInstance == null) {
            userInstance = new User();
        }
        return userInstance;
    }

    private String username;
    private String password;

    public void initWithCredentials(String username, String password) {
        this.username = username;
        try {
            this.password = AESCrypt.encrypt(NoteMeUtils.AES_PASSWORD_KEY, password);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to encrypt password. " + e.getMessage());
        }
    }

    public static void destroySharedInstance() {
        userInstance = null;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
