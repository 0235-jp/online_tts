package com.example.onlinetts.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.onlinetts.tts.provider.TtsProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "online_tts_encrypted_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getApiKey(providerType: TtsProviderType): String {
        return prefs.getString(apiKeyKey(providerType), "") ?: ""
    }

    fun setApiKey(providerType: TtsProviderType, apiKey: String) {
        prefs.edit().putString(apiKeyKey(providerType), apiKey).apply()
    }

    private fun apiKeyKey(providerType: TtsProviderType): String {
        return "api_key_${providerType.name}"
    }
}
