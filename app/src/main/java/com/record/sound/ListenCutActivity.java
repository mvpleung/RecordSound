package com.record.sound;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.record.sound.custom.AppManager;
import com.record.sound.custom.CustomDialog;
import com.record.sound.custom.LoadingDialog;
import com.record.sound.library.inter.ScrollViewListener;
import com.record.sound.library.utils.AudioUtils;
import com.record.sound.library.utils.CheapWAV;
import com.record.sound.library.utils.DateUtils;
import com.record.sound.library.utils.DensityUtil;
import com.record.sound.library.utils.FileUtils;
import com.record.sound.library.utils.SamplePlayer;
import com.record.sound.library.utils.SoundFiles;
import com.record.sound.library.view.ObservableScrollView;
import com.record.sound.library.view.WaveSurfaceView;
import com.record.sound.library.view.WaveformView_1;
import com.record.sound.utils.OtherUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.record.sound.library.utils.DateUtils.formatTime;


public class ListenCutActivity extends Activity implements ScrollViewListener {

    @Bind(R.id.project_line)
    View mProjectLine;
    @Bind(R.id.imgmessage)
    ImageView imgMessage;
    @Bind(R.id.imgback)
    ImageView imgBack;
    @Bind(R.id.title)
    TextView mTitle;
    @Bind(R.id.rtContent)
    TextView rtContent;
    @Bind(R.id.starttime)
    TextView mStartTime;
    @Bind(R.id.endtime)
    TextView mEndTime;
    @Bind(R.id.recording)
    TextView mRecording;
    @Bind(R.id.project_title)
    RelativeLayout mProjectTitle;
    @Bind(R.id.wavesfv)
    WaveSurfaceView mWaveSfv;
    @Bind(R.id.waveview)
    WaveformView_1 mWaveView;
    @Bind(R.id.scrollview)
    ObservableScrollView mScrollView;
    @Bind(R.id.content_layout)
    LinearLayout mContentLayout;
    @Bind(R.id.time_layout)
    LinearLayout mTimeLayout;

    private List<float[]> mCutPosition;
    private List<long[]> mCutPositionTemp = new ArrayList<>();
    private List<long[]> mCutPositionUse = new ArrayList<>();
    private List<long[]> mCutPositionUsed = new ArrayList<>();
    private List<String> cutPaths = new ArrayList<>();
    private List<Integer> mFlagPositionsList = new ArrayList<>();
    private boolean mLoadingKeepGoing;
    private boolean isDeleteAll = false;
    private boolean isPlayed = false;
    private String mFileName;
    private int mTimeCounter = -1;
    private int mTotalLength;
    private int mTotalTime;
    private int width;
    private int height;
    private float mDensity;
    private int mCurrentPosition = 0;
    private File mFile;
    private File mOutFile;
    private String[] mFlagsPositions;
    private String[] mFlagsPositionsSub;

