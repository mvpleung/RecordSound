package com.record.sound.custom;

import android.app.Activity;
import android.app.Application;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Created by Administrator on 2016/4/27.
 */
public class AppManager {

    private Stack<Activity> activityStack;

    private WeakReference<Activity> mActivityReference;

    private Application mApplication;

    private AppManager() {

    }

    private static class AppManagerHolder {
        static AppManager instance = new AppManager();
    }

    /**
     * 单一实例
     */
    public static AppManager getAppManager() {
        return AppManagerHolder.instance;
    }

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<Activity>();
        }
        activityStack.add(activity);
        mActivityReference = new WeakReference<Activity>(activity);
    }

    /**
     * 获取Application
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Application> T getApplication() {
        if (mApplication == null) {
            Activity mActivity = getCurrentActivity();
            mApplication = mActivity != null ? mActivity.getApplication() : null;
        }
        return (T) mApplication;
    }

    /**
     * 获取当前Activity(获取堆栈中最后一个压入的)
     */
    public Activity getCurrentActivity() {
        if (activityStack == null)
            activityStack = new Stack<Activity>();
        if (activityStack.size() > 0)
            return activityStack.get(activityStack.size() - 1);
        else
            return null;
    }

    /**
     * 当前Activity的引用
     *
     * @return
     */
    public Activity getActivityReference() {
        return mActivityReference != null ? mActivityReference.get() : getCurrentActivity();
    }

    /**
     * 获取当前正在运行的
     *
     * @return
     */
    public Activity getRunningActivity() {
        return mActivityReference != null ? mActivityReference.get() : null;
    }

    /**
     * 获取当前正在运行的activity
     *
     * @return
     */
   /* public ComponentName getRunningComponentName() {
        ActivityManager manager = (ActivityManager) getCurrentActivity().getSystemService(Context.ACTIVITY_SERVICE);
        RunningTaskInfo info = manager.getRunningTasks(1).get(0);
        return info.topActivity;
        // String shortClassName = info.topActivity.getShortClassName(); // 类名
        // String className = info.topActivity.getClassName(); // 完整类名
        // String packageName = info.topActivity.getPackageName(); // 包名
    }*/

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public void finishActivity() {
        if (activityStack != null && !activityStack.isEmpty())
            finishActivity(activityStack.lastElement());
    }

    /**
     * 结束指定Activity后的所有Actiivty
     */
    public void finishActivityByPosition(Activity activity) {
        if (activity == null || activityStack == null || activityStack.isEmpty())
            return;
        List<Activity> activities = null;
        for (int i = activityStack.size() - 1; i > 0; i--) {
            Activity mActivity = activityStack.get(i);
            if (mActivity != null) {
                if (mActivity.getClass().equals(activity.getClass())) {
                    break;
                }
                if (!mActivity.isFinishing())
                    mActivity.finish();
                if (activities == null)
                    activities = new ArrayList<Activity>();
                activities.add(mActivity);
            }
        }
        if (activities != null)
            activityStack.removeAll(activities);
    }

    /**
     * 结束指定Activity后的所有Actiivty
     */
    public void finishActivityByPosition(Class<?> cls) {
        if (cls == null || activityStack == null || activityStack.isEmpty())
            return;
        List<Activity> activities = null;
        for (int i = activityStack.size() - 1; i > 0; i--) {
            Activity mActivity = activityStack.get(i);
            if (mActivity != null) {
                if (mActivity.getClass().equals(cls)) {
                    break;
                }
                if (!mActivity.isFinishing())
                    mActivity.finish();
                if (activities == null)
                    activities = new ArrayList<Activity>();
                activities.add(mActivity);
            }
        }
        if (activities != null)
            activityStack.removeAll(activities);
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            if (activityStack != null) {
                activityStack.remove(activity);
            }
            if (!activity.isFinishing())
                activity.finish();
            activity = null;
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(Class<?> cls) {
        if (activityStack == null || activityStack.isEmpty())
            return;
        Iterator<Activity> mIterators = activityStack.iterator();
        while (mIterators.hasNext()) {
            Activity mActivity = mIterators.next();
            if (mActivity.getClass().equals(cls)) {
                if (!mActivity.isFinishing())
                    mActivity.finish();
                mIterators.remove();
            }
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        if (activityStack != null)
            while (!activityStack.isEmpty()) {
                Activity activity = activityStack.pop();
                if (activity == null) {
                    break;
                }
                if (!activity.isFinishing())
                    activity.finish();
            }
    }

    long exitTime = 0;

    /**
     * 退出提示
     *
     * @param context
     */
   /* public void AppExitPrompt(Context context) {
        AppExitPrompt(context, R.string.app_exit_tips);
    }*/

    /**
     * 退出提示
     *
     * @param context
     * @param resId   提示文本资源ID
     */
   /* public void AppExitPrompt(Context context, int resId) {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            UITools.showToastShortDuration(context, R.string.app_exit_tips);
            exitTime = System.currentTimeMillis();
        } else {
            AppExit(context);
        }
    }*/

    /**
     * 退出应用程序
     */
   public void AppExit() {
       // AppExit(getCurrentActivity());
    }

    /**
     * 退出应用程序
     */
    /*@SuppressWarnings("deprecation")
    public void AppExit(Context context) {
        try {
            UITools.dismissLoading();
            finishAllActivity();
            if (!AppConfig.DEBUG_MODEL)
                context.stopService(new Intent(context, LogService.class));
            ActivityManager activityMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startMain);
            } else {// android2.1
                activityMgr.restartPackage(context.getPackageName());
            }
        } catch (Exception e) {
          //  LogUtil.e("AppExit Exception : \n" + e.getMessage(), e);
        } finally {
            if (activityStack != null) {
                activityStack.clear();
                activityStack = null;
            }
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        System.exit(0);
    }*/
}
