use std::hint::black_box;
use std::time::{Duration, Instant};

fn shim_bundle() -> &'static [u8] {
    include_bytes!("../../jdk-shim/bundle.bin")
}

fn bench_bundle() -> &'static [u8] {
    include_bytes!("../../test-classes/bench-bundle.bin")
}

fn combined_bundle(shim: &[u8], app: &[u8]) -> Vec<u8> {
    let mut v = Vec::with_capacity(shim.len() + app.len());
    v.extend_from_slice(shim);
    v.extend_from_slice(app);
    v
}

fn run_bench_class(bundle: &[u8], class: &str) -> i32 {
    let result = jvm_core::run_static_native(bundle, class, "run", "()I");
    result
        .parse::<i32>()
        .unwrap_or_else(|e| panic!("unexpected bench result from {class}: {result:?} ({e})"))
}

fn measure(bundle: &[u8], class: &str, iterations: usize) -> Duration {
    let mut checksum = 0i64;
    let start = Instant::now();
    for _ in 0..iterations {
        checksum += i64::from(run_bench_class(bundle, class));
    }
    black_box(checksum);
    start.elapsed()
}

fn assert_ratio(label: &str, numerator: Duration, denominator: Duration, max_ratio: f64) {
    let ratio = numerator.as_secs_f64() / denominator.as_secs_f64();
    assert!(
        ratio <= max_ratio,
        "{label} regressed to {ratio:.2}x (limit {max_ratio:.2}x, numerator={numerator:?}, denominator={denominator:?})"
    );
}

#[test]
fn utf16_perf_guardrails() {
    let bundle = combined_bundle(shim_bundle(), bench_bundle());
    const ITERS: usize = 6;

    black_box(run_bench_class(&bundle, "BenchStringUtf16Ascii"));
    black_box(run_bench_class(&bundle, "BenchStringUtf16Cjk"));
    black_box(run_bench_class(&bundle, "BenchStringUtf16Emoji"));
    black_box(run_bench_class(&bundle, "BenchRegexFindAscii"));
    black_box(run_bench_class(&bundle, "BenchRegexFindCjk"));
    black_box(run_bench_class(&bundle, "BenchRegexFindEmoji"));
    black_box(run_bench_class(&bundle, "BenchRegexFindEmptyEmoji"));

    let string_ascii = measure(&bundle, "BenchStringUtf16Ascii", ITERS);
    let string_cjk = measure(&bundle, "BenchStringUtf16Cjk", ITERS);
    let string_emoji = measure(&bundle, "BenchStringUtf16Emoji", ITERS);
    let regex_ascii = measure(&bundle, "BenchRegexFindAscii", ITERS);
    let regex_cjk = measure(&bundle, "BenchRegexFindCjk", ITERS);
    let regex_emoji = measure(&bundle, "BenchRegexFindEmoji", ITERS);

    black_box(measure(&bundle, "BenchRegexFindEmptyEmoji", ITERS));

    assert_ratio("string_utf16_cjk / string_utf16_ascii", string_cjk, string_ascii, 6.0);
    assert_ratio(
        "regex_find_cjk / regex_find_ascii",
        regex_cjk,
        regex_ascii,
        6.0,
    );
    assert_ratio(
        "string_utf16_emoji / string_utf16_ascii",
        string_emoji,
        string_ascii,
        12.0,
    );
    assert_ratio(
        "regex_find_emoji / regex_find_ascii",
        regex_emoji,
        regex_ascii,
        12.0,
    );
}
