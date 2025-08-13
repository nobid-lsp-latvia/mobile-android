// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.features.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import lv.lvrtc.uilogic.components.content.ContentScreen
import lv.lvrtc.uilogic.components.content.ContentTitle
import lv.lvrtc.uilogic.components.content.ScreenNavigateAction
import lv.lvrtc.uilogic.components.wrap.WrapPrimaryButton
import lv.lvrtc.uilogic.components.wrap.WrapSecondaryButton
import lv.lvrtc.uilogic.extension.cacheDeepLink
import lv.lvrtc.uilogic.navigation.CommonScreens

@Composable
fun SuccessScreen(
    navController: NavController,
    viewModel: SuccessViewModel
) {
    val context = LocalContext.current

    ContentScreen(
        isLoading = false,
        onBack = { viewModel.setEvent(Event.BackPressed) },
        navigatableAction = ScreenNavigateAction.NONE
    ) { paddingValues ->
        SuccessScreenView(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(CommonScreens.Success.screenRoute) {
                                inclusive = true
                            }
                        }
                    }

                    is Effect.Navigation.PopBackStackUpTo -> {
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = navigationEffect.inclusive
                        )
                    }

                    is Effect.Navigation.DeepLink -> {
                        context.cacheDeepLink(navigationEffect.link)
                        navigationEffect.routeToPop?.let {
                            navController.popBackStack(
                                route = it,
                                inclusive = false
                            )
                        } ?: navController.popBackStack()
                    }

                    is Effect.Navigation.Pop -> navController.popBackStack()
                }
            },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun SuccessScreenView(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ContentTitle(
                title = state.successConfig.headerConfig?.title,
                titleStyle = MaterialTheme.typography.headlineSmall.copy(
                ),
                subtitle = state.successConfig.content,
            )
        }

        val imageConfig = state.successConfig.imageConfig
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                // DEFAULT
                imageConfig.type == SuccessUIConfig.ImageConfig.Type.DEFAULT -> {
                    Image(
                        modifier = Modifier.fillMaxWidth(0.6f),
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = imageConfig.contentDescription,
                        contentScale = ContentScale.FillWidth
                    )
                }
                // Image
                imageConfig.type == SuccessUIConfig.ImageConfig.Type.DRAWABLE && imageConfig.drawableRes != null -> {
                    Image(
                        modifier = Modifier.fillMaxWidth(0.25f),
                        painter = painterResource(id = imageConfig.drawableRes),
                        contentDescription = imageConfig.contentDescription,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.successConfig.buttonConfig.forEach { buttonConfig ->
                Button(
                    onEventSent = onEventSent,
                    config = buttonConfig
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@Composable
private fun Button(
    onEventSent: (Event) -> Unit,
    config: SuccessUIConfig.ButtonConfig
) {
    when (config.style) {
        SuccessUIConfig.ButtonConfig.Style.PRIMARY -> {
            WrapPrimaryButton(
                onClick = { onEventSent(Event.ButtonClicked(config)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonRow(text = config.text)
            }
        }

        SuccessUIConfig.ButtonConfig.Style.OUTLINE -> {
            WrapSecondaryButton(
                onClick = { onEventSent(Event.ButtonClicked(config)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonRow(text = config.text)
            }
        }
    }
}

@Composable
private fun ButtonRow(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}