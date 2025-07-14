package com.szxm.av.widget;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.szxm.av.R;
import com.szxm.av.bean.BaseCard;
import com.szxm.av.bean.CardEntity;
import com.szxm.av.bean.ChatBotSelectedFileAttachment;

import java.lang.ref.WeakReference;

public class ChatBotSentItemImageCard extends BaseCard {

    private WeakReference<Context> mContextRef;
    private ImageView mImageView;

    public ChatBotSentItemImageCard(Context context) {
        super(context);
        this.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT));

    }

    @Override
    public void onCreate(Context context) {
        this.mContextRef = new WeakReference<>(context);
        View root = LayoutInflater.from(context).inflate(R.layout.layout_auiaichat_send_image_item, this, true);
        mImageView = root.findViewById(R.id.chatbot_send_image_item);
    }

    @Override
    public void onBind(CardEntity entity) {
        super.onBind(entity);

        if (null != entity.bizData && entity.bizData instanceof ChatBotSelectedFileAttachment) {
            String filePath = ((ChatBotSelectedFileAttachment) entity.bizData).attachmentFilePath;
            if(!TextUtils.isEmpty(filePath)) {
                Glide.with(this)
                        .load(filePath)
                        .into(mImageView);
            }
        }
    }
}
