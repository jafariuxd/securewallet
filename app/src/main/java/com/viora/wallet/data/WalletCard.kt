package com.viora.wallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "wallet_cards")
data class WalletCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val cardType: String, // BANK_CARD, NATIONAL_ID, SHENASNAMEH, PASSPORT, MILITARY_CARD, DRIVERS_LICENSE, STUDENT_ID, POSTAL_ADDRESS, OTHER
    val ownerName: String = "",
    val cardNumber: String? = null,
    val secondNumber: String? = null, // cvv2, national code, or other sub-number
    val expiryDate: String? = null, // MM/YY or YYYY/MM/DD
    val shebaNumber: String? = null,
    val accountNumber: String? = null,
    val frontImageBase64: String? = null,
    val backImageBase64: String? = null,
    val additionalImagesJson: String? = null,
    val extraFieldsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getExtraFieldsMap(): Map<String, String> {
        if (extraFieldsJson.isNullOrBlank()) return emptyMap()
        return try {
            val jsonObject = JSONObject(extraFieldsJson)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.optString(key, "")
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getAllImages(): List<String> {
        if (!additionalImagesJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<String>()
                val jsonArray = JSONArray(additionalImagesJson)
                for (i in 0 until jsonArray.length()) {
                    val str = jsonArray.optString(i)
                    if (str.isNotEmpty()) list.add(str)
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val list = mutableListOf<String>()
        if (!frontImageBase64.isNullOrEmpty()) list.add(frontImageBase64)
        if (!backImageBase64.isNullOrEmpty()) list.add(backImageBase64)
        return list
    }

    companion object {
        fun encodeMapToJson(map: Map<String, String>): String {
            val jsonObject = JSONObject()
            for ((key, value) in map) {
                if (value.isNotBlank()) {
                    jsonObject.put(key, value)
                }
            }
            return jsonObject.toString()
        }

        fun encodeListToJson(list: List<String>): String {
            val jsonArray = JSONArray()
            for (item in list) {
                jsonArray.put(item)
            }
            return jsonArray.toString()
        }
    }
}

