# 題材企画: `@EntityGraph`なしでデタッチ後に明細を読んで失敗する

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象技術 | Java 21、Spring Boot、Spring Data JPA、Hibernate、H2、JUnit Jupiter |
| 対象読者 | トランザクション境界の外側で、明細などの`@OneToMany(fetch = LAZY)`を読むSpring Data JPA開発者 |
| 難易度プロファイル | 実践・上級 |
| 選定理由 | 受領書の見出しと明細をトランザクション内で取得し、トランザクション完了後に明細名を参照する契約で、通常の`findById`はLAZYコレクションを初期化していないため`LazyInitializationException`になる。リポジトリの取得方法とトランザクション完了後のDTO変換を実境界で観測できる。 |
| 実行基盤 | Maven、Java 21、Spring Boot 3.4.3、Spring Data JPA、Hibernate、H2、JUnit Jupiter |
| JPA固有性 | 原因は永続化コンテキストを離れたLAZY関連の初期化時点と、Spring Data JPAの`@EntityGraph`によるフェッチ計画の指定にある。 |

## 学習する契約

> 保存済みの受領書`receipt-001`を詳細表示用に取得する場合、サービスメソッドのトランザクションが完了した後でも、明細名`"Green tea"`をDTOへ変換できなければならない。バグ状態では通常の`findById`がLAZYな`lines`を未初期化のまま返し、デタッチ後の変換で例外になる。

### 対象の直接原因

詳細表示用の取得で`@EntityGraph(attributePaths = "lines")`を指定せず、遅延関連を初期化していない。サービスメソッドのトランザクション終了後には永続化コンテキストが閉じるため、未初期化コレクションを読めない。

### 対象外

このラボはOpen Session in View、JSONシリアライズ、N+1の件数測定、JOIN FETCHとページング、複数階層のEntityGraph、キャッシュ、HTTPコントローラー、トランザクション伝播を扱わない。単一の受領書・一つの`@OneToMany`・トランザクション後のDTO変換だけを扱う。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `ReceiptViewService#loadReceiptView(Long)`。内部ではSpring Data JPAリポジトリを使い、戻り値は明細名を含むDTOとする。 |
| 入力・初期状態 | `receipt-001`と明細`Green tea`をH2へ保存し、サービスから詳細DTOを取得する。 |
| Redの観測 | DTOの明細名が`["Green tea"]`であるべきだが、バグ状態では`LazyInitializationException`が発生する。 |
| 最終観測 | DBから再読込した受領書に明細行が一件あること、サービスの戻りDTOが見出しと明細名を持つことを独立に確認する。 |
| 直接観測 | 通常取得と`@EntityGraph`取得について、トランザクション内で`Hibernate.isInitialized(lines)`を比較し、後者だけが初期化済みであることを確認する。 |
| 決定性 | H2インメモリDB、固定文字列、Springの`TransactionTemplate`を使う。時刻、乱数、ネットワーク、外部I/Oは使わない。 |
| 固定状態の検証コマンド | `mvn --batch-mode clean test` |
| バグ状態の確認コマンド | `git checkout 8c70370`で`mvn --batch-mode test -Dtest=ReceiptViewServiceTest`を実行する。 |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| A: 明細行がDBへ保存されていない | EntityManagerをclearした後に受領書を再読込し、DB上の明細件数と明細名を確認する。 |
| B: サービスがトランザクションを離れる前にDTOへ変換している | サービスの戻り値をエンティティにして、呼出し側で明細へアクセスする境界を確認する。 |
| C: 通常取得がLAZY関連を初期化していない | `Hibernate.isInitialized`で通常取得とEntityGraph取得の状態を比較する。 |

## 予定する履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | デタッチ後に受領書明細を読めない状態を再現する | DBの明細行は存在するが、サービス呼出し後のDTO変換で`LazyInitializationException`が発生する。 |
| 2 | 詳細取得にEntityGraphを指定する | 同じ統合テストと全テストが成功する。 |
