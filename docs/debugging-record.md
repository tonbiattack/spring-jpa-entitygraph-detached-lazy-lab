# デバッグ記録: EntityGraphなしでデタッチ後の受領書明細を読めない

## 実行環境と再現境界

このラボはJava 21、Spring Boot 3.4.3、Spring Data JPA、Hibernate、H2、JUnit Jupiterを使います。テストは`TransactionTemplate`で保存・取得のトランザクションを明示的に区切り、サービスから返った受領書をデタッチ済みの状態でDTOへ変換します。

| 境界 | 内容 |
| --- | --- |
| Arrange | `receipt-service-001`と明細`Green tea`をH2へ保存する。 |
| Act | `ReceiptViewService#loadReceiptForView`を呼び、メソッドのトランザクションを完了させる。 |
| Assert | `ReceiptView.from`が見出しと明細名を含むDTOを返すことを確認する。 |
| Observe | 別トランザクションで明細名をDBから読み直し、明細行が存在することを独立に確認する。 |

## 最初に観測した事実

バグコミット[`8c70370`](../../commit/8c70370)で次を実行すると、意図した例外差分が再現します。

```bash
git switch --detach 8c70370
mvn --batch-mode test -Dtest=ReceiptViewServiceTest
git switch main
```

| 観測点 | 期待 | バグ状態の実測 |
| --- | --- | --- |
| DBの明細名 | `["Green tea"]` | `["Green tea"]` |
| サービス完了後のDTO変換 | 成功 | `LazyInitializationException` |
| 例外の対象 | なし | `Receipt.lines`の初期化で「no Session」 |

失敗出力は[`evidence/01-bug-service-test-output.txt`](../evidence/01-bug-service-test-output.txt)に保存しています。DB上の明細は存在するため、カスケード保存や所有側の設定ではなく、取得後に明細を初期化できるかが問題の中心です。

## 競合仮説と検証

| 仮説 | 最小の検証 | 結果 | 判断 |
| --- | --- | --- | --- |
| 明細行が保存されていない | 別トランザクションで詳細取得し、明細名を読む | `Green tea`が存在する | 棄却 |
| DTO変換がサービスのトランザクション内で済んでいる | サービスの戻り値を受け取った後に`ReceiptView.from`を呼ぶ | 呼出し側で例外になる | 棄却 |
| 通常取得がLAZY関連を初期化していない | 通常取得とEntityGraph取得の`Hibernate.isInitialized(lines)`を比較する | 前者はfalse、後者はtrue | 採用 |

直接観測は[`EntityGraphInitializationObservationTest`](../src/test/java/jp/tonbiattack/debuglab/receipt/EntityGraphInitializationObservationTest.java)へ分離しました。バグ状態でも成功する出力は[`evidence/02-entitygraph-initialization-observation-output.txt`](../evidence/02-entitygraph-initialization-observation-output.txt)にあります。

## 確定した原因

Entity graphは、同時取得する永続フィールド群を定義するフェッチ計画です。[1] Spring Data JPAの`@EntityGraph`は、リポジトリメソッドへEntityGraphを設定し、`attributePaths`で動的なフェッチグラフを指定できます。[2]

> Entity graphはクエリまたは永続化操作のテンプレートであり、同時に取得する永続フィールドのフェッチ計画として使われる。— Jakarta EE Tutorial [1]

本ラボの`ReceiptRepository#findDetailedById`には`lines`を指定した`@EntityGraph`がありました。しかし詳細表示サービスは通常の`findById`を使い、`lines`を未初期化のまま返していました。サービスの読み取りトランザクション終了後には永続化コンテキストが利用できないため、DTO変換時のLAZY初期化が失敗します。

## 最小修正

サービス内の取得だけを`findDetailedById`へ置き換えます。

```diff
- return repository.findById(receiptId).orElseThrow();
+ return repository.findDetailedById(receiptId).orElseThrow();
```

修正はコミット[`62bec24`](../../commit/62bec24)にあります。全関連をEAGERへ変える、Open Session in Viewを有効化する、DTO変換を別の層へ移すといった範囲外の変更は加えていません。

## 回帰保証

修正済みの`main`で全統合テストをクリーン実行します。

```bash
mvn --batch-mode clean test
```

完全な成功出力は[`evidence/03-fixed-full-test-output.txt`](../evidence/03-fixed-full-test-output.txt)に保存しています。

### 再発防止テスト

`ReceiptViewServiceTest#loadsLineItemsIntoView_afterServiceTransactionHasCompleted`は、DB上の明細名と、サービス完了後に作るDTOの両方を確認します。詳細画面がLAZYコレクションを未初期化のまま返す回帰を検出します。

`EntityGraphInitializationObservationTest#detailedEntityGraphInitializesLines_butRegularFindByIdDoesNot`は、通常取得とEntityGraph取得の初期化状態を同じH2状態で比較します。これはN+1の一般的な性能測定ではなく、詳細表示用の取得方法を選ぶ根拠を実行可能な形で残す最小観測です。

## 再現手順

修正済み状態は、リポジトリ直下で`mvn --batch-mode clean test`を実行します。バグ状態の確認には`8c70370`へ一時的に切り替え、`mvn --batch-mode test -Dtest=ReceiptViewServiceTest`を実行します。確認後は`git switch main`で修正済み状態へ戻してください。未コミット変更のある作業ツリーで切替を行わないでください。

## スコープと注意点

このラボは、トランザクション後の詳細DTO変換に必要な一つの関連だけをフェッチします。すべての関連を常にEAGERにする指針ではありません。一覧画面、複数階層の詳細画面、ページング、JSONシリアライズ、N+1の最適化では、別の入出力契約とフェッチ計画を設計してください。

## References

[1]: https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-entitygraphs/persistence-entitygraphs.html "Jakarta EE Tutorial: Creating Fetch Plans with Entity Graphs"
[2]: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html "Spring Data JPA API: EntityGraph"
