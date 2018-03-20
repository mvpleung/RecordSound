package com.record.sound.library.utils;

/**
 * 时间、日期工具类
 *
 * @author afnasdf
 */
public class DateUtils {

    public static String formatSecond(int second) {
        String html = "0";
        int s = second;
        String format;
        Object[] array;
        Integer hours = (int) (s / (60 * 60));
        Integer minutes = (int) (s / 60 - hours * 60);
        Integer seconds = (int) (s - minutes * 60 - hours * 60 * 60);
        if (hours > 0) {
            if (hours >= 10) {
                html = hours + "";
            } else {
                html = "0" + hours;
            }
        } else {
            html = "00";
        }
        if (minutes > 0) {
            if (minutes >= 10) {
                html = html + ":" + minutes;
            } else {
                html = html + ":0" + minutes;
            }
        } else {
            html = html + ":00";
        }
        if (seconds >= 10) {
            html = html + ":" + seconds;
        } else {
            html = html + ":0" + seconds;
        }
        return html;
    }

    /**
     * 时间格式化
     *
     * @param timeSec
     * @return
     */
    public static String formatTime(int timeSec) {
        if (timeSec < 10) {
            return "00:" + "0" + timeSec;
        }
        if (timeSec >= 10 && timeSec < 60) {
            return "00:" + timeSec;
        }
        if (timeSec >= 60) {
            String str = "";
            int m = timeSec / 60;
            int s = timeSec % 60;
            if (m < 10) {
                str = "0" + m + ":";
            } else {
                str = "" + m + ":";
            }
            if (s < 10) {
                str = str + "0" + s;
            } else {
                str = str + s;
            }
            return str;
        }
        return "";
    }

}