    private LoadingDialog mLoadingDialog;
    private OtherUtils mOtherUtils;
    private Timer mTimerSpeed;
    private Thread mLoadSoundFileThread;
    private SamplePlayer mSamplePlayer;
    private MediaPlayer mMediaPlayer;
    private SoundFiles mSoundFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cut_record);
        mLoadingDialog = LoadingDialog.getInstance(this);
        mOtherUtils = OtherUtils.getInstance(this);
        AppManager.getAppManager().addActivity(ListenCutActivity.this);
        ButterKnife.bind(this);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        Intent intent = getIntent();
        mFileName = intent.getData().toString();
        initControl();
    }

    private Handler myHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    try {
                        mScrollView.scrollTo((mTotalLength * mMediaPlayer.getCurrentPosition()) / mMediaPlayer.getDuration(), 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 10:
                    mOtherUtils.showStringToast("裁剪录音成功");
                    File file_pcm = new File(mOutFile.getAbsolutePath().replace(".wav", ".pcm"));
                    if (mOutFile.exists() && file_pcm.exists()) {
                        File NewWavFile = new File(FileUtils.DATA_DIRECTORY + mFileName.substring(mFileName.lastIndexOf("/")));
                        File NewPcmFile = new File((FileUtils.DATA_DIRECTORY + mFileName.substring(mFileName.lastIndexOf("/"))).replace(".wav", ".pcm"));
                        mOutFile.renameTo(NewWavFile);
                        file_pcm.renameTo(NewPcmFile);
                    }
                    // 需要从新加载界面
                    LoadRecordFile();
                    mWaveView.clearPosition();
                    break;
                case 4:
                    mTotalTime = mWaveView.pixelsToMillisecsTotal() / 1000;
                    TimeSize();
                    break;
                case 100:
                    //切割段全选，则退出当前界面或者取消停留在本页面
                    DeleteAllRecord();
                    break;
                default:
                    break;
            }
        }
    };

    public void initControl() {
        mTitle.setText("裁剪");
        rtContent.setText("清除");
        rtContent.setTextSize(14);
        rtContent.setVisibility(View.VISIBLE);
        rtContent.setTextColor(getResources().getColor(R.color.topic_color));
        mTitle.setTextColor(getResources().getColor(R.color.white));
        mProjectTitle.setBackgroundColor(getResources().getColor(R.color.recording_bg));
        mProjectLine.setVisibility(View.GONE);
        imgMessage.setVisibility(View.GONE);
        imgBack.setImageResource(R.mipmap.login_nav_01);
        mScrollView.setScrollViewListener(this);
        mContentLayout.setPadding(width / 2 - DensityUtil.dip2px(0), 0, width / 2 - DensityUtil.dip2px(0), 0);
        FileUtils.createDirectory();
        if (mWaveSfv != null) {
            mWaveSfv.setLine_off(42);
            //解决surfaceView黑色闪动效果
            mWaveSfv.setZOrderOnTop(true);
            mWaveSfv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
        mWaveView.setLine_offset(42);
        TimeSize();
        LoadRecordFile();
        mTimerCounter.start();
    }

    /**
     * 音频的时间刻度
     */
    private void TimeSize() {
        mStartTime.setText("00:00");
        mEndTime.setText(formatTime(mTotalTime));
        mTimeLayout.removeAllViews();
        mTotalLength = mTotalTime * DensityUtil.dip2px(60);
        mContentLayout.setLayoutParams(new FrameLayout.LayoutParams(mTotalTime * DensityUtil.dip2px(60), LinearLayout.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < mTotalTime; i++) {
            LinearLayout line1 = new LinearLayout(this);
            line1.setOrientation(LinearLayout.HORIZONTAL);
            line1.setLayoutParams(new LinearLayout.LayoutParams(DensityUtil.dip2px(60), LinearLayout.LayoutParams.WRAP_CONTENT));
            line1.setGravity(Gravity.BOTTOM);
            line1.setBackgroundResource(R.mipmap.icon_listen_scale);

            TextView timeText = new TextView(this);
            timeText.setText(DateUtils.formatTime(i));
            timeText.setWidth(DensityUtil.dip2px(60) - 2);
            timeText.setTextSize(10);
            timeText.setPadding(10, 8, 0, 0);
            TextPaint paint = timeText.getPaint();
            paint.setFakeBoldText(false); //字体加粗设置
            timeText.setTextColor(Color.parseColor("#FFFFFF"));
            line1.addView(timeText);
            mTimeLayout.addView(line1);
        }
    }

    /**
     * 载入wav文件显示波形
     */
    private void LoadRecordFile() {
        try {
            Thread.sleep(300);//让文件写入完成后再载入波形 适当的休眠下
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mFile = new File(mFileName);
        mLoadingKeepGoing = true;
        mLoadSoundFileThread = new Thread() {
            public void run() {
                try {
                    mSoundFiles = SoundFiles.create(mFile.getAbsolutePath(), null);
                    if (mSoundFiles == null) {
                        return;
                    }
                    mSamplePlayer = new SamplePlayer(mSoundFiles);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return;
                }
                if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                            mWaveSfv.setVisibility(View.INVISIBLE);
                            mWaveView.setVisibility(View.VISIBLE);
                        }
                    };
                    ListenCutActivity.this.runOnUiThread(runnable);
                }
            }
        };
        mLoadSoundFileThread.start();
    }

    private Thread mTimerCounter = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                TimerTask timerTask_speed = new TimerTask() {
                    @Override
                    public void run() {
                        if (mTimeCounter != -1) {
                            mTimeCounter = mTimeCounter + 1;
                            myHandler.sendEmptyMessage(1);
                        }
                    }
                };
                if (mTimerSpeed == null) {
                    mTimerSpeed = new Timer();
                }
                mTimerSpeed.schedule(timerTask_speed, 0, 10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    /**
     * waveview载入波形完成
     */
    private void finishOpeningSoundFile() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mDensity = metrics.density;
        mWaveView.setSoundFile(mSoundFiles);
        mWaveView.recomputeHeights(mDensity);
        myHandler.sendEmptyMessage(4);
    }

    /**
     * 显示是否进行删除操作的提示
     */
    private void DeleteAllRecord() {
        CustomDialog mCustomDialog = new CustomDialog(ListenCutActivity.this, "确认要全部删除吗？") {

            @Override
            public void EnsureEvent() {
                finish();
            }
        };
        mCustomDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mLoadingDialog != null) {
                    mLoadingDialog.CloseDialog();
                }
            }
        });
        mCustomDialog.setCanceledOnTouchOutside(false);
        mCustomDialog.show();
    }

    @Override
    public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy, boolean s) {
        mWaveView.showSelectArea(true);
        mCurrentPosition = x;
        mScrollView.scrollTo(x, 0);
    }

    /**
     * 开始播放音频文件
     */
    protected void StartPlay() {
        final Resources res = ListenCutActivity.this.getResources();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
            mRecording.setText("播放");
            mMediaPlayer.pause();
            mTimeCounter = -1;
        } else {
            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_suspend), null, null);
            mRecording.setText("暂停");
            if (mMediaPlayer == null) {
                mScrollView.scrollTo(0, 0);
                try {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setDataSource(mFile.getAbsolutePath());
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    mTotalTime = mMediaPlayer.getDuration();
                    mTimeCounter = 0;
                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public synchronized void onCompletion(MediaPlayer arg0) {
                            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
                            mRecording.setText("播放");
                            mScrollView.scrollTo(mTotalLength, 0);
                            mTimeCounter = -1;
                            mMediaPlayer = null;
                        }
                    });
                } catch (IOException e) {
                    mOtherUtils.showStringToast("文件播放出错");
                }
            } else {
                mTimeCounter = 0;
                mMediaPlayer.start();
            }
        }
    }

    /**
     * 音频的裁剪删除操作
     */
    protected void CutVideo() {
        mCutPosition = mWaveView.getCutPosition();
        if (mCutPosition != null && mCutPosition.size() > 0) {
            saveRingtone();
        } else {
            mOtherUtils.showStringToast("请选择删除音频段");
        }
    }

    private void saveRingtone() {
        final String outPath = FileUtils.DATA_DIRECTORY + "/result_" + mFileName.substring(mFileName.lastIndexOf("/") + 1);
        mCutPositionTemp.clear();
        for (int i = 0; i < mCutPosition.size(); i++) {
            long[] temp_fs = new long[2];
            float[] fs = mCutPosition.get(i);
            int pixelsToMillisecsTotal = mWaveView.pixelsToMillisecsTotal();

            double start = fs[0] * pixelsToMillisecsTotal / mTotalLength / 1000;
            double end = fs[1] * pixelsToMillisecsTotal / mTotalLength / 1000;

            temp_fs[0] = mWaveView.secondsToFrames(start);
            temp_fs[1] = mWaveView.secondsToFrames(end);
            mCutPositionTemp.add(temp_fs);
        }

        //start
        if (mFlagsPositions != null) {
            List<Integer> mFlagPositionsSub = new ArrayList<>();
            //倒序遍历集合
            for (int i = mCutPosition.size() - 1; i >= 0; i--) {
                float[] fs = mCutPosition.get(i);
                int pixelsToMillisecsTotal = mWaveView.pixelsToMillisecsTotal();
                //最后的编辑区间
                double start = fs[0] * pixelsToMillisecsTotal / mTotalLength / 1000;
                double end = fs[1] * pixelsToMillisecsTotal / mTotalLength / 1000;
                //清除删除区域的标记点
                for (int j = mFlagsPositions.length - 1; j >= 0; j--) {//必须保证每个元素都要遍历的到
                    boolean temp = false;
                    double pos = (double) Integer.valueOf(mFlagsPositions[j]) / 1000;
                    if ((pos <= end) && (pos >= start)) {
                        mFlagPositionsList.set(j, 0);
                        temp = true;
                    }
                    if (pos > end && !temp) {//在删除区间的右侧（需进行相应时间点的操作运算）
                        mFlagPositionsList.set(j, (int) (mFlagPositionsList.get(j) - (end - start) * 1000));
                    }
                }
            }

            for (int i = 0; i < mFlagPositionsList.size(); i++) {
                if (mFlagPositionsList.get(i) != 0) {
                    mFlagPositionsSub.add(mFlagPositionsList.get(i));
                }
            }
            mFlagPositionsList = mFlagPositionsSub;

            mFlagsPositionsSub = new String[mFlagPositionsList.size()];

            for (int i = 0; i < mFlagPositionsList.size(); i++) {
                mFlagsPositionsSub[i] = mFlagPositionsList.get(i) + "";
            }
            mFlagsPositions = mFlagsPositionsSub;
        }

        mLoadingDialog.createLoadingDialog(ListenCutActivity.this, "操作中");

        new Thread() {
            public void run() {
                mOutFile = new File(outPath);
                try {
                    CheapWAV a = new CheapWAV();
                    a.ReadFile(new File(mFileName));
                    int numFrames = a.getNumFrames();//获取音频文件总帧数
                    mCutPositionUse.clear();

                    //头部开始计算
                    long[] lg_f = new long[2];
                    lg_f[0] = 0;
                    lg_f[1] = 0;
                    mCutPositionUse.add(lg_f);
                    //添加选中的区间
                    for (int i = 0; i < mCutPositionTemp.size(); i++) {
                        mCutPositionUse.add(mCutPositionTemp.get(i));
                    }
                    //最后的区间
                    long[] lg_e = new long[2];
                    lg_e[0] = numFrames;
                    lg_e[1] = numFrames;
                    mCutPositionUse.add(lg_e);

                    mCutPositionUsed.clear();
                    for (int i = 0; i < mCutPositionUse.size(); i++) {
                        if ((i + 1) < mCutPositionUse.size()) {
                            //不超边界
                            if ((mCutPositionUse.get(i + 1)[0] - mCutPositionUse.get(i)[1]) != 0) {
                                //所取区域的帧数不能为0
                                long[] lon = new long[2];
                                lon[0] = mCutPositionUse.get(i)[1];
                                lon[1] = mCutPositionUse.get(i + 1)[0];
                                mCutPositionUsed.add(lon);
                            }
                        }
                    }

                    // 全部删除
                    if (mCutPositionUsed.size() == 0) {
                        myHandler.sendEmptyMessage(100);
                        return;
                    }

                    File out = new File(FileUtils.DATA_DIRECTORY + "/cut_files/");
                    if (!out.exists()) {
                        out.mkdirs();
                    }
                    cutPaths.clear();
                    for (int i = 0; i < mCutPositionUsed.size(); i++) {
                        File outputFile = new File(FileUtils.DATA_DIRECTORY + "/cut_files/" + "cut_" + i + ".wav");
                        cutPaths.add(outputFile.getAbsolutePath());
                        a.WriteFile(outputFile, (int) mCutPositionUsed.get(i)[0],
                                (int) (mCutPositionUsed.get(i)[1] - mCutPositionUsed.get(i)[0]));
                    }

                    File file = new File(mFileName);
                    if (file.exists()) {
                        file.delete();
                    }

                    //合并剪贴的片段文件
                    if (cutPaths.size() > 0) {
                        AudioUtils.mergeAudioFiles(mFileName, cutPaths);
                    }

                    //遍历删除临时文件
                    for (int i = 0; i < cutPaths.size(); i++) {
                        File f = new File(cutPaths.get(i));
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                    //删除文件夹
                    out.delete();
                    cutPaths.clear();
                    myHandler.sendEmptyMessage(10);
                } catch (Exception e) {
                    mLoadingDialog.CloseDialog();
                    e.printStackTrace();
                }
                mLoadingDialog.CloseDialog();
            }
        }.start();
    }

    @OnClick({R.id.imgback, R.id.save, R.id.recording, R.id.cut, R.id.delete_layout, R.id.waveview, R.id.rtContent})
    public void LayoutClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.imgback:
                finish();
                break;
            case R.id.recording:
                isPlayed = true;
                StartPlay();
                break;
            case R.id.cut:
                mWaveView.setCutPosition(mCurrentPosition);
                break;
            case R.id.delete_layout:
                CutVideo();
                break;
            case R.id.waveview:
                mWaveView.showSelectArea(false);
                break;
            case R.id.rtContent:
                mCutPosition = mWaveView.getCutPosition();
                if (mCutPosition != null && mCutPosition.size() > 0) {
                    CustomDialog mCustomDialog = new CustomDialog(ListenCutActivity.this, "确定清除所有音轨切割线吗？") {

                        @Override
                        public void EnsureEvent() {
                            mWaveView.clearPosition();
                            dismiss();
                        }
                    };
                    mCustomDialog.setCanceledOnTouchOutside(false);
                    mCustomDialog.show();
                } else {
                    mOtherUtils.showStringToast("请选择删除音频段");
                }
                break;
            case R.id.save:
                /* intent.setData(Uri.parse(mFile.getAbsolutePath()));
                if (isPlayed) {
                    intent.putExtra("TimeLength", mTotalTime / 1000);
                } else {
                    intent.putExtra("TimeLength", mTotalTime);
                }
                intent.setClass(ListenCutActivity.this, RecordSaveActivity.class);
                startActivity(intent); */
                mOtherUtils.showStringToast("跳转到保存文件页面：" + mFile.getAbsolutePath());
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            Resources res = ListenCutActivity.this.getResources();
            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
            mRecording.setText("播放");
            mMediaPlayer.pause();
        }
    }

    /**
     * activity销毁之前需先销毁播放器
     */
    @Override
    protected void onDestroy() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mWaveView != null) {
            mWaveView.clearPosition();
        }
        // 注销定时器
        mTimerSpeed.cancel();
        mTimerSpeed = null;
        super.onDestroy();
    }

}
