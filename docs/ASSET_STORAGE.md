# Asset Storage Strategy

Pine Creekは広い町、複数車両、キャラクター、音声を増やしていくため、Gitリポジトリを『最終ビルド置き場』にはしません。

## Current snapshot — 2026-09-25

Linux作業環境での確認時:

- `.git` object storage: 約31 MiB
- working tree全体: 約826 MiB
- working treeが大きい主因: `godot/build/` に残っている過去APKとローカル生成物
- `godot/build/` と `.godot/` はGit管理対象外
- Git LFSは現時点で未導入

つまり現状はGit履歴が膨張しているわけではありません。ローカル作業ディレクトリに古いbuild成果物が多い状態です。

## GitHub側の現行目安

2026-09-25時点のGitHub公式資料では、通常Gitで50 MiBを超える単一fileは警告対象、100 MiB超はblockされます。repositoryは理想として1 GB未満、5 GB未満が強く推奨されています。

GitHub Releaseは1 releaseあたり最大1000 assets、各assetは2 GiB未満で、release全体の合計sizeとdownload bandwidthには明示的な上限がありません。そのためAPKやfull soundtrackの配布先としてmain Gitより適しています。

## 1. Main Git repositoryに置くもの

- GDScript / Python / Blender生成script
- scene / resource / story data
- docs
- 最適化済みの小〜中規模GLB
- 512〜1024px程度の実ゲーム用texture
- 小さなUI PNG
- 実ゲームで直接必要な短いPCM/Ogg
- サイズが小さい編集用`.blend`

目安として、数MB程度のassetは普通のGitで問題ありません。大きなbinaryを無制限にcommitしないことを優先します。

## 2. GitHub Releaseへ置くもの

- APK
- APK SHA-256
- 3分フル版soundtrack WAV
- 将来の大きなoptional soundtrack pack
- 大きなsource-art archiveを配布したい場合のzip
- milestone時の高解像度promo export

Releaseは『配布物』の置き場として使い、Git履歴を太らせません。

## 3. Generated assets

生成できるものはbinaryそのものよりgenerator scriptを重要資産にします。

現在のaudio生成方式と同じ考え方で:

- procedural props
- character prototype
- repeated map objects
- road placement
- portrait render

を再生成可能にします。

`.blend`が小さい間はsourceとしてcommitして構いませんが、同じ内容をscriptから確実に生成できる場合は巨大な中間ファイルを増やさない方針です。

## 4. Map assets

広いマップだからといって大容量textureを並べません。

- 512m前後のworld cell
- modular building assets
- repeated tree/pole/snowbank assets
- placement data
- road curve data
- low resolution terrain source

を組み合わせます。

巨大な単一`.blend`、巨大な一枚terrain texture、何千個もの重複meshを避けます。

## 5. Git LFSを使う条件

Git LFSは今すぐ必要ありません。

導入を検討する条件:

- 編集元binaryが継続的に20〜50 MiB級になる
- `.blend`や高解像度textureを履歴込みで保持する必要がある
- main repoのbinary履歴がclone速度へ明確に影響し始める

その場合は `.blend`、大きいsource texture、音声masterなどだけをLFSへ移します。コードやscene dataをLFSへ入れません。

## 6. Separate asset repository / object storage fallback

将来さらに巨大化した場合の順番:

1. Main repo + GitHub Releases（現在の推奨）
2. 必要なbinaryだけGit LFS
3. `PineCreekAssets` のような別asset repo + LFS
4. 最終手段としてS3/R2等のobject storage + version manifest + SHA-256

別ストレージへ移す場合も、どのasset versionがどのgame commitに対応するかをmanifestで固定します。

## 7. Practical size rules for this project

- raw APKをGitへcommitしない
- build screenshotを大量commitしない
- soundtrack full WAVはRelease asset
- 4K textureは明確な理由がない限り作らない
- mobile本番textureはatlas化を優先
- 似た建物/車/NPCはmaterial・mesh差し替えで増やす
- source `.blend`はモジュール単位に分ける
- map全体を一つの巨大`.blend`にしない

この方針なら、田舎らしい広いマップにしてもGit容量をかなり小さく保てます。
