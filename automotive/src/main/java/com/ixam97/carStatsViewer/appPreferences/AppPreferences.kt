package com.ixam97.carStatsViewer.appPreferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.Exclude

class AppPreferences(
    val context: Context
) {
    var sharedPref: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
    )

    private val VersionString = AppPreference<String>(context.getString(R.string.preference_version_key),"", sharedPref)

    private val Debug = AppPreference<Boolean>(context.getString(R.string.preference_debug_key), false, sharedPref)
    private val DebugDelays = AppPreference<Boolean>("preference_debug_delays", false, sharedPref)
    private val DebugColors = AppPreference<Boolean>("preference_debug_colors", false, sharedPref)
    private val DebugScreenshotReceiver = AppPreference<String>("preference_debug_screenshot_receiver", "", sharedPref)
    private val DebugUserID = AppPreference<String>("preference_debug_user_id", "Anonymous", sharedPref)
    private val Notification = AppPreference<Boolean>(context.getString(R.string.preference_notifications_key), false, sharedPref)
    private val ConsumptionUnit = AppPreference<Boolean>(context.getString(R.string.preference_consumption_unit_key), false, sharedPref)
    // private //val ExperimentalLayout = AppPreference<Boolean>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    // private //val DeepLog = AppPreference<Boolean>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    private val PlotSpeed = AppPreference<Boolean>(context.getString(R.string.preference_plot_speed_key), false, sharedPref)
    // private //val PlotDistance = AppPreference<Int>(context.getString(R.string.preferences_notifications_key), false, sharedPref)
    private val ConsumptionPlotSingleMotor = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_single_motor_key), false, sharedPref)
    private val ConsumptionPlotSecondaryColor = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_secondary_color_key), false, sharedPref)
    private val ConsumptionPlotVisibleGages = AppPreference<Boolean>(context.getString(R.string.preference_consumption_plot_visible_gages_key), true, sharedPref)
    private val ChargePlotSecondaryColor = AppPreference<Boolean>(context.getString(R.string.preference_charge_plot_secondary_color_key), false, sharedPref)
    private val ChargePlotVisibleGages = AppPreference<Boolean>(context.getString(R.string.preference_charge_plot_visible_gages_key), true, sharedPref)
    private val ChargePlotDimension = AppPreference<PlotDimensionX>(context.getString(R.string.preference_charge_plot_dimension_key), PlotDimensionX.TIME, sharedPref)
    private val DistanceUnit = AppPreference<DistanceUnitEnum>(context.getString(R.string.preference_distance_unit_key), DistanceUnitEnum.KM, sharedPref)
    private val SecondaryConsumptionDimension = AppPreference<Int>(context.getString(R.string.preference_secondary_dimension_key), 0, sharedPref)
    private val MainPrimaryDimensionRestriction = AppPreference<Int>("preference_dimension_restriction", 0, sharedPref)
    private val MainViewTrip = AppPreference<Int>(context.getString(R.string.preference_main_view_trip_key), 1, sharedPref)
    private val SmtpAddress = AppPreference<String>(context.getString(R.string.preference_smtp_address_key), "", sharedPref)
    private val SmtpPassword = AppPreference<String>(context.getString(R.string.preference_smtp_password_key), "", sharedPref)
    private val SmtpServer = AppPreference<String>(context.getString(R.string.preference_smtp_server_key), "", sharedPref)
    private val LogTargetAddress = AppPreference<String>(context.getString(R.string.preference_log_target_address_key), "ixam97@ixam97.de", sharedPref)
    private val LogUserName = AppPreference<String>(context.getString(R.string.preference_log_user_name_key), "", sharedPref)
    private val UseLocation = AppPreference<Boolean>(context.getString(R.string.preference_use_location_key), false, sharedPref)
    private val Autostart = AppPreference<Boolean>(context.getString(R.string.preference_autostart_key), false, sharedPref)
    private val ModelYear = AppPreference<Int>(context.getString(R.string.preference_model_year_key), 0, sharedPref)
    private val DriveTrain = AppPreference<Int>(context.getString(R.string.preference_drive_train_key), 0, sharedPref)
    private val PlusPack = AppPreference<Boolean>(context.getString(R.string.preference_plus_key), false, sharedPref)
    private val PerformanceUpgrade = AppPreference<Boolean>(context.getString(R.string.preference_performance_key), false, sharedPref)
    private val BstEdition = AppPreference<Boolean>(context.getString(R.string.preference_bst_key), false, sharedPref)

    private val AltLayout = AppPreference<Boolean>("preference_alt_layout", false, sharedPref)
    private val ShowScreenshotButton = AppPreference<Boolean>("preference_show_screenshot_button", false, sharedPref)
    private val TripFilterManual = AppPreference<Boolean>(context.getString(R.string.preference_trip_filter_manual_key), true, sharedPref)
    private val TripFilterCharge = AppPreference<Boolean>(context.getString(R.string.preference_trip_filter_charge_key), true, sharedPref)
    private val TripFilterAuto = AppPreference<Boolean>(context.getString(R.string.preference_trip_filter_auto_key), true, sharedPref)
    private val TripFilterMonth = AppPreference<Boolean>(context.getString(R.string.preference_trip_filter_month_key), true, sharedPref)
    private val TripFilterTime = AppPreference<Long>(context.getString(R.string.preference_trip_filter_time_key), 0L, sharedPref)

    private val MainViewConnectionApi = AppPreference<Int>(context.getString(R.string.preference_main_view_connection_api_key), 0, sharedPref)
    private val HttpApiTelemetryType = AppPreference<Int>("preference_telemetry_type", 2, sharedPref)

    private val PhoneNotification = AppPreference<Boolean>("preference_phone_notification", false, sharedPref)
    private val ColorTheme = AppPreference<Int>("preference_color_theme", 0, sharedPref)

    private val CarAppSelectedRealTimeData = AppPreference<Int>("preference_car_app_selected_real_time_data", 1, sharedPref)
    private val CarAppRealTimeData = AppPreference<Boolean>("preference_car_app_real_time_data", false, sharedPref)

    var versionString: String get() = VersionString.value; set(value) {VersionString.value = value}

    var debug: Boolean get() = Debug.value; set(value) {Debug.value = value}
    var debugDelays: Boolean get() = DebugDelays.value; set(value) {DebugDelays.value = value}
    var debugColors: Boolean get() = DebugColors.value; set(value) {DebugColors.value = value}
    var debugScreenshotReceiver: String get() = DebugScreenshotReceiver.value; set(value) {DebugScreenshotReceiver.value = value}
    var debugUserID: String get() = DebugUserID.value; set(value) {DebugUserID.value = value}
    var notifications: Boolean get() = Notification.value; set(value) {Notification.value = value}
    var consumptionUnit: Boolean get() = ConsumptionUnit.value; set(value) {ConsumptionUnit.value = value}
    var plotSpeed: Boolean get() = PlotSpeed.value; set(value) {PlotSpeed.value = value}
    var consumptionPlotSingleMotor: Boolean get() = ConsumptionPlotSingleMotor.value; set(value) {ConsumptionPlotSingleMotor.value = value}
    var consumptionPlotSecondaryColor: Boolean get() = ConsumptionPlotSecondaryColor.value; set(value) {ConsumptionPlotSecondaryColor.value = value}
    var consumptionPlotVisibleGages: Boolean get() = ConsumptionPlotVisibleGages.value; set(value) {ConsumptionPlotVisibleGages.value = value}
    var chargePlotSecondaryColor: Boolean get() = ChargePlotSecondaryColor.value; set(value) {ChargePlotSecondaryColor.value = value}
    var chargePlotVisibleGages: Boolean get() = ChargePlotVisibleGages.value; set(value) {ChargePlotVisibleGages.value = value}
    var chargePlotDimension: PlotDimensionX get() = ChargePlotDimension.value; set(value) {ChargePlotDimension.value = value}
    var distanceUnit: DistanceUnitEnum get() = DistanceUnit.value; set(value) {DistanceUnit.value = value}
    var mainPrimaryDimensionRestriction : Int get() = MainPrimaryDimensionRestriction.value; set(value) {MainPrimaryDimensionRestriction.value = value}
    var secondaryConsumptionDimension: Int get() = SecondaryConsumptionDimension.value; set(value) {SecondaryConsumptionDimension.value = value}
    var mainViewTrip: Int get() = MainViewTrip.value; set(value) {MainViewTrip.value = value}
    var smtpAddress: String get() = SmtpAddress.value; set(value) {SmtpAddress.value = value}
    var smtpPassword: String get() = SmtpPassword.value; set(value) {SmtpPassword.value = value}
    var smtpServer: String get() = SmtpServer.value; set(value) {SmtpServer.value = value}
    var logUserName: String get() = LogUserName.value; set(value) {LogUserName.value = value}
    var logTargetAddress: String get() = LogTargetAddress.value; set(value) {LogTargetAddress.value = value}
    var useLocation: Boolean get() = UseLocation.value; set(value) {UseLocation.value = value}
    var autostart: Boolean get() = Autostart.value; set(value) {Autostart.value = value}
    var modelYear: Int get() = ModelYear.value; set(value) {ModelYear.value = value}
    var driveTrain: Int get() = DriveTrain.value; set(value) {DriveTrain.value = value}
    var plusPack: Boolean get() = PlusPack.value; set(value) {PlusPack.value = value}
    var performanceUpgrade: Boolean get() = PerformanceUpgrade.value; set(value) {PerformanceUpgrade.value = value}
    var bstEdition: Boolean get() = BstEdition.value; set(value) {BstEdition.value = value}
    var showScreenshotButton: Boolean get() = ShowScreenshotButton.value; set(value) {ShowScreenshotButton.value = value}
    var altLayout: Boolean get() = AltLayout.value; set(value) {AltLayout.value = value}

    var tripFilterManual: Boolean get() = TripFilterManual.value; set(value) {TripFilterManual.value = value}
    var tripFilterCharge: Boolean get() = TripFilterCharge.value; set(value) {TripFilterCharge.value = value}
    var tripFilterAuto: Boolean get() = TripFilterAuto.value; set(value) {TripFilterAuto.value = value}
    var tripFilterMonth: Boolean get() = TripFilterMonth.value; set(value) {TripFilterMonth.value = value}
    var tripFilterTime: Long get() = TripFilterTime.value; set(value) {TripFilterTime.value = value}

    var mainViewConnectionApi: Int get() = MainViewConnectionApi.value; set(value) {MainViewConnectionApi.value = value}
    var httpApiTelemetryType: Int get() = HttpApiTelemetryType.value; set(value) {HttpApiTelemetryType.value = value}

    var phoneNotification: Boolean get() = PhoneNotification.value; set(value) {PhoneNotification.value = value}
    var colorTheme: Int get() = ColorTheme.value; set(value) {ColorTheme.value = value}

    // var carAppSelectedRealTimeData: Int get() = CarAppSelectedRealTimeData.value; set(value) {CarAppSelectedRealTimeData.value = value}
    var carAppRealTimeData: Boolean get() = CarAppRealTimeData.value; set(value) {CarAppRealTimeData.value = value}

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