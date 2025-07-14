package com.szxm.av.controller;

import android.content.Context;

import com.aliyun.auikits.aiagent.ARTCAICallCustomEngineImpl;
import com.aliyun.auikits.aiagent.ARTCAICallEngine;

public class ARTCAICustomController extends ARTCAICallController {

    public ARTCAICustomController(Context context, String userId) {
        super(context, userId);
        mARTCAICallEngine = new ARTCAICallCustomEngineImpl(context, userId);
    }

    @Override
    public void start() {
        mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                setCallState(AICallState.Connecting, ARTCAICallEngine.AICallErrorCode.None);
                // 调用启动服务
                if (mARTCAiCallConfig.mAiCallAgentTemplateConfig.isSharedAgent) {
                    mARTCAICallEngine.getIARTCAICallService().generateAIAgentCall(mUserId, mARTCAiCallConfig.agentId, mAiAgentType, mARTCAiCallConfig, getStartActionCallback());
                } else {
                    mARTCAICallEngine.getIARTCAICallService().startAIAgentService(mUserId, mAiAgentType, mARTCAiCallConfig, getStartActionCallback());
                }
            }
        });
    }

    @Override
    public void startCall(String token) {

    }
}
