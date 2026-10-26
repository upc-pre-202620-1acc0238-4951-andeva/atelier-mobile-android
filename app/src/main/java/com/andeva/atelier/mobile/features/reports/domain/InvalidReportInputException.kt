package com.andeva.atelier.mobile.features.reports.domain

/** Un dato del reporte no es válido; [field] dice cuál (`period`…). */
class InvalidReportInputException(val field: String) : Exception("Invalid report input: $field")
