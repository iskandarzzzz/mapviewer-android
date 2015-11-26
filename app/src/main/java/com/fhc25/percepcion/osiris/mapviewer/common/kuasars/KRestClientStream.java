package com.fhc25.percepcion.osiris.mapviewer.common.kuasars;

import com.fhc25.percepcion.osiris.mapviewer.common.log.Lgr;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;


/**
 * Performs all the requests to backend via REST, but returns a InputStream
 */
public class KRestClientStream {

    private static final String TAG = KRestClientStream.class.getCanonicalName();

    private static int mResponseCode = 0;
    private static String mMessage = "";

    private static int CONNECTION_TIMEOUT = 10000;
    private static int SOCKETCONNECTION_TIMEOUT = 15000;

    public enum RequestMethod {
        GET
    }

    public static void Execute(final RequestMethod method,
                               final String url,
                               final List<NameValuePair> headers,
                               final List<NameValuePair> params,
                               final KRestListenerStream listener) throws Exception {
        new Thread() {

            @Override
            public void run() {
                switch (method) {
                    case GET: {
                        // add parameters
                        String combinedParams = "";
                        if (params != null) {
                            combinedParams += "?";
                            for (NameValuePair p : params) {

                                String paramString = "";
                                try {
                                    paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), HTTP.DEFAULT_CONTENT_CHARSET);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                if (combinedParams.length() > 1)
                                    combinedParams += "&" + paramString;
                                else
                                    combinedParams += paramString;
                            }
                        }
                        HttpGet request = new HttpGet(url + combinedParams);
                        // add headers
                        if (headers != null) {
                            for (NameValuePair h : headers)
                                request.addHeader(h.getName(), h.getValue());
                        }
                        executeRequest(request, url, listener);
                        break;
                    }
                }
            }

        }.start();
    }

    /**
     * Executes the rest request.
     *
     * @param request  the request to be performed.
     * @param url      the url where to request.
     * @param listener a listener for callbacks.
     *                 <br>Return values are: successful response, http error response code, server timeout (508) and
     *                 internal error (-1).
     */
    private static void executeRequest(HttpUriRequest request, String url, KRestListenerStream listener) {
        /*
        HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, Kuasars.getConnectionTimeout());
		HttpConnectionParams.setSoTimeout(httpParameters, Kuasars.getSocketTimeout());
		HttpClient client = new DefaultHttpClient(httpParameters);
		*/
        HttpClient client = getHttpClient();
        // HttpResponse httpResponse;

        serverConnection(client, request, listener);

        /*
        if (Utils.isOnlineNoToast(Kuasars.getContext())) {
            serverConnection(client, request, listener);
        } else {
            try {
                Thread.currentThread();
                Thread.sleep(2000);
                Lgr.v(TAG, "waits 2 seconds");
                if (Utils.isOnlineNoToast(Kuasars.getContext())) {
                    serverConnection(client, request, listener);
                } else {
                    Thread.currentThread();
                    Thread.sleep(5000);
                    Lgr.v(TAG, "waits 5 seconds");
                    if (Utils.isOnlineNoToast(Kuasars.getContext())) {
                        serverConnection(client, request, listener);
                    } else {
                        listener.onConnectionFailed();
                        //listener.onError(new KError(-1,1,"Connection Error"));
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                listener.onError(new KError(-1, 2, e.getMessage()));
            }
        }
        */

    }

    private static void serverConnection(HttpClient client, HttpUriRequest request, KRestListenerStream listener) {
        try {
            HttpResponse httpResponse = client.execute(request);
            mResponseCode = httpResponse.getStatusLine().getStatusCode();
            mMessage = httpResponse.getStatusLine().getReasonPhrase();
            HttpEntity entity = httpResponse.getEntity();

            if (mResponseCode >= 200 && mResponseCode <= 299) {
                InputStream isResponse = null;
                if (entity != null) {
                    isResponse = entity.getContent();
                }
                listener.onComplete(isResponse);
            } else {
                String errorText = convertStreamToString(entity.getContent());
                Lgr.e(TAG, errorText);
                KError error = null;
                try {
                    error = new KError(errorText);
                } catch (JSONException je) {
                    error = new KError(-1, 3, "Malformed response");
                }
                listener.onError(error);
            }
            Lgr.v(TAG, "ResponseCode: " + mResponseCode);
        } catch (ConnectTimeoutException e) {
            //e.printStackTrace();
            //listener.onError(new KError(508, 0, e.getMessage()));
            listener.onConnectionFailed();
        } catch (SocketTimeoutException e) {
            //e.printStackTrace();
            //listener.onError(new KError(508, 0, e.getMessage()));
            listener.onConnectionFailed();
        } catch (UnknownHostException e) {
            Lgr.e(TAG, e.getMessage());
            listener.onConnectionFailed();
        } catch (IOException e) {
            Lgr.e(TAG, e.getMessage());
            listener.onConnectionFailed();
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(new KError(-1, 0, e.getMessage()));
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        int lineCount = 0;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
                lineCount++;
            }
            if (lineCount == 1) {
                sb.deleteCharAt(sb.length() - 1);
            }
            is.close();

        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static HttpClient getHttpClient() {
        try {

            HttpParams params = new BasicHttpParams();

            // Turn off stale checking.  Our connections break all the time anyway,
            // and it's not worth it to pay the penalty of checking every time.
            HttpConnectionParams.setStaleCheckingEnabled(params, false);

            // Default connection and socket timeout of 20 seconds.  Tweak to taste.
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, SOCKETCONNECTION_TIMEOUT);
            HttpConnectionParams.setSocketBufferSize(params, 8192);

            // Don't handle redirects -- return them to the caller.  Our code
            // often wants to re-POST after a redirect, which we must do ourselves.
            HttpClientParams.setRedirecting(params, false);

            SSLSocketFactory mySSLSocketFactory = SSLSocketFactory.getSocketFactory();

            // disable ssl check on debug
            /*
            if (DisableSSLcertificateCheck ) {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                mySSLSocketFactory = new MySSLSocketFactory(trustStore);
                HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
                mySSLSocketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
            }
            */

            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(new Scheme("https", mySSLSocketFactory, 443));
            ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);

            return new DefaultHttpClient(manager, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

}
