# Legacy Java/OpenGL Archive

Pine Creekの旧Java + 自前OpenGL ES実装は、開発履歴・比較資料としてリポジトリ内に残しています。

## Archive status

**READ ONLY / NOT ACTIVE DEVELOPMENT**

現行版は `godot/` です。旧Java版に対してGitHub Actionsでの自動ビルド、release APK生成、CI回帰テスト、新機能追加、現行ストーリーとの同期は行いません。

## 残しているもの

```text
app/                       旧Androidアプリ本体
tools/*.java               旧物理/カメラSelfTest
build.gradle               旧Gradle設定
settings.gradle            旧Gradle設定
gradle.properties          旧Gradle設定
gradle/                    旧Gradle wrapper関連
gradlew / gradlew.bat      旧Gradle wrapper
```

これらは履歴確認や旧実装比較のためだけに残します。

## Package

- Legacy Java: `com.pinecreek.game`
- Current Godot: `com.pinecreek.game.godot`

## Workflow removal

旧 `.github/workflows/build-apk.yml` は削除しました。

理由は、現行Godot版をビルドしていないこと、pushごとに旧Java版だけを検証する意味がなくなったこと、古いSDK/Gradle/artwork条件による不要なCI failureを止めるためです。

過去のGitHub Actions実行履歴はGitHub側に履歴として残る場合がありますが、新しいpushで旧Java workflowが起動することはありません。
