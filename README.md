# Online TTS

Android の TextToSpeechService として動作するオンライン音声合成アプリ。外部 TTS API を利用し、端末の「テキスト読み上げ」設定からエンジンとして選択できる。

## 対応プロバイダー

- [Aivis Cloud API](https://aivis-project.com/) - ストリーミング再生対応

## ビルド

```
./gradlew assembleDebug
```

- Min SDK 26 / Target SDK 35
- JDK 17

## セットアップ

1. アプリをインストール
2. 設定画面で API キーとモデル UUID を入力
3. 音声を検索・選択
4. 端末の「設定 > テキスト読み上げ」で Online TTS をエンジンに選択
