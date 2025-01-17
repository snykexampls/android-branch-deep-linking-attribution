package io.branch.referral

import android.net.Uri
import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReferringUrlUtility (prefHelper: PrefHelper) {
    private val urlQueryParameters: MutableMap<String, BranchUrlQueryParameter>
    private var prefHelper: PrefHelper

    init {
        this.prefHelper = prefHelper
        urlQueryParameters = deserializeFromJson(prefHelper.referringURLQueryParameters)
        checkForAndMigrateOldGclid()
    }

    fun parseReferringURL(urlString: String) {
        val uri = Uri.parse(urlString)

        for (originalParamName in uri.queryParameterNames) {
            val paramName = originalParamName.lowercase()
            val paramValue = uri.getQueryParameter(originalParamName)
            PrefHelper.Debug("Found URL Query Parameter - Key: $paramName, Value: $paramValue")

            if (isSupportedQueryParameter(paramName)) {
                val param = findUrlQueryParam(paramName)
                param.value = paramValue
                param.timestamp = Date()
                param.isDeepLink = true

                // If there is no validity window, set to default.
                if (param.validityWindow == 0L) {
                    param.validityWindow = defaultValidityWindowForParam(paramName)
                }

                urlQueryParameters[paramName] = param
            }
        }

        prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
        PrefHelper.Debug(prefHelper.referringURLQueryParameters.toString())
    }

    fun getURLQueryParamsForRequest(request: ServerRequest): JSONObject {
        val returnedParams = mutableMapOf<String, Any>()

        val gclid = addGclidValueFor(request)
        if (gclid.length() > 0) {
            val keys = gclid.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                returnedParams[key] = gclid.get(key)
            }
        }

        return JSONObject(returnedParams as Map<*, *>)
    }

    private fun addGclidValueFor(request: ServerRequest): JSONObject {
        val returnParams = JSONObject()

        if (request is ServerRequestLogEvent || request is ServerRequestRegisterOpen) {
            val gclid = urlQueryParameters[Defines.Jsonkey.Gclid.key]
            if (gclid != null) {
                if (gclid.value != null && gclid.value != PrefHelper.NO_STRING_VALUE) {
                    returnParams.put(Defines.Jsonkey.Gclid.key, gclid.value)

                    //Only v1/open requires is_deeplink_gclid
                    if (request is ServerRequestRegisterOpen) {
                        returnParams.put(Defines.Jsonkey.IsDeeplinkGclid.key, gclid.isDeepLink)
                    }

                    gclid.isDeepLink = false
                    prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
                }
            }
        }

        return returnParams
    }

    private fun isSupportedQueryParameter(paramName: String): Boolean {
        val lowercase = paramName.lowercase()
        val validURLQueryParameters = listOf(Defines.Jsonkey.Gclid.key)
        return lowercase in validURLQueryParameters
    }

    private fun findUrlQueryParam(paramName: String): BranchUrlQueryParameter {
        return urlQueryParameters[paramName] ?: BranchUrlQueryParameter(name = paramName)
    }

    private fun defaultValidityWindowForParam(paramName: String): Long {
        return if (paramName == Defines.Jsonkey.Gclid.key) {
            30 * 24 * 60 * 60 // 30 days = 2,592,000 seconds
        }
        else {
            0L // Default, means indefinite.
        }
    }

    private fun serializeToJson(urlQueryParameters: MutableMap<String, BranchUrlQueryParameter>): JSONObject {
        val json = JSONObject()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        for (param in urlQueryParameters.values) {
            val paramDict = JSONObject()
            paramDict.put("name", param.name)
            paramDict.put("value", param.value ?: JSONObject.NULL)
            paramDict.put("timestamp", param.timestamp?.let { dateFormat.format(it) })
            paramDict.put("isDeeplink", param.isDeepLink)
            paramDict.put("validityWindow", param.validityWindow)

            json.put(param.name.toString(), paramDict)
        }

        return json
    }

    private fun deserializeFromJson(json: JSONObject): MutableMap<String, BranchUrlQueryParameter> {
        val result = mutableMapOf<String, BranchUrlQueryParameter>()

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val temp = json.getJSONObject(key)

            val param = BranchUrlQueryParameter()
            param.name = temp.getString("name")

            if (!temp.isNull("value")) {
                param.value = temp.getString("value")
            }

            param.timestamp = temp.get("timestamp") as Date?
            param.validityWindow = temp.getLong("validityWindow")

            if (!temp.isNull("isDeeplink")) {
                param.isDeepLink = temp.getBoolean("isDeeplink")
            } else {
                param.isDeepLink = false
            }

            param.name?.let { paramName ->
                result[paramName] = param
            }
        }

        return result
    }

    /**
     * To support updates from older versions, this function checks for the presence of an old Gclid value in
     * PrefHelper, and if one exists, it migrates the Gclid value to the new BranchUrlQueryParameter format.
     *
     * It is run upon initialization of the ReferringUrlUtility class.
     * 1. First it checks for an saved Gclid in the new BranchUrlQueryParameter format.
     * 2. If it doesn't exist, then it will check for an old format Gclid saved in PrefHelper
     * 3. If that Gclid does exist, it will be turned into a BranchUrlQueryParameter and saved.
     * 4. Lastly, the old gclid is cleared and now the function
     */
    @VisibleForTesting
    private fun checkForAndMigrateOldGclid() {
        val newGclid = urlQueryParameters[Defines.Jsonkey.Gclid.key]
        if (newGclid?.value == null) {
            val existingGclidValue = prefHelper.referrerGclid
            if (existingGclidValue != null && existingGclidValue != PrefHelper.NO_STRING_VALUE) {
                val existingGclidValidityWindow = prefHelper.referrerGclidValidForWindow

                val gclid = BranchUrlQueryParameter(
                    name = Defines.Jsonkey.Gclid.key,
                    value = existingGclidValue,
                    timestamp = Date(),
                    validityWindow = existingGclidValidityWindow,
                    isDeepLink = false
                )

                urlQueryParameters[Defines.Jsonkey.Gclid.key] = gclid

                prefHelper.setReferringUrlQueryParameters(serializeToJson(urlQueryParameters))
                prefHelper.clearGclid()

                PrefHelper.Debug("Updated old Gclid ($existingGclidValue) to new BranchUrlQueryParameter ($gclid)")
            }
        }
    }
}

    data class BranchUrlQueryParameter(
    var name: String? = null,
    var value: String? = null,
    var timestamp: Date? = null,
    var isDeepLink: Boolean = false,
    var validityWindow: Long = 0
)
