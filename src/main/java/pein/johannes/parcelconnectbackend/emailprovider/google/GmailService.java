package pein.johannes.parcelconnectbackend.emailprovider.google;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class GmailService {

    private final String serverAuthCode = "4/0AY0e-g6Y1YrNJcuyxJ1Ryw9LtgiYKi2Qon8HEExg13DZtbyQNEtHqdPm_vXMBih5V4Uylg";
    private final String userId = "qdF24MeoNyTGp430ZUWSU0ByT0b2";

    public void setUp() throws IOException {

        // Set path to the Web application client_secret_*.json file you downloaded from the
// Google API Console: https://console.developers.google.com/apis/credentials
// You can also find your Web application client ID and client secret from the
// console and specify them directly when you create the GoogleAuthorizationCodeTokenRequest
// object.
        String CLIENT_SECRET_FILE = "/Users/peinjoh/repos/parcel-connect-backend/src/main/resources/client_secret.json";

        // Exchange auth code for access token
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JacksonFactory.getDefaultInstance(), new FileReader(CLIENT_SECRET_FILE));
        GoogleTokenResponse tokenResponse =
                new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        "https://oauth2.googleapis.com/token",
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getClientSecret(),
                        serverAuthCode,
                        "")  // Specify the same redirect URI that you use with your web
                        // app. If you don't have a web version of your app, you can
                        // specify an empty string.
                        .execute();

        String accessToken = tokenResponse.getAccessToken();

        // Use access token to call API
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        Gmail gmail = new Gmail.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("ParcelConnect")
                .build();

        ListMessagesResponse response = gmail.users().messages().list("me").execute();
        System.out.println(response.getMessages().get(0));
    }
}