package com.example.flutter_aliyun_aicall_plugin;


import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import com.szxm.av.AUIAICallInCallController;

public class FlutterAliyunAicallPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private Activity activity;
  private Context appContext;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
    appContext = binding.getApplicationContext();
    channel = new MethodChannel(binding.getBinaryMessenger(), "aliyun_av_plugin");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    try {
      switch (call.method) {
        case "callVoiceAgent": {
          String userId = call.argument("UserId");
          String loginAuthor = call.argument("loginAuthorization");
          if (userId != null && loginAuthor != null) {
            AUIAICallInCallController.initialize(appContext);
            AUIAICallInCallController.getInstance().callVoiceAgent(userId, loginAuthor);
            result.success(true);
          } else {
            result.error("INVALID_ARGUMENTS", "UserId or loginAuthorization is null", null);
          }
          break;
        }
        case "callViodeAgent": {
          String userId = call.argument("UserId");
          String loginAuthor = call.argument("loginAuthorization");
          if (userId != null && loginAuthor != null) {
            AUIAICallInCallController.initialize(appContext);
            AUIAICallInCallController.getInstance().callViodeAgent(userId, loginAuthor);
            result.success(true);
          } else {
            result.error("INVALID_ARGUMENTS", "UserId or loginAuthorization is null", null);
          }
          break;
        }
        default:
          result.notImplemented();
          break;
      }
    } catch (Exception e) {
      result.error("PLUGIN_ERROR", e.getMessage(), null);
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
    if (channel != null) {
      channel.setMethodCallHandler(null);
    }
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}