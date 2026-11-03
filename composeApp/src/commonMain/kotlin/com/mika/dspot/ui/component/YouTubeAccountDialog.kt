package com.mika.dspot.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.data.entities.GoogleAccountEntity
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.PeopleAlt
import com.mika.dspot.ui.icon.PlaylistAdd
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.login.LoginDestination
import com.mika.dspot.ui.theme.typo
import com.mika.domain.utils.LocalResource
import com.mika.dspot.viewModel.SettingBasicAlertState
import com.mika.dspot.viewModel.SettingsViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.add_an_account
import dspot.composeapp.generated.resources.cancel
import dspot.composeapp.generated.resources.guest
import dspot.composeapp.generated.resources.log_out
import dspot.composeapp.generated.resources.log_out_warning
import dspot.composeapp.generated.resources.no_account
import dspot.composeapp.generated.resources.signed_in
import dspot.composeapp.generated.resources.warning
import dspot.composeapp.generated.resources.youtube_account
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAccountDialog(
    viewModel: SettingsViewModel,
    navController: NavController,
    onDismissRequest: () -> Unit,
) {
    val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle()

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.wrapContentSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            shadowElevation = 1.dp,
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            colors =
                                IconButtonDefaults.iconButtonColors().copy(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxHeight(),
                        ) {
                            Icon(SimpIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            stringResource(Res.string.youtube_account),
                            style = typo().titleMedium,
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .wrapContentWidth(),
                        )
                    }
                }
                if (googleAccounts is LocalResource.Success) {
                    val data = (googleAccounts as LocalResource.Success<List<GoogleAccountEntity>?>).data
                    if (data.isNullOrEmpty()) {
                        item {
                            Text(
                                stringResource(Res.string.no_account),
                                style = typo().bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                            )
                        }
                    } else {
                        items(data) { account ->
                            Row(
                                modifier =
                                    Modifier
                                        .padding(vertical = 8.dp)
                                        .clickable {
                                            viewModel.setUsedAccount(account)
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(24.dp))
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(account.thumbnailUrl)
                                            .crossfade(550)
                                            .build(),
                                    placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                                    error = rememberVectorPainter(SimpIcons.PeopleAlt),
                                    contentDescription = account.name,
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, style = typo().labelMedium, color = MaterialTheme.colorScheme.onBackground)
                                    Text(account.email, style = typo().bodySmall)
                                }
                                Spacer(Modifier.width(12.dp))
                                AnimatedVisibility(account.isUsed) {
                                    Text(
                                        stringResource(Res.string.signed_in),
                                        style = typo().bodySmall,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.widthIn(0.dp, 64.dp),
                                    )
                                }
                                Spacer(Modifier.width(24.dp))
                            }
                        }
                    }
                } else {
                    item {
                        CenterLoadingBox(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                        )
                    }
                }
                item {
                    Column {
                        ActionButton(
                            icon = SimpIcons.PeopleAlt,
                            text = Res.string.guest,
                        ) {
                            viewModel.setUsedAccount(null)
                            onDismissRequest()
                        }
                        ActionButton(
                            icon = SimpIcons.Close,
                            text = Res.string.log_out,
                        ) {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.warning) },
                                    message = runBlocking { getString(Res.string.log_out_warning) },
                                    confirm =
                                        runBlocking { getString(Res.string.log_out) } to {
                                            viewModel.logOutAllYouTube()
                                            onDismissRequest()
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        }
                        ActionButton(
                            icon = SimpIcons.PlaylistAdd,
                            text = Res.string.add_an_account,
                        ) {
                            onDismissRequest()
                            navController.navigate(LoginDestination)
                        }
                    }
                }
            }
        }
    }
}
