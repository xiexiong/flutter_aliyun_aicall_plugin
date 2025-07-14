package com.szxm.av;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.AICallErrorCode.AgentConcurrentLimit;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.ARTCAICallAgentType.ChatBot;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.ARTCAICallAgentType.VisionAgent;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.ARTCAICallAgentType.VoiceAgent;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.ARTCAICallTurnDetectionMode.ARTCAICallTurnDetectionNormalMode;
import static com.aliyun.auikits.aiagent.ARTCAICallEngine.ARTCAICallTurnDetectionMode.ARTCAICallTurnDetectionSemanticMode;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.auikits.aiagent.ARTCAICallEngine;
import com.aliyun.auikits.aiagent.debug.ARTCAICallEngineDebuger;
import com.aliyun.auikits.aiagent.util.Logger;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnDismissListener;
import com.szxm.av.R;
import com.szxm.av.controller.ARTCAICallController;
import com.szxm.av.controller.ARTCAICallDepositController;
import com.szxm.av.service.ForegroundAliveService;
import com.szxm.av.utils.AUIAICallAgentIdConfig;
import com.szxm.av.utils.AUIAIConstStrKey;
import com.szxm.av.utils.AppServiceConst;
import com.szxm.av.utils.DisplayUtil;
import com.szxm.av.utils.SettingStorage;
import com.szxm.av.utils.TimeUtil;
import com.szxm.av.widget.AICallNoticeDialog;
import com.szxm.av.widget.AICallSentenceLatencyItem;
import com.szxm.av.widget.AICallSentenceLatencyViewModel;
import com.szxm.av.widget.AICallSubtitleMessageItem;
import com.szxm.av.widget.AICallSubtitleRecyclerViewAdapter;
import com.szxm.av.widget.AICallSubtitleSpacingItemDecoraion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AUIAICallInCallActivity extends ComponentActivity {
    private final String TAG = AUIAICallInCallActivity.class.getName();
    private static final boolean IS_SUBTITLE_ENABLE = true;
    private static String sUserId = null;

    private Handler mHandler = null;
    private boolean mUIProgressing = false;
    private long mCallConnectedMillis = 0;

    private ARTCAICallController mARTCAICallController = null;
    private ARTCAICallEngine mARTCAICallEngine = null;
    private ImageView btnCameraDirectionNew = null;
    private TextView tvCallTips,tvCallMicrophone,tvCallDuration,btnAiCallFullScreenSubtitle = null;
    private ARTCAICallController.AICallState mCallState;
    private ARTCAICallEngine.AICallErrorCode mAICallErrorCode = ARTCAICallEngine.AICallErrorCode.None;
    private ARTCAICallEngine.ARTCAICallRobotState mRobotState = ARTCAICallEngine.ARTCAICallRobotState.Listening;
    private boolean isUserSpeaking = false;
    private boolean initVisCameraDirection = true;
    private boolean isOpenCamera = true;
    private boolean isOpenMuted = true;
    private long mLastBackButtonExitMillis = 0;
    private long mLastCallMillis = 0;
    private ARTCAICallEngine.ARTCAICallAgentType mAiAgentType = ARTCAICallEngine.ARTCAICallAgentType.VoiceAgent;
    private boolean mIsSharedAgent = false;
    private boolean mIsPushToTalkMode = false;
    private boolean mIsVoicePrintRecognized = false;

    private SubtitleHolder mSubtitleHolder = null;
    private boolean mIsFullScreenSubtitleOpen = false;
    private ActionLayerHolder mActionLayerHolder = null;
    private final SmallVideoViewHolder mSmallVideoViewHolder = new SmallVideoViewHolder();

//    private AUIAICallAgentAnimator mAICallAgentAnimator = null;
    private boolean mIsPreviewShowInSmallView = true;

    // 对话延时
    private final List<AICallSentenceLatencyItem> aiCallSentenceLatencyItems = new ArrayList<>();

    private AICallSentenceLatencyViewModel mAICallSentenceLatencyViewModel;

    private ARTCAICallController.IARTCAICallStateCallback mCallStateCallback = new ARTCAICallController.IARTCAICallStateCallback() {
        @Override
        public void onAICallEngineStateChanged(ARTCAICallController.AICallState oldCallState, ARTCAICallController.AICallState newCallState, ARTCAICallEngine.AICallErrorCode errorCode) {
            Log.i(TAG, "onAICallEngineStateChanged(ARTCAICallController.AICallState "+oldCallState+", ARTCAICallController.AICallState "+newCallState+", ARTCAICallEngine.AICallErrorCode "+errorCode+")");
            switch (newCallState) {
                case None:
                    break;
                case Connecting:
                    break;
                case Connected:
                    startUIUpdateProgress();
                    break;
                case Over:
                    stopUIUpdateProgress();
                    break;
                case Error:
                    break;
                default:
                    break;
            }
            mAICallErrorCode = errorCode;
            mCallState = newCallState;
            updateUIByEngineState();
            updateForegroundAliveService();
            updateAvatarVisibility(mAiAgentType);
        }
    };

    private ARTCAICallEngine.IARTCAICallEngineCallback mARTCAIEngineCallback = new ARTCAICallEngine.IARTCAICallEngineCallback() {

        @Override
        public void onAICallEngineRobotStateChanged(ARTCAICallEngine.ARTCAICallRobotState oldRobotState, ARTCAICallEngine.ARTCAICallRobotState newRobotState) {
            Log.i(TAG, "onAICallEngineRobotStateChanged(ARTCAICallEngine.ARTCAICallRobotState "+oldRobotState+", ARTCAICallEngine.ARTCAICallRobotState "+newRobotState+",");
            switch (newRobotState) {
                case Listening:
                    break;
                case Thinking:
                    break;
                case Speaking:
                    break;
                default:
                    break;
            }
            mRobotState = newRobotState;
            updateUIByEngineState();
        }

        @Override
        public void onUserSpeaking(boolean isSpeaking) {
            Log.i(TAG, "onUserSpeaking: "+isSpeaking);
            isUserSpeaking = isSpeaking;
            updateUIByEngineState();
        }

        @Override
        public void onUserAsrSubtitleNotify(String text, boolean isSentenceEnd, int sentenceId, ARTCAICallEngine.VoicePrintStatusCode voicePrintStatusCode) {
            Log.i(TAG, "onUserAsrSubtitleNotify(String "+text+", boolean "+isSentenceEnd+", int "+sentenceId+", ARTCAICallEngine.VoicePrintStatusCode "+voicePrintStatusCode+")");
            if (IS_SUBTITLE_ENABLE) {
                // 无效的Asr字幕直接不显示
                if(voicePrintStatusCode != ARTCAICallEngine.VoicePrintStatusCode.UndetectedSpeakerWithAIVad &&
                        voicePrintStatusCode != ARTCAICallEngine.VoicePrintStatusCode.SpeakerNotRecognized) {
                    mSubtitleHolder.updateSubtitle(true, isSentenceEnd, text, sentenceId);
                }
                if (voicePrintStatusCode == ARTCAICallEngine.VoicePrintStatusCode.SpeakerNotRecognized ||
                        voicePrintStatusCode == ARTCAICallEngine.VoicePrintStatusCode.SpeakerRecognized) {
                    mIsVoicePrintRecognized = true;
                }
                // 声纹非主讲人反馈结果
                if (voicePrintStatusCode == ARTCAICallEngine.VoicePrintStatusCode.SpeakerNotRecognized) {
                    setSecondaryCallTips(true, getResources().getString(R.string.main_speaker_not_recognized),
                            getResources().getString(R.string.reset_voiceprint), new Runnable() {
                                @Override
                                public void run() {
                                    mARTCAICallEngine.clearVoicePrint();
                                }
                            });
                } else if (voicePrintStatusCode == ARTCAICallEngine.VoicePrintStatusCode.UndetectedSpeakerWithAIVad && false) {
                    setSecondaryCallTips(true, getResources().getString(R.string.aival_main_speaker_not_recognized), null, null);
                }
            }
        }

        @Override
        public void onAIAgentSubtitleNotify(String text, boolean end, int userAsrSentenceId) {
            Log.i(TAG, "onAIAgentSubtitleNotify(String "+text+", boolean "+end+", int "+userAsrSentenceId+")");
            if (IS_SUBTITLE_ENABLE) {
                mSubtitleHolder.updateSubtitle(false, end, text, userAsrSentenceId);
            }
        }

        @Override
        public void onAgentEmotionNotify(String emotion,int userAsrSentenceId) {
            Log.i(TAG, "onAgentEmotionNotify(String "+emotion+",int "+userAsrSentenceId+") ");
            if(false) {
                Toast.makeText(AUIAICallInCallActivity.this, "The agent seems to be " + emotion, Toast.LENGTH_SHORT);
            }
//            if (mAICallAgentAnimator != null) {
//                mAICallAgentAnimator.updateAgentAnimator(emotion);
//            }
        }

        @Override
        public void onNetworkStatusChanged(String uid, ARTCAICallEngine.ARTCAICallNetworkQuality quality) {
            Log.i(TAG, "onNetworkStatusChanged: (String "+uid+", ARTCAICallEngine.ARTCAICallNetworkQuality "+quality+")");
        }

        @Override
        public void onVoiceIdChanged(String voiceId) {
            Log.i(TAG, "onVoiceIdChanged: "+voiceId);
        }

        @Override
        public void onVoiceVolumeChanged(String uid, int volume) {
            Log.i(TAG, "onVoiceVolumeChanged: (String "+uid+", int "+volume+")");
        }

        @Override
        public void onVoiceInterrupted(boolean enable) {
            Log.i(TAG, "onVoiceInterrupted: "+enable);
        }

        @Override
        public void onCallBegin() {
            Log.i(TAG, "onCallBegin: " + mARTCAICallEngine.getAgentInfo().instanceId);
            long current = SystemClock.elapsedRealtime();

            if(false) {
                Logger.i( "Call started duration " + (current - mLastCallMillis));
                Toast.makeText(AUIAICallInCallActivity.this, "Call started duration " + (current - mLastCallMillis), Toast.LENGTH_SHORT);
            }
        }

        @Override
        public void onCallEnd() {
            Log.i(TAG, "onCallEnd: ");
        }

        @Override
        public void onErrorOccurs(ARTCAICallEngine.AICallErrorCode errorCode) {
            Log.i(TAG, "onErrorOccurs: "+errorCode);
            if (errorCode == AgentConcurrentLimit) {
                AICallNoticeDialog.showDialog(AUIAICallInCallActivity.this,
                        0, false, R.string.token_resource_exhausted, true, new OnDismissListener() {
                            @Override
                            public void onDismiss(DialogPlus dialog) {
                                finish();
                            }
                        });
            }
        }

        @Override
        public void onAgentAudioAvailable(boolean available) {
            Log.i(TAG, "onAgentAudioAvailable: "+available);
        }

        @Override
        public void onAgentVideoAvailable(boolean available) {
            Log.i(TAG, "onAgentVideoAvailable: "+available);
        }

        public void onAgentDataChannelAvailable() {
            Log.i(TAG, "onAgentDataChannelAvailable: ");
        }

        @Override
        public void onAgentAvatarFirstFrameDrawn() {
            Log.i(TAG, "onAgentAvatarFirstFrameDrawn: ");
//            mSubtitleHolder.updateSubtitleVisibility();
        }

        @Override
        public void onUserOnLine(String uid) {
            Log.i(TAG, "onUserOnLine: "+uid);
        }

        @Override
        public void onPushToTalk(boolean enable) {
            Log.i(TAG, "onPushToTalk: "+enable);
            mIsPushToTalkMode = enable;
            updateActionLayerHolder();
        }

        @Override
        public void onVoicePrintEnable(boolean enable) {
            Log.i(TAG, "onVoicePrintEnable: "+enable);
            if (!enable) {
                mIsVoicePrintRecognized = false;
            }
        }

        @Override
        public void onVoicePrintCleared() {
            Log.i(TAG, "onVoicePrintCleared: ");
            mIsVoicePrintRecognized = false;
        }

        @Override
        public void onAgentWillLeave(int reason, String message) {
            Log.i(TAG, "onAgentWillLeave: (int "+reason+", String "+message+")");
            int toastResId = R.string.ai_agent_leave_notify_default;
            if (reason == 2001) {
                toastResId = R.string.ai_agent_leave_notify_long_time_idle;
            }
            Toast.makeText(AUIAICallInCallActivity.this, toastResId, Toast.LENGTH_SHORT);
            handUp(false);
        }

        @Override
        public void onReceivedAgentCustomMessage(String data) {
            Log.i(TAG, "onReceivedAgentCustomMessage: "+data);
        }

        @Override
        public void onHumanTakeoverWillStart(String takeoverUid, int takeoverMode) {
            Log.i(TAG, "onHumanTakeoverWillStart: (String "+takeoverUid+", int "+takeoverMode+") ");
            Toast.makeText(AUIAICallInCallActivity.this, R.string.ai_agent_human_takeover_will_start, Toast.LENGTH_SHORT);
        }

        @Override
        public void onHumanTakeoverConnected(String takeoverUid) {
            Log.i(TAG, "onHumanTakeoverConnected: "+ takeoverUid);
            Toast.makeText(AUIAICallInCallActivity.this, R.string.ai_agent_human_takeover_connect, Toast.LENGTH_SHORT);

        }

        @Override
        public void onAudioDelayInfo(int id, int delay_ms) {
            Log.i(TAG, "onAudioDelayInfo: (int "+id+", int "+delay_ms+")");
            if(false) {
                Toast.makeText(AUIAICallInCallActivity.this, "AudioDelayInfo: id :" + id + ", delay: " + delay_ms, Toast.LENGTH_SHORT);
            }
            AICallSentenceLatencyItem aICallSentenceLatencyItem = new AICallSentenceLatencyItem(id, delay_ms);
            aiCallSentenceLatencyItems.add(aICallSentenceLatencyItem);
            mAICallSentenceLatencyViewModel.updateData(aiCallSentenceLatencyItems);
        }
        @Override
        public void onVisionCustomCapture(boolean enable) {
            Log.i(TAG, "onVisionCustomCapture: "+enable);
            if(false) {
                Toast.makeText(AUIAICallInCallActivity.this, "onVisionCustomCapture enable " + enable, Toast.LENGTH_SHORT);
            }
        }
        @Override
        public void onSpeakingInterrupted(ARTCAICallEngine.ARTCAICallSpeakingInterruptedReason reason) {
            Log.i(TAG, "onSpeakingInterrupted: "+reason);
//            mAICallAgentAnimator.onAgentInterrupted();
            if(false) {
                Toast.makeText(AUIAICallInCallActivity.this, "onSpeakingInterrupted reason " + reason, Toast.LENGTH_SHORT);
            }
        }
        @Override
        public void onReceivedAgentVcrResult(ARTCAICallEngine.ARTCAICallAgentVcrResult result) {
            Log.i(TAG, "onReceivedAgentVcrResult: "+result);
            if(false) {
                Toast.makeText(AUIAICallInCallActivity.this, "VCR result " + result, Toast.LENGTH_SHORT);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aiCallSentenceLatencyItems.clear();
        // 去掉屏幕常亮
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auiaicall_in_call);

        SettingStorage.getInstance().init(this);
        // Sentence Latency
        mAICallSentenceLatencyViewModel = new ViewModelProvider(this).get(AICallSentenceLatencyViewModel.class);

        // 设置屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHandler = new Handler();

        findViewById(R.id.video_agent_small_view_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeVideoView();
            }
        });

        // 字幕开关控制全屏字幕的可见性
        mSubtitleHolder = new SubtitleHolder(this);
        btnAiCallFullScreenSubtitle = (TextView) findViewById(R.id.btn_ai_call_full_screen_subtitle);
        btnAiCallFullScreenSubtitle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(mIsFullScreenSubtitleOpen) {
                    btnAiCallFullScreenSubtitle.setBackgroundResource(R.drawable.bg_btn_subtitle_white);
                    btnAiCallFullScreenSubtitle.setTextColor(getResources().getColor(R.color.color_51565F));
                    if (mAiAgentType != VoiceAgent && isOpenCamera) {
                        btnAiCallFullScreenSubtitle.setBackgroundResource(R.drawable.bg_btn_subtitle_black);
                        btnAiCallFullScreenSubtitle.setTextColor(getResources().getColor(R.color.white));
                        btnCameraDirectionNew.setVisibility(VISIBLE);
                    }
                } else {
                    btnAiCallFullScreenSubtitle.setBackgroundResource(R.drawable.bg_btn_subtitle_white);
                    btnAiCallFullScreenSubtitle.setTextColor(getResources().getColor(R.color.color_1A1A1A));
                    btnCameraDirectionNew.setVisibility(GONE);
                }
                mSubtitleHolder.setFullScreenSubtitleVisibility(!mIsFullScreenSubtitleOpen);
            }
        });
        findViewById(R.id.avatar_layer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mARTCAICallEngine.interruptSpeaking();
            }
        });
        findViewById(R.id.ll_ai_agent_logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mARTCAICallEngine.interruptSpeaking();
            }
        });

        String aiAgentRegion = null;
        String aiAgentId = null;
        mAiAgentType = ARTCAICallEngine.ARTCAICallAgentType.VoiceAgent;
        String loginUserId = null;
        String loginAuthorization = null;
        boolean chatSyncConfig = false;
        String rtcAuthToken = null;
        if (null != getIntent() && null != getIntent().getExtras()) {
            aiAgentRegion = getIntent().getExtras().getString(AUIAIConstStrKey.BUNDLE_KEY_AI_AGENT_REGION, null);
            aiAgentId = getIntent().getExtras().getString(AUIAIConstStrKey.BUNDLE_KEY_AI_AGENT_ID, null);
            mAiAgentType = (ARTCAICallEngine.ARTCAICallAgentType) getIntent().getExtras().getSerializable(AUIAIConstStrKey.BUNDLE_KEY_AI_AGENT_TYPE);
            mIsSharedAgent =  getIntent().getExtras().getBoolean(AUIAIConstStrKey.BUNDLE_KEY_IS_SHARED_AGENT, false);
            rtcAuthToken = getIntent().getExtras().getString(AUIAIConstStrKey.BUNDLE_KEY_RTC_AUTH_TOKEN, null);
            loginUserId = getIntent().getExtras().getString(AUIAIConstStrKey.BUNDLE_KEY_LOGIN_USER_ID, null);
            loginAuthorization = getIntent().getExtras().getString(AUIAIConstStrKey.BUNDLE_KEY_LOGIN_AUTHORIZATION, null);
            chatSyncConfig = getIntent().getExtras().getBoolean(AUIAIConstStrKey.BUNDLE_KEY_CHAT_SYNC_CONFIG, false);
        }

        if(TextUtils.isEmpty(aiAgentRegion)) {
            if(!mIsSharedAgent) {
                aiAgentRegion = AUIAICallAgentIdConfig.getRegion();
            }
        }

