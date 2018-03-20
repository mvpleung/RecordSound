package com.record.sound.custom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

import com.record.sound.R;


/**
 * 可自定义边界的TextView（包含标题和子标题）
 *
 * @author LiangZiChao
 * @Data 2015年6月12日
 * @Package net.gemeite.smartcommunity.widget
 */
public class CustomTextView extends CheckedTextView {

    final int Default_TextSize = 15, Default_SubTextSize = 12, Default_Zero = 0;

    boolean mDrawableCenter;

    /**
     * 图片宽度，图片高度，标题字号，子标题字号
     */
    int mBoundWidth, mBoundHeight, mTextSize, mSubTextSize;

    /**
     * 标题颜色，子标题颜色
     */
    ColorStateList mTextColor, mSubTextColor;

    /**
     * check
     */
    int mCheckMarkBound;

    /**
     * 标题，子标题
     */
    CharSequence mText, mSubText;

    Drawable[] mDrawables;

    public CustomTextView(Context context) {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        mDrawableCenter = a.getBoolean(R.styleable.CustomTextView_drawableCenter, false);
        mBoundWidth = a.getDimensionPixelSize(R.styleable.CustomTextView_boundWidth, getWidth());
        mBoundHeight = a.getDimensionPixelSize(R.styleable.CustomTextView_boundHeight, getHeight());
        mTextSize = a.getDimensionPixelSize(R.styleable.CustomTextView_android_textSize, Default_TextSize);
        mSubTextSize = a.getDimensionPixelSize(R.styleable.CustomTextView_subTextSize, Default_SubTextSize);
        mText = a.getText(R.styleable.CustomTextView_android_text);
        mSubText = a.getText(R.styleable.CustomTextView_subText);
        mTextColor = a.getColorStateList(R.styleable.CustomTextView_android_textColor);
        mSubTextColor = a.getColorStateList(R.styleable.CustomTextView_subTextColor);
        mCheckMarkBound = a.getDimensionPixelSize(R.styleable.CustomTextView_checkMarkBound, Default_Zero);
        setText(mText, mSubText);
        setCheckMark(a.getDrawable(R.styleable.CustomTextView_android_checkMark));
        mDrawables = getCompoundDrawables();
        initCompoundDrawable();
        a.recycle();
    }

    public void initCompoundDrawable() {
        if (mDrawables != null && mDrawables.length > 0 && mBoundWidth != Default_Zero && mBoundHeight != Default_Zero) {
            for (Drawable drawable : mDrawables) {
                if (drawable != null)
                    drawable.setBounds(0, 0, mBoundWidth, mBoundHeight);
            }
            setCompoundDrawables(mDrawables[0], mDrawables[1], mDrawables[2], mDrawables[3]);
        }
    }

    /**
     * set Drawalbe
     *
     * @params mDrawables
     * [left,top,right,bottom]
     */
    public void setCompoundDrawables(Drawable[] mDrawables, int mBoundWidth, int mBoundHeight) {
        this.mBoundWidth = mBoundWidth;
        this.mBoundHeight = mBoundHeight;
        this.mDrawables = mDrawables;
        initCompoundDrawable();
    }

    /**
     * CompoundDrawableTop
     *
     * @params mDrawable
     * @params mCheckMarkWidth
     * @params mBoundHeight
     */
    public void setCompoundDrawableTop(Drawable mDrawable, int mBoundWidth, int mBoundHeight) {
        if (mDrawables == null)
            mDrawables = getCompoundDrawables();
        this.mBoundWidth = mBoundWidth;
        this.mBoundHeight = mBoundHeight;
        mDrawable.setBounds(0, 0, mBoundWidth, mBoundHeight);
        setCompoundDrawables(mDrawables[0], mDrawable, mDrawables[2], mDrawables[3]);
    }

    /**
     * CompoundDrawableLeft
     *
     * @params mDrawable
     * @params mCheckMarkWidth
     * @params mCheckMarkHeight
     */
    public void setCompoundDrawableLeft(Drawable mDrawable, int mBoundWidth, int mBoundHeight) {
        if (mDrawables == null)
            mDrawables = getCompoundDrawables();
        this.mBoundWidth = mBoundWidth;
        this.mBoundHeight = mBoundHeight;
        mDrawable.setBounds(0, 0, mBoundWidth, mBoundHeight);
        setCompoundDrawables(mDrawable, mDrawables[1], mDrawables[2], mDrawables[3]);
    }

