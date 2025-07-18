package com.szxm.av.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.szxm.av.R;
import com.szxm.av.bean.AudioToneData;
import com.szxm.av.bean.BaseCard;
import com.szxm.av.bean.CardEntity;


public class AudioToneCard extends BaseCard {
    private ImageView mIvAudioTone;
    private ImageView mIvAudioToneTag;
    private TextView mTvAudioToneTitle;
    private TextView mTvAudioToneSelector;

    public AudioToneCard(Context context) {
        super(context);
    }

    @Override
    public void onCreate(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.card_audio_tone, this, true);

        mIvAudioTone = root.findViewById(R.id.iv_audio_tone);
        mIvAudioToneTag = root.findViewById(R.id.iv_audio_tone_tag);
        mTvAudioToneTitle = root.findViewById(R.id.tv_audio_tone_title);
        mTvAudioToneSelector = root.findViewById(R.id.tv_audio_tone_selector);
    }

    @Override
    public void onBind(CardEntity entity) {
        super.onBind(entity);

        if (null != entity.bizData && entity.bizData instanceof AudioToneData) {
            AudioToneData audioToneData = (AudioToneData) entity.bizData;

            mIvAudioTone.setImageResource(audioToneData.getIconResId());
            mTvAudioToneTitle.setText(audioToneData.getTitle());
            if (audioToneData.isUsing()) {
                mIvAudioToneTag.setVisibility(View.VISIBLE);
                mTvAudioToneSelector.setVisibility(View.GONE);
            } else {
                mIvAudioToneTag.setVisibility(View.GONE);
                mTvAudioToneSelector.setVisibility(View.VISIBLE);
            }
        }

    }
}
