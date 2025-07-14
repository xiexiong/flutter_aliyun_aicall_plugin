package com.szxm.av.bean;

import android.net.Uri;


public class ChatBotSelectedFileAttachment {

    public enum ChatBotAttachmentType {
        None,
        Image,
        Audio,
        Video,
        Other
    };

    public String attachmentId;
    public Uri attachmentUri;
    public boolean uploadFailed = false;
    public float progress = 0;
    public ChatBotAttachmentType attachmentType;
    public String attachmentFilePath;
    public long lastUpdateTime = 0;

    public ChatBotSelectedFileAttachment(String attachmentId, Uri attachmentUri, ChatBotAttachmentType attachmentType) {
        this.attachmentId = attachmentId;
        this.attachmentUri = attachmentUri;
        this.attachmentType = attachmentType;
    }

    public ChatBotSelectedFileAttachment(String attachmentId, ChatBotAttachmentType attachmentType, String attachmentFilePath) {
        this.attachmentId = attachmentId;
        this.attachmentType = attachmentType;
        this.attachmentFilePath = attachmentFilePath;
    }
}
