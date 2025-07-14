// void main() async {
//   await AliyunAvPlugin.startCall(userId: "user1", channelId: "channelA");
//   String status = await AliyunAvPlugin.queryAgentStatus(agentId: "agent1");
//   String subtitle = await AliyunAvPlugin.showRealtimeSubtitle(sessionId: "session1");
//   await AliyunAvPlugin.interruptAgent(agentId: "agent1");
// }
import 'package:flutter/material.dart';
import 'package:flutter_aliyun_aicall_plugin/flutter_aliyun_aicall_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Aliyun AV Demo',
      debugShowCheckedModeBanner: false,
      home: const AvCallPage(),
    );
  }

  @override
  void initState() {
    super.initState();
  }
}

Future<void> _startCallVoice() async {
  try {
    bool isSuccess = await FlutterAliyunAicallPlugin.callVoiceAgent(
      userId: "user1",
      loginAuthor: "channelA",
    );
    // ignore: avoid_print
    print('========Start call result: $isSuccess');
  } catch (e) {
    // ignore: avoid_print
    print('========Start call error: $e');
  }
}

Future<void> _startCallVideo() async {
  try {
    bool isSuccess = await FlutterAliyunAicallPlugin.callViodeAgent(
      userId: "user1",
      loginAuthor: "channelA",
    );
    // ignore: avoid_print
    print('========Start call result: $isSuccess');
  } catch (e) {
    // ignore: avoid_print
    print('========Start call error: $e');
  }
}

class AvCallPage extends StatelessWidget {
  const AvCallPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          SizedBox(height: 120.0),
          Center(
            child: GestureDetector(
              onTap: () {
                print('语音通话');
                _startCallVoice();
              },
              child: Container(width: 200.0, color: Colors.amber, child: Text('语音通话')),
            ),
          ),
          SizedBox(height: 80.0),
          Center(
            child: GestureDetector(
              onTap: () {
                print('视频通话');
                _startCallVideo();
              },
              child: Container(width: 200.0, color: Colors.amber, child: Text('视频通话')),
            ),
          ),
        ],
      ),
    );
  }
}
