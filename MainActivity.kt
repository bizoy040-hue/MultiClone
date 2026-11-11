package com.multiclone.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cm = CloneManager(this)
        setContent { MaterialTheme { App(cm, tick) } }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

private data class Tab(val icon: ImageVector, val label: Int)

@Composable
fun App(cm: CloneManager, tick: Int) {
    val tabs = listOf(
        Tab(Icons.Default.Home, R.string.tab_dashboard),
        Tab(Icons.AutoMirrored.Filled.List, R.string.tab_apps),
        Tab(Icons.Default.Person, R.string.tab_clones),
        Tab(Icons.Default.Settings, R.string.tab_settings),
    )
    var sel by remember { mutableIntStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = sel == i,
                    onClick = { sel = i },
                    icon = { Icon(t.icon, contentDescription = null) },
                    label = { Text(stringResource(t.label)) }
                )
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (sel) {
                0 -> Dashboard(cm, tick)
                1 -> AppsScreen(cm, tick)
                2 -> ClonesScreen(cm, tick)
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
fun Dashboard(cm: CloneManager, tick: Int) {
    val ctx = LocalContext.current
    val inside = remember(tick) { cm.isInsideSpace() }
    val has = remember(tick) { cm.hasSpace() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            val msg = if (inside) R.string.status_inside else if (has) R.string.status_ready else R.string.status_none
            Text(stringResource(msg), Modifier.padding(16.dp))
        }
        if (!inside && !has) {
            Button(onClick = {
                val i = cm.provisioningIntent()
                if (i == null) Toast.makeText(ctx, R.string.not_supported, Toast.LENGTH_LONG).show()
                else launcher.launch(i)
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_space)) }
        }
        Text(stringResource(R.string.limits_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.limits_body))
    }
}

private data class AppRow(val pkg: String, val label: String, val icon: ImageBitmap)

@Composable
fun AppsScreen(cm: CloneManager, tick: Int) {
    val ctx = LocalContext.current
    val inside = remember(tick) { cm.isInsideSpace() }
    val apps by produceState<List<AppRow>>(emptyList(), tick) {
        value = withContext(Dispatchers.Default) {
            val pm = ctx.packageManager
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
                .map { it.activityInfo.applicationInfo }
                .filter { it.packageName != ctx.packageName }
                .distinctBy { it.packageName }
                .map {
                    AppRow(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm).toBitmap(96, 96).asImageBitmap())
                }
                .sortedBy { it.label.lowercase() }
        }
    }
    val picked = remember { mutableStateListOf<String>() }
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.apps_title), Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.weight(1f)) {
            items(apps, key = { it.pkg }) { a ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!inside) {
                        Checkbox(checked = a.pkg in picked, onCheckedChange = { c ->
                            if (c) picked.add(a.pkg) else picked.remove(a.pkg)
                        })
                    }
                    Image(a.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(a.label, Modifier.weight(1f))
                }
            }
        }
        if (!inside) {
            Button(
                enabled = picked.isNotEmpty(),
                onClick = {
                    val msg = when {
                        !cm.canAddExistingApps() -> R.string.need_api28
                        !cm.hasSpace() -> R.string.need_space
                        cm.requestClone(picked.toList()) -> { picked.clear(); R.string.clone_requested }
                        else -> R.string.clone_failed
                    }
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text(stringResource(R.string.clone_selected, picked.size)) }
        }
    }
}

@Composable
fun ClonesScreen(cm: CloneManager, tick: Int) {
    val ctx = LocalContext.current
    val inside = remember(tick) { cm.isInsideSpace() }
    val apps = remember(tick) { if (inside) emptyList() else cm.spaceApps() }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.clones_title), style = MaterialTheme.typography.titleLarge)
        if (!inside && apps.isEmpty()) Text(stringResource(R.string.no_clones), Modifier.padding(top = 12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(apps, key = { it.pkg }) { a ->
                val icon = remember(a.pkg) { a.icon.toBitmap(96, 96).asImageBitmap() }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(icon, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(a.label, Modifier.weight(1f))
                    TextButton(onClick = {
                        if (!cm.open(a)) Toast.makeText(ctx, R.string.open_failed, Toast.LENGTH_LONG).show()
                    }) { Text(stringResource(R.string.open)) }
                }
            }
        }
        if (inside) {
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.remove_space))
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.remove_space)) },
            text = { Text(stringResource(R.string.remove_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; cm.removeSpace() }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    fun launchOrFallback(i: Intent) {
        try {
            ctx.startActivity(i)
        } catch (e: Exception) {
            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { }
        }
    }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }) {
                Text("English")
            }
            Button(onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("bn")) }) {
                Text("বাংলা")
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.otp_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.otp_body))
        Button(onClick = {
            launchOrFallback(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).setData(Uri.parse("package:")))
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_autofill)) }
        OutlinedButton(onClick = { launchOrFallback(Intent(Settings.ACTION_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.open_settings))
        }
    }
}
