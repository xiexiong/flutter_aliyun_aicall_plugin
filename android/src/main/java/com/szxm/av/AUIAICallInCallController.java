package com.szxm.av;

import android.content.Context;
import android.content.Intent;

import com.aliyun.auikits.aiagent.ARTCAICallEngine;
import com.aliyun.auikits.aiagent.ARTCAICallEngineImpl;
import com.szxm.av.utils.AUIAIConstStrKey;

import java.util.UUID;

public class AUIAICallInCallController {
    private static AUIAICallInCallController instance;
    private Context appContext;
    private ARTCAICallEngineImpl engine = null;

    private String VoiceAgentId = "f05abdde9e5648efb966c3cb46361c5a";
    private String VoiceAppId = "28383372-04d8-4edd-b629-cf80c3bf2ec9";
    private String VoiceAppKey = "7523e3057a76a46cf1325a54ac493fdb";
    private String VideoAgentId = "47b870aa48cc49d1b43485042c6ddcf5";
    private String VideoAppId = "d858f7bd-196d-45b8-88cd-3e8e9e72094f";
    private String VideoAppKey = "19e1007f8a1c06ed234a0f374786c77f";
    private String channelVoiceID = "";
    private String channelVideoID = "";

    private AUIAICallInCallController(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new AUIAICallInCallController(context);
        }
    }
    public static AUIAICallInCallController getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Must call initialize() first");
        }
        return instance;
    }

    public void callVoiceAgent(String UserId,String loginAuthorization){
        channelVoiceID = UUID.randomUUID().toString();
        String rtcToken = ArtcTokenUtils.createBase64Token(VoiceAppId,VoiceAppKey,channelVoiceID,UserId);
        AgentIntent(ARTCAICallEngine.ARTCAICallAgentType.VoiceAgent,VoiceAgentId,rtcToken,UserId,loginAuthorization,"","","");
    }

    public void callViodeAgent(String UserId,String loginAuthorization){
        channelVideoID = UUID.randomUUID().toString();
        String rtcToken = ArtcTokenUtils.createBase64Token(VideoAppId,VideoAppKey,channelVideoID,UserId);
        AgentIntent(ARTCAICallEngine.ARTCAICallAgentType.VisionAgent,VideoAgentId,rtcToken,UserId,loginAuthorization,"","","");
    }



    private void AgentIntent(ARTCAICallEngine.ARTCAICallAgentType agentType,String agentId,String rtcToken,String UserId,
                                String loginAuthorization,String chatBotAgentId ,String sessionId  ,String receiverId ) {
        Intent intent = new Intent(this.appContext, AUIAICallInCallActivity.class);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_LOGIN_USER_ID, UserId);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_LOGIN_AUTHORIZATION, loginAuthorization);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_AI_AGENT_TYPE, agentType);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_RTC_AUTH_TOKEN, rtcToken);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_AI_AGENT_ID, agentId);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_KEY_IS_SHARED_AGENT, false);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_CHAT_BOT_AGENT_ID, chatBotAgentId);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_SESSION_ID, sessionId);
        intent.putExtra(AUIAIConstStrKey.BUNDLE_RECEIVER_ID, receiverId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
    }




}
