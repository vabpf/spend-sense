use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct MonthForecastOutput {
    pub spend_to_date: f64,
    pub projected_remaining_spend: f64,
    pub projected_month_end_total: f64,
    pub variable_daily_burn_rate: f64,
    pub days_elapsed: i32,
    pub days_remaining: i32,
    pub isolated_spikes_count: i32,
    pub isolated_spikes_total: f64,
    pub safe_remaining_daily_pace: Option<f64>,
    pub percent_vs_last_month: Option<i32>,
    pub is_stabilizing_with_prior: bool,
}

pub fn calculate_month_forecast(
    amounts: &[f64],
    days_elapsed: i32,
    total_days_in_month: i32,
    prior_month_total: f64,
    prior_month_total_days: i32,
) -> MonthForecastOutput {
    let spend_to_date: f64 = amounts.iter().sum();
    let days_remaining = (total_days_in_month - days_elapsed).max(0);

    if days_remaining <= 0 {
        let percent = if prior_month_total > 0.0 {
            Some((((spend_to_date - prior_month_total) / prior_month_total) * 100.0).round() as i32)
        } else {
            None
        };
        return MonthForecastOutput {
            spend_to_date,
            projected_remaining_spend: 0.0,
            projected_month_end_total: spend_to_date,
            variable_daily_burn_rate: if days_elapsed > 0 { spend_to_date / days_elapsed as f64 } else { 0.0 },
            days_elapsed,
            days_remaining: 0,
            isolated_spikes_count: 0,
            isolated_spikes_total: 0.0,
            safe_remaining_daily_pace: None,
            percent_vs_last_month: percent,
            is_stabilizing_with_prior: false,
        };
    }

    if amounts.is_empty() {
        let prior_daily_baseline = if prior_month_total_days > 0 {
            prior_month_total / prior_month_total_days as f64
        } else {
            0.0
        };
        let projected_remaining = prior_daily_baseline * days_remaining as f64;
        let percent = if prior_month_total > 0.0 {
            Some((((projected_remaining - prior_month_total) / prior_month_total) * 100.0).round() as i32)
        } else {
            None
        };
        return MonthForecastOutput {
            spend_to_date: 0.0,
            projected_remaining_spend: projected_remaining,
            projected_month_end_total: projected_remaining,
            variable_daily_burn_rate: prior_daily_baseline,
            days_elapsed,
            days_remaining,
            isolated_spikes_count: 0,
            isolated_spikes_total: 0.0,
            safe_remaining_daily_pace: if prior_month_total > 0.0 {
                Some(prior_month_total / total_days_in_month.max(1) as f64)
            } else {
                None
            },
            percent_vs_last_month: percent,
            is_stabilizing_with_prior: true,
        };
    }

    // 1. Tukey Outlier Fence Detection
    let mut sorted_amounts = amounts.to_vec();
    sorted_amounts.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
    let n = sorted_amounts.len();

    let median = if n % 2 == 1 {
        sorted_amounts[n / 2]
    } else {
        (sorted_amounts[n / 2 - 1] + sorted_amounts[n / 2]) / 2.0
    };

    let q1 = sorted_amounts[((n as f64 * 0.25) as usize).min(n - 1)];
    let q3 = sorted_amounts[((n as f64 * 0.75) as usize).min(n - 1)];
    let iqr = (q3 - q1).max(0.0);

    let outlier_cutoff = (q3 + 1.5 * iqr).max(median * 3.0);

    let mut regular_sum = 0.0;
    let mut isolated_spikes_sum = 0.0;
    let mut isolated_spikes_count = 0;

    for &amt in amounts {
        if n >= 4 && amt > outlier_cutoff && amt > 100.0 {
            isolated_spikes_sum += amt;
            isolated_spikes_count += 1;
        } else {
            regular_sum += amt;
        }
    }

    // 2. Variable Daily Burn Rate
    let current_daily_rate = if days_elapsed > 0 {
        regular_sum / days_elapsed as f64
    } else {
        0.0
    };

    // 3. Bayesian Shrinkage Stabilization (t / (t + 4))
    let prior_daily_baseline = if prior_month_total_days > 0 {
        prior_month_total / prior_month_total_days as f64
    } else {
        0.0
    };

    let (projected_daily_pace, is_stabilizing) = if prior_month_total > 0.0 {
        let t = days_elapsed as f64;
        let w_t = t / (t + 4.0);
        let blended = w_t * current_daily_rate + (1.0 - w_t) * prior_daily_baseline;
        (blended, w_t < 0.75)
    } else {
        (current_daily_rate, false)
    };

    // 4. Projection
    let projected_remaining_spend = projected_daily_pace * days_remaining as f64;
    let projected_month_end_total = spend_to_date + projected_remaining_spend;

    let safe_remaining_daily_pace = if prior_month_total > 0.0 && prior_month_total > spend_to_date {
        Some((prior_month_total - spend_to_date) / days_remaining.max(1) as f64)
    } else {
        None
    };

    let percent_vs_last_month = if prior_month_total > 0.0 {
        Some((((projected_month_end_total - prior_month_total) / prior_month_total) * 100.0).round() as i32)
    } else {
        None
    };

    MonthForecastOutput {
        spend_to_date,
        projected_remaining_spend,
        projected_month_end_total,
        variable_daily_burn_rate: projected_daily_pace,
        days_elapsed,
        days_remaining,
        isolated_spikes_count,
        isolated_spikes_total: isolated_spikes_sum,
        safe_remaining_daily_pace,
        percent_vs_last_month,
        is_stabilizing_with_prior: is_stabilizing,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bayesian_shrinkage_day1() {
        let amounts = vec![100.0];
        let forecast = calculate_month_forecast(&amounts, 1, 30, 1500.0, 30);
        assert_eq!(forecast.days_elapsed, 1);
        assert_eq!(forecast.days_remaining, 29);
        assert!(forecast.is_stabilizing_with_prior);
        // Prior daily = $50/day. Day 1 = $100. Blended = (1/5)*100 + (4/5)*50 = 20 + 40 = 60.
        assert!((forecast.variable_daily_burn_rate - 60.0).abs() < 0.01);
    }

    #[test]
    fn test_outlier_isolation() {
        // 10 days of $40 + one $400 spike
        let mut amounts = vec![40.0; 10];
        amounts.push(400.0);
        let forecast = calculate_month_forecast(&amounts, 10, 30, 1200.0, 30);
        assert_eq!(forecast.isolated_spikes_count, 1);
        assert_eq!(forecast.isolated_spikes_total, 400.0);
        assert!((forecast.variable_daily_burn_rate - 40.0).abs() < 0.01);
    }

    #[test]
    fn test_empty_transactions_with_prior() {
        let forecast = calculate_month_forecast(&[], 0, 30, 1200.0, 30);
        assert_eq!(forecast.spend_to_date, 0.0);
        assert_eq!(forecast.days_remaining, 30);
        assert_eq!(forecast.variable_daily_burn_rate, 40.0); // 1200 / 30
        assert_eq!(forecast.projected_remaining_spend, 1200.0);
        assert_eq!(forecast.projected_month_end_total, 1200.0);
        assert!(forecast.is_stabilizing_with_prior);
    }

    #[test]
    fn test_end_of_month_convergence() {
        let amounts = vec![150.0, 250.0];
        let forecast = calculate_month_forecast(&amounts, 30, 30, 500.0, 30);
        assert_eq!(forecast.days_remaining, 0);
        assert_eq!(forecast.projected_remaining_spend, 0.0);
        assert_eq!(forecast.projected_month_end_total, 400.0);
        assert_eq!(forecast.spend_to_date, 400.0);
        assert!(!forecast.is_stabilizing_with_prior);
    }

    #[test]
    fn test_safe_remaining_daily_pace() {
        let amounts = vec![300.0];
        // Day 10 of 30. Remaining: 20 days. Prior total = 900.
        // Remaining budget = 900 - 300 = 600.
        // Safe pace = 600 / 20 = 30.
        let forecast = calculate_month_forecast(&amounts, 10, 30, 900.0, 30);
        assert_eq!(forecast.safe_remaining_daily_pace, Some(30.0));
    }
}
