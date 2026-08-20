# EntityGraphなしでデタッチ後の受領書明細を読めないデバッグラボ

この教材は、Spring Data JPAで通常の`findById`を使って受領書を取得し、トランザクション完了後にLAZYな明細コレクションをDTOへ変換しようとして`LazyInitializationException`になる不具合を再現・修正します。Entity graphは、クエリや永続化操作で同時取得するフィールド群を表すフェッチ計画です。[1] Spring Data JPAの`@EntityGraph(attributePaths = ...)`は、リポジトリメソッドへ動的なフェッチグラフを指定できます。[2]

| 項目 | 内容 |
| --- | --- |
| 対象 | Java 21、Spring Boot 3.4.3、Spring Data JPA、Hibernate、H2、JUnit Jupiter |
| 原因 | 詳細表示のサービスが通常の`findById`を使い、LAZYな`lines`を未初期化のままデタッチする |
| バグコミット | [`8c70370`](../../commit/8c70370) — デタッチ後に受領書明細を読めない状態を再現する |
| 修正コミット | [`62bec24`](../../commit/62bec24) — 詳細取得にEntityGraphを指定する |
| 実行境界 | `@SpringBootTest`、H2、実際の`JpaRepository`、`TransactionTemplate`、サービス完了後のDTO変換 |

## この題材で守る契約

`receipt-service-001`を詳細表示用に取得した後、サービスの読み取りトランザクションが完了していても、`Green tea`を含む`ReceiptView`へ変換できなければなりません。テストは、DB上の明細が一件残ることと、デタッチ済みエンティティからDTOを作れることを分けて確認します。

| 観測点 | 正しい状態 | バグ状態 |
| --- | --- | --- |
| DBから読み直した明細名 | `["Green tea"]` | `["Green tea"]` |
| 通常取得の`lines`初期化状態 | 未初期化でもよい | 未初期化 |
| サービス完了後のDTO変換 | 成功し、明細名を返す | `LazyInitializationException` |

## 最短の開始手順

修正済みの`main`で、H2を使う統合テスト全体を実行します。

```bash
mvn --batch-mode clean test
```

`ReceiptViewServiceTest`はサービス完了後のDTO変換と最終DB状態を、`EntityGraphInitializationObservationTest`は通常取得とEntityGraph取得の初期化状態を直接確認します。完全な成功出力は[`evidence/03-fixed-full-test-output.txt`](evidence/03-fixed-full-test-output.txt)に保存しています。

## バグを再現する

以下は意図した失敗を確認する手順です。未コミット変更のない作業ツリーで実行し、確認後には`main`へ戻してください。

```bash
git switch --detach 8c70370
mvn --batch-mode test -Dtest=ReceiptViewServiceTest
git switch main
```

バグコミットでは、DBへ明細が正しく保存されている確認は成功します。しかしサービスは通常の`findById`で受領書を返すため、トランザクション完了後に`ReceiptView.from`が`lines`を読むと`LazyInitializationException`になります。失敗出力は[`evidence/01-bug-service-test-output.txt`](evidence/01-bug-service-test-output.txt)に保存しています。

原因をサービス層から切り出すため、[`EntityGraphInitializationObservationTest`](src/test/java/jp/tonbiattack/debuglab/receipt/EntityGraphInitializationObservationTest.java)は同一のDB状態で通常取得と`@EntityGraph(attributePaths = "lines")`付き詳細取得を比較します。通常取得では`lines`が未初期化、詳細取得では初期化済みです。この直接観測テストはバグ状態でも成功し、出力は[`evidence/02-entitygraph-initialization-observation-output.txt`](evidence/02-entitygraph-initialization-observation-output.txt)にあります。

## 原因と最小修正

`ReceiptRepository`には明細をフェッチ計画へ含める`findDetailedById`を用意していましたが、詳細表示サービスが通常の`findById`を選んでいました。修正はサービス内の取得メソッドを一行置き換えるだけです。

```diff
- return repository.findById(receiptId).orElseThrow();
+ return repository.findDetailedById(receiptId).orElseThrow();
```

この変更は詳細表示という目的の取得だけへ`lines`のフェッチを限定します。Open Session in View、JSONシリアライズ、全関連のEAGER化、N+1最適化、JOIN FETCHとページング、HTTP APIは変更しません。詳しい調査は[デバッグ記録](docs/debugging-record.md)、既存題材との比較は[新規性レポート](docs/novelty-report.md)を参照してください。

## プロジェクト構成

| パス | 役割 |
| --- | --- |
| `src/main/java/.../Receipt.java` | LAZYな明細コレクションを持つ受領書エンティティ |
| `src/main/java/.../ReceiptRepository.java` | 通常取得と`@EntityGraph`付き詳細取得を提供するリポジトリ |
| `src/main/java/.../ReceiptViewService.java` | バグと最小修正の対象となる詳細表示サービス |
| `src/main/java/.../ReceiptView.java` | トランザクション後に明細名を読む最小DTOマッパー |
| `src/test/java/.../ReceiptViewServiceTest.java` | 公開サービス契約と最終DB状態を検証する統合テスト |
| `src/test/java/.../EntityGraphInitializationObservationTest.java` | 初期化状態を直接比較する観測テスト |
| `evidence/` | バグ状態、直接観測、修正状態のMaven出力 |

## References

[1]: https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-entitygraphs/persistence-entitygraphs.html "Jakarta EE Tutorial: Creating Fetch Plans with Entity Graphs"
[2]: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html "Spring Data JPA API: EntityGraph"
