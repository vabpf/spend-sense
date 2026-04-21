package com.spendsense.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'")
        db.execSQL("ALTER TABLE regex_patterns ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'")
    }
}
