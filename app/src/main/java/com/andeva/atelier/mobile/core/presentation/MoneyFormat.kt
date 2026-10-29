package com.andeva.atelier.mobile.core.presentation

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal
import java.util.Locale

private val PeruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()

/** Importe con dos decimales y su símbolo: `S/ 1,250.00` o `US$ 40.00`. Es el importe neto, sin IGV. */
fun Money.format(): String = "${currencySymbol(currency)} ${amount.formatAmount()}"

private fun currencySymbol(currency: String) = if (currency == Money.USD) "US$" else "S/"

private fun BigDecimal.formatAmount(): String = String.format(PeruLocale, "%,.2f", this)

/** Cantidad sin ceros sobrantes: `2`, `1.5`. */
fun BigDecimal.formatQuantity(): String = stripTrailingZeros().toPlainString()
