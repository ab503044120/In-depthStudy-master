package com.sun.study.module.okhttp;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.sun.study.module.okhttp.builder.GetBuilder;
import com.sun.study.module.okhttp.builder.OtherRequestBuilder;
import com.sun.study.module.okhttp.builder.PostFileBuilder;
import com.sun.study.module.okhttp.builder.PostFormBuilder;
import com.sun.study.module.okhttp.builder.PostStringBuilder;
import com.sun.study.module.okhttp.callback.OkHttpCallBack;
import com.sun.study.module.okhttp.cookie.OkHttpCookieUtils;
import com.sun.study.module.okhttp.cookie.SimpleCookieJar;
import com.sun.study.module.okhttp.request.RequestCall;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class OkHttpProxy {
    
    public static final String TAG = "log-OkHttp3";
    public static final long DEFAULT_MILLISECONDS = 10000;
    
    private static OkHttpProxy mInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mHandler;

    private boolean debug;
    private String tag;
    
    private OkHttpProxy() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.cookieJar(new SimpleCookieJar());
        mHandler = new Handler(Looper.getMainLooper());

        okHttpClientBuilder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        mOkHttpClient = okHttpClientBuilder.build();
    }

    public static OkHttpProxy getInstance() {
        if (mInstance == null) {
            synchronized (OkHttpProxy.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpProxy();
                }
            }
        }
        return mInstance;
    }

    public OkHttpProxy debug(String tag) {
        debug = true;
        this.tag = tag;
        return this;
    }

    public void setOkHttpClient(OkHttpClient mOkHttpClient) {
        this.mOkHttpClient = mOkHttpClient;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public static GetBuilder get() {
        return new GetBuilder();
    }

    public static PostStringBuilder postString() {
        return new PostStringBuilder();
    }

    public static PostFileBuilder postFile() {
        return new PostFileBuilder();
    }

    public static PostFormBuilder post() {
        return new PostFormBuilder();
    }

    public static OtherRequestBuilder put() {
        return new OtherRequestBuilder(METHOD.PUT);
    }

    public static OtherRequestBuilder head() {
        return new OtherRequestBuilder(METHOD.HEAD);
    }

    public static OtherRequestBuilder delete() {
        return new OtherRequestBuilder(METHOD.DELETE);
    }

    public static OtherRequestBuilder patch() {
        return new OtherRequestBuilder(METHOD.PATCH);
    }

    public void execute(final RequestCall requestCall, OkHttpCallBack callback) {
        if (debug) {
            if (TextUtils.isEmpty(tag)) {
                tag = TAG;
            }
            Log.d(tag, "{method:" + requestCall.getRequest().method() + ", detail:" + requestCall.getOkHttpRequest().toString() + "}");
        }

        if (callback == null)
            callback = OkHttpCallBack.DEFAULT_CALLBACK;
        final OkHttpCallBack finalCallback = callback;

        requestCall.getCall().enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                sendFailResultCallback(call, e, finalCallback);
            }

            @Override
            public void onResponse(final Call call, final Response response) {
                try {
                    if (response.isSuccessful()) {
                        sendSuccessResultCallback(finalCallback.parseResponse(response), finalCallback);
                    } else {
                        sendFailResultCallback(call, new RuntimeException(response.body().string()), finalCallback);
                    }
                } catch (Exception e) {
                    sendFailResultCallback(call, e, finalCallback);
                }
            }
        });
    }

    public void sendFailResultCallback(final Call call, final Exception e, final OkHttpCallBack callback) {
        if (callback == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(call, e);
            }
        });
    }

    public void sendSuccessResultCallback(final Object object, final OkHttpCallBack callback) {
        if (callback == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(object);
            }
        });
    }

    public void cancelTag(Object tag) {
        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }

    public void setCertificates(InputStream... certificates) {
        if (certificates == null) return;
        mOkHttpClient = getOkHttpClient().newBuilder()
                .sslSocketFactory(OkHttpCookieUtils.getSslSocketFactory(certificates, null, null))
                .build();
    }

    public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
        mOkHttpClient = getOkHttpClient().newBuilder()
                .hostnameVerifier(hostNameVerifier)
                .build();
    }

    public void setConnectTimeout(int timeout, TimeUnit units) {
        mOkHttpClient = getOkHttpClient().newBuilder()
                .connectTimeout(timeout, units)
                .build();
    }

    public static class METHOD {
        public static final String HEAD = "HEAD";
        public static final String DELETE = "DELETE";
        public static final String PUT = "PUT";
        public static final String PATCH = "PATCH";
    }

}

