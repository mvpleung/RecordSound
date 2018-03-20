package com.record.sound.custom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.record.sound.R;
import com.record.sound.utils.PxUtil;


/**
 * 提示确认选择框
 *
 * @author Macthes
 * @description
 * @Date 2014-8-21下午5:48:52
 */

public class CustomDialog extends Dialog {

    private Context mContext;
    private TextView tvContent;
    private String Content;
    private String RightBtn;
    private RelativeLayout out_layout;
    private Button positiveButton, negativeButton;
    private View centerView;

    public CustomDialog(Context mContext, String Content) {
        super(mContext, R.style.DialogStyle);
        initDialog(mContext, Content);
    }

    public CustomDialog(Context mContext, String Content, String RightBtn) {
        super(mContext, R.style.DialogStyle);
        initDialog(mContext, Content, RightBtn);
    }

    /**
     * 初始化Dialog
     */
    private void initDialog(Context mContext, String Content) {
        this.mContext = mContext;
        this.Content = Content;
    }

    /**
     * 初始化Dialog
     */
    private void initDialog(Context mContext, String Content, String RightBtn) {
        this.mContext = mContext;
        this.Content = Content;
        this.RightBtn = RightBtn;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_login_out);
        out_layout = (RelativeLayout) findViewById(R.id.out_layout);
        tvContent = (TextView) findViewById(R.id.message);
        negativeButton = (Button) findViewById(R.id.negativeButton);
        positiveButton = (Button) findViewById(R.id.positiveButton);

        if (centerView != null) {
            out_layout.addView(centerView, 2);
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        int screenWidth = PxUtil.getScreenWidth(mContext);
        lp.width = (int) (screenWidth == 720 ? (screenWidth * 4 / 6) : (screenWidth * 65 / 100)); // 设置宽度
        out_layout.setMinimumHeight(lp.width * 618 / 1000);
        getWindow().setAttributes(lp);

        tvContent.setMinHeight(lp.width * 618 / (screenWidth == 720 ? 2000 : (2 * 1000)));
        tvContent.setMaxHeight(PxUtil.getScreenHeight(mContext) * 2 / 3);
        tvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvContent.setText(String.valueOf(Content));

        if (RightBtn != null && !RightBtn.equals("")) {
            positiveButton.setText(RightBtn);
        }

        // 确定
        positiveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EnsureEvent();
            }
        });

        // 取消
        negativeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void EnsureEvent() {

    }

}

