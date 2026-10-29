package com.andeva.atelier.mobile.core.presentation

import com.andeva.atelier.mobile.core.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {

    @Test
    fun `formatea soles con dos decimales y separador de miles`() {
        assertEquals("S/ 1,250.50", Money(BigDecimal("1250.5"), Money.PEN).format())
    }

    @Test
    fun `formatea dolares con su simbolo`() {
        assertEquals("US$ 40.00", Money(BigDecimal("40"), Money.USD).format())
    }

    @Test
    fun `la cantidad no muestra ceros sobrantes`() {
        assertEquals("2", BigDecimal("2.000").formatQuantity())
        assertEquals("1.5", BigDecimal("1.50").formatQuantity())
        assertEquals("100", BigDecimal("100").formatQuantity())
    }
}
