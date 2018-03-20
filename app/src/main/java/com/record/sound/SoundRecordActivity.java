package com.record.sound;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.record.sound.adapter.SoundtrackAdapter;
import com.record.sound.custom.AppManager;
import com.record.sound.custom.CustomDialog;
import com.record.sound.custom.LoadingDialog;
import com.record.sound.custom.SwitchButton;
import com.record.sound.entity.SoundtrackInfo;
import com.record.sound.library.utils.DateUtils;
import com.record.sound.library.utils.DensityUtil;
import com.record.sound.library.utils.FileUtils;
import com.record.sound.library.utils.Pcm2Wav;
import com.record.sound.library.view.WaveCanvas;
import com.record.sound.library.view.WaveSurfaceView;
import com.record.sound.library.view.WaveformView;
import com.record.sound.utils.OtherUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SoundRecordActivity extends Activity {

    private boolean isRecord = false;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static final int FREQUENCY = 16000;
    // 设置单声道声道
    private static final int CHANNELCONGIFIGURATION = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式：每个样本16位
    private static final int AUDIOENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 音频获取源
    public final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    @Bind(R.id.project_line)
    View mProjectLine;
    @Bind(R.id.imgmessage)
    ImageView imgMessage;
    @Bind(R.id.imgback)
    ImageView imgBack;
    @Bind(R.id.status)
    ImageView mStatus;
    @Bind(R.id.title)
    TextView mTitle;
    @Bind(R.id.time)
    TextView timeCounter;
    @Bind(R.id.tip)
    TextView mTip;
    @Bind(R.id.recording)
    TextView mRecording;
    @Bind(R.id.music_layout)
    RelativeLayout mMusicLayout;
    @Bind(R.id.project_title)
    RelativeLayout mProjectTitle;
    @Bind(R.id.soundtrack_layout)
    RelativeLayout mSoundtrackLayout;
    @Bind(R.id.menu_layout)
    LinearLayout mMenuLayout;
    @Bind(R.id.wavesfv)
    WaveSurfaceView waveSfv;
    @Bind(R.id.waveview)
    WaveformView waveView;
    @Bind(R.id.soundtrack)
    SwitchButton mSoundtrack;
    @Bind(R.id.soundtrack_list)
    ListView mSoundtrackList;
    @Bind(R.id.album_bar)
    SeekBar mAlbumBar;

    private int progress = 7;
    private int mTotalTime = 0;
    private int mTimeCounter = -1;
    // 默认没在录制状态：1、录制状态 2、为暂停装填 3、为录制结束状态
    private int currentStatus = 0;
    private int swidth;
    private int recBufSize;
    // 文件名
    private String mFileName = UUID.randomUUID().toString();
    private boolean isEdit = false;
    private boolean isBiggest = false;
    private boolean isSuspend = false;
    private boolean isMusic = false;

    private LoadingDialog mLoadingDialog;
    private OtherUtils mOtherUtils;
    private AudioManager mAudioManager;
    private AudioRecord mAudioRecord;
    private WaveCanvas mWaveCanvas;
    private MediaPlayer mMediaPlayer;

    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 1://时间记录
                    if (mTimeCounter == -1) {
                        timeCounter.setText("00:00:00");
                    } else {
                        if (DateUtils.formatSecond(mTimeCounter / 1000).equals("01:30:00")) {
                            isBiggest = true;
                            currentStatus = 2;
                            mTotalTime = mTimeCounter;
                            mStatus.setImageResource(R.mipmap.icon_rec);
                            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.icon_record_play), null, null);
                            mRecording.setText("录制暂停");
                            mOtherUtils.showStringToast("已达到最大录制时间");
                            timeCounter.setText(DateUtils.formatSecond(mTimeCounter / 1000));
                            if (mWaveCanvas != null) {
                                mWaveCanvas.Stop();
                                mWaveCanvas.clearMarkPosition();
                                mWaveCanvas = null;
                            }
                        } else {
                            timeCounter.setText(DateUtils.formatSecond(mTimeCounter / 1000));
                        }
                    }
                    break;
                case 2:
                    if (isEdit) {
                        timeCounter.setText(DateUtils.formatSecond(mTotalTime / 1000));
                        isEdit = false;
                    }
                    break;
                case 3:
                    timeCounter.setText(DateUtils.formatSecond(mTotalTime / 1000));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_sound_recording);
        AppManager.getAppManager().addActivity(SoundRecordActivity.this);
        DisplayMetrics metrics = new DisplayMetrics();
        mLoadingDialog = LoadingDialog.getInstance(this);
        mOtherUtils = OtherUtils.getInstance(this);
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        swidth = metrics.widthPixels;
        ButterKnife.bind(this);
        timerCounter.start();
        initControl();
        FileUtils.createDirectory();
        //解决surfaceView黑色闪动效果
        if (waveSfv != null) {
            waveSfv.setLine_off(0);
            waveSfv.setZOrderOnTop(true);
            waveSfv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
        waveView.setLine_offset(0);
    }

    public void initControl() {
        mTitle.setText("录音");
        mTitle.setTextColor(getResources().getColor(R.color.white));
        mProjectTitle.setBackgroundColor(getResources().getColor(R.color.recording_bg));
        mProjectLine.setVisibility(View.GONE);
        imgMessage.setVisibility(View.GONE);
        imgBack.setImageResource(R.mipmap.login_nav_01);
        mSoundtrack.setOpenSwitchColor(getResources().getColor(R.color.topic_color));
        mSoundtrack.setOnStateChangedListener(new SwitchButton.OnStateChangedListener() {
            @Override
            public void toggleToOn() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.start();
                }
            }

            @Override
            public void toggleToOff() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                }
            }
        });
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAlbumBar.setMax(mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC));
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
        mAlbumBar.setProgress(progress);
        mAlbumBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SoundRecordActivity.this.progress = progress;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
            }
        });
    }

    @OnClick({R.id.imgback, R.id.add_music_layout, R.id.recording, R.id.listen, R.id.reset, R.id.cut, R.id.save, R.id.change})
    public void LayoutClick(View view) {
        final Resources res = SoundRecordActivity.this.getResources();
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.imgback:
                if (isMusic) {
                    isMusic = false;
                    waveSfv.setVisibility(View.VISIBLE);
                    mSoundtrackLayout.setVisibility(View.GONE);
                    if (mWaveCanvas != null) {
                        mTitle.setText("正在录制");
                    } else {
                        mTitle.setText("录音");
                    }
                } else {
                    CleanWave();
                    waveSfv.setVisibility(View.GONE);
                    waveView.setVisibility(View.INVISIBLE);
                    finish();
                }
                break;
            case R.id.add_music_layout:
            case R.id.change:
                if (mWaveCanvas != null) {
                    currentStatus = 2;
                    mTotalTime = mTimeCounter;
                    mWaveCanvas.pause();
                    mStatus.setImageResource(R.mipmap.icon_rec);
                    mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
                    mRecording.setText("录制暂停");
                    if (mMediaPlayer != null) {
                        mMediaPlayer.pause();
                    }
                }
                mTitle.setText("选择配音");
                isMusic = true;
                waveSfv.setVisibility(View.GONE);
                mSoundtrackLayout.setVisibility(View.VISIBLE);
                SoundtrackData();
                break;
            case R.id.recording:
                if (!isBiggest) {
                    if (mWaveCanvas == null || !mWaveCanvas.isRecording) {
                        currentStatus = 1;
                        mTimeCounter = 0;
                        mMenuLayout.setVisibility(View.VISIBLE);
                        mRecording.setText("麦克风正在录制中");
                        mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_suspend), null, null);
                        mTitle.setText("正在录制");
                        mStatus.setImageResource(R.mipmap.icon_rec_pre);
                        waveSfv.setVisibility(View.VISIBLE);
                        waveView.setVisibility(View.INVISIBLE);
                        mTitle.setTextColor(getResources().getColor(R.color.white));
                        if (mMediaPlayer != null) {
                            mMediaPlayer.start();
                        }
                        initAudio();
                    } else {
                        switch (currentStatus) {
                            case 1:
                                currentStatus = 2;
                                mTotalTime = mTimeCounter;
                                mWaveCanvas.pause();
                                mStatus.setImageResource(R.mipmap.icon_rec);
                                mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
                                mRecording.setText("录制暂停");
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.pause();
                                }
                                break;
                            case 2:
                                isSuspend = false;
                                currentStatus = 1;
                                mTimeCounter = mTotalTime;
                                mWaveCanvas.reStart();
                                if (!waveSfv.isShown()) {
                                    waveSfv.setVisibility(View.VISIBLE);
                                    waveView.setVisibility(View.INVISIBLE);
                                }
                                mStatus.setImageResource(R.mipmap.icon_rec_pre);
                                mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_suspend), null, null);
                                mRecording.setText("麦克风正在录制中");
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.start();
                                }
                                break;
                        }
                    }
                } else {
                    mOtherUtils.showStringToast("已达到最大录制时间");
                }
                break;
            case R.id.listen:
                intent.setData(Uri.parse(DealFile(0).getAbsolutePath()));
                intent.setClass(SoundRecordActivity.this, ListenRecordActivity.class);
                startActivity(intent);
                break;
            case R.id.reset:
                CustomDialog mCustomDialog = new CustomDialog(SoundRecordActivity.this, "您确定要重新录制节目吗？") {

                    @Override
                    public void EnsureEvent() {
                        isSuspend = false;
                        isBiggest = false;
                        mTimeCounter = -1;
                        currentStatus = 2;
                        waveSfv.setVisibility(View.INVISIBLE);
                        mStatus.setImageResource(R.mipmap.icon_rec);
                        mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
                        mRecording.setText("录制暂停");
                        CleanWave();
                        waveSfv.setVisibility(View.VISIBLE);
                        waveView.setVisibility(View.INVISIBLE);
                        if (mWaveCanvas != null) {
                            mWaveCanvas.Stop();
                            mWaveCanvas.clearMarkPosition();
                            mWaveCanvas = null;
                        }
                        if (mMediaPlayer != null) {
                            mMediaPlayer.stop();
                            mMediaPlayer.seekTo(0);
                        }
                        dismiss();
                    }
                };
                mCustomDialog.setCanceledOnTouchOutside(false);
                mCustomDialog.show();
                break;
            case R.id.cut:
                intent.setData(Uri.parse(DealFile(1).getAbsolutePath()));
                intent.setClass(SoundRecordActivity.this, ListenCutActivity.class);
                startActivity(intent);
                break;
            case R.id.save:
                /* intent.setData(Uri.parse(DealFile(2).getAbsolutePath()));
                mTotalTime = mTimeCounter;
                intent.putExtra("TimeLength", mTotalTime / 1000);
                intent.setClass(SoundRecordActivity.this, RecordSaveActivity.class);
                startActivity(intent); */
                mOtherUtils.showStringToast("跳转到保存文件页面：" + DealFile(2).getAbsolutePath());
                break;
        }
    }

    public void SoundtrackData() {
        mLoadingDialog.createLoadingDialog(SoundRecordActivity.this, "加载音乐中");
        Cursor cursor = null;
        List<SoundtrackInfo> mediaList = null;
        try {
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
                    null, MediaStore.Audio.AudioColumns.IS_MUSIC);
            if (cursor == null) {
                mTip.setVisibility(View.VISIBLE);
                if (mLoadingDialog != null) {
                    mLoadingDialog.CloseDialog();
                }
                mOtherUtils.showStringToast("搜索音乐文件完成，未发现音频文件");
                return;
            }
            int count = cursor.getCount();
            if (count <= 0) {
                mTip.setVisibility(View.VISIBLE);
                if (mLoadingDialog != null) {
                    mLoadingDialog.CloseDialog();
                }
                mOtherUtils.showStringToast("搜索音乐文件完成，未发现音频文件");
                return;
            }
            mediaList = new ArrayList<>();
            SoundtrackInfo mSoundtrackInfo;
            // String[] columns = cursor.getColumnNames();
            while (cursor.moveToNext()) {
                mSoundtrackInfo = new SoundtrackInfo();
                mSoundtrackInfo.Id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                mSoundtrackInfo.Title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                mSoundtrackInfo.DisplayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                mSoundtrackInfo.Duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                mSoundtrackInfo.Size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));

                /* if (!checkIsMusic(mSoundtrackInfo.Duration, mSoundtrackInfo.Size)) {
                    continue;
                } */
                mSoundtrackInfo.Artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                mSoundtrackInfo.Path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                mediaList.add(mSoundtrackInfo);
            }
        } catch (Exception e) {
            if (mLoadingDialog != null) {
                mLoadingDialog.CloseDialog();
            }
            mOtherUtils.showStringToast("加载音乐文件异常，请重试");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (mediaList.size() > 0) {
            mTip.setVisibility(View.GONE);
            mSoundtrackList.setVisibility(View.VISIBLE);
            final SoundtrackAdapter mSoundtrackAdapter = new SoundtrackAdapter(SoundRecordActivity.this, mediaList);
            mSoundtrackList.setAdapter(mSoundtrackAdapter);
            mSoundtrackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    isMusic = false;
                    mSoundtrack.setState(true);
                    mMusicLayout.setVisibility(View.VISIBLE);
                    mSoundtrackLayout.setVisibility(View.GONE);
                    mMediaPlayer = new MediaPlayer();
                    try {
                        mMediaPlayer.setDataSource(mSoundtrackAdapter.getSoundtrackInfo(position).Path);
                        mMediaPlayer.prepare();
                        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mMediaPlayer.stop();
                                mMediaPlayer.release();
                                mMediaPlayer = null;
                            }
                        });
                    } catch (IOException e) {
                        mOtherUtils.showStringToast("配音文件异常，请换一个重试");
                        e.printStackTrace();
                    }
                }
            });
        } else {
            mTip.setVisibility(View.VISIBLE);
            mSoundtrackList.setVisibility(View.GONE);
        }
        if (mLoadingDialog != null) {
            mLoadingDialog.CloseDialog();
        }
    }

    @Override
    public void onBackPressed() {
        if (isMusic) {
            isMusic = false;
            waveSfv.setVisibility(View.VISIBLE);
            mSoundtrackLayout.setVisibility(View.GONE);
            if (mWaveCanvas != null) {
                mTitle.setText("正在录制");
            } else {
                mTitle.setText("录音");
            }
        } else {
            CleanWave();
            waveSfv.setVisibility(View.GONE);
            waveView.setVisibility(View.INVISIBLE);
            finish();
        }
    }

    /**
     * 初始化录音
     */
    private void initAudio() {
        recBufSize = AudioRecord.getMinBufferSize(FREQUENCY,
                CHANNELCONGIFIGURATION, AUDIOENCODING);//设置录音缓冲区(一般为20ms,1280)
        mAudioRecord = new AudioRecord(AUDIO_SOURCE,// 指定音频来源，这里为麦克风
                FREQUENCY, // 16000HZ采样频率
                CHANNELCONGIFIGURATION,// 录制通道
                AUDIO_SOURCE,// 录制编码格式
                recBufSize);
        mWaveCanvas = new WaveCanvas();
        mWaveCanvas.baseLine = waveSfv.getHeight() / 2;
        mWaveCanvas.Start(mAudioRecord, recBufSize, waveSfv, mFileName, FileUtils.DATA_DIRECTORY, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        }, (swidth - DensityUtil.dip2px(10)) / 2, this);
    }

    private Timer TimerSpeed;
    private Thread timerCounter = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                TimerTask timerTask_speed = new TimerTask() {
                    @Override
                    public void run() {
                        if (!isSuspend) {
                            if (mTimeCounter != -1 && currentStatus != 2) {
                                mTimeCounter = mTimeCounter + 100;
                                mHandler.sendEmptyMessage(1);
                                mHandler.sendEmptyMessage(2);
                            }
                        }
                    }
                };
                if (TimerSpeed == null) {
                    TimerSpeed = new Timer();
                }
                TimerSpeed.schedule(timerTask_speed, 0, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (progress == 0) {
                    progress = 0;
                    mOtherUtils.showStringToast("已达到最小音量" + progress);
                    return true;
                }
                --progress;
                mAlbumBar.setProgress(progress);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (progress == 15) {
                    progress = 15;
                    mOtherUtils.showStringToast("已达到最大音量" + progress);
                    return true;
                }
                ++progress;
                mAlbumBar.setProgress(progress);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public File DealFile(int Type) {
        mLoadingDialog.createLoadingDialog(SoundRecordActivity.this);
        String WavString = null;
        if (Type == 0) {
            WavString = FileUtils.DATA_DIRECTORY + "RecordListen.wav";
        } else if (Type == 1) {
            WavString = FileUtils.DATA_DIRECTORY + "RecordCut.wav";
        } else if (Type == 2) {
            WavString = FileUtils.DATA_DIRECTORY + "RecordSave.wav";
        }
        String PcmString = FileUtils.DATA_DIRECTORY + mFileName + ".pcm";
        if (PcmString.length() > 0) {
            try {
                Pcm2Wav p2w = new Pcm2Wav();
                p2w.convertAudioFiles(PcmString, WavString);
            } catch (Exception e) {
                e.printStackTrace();
                mOtherUtils.showStringToast("Pcm转换Wav失败！");
            }
        } else {
            mOtherUtils.showStringToast("你操作的文件不存在！");
        }
        mLoadingDialog.CloseDialog();
        return new File(WavString);
    }

    // 清除时间点
    public void CleanWave() {
        File mFile1 = new File(FileUtils.DATA_DIRECTORY + mFileName + ".wav");
        File mFile2 = new File(FileUtils.DATA_DIRECTORY + mFileName + ".pcm");
        if (mFile1.exists() && mFile2.exists()) {
            mFile1.delete();
            mFile2.delete();
        }
        mHandler.sendEmptyMessage(1);
    }

    @Override
    protected void onPause() {
        if (mWaveCanvas != null) {
            isSuspend = true;
            mWaveCanvas.pause();
            currentStatus = 2;
            mTotalTime = mTimeCounter;
            mStatus.setImageResource(R.mipmap.icon_rec);
            Resources res = SoundRecordActivity.this.getResources();
            mRecording.setCompoundDrawablesWithIntrinsicBounds(null, res.getDrawable(R.mipmap.icon_record_play), null, null);
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWaveCanvas != null) {
            mWaveCanvas.Stop();
            mWaveCanvas.clear();
            mWaveCanvas = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}
