# Loader Cache Rules

This file defines cache rules for loader-aware class identity, class loading,
and symbolic reference resolution in 199xVM.

Loader-aware resolution is the baseline: class identity includes the defining
loader, and symbolic references resolve in the context of the referring class or
interface. A cache that drops that context is invalid.

---

## Scope

This file covers:

- loader-scoped class identity records;
- initiating-loader records;
- `Class` mirror reuse;
- array class identity records;
- pure class-name query normalization;
- symbolic class, field, method, and interface method resolution caches;
- exact cached resolution failures.

---

## Terminology

- **Binary name** means the Java-visible binary name used in JVM class identity,
  such as `java.lang.String`.
- **Internal name** means an implementation form such as `java/lang/String`.
- **Class identity** means a non-array class or interface identified by binary
  name plus defining loader.
- **Resolved reference** means the canonical result of loader-aware symbolic
  resolution.
- **Name query** means an input string plus the grammar for a loader-facing entry
  point such as `Class.forName(String)`.
- **Normalized class query** means the pure parse result of a name query before
  loader-aware resolution.

A constructor target is a resolved method target for `<init>` with declaring
class identity and descriptor preserved.

---

## General Rule

Caches in this subsystem may store only:

- immutable loader-independent metadata;
- canonical class identity records;
- canonical resolved references;
- exact resolution failures for an exact symbolic-reference context;
- pure normalized class queries.

They must not store:

- unresolved name guesses;
- global same-name fallback results;
- ad-hoc fallback outputs;
- values that require later loader-context repair;
- mutable runtime state;
- workload-specific results.

---

## Global Immutable Metadata

A global cache may store only immutable, loader-independent,
context-independent values.

Allowed values:

- parsed descriptors and method descriptors;
- immutable class-file parse output keyed by immutable bytes or a byte hash;
- immutable constant-pool decode helpers;
- parsed name-query results.

Forbidden values:

- class identity;
- `Class` mirrors;
- resolved member owners;
- per-caller symbolic resolution results;
- mutable runtime state;
- broad negative lookup results.

Required key properties:

- descriptor caches are keyed by descriptor string;
- class-file metadata caches are keyed by immutable bytes or an equivalent hash;
- name-query caches are keyed by input string plus entry grammar.

---

## Loader-Owned Identity Records

Loader-owned caches store loader-visible class identity facts. They are part of
the loader model and must not redefine class identity.

Allowed values:

- `(defining loader, binary name) -> class identity`;
- `(initiating loader, binary name) -> class identity`;
- `class identity -> binary name`;
- `class identity -> defining loader`;
- `class identity -> Class mirror`.

Array-name keys must preserve JVM array-name form. Non-array identity keys must
preserve Java-visible binary names even when the VM also keeps internal
slash-separated names.

Forbidden values:

- unresolved field or method owner guesses;
- speculative fallback results;
- dispatch shortcuts for a call site;
- mutable field values or initialization state.

---

## Resolution Caches

A resolution cache may store only canonical results of symbolic resolution.

A value may enter this cache after loader-aware resolution selects:

- the resolved class identity;
- the declaring field owner, field name, and field descriptor;
- the declaring method or interface owner, method name, and method descriptor;
- the exact JVM error for a failed resolution.

Required key properties:

- caller class identity is preserved;
- symbolic-reference context is preserved, such as constant-pool index or an
  equivalent stable reference identity;
- resolution kind is preserved;
- owner identity, member name, and descriptor are preserved where relevant.

Valid key shapes include:

- `(caller class identity, constant-pool index, class resolution)`;
- `(caller class identity, constant-pool index, field resolution)`;
- `(caller class identity, constant-pool index, method resolution)`;
- `(caller class identity, constant-pool index, interface method resolution)`.

Equivalent keys are valid only if they preserve the same distinctions.

Forbidden values:

- binary-name-only owners;
- unresolved symbolic names as final targets;
- ad-hoc fallback outputs;
- broad "not found" results;
- results that still require later loader-context repair.

---

## Name Query Normalization

A name-query normalization cache may be global only when it is pure and
loader-independent.

Its key must include:

- original input string;
- entry grammar.

A normalized class query may contain:

- original input;
- entry grammar;
- normalized kind, such as class, array, primitive, or invalid;
- normalized binary name or array name when applicable;
- normalized component query when applicable;
- deterministic parse failure reason.

It must not contain:

- resolved `Class` mirrors;
- defining or initiating loader;
- access decisions;
- assignability decisions;
- linkage outcomes.

---

## Negative Results

A failed resolution may be cached only when it belongs to an exact caller,
symbolic reference, and resolution kind, and repeating that resolution must fail
with the same JVM error.

Allowed failures:

- exact failed class resolution;
- exact failed field resolution;
- exact failed method resolution;
- exact failed interface method resolution.

Forbidden failures:

- "loader L does not know class N";
- "method M is not found anywhere";
- "field F is not found anywhere";
- any failure cached without caller identity, symbolic-reference context, and
  resolution kind.

---

## Runtime State

Mutable runtime state is not cache.

Forbidden cached values:

- static field values;
- instance field values;
- object contents;
- class initialization status;
- monitor state;
- mutable reflection state.

Resolved identities may be cached. Runtime values and side effects attached to
those identities must remain runtime state.

---

## Invalidation

A cache does not need invalidation when it stores only:

- immutable metadata;
- canonical identity records;
- exact resolution results for stable symbolic-reference contexts;
- pure name-query normalization results.

A cache requiring invalidation is valid only if its owner, invalidation trigger,
and stale-read prevention rule are explicit.

If invalidation cannot be specified precisely, the cache must not exist.
