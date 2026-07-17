# Transaction Graph Node Explorer

A Spring Boot REST service that exposes APIs to explore a hierarchical transaction graph
made of `GraphNode`s (each holding a list of `NodeTransaction`s).

## Tech Stack
- Java 17
- Spring Boot 3.2.5 (Spring Web only, no database)
- Maven
- Jackson for JSON binding (comes bundled with `spring-boot-starter-web`)

## Running the app

```bash
mvn spring-boot:run
```

or build a jar and run it:

```bash
mvn clean package
java -jar target/graph-explorer-1.0.0.jar
```

The app starts on `http://localhost:8080`.

The dataset is loaded once at startup from
`src/main/resources/transactions-graph-nodes.json` into two in-memory maps
(`nodeById`, `childrenByParentId`) — no database is used.

## Endpoints

### 1. `GET /api/graph/nodes/{id}`

Returns node details, its level, its parent chain, its direct children, its own
transactions, and the transactions of its direct children (`nextLevelTransactions`).

```bash
curl http://localhost:8080/api/graph/nodes/N2
```

Optional query params:

- `maxDepth` (default `1`) — when `> 1`, an additional nested `childrenTree`
  field is returned, built via recursive traversal down to `maxDepth` levels
  below the requested node, with cycle protection via a visited set.

  ```bash
  curl "http://localhost:8080/api/graph/nodes/N1?maxDepth=3"
  ```

- `withAggregates=true` — adds a `levelAggregates` array with, for the subtree
  rooted at the requested node: `level`, `nodeCount`, `transactionCount`,
  `totalAmount` (sum of absolute transaction amounts at that level).

  ```bash
  curl "http://localhost:8080/api/graph/nodes/N1?withAggregates=true"
  ```

If the node id does not exist, the API returns `404` with:

```json
{ "error": "NODE_NOT_FOUND", "message": "Graph node N999 does not exist" }
```

If `maxDepth` is outside `[0, 10]`, the API returns `400` with
`{ "error": "INVALID_REQUEST", ... }`.

If a cycle is detected while walking the parent chain or the children tree,
the API returns `400` with `{ "error": "CYCLE_DETECTED", ... }`.

### 2. `GET /api/graph/nodes/{id}/children-transactions`

Returns node details/level plus only the direct-children transactions that
match the supplied filters.

```bash
curl "http://localhost:8080/api/graph/nodes/N1/children-transactions?minAmount=1000&maxAmount=2000&txnType=POS"
```

Query params (all optional, combinable): `minAmount`, `maxAmount`, `txnType`
(`POS`, `ATM`, `TRANSFER`, `SALARY`).

## Level & orphan handling

- `level` = 0 for a node whose `parentId` is `null`.
- `level` = 0 for a node whose `parentId` points to a missing node (orphan —
  treated as root-like). In that case `parentChain = []` and `isRoot = true`.
- Otherwise `level` = `parent.level + 1`, computed by walking the parent chain.
- `isLeaf` = `true` when the node has no direct children in the dataset.

## Assumptions

- Nodes and transactions are not assumed to be in any particular order in the
  source JSON.
- `totalAmount` in level aggregates is the sum of absolute transaction
  amounts (direction is not netted); this can be changed easily in
  `GraphService.computeLevelAggregates` if signed totals are preferred.
- Spring Boot 3.2.5 / Java 17 was used, so no `javax.*` imports — `jakarta.annotation.PostConstruct`
  is used instead.
