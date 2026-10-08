use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct RawTransactionInput {
    pub id: i64,
    pub amount: f64,
    pub timestamp: i64,
    pub category_id: i64,
    pub payment_source: String,
    pub payment_source_type: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct DailyRollup {
    pub date_key: String,
    pub year: i32,
    pub month: i32,
    pub day: i32,
    pub day_start: i64,
    pub total_amount: f64,
    pub transaction_count: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct CategoryRollup {
    pub year: i32,
    pub month: i32,
    pub category_id: i64,
    pub total_amount: f64,
    pub transaction_count: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct PaymentRollup {
    pub year: i32,
    pub month: i32,
    pub payment_source: String,
    pub payment_source_type: String,
    pub total_amount: f64,
    pub transaction_count: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct AggregationResult {
    pub daily: Vec<DailyRollup>,
    pub categories: Vec<CategoryRollup>,
    pub payments: Vec<PaymentRollup>,
}

/// Extracts (date_key "YYYY-MM-DD", year, month 0-indexed, day, day_start_millis)
/// from an epoch timestamp (UTC ms) and timezone offset in milliseconds.
pub fn epoch_to_date_info(timestamp_ms: i64, tz_offset_ms: i64) -> (String, i32, i32, i32, i64) {
    let local_ms = timestamp_ms + tz_offset_ms;
    let days = if local_ms >= 0 {
        local_ms / 86_400_000
    } else {
        (local_ms - 86_399_999) / 86_400_000
    };
    let day_start = days * 86_400_000 - tz_offset_ms;

    // Civil date from epoch day (Euclidean affine algorithm by Peter Baum)
    let z = days + 719468;
    let era = if z >= 0 { z } else { z - 146096 } / 146097;
    let doe = (z - era * 146097) as u32;
    let yoe = (doe - doe / 1029 + doe / 10957 - doe / 146096) / 365;
    let y = yoe as i32 + era as i32 * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = (doy - (153 * mp + 2) / 5 + 1) as i32;
    let m = (if mp < 10 { mp + 3 } else { mp - 9 }) as i32; // 1..12
    let actual_year = if m <= 2 { y + 1 } else { y };
    let month_zero_based = m - 1; // 0..11

    let date_key = format!("{:04}-{:02}-{:02}", actual_year, m, d);
    (date_key, actual_year, month_zero_based, d, day_start)
}

pub fn aggregate_batch(transactions: &[RawTransactionInput], tz_offset_ms: i64) -> AggregationResult {
    let mut daily_map: HashMap<String, DailyRollup> = HashMap::with_capacity(64);
    let mut category_map: HashMap<(i32, i32, i64), CategoryRollup> = HashMap::with_capacity(32);
    let mut payment_map: HashMap<(i32, i32, String, String), PaymentRollup> = HashMap::with_capacity(32);

    for txn in transactions {
        let (date_key, year, month, day, day_start) = epoch_to_date_info(txn.timestamp, tz_offset_ms);

        // Daily
        daily_map
            .entry(date_key.clone())
            .and_modify(|d| {
                d.total_amount += txn.amount;
                d.transaction_count += 1;
            })
            .or_insert(DailyRollup {
                date_key,
                year,
                month,
                day,
                day_start,
                total_amount: txn.amount,
                transaction_count: 1,
            });

        // Category
        category_map
            .entry((year, month, txn.category_id))
            .and_modify(|c| {
                c.total_amount += txn.amount;
                c.transaction_count += 1;
            })
            .or_insert(CategoryRollup {
                year,
                month,
                category_id: txn.category_id,
                total_amount: txn.amount,
                transaction_count: 1,
            });

        // Payment
        let src = if txn.payment_source.trim().is_empty() {
            "Manual".to_string()
        } else {
            txn.payment_source.trim().to_string()
        };
        let src_type = if txn.payment_source_type.trim().is_empty() {
            "Manual".to_string()
        } else {
            txn.payment_source_type.trim().to_string()
        };

        payment_map
            .entry((year, month, src.clone(), src_type.clone()))
            .and_modify(|p| {
                p.total_amount += txn.amount;
                p.transaction_count += 1;
            })
            .or_insert(PaymentRollup {
                year,
                month,
                payment_source: src,
                payment_source_type: src_type,
                total_amount: txn.amount,
                transaction_count: 1,
            });
    }

    let mut daily: Vec<DailyRollup> = daily_map.into_values().collect();
    daily.sort_by(|a, b| b.date_key.cmp(&a.date_key));

    let mut categories: Vec<CategoryRollup> = category_map.into_values().collect();
    categories.sort_by(|a, b| b.total_amount.partial_cmp(&a.total_amount).unwrap_or(std::cmp::Ordering::Equal));

    let mut payments: Vec<PaymentRollup> = payment_map.into_values().collect();
    payments.sort_by(|a, b| b.total_amount.partial_cmp(&a.total_amount).unwrap_or(std::cmp::Ordering::Equal));

    AggregationResult {
        daily,
        categories,
        payments,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_date_conversion() {
        // 2026-10-08T00:00:00Z = 1791417600 seconds = 1791417600000 ms
        let (key, year, month, day, _) = epoch_to_date_info(1791417600000, 0);
        assert_eq!(key, "2026-10-08");
        assert_eq!(year, 2026);
        assert_eq!(month, 9); // 0-based October
        assert_eq!(day, 8);
    }

    #[test]
    fn test_leap_year_feb29() {
        // 2024-02-29T12:00:00Z = 1709208000000 ms
        let (key, year, month, day, _) = epoch_to_date_info(1709208000000, 0);
        assert_eq!(key, "2024-02-29");
        assert_eq!(year, 2024);
        assert_eq!(month, 1); // 0-based February
        assert_eq!(day, 29);
    }

    #[test]
    fn test_year_end_boundary() {
        // 2025-12-31T23:59:59Z = 1767225599000 ms
        let (key, year, month, day, _) = epoch_to_date_info(1767225599000, 0);
        assert_eq!(key, "2025-12-31");
        assert_eq!(year, 2025);
        assert_eq!(month, 11); // 0-based December
        assert_eq!(day, 31);

        // 1 second later: 2026-01-01T00:00:00Z = 1767225600000 ms
        let (key_new, year_new, month_new, day_new, _) = epoch_to_date_info(1767225600000, 0);
        assert_eq!(key_new, "2026-01-01");
        assert_eq!(year_new, 2026);
        assert_eq!(month_new, 0); // 0-based January
        assert_eq!(day_new, 1);
    }

    #[test]
    fn test_timezone_offset_day_shift() {
        // 2026-10-08T22:00:00Z = 1791496800000 ms
        // UTC: Oct 8
        let (key_utc, _, _, day_utc, _) = epoch_to_date_info(1791496800000, 0);
        assert_eq!(key_utc, "2026-10-08");
        assert_eq!(day_utc, 8);

        // UTC+7 (+25,200,000 ms): 22:00 + 7h = next day 05:00 AM Oct 9
        let (key_tz7, _, _, day_tz7, _) = epoch_to_date_info(1791496800000, 25_200_000);
        assert_eq!(key_tz7, "2026-10-09");
        assert_eq!(day_tz7, 9);
    }

    #[test]
    fn test_empty_batch() {
        let result = aggregate_batch(&[], 0);
        assert!(result.daily.is_empty());
        assert!(result.categories.is_empty());
        assert!(result.payments.is_empty());
    }

    #[test]
    fn test_blank_payment_sources_defaults_to_manual() {
        let txns = vec![RawTransactionInput {
            id: 1,
            amount: 25.0,
            timestamp: 1791417600000,
            category_id: 5,
            payment_source: "".into(),
            payment_source_type: "  ".into(),
        }];

        let result = aggregate_batch(&txns, 0);
        assert_eq!(result.payments.len(), 1);
        assert_eq!(result.payments[0].payment_source, "Manual");
        assert_eq!(result.payments[0].payment_source_type, "Manual");
    }

    #[test]
    fn test_aggregate_batch_sorting() {
        let txns = vec![
            RawTransactionInput {
                id: 1,
                amount: 50.0,
                timestamp: 1791417600000, // Oct 8
                category_id: 10,
                payment_source: "Chase".into(),
                payment_source_type: "Credit Card".into(),
            },
            RawTransactionInput {
                id: 2,
                amount: 150.0,
                timestamp: 1791417600000, // Oct 8
                category_id: 20,
                payment_source: "Cash".into(),
                payment_source_type: "Manual".into(),
            },
            RawTransactionInput {
                id: 3,
                amount: 30.0,
                timestamp: 1791331200000, // Oct 7
                category_id: 10,
                payment_source: "Chase".into(),
                payment_source_type: "Credit Card".into(),
            },
        ];

        let result = aggregate_batch(&txns, 0);

        // Daily sorted descending by date_key: Oct 8 ($200) before Oct 7 ($30)
        assert_eq!(result.daily.len(), 2);
        assert_eq!(result.daily[0].date_key, "2026-10-08");
        assert_eq!(result.daily[0].total_amount, 200.0);
        assert_eq!(result.daily[0].transaction_count, 2);

        assert_eq!(result.daily[1].date_key, "2026-10-07");
        assert_eq!(result.daily[1].total_amount, 30.0);
        assert_eq!(result.daily[1].transaction_count, 1);

        // Categories sorted descending by total_amount: Cat 20 ($150) before Cat 10 ($80)
        assert_eq!(result.categories.len(), 2);
        assert_eq!(result.categories[0].category_id, 20);
        assert_eq!(result.categories[0].total_amount, 150.0);
        assert_eq!(result.categories[1].category_id, 10);
        assert_eq!(result.categories[1].total_amount, 80.0);

        // Payments sorted descending by total_amount: Cash ($150) before Chase ($80)
        assert_eq!(result.payments.len(), 2);
        assert_eq!(result.payments[0].payment_source, "Cash");
        assert_eq!(result.payments[0].total_amount, 150.0);
        assert_eq!(result.payments[1].payment_source, "Chase");
        assert_eq!(result.payments[1].total_amount, 80.0);
    }
}
