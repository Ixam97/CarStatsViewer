package com.ixam97.carStatsViewer.dataManager

sealed class EmulatorIntentExtras {
    companion object {
        const val PROPERTY_ID = "propertyId"
        const val TYPE = "valueType"
        const val VALUE = "value"
        const val TYPE_FLOAT = "Float"
        const val TYPE_INT = "Int"
        const val TYPE_BOOLEAN = "Boolean"
        const val TYPE_STRING = "String"
    }
}