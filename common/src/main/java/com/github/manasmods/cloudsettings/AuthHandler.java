package com.github.manasmods.cloudsettings;

import com.github.manasmods.cloudsettings.cloudservice.CloudSettingsApi;
import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.util.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthHandler {
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private String authKey = null;

    public AuthHandler() {}

    public boolean isLoggedIn() {
        return getAuthKey() != null;
    }

    public void login(String userId) {
        // Check if we are already logged in
        if (isLoggedIn()) return;
        // Exit since mod is disabled
        if (!CloudSettings.isEnabled()) return;
        // Try to log in with stored key
        if (autoLogin()) return;
        String token = loginLoop(userId);
        if (token == null || token.startsWith(" ")) return;
        Constants.logger.info("Log in successful");
        this.authKey = token;
        writeAutoLoginFile();
    }

    private String loginLoop(String userId) {
        // Request password
        String password = AuthenticationWindow.requestPassword();
        // Exit since mod is disabled
        if (password == null) return null;
        // Try to log in
        String token = CloudSettingsApi.login(Minecraft.getInstance().getUser().getUuid(), password);
        if (token == null) loginLoop(userId);
        return token;
    }

    private boolean autoLogin() {
        if (!CloudSettings.getLoginKeyFile().exists()) return false;
        try (BufferedReader keyFileReader = new BufferedReader(new FileReader(CloudSettings.getLoginKeyFile()))) {
            String storedToken = keyFileReader.readLine();
            Constants.logger.info("Loaded Key {} from storage", storedToken);
            String token = CloudSettingsApi.autoLogin(storedToken);
            if (token != null) {
                Constants.logger.info("Auto Log in was successful");
                this.authKey = token;
                writeAutoLoginFile();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Constants.logger.info("Auto log in failed.");
        return false;
    }

    private void writeAutoLoginFile() {
        // Create parent dir
        if (CloudSettings.getLoginKeyFile().getParentFile().mkdirs()) Constants.logger.info("Created storage directory");
        // Remove old file
        if (CloudSettings.getLoginKeyFile().exists()) {
            if (CloudSettings.getLoginKeyFile().delete()) {
                Constants.logger.info("Deleted old auto log in file");
            }
        }
        // Create new file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CloudSettings.getLoginKeyFile()))) {
            writer.write(this.authKey);
            Constants.logger.info("Created new auto log in file");
        } catch (IOException e) {
            if (CloudSettings.getLoginKeyFile().exists()) {
                if (CloudSettings.getLoginKeyFile().delete()) {
                    Constants.logger.info("Deleted old auto log in file.", e);
                }
            }
        }
    }

    @Nullable
    public HttpGet authorizeGetRequest(String path) {
        if (!isLoggedIn()) return null;
        HttpGet request = new HttpGet(Constants.CLOUD_SERVER + "/cloudsettings/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", this.authKey);
        return request;
    }

    @Nullable
    public HttpPost authorizedPostRequest(String path) {
        if (!isLoggedIn()) return null;
        HttpPost request = new HttpPost(Constants.CLOUD_SERVER + "/cloudsettings/" + path);
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        request.addHeader("user-token", this.authKey);
        request.addHeader("Content-type", "application/json");
        return request;
    }

    public void logout() {
        setAuthKey(null);
        Constants.logger.info("Logged out");
    }
}