//        语音控制动画
//        if (mAICallAgentAnimator == null) {
//            FrameLayout callAgentContainer = findViewById(R.id.ai_call_agent_avatar_container);
//            if (mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VoiceAgent) {
//                mAICallAgentAnimator = new AUIAICallAgentAvatarAnimator(this);
//            } else {
//                mAICallAgentAnimator = new AUIAICallAgentSimpleAnimator(this);
//            }
//            callAgentContainer.removeAllViews();
//            callAgentContainer.addView(mAICallAgentAnimator);
//
//            mAICallAgentAnimator.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mARTCAICallEngine.interruptSpeaking();
//                }
//            });
//        }


        ARTCAICallEngineDebuger.enableDumpData = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_AUDIO_DUMP_SWITCH);
        ARTCAICallEngineDebuger.enableUserSpecifiedAudioTips = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_AUDIO_TIPS_SWITCH);
        ARTCAICallEngineDebuger.enableLabEnvironment = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_USE_RTC_PRE_ENV_SWITCH);
        ARTCAICallEngineDebuger.enableAecPlugin = true;

        ARTCAICallEngine.ARTCAICallConfig artcaiCallConfig = new ARTCAICallEngine.ARTCAICallConfig();
        artcaiCallConfig.region = aiAgentRegion;
        artcaiCallConfig.agentId = aiAgentId;
        artcaiCallConfig.mAiCallAgentTemplateConfig.isSharedAgent = mIsSharedAgent;
        artcaiCallConfig.mAiCallAgentTemplateConfig.appServerHost = AppServiceConst.HOST;
        artcaiCallConfig.mAiCallAgentTemplateConfig.loginUserId = loginUserId;
        artcaiCallConfig.mAiCallAgentTemplateConfig.loginAuthrization = loginAuthorization;
        mIsPushToTalkMode = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_BOOT_ENABLE_PUSH_TO_TALK, SettingStorage.DEFAULT_ENABLE_PUSH_TO_TALK);
        artcaiCallConfig.agentConfig.enablePushToTalk = mIsPushToTalkMode;
        artcaiCallConfig.agentConfig.voiceprintConfig.useVoicePrint = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_BOOT_ENABLE_VOICE_PRINT, SettingStorage.DEFAULT_ENABLE_VOICE_PRINT);
        artcaiCallConfig.userData = SettingStorage.getInstance().get(SettingStorage.KEY_BOOT_USER_EXTEND_DATA);
        artcaiCallConfig.videoConfig.useHighQualityPreview = true;
        artcaiCallConfig.videoConfig.cameraCaptureFrameRate = 15;
        // 这里frameRate设置为5，需要根据控制台上的智能体的抽帧帧率（一般为2）进行调整，最大不建议超过15fps
        // videoEncoderBitRate: videoEncoderFrameRate超过10可以设置为512
        artcaiCallConfig.videoConfig.videoEncoderFrameRate = 5;
        artcaiCallConfig.videoConfig.videoEncoderBitRate = 340;
        if(mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) {
            artcaiCallConfig.videoConfig.useFrontCameraDefault = true;
        }

        if(false) {
            updateAgentConfig(artcaiCallConfig.agentConfig);
        }
        artcaiCallConfig.enableAudioDelayInfo = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_BOOT_ENABLE_AUDIO_DELAY_INFO, true);
        String pronunciationStr = SettingStorage.getInstance().get(SettingStorage.KEY_PRONUNCIATION_RULES);
        if(!TextUtils.isEmpty(pronunciationStr)) {
            List<JSONObject> list = new ArrayList<>();
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(pronunciationStr);
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getJSONObject(i));
                }
                artcaiCallConfig.agentConfig.ttsConfig.pronunciationRules = list;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String vcrConfigStr = SettingStorage.getInstance().get(SettingStorage.KEY_VCR_CONFIG_RULES);
        if(!TextUtils.isEmpty(vcrConfigStr)) {
            try {
                JSONObject jsonObject = new JSONObject(vcrConfigStr);
                artcaiCallConfig.agentConfig.vcrConfig = new ARTCAICallEngine.ARTCAICallAgentVcrConfig(jsonObject);
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(chatSyncConfig) {
            if(false) {
                artcaiCallConfig.chatSyncConfig.chatBotAgentId = AUIAICallAgentIdConfig.getAIAgentId(ChatBot, false);
            }
            else {
                artcaiCallConfig.chatSyncConfig.chatBotAgentId = AUIAICallAgentIdConfig.getAIAgentId(ChatBot, false);
            }
            artcaiCallConfig.chatSyncConfig.sessionId = loginUserId + "_" + artcaiCallConfig.chatSyncConfig.chatBotAgentId;
            artcaiCallConfig.chatSyncConfig.receiverId = loginUserId;
        }

        if(TextUtils.isEmpty(artcaiCallConfig.agentConfig.voiceprintConfig.voiceprintId)) {
            artcaiCallConfig.agentConfig.voiceprintConfig.voiceprintId = loginUserId;
        }
        artcaiCallConfig.agentType = mAiAgentType;
        mARTCAICallController = new ARTCAICallDepositController(this, loginUserId); // new ARTCAICustomController(this, loginUserId);
        mARTCAICallController.setBizCallEngineCallback(mARTCAIEngineCallback);
        mARTCAICallController.setCallStateCallback(mCallStateCallback);
        mARTCAICallController.init(artcaiCallConfig);
        mARTCAICallController.setAICallAgentType(mAiAgentType);
        mARTCAICallController.enableFetchVoiceIdList(false);

        mARTCAICallEngine = mARTCAICallController.getARTCAICallEngine();
////        摄像头默认关闭状态
//        mARTCAICallEngine.muteLocalCamera(true);
        btnCameraDirectionNew = findViewById(R.id.btn_camera_direction_new);
//        btnCameraDirectionNew.setVisibility(GONE);
        tvCallTips = (TextView) findViewById(R.id.tv_call_tips);
        tvCallDuration = findViewById(R.id.tv_call_duration);
        tvCallMicrophone = (TextView) findViewById(R.id.tv_call_microphone);

        if (mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.AvatarAgent) {
            mARTCAICallEngine.setAgentView(findViewById(R.id.avatar_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
        } else if (mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VisionAgent) {
            mARTCAICallEngine.setLocalView(findViewById(R.id.avatar_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
        } else if(mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) {
            ARTCAICallEngine.ARTCAICallVideoCanvas remoteCanvas = new ARTCAICallEngine.ARTCAICallVideoCanvas();
            remoteCanvas.zOrderOnTop = false;
            remoteCanvas.zOrderMediaOverlay = false;
            mARTCAICallEngine.setAgentView(findViewById(R.id.avatar_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT), remoteCanvas
            );
            mARTCAICallEngine.setLocalView(findViewById(R.id.video_agent_small_view_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
        }
        if(TextUtils.isEmpty(rtcAuthToken) || SettingStorage.getInstance().getBoolean(SettingStorage.KEY_USE_APP_SERVER_START_AGENT)) {
            mARTCAICallController.start();
        } else {
            mARTCAICallController.startCall(rtcAuthToken);
        }
        mLastCallMillis = SystemClock.elapsedRealtime();
        mSmallVideoViewHolder.init(this);
        updateActionLayerHolder();
    }

    @Override
    public void onBackPressed() {
        long nowMillis = SystemClock.elapsedRealtime();
        long duration = nowMillis - mLastBackButtonExitMillis;
        final long DOUBLE_PRESS_THRESHOLD = 1000;
        if (duration <= DOUBLE_PRESS_THRESHOLD) {
            if (handUp(false)) {
                super.onBackPressed();
            }
        } else {
            Toast.makeText(this, R.string.tips_exit, Toast.LENGTH_SHORT);
        }
        mLastBackButtonExitMillis = nowMillis;
    }

    /**
     *
     * @param keepActivity
     * @return 是否可以直接关闭Activity
     */
    private boolean handUp(boolean keepActivity) {
        if (!keepActivity) {
            mARTCAICallEngine.handup();
            finish();
        } else {
            mARTCAICallEngine.handup();
        }
        return false;
    }

    // 改为接通之后显示Avatar
    private void updateAvatarVisibility(ARTCAICallEngine.ARTCAICallAgentType aiAgentType) {
        if (mCallState == ARTCAICallController.AICallState.Connected) {
            if (useVideo()) {
                if (initVisCameraDirection){
                    setTypeWhite();
                    initVisCameraDirection =false;
                }

                if (mARTCAICallEngine.isLocalCameraMute()) {
                    if(aiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VisionAgent) {
                        findViewById(R.id.ll_ai_agent_logo).setVisibility(VISIBLE);
                        findViewById(R.id.avatar_layer).setVisibility(GONE);
                        findViewById(R.id.ai_call_agent_avatar_container).setVisibility(GONE);
                    }
                } else {
                    findViewById(R.id.ll_ai_agent_logo).setVisibility(GONE);
                    findViewById(R.id.avatar_layer).setVisibility(VISIBLE);
                    findViewById(R.id.ai_call_agent_avatar_container).setVisibility(GONE);
                }
                if(aiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) {
                    findViewById(R.id.small_view_layer).setVisibility(VISIBLE);

                    if(mIsPreviewShowInSmallView) {
                        if(mARTCAICallEngine.isLocalCameraMute()) {
                            findViewById(R.id.small_view_camera_mute).setVisibility(VISIBLE);
                            findViewById(R.id.video_agent_small_view_layer).setVisibility(GONE);
                        } else {
                            findViewById(R.id.small_view_camera_mute).setVisibility(GONE);
                            findViewById(R.id.video_agent_small_view_layer).setVisibility(VISIBLE);
                        }
                    } else {
                        if(mARTCAICallEngine.isLocalCameraMute()) {
                            findViewById(R.id.ll_ai_agent_logo).setVisibility(VISIBLE);
                            findViewById(R.id.avatar_layer).setVisibility(GONE);
                            findViewById(R.id.ai_call_agent_avatar_container).setVisibility(GONE);
                        } else {
                            findViewById(R.id.ll_ai_agent_logo).setVisibility(GONE);
                            findViewById(R.id.avatar_layer).setVisibility(VISIBLE);
                            findViewById(R.id.ai_call_agent_avatar_container).setVisibility(GONE);
                        }
                    }
                }
            } else {
                findViewById(R.id.ll_ai_agent_logo).setVisibility(VISIBLE);
                findViewById(R.id.avatar_layer).setVisibility(GONE);
                findViewById(R.id.ai_call_agent_avatar_container).setVisibility(GONE);
            }
        }
    }

    private void changeVideoView() {
        ((FrameLayout)findViewById(R.id.video_agent_small_view_layer)).removeAllViews();
        ((FrameLayout)findViewById(R.id.avatar_layer)).removeAllViews();
        if(mIsPreviewShowInSmallView) {
            mARTCAICallEngine.setAgentView(findViewById(R.id.video_agent_small_view_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
            ARTCAICallEngine.ARTCAICallVideoCanvas localCanvas = new ARTCAICallEngine.ARTCAICallVideoCanvas();
            localCanvas.zOrderOnTop = false;
            localCanvas.zOrderMediaOverlay = false;
            mARTCAICallEngine.setLocalView(findViewById(R.id.avatar_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT), localCanvas
            );
            mIsPreviewShowInSmallView = false;
        } else {
            ARTCAICallEngine.ARTCAICallVideoCanvas remoteCanvas = new ARTCAICallEngine.ARTCAICallVideoCanvas();
            remoteCanvas.zOrderMediaOverlay = false;
            remoteCanvas.zOrderOnTop = false;
            mARTCAICallEngine.setAgentView(findViewById(R.id.avatar_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
            mARTCAICallEngine.setLocalView(findViewById(R.id.video_agent_small_view_layer),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            );
            mIsPreviewShowInSmallView = true;
        }

        if(mARTCAICallEngine.isLocalCameraMute()) {
            if(mIsPreviewShowInSmallView) {
                findViewById(R.id.small_view_camera_mute).setVisibility(VISIBLE);
                findViewById(R.id.video_agent_small_view_layer).setVisibility(GONE);
                findViewById(R.id.ll_ai_agent_logo).setVisibility(GONE);
                findViewById(R.id.avatar_layer).setVisibility(VISIBLE);
            } else {
                findViewById(R.id.small_view_camera_mute).setVisibility(GONE);
                findViewById(R.id.video_agent_small_view_layer).setVisibility(VISIBLE);
                findViewById(R.id.ll_ai_agent_logo).setVisibility(VISIBLE);
                findViewById(R.id.avatar_layer).setVisibility(GONE);
            }
        }
    }

    private static String generateUserId() {
        if (TextUtils.isEmpty(sUserId)) {
            sUserId = UUID.randomUUID().toString();
        }
        return sUserId;
    }

    private void startUIUpdateProgress() {
        mUIProgressing = true;
        mCallConnectedMillis = SystemClock.elapsedRealtime();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgressUI();
            }
        });
    }

    private void stopUIUpdateProgress() {
        mUIProgressing = false;
    }

    private boolean useAvatar() {
        return mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.AvatarAgent || mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent;
    }

    private boolean useVideo() {
        return mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.AvatarAgent ||
                mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VisionAgent || mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent;
    }

    private void updateProgressUI() {
        if (mUIProgressing) {
            boolean hasNextRun = true;
            // 更新通话时长
            long duration = mCallConnectedMillis > 0 ? SystemClock.elapsedRealtime() - mCallConnectedMillis : 0;
            ((TextView)findViewById(R.id.tv_call_duration)).setText(TimeUtil.formatDuration(duration));

            // 更新实时字幕
//            mSubtitleHolder.refreshSubtitle();

            // 数字人体验超过5分钟，自动结束
            if (useAvatar() && duration > 5 * 60 * 1000) {
                AICallNoticeDialog.showDialog(AUIAICallInCallActivity.this,
                        0, false, R.string.token_time_lit_tips, true, new OnDismissListener() {
                            @Override
                            public void onDismiss(DialogPlus dialog) {
                                finish();
                            }
                        });
                handUp(true);
                hasNextRun = false;
            }

            if (hasNextRun) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateProgressUI();
                    }
                }, 100);
            }
        }
    }

    private void updateForegroundAliveService() {
        if ( mCallState == ARTCAICallController.AICallState.Connected) {
            // start
            Intent serviceIntent = new Intent(this, ForegroundAliveService.class);
            startService(serviceIntent);
        } else {
            // stop
            Intent serviceIntent = new Intent(this, ForegroundAliveService.class);
            stopService(serviceIntent);
        }
    }

    private void updateUIByEngineState() {
        updateCallTips();
        updateSpeechAnimationType();
    }

    private void updateActionLayerHolder() {
        boolean needInit = false;
        if (mIsPushToTalkMode) {
            if ((mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VisionAgent || mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) && !(mActionLayerHolder instanceof VisionPushToTalkLayerHolder)) {
                mActionLayerHolder = new VisionPushToTalkLayerHolder();
                needInit = true;
            } else if ((mAiAgentType != ARTCAICallEngine.ARTCAICallAgentType.VisionAgent &&   mAiAgentType != ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) && !(mActionLayerHolder instanceof AudioPushToTalkLayerHolder)) {
                mActionLayerHolder = new AudioPushToTalkLayerHolder();
                needInit = true;
            }
        } else {
            if ((mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VisionAgent  || mAiAgentType == ARTCAICallEngine.ARTCAICallAgentType.VideoAgent) && !(mActionLayerHolder instanceof VisionActionLayerHolder)) {
                mActionLayerHolder = new VisionActionLayerHolder();
                needInit = true;
            } else if (mAiAgentType != ARTCAICallEngine.ARTCAICallAgentType.VisionAgent && mAiAgentType != ARTCAICallEngine.ARTCAICallAgentType.VideoAgent && !(mActionLayerHolder instanceof AudioActionLayerHolder)) {
                mActionLayerHolder = new AudioActionLayerHolder();
                needInit = true;
            }
        }

        if (needInit) {
            mActionLayerHolder.init();
        }
    }

    private void updateCallTips() {
        int resId = 0;
        boolean needSetText = false;
        boolean keepText = false;

        if (mCallState == ARTCAICallController.AICallState.Over) {
            keepText = true;
        } else if (mCallState == ARTCAICallController.AICallState.Connecting) {
            resId = R.string.call_connection_tips;
            needSetText = true;
        } else if (mCallState == ARTCAICallController.AICallState.Connected) {
            if (mRobotState == ARTCAICallEngine.ARTCAICallRobotState.Thinking) {
                resId = R.string.robot_thinking_tips;
                needSetText = true;
            } else if (mRobotState == ARTCAICallEngine.ARTCAICallRobotState.Speaking) {
                boolean isVoiceInterruptEnable = mARTCAICallEngine.isVoiceInterruptEnable();
                boolean isPushToTalkEnable = mARTCAICallEngine.isPushToTalkEnable();
                if (!isVoiceInterruptEnable || isPushToTalkEnable) {
                    resId = R.string.robot_speaking_tips_without_voice_interrupt;
                } else {
                    resId = R.string.robot_speaking_tips;
                }
                needSetText = true;
            } else if (mRobotState == ARTCAICallEngine.ARTCAICallRobotState.Listening) {
                resId = R.string.robot_listening_tips;
                needSetText = true;
            }
        } else if (mCallState == ARTCAICallController.AICallState.Error) {
            switch (mAICallErrorCode) {
                case StartFailed:
                    resId = R.string.call_error_start_failed;
                    break;
                case AgentSubscriptionRequired:
                    resId = R.string.call_error_agent_subscription_required;
                    break;
                case AgentNotFund:
                    resId = R.string.call_error_agent_not_found;
                    break;
                case TokenExpired:
                    resId = R.string.call_error_token_expired;
                    break;
                case ConnectionFailed:
                    resId = R.string.call_error_connection_failed;
                    break;
                case KickedByUserReplace:
                    resId = R.string.call_error_kicked_by_user_replace;
                    break;
                case KickedBySystem:
                    resId = R.string.call_error_kicked_by_system;
                    break;
                case LocalDeviceException:
                    resId = R.string.call_error_local_device_exception;
                    break;
                case AgentLeaveChannel:
                    resId = R.string.error_tips_avatar_leave_join;
                    break;
                case AgentAudioSubscribeFailed:
                    resId = R.string.error_tips_avatar_subscribe_fail;
                    break;
                case AgentConcurrentLimit:
                    resId = R.string.token_resource_exhausted;
                    break;
                case AiAgentAsrUnavailable:
                    resId = R.string.error_tips_asr_unavailable;
                    break;
                case AvatarAgentUnavailable:
                    resId = R.string.error_tips_avatar_agent_unavailable;
                    break;
                default:
                    resId = R.string.call_error_default;
                    break;
            }
            handUp(true);
            needSetText = true;
        }


        if (!keepText) {
            if (needSetText) {
                tvCallTips.setText(resId);
            } else {
                tvCallTips.setText("");
            }
        }
    }

    private void setSecondaryCallTips(boolean show, String tips, String actionTips, Runnable actionRunnable) {
        ViewGroup llCallSecondaryTips = findViewById(R.id.ll_call_secondary_tips);
        if (show && llCallSecondaryTips.getVisibility() == GONE) {
            if (useVideo()) {
                llCallSecondaryTips.setBackgroundResource(R.drawable.bg_secondary_tips_incall);
            } else {
                llCallSecondaryTips.setBackground(null);
            }
            ((TextView)findViewById(R.id.tv_call_secondary_tips)).setText(tips);
            if(!TextUtils.isEmpty(actionTips)) {
                findViewById(R.id.btn_call_secondary_tips).setVisibility(VISIBLE);
                ((TextView)findViewById(R.id.btn_call_secondary_tips)).setText(actionTips);
            }else {
                findViewById(R.id.btn_call_secondary_tips).setVisibility(GONE);
            }


            Runnable delayGoneRunnable = () -> {
                setSecondaryCallTips(false, null, null, null);
            };
            ((TextView)findViewById(R.id.btn_call_secondary_tips)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != actionRunnable) {
                        actionRunnable.run();
                    }
                    setSecondaryCallTips(false, null, null, null);
                    llCallSecondaryTips.removeCallbacks(delayGoneRunnable);
                }
            });

            llCallSecondaryTips.postDelayed(delayGoneRunnable, 8000);
        }
        llCallSecondaryTips.setVisibility(show ? VISIBLE : GONE);
    }

    private void updateSpeechAnimationType() {
        Log.i("AUIAICALL", "updateSpeechAnimationType: [robotState: " + mRobotState + ", isUserSpeaking: " + isUserSpeaking + "]");
//        if (mAICallAgentAnimator != null) {
//            mAICallAgentAnimator.updateState(AUIAICallAgentAnimator.AUIAICallState.valueOf(mCallState.name()));
//            mAICallAgentAnimator.updateAgentAnimator(AUIAICallAgentAnimator.ARTCAICallAgentState.valueOf(mRobotState.name()));
//        }
    }

    private void updateAgentConfig(ARTCAICallEngine.ARTCAICallAgentConfig agentConfig) {
        try {
            String enableVoiceInterruptKey = SettingStorage.getInstance().get(SettingStorage.KEY_ENABLE_VOICE_INTERRUPT);
            if(TextUtils.isEmpty(enableVoiceInterruptKey) || !enableVoiceInterruptKey.equals("0")) {
                agentConfig.interruptConfig.enableVoiceInterrupt = true;
            } else {
                agentConfig.interruptConfig.enableVoiceInterrupt = false;
            }
            agentConfig.ttsConfig.agentVoiceId = SettingStorage.getInstance().get(SettingStorage.KEY_VOICE_ID);
            agentConfig.userOfflineTimeout = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_USER_OFFLINE_TIMEOUT));
            agentConfig.agentMaxIdleTime = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_MAX_IDLE_TIME));
            agentConfig.workflowOverrideParams = SettingStorage.getInstance().get(SettingStorage.KEY_WORK_FLOW_OVERRIDE_PARAMS);
            agentConfig.llmConfig.bailianAppParams = SettingStorage.getInstance().get(SettingStorage.KEY_BAILIAN_APP_PARAMS);
            agentConfig.llmConfig.llmSystemPrompt = SettingStorage.getInstance().get(SettingStorage.KEY_LLM_SYSTEM_PROMPT);
            agentConfig.volume = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_VOLUME));
            agentConfig.agentGreeting = SettingStorage.getInstance().get(SettingStorage.KEY_GREETING);
            agentConfig.voiceprintConfig.voiceprintId = SettingStorage.getInstance().get(SettingStorage.KEY_VOICE_PRINT_ID);
            agentConfig.enableIntelligentSegment = SettingStorage.getInstance().get(SettingStorage.KEY_ENABLE_INTELLIGENT_SEGMENT).equals("1") ? true:false;
            agentConfig.avatarConfig.agentAvatarId = SettingStorage.getInstance().get(SettingStorage.KEY_AVATAR_ID);
            agentConfig.asrConfig.asrMaxSilence = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_ASR_MAX_SILENCE));
            agentConfig.userOnlineTimeout = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_USER_ONLINE_TIME_OUT));
            agentConfig.asrConfig.asrLanguageId = SettingStorage.getInstance().get(SettingStorage.KEY_USER_ASR_LANGUAGE);
            String interruptWorks = SettingStorage.getInstance().get(SettingStorage.KEY_INTERRUPT_WORDS);
            agentConfig.asrConfig.vadLevel = Integer.parseInt(SettingStorage.getInstance().get(SettingStorage.KEY_VAD_LEVEL));
            agentConfig.asrConfig.customParams = SettingStorage.getInstance().get(SettingStorage.KEY_ASR_CUSTOM_PARAMS);
            String asrHotWords = SettingStorage.getInstance().get(SettingStorage.KEY_ASR_HOT_WORDS);
            String turnEndWords = SettingStorage.getInstance().get(SettingStorage.KEY_TURN_END_WORDS);
            String sematnicDuration = SettingStorage.getInstance().get(SettingStorage.KEY_BOOT_SEMATNIC_DURATION);


            if(!TextUtils.isEmpty(interruptWorks)) {
                agentConfig.interruptConfig.interruptWords = new ArrayList<String>();
                if(interruptWorks.contains(",")) {
                    String[] inputs = interruptWorks.split(",");
                    if(inputs.length > 0) {
                        for(String input : inputs) {
                            agentConfig.interruptConfig.interruptWords.add(input);
                        }
                    }
                } else {
                    agentConfig.interruptConfig.interruptWords.add(interruptWorks);
                }
            }
            if(!TextUtils.isEmpty(asrHotWords)) {
                agentConfig.asrConfig.asrHotWords = new ArrayList<String>();
                if(asrHotWords.contains(",")) {
                    String[] inputs = asrHotWords.split(",");
                    if(inputs.length > 0) {
                        for(String input : inputs) {
                            agentConfig.asrConfig.asrHotWords.add(input);
                        }
                    }
                } else {
                    agentConfig.asrConfig.asrHotWords.add(asrHotWords);
                }
            }

            if(!TextUtils.isEmpty(turnEndWords)) {
                agentConfig.turnDetectionConfig.turnEndWords = new ArrayList<String>();
                if(turnEndWords.contains(",")) {
                    String[] inputs = turnEndWords.split(",");
                    if(inputs.length > 0) {
                        for(String input : inputs) {
                            agentConfig.turnDetectionConfig.turnEndWords.add(input);
                        }
                    }
                } else {
                    agentConfig.turnDetectionConfig.turnEndWords.add(turnEndWords);
                }
            }
            agentConfig.turnDetectionConfig.mode = SettingStorage.getInstance().getBoolean(SettingStorage.KEY_BOOT_ENABLE_SEMATNIC, false) ? ARTCAICallTurnDetectionSemanticMode : ARTCAICallTurnDetectionNormalMode;
            if(!TextUtils.isEmpty(sematnicDuration)) {
                agentConfig.turnDetectionConfig.semanticWaitDuration = Integer.parseInt(sematnicDuration);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class SubtitleHolder {
        private LinearLayout mLlFullScreenSubtitle = null;

        private List<SubtitleTextPart> mSubtitlePartList = new ArrayList<>();
        // 展开全屏字幕显示
        private RecyclerView mRvFullScreenSubtitle = null;
        private List<AICallSubtitleMessageItem> mFullScreenSubtitleMessageList = new ArrayList<>();
        private AICallSubtitleRecyclerViewAdapter mSubtitleItemAdapter = null;
        private boolean shouldSubtitleAutoScroll = false;
        private Integer mAsrSentenceId = null;
        private Boolean isLastSubtitleOfAsr = null;
        private static final int INTERNATIONAL_WORD_INTERVAL = 30;
        private static final int CHINESE_WORD_INTERVAL = 100;
        private static final String SINGLE_WORD = "龍";
        private static final String TARGET_WORD = " > ";

        // 问答消息字幕片段
        private class SubtitleTextPart {
            long receiveTime = 0;
            String text;
            long displayEndTime = 0;
        }

        public SubtitleHolder(Context context) {
            initUIComponent();
            mSubtitleItemAdapter = new AICallSubtitleRecyclerViewAdapter(context, mFullScreenSubtitleMessageList);
            mRvFullScreenSubtitle.setAdapter(mSubtitleItemAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
//            layoutManager.setStackFromEnd(true);
            mRvFullScreenSubtitle.setLayoutManager(layoutManager);
            // 设置列表项间距
            mRvFullScreenSubtitle.addItemDecoration(new AICallSubtitleSpacingItemDecoraion(20));
            // 添加手动滚动监听
            mRvFullScreenSubtitle.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    // 判断用户是否在手动滚动
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if(!mRvFullScreenSubtitle.canScrollVertically(1)) {
                            // 当前在底部，恢复自动滚动
                            shouldSubtitleAutoScroll = true;
                        }
                    } else {
                        shouldSubtitleAutoScroll = false;
                    }
                }
            });
        }

        private void updateSubtitle(boolean isAsrText, boolean end, String text, int asrSentenceId) {
            Log.i("AUIAICall", "updateSubtitle [isAsrText" + isAsrText + ", end: " + end +
                    ", text: " + text + ", asrSentenceId: " + asrSentenceId + "]");
            boolean resetSubtitle = false;
            if (isLastSubtitleOfAsr == null || isAsrText || isLastSubtitleOfAsr) { // asr字幕、robot字幕切换
                resetSubtitle = true;
            } else if (mAsrSentenceId == null || mAsrSentenceId != asrSentenceId) { // 新对话
                resetSubtitle = true;
            }
            mAsrSentenceId = asrSentenceId;
            isLastSubtitleOfAsr = isAsrText;
            if (resetSubtitle) {
                mSubtitlePartList.clear();
            }
            SubtitleTextPart subtitleTextPart = new SubtitleTextPart();
            subtitleTextPart.text = text;
            subtitleTextPart.receiveTime = SystemClock.elapsedRealtime();
            mSubtitlePartList.add(subtitleTextPart);

            AICallSubtitleMessageItem subtitleMessageItem = new AICallSubtitleMessageItem(isAsrText, asrSentenceId, subtitleTextPart.receiveTime,text,subtitleTextPart.displayEndTime);
            if (resetSubtitle) {
                if(isAsrText) {
                    mSubtitleItemAdapter.replaceLastSubtitle(subtitleMessageItem);
                } else {
                    mSubtitleItemAdapter.addSubtitleItem(subtitleMessageItem);
                }
            } else {
                mSubtitleItemAdapter.appendToLastSubtitle(subtitleMessageItem);
            }
            // 更新字幕时检查是否应该自动滚动
            if(shouldSubtitleAutoScroll) {
                mRvFullScreenSubtitle.scrollToPosition(mSubtitleItemAdapter.getItemCount() - 1);
            }
            initUIComponent();
        }

        private float measureText(TextPaint textPaint, String ch, float lineWidth) {
            if ("\n".equals(ch)) {
                return lineWidth;
            } else {
                return textPaint.measureText(ch);
            }
        }

        private void initUIComponent() {
            if (null == mLlFullScreenSubtitle) {
                mLlFullScreenSubtitle = findViewById(R.id.ll_full_screen_subtitle);
            }
            if (null == mRvFullScreenSubtitle) {
                mRvFullScreenSubtitle = findViewById(R.id.rv_full_screen_subtitle);
            }
        }

        private void setFullScreenSubtitleVisibility(boolean visible) {
            if (null != mLlFullScreenSubtitle) {
                mLlFullScreenSubtitle.setVisibility(visible ? VISIBLE : GONE);
//                findViewById(R.id.ll_full_screen_subtitle_top_container).setVisibility(visible ? VISIBLE : GONE);
                findViewById(R.id.ll_full_screen_subtitle_bottom_container).setVisibility(visible ? VISIBLE : GONE);

                mIsFullScreenSubtitleOpen = visible;
//                setEngineTipVisibility(!visible);
                // 启动的时候自动滚动到底部
                if(visible) {
                    mRvFullScreenSubtitle.scrollToPosition(mSubtitleItemAdapter.getItemCount() - 1);
                }
            }
        }
    }

    private abstract class ActionLayerHolder {
        protected View mActionLayer = null;
        protected ActionLayerHolder(View actionLayer) {
            findViewById(R.id.action_layer_voice).setVisibility(GONE);
            findViewById(R.id.action_layer_video).setVisibility(GONE);
            findViewById(R.id.action_layer_push_to_talk_voice).setVisibility(GONE);
            findViewById(R.id.action_layer_push_to_talk_video).setVisibility(GONE);
            mActionLayer = actionLayer;
            mActionLayer.setVisibility(VISIBLE);
        }
        protected void init() {
            initStopCallButtonUI();
            initMuteButtonUI();
            initSpeakerButtonUI();
            updateSpeakerButtonUI();
        }

        protected void initStopCallButtonUI() {
            mActionLayer.findViewById(R.id.btn_stop_call).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (handUp(false)) {
                        finish();
                    }
                }
            });
        }

        protected void initMuteButtonUI() {
            mActionLayer.findViewById(R.id.btn_mute_call).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isMicrophoneOn = mARTCAICallEngine.isMicrophoneOn();
                    mARTCAICallEngine.muteMicrophone(isMicrophoneOn);
                    updateMuteButtonUI(!isMicrophoneOn);
                    updateUIByEngineState();
                }
            });
        }

        protected void updateMuteButtonUI(boolean isMuted) {
            ImageView ivMuteCall = (ImageView) mActionLayer.findViewById(R.id.iv_mute_call);
            TextView tvMuteCall = (TextView) mActionLayer.findViewById(R.id.tv_mute_call);
            isOpenMuted = isMuted;
            if (isMuted) {
                if (isOpenCamera && mAiAgentType == VisionAgent){
                    ivMuteCall.setImageResource(R.drawable.ic_voice_mute_white);
                }else {
                    ivMuteCall.setImageResource(R.drawable.ic_voice_mute);
                }
                tvMuteCall.setText(R.string.mute_call);
                tvCallTips.setVisibility(VISIBLE);
                tvCallMicrophone.setVisibility(GONE);
                tvCallMicrophone.setText(R.string.microphone_open);
            } else {
                if (isOpenCamera && mAiAgentType == VisionAgent){
                    ivMuteCall.setImageResource(R.drawable.ic_voice_open_white);
                    tvCallMicrophone.setTextColor(getResources().getColor(R.color.white));
                }else {
                    ivMuteCall.setImageResource(R.drawable.ic_voice_open);
                    tvCallMicrophone.setTextColor(getResources().getColor(R.color.color_51565F));
                }
                tvMuteCall.setText(R.string.unmute_call);
                tvCallMicrophone.setVisibility(VISIBLE);

                tvCallTips.setVisibility(GONE);
                tvCallMicrophone.setText(R.string.microphone_off);
            }
        }

        protected void initSpeakerButtonUI() {
            mActionLayer.findViewById(R.id.btn_speaker).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isSpeakerOn = mARTCAICallEngine.isSpeakerOn();
                    mARTCAICallEngine.enableSpeaker(!isSpeakerOn);
                    updateSpeakerButtonUI();
                }
            });
        }
        protected void updateSpeakerButtonUI() {
            boolean isSpeakerOn = null != mARTCAICallEngine ? mARTCAICallEngine.isSpeakerOn() : true;
            ImageView ivSpeaker = (ImageView) mActionLayer.findViewById(R.id.iv_speaker);
            TextView tvSpeaker = (TextView) mActionLayer.findViewById(R.id.tv_speaker);
            if (isSpeakerOn) {
                ivSpeaker.setImageResource(R.drawable.ic_speaker_on);
                tvSpeaker.setText(R.string.speaker_on);
            } else {
                ivSpeaker.setImageResource(R.drawable.ic_speaker_off);
                tvSpeaker.setText(R.string.speaker_on);
            }
        }

        protected void initPushToTalkButton() {
            ViewGroup llPushToTalk = mActionLayer.findViewById(R.id.btn_push_to_talk);

            llPushToTalk.setOnTouchListener(new View.OnTouchListener() {
                static final int MSG_AUTO_FINISH_PUSH_TO_TALK = 8888;
                static final int AUTO_FINISH_PUSH_TO_TALK_TIME = 60000;
                long startTalkMillis = 0;
                Handler uiHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);

                        if (msg.what == MSG_AUTO_FINISH_PUSH_TO_TALK) {
                            Log.i("initPushToTalkButton",  "MSG_AUTO_FINISH_PUSH_TO_TALK");
                            onFinishTalk(true);
                        }
                    }
                };
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.i("initPushToTalkButton",  "onTouch: " + event.getAction());
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        onStartTalk();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        onFinishTalk(false);
                    }
                    return true;
                }

                private void onStartTalk() {
                    Log.i("initPushToTalkButton",  "onStartTalk");
                    if (null != mARTCAICallEngine) {
                        mARTCAICallEngine.startPushToTalk();
                        startTalkMillis = SystemClock.uptimeMillis();
                        uiHandler.sendEmptyMessageDelayed(MSG_AUTO_FINISH_PUSH_TO_TALK, AUTO_FINISH_PUSH_TO_TALK_TIME);

                        ImageView ivPushToTalk = mActionLayer.findViewById(R.id.iv_push_to_talk);
                        TextView tvPushToTalk = mActionLayer.findViewById(R.id.tv_push_to_talk);
                        ivPushToTalk.setImageResource(R.drawable.ic_microphone_speaking);
                        tvPushToTalk.setText(R.string.release_to_send);
                    }
                }
                private void onFinishTalk(boolean auto) {
                    Log.i("initPushToTalkButton",  "onFinishTalk");
                    if (null != mARTCAICallEngine && startTalkMillis != 0) {
                        long talkTime = SystemClock.uptimeMillis() - startTalkMillis;
                        startTalkMillis = 0;
                        if (talkTime > 500) { // 大于500ms才会发送
                            mARTCAICallEngine.finishPushToTalk();
                        } else {
                            mARTCAICallEngine.cancelPushToTalk();
                        }

                        ImageView ivPushToTalk = mActionLayer.findViewById(R.id.iv_push_to_talk);
                        TextView tvPushToTalk = mActionLayer.findViewById(R.id.tv_push_to_talk);
                        ivPushToTalk.setImageResource(R.drawable.ic_microphone_idle);
                        tvPushToTalk.setText(R.string.push_to_talk);
                    }
                    if (!auto) {
                        uiHandler.removeMessages(MSG_AUTO_FINISH_PUSH_TO_TALK);
                    }
                }

            });
        }

        protected void initCameraButtonUI() {
            mActionLayer.findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isLocalCameraMute = !mARTCAICallEngine.isLocalCameraMute();
                    mARTCAICallEngine.muteLocalCamera(isLocalCameraMute);
                    updateCameraButtonUI(isLocalCameraMute);
                    updateAvatarVisibility(mAiAgentType);
                }
            });
        }
        protected void initCameraDirectionUI() {
            mActionLayer.findViewById(R.id.btn_camera_direction).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mARTCAICallEngine.switchCamera();
                }
            });
            findViewById(R.id.btn_camera_direction_new).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mARTCAICallEngine.switchCamera();
                }
            });
        }
        protected void updateCameraButtonUI(boolean isCameraMute) {
            ImageView ivCamera = (ImageView) mActionLayer.findViewById(R.id.iv_camera);
            ImageView ivMuteCall = (ImageView) mActionLayer.findViewById(R.id.iv_mute_call);
            TextView tvCamera = (TextView) mActionLayer.findViewById(R.id.tv_camera);
            if (isCameraMute) {
                isOpenCamera = false;
                ivCamera.setImageResource(R.drawable.ic_camera_preview_off);
                if (isOpenMuted){
                    ivMuteCall.setImageResource(R.drawable.ic_voice_mute);
                }else{
                    ivMuteCall.setImageResource(R.drawable.ic_voice_open);
                }
                setType51565F();
                tvCamera.setText(R.string.camera_off);
            } else {
                isOpenCamera = true;
                ivCamera.setImageResource(R.drawable.ic_camera_preview_on_white);
                if (isOpenMuted){
                    ivMuteCall.setImageResource(R.drawable.ic_voice_mute_white);
                }else{
                    ivMuteCall.setImageResource(R.drawable.ic_voice_open_white);
                }
                setTypeWhite();
                tvCamera.setText(R.string.camera_on);
            }
        }
    }

    private void setTypeWhite(){

        if (!mIsFullScreenSubtitleOpen) {
            btnAiCallFullScreenSubtitle.setBackground(getResources().getDrawable(R.drawable.bg_btn_subtitle_black));
            btnAiCallFullScreenSubtitle.setTextColor(getResources().getColor(R.color.white));
            btnCameraDirectionNew.setVisibility(VISIBLE);
        }
        tvCallDuration.setTextColor(getResources().getColor(R.color.white));
        tvCallTips.setTextColor(getResources().getColor(R.color.white));
    }

    private void setType51565F(){
        if (!mIsFullScreenSubtitleOpen){
            btnAiCallFullScreenSubtitle.setBackground(getResources().getDrawable(R.drawable.bg_btn_subtitle_white));
            btnAiCallFullScreenSubtitle.setTextColor(getResources().getColor(R.color.color_51565F));
            btnCameraDirectionNew.setVisibility(GONE);
        }
        tvCallTips.setTextColor(getResources().getColor(R.color.color_51565F));
        tvCallDuration.setTextColor(getResources().getColor(R.color.color_51565F));
    }
    // VisionChat
    private class VisionActionLayerHolder extends ActionLayerHolder {
        private VisionActionLayerHolder() {
            super(findViewById(R.id.action_layer_video));
        }
        @Override
        protected void init() {
            initCameraButtonUI();
            initCameraDirectionUI();
            initStopCallButtonUI();
            initMuteButtonUI();
            initSpeakerButtonUI();
            updateSpeakerButtonUI();
        }
    }

    // VoiceChat、AvatarChat
    private class AudioActionLayerHolder extends ActionLayerHolder {
        private AudioActionLayerHolder() {
            super(findViewById(R.id.action_layer_voice));
        }
        @Override
        protected void init() {
            initStopCallButtonUI();
            initMuteButtonUI();
            initSpeakerButtonUI();
            updateSpeakerButtonUI();
        }
    }

    // VoiceChat、AvatarChat - PushToTalk
    private class AudioPushToTalkLayerHolder extends ActionLayerHolder {
        private AudioPushToTalkLayerHolder() {
            super(findViewById(R.id.action_layer_push_to_talk_voice));
        }

        @Override
        protected void init() {
            initStopCallButtonUI();
            initSpeakerButtonUI();
            initPushToTalkButton();
            updateSpeakerButtonUI();
        }
    }


    // VoiceChat、AvatarChat - PushToTalk
    private class VisionPushToTalkLayerHolder extends ActionLayerHolder {
        private VisionPushToTalkLayerHolder() {
            super(findViewById(R.id.action_layer_push_to_talk_video));
        }

        @Override
        protected void init() {
            initCameraButtonUI();
            initCameraDirectionUI();
            initStopCallButtonUI();
            initSpeakerButtonUI();
            initPushToTalkButton();
            updateSpeakerButtonUI();
        }
    }


    private class SmallVideoViewHolder {
        private FrameLayout mSmallAvatarLayerContainer = null;
        private FrameLayout mSmallAvatarLayer = null;

        private int mMinMargin;

        private int mLeftMargin, mTopMargin;

        public void init(Context context) {
            mMinMargin = DisplayUtil.dip2px(10);

            mSmallAvatarLayerContainer = findViewById(R.id.small_avatar_layer_container);
            mSmallAvatarLayer = findViewById(R.id.small_avatar_layer);

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mSmallAvatarLayer.getLayoutParams();
            mLeftMargin = layoutParams.leftMargin;
            mTopMargin = layoutParams.topMargin;

            mSmallAvatarLayer.setOnTouchListener(new View.OnTouchListener() {
                private PointF downPoint = new PointF();
                private PointF curPoint = new PointF();
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("SmallVideoViewHolder", "mSmallAvatarLayer onTouch [event: " + event.getAction() + ", x: " + event.getX() + ", y: " + event.getY());
                    return false;
                }
            });

            mSmallAvatarLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("SmallVideoViewHolder", "mSmallAvatarLayer onClick");
                }
            });

        }
    }

    public void copyToClipboard(Context context, String text) {
        // 获取剪贴板管理器
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建剪贴板数据
        ClipData clip = ClipData.newPlainText("AUIAICall", text); // "label" 可以自定义
        // 将数据放入剪贴板
        clipboard.setPrimaryClip(clip);
    }

}