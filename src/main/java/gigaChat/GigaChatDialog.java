package gigaChat;

import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GigaChatDialog {

    private static final String URL_GET_ACCESS_TOKEN = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String SCOPE = System.getenv("scope");
    private static final String AUTHORIZATION_KEY = System.getenv("authKey");
    private static final String CERT_PATH = "src/main/resources/certs/russian_trusted_root_ca.cer";
    private static final String API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static String ACCESS_TOKEN;


    private OkHttpClient client;

    public GigaChatDialog() {
        try {
            this.client = createClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OkHttpClient createClient() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream certInput = new FileInputStream(CERT_PATH);
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(certInput);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("caCert", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        RequestBody formBody = new FormBody.Builder()
                .add("scope", SCOPE)
                .build();

        // Создание OkHttpClient с SSL-сертификатом
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .build();

        Request request = new Request.Builder()
                .url(URL_GET_ACCESS_TOKEN)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .addHeader("RqUID", UUID.randomUUID().toString())
                .addHeader("Authorization", "Basic " + AUTHORIZATION_KEY)
                .build();


        // Выполнение запроса
        try (Response response = client.newCall(request).execute()) {

            // Обработка ответа
            if (response.isSuccessful() && response.body() != null) {
//                System.out.println("Access Token: " + response.body().string());
                ACCESS_TOKEN = parseToken(response.body().string());
//                System.out.println("Access Token: " + ACCESS_TOKEN);
            } else {
                System.out.println("Response body: " + (response.body() != null ? response.body().string() : "No response body"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String parseToken(String string) {
        for (String str : string.split(",")) {
            if (str.contains("access_token")) {
                String[] split = str.split(":");
               return split[1].substring(1, split[1].length()-1);
            }
        }
        return null;
    }

    public String getResponse(String message) {
        String jsonPayload = "{ \"model\": \"GigaChat\", \"messages\": [{ \"role\": \"user\", \"content\": \"" + message + "\" }], \"stream\": false }";
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));


        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + ACCESS_TOKEN)
                .addHeader("Content-Type", "application/json")
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return new String(response.body().bytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
