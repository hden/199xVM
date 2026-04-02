# Benchmark Guide

Benchmarks measure the interpreter hot paths identified in [Issue #13](https://github.com/kawasima/199xVM/issues/13).
Unicode-sensitive string and regex benchmarks also track the UTF-16 compatibility work from [Issue #77](https://github.com/kawasima/199xVM/issues/77).

## Prerequisites

Build the JDK shim and benchmark class bundles before running:

```sh
./build-shim.sh          # builds jdk-shim/bundle.bin
./build-test-bundle.sh   # builds test-classes/bench-bundle.bin
```

Using the repo's container workflow:

```sh
docker-compose run --rm java make test-bundle
```

## Running benchmarks

```sh
cargo bench --package jvm-core
```

Using Docker:

```sh
docker-compose run --rm rust cargo bench --package jvm-core
```

Results are written to `target/criterion/` as HTML reports.

## Benchmark scenarios

| Name | Java class | Issue #13 bottleneck |
|---|---|---|
| `method_call_1000x` | `BenchMethodCall.run()` | O(n) method lookup + constant pool clone per static call |
| `static_field_1000x` | `BenchStaticField.run()` | `format!` string allocation on every `getstatic`/`putstatic` |
| `inherited_static_field_1000x` | `BenchInheritedStaticField.run()` | inherited `getstatic`/`putstatic` owner resolution on every access |
| `string_ldc_1000x` | `BenchStringLdc.run()` | `String::clone` on every `ldc` string constant |
| `virtual_call_1000x` | `BenchVirtualCall.run()` | Interface virtual dispatch + interface name list rebuild |
| `string_utf16_ascii_10000x` | `BenchStringUtf16Ascii.run()` | ASCII baseline for UTF-16 string operations |
| `string_utf16_cjk_10000x` | `BenchStringUtf16Cjk.run()` | CJK path guard for UTF-8 multibyte / UTF-16 single-unit indexing |
| `string_utf16_emoji_10000x` | `BenchStringUtf16Emoji.run()` | Surrogate-pair path guard for UTF-16 string operations |
| `regex_find_ascii_10000x` | `BenchRegexFindAscii.run()` | ASCII baseline for `Matcher.find()` |
| `regex_find_cjk_10000x` | `BenchRegexFindCjk.run()` | CJK regression guard for byte/code-unit conversion in regex search |
| `regex_find_emoji_10000x` | `BenchRegexFindEmoji.run()` | Surrogate-pair path guard for regex offset conversion |
| `regex_find_empty_emoji_10000x` | `BenchRegexFindEmptyEmoji.run()` | Empty-pattern UTF-16 boundary enumeration on surrogate pairs |
| `declared_methods_1000x` | `BenchDeclaredMethods.run()` | repeated `Class.getDeclaredMethods()` metadata rebuild |
| `process_clinit_launch_to_exit` | `ClinitYieldProcessMain.main()` | launcher/process path when the first bytecode schedules a heavy `<clinit>` |
| `process_super_clinit_chain_launch_to_exit` | `ClinitChainYieldProcessMain.main()` | launcher/process path when class initialization walks a heavy superclass chain |

Each Java method runs an inner loop of either 1000 or 10000 iterations so the per-call overhead is amplified and measurable above criterion's noise floor.

The Unicode-specific scenarios are intentionally split by text shape:

- CJK benchmarks isolate the original `#77` failure mode where UTF-8 byte offsets and UTF-16-visible indexes diverged even without surrogate pairs.
- Emoji benchmarks isolate surrogate-pair handling and the empty-pattern boundary path. They are correctness-sensitive first and performance-sensitive second.

## Comparing before and after a fix

Save a named baseline before applying a fix:

```sh
cargo bench --package jvm-core -- --save-baseline before
```

Apply the fix, then compare:

```sh
cargo bench --package jvm-core -- --baseline before
```

criterion will print a percentage change and confidence interval for each benchmark.

For the new UTF-16 scenarios, a targeted compare is often easier to read:

```sh
docker-compose run --rm rust cargo bench --package jvm-core --bench interpreter -- \
  string_utf16_cjk_10000x regex_find_cjk_10000x \
  string_utf16_emoji_10000x regex_find_emoji_10000x
```

## PR guardrail

For pull requests, use the lightweight relative-ratio gate before looking at full Criterion output:

```sh
docker-compose run --rm rust cargo test --release --package jvm-core --test perf_guardrail_test
```

Current guardrails:

- `string_utf16_cjk / string_utf16_ascii <= 6x`
- `regex_find_cjk / regex_find_ascii <= 6x`
- `string_utf16_emoji / string_utf16_ascii <= 12x`
- `regex_find_emoji / regex_find_ascii <= 12x`

`regex_find_empty_emoji_10000x` is benchmarked in Criterion but intentionally not used as a hard PR gate because the path is correctness-sensitive and more timing-noise-prone.

The historical tables below predate the later inherited-static-field, class-init, and UTF-16/regex
scenarios, so they currently cover only the original four microbenchmarks.

## Baseline (2026-03-12, unoptimized interpreter)

| Benchmark | Time (median) |
|---|---|
| `method_call_1000x` | 4.91 ms |
| `static_field_1000x` | 4.15 ms |
| `string_ldc_1000x` | 3.54 ms |
| `virtual_call_1000x` | 5.06 ms |

## After Issue #13 fixes (2026-03-12)

Changes: `resolve_method_exec_info` helper (eliminates repeated `find_method` calls),
`ConstantPool.entries` wrapped in `Rc` (O(1) clone), `static_fields` restructured to
`HashMap<class, HashMap<field, value>>` (no `format!` key), `intern_string` uses `entry` API.

| Benchmark | Before | After | Change |
|---|---|---|---|
| `method_call_1000x` | 4.91 ms | 4.39 ms | **-10.7%** |
| `static_field_1000x` | 4.15 ms | 3.93 ms | **-5.4%** |
| `string_ldc_1000x` | 3.54 ms | 3.68 ms | ~0% (noise) |
| `virtual_call_1000x` | 5.06 ms | 4.54 ms | **-10.3%** |
