# Contributing to Pine Creek

Pine Creekは、雪国の架空の田舎町を舞台にした小さなオープンソースAndroidゲームです。

## まずIssue

大きな変更を始める前にIssueを作り、目的を共有してください。

特に歓迎するもの:

- 車両挙動の改善
- OpenGL ES描画の最適化
- 低ポリモデル改善
- 新しいストーリー
- 町内放送 / 住民会話
- UI改善
- Android端末ごとの不具合修正

## 開発環境

[docs/BUILD.md](docs/BUILD.md) を参照してください。

## ブランチ

```text
main
└─ feature/your-feature
```

例:

```bash
git checkout -b feature/better-snow
```

## コーディング方針

- Java 17
- Android標準APIを優先
- ゲームエンジン追加はIssueで相談
- 外部アセットを追加する場合はライセンスを明記
- 実在メーカーのロゴや権利上問題のある素材を持ち込まない
- ストーリーは架空の町・人物・車両として作る
- 日本語UI / 日本語ストーリーを基本とする

## Pull Request

PRには以下を書いてください。

- 何を変更したか
- なぜ変更したか
- 実機確認の有無
- 操作系変更の場合は変更前後の挙動
- 新規アセットがある場合は出所とライセンス

## テスト

最低限、GitHub Actionsの **Build Pine Creek APK** が成功することを確認してください。

可能なら実機で以下を確認してください。

- 起動
- タイトル画面
- 前進
- バック
- 左右ステアリング
- ブレーキ
- ストーリー開始
- セーブ / 続きから
