package com.record.sound.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 应用程序异常类：用于捕获异常和提示错误信息
 *
 * @author
 * @Desc LiangZC Update By 2014-8-8
 * @create 2014-3-17
 */
@SuppressLint("HandlerLeak")
public class AppException extends Exception implements UncaughtExceptionHandler {

    /**
     *
     */
    private static final long serialVersionUID = -6725174445891438953L;

    /**
     * 定义异常类型
     */
    public final static byte EXCEPTION_NETWORK = 0x04;
    public final static byte EXCEPTION_IO = 0x05;
    public final static byte EXCEPTION_TIMEOUT = 0x06;
    public final static byte EXCEPTION_XMLPULL = 0x07;
    public final static byte EXCEPTION_NULL = 0x08;
    public final static byte EXCEPTION_CAST = 0x09;
    public final static byte EXCEPTION_RUNTIME = 0x10;
    public final static byte EXCEPTION_SOCKET = 0x11;
    public final static byte EXCEPTION_HTTP_CODE = 0x12;
    public final static byte EXCEPTION_HTTP_ERROR = 0x13;
    public final static byte EXCEPTION_RUN = 0x14;
    public final static byte EXCEPTION_XML = 0x15;
    public final static byte EXCEPTION_JSON = 0x16;
    /**
     * 服务器异常（请求成功，操作数据失败）
     */
    public final static byte EXCEPTION_SERVER_FAIL = 0x17;
    /**
     * 服务器警告
     */
    public final static byte EXCEPTION_SERVER_WARN = 0x18;
    /**
     * 登录超时
     */
    public final static byte EXCEPTION_SERVER_TIMEOUT = 0x19;

    private byte exceptionType;
    private int code;

    /**
     * 系统默认的UncaughtException处理类
     */
    private UncaughtExceptionHandler mDefaultHandler;

    private AppException() {
        this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public AppException(byte exceptionType, Throwable throwable) {
        super(throwable);
        this.exceptionType = exceptionType;
    }

    public AppException(byte exceptionType, int code, Throwable throwable) {
        super(throwable);
        this.exceptionType = exceptionType;
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    /**
     * 异常类型
     *
     * @return
     */
    public byte getExceptionType() {
        return this.exceptionType;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setExceptionType(byte exceptionType) {
        this.exceptionType = exceptionType;
    }

    /**
     * 获取异常类型处理对象
     *
     * @param throwable
     * @return
     */
    public static AppException convertException(Throwable throwable) {
        byte exType = EXCEPTION_SERVER_FAIL;
        if (throwable instanceof UnknownHostException
                || throwable instanceof ConnectException) {

            exType = EXCEPTION_NETWORK;

        } else if (throwable instanceof SocketTimeoutException
                || throwable instanceof ConnectTimeoutException) {

            exType = EXCEPTION_TIMEOUT;

        } else if (throwable instanceof IOException) {

            exType = EXCEPTION_IO;

        } else if (throwable instanceof NullPointerException) {

            exType = EXCEPTION_NULL;

        } else if (throwable instanceof ClassCastException) {

            exType = EXCEPTION_CAST;

        } else if (throwable instanceof RuntimeException) {

            exType = EXCEPTION_RUNTIME;

        } else if (throwable instanceof XmlPullParserException
                || throwable instanceof IllegalAccessException
                || throwable instanceof InstantiationException
                || throwable instanceof IllegalArgumentException) {

            exType = EXCEPTION_XMLPULL;

        } else if (throwable instanceof JSONException) {

            exType = EXCEPTION_JSON;

        }
        return new AppException(exType, throwable);
    }

    /**
     * 转换异常
     *
     * @param exceptionType 异常类型
     * @param errorMsg      异常信息
     * @return
     */
    public static AppException convertException(byte exceptionType,
                                                String errorMsg) {
        return convertException(new Throwable(errorMsg));
    }

    /**
     * 转换异常
     *
     * @param errorMsg 异常信息
     * @return
     * @params exceptionType
     * 异常类型
     */
    public static AppException convertException(String errorMsg) {
        return convertException(EXCEPTION_SERVER_FAIL, errorMsg);
    }

    /**
     * 转换异常描述
     *
     * @param defaultDesc 默认描述（业务描述）
     * @return
     */
    public String getExceptionDesc(String defaultDesc) {
        String outputString = defaultDesc;
        switch (getExceptionType()) {
            case EXCEPTION_NULL:
            case EXCEPTION_CAST:
            case EXCEPTION_IO:
            case EXCEPTION_RUNTIME:
            case EXCEPTION_XMLPULL:
            case EXCEPTION_JSON:
                break;
            case EXCEPTION_NETWORK:
            case EXCEPTION_TIMEOUT:
                outputString = "网络连接超时";
                break;
            case EXCEPTION_SERVER_WARN:
            case EXCEPTION_SERVER_FAIL:
            case EXCEPTION_SERVER_TIMEOUT:
                String throwMsg = getCause() != null ? getCause().getMessage()
                        : null;
                outputString = !TextUtils.isEmpty(throwMsg) ? throwMsg
                        : defaultDesc;
                break;
        }
        return outputString;
    }

    /**
     * 获取APP异常崩溃处理对象
     *
     * @return
     * @params context
     */
    public static AppException getAppExceptionHandler() {
        return new AppException();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AppManager.getAppManager().AppExit();
        }

    }

    /**
     * 自定义异常处理
     *
     * @param ex
     * @return true:处理了该异常信息;否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            Log.e("APPEXCEPTION", "handleException  ex == null");
            return false;
        }
        // final String message = ex.getMessage();
        final Context context = AppManager.getAppManager().getCurrentActivity();
        if (context == null) {
            return false;
        }
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(context, "页面加载异常，请联系开发人员", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();
        return true;
    }

}