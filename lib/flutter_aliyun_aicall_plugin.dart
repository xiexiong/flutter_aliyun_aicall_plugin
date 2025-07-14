import 'package:flutter/services.dart';

class FlutterAliyunAicallPlugin {
  static const MethodChannel _channel = MethodChannel('aliyun_av_plugin');

  /// 启动音频通话
  static Future<bool> callVoiceAgent({required String userId, required String loginAuthor}) async {
    return await _channel.invokeMethod('callVoiceAgent', {
      'UserId': userId,
      'loginAuthorization': loginAuthor,
    });
  }

  /// 启动视频通话
  static Future<bool> callViodeAgent({required String userId, required String loginAuthor}) async {
    return await _channel.invokeMethod('callViodeAgent', {
      'UserId': userId,
      'loginAuthorization': loginAuthor,
    });
  }
}
