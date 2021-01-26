package me.drepic.proton;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    private final String ANSI_GREEN = "\u001B[32m";
    private final String ANSI_RESET = "\u001B[0m";

    public UpdateChecker(String version) throws IOException {
        CompletableFuture<String> getRequest = CompletableFuture.supplyAsync(() -> {
            try {
                return getLatestVersion();
            } catch (IOException e) {
                return "";
            }
        });

        getRequest.thenAccept(json -> notifyVersion(json, version));
    }

    private void notifyVersion(String json, String localVersion) {
        if (!json.isEmpty()) {
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();

            if (!obj.has("name")) return;
            String remoteVersionName = obj.get("name").getAsString();
            String remoteVersion = remoteVersionName.startsWith("v") ? remoteVersionName.substring(1) : remoteVersionName;
            
            if (needsUpdate(localVersion, remoteVersion)) {
                Proton.pluginLogger().log(Level.INFO,
                        String.format("New version available for Proton: [v%s] -> [%sv%s%s]", localVersion, ANSI_GREEN, remoteVersion, ANSI_RESET)
                );
            } else {
                Proton.pluginLogger().log(Level.INFO,
                        String.format("Current version is up-to-date: [v%s]", localVersion)
                );
            }
        }

    }

    private boolean needsUpdate(String localVersion, String remoteVersion) {
        String[] localSubversions = localVersion.split("\\.");
        String[] remoteSubversions = remoteVersion.split("\\.");

        int minLen = Math.min(localSubversions.length, remoteSubversions.length);

        for (int i = 0; i < minLen; i++) { //left to right
            int local = Integer.parseInt(localSubversions[i]);
            int remote = Integer.parseInt(remoteSubversions[i]);
            if (remote > local) return true;
        }

        return remoteSubversions.length > localSubversions.length;
    }

    private String getLatestVersion() throws IOException {
        URL url = new URL("https://api.spiget.org/v2/resources/87159/versions/latest");

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode;

        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            return "";
        }

        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();
            return content.toString();
        }

        connection.disconnect();
        return "";

    }

}
