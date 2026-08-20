# 新規性レポート: EntityGraphなしでデタッチ後の受領書明細を読めない

## 結論

本ラボは、詳細表示サービスが通常の`findById`で`@OneToMany(fetch = LAZY)`の受領書を返し、トランザクション完了後のDTO変換で`LazyInitializationException`になる問題を扱います。`@EntityGraph(attributePaths = "lines")`付きの詳細取得を選ぶことで、必要な明細だけを取得時に初期化します。[1] [2]

既存のJPA原稿にあるPersistence Contextとバルク更新、`orphanRemoval`、楽観ロック、JPQL null比較とは、直接原因、実境界、観測契約、最小修正が異なります。既存のJPAエラー一覧には`LazyInitializationException`の語が登場しますが、EntityGraphを用いた詳細表示の取得選択、トランザクション完了後のDTO変換、初期化状態の直接比較を扱う独立した再現教材ではありません。

## 監査方法

2026-08-20に`/home/ubuntu/qiita`配下のMarkdownを対象として、`LazyInitializationException`、`FetchType.LAZY`、`EntityGraph`、`@EntityGraph`、`遅延ロード`、`遅延読み込み`、Spring Data JPA・JPA・Hibernateを検索しました。さらに、直前のJPQL null比較ラボを含むホームディレクトリ直下のJava/Spring教材を列挙し、原因・境界・契約・最小修正を比較しました。

| 監査対象 | 確認結果 | 本ラボへの影響 |
| --- | --- | --- |
| Qiita原稿の`@EntityGraph` | 該当なし | 同じ取得方法・修正を主題とする原稿は確認されなかった。 |
| Qiita原稿の`LazyInitializationException` | JPAエラー一覧に用語の掲載あり | 一般的なエラー一覧であり、実行可能なEntityGraph教材とは異なる。 |
| Qiita原稿の`FetchType.LAZY` | JPA一般解説・挙動の読みにくさを論じる原稿で掲載 | 詳細取得、トランザクション後DTO変換、初期化状態比較の契約は確認されなかった。 |
| 既存JPAデバッグ原稿 | バルク更新、`orphanRemoval`、楽観ロック、JPQL null比較を確認 | いずれも別の永続化規則を扱う。 |
| Repository Catalog | `/home/ubuntu/repository-catalog`が存在しない | カタログ更新・検証・語彙スクリーニングは実行できず、この限界を明示する。 |

## 既存JPA題材との四軸比較

| 比較対象 | 直接原因 | 実境界 | 観測契約 | 最小修正 | 本ラボとの差分 |
| --- | --- | --- | --- | --- | --- |
| 本ラボ | 詳細表示サービスがLAZY関連を未初期化の通常取得で返す | `ReceiptViewService`と`@EntityGraph`付きリポジトリ | トランザクション後に`Green tea`をDTOへ変換できる | 詳細取得を`findDetailedById`へ変える | 基準 |
| Spring Data JPAのバルク更新とPersistence Context | バルクUPDATE後に管理状態が古い | `@Modifying`とdirty checking | 状態が古い値で上書きされない | Persistence Contextの同期を選ぶ | 本ラボはSELECT時のフェッチ計画であり、更新・dirty checkingを扱わない。 |
| Spring Data JPAの`orphanRemoval` | 親子関連の削除伝播が不適切 | 親から明細を外す操作 | 明細DB行が削除される | 所有側とコレクションを整合させる | 本ラボは行を削除せず、既存明細を取得する。 |
| Spring Data JPAの楽観ロック | `@Version`を伴う競合更新 | 複数更新の永続化 | 古い在庫更新を拒否する | 競合を処理する | 本ラボは単一読み取りで、versionと並行更新を扱わない。 |
| Spring Data JPAのJPQL null比較 | `= :parameter`へのnull束縛 | `@Query`付き検索リポジトリ | 未割当行を結果として返す | `IS NULL`条件を使う | 本ラボはクエリ結果の行選択でなく、取得済み関連の初期化を扱う。 |
| JPAエラー一覧のLazyInitialization項目 | 例外の一般的な分類 | 記事のエラー解説 | エラー原因の列挙 | 個別の再現・修正なし | 本ラボはH2統合テスト、二つの取得方法の直接比較、分離コミットを持つ。 |

## 先行Java/Spring教材との差分

先行する`spring-webhook-record-array-dedup-lab`、`java-string-split-trailing-empty-lab`、`java-collectors-tomap-duplicate-key-lab`、`java-priorityqueue-iteration-order-lab`、`java-regex-replacement-literal-lab`、`java-uri-resolve-leading-slash-lab`、`java-map-getordefault-null-lab`、`java-list-remove-integer-overload-lab`、`java-urldecoder-plus-token-lab`、`java-scanner-nextline-newline-lab`、`java-map-merge-null-removal-lab`、`java-string-format-default-locale-lab`は、Javaの値・文字列・コレクション・URI・入力・ロケール規則を扱います。いずれもJPAの関連フェッチ、永続化コンテキスト終了後のDTO変換、EntityGraphの選択を扱いません。

| 軸 | 先行Java/Spring教材群 | 本ラボ |
| --- | --- | --- |
| 直接原因 | 標準ライブラリ、record等値性、Spring MVCの入力同一性 | JPAのLAZY関連初期化とEntityGraphのフェッチ計画 |
| 実境界 | CSV、Map、URI、トークン、Scanner、Webhook | Spring Data JPAリポジトリ、サービス完了後のDTO変換 |
| 観測契約 | 文字列・Map・順序・入力値・HTTP処理 | DB明細の存在とデタッチ済みエンティティのDTO変換 |
| 最小修正 | 標準APIの引数・オーバーロード・実装選択 | 詳細表示時にEntityGraph付き取得を選ぶ |

## 採用判断

本ラボは、原因を「EntityGraph付き詳細取得を選ばない」一点へ、境界をサービス完了後のDTO変換へ、観測をDB最終状態と初期化状態へ限定しました。既存のJPA近接題材と同じエンティティ名やエラー名を言い換えただけではなく、取得時フェッチ計画という別の設計判断を、実行可能な統合テストで検証します。したがって、既存Qiita原稿および先行十三件のJava/Spring教材と重複しない追加題材として採用します。

## References

[1]: https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-entitygraphs/persistence-entitygraphs.html "Jakarta EE Tutorial: Creating Fetch Plans with Entity Graphs"
[2]: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html "Spring Data JPA API: EntityGraph"
