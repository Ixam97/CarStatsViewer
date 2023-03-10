package com.ixam97.carStatsViewer.appPreferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.enums.DistanceUnitEnum
import com.ixam97.carStatsViewer.plot.enums.PlotDimension
import com.ixam97.carStatsViewer.utils.Exclude
import java.net.URL

class AppPreferences(
    val context: Context
) {
    private var sharedPref: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
    )

    private val VersionString = AppPreference<String>(context.getString(R.string.preference_version_key),"", sharedPref)

    private val Debug = AppPreference<Boolean>(context.getString(R.string.preference_debug_key), false, sharedPref)
    private val Notification = AppPreference<Boolean>(context.getString(R.string.preference_notifications_key), false, sharedPref)
    private val ConsumptionUnit = AppPreference<Boolean>(context.getString(R.string.preference_consumption_unit_key), false, sharedPref)
    // private //val ExperimentalLayout = AppPreference<Boolean>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    // private //val DeepLog = AppPreference<Boolean>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    private val PlotSpeed = AppPreference<Boolean>(context.getString(R.string.preference_plot_speed_key), false, sharedPref)
    // private //val PlotDistance = AppPreference<Int>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    private val ConsumptionPlotSingleMotor = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_single_motor_key), false, sharedPref)
    private val ConsumptionPlotSecondaryColor = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_secondary_color_key), false, sharedPref)
    private val ConsumptionPlotVisibleGages = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_visible_gages_key), true, sharedPref)
    private val ChagrPlotSecondaryColor = AppPreference<Boolean>(context.getString(R.string.preference_charge_plot_secondary_color_key), false, sharedPref)
    private val ChargePlotVisibleGages = AppPreference<Boolean>(context.getString(R.string.preference_charge_plot_visible_gages_key), true, sharedPref)
    private val ChargePlotDimension = AppPreference<PlotDimension>(context.getString(R.string.preference_charge_plot_dimension_key), PlotDimension.TIME, sharedPref)
    private val DistanceUnit = AppPreference<DistanceUnitEnum>(context.getString(R.string.preference_distance_unit_key), DistanceUnitEnum.KM, sharedPref)
    private val SecondaryConsumptionDimension = AppPreference<Int>(context.getString(R.string.preference_secondary_dimension_key), 0, sharedPref)
    private val MainViewTrip = AppPreference<Int>(context.getString(R.string.preference_main_view_trip_key), 1, sharedPref)
    private val SmtpAddress = AppPreference<String>(context.getString(R.string.preference_smtp_address_key), "", sharedPref)
    private val SmtpPassword = AppPreference<String>(context.getString(R.string.preference_smtp_password_key), "", sharedPref)
    private val SmtpServer = AppPreference<String>(context.getString(R.string.preference_smtp_server_key), "", sharedPref)
    private val LogTargetAddress = AppPreference<String>(context.getString(R.string.preference_log_target_address_key), "ixam97@ixam97.de", sharedPref)
    private val LogUserName = AppPreference<String>(context.getString(R.string.preference_log_user_name_key), "", sharedPref)
    private val AbrpGenericToken = AppPreference<String>(context.getString(R.string.preference_abrp_generic_token_key), "", sharedPref)
    private val HTTPLiveDataURL = AppPreference<String>(context.getString(R.string.preference_http_live_data_url), "", sharedPref)

    var versionString: String get() = VersionString.value; set(value) {VersionString.value = value}

    var debug: Boolean get() = Debug.value; set(value) {Debug.value = value}
    var notifications: Boolean get() = Notification.value; set(value) {Notification.value = value}
    var consumptionUnit: Boolean get() = ConsumptionUnit.value; set(value) {ConsumptionUnit.value = value}
    var plotSpeed: Boolean get() = PlotSpeed.value; set(value) {PlotSpeed.value = value}
    var consumptionPlotSingleMotor: Boolean get() = ConsumptionPlotSingleMotor.value; set(value) {ConsumptionPlotSingleMotor.value = value}
    var consumptionPlotSecondaryColor: Boolean get() = ConsumptionPlotSecondaryColor.value; set(value) {ConsumptionPlotSecondaryColor.value = value}
    var consumptionPlotVisibleGages: Boolean get() = ConsumptionPlotVisibleGages.value; set(value) {ConsumptionPlotVisibleGages.value = value}
    var chargePlotSecondaryColor: Boolean get() = ChagrPlotSecondaryColor.value; set(value) {ChagrPlotSecondaryColor.value = value}
    var chargePlotVisibleGages: Boolean get() = ChargePlotVisibleGages.value; set(value) {ChargePlotVisibleGages.value = value}
    var chargePlotDimension: PlotDimension get() = ChargePlotDimension.value; set(value) {ChargePlotDimension.value = value}
    var distanceUnit: DistanceUnitEnum get() = DistanceUnit.value; set(value) {DistanceUnit.value = value}
    var secondaryConsumptionDimension: Int get() = SecondaryConsumptionDimension.value; set(value) {SecondaryConsumptionDimension.value = value}
    var mainViewTrip: Int get() = MainViewTrip.value; set(value) {MainViewTrip.value = value}
    var smtpAddress: String get() = SmtpAddress.value; set(value) {SmtpAddress.value = value}
    var smtpPassword: String get() = SmtpPassword.value; set(value) {SmtpPassword.value = value}
    var smtpServer: String get() = SmtpServer.value; set(value) {SmtpServer.value = value}
    var logUserName: String get() = LogUserName.value; set(value) {LogUserName.value = value}
    var logTargetAddress: String get() = LogTargetAddress.value; set(value) {LogTargetAddress.value = value}
    var abrpGenericToken: String get() = AbrpGenericToken.value; set(value) {AbrpGenericToken.value = value}
    var httpLiveDataURL : String get() = HTTPLiveDataURL.value; set(value) {HTTPLiveDataURL.value = value}

    // Preferences not saved permanently:
    val exclusionStrategy = AppPreferences.exclusionStrategy
    var doDistractionOptimization: Boolean get() = AppPreferences.doDistractionOptimization; set(value) {AppPreferences.doDistractionOptimization = value}

    companion object {
        val exclusionStrategy: ExclusionStrategy = object : ExclusionStrategy {
            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return false
            }

            override fun shouldSkipField(field: FieldAttributes): Boolean {
                return field.getAnnotation(Exclude::class.java) != null
            }
        }

        var doDistractionOptimization = false
    }
}