use jni::objects::{JClass, JDoubleArray, JString};
use jni::sys::{jboolean, jdouble, jint, jlong, jstring};
use jni::JNIEnv;

use crate::aggregator::{aggregate_batch, RawTransactionInput};
use crate::forecaster::calculate_month_forecast;
use crate::matching::match_notification;
use crate::profiler::{profile_category, ProfileTransactionInput};

#[no_mangle]
pub extern "system" fn Java_com_spendsense_core_SpendSenseCore_nativeIsAvailable(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1 // true
}

#[no_mangle]
pub extern "system" fn Java_com_spendsense_core_SpendSenseCore_nativeForecast(
    mut env: JNIEnv,
    _class: JClass,
    amounts_array: JDoubleArray,
    days_elapsed: jint,
    total_days: jint,
    prior_total: jdouble,
    prior_days: jint,
) -> jstring {
    let len = match env.get_array_length(&amounts_array) {
        Ok(l) => l as usize,
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let mut amounts = vec![0.0f64; len];
    if env.get_double_array_region(&amounts_array, 0, &mut amounts).is_err() {
        return env.new_string("").unwrap().into_raw();
    }

    let forecast = calculate_month_forecast(
        &amounts,
        days_elapsed,
        total_days,
        prior_total,
        prior_days,
    );

    let json = serde_json::to_string(&forecast).unwrap_or_default();
    env.new_string(json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_spendsense_core_SpendSenseCore_nativeAggregateBatch(
    mut env: JNIEnv,
    _class: JClass,
    txns_json: JString,
    tz_offset_ms: jlong,
) -> jstring {
    let json_str: String = match env.get_string(&txns_json) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let transactions: Vec<RawTransactionInput> = match serde_json::from_str(&json_str) {
        Ok(t) => t,
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result = aggregate_batch(&transactions, tz_offset_ms);
    let output_json = serde_json::to_string(&result).unwrap_or_default();
    env.new_string(output_json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_spendsense_core_SpendSenseCore_nativeProfileCategory(
    mut env: JNIEnv,
    _class: JClass,
    txns_json: JString,
    currency_str: JString,
) -> jstring {
    let json_str: String = match env.get_string(&txns_json) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let currency: String = match env.get_string(&currency_str) {
        Ok(s) => s.into(),
        Err(_) => "USD".into(),
    };

    let transactions: Vec<ProfileTransactionInput> = match serde_json::from_str(&json_str) {
        Ok(t) => t,
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result = profile_category(&transactions, &currency);
    let output_json = serde_json::to_string(&result).unwrap_or_default();
    env.new_string(output_json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_spendsense_core_SpendSenseCore_nativeMatchNotification(
    mut env: JNIEnv,
    _class: JClass,
    text_str: JString,
    pattern_str: JString,
) -> jstring {
    let text: String = match env.get_string(&text_str) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let pattern: String = match env.get_string(&pattern_str) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result = match_notification(&text, &pattern);
    let output_json = serde_json::to_string(&result).unwrap_or_default();
    env.new_string(output_json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}
