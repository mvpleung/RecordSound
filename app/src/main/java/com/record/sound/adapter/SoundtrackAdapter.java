package com.record.sound.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.record.sound.R;
import com.record.sound.entity.SoundtrackInfo;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by matches on 2018/2/3.
 */

public class SoundtrackAdapter extends BaseAdapter {

    Context mContext;
    List<SoundtrackInfo> mListData;
    LayoutInflater mInflater;

    public SoundtrackAdapter(Context mContext, List<SoundtrackInfo> mListData) {
        this.mContext = mContext;
        this.mListData = mListData;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public Object getItem(int position) {
        return mListData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public SoundtrackInfo getSoundtrackInfo(int position) {
        return mListData.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mInflater.inflate(R.layout.list_soundtrack_text_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        SoundtrackInfo mSoundtrackInfo = mListData.get(position);
        holder.mTitle.setText(mSoundtrackInfo.DisplayName);
        return convertView;
    }

    static class ViewHolder {
        @Bind(R.id.title)
        TextView mTitle;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}
