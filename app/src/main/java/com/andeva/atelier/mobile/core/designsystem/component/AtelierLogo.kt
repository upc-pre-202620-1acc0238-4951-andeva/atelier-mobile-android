package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AlbertSans

/** Imagotipo: isotipo en squircle azul + «Atelier / Workshop» en Albert Sans ExtraBold. */
@Composable
fun AtelierLogo(modifier: Modifier = Modifier, name: String = "Atelier Workshop") {
    val inkStyle = TextStyle(fontFamily = AlbertSans, fontSize = 16.sp, lineHeight = 16.sp)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = name },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(R.drawable.ic_atelier_logo), contentDescription = null, modifier = Modifier.size(36.dp))
        Column {
            Text("Atelier", style = inkStyle, color = MaterialTheme.colorScheme.onBackground)
            Text("Workshop", style = inkStyle, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
