# Design Notes

## Data loading

`GraphService.loadData()` runs once at startup (`@PostConstruct`). It reads
`transactions-graph-nodes.json` from the classpath with a plain Jackson
`ObjectMapper` into a `GraphDataset` (a thin wrapper around `List<GraphNode>`),
then builds two in-memory maps:

- `Map<String, GraphNode> nodeById` — O(1) lookup by node id.
- `Map<String, List<GraphNode>> childrenByParentId` — O(1) lookup of direct
  children for any node id.

A node is only added to `childrenByParentId` under its `parentId` if that
parent actually exists in `nodeById`. This means a node whose `parentId`
references a missing node is automatically treated as having no parent
(orphan), without any special-casing later in the traversal code.

No database is used; the two maps are enough for the required lookups and
are trivial to swap for an H2-backed repository later if persistence or
querying beyond simple traversal is needed.

## Computing level / parentChain / children

- `buildParentChain(node)` walks `parentId` upward starting from the node,
  collecting ancestors into a list, then reverses it so the result reads
  root → ... → direct parent. It stops as soon as `parentId` is `null` or
  points to a missing node.
- `level` is simply `parentChain.size()`.
- `isRoot` is `true` when the node has no resolvable parent (`parentId ==
  null` or parent missing).
- `isLeaf` is `true` when `childrenByParentId` has no entry for the node.
- Direct children come straight from `childrenByParentId.get(nodeId)`.
- `nextLevelTransactions` is simply the concatenation of `transactions` across
  all direct children.

## Depth-limited children tree (bonus)

`buildChildTree` is a straightforward recursive DFS bounded by `maxDepth`,
starting the requested node's direct children at relative depth 1. Query
param validation happens first in the controller/service layer:
`maxDepth` must be in `[0, 10]` or a `400 INVALID_REQUEST` is returned.

## Aggregate information by level (bonus)

`computeLevelAggregates` runs a BFS over the subtree rooted at the requested
node (level 0 = the requested node itself), accumulating per-level
`nodeCount`, `transactionCount`, and `totalAmount` (sum of absolute
transaction amounts) into `TreeMap`s so the levels come back sorted.

## Cycle detection

Any traversal that walks node→parent or node→children (parent chain
construction, children-tree construction, level aggregation) carries its own
`visited` set of node ids. If a node id is encountered a second time within
the same traversal, a `CycleDetectedException` is thrown, which the
`GlobalExceptionHandler` converts into an HTTP `400` with
`{"error": "CYCLE_DETECTED", ...}`. This guarantees no traversal can loop
forever even if a future dataset introduces a cycle.

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) centralizes exception →
HTTP response mapping:

- `NodeNotFoundException` → `404 NODE_NOT_FOUND`
- `InvalidRequestException` → `400 INVALID_REQUEST`
- `CycleDetectedException` → `400 CYCLE_DETECTED`
- anything else → `500 INTERNAL_ERROR`

## Scaling / extending

- Swap the in-memory maps for a real repository (e.g., JPA + H2/Postgres)
  behind the same `GraphService` interface if the dataset grows or needs to
  be mutated at runtime; the traversal logic (parent chain, children tree,
  aggregates) stays the same, only the lookup implementation changes.
- For very deep/wide graphs, `computeLevelAggregates` and `buildChildTree`
  are already bounded traversals (BFS/DFS with visited sets), so they scale
  linearly with subtree size rather than the whole dataset.
- Pagination could be added to `children-transactions` if the number of
  matching transactions becomes large.
