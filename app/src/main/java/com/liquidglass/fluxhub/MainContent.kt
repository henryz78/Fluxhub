package com.liquidglass.fluxhub

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.liquidglass.fluxhub.destinations.AdaptiveLuminanceGlassContent
import com.liquidglass.fluxhub.destinations.BottomTabsContent
import com.liquidglass.fluxhub.destinations.ButtonsContent
import com.liquidglass.fluxhub.destinations.ControlCenterContent
import com.liquidglass.fluxhub.destinations.DialogContent
import com.liquidglass.fluxhub.destinations.GlassPlaygroundContent
import com.liquidglass.fluxhub.destinations.HomeContent
import com.liquidglass.fluxhub.destinations.LazyScrollContainerContent
import com.liquidglass.fluxhub.destinations.LockScreenContent
import com.liquidglass.fluxhub.destinations.MagnifierContent
import com.liquidglass.fluxhub.destinations.ProgressiveBlurContent
import com.liquidglass.fluxhub.destinations.ScrollContainerContent
import com.liquidglass.fluxhub.destinations.SliderContent
import com.liquidglass.fluxhub.destinations.ToggleContent

@Composable
fun MainContent() {
    var destination by rememberSaveable { mutableStateOf(CatalogDestination.Home) }

    BackHandler(destination != CatalogDestination.Home) {
        destination = CatalogDestination.Home
    }

    when (destination) {
        CatalogDestination.Home -> HomeContent(onNavigate = { destination = it })

        CatalogDestination.Buttons -> ButtonsContent()
        CatalogDestination.Toggle -> ToggleContent()
        CatalogDestination.Slider -> SliderContent()
        CatalogDestination.BottomTabs -> BottomTabsContent()
        CatalogDestination.Dialog -> DialogContent()

        CatalogDestination.LockScreen -> LockScreenContent()
        CatalogDestination.ControlCenter -> ControlCenterContent()
        CatalogDestination.Magnifier -> MagnifierContent()

        CatalogDestination.GlassPlayground -> GlassPlaygroundContent()
        CatalogDestination.AdaptiveLuminanceGlass -> AdaptiveLuminanceGlassContent()
        CatalogDestination.ProgressiveBlur -> ProgressiveBlurContent()
        CatalogDestination.ScrollContainer -> ScrollContainerContent()
        CatalogDestination.LazyScrollContainer -> LazyScrollContainerContent()
    }
}
