package io.github.khangnt.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.github.khangnt.downloader.model.Task;
import io.github.khangnt.downloader.util.Utils;

/**
 * Created by Khang NT on 6/5/17.
 * Email: khang.neon.1997@gmail.com
 */

public class DefaultHttpClient implements HttpClient {
    @Override
    public InputStream openConnection(Task task, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = openConnection(task.getUrl(), headers, "GET");
        return connection.getInputStream();
    }

    @Override
    public long fetchContentLength(Task task) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Range", "bytes=0-");
        try {
            HttpURLConnection connection = openConnection(task.getUrl(), headers, "HEAD");
            String contentLength = connection.getHeaderField("Content-Length");
            if (Utils.isEmpty(contentLength)) return 0;
            else return Long.parseLong(contentLength);
        } catch (IOException ex) {
            Log.d("Can't get content length of task-%d", task.getId());
            return C.UNKNOWN_LENGTH;
        }
    }

    private HttpURLConnection openConnection(String urlStr, Map<String, String> headers,
                                             String method) throws IOException {
        while (true) {
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestMethod(method);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }
            urlConnection.connect();
            switch (urlConnection.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    String location = urlConnection.getHeaderField("Location");
                    URL base = new URL(urlStr);
                    URL next = new URL(base, location);  // Deal with relative URLs
                    urlStr = next.toExternalForm();
                    urlConnection.disconnect();
                    continue;
            }

            if (urlConnection.getResponseCode() / 100 != 2) {
                urlConnection.disconnect();
                throw new IOException("Unsuccessful response code: " + urlConnection.getResponseCode()
                        + " - " + urlConnection.getResponseMessage());
            }
            return urlConnection;
        }
    }
}
