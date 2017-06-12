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
    public ContentDescription fetchContentDescription(Task task) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Range", "bytes=0-");
        try {
            HttpURLConnection connection = openConnection(task.getUrl(), headers, "HEAD");
            String contentLength = connection.getHeaderField("Content-Length");
            boolean acceptRange = false;
            long length;
            if (Utils.isEmpty(contentLength)) {
                length = 0;
            } else {
                length = Long.parseLong(contentLength);
            }
            if (length > 0) {
                String acceptRangeHeader = connection.getHeaderField("Accept-Ranges");
                if (acceptRangeHeader == null) {
                    // accept-range is not presented --> check Content-Range header
                    acceptRange = connection.getHeaderField("Content-Range") != null;
                } else {
                    acceptRange = !acceptRangeHeader.trim().equalsIgnoreCase("none");
                }
            }
            return new ContentDescription(length, acceptRange);
        } catch (IOException ex) {
            Log.d("Can't get content length of task-%d", task.getId());
            return new ContentDescription(C.UNKNOWN_LENGTH, false);
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
