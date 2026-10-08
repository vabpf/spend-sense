use regex::Regex;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MatchResult {
    pub is_match: bool,
    pub amount: Option<f64>,
    pub currency: Option<String>,
    pub merchant: Option<String>,
}

pub fn match_notification(
    text: &str,
    pattern: &str,
) -> MatchResult {
    let re = match Regex::new(pattern) {
        Ok(r) => r,
        Err(_) => {
            return MatchResult {
                is_match: false,
                amount: None,
                currency: None,
                merchant: None,
            }
        }
    };

    if let Some(captures) = re.captures(text) {
        let amount = captures.name("amount").and_then(|m| {
            let clean = m.as_str().replace(',', "").replace('.', "");
            clean.parse::<f64>().ok()
        });

        let currency = captures.name("currency").map(|m| m.as_str().to_string());
        let merchant = captures.name("merchant").map(|m| m.as_str().to_string());

        MatchResult {
            is_match: true,
            amount,
            currency,
            merchant,
        }
    } else {
        MatchResult {
            is_match: false,
            amount: None,
            currency: None,
            merchant: None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_match_notification() {
        let text = "TK 1234 -50,000 VND tai Highlands Coffee";
        let pattern = r"-(?P<amount>[0-9,]+)\s*(?P<currency>VND)\s*tai\s*(?P<merchant>.+)";
        let res = match_notification(text, pattern);
        assert!(res.is_match);
        assert_eq!(res.currency.as_deref(), Some("VND"));
        assert_eq!(res.merchant.as_deref(), Some("Highlands Coffee"));
    }
}
