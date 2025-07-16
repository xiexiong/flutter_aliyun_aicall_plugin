class SubMessageItem {
  bool _isAsrText;
  int _asrSentenceId;
  int _receiveTime;
  String _text;
  int _displayEndTime;

  SubMessageItem({
    required bool isAsrText,
    required int asrSentenceId,
    required int receiveTime,
    required String text,
    required int displayEndTime,
  }) : _isAsrText = isAsrText,
       _asrSentenceId = asrSentenceId,
       _receiveTime = receiveTime,
       _text = text,
       _displayEndTime = displayEndTime;

  // Getter方法
  bool get isAsrText => _isAsrText;
  int get asrSentenceId => _asrSentenceId;
  int get receiveTime => _receiveTime;
  String get text => _text;
  int get displayEndTime => _displayEndTime;

  // Setter方法
  set isAsrText(bool value) => _isAsrText = value;
  set asrSentenceId(int value) => _asrSentenceId = value;
  set receiveTime(int value) => _receiveTime = value;
  set text(String value) => _text = value;
  set displayEndTime(int value) => _displayEndTime = value;
}
