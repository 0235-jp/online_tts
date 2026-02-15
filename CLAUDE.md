# CLAUDE.md

## プロジェクト概要

Android TextToSpeechService として動作するオンライン TTS アプリ。外部 API から音声を合成し、端末のテキスト読み上げエンジンとして利用可能。

## ビルド・テスト

```bash
./gradlew assembleDebug      # デバッグビルド
./gradlew assembleRelease    # リリースビルド
```

テストは GitHub Actions で `assembleDebug` の成功を確認。ユニットテストは未整備。

## アーキテクチャ

### レイヤー構成

```
UI (Jetpack Compose + ViewModel)
  ↓
Data (Repository + DataStore + EncryptedPreferences)
  ↓
TTS Provider (抽象インターフェース)
  ↓
API Client (Ktor + OkHttp)
```

### DI

Dagger Hilt を使用。`OnlineTtsService` は `TextToSpeechService` のため `@AndroidEntryPoint` が使えず `@EntryPoint` でアクセス。

### TTS プロバイダー設計

新プロバイダー追加に必要な作業:
1. `TtsProviderType` に enum 値を追加
2. `TtsProvider` 実装クラスを作成
3. `TtsModule` に `@Provides @IntoMap` を追加

Service, UI, Repository, DataStore の変更は不要。

### ストリーミング

`TtsProvider.synthesizeStreaming()` が `Flow<SynthesisEvent>` を返す。デフォルト実装は `synthesize()` をラップするため、ストリーミング非対応プロバイダーはオーバーライド不要。

`StreamingAudioDecoder` は MP3 フレームを解析し `MediaCodec` に1フレームずつ投入してPCMを逐次出力。

### Voice ID

プロバイダーが不透明な文字列としてエンコード/デコード（例: Aivis Cloud は `"modelUuid:styleId"`）。

## コーディング規約

- 言語: Kotlin
- UI: Jetpack Compose + Material3
- 非同期: Kotlin Coroutines + Flow
- シリアライゼーション: kotlinx.serialization（`encodeToString` は `import kotlinx.serialization.encodeToString` が必要）
- ネットワーク: Ktor 3.x + OkHttp エンジン
- DI: Dagger Hilt
- Min SDK 26 / JDK 17
- UI テキストは日本語

## 重要なファイル

| ファイル | 役割 |
|---------|------|
| `tts/provider/TtsProvider.kt` | プロバイダーインターフェース |
| `tts/engine/OnlineTtsService.kt` | Android TTS サービス |
| `tts/engine/StreamingAudioDecoder.kt` | ストリーミング MP3→PCM デコード |
| `tts/engine/AudioDecoder.kt` | 非ストリーミング MP3/WAV→PCM デコード |
| `tts/aiviscloud/AivisCloudTtsProvider.kt` | Aivis Cloud 実装 |
| `data/repository/SettingsRepository.kt` | 設定の永続化 |
| `di/TtsModule.kt` | プロバイダーの DI 登録 |

## 既知の注意点

- `AndroidManifest.xml` の TTS サービスには `android:permission="android.permission.BIND_TTS_ENGINE"` と全 intent-filter に `CATEGORY_DEFAULT` が必須
- `NetworkModule` の Json 設定に `encodeDefaults = true` が必要（API リクエストのデフォルト値が省略されるのを防ぐ）
- Aivis Cloud API の `output_format` デフォルトは `mp3`（`wav` ではない）