    /**
     * CompoundDrawableRight
     *
     * @params mDrawable
     * @params mCheckMarkWidth
     * @params mCheckMarkHeight
     */
    public void setCompoundDrawableRight(Drawable mDrawable, int mBoundWidth, int mBoundHeight) {
        if (mDrawables == null)
            mDrawables = getCompoundDrawables();
        this.mBoundWidth = mBoundWidth;
        this.mBoundHeight = mBoundHeight;
        mDrawable.setBounds(0, 0, mBoundWidth, mBoundHeight);
        setCompoundDrawables(mDrawables[0], mDrawables[1], mDrawable, mDrawables[3]);
    }

    /**
     * CompoundDrawableBottom
     *
     * @params mDrawable
     * @params mCheckMarkWidth
     * @params mCheckMarkHeight
     */
    public void setCompoundDrawableBottom(Drawable mDrawable, int mBoundWidth, int mBoundHeight) {
        if (mDrawables == null)
            mDrawables = getCompoundDrawables();
        this.mBoundWidth = mBoundWidth;
        this.mBoundHeight = mBoundHeight;
        mDrawable.setBounds(0, 0, mBoundWidth, mBoundHeight);
        setCompoundDrawables(mDrawables[0], mDrawables[1], mDrawables[2], mDrawable);
    }

    /**
     * DrawableCenter
     *
     * @params mDrawableCenter
     */
    public void setDrawableCenter(boolean mDrawableCenter) {
        this.mDrawableCenter = mDrawableCenter;
    }

    /**
     * DrawableCenter
     *
     * @params mDrawableCenter
     */
    public boolean isDrawableCenter() {
        return this.mDrawableCenter;
    }

    /**
     * 设置文本
     *
     * @params mText
     */
    public void setText(CharSequence mText, CharSequence mSubText) {
        if (!TextUtils.isEmpty(mText) && !TextUtils.isEmpty(mSubText)) {
            SpannableString mSpannableString = new SpannableString(mText + "\n" + mSubText);
            if (!TextUtils.isEmpty(mText) && mTextSize > Default_Zero)
                mSpannableString.setSpan(new AbsoluteSizeSpan(mTextSize), 0, mText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!TextUtils.isEmpty(mSubText) && mSubTextSize > Default_Zero)
                mSpannableString.setSpan(new AbsoluteSizeSpan(mSubTextSize), mText.length(), mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (mTextColor != null) {
                mSpannableString.setSpan(new ForegroundColorSpan(mTextColor.getColorForState(getDrawableState(), 0)), 0, mText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (mSubTextColor != null) {
                mSpannableString.setSpan(new ForegroundColorSpan(mSubTextColor.getColorForState(getDrawableState(), 0)), mText.length(), mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            setText(mSpannableString);
        }
    }

    /**
     * 设置复选框
     *
     * @params mDrawable
     */
    public void setCheckMark(Drawable mDrawable) {
        if (mDrawable != null && mCheckMarkBound > 0) {
            mDrawable.setBounds(0, 0, mCheckMarkBound, mCheckMarkBound);
            setCheckMarkDrawable(mDrawable);
        }
    }

    /**
     * 设置复选框
     *
     * @params mDrawable
     */
    public void setCheckMark(int mResId) {
        setCheckMark(getResources().getDrawable(mResId));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawableCenter) {
            Drawable[] drawables = getCompoundDrawables();
            if (drawables != null) {
                Drawable drawable = drawables[0];
                if (drawable != null) {
                    float textWidth = getPaint().measureText(getText().toString());
                    int drawablePadding = getCompoundDrawablePadding();
                    int drawableWidth = 0;
                    drawableWidth = drawable.getIntrinsicWidth();
                    float bodyWidth = textWidth + drawableWidth + drawablePadding;
                    canvas.translate((getWidth() - bodyWidth) / 2, 0);
                }
                drawable = drawables[2];
                if (drawable != null) {

                    float textWidth = getPaint().measureText(getText().toString());
                    int drawablePadding = getCompoundDrawablePadding();
                    int drawableWidth = 0;
                    drawableWidth = drawable.getIntrinsicWidth();
                    float bodyWidth = textWidth + drawableWidth + drawablePadding;
                    setPadding(0, 0, (int) (getWidth() - bodyWidth), 0);
                    canvas.translate((getWidth() - bodyWidth) / 2, 0);
                }
                drawable = drawables[1];
                if (drawable != null) {

                    int drawablePadding = getCompoundDrawablePadding();
                    int drawableHeight = drawable.getIntrinsicHeight();
                    float bodyHeight = drawableHeight + drawablePadding;
                    float translateY = (getHeight() - bodyHeight) / 3;
                    canvas.translate(0, translateY);
                    setPadding(0, 0, 0, (int) (getHeight() - translateY + bodyHeight));
                }
            }
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int getAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * 是否超出
     *
     * @return
     */
    public boolean isOverFlowed() {
        Paint mPaint = getPaint();
        float width = mPaint.measureText(getText().toString());
        if (width > getAvailableWidth())
            return true;
        return false;
    }
}
