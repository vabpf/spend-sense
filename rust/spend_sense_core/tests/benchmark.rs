use spend_sense_core::aggregator::{aggregate_batch, RawTransactionInput};
use spend_sense_core::forecaster::calculate_month_forecast;
use spend_sense_core::profiler::{profile_category, ProfileTransactionInput};
use std::time::Instant;

#[test]
fn bench_aggregation_10k_transactions() {
    let mut txns = Vec::with_capacity(10_000);
    let base_ts = 1791417600000i64; // 2026-10-08

    for i in 0..10_000 {
        txns.push(RawTransactionInput {
            id: i,
            amount: 10.0 + (i % 100) as f64,
            timestamp: base_ts - (i % 60) * 86_400_000, // span over 60 days
            category_id: (i % 15) as i64,
            payment_source: format!("Card-{}", i % 4),
            payment_source_type: "Credit Card".into(),
        });
    }

    let start = Instant::now();
    let result = aggregate_batch(&txns, 25_200_000); // UTC+7
    let duration = start.elapsed();

    println!(">>> Aggregated 10,000 transactions in: {:?}", duration);
    assert!(result.daily.len() > 0);
    assert!(result.categories.len() > 0);
    assert!(result.payments.len() > 0);
    // 10,000 transactions in Rust should comfortably complete in < 50ms
    assert!(duration.as_millis() < 50, "Aggregation took too long: {:?}", duration);
}

#[test]
fn bench_forecast_10k_iterations() {
    let amounts: Vec<f64> = (0..100).map(|i| 20.0 + (i % 50) as f64).collect();

    let start = Instant::now();
    for _ in 0..10_000 {
        let out = calculate_month_forecast(&amounts, 15, 30, 2500.0, 30);
        assert!(out.projected_month_end_total > 0.0);
    }
    let duration = start.elapsed();

    println!(">>> 10,000 month forecasts with Tukey IQR + Bayes shrinkage in: {:?}", duration);
    // 10,000 full forecasts in < 100ms
    assert!(duration.as_millis() < 100, "Forecasting took too long: {:?}", duration);
}

#[test]
fn bench_profiler_1k_transactions() {
    let mut txns = Vec::with_capacity(1_000);
    let base_ts = 1791417600000i64;

    for i in 0..1_000 {
        txns.push(ProfileTransactionInput {
            amount: 5.0 + (i % 80) as f64,
            timestamp: base_ts + (i as i64 * 60_000), // minute-by-minute
            merchant: format!("Merchant-{}", i % 20),
            notes: None,
        });
    }

    let start = Instant::now();
    let result = profile_category(&txns, "USD");
    let duration = start.elapsed();

    println!(">>> Profiled 1,000 category transactions in: {:?}", duration);
    assert_eq!(result.transaction_count, 1000);
    assert!(duration.as_millis() < 30, "Profiling took too long: {:?}", duration);
}
