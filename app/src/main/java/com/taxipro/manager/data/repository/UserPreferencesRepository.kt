package com.taxipro.manager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private val CURRENCY_KEY = stringPreferencesKey("currency_symbol")
    private val INITIAL_HISTORICAL_KM_KEY = doublePreferencesKey("initial_historical_km")
    private val INITIAL_HISTORICAL_EXPENSES_KEY = doublePreferencesKey("initial_historical_expenses")

    val currencySymbol: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENCY_KEY] ?: "€"
        }

    val initialHistoricalKm: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_HISTORICAL_KM_KEY] ?: 0.0
        }

    val initialHistoricalExpenses: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_HISTORICAL_EXPENSES_KEY] ?: 0.0
        }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = symbol
        }
    }

    suspend fun setInitialHistoricalKm(km: Double) {
        context.dataStore.edit { preferences ->
            preferences[INITIAL_HISTORICAL_KM_KEY] = km
        }
    }

    suspend fun setInitialHistoricalExpenses(expenses: Double) {
        context.dataStore.edit { preferences ->
            preferences[INITIAL_HISTORICAL_EXPENSES_KEY] = expenses
        }
    }
}
