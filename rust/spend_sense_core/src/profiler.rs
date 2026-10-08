use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TimeBucketOutput {
    pub bucket_name: String,
    pub total_amount: f64,
    pub count: i32,
    pub fraction: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TicketTierOutput {
    pub tier_name: String,
    pub threshold_label: String,
    pub total_amount: f64,
    pub count: i32,
    pub average_ticket: f64,
    pub fraction: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TopMerchantOutput {
    pub merchant_name: String,
    pub total_amount: f64,
    pub visit_count: i32,
    pub average_ticket: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct CategoryBehaviorOutput {
    pub total_amount: f64,
    pub transaction_count: i32,
    pub time_buckets: Vec<TimeBucketOutput>,
    pub ticket_tiers: Vec<TicketTierOutput>,
    pub top_merchants: Vec<TopMerchantOutput>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ProfileTransactionInput {
    pub amount: f64,
    pub timestamp: i64,
    pub merchant: String,
    pub notes: Option<String>,
}

pub fn profile_category(
    transactions: &[ProfileTransactionInput],
    currency: &str,
) -> CategoryBehaviorOutput {
    let total_amount: f64 = transactions.iter().map(|t| t.amount).sum();
    let count = transactions.len() as i32;
    let safe_total = if total_amount > 0.0 { total_amount } else { 1.0 };

    if transactions.is_empty() {
        return CategoryBehaviorOutput {
            total_amount: 0.0,
            transaction_count: 0,
            time_buckets: Vec::new(),
            ticket_tiers: Vec::new(),
            top_merchants: Vec::new(),
        };
    }

    // 1. Time-of-Day Context
    let mut morning_amt = 0.0;
    let mut morning_cnt = 0;
    let mut midday_amt = 0.0;
    let mut midday_cnt = 0;
    let mut evening_amt = 0.0;
    let mut evening_cnt = 0;
    let mut other_amt = 0.0;
    let mut other_cnt = 0;

    for txn in transactions {
        // Extract time of day (minute within day: 0..1439)
        let total_seconds = txn.timestamp / 1000;
        let day_seconds = (total_seconds % 86400 + 86400) % 86400;
        let time_minutes = (day_seconds / 60) as i32;

        if (360..=630).contains(&time_minutes) {
            // 06:00 to 10:30
            morning_amt += txn.amount;
            morning_cnt += 1;
        } else if (690..=840).contains(&time_minutes) {
            // 11:30 to 14:00
            midday_amt += txn.amount;
            midday_cnt += 1;
        } else if time_minutes >= 1080 || time_minutes <= 180 {
            // 18:00 to 03:00
            evening_amt += txn.amount;
            evening_cnt += 1;
        } else {
            other_amt += txn.amount;
            other_cnt += 1;
        }
    }

    let mut time_buckets = vec![
        TimeBucketOutput {
            bucket_name: "Evening & Night".into(),
            total_amount: evening_amt,
            count: evening_cnt,
            fraction: (evening_amt / safe_total) as f32,
        },
        TimeBucketOutput {
            bucket_name: "Workday Midday".into(),
            total_amount: midday_amt,
            count: midday_cnt,
            fraction: (midday_amt / safe_total) as f32,
        },
        TimeBucketOutput {
            bucket_name: "Morning Routine".into(),
            total_amount: morning_amt,
            count: morning_cnt,
            fraction: (morning_amt / safe_total) as f32,
        },
        TimeBucketOutput {
            bucket_name: "Daytime / Errands".into(),
            total_amount: other_amt,
            count: other_cnt,
            fraction: (other_amt / safe_total) as f32,
        },
    ];
    time_buckets.retain(|b| b.count > 0);
    time_buckets.sort_by(|a, b| b.total_amount.partial_cmp(&a.total_amount).unwrap_or(std::cmp::Ordering::Equal));

    // 2. Ticket-Size Tiers (Tertile Partitioning)
    let mut sorted_amounts: Vec<f64> = transactions.iter().map(|t| t.amount).collect();
    sorted_amounts.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
    let n = sorted_amounts.len();

    let format_thresh = |v: f64| -> String {
        if currency.trim().eq_ignore_ascii_case("VND") {
            format!("{:.0}₫", v)
        } else {
            format!("{} {:.0}", currency, v)
        }
    };

    let make_tier = |name: &str, thresh: &str, txns: &[&ProfileTransactionInput]| -> TicketTierOutput {
        let amt: f64 = txns.iter().map(|t| t.amount).sum();
        let c = txns.len() as i32;
        let avg = if c > 0 { amt / c as f64 } else { 0.0 };
        TicketTierOutput {
            tier_name: name.into(),
            threshold_label: thresh.into(),
            total_amount: amt,
            count: c,
            average_ticket: avg,
            fraction: (amt / safe_total) as f32,
        }
    };

    let mut ticket_tiers = Vec::new();
    if (1..=2).contains(&n) {
        if n == 1 || (sorted_amounts[0] - sorted_amounts[n - 1]).abs() < 0.001 {
            ticket_tiers.push(make_tier("Standard Purchases", "Standard", &transactions.iter().collect::<Vec<_>>()));
        } else {
            let low = sorted_amounts[0];
            let high = sorted_amounts[n - 1];
            let micro: Vec<&ProfileTransactionInput> = transactions.iter().filter(|t| t.amount == low).collect();
            let major: Vec<&ProfileTransactionInput> = transactions.iter().filter(|t| t.amount == high).collect();
            ticket_tiers.push(make_tier("Major Outings / Splurges", &format!("Top Tier ({})", format_thresh(high)), &major));
            ticket_tiers.push(make_tier("Micro / Quick Bites", &format!("Quick ({})", format_thresh(low)), &micro));
        }
    } else {
        let bottom_idx = (n / 3).max(1);
        let top_idx = ((2 * n) / 3).clamp(bottom_idx, n - 1);
        let q1 = sorted_amounts[bottom_idx - 1];
        let q2 = sorted_amounts[top_idx];

        if q1 < q2 {
            let micro: Vec<&ProfileTransactionInput> = transactions.iter().filter(|t| t.amount <= q1).collect();
            let standard: Vec<&ProfileTransactionInput> = transactions.iter().filter(|t| t.amount > q1 && t.amount < q2).collect();
            let major: Vec<&ProfileTransactionInput> = transactions.iter().filter(|t| t.amount >= q2).collect();

            if !major.is_empty() {
                ticket_tiers.push(make_tier("Major Outings / Splurges", &format!("Top Tier (≥ {})", format_thresh(q2)), &major));
            }
            if !standard.is_empty() {
                ticket_tiers.push(make_tier("Standard Purchases", &format!("{} – {}", format_thresh(q1), format_thresh(q2)), &standard));
            }
            if !micro.is_empty() {
                ticket_tiers.push(make_tier("Micro / Quick Bites", &format!("Micro (≤ {})", format_thresh(q1)), &micro));
            }
        } else {
            ticket_tiers.push(make_tier("Standard Purchases", "Standard", &transactions.iter().collect::<Vec<_>>()));
        }
    }

    // 3. Top Venues & Merchants
    let mut merchant_map: HashMap<String, (f64, i32)> = HashMap::new();
    for txn in transactions {
        let m = if !txn.merchant.trim().is_empty() {
            txn.merchant.trim().to_string()
        } else if let Some(notes) = &txn.notes {
            if !notes.trim().is_empty() {
                notes.trim().to_string()
            } else {
                "Direct / Unnamed".to_string()
            }
        } else {
            "Direct / Unnamed".to_string()
        };

        let entry = merchant_map.entry(m).or_insert((0.0, 0));
        entry.0 += txn.amount;
        entry.1 += 1;
    }

    let mut top_merchants: Vec<TopMerchantOutput> = merchant_map
        .into_iter()
        .map(|(name, (amt, v_count))| TopMerchantOutput {
            merchant_name: name,
            total_amount: amt,
            visit_count: v_count,
            average_ticket: if v_count > 0 { amt / v_count as f64 } else { 0.0 },
        })
        .collect();
    top_merchants.sort_by(|a, b| b.total_amount.partial_cmp(&a.total_amount).unwrap_or(std::cmp::Ordering::Equal));
    top_merchants.truncate(6);

    CategoryBehaviorOutput {
        total_amount,
        transaction_count: count,
        time_buckets,
        ticket_tiers,
        top_merchants,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_profiling_empty() {
        let profile = profile_category(&[], "USD");
        assert_eq!(profile.transaction_count, 0);
        assert_eq!(profile.total_amount, 0.0);
        assert!(profile.time_buckets.is_empty());
        assert!(profile.ticket_tiers.is_empty());
        assert!(profile.top_merchants.is_empty());
    }

    #[test]
    fn test_profiling_ticket_tiers() {
        let txns = vec![
            ProfileTransactionInput { amount: 5.0, timestamp: 1000, merchant: "A".into(), notes: None },
            ProfileTransactionInput { amount: 8.0, timestamp: 2000, merchant: "B".into(), notes: None },
            ProfileTransactionInput { amount: 15.0, timestamp: 3000, merchant: "C".into(), notes: None },
            ProfileTransactionInput { amount: 20.0, timestamp: 4000, merchant: "D".into(), notes: None },
            ProfileTransactionInput { amount: 60.0, timestamp: 5000, merchant: "E".into(), notes: None },
            ProfileTransactionInput { amount: 120.0, timestamp: 6000, merchant: "F".into(), notes: None },
        ];
        let profile = profile_category(&txns, "USD");
        assert_eq!(profile.transaction_count, 6);
        let total_count: i32 = profile.ticket_tiers.iter().map(|t| t.count).sum();
        assert_eq!(total_count, 6);
        let total_sum: f64 = profile.ticket_tiers.iter().map(|t| t.total_amount).sum();
        assert_eq!(total_sum, 228.0);
    }

    #[test]
    fn test_single_and_two_transactions_tiers() {
        // Single transaction
        let single = vec![ProfileTransactionInput { amount: 50.0, timestamp: 1000, merchant: "A".into(), notes: None }];
        let p1 = profile_category(&single, "USD");
        assert_eq!(p1.ticket_tiers.len(), 1);
        assert_eq!(p1.ticket_tiers[0].tier_name, "Standard Purchases");

        // Two distinct transactions
        let double = vec![
            ProfileTransactionInput { amount: 10.0, timestamp: 1000, merchant: "A".into(), notes: None },
            ProfileTransactionInput { amount: 90.0, timestamp: 2000, merchant: "B".into(), notes: None },
        ];
        let p2 = profile_category(&double, "USD");
        assert_eq!(p2.ticket_tiers.len(), 2);
    }

    #[test]
    fn test_top_merchants_notes_and_blank_handling() {
        let txns = vec![
            ProfileTransactionInput { amount: 30.0, timestamp: 1000, merchant: "Starbucks".into(), notes: None },
            ProfileTransactionInput { amount: 20.0, timestamp: 2000, merchant: "".into(), notes: Some("Highlands".into()) },
            ProfileTransactionInput { amount: 10.0, timestamp: 3000, merchant: "".into(), notes: None },
        ];
        let profile = profile_category(&txns, "USD");
        assert_eq!(profile.top_merchants.len(), 3);
        assert_eq!(profile.top_merchants[0].merchant_name, "Starbucks");
        assert_eq!(profile.top_merchants[1].merchant_name, "Highlands");
        assert_eq!(profile.top_merchants[2].merchant_name, "Direct / Unnamed");
    }

    #[test]
    fn test_vnd_formatting() {
        let txns = vec![
            ProfileTransactionInput { amount: 25000.0, timestamp: 1000, merchant: "A".into(), notes: None },
            ProfileTransactionInput { amount: 95000.0, timestamp: 2000, merchant: "B".into(), notes: None },
        ];
        let profile = profile_category(&txns, "VND");
        assert!(profile.ticket_tiers[0].threshold_label.contains('₫'));
    }
}
