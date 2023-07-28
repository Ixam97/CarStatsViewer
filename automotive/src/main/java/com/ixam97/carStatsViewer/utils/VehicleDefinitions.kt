package com.ixam97.carStatsViewer.utils

object VehicleDefinitions {
    object Polestar2 {
        private const val name = "Polestar 2"
        private const val manufacturer = "Polestar"

        private const val nameCode = "2"
        private const val manufacturerCode = "polestar"

        val modelYears = arrayListOf<String>(
            "2021",
            "2022",
            "2023",
            "2024"
        )

        val modelYearsCode = arrayListOf<String>(
            "20",
            "22",
            "23",
            "24"
        )

        val driveTrains = arrayListOf<String>(
            "Standard Range Single Motor",
            "Long Range Single Motor",
            "Long Range Dual Motor"
        )

        val driveTrainsCode = mapOf<String, ArrayList<String>>(
            "24" to arrayListOf<String>(
                "69:single:sr",
                "82:single:lr",
                "82:dual:lr"),
            "23" to arrayListOf<String>(
                "67:single:sr",
                "78:single:lr",
                "78:dual:lr"),
            "22" to arrayListOf<String>(
                "61:single:sr",
                "75:single",
                "75"),
            "20" to arrayListOf(
                "75","75","75")
        )

        fun getVehicleString(modelYearIndex: Int, driveTrainIndex: Int, plus: Boolean, performance: Boolean, bst: Boolean): String {
            if (modelYearIndex == 0) return "Polestar 2 Launch Edition"
            if (bst) return "Polestar 2 BST Edition"
            var vehicleString = "$name ${modelYears[modelYearIndex]} ${driveTrains[driveTrainIndex]}"
            if (plus) vehicleString += " Plus"
            if (performance) vehicleString += " Perf."
            return vehicleString
        }

        fun getVehicleCode(modelYearIndex: Int, driveTrainIndex: Int, plus: Boolean, performance: Boolean, bst: Boolean): String {
            if (bst) return "polestar:2:23:78:bst"
            val modelYearCode = modelYearsCode[modelYearIndex]
            var vehicleCode = "$manufacturerCode:$nameCode:$modelYearCode:${driveTrainsCode[modelYearCode]?.get(driveTrainIndex)}"
            if (plus) vehicleCode += ":heatpump"
            return vehicleCode

        }

    }
}