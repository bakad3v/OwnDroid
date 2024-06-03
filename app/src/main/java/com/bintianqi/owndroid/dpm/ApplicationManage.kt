package com.bintianqi.owndroid.dpm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT
import android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
import android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
import android.app.admin.PackagePolicy
import android.app.admin.PackagePolicy.PACKAGE_POLICY_ALLOWLIST
import android.app.admin.PackagePolicy.PACKAGE_POLICY_ALLOWLIST_AND_SYSTEM
import android.app.admin.PackagePolicy.PACKAGE_POLICY_BLOCKLIST
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build.VERSION
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bintianqi.owndroid.*
import com.bintianqi.owndroid.R
import com.bintianqi.owndroid.ui.*
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

private var dialogConfirmButtonAction = {}
private var dialogDismissButtonAction = {}
private var dialogGetStatus = { false }

@Composable
fun ApplicationManage(navCtrl:NavHostController, pkgName: MutableState<String>, dialogStatus: MutableIntState) {
    val focusMgr = LocalFocusManager.current
    val localNavCtrl = rememberNavController()
    val backStackEntry by localNavCtrl.currentBackStackEntryAsState()
    val titleMap = mapOf(
        "BlockUninstall" to R.string.block_uninstall,
        "UserControlDisabled" to R.string.ucd,
        "PermissionManage" to R.string.permission_manage,
        "CrossProfilePackage" to R.string.cross_profile_package,
        "CrossProfileWidget" to R.string.cross_profile_widget,
        "CredentialManagePolicy" to R.string.credential_manage_policy,
        "Accessibility" to R.string.permitted_accessibility_services,
        "IME" to R.string.permitted_ime,
        "KeepUninstalled" to R.string.keep_uninstalled_packages,
        "InstallApp" to R.string.install_app,
        "UninstallApp" to R.string.uninstall_app,
        "ClearAppData" to R.string.clear_app_storage,
        "DefaultDialer" to R.string.set_default_dialer,
    )
    val clearAppDataDialog = remember { mutableStateOf(false) }
    val defaultDialerAppDialog = remember { mutableStateOf(false) }
    val enableSystemAppDialog = remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopBar(backStackEntry, navCtrl, localNavCtrl) {
                Text(text = stringResource(titleMap[backStackEntry?.destination?.route] ?: R.string.app_manager))
            }
        }
    ) {  paddingValues->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())
        ) {
            if(backStackEntry?.destination?.route!="InstallApp") { 
                TextField(
                    value = pkgName.value,
                    onValueChange = { pkgName.value = it },
                    label = { Text(stringResource(R.string.package_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {focusMgr.clearFocus()}),
                    trailingIcon = {
                        Icon(painter = painterResource(R.drawable.checklist_fill0), contentDescription = null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = {navCtrl.navigate("PackageSelector")})
                                .padding(3.dp))
                    },
                    singleLine = true
                )
            }
            NavHost(
                navController = localNavCtrl, startDestination = "Home",
                enterTransition = Animations.navHostEnterTransition,
                exitTransition = Animations.navHostExitTransition,
                popEnterTransition = Animations.navHostPopEnterTransition,
                popExitTransition = Animations.navHostPopExitTransition
            ) { 
                composable(route = "Home") {
                    Home(localNavCtrl, pkgName.value, dialogStatus, clearAppDataDialog, defaultDialerAppDialog, enableSystemAppDialog)
                }
                composable(route = "UserControlDisabled") { UserCtrlDisabledPkg(pkgName.value) }
                composable(route = "PermissionManage") { PermissionManage(pkgName.value, navCtrl) }
                composable(route = "CrossProfilePackage") { CrossProfilePkg(pkgName.value) }
                composable(route = "CrossProfileWidget") { CrossProfileWidget(pkgName.value) }
                composable(route = "CredentialManagePolicy") { CredentialManagePolicy(pkgName.value) }
                composable(route = "Accessibility") { PermittedAccessibility(pkgName.value) }
                composable(route = "IME") { PermittedIME(pkgName.value) }
                composable(route = "KeepUninstalled") { KeepUninstalledApp(pkgName.value) }
                composable(route = "InstallApp") { InstallApp() }
                composable(route = "UninstallApp") { UninstallApp(pkgName.value) }
            }
        }
    }
    if(dialogStatus.intValue!=0) { 
        LocalFocusManager.current.clearFocus()
        AppControlDialog(dialogStatus)
    }
    if(clearAppDataDialog.value) {
        ClearAppDataDialog(clearAppDataDialog, pkgName.value)
    }
    if(defaultDialerAppDialog.value) {
        DefaultDialerAppDialog(defaultDialerAppDialog, pkgName.value)
    }
    if(enableSystemAppDialog.value) {
        EnableSystemAppDialog(enableSystemAppDialog, pkgName.value)
    }
}

@Composable
private fun Home(
    navCtrl:NavHostController,
    pkgName: String,
    dialogStatus: MutableIntState,
    clearAppDataDialog: MutableState<Boolean>,
    defaultDialerAppDialog: MutableState<Boolean>,
    enableSystemAppDialog: MutableState<Boolean>
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        val context = LocalContext.current
        val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val receiver = ComponentName(context, Receiver::class.java)
        Spacer(Modifier.padding(vertical = 5.dp))
        if(VERSION.SDK_INT >= 24&&isProfileOwner(dpm)&&dpm.isManagedProfile(receiver)) {
            Text(text = stringResource(R.string.scope_is_work_profile), textAlign = TextAlign.Center,modifier = Modifier.fillMaxWidth())
        }
        SubPageItem(R.string.app_info,"", R.drawable.open_in_new) { 
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.parse("package:$pkgName"))
            startActivity(context, intent, null)
        }
        if(VERSION.SDK_INT>=24 && (isDeviceOwner(dpm) || isProfileOwner(dpm))) { 
            val getSuspendStatus = {
                try{ dpm.isPackageSuspended(receiver, pkgName) }
                catch(e:NameNotFoundException) {  false }
                catch(e:IllegalArgumentException) {  false }
            }
            SwitchItem(
                title = R.string.suspend, desc = "", icon = R.drawable.block_fill0,
                getState = getSuspendStatus,
                onCheckedChange = { dpm.setPackagesSuspended(receiver, arrayOf(pkgName), it) },
                onClickBlank = {
                    dialogGetStatus = getSuspendStatus
                    dialogConfirmButtonAction = { dpm.setPackagesSuspended(receiver, arrayOf(pkgName), true) }
                    dialogDismissButtonAction = { dpm.setPackagesSuspended(receiver, arrayOf(pkgName), false) }
                    dialogStatus.intValue = 1
                }
            )
        }
        if(isDeviceOwner(dpm) || isProfileOwner(dpm)) {
            SwitchItem(
                title = R.string.hide, desc = stringResource(R.string.isapphidden_desc), icon = R.drawable.visibility_off_fill0,
                getState = { dpm.isApplicationHidden(receiver,pkgName) },
                onCheckedChange = { dpm.setApplicationHidden(receiver, pkgName, it) },
                onClickBlank = {
                    dialogGetStatus = { dpm.isApplicationHidden(receiver,pkgName) }
                    dialogConfirmButtonAction = { dpm.setApplicationHidden(receiver, pkgName, true) }
                    dialogDismissButtonAction = { dpm.setApplicationHidden(receiver, pkgName, false) }
                    dialogStatus.intValue = 2
                }
            )
        }
        if(isDeviceOwner(dpm) || isProfileOwner(dpm)) {
            SwitchItem(
                title = R.string.block_uninstall, desc = "", icon = R.drawable.delete_forever_fill0,
                getState = { dpm.isUninstallBlocked(receiver,pkgName) },
                onCheckedChange = { dpm.setUninstallBlocked(receiver,pkgName,it) },
                onClickBlank = {
                    dialogGetStatus = { dpm.isUninstallBlocked(receiver,pkgName) }
                    dialogConfirmButtonAction = { dpm.setUninstallBlocked(receiver,pkgName,true) }
                    dialogDismissButtonAction = { dpm.setUninstallBlocked(receiver,pkgName,false) }
                    dialogStatus.intValue = 3
                }
            )
        }
        if(VERSION.SDK_INT>=24 && (isDeviceOwner(dpm) || isProfileOwner(dpm))) {
            val setAlwaysOnVpn: (Boolean)->Unit = {
                try {
                    dpm.setAlwaysOnVpnPackage(receiver, pkgName, it)
                } catch(e: UnsupportedOperationException) {
                    Toast.makeText(context, R.string.unsupported, Toast.LENGTH_SHORT).show()
                } catch(e: NameNotFoundException) {
                    Toast.makeText(context, R.string.not_installed, Toast.LENGTH_SHORT).show()
                }
            }
            SwitchItem(
                title = R.string.always_on_vpn, desc = "", icon = R.drawable.vpn_key_fill0,
                getState = { pkgName == dpm.getAlwaysOnVpnPackage(receiver) },
                onCheckedChange = setAlwaysOnVpn,
                onClickBlank = {
                    dialogGetStatus = { pkgName == dpm.getAlwaysOnVpnPackage(receiver) }
                    dialogConfirmButtonAction = { setAlwaysOnVpn(true) }
                    dialogDismissButtonAction = { setAlwaysOnVpn(false) }
                    dialogStatus.intValue = 4
                }
            )
        }
        if((VERSION.SDK_INT>=33&&isProfileOwner(dpm))||(VERSION.SDK_INT>=30&&isDeviceOwner(dpm))) { 
            SubPageItem(R.string.ucd, "", R.drawable.do_not_touch_fill0) { navCtrl.navigate("UserControlDisabled") }
        }
        if(VERSION.SDK_INT>=23&&(isDeviceOwner(dpm)||isProfileOwner(dpm))) { 
            SubPageItem(R.string.permission_manage, "", R.drawable.key_fill0) { navCtrl.navigate("PermissionManage") }
        }
        if(VERSION.SDK_INT>=30&&isProfileOwner(dpm)&&dpm.isManagedProfile(receiver)) { 
            SubPageItem(R.string.cross_profile_package, "", R.drawable.work_fill0) { navCtrl.navigate("CrossProfilePackage") }
        }
        if(isProfileOwner(dpm)) { 
            SubPageItem(R.string.cross_profile_widget, "", R.drawable.widgets_fill0) { navCtrl.navigate("CrossProfileWidget") }
        }
        if(VERSION.SDK_INT>=34&&isDeviceOwner(dpm)) { 
            SubPageItem(R.string.credential_manage_policy, "", R.drawable.license_fill0) { navCtrl.navigate("CredentialManagePolicy") }
        }
        if(isProfileOwner(dpm)||isDeviceOwner(dpm)) { 
            SubPageItem(R.string.permitted_accessibility_services, "", R.drawable.settings_accessibility_fill0) { navCtrl.navigate("Accessibility") }
        }
        if(isDeviceOwner(dpm)||isProfileOwner(dpm)) { 
            SubPageItem(R.string.permitted_ime, "", R.drawable.keyboard_fill0) { navCtrl.navigate("IME") }
        }
        if(isDeviceOwner(dpm) || isProfileOwner(dpm)) {
            SubPageItem(R.string.enable_system_app, "", R.drawable.enable_fill0) { enableSystemAppDialog.value = true }
        }
        if(VERSION.SDK_INT>=28&&isDeviceOwner(dpm)) { 
            SubPageItem(R.string.keep_uninstalled_packages, "", R.drawable.delete_fill0) { navCtrl.navigate("KeepUninstalled") }
        }
        if(VERSION.SDK_INT>=28) { 
            SubPageItem(R.string.clear_app_storage, "", R.drawable.mop_fill0) {
                if(pkgName != "") { clearAppDataDialog.value = true }
            }
        }
        SubPageItem(R.string.install_app, "", R.drawable.install_mobile_fill0) { navCtrl.navigate("InstallApp") }
        SubPageItem(R.string.uninstall_app, "", R.drawable.delete_fill0) { navCtrl.navigate("UninstallApp") }
        if(VERSION.SDK_INT >= 34 && (isDeviceOwner(dpm) || isProfileOwner(dpm))) {
            SubPageItem(R.string.set_default_dialer, "", R.drawable.call_fill0) { defaultDialerAppDialog.value = true }
        }
        Spacer(Modifier.padding(vertical = 30.dp))
        LaunchedEffect(Unit) { fileUriFlow.value = Uri.parse("") }
    }
}

@SuppressLint("NewApi")
@Composable
private fun UserCtrlDisabledPkg(pkgName:String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val pkgList = remember { mutableStateListOf<String>() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
        val refresh = {
            pkgList.clear()
            dpm.getUserControlDisabledPackages(receiver).forEach { pkgList.add(it) }
        }
        LaunchedEffect(Unit) { refresh() }
        var inited by remember{mutableStateOf(false)}
        if(!inited) { refresh();inited=true }
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.ucd), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Text(text = stringResource(R.string.ucd_desc))
        Spacer(Modifier.padding(vertical = 5.dp))
        Text(text = stringResource(R.string.app_list_is))
        SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) { 
            Text(text = if(pkgList.isEmpty()) stringResource(R.string.none) else pkgList.toText())
        }
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
            Button(
                onClick = {
                    pkgList.add(pkgName)
                    dpm.setUserControlDisabledPackages(receiver, pkgList)
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.49F)
            ) { 
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = {
                    pkgList.remove(pkgName)
                    dpm.setUserControlDisabledPackages(receiver,pkgList)
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.96F)
            ) { 
                Text(stringResource(R.string.remove))
            }
        }
        Button(
            onClick = { dpm.setUserControlDisabledPackages(receiver, listOf()); refresh() },
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text(stringResource(R.string.clear_list))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@SuppressLint("NewApi")
@Composable
private fun PermissionManage(pkgName: String, navCtrl: NavHostController) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val focusMgr = LocalFocusManager.current
    var inputPermission by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf(context.getString(R.string.unknown)) }
    val grantState = mapOf(
        PERMISSION_GRANT_STATE_DEFAULT to stringResource(R.string.decide_by_user),
        PERMISSION_GRANT_STATE_GRANTED to stringResource(R.string.granted),
        PERMISSION_GRANT_STATE_DENIED to stringResource(R.string.denied)
    )
    val applyPermission by selectedPermission.collectAsState()
    LaunchedEffect(applyPermission) {
        if(applyPermission != "") {
            inputPermission = applyPermission
            selectedPermission.value = ""
        }
    }
    LaunchedEffect(pkgName) {
        if(pkgName!="") { currentState = grantState[dpm.getPermissionGrantState(receiver,pkgName,inputPermission)]!! }
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.permission_manage), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        OutlinedTextField(
            value = inputPermission,
            label = { Text(stringResource(R.string.permission)) },
            onValueChange = {
                inputPermission = it
                currentState = grantState[dpm.getPermissionGrantState(receiver,pkgName,inputPermission)]!!
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusMgr.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(painter = painterResource(R.drawable.checklist_fill0), contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = { navCtrl.navigate("PermissionPicker") })
                        .padding(3.dp))
            }
        )
        Spacer(Modifier.padding(vertical = 5.dp))
        Text(stringResource(R.string.current_state, currentState))
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
            Button(
                onClick = {
                    dpm.setPermissionGrantState(receiver,pkgName,inputPermission, PERMISSION_GRANT_STATE_GRANTED)
                    currentState = grantState[dpm.getPermissionGrantState(receiver,pkgName,inputPermission)]!!
                },
                modifier = Modifier.fillMaxWidth(0.49F)
            ) {
                Text(stringResource(R.string.grant))
            }
            Button(
                onClick = {
                    dpm.setPermissionGrantState(receiver,pkgName,inputPermission, PERMISSION_GRANT_STATE_DENIED)
                    currentState = grantState[dpm.getPermissionGrantState(receiver,pkgName,inputPermission)]!!
                },
                Modifier.fillMaxWidth(0.96F)
            ) {
                Text(stringResource(R.string.deny))
            }
        }
        Button(
            onClick = {
                dpm.setPermissionGrantState(receiver,pkgName,inputPermission, PERMISSION_GRANT_STATE_DEFAULT)
                currentState = grantState[dpm.getPermissionGrantState(receiver,pkgName,inputPermission)]!!
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.decide_by_user))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@SuppressLint("NewApi")
@Composable
private fun CrossProfilePkg(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context, Receiver::class.java)
    val crossProfilePkg = remember { mutableStateListOf<String>() }
    val refresh = {
        crossProfilePkg.clear()
        dpm.getCrossProfilePackages(receiver).forEach { crossProfilePkg += it }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.cross_profile_package), style = typography.headlineLarge)
        Text(text = stringResource(R.string.app_list_is))
        SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) { 
            Text(text = if(crossProfilePkg.isEmpty()) stringResource(R.string.none) else crossProfilePkg.toText())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
            Button(
                onClick = {
                    crossProfilePkg.add(pkgName)
                    dpm.setCrossProfilePackages(receiver, crossProfilePkg.toSet())
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.49F)
            ) {
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = {
                    crossProfilePkg.remove(pkgName)
                    dpm.setCrossProfilePackages(receiver, crossProfilePkg.toSet())
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.96F)
            ) {
                Text(stringResource(R.string.remove))
            }
        }
        Button(
            onClick = {
                crossProfilePkg.clear()
                dpm.setCrossProfilePackages(receiver, crossProfilePkg.toSet())
                refresh()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.clear_list))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@Composable
private fun CrossProfileWidget(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val pkgList = remember { mutableStateListOf<String>() }
    val refresh = {
        pkgList.clear()
        dpm.getCrossProfileWidgetProviders(receiver).forEach {
            pkgList += it
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.cross_profile_widget), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Text(text = stringResource(R.string.app_list_is))
        SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) { 
            Text(text = if(pkgList.isEmpty()) stringResource(R.string.none) else pkgList.toText())
        }
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
            Button(
                onClick = {
                    if(pkgName != "") { dpm.addCrossProfileWidgetProvider(receiver, pkgName) }
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.49F)
            ) { 
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = {
                    if(pkgName != "") { dpm.removeCrossProfileWidgetProvider(receiver, pkgName) }
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.96F)
            ) { 
                Text(stringResource(R.string.remove))
            }
        }
        Button(
            onClick = {
                pkgList.forEach {
                    dpm.removeCrossProfileWidgetProvider(receiver, it)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.clear_list))
        }
        Spacer(Modifier.padding(vertical = 10.dp))
    }
}

@SuppressLint("NewApi")
@Composable
private fun CredentialManagePolicy(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    var policy: PackagePolicy?
    var policyType by remember{ mutableIntStateOf(-1) }
    val credentialList = remember { mutableStateListOf<String>() }
    val refreshPolicy = {
        policy = dpm.credentialManagerPolicy
        policyType = policy?.policyType ?: -1
        (policy?.packageNames ?: mutableSetOf()).forEach { credentialList += it }
    }
    val apply = {
        try {
            if(policyType != -1 && credentialList.isNotEmpty()) {
                dpm.credentialManagerPolicy = PackagePolicy(policyType, credentialList.toSet())
            }else{
                dpm.credentialManagerPolicy = null
            }
            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
        } catch(e:java.lang.IllegalArgumentException) {
            Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show()
        } finally {
            refreshPolicy()
        }
    }
    LaunchedEffect(Unit) { refreshPolicy() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.credential_manage_policy), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        RadioButtonItem(
            stringResource(R.string.none),
            { policyType==-1 }, { policyType=-1 }
        )
        RadioButtonItem(
            stringResource(R.string.blacklist),
            { policyType==PACKAGE_POLICY_BLOCKLIST },
            { policyType=PACKAGE_POLICY_BLOCKLIST }
        )
        RadioButtonItem(
            stringResource(R.string.whitelist),
            { policyType==PACKAGE_POLICY_ALLOWLIST },
            { policyType=PACKAGE_POLICY_ALLOWLIST }
        )
        RadioButtonItem(
            stringResource(R.string.whitelist_and_system_app),
            { policyType==PACKAGE_POLICY_ALLOWLIST_AND_SYSTEM },
            { policyType=PACKAGE_POLICY_ALLOWLIST_AND_SYSTEM }
        )
        Spacer(Modifier.padding(vertical = 5.dp))
        AnimatedVisibility(policyType != -1) {
            Column {
                Text(stringResource(R.string.app_list_is))
                SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) {
                    Text(text = if(credentialList.isEmpty()) stringResource(R.string.none) else credentialList.toText())
                }
                Spacer(Modifier.padding(vertical = 10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                    Button(
                        onClick = {
                            credentialList.add(pkgName)
                            apply()
                        },
                        modifier = Modifier.fillMaxWidth(0.49F)
                    ) {
                        Text(stringResource(R.string.add))
                    }
                    Button(
                        onClick = {
                            credentialList.remove(pkgName)
                            apply()
                        },
                        modifier = Modifier.fillMaxWidth(0.96F)
                    ) {
                        Text(stringResource(R.string.remove))
                    }
                }
                Button(
                    onClick = {
                        credentialList.clear()
                        apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clear_list))
                }
            }
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@Composable
private fun PermittedAccessibility(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val pkgList = remember { mutableStateListOf<String>() }
    var allowAll by remember { mutableStateOf(false) }
    val refresh = {
        pkgList.clear()
        val getList = dpm.getPermittedAccessibilityServices(receiver)
        if(getList != null) {
            allowAll = false
            getList.forEach { pkgList += it }
        } else {
            allowAll = true
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.permitted_accessibility_services), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.allow_all), style = typography.titleLarge)
            Switch(
                checked = allowAll,
                onCheckedChange = {
                    dpm.setPermittedAccessibilityServices(receiver, if(it) null else listOf())
                    refresh()
                }
            )
        }
        AnimatedVisibility(!allowAll) {
            Column {
                SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) {
                    if (pkgList.isEmpty()) {
                        Text(stringResource(R.string.only_system_accessibility_allowed))
                    } else {
                        Text(stringResource(R.string.permitted_packages_is) + pkgList.toText())
                    }
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = {
                            pkgList.add(pkgName)
                            dpm.setPermittedAccessibilityServices(receiver, pkgList)
                            refresh()
                        },
                        modifier = Modifier.fillMaxWidth(0.49F)
                    ) {
                        Text(stringResource(R.string.add))
                    }
                    Button(
                        onClick = {
                            pkgList.remove(pkgName)
                            dpm.setPermittedAccessibilityServices(receiver, pkgList)
                            refresh()
                        },
                        modifier = Modifier.fillMaxWidth(0.96F)
                    ) {
                        Text(stringResource(R.string.remove))
                    }
                }
                Button(
                    onClick = {
                        pkgList.clear()
                        dpm.setPermittedAccessibilityServices(receiver, pkgList)
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clear_list))
                }
            }
        }
        Information {
            Text(stringResource(R.string.system_accessibility_always_allowed))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@Composable
private fun PermittedIME(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val permittedIme = remember { mutableStateListOf<String>() }
    var allowAll by remember { mutableStateOf(false) }
    val refresh = {
        permittedIme.clear()
        val getList = dpm.getPermittedInputMethods(receiver)
        if(getList != null) {
            allowAll = false
            getList.forEach { permittedIme += it }
        } else {
            allowAll = true
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.permitted_ime), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.allow_all), style = typography.titleLarge)
            Switch(
                checked = allowAll,
                onCheckedChange = {
                    dpm.setPermittedInputMethods(receiver, if(it) null else listOf())
                    refresh()
                }
            )
        }
        AnimatedVisibility(!allowAll) {
            Column {
                SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) {
                    if(permittedIme.isEmpty()) {
                        Text(stringResource(R.string.only_system_ime_allowed))
                    } else {
                        Text(stringResource(R.string.permitted_packages_is) + permittedIme.toText())
                    }
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = {
                            permittedIme.add(pkgName)
                            dpm.setPermittedInputMethods(receiver, permittedIme)
                            refresh()
                        },
                        modifier = Modifier.fillMaxWidth(0.49F)
                    ) {
                        Text(stringResource(R.string.add))
                    }
                    Button(
                        onClick = {
                            permittedIme.remove(pkgName)
                            dpm.setPermittedInputMethods(receiver, permittedIme)
                            refresh()
                        },
                        modifier = Modifier.fillMaxWidth(0.96F)
                    ) {
                        Text(stringResource(R.string.remove))
                    }
                }
                Button(
                    onClick = {
                        permittedIme.clear()
                        dpm.setPermittedInputMethods(receiver, permittedIme)
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clear_list))
                }
            }
        }
        Information {
            Text(stringResource(R.string.system_ime_always_allowed))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@SuppressLint("NewApi")
@Composable
private fun KeepUninstalledApp(pkgName: String) { 
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    val pkgList = remember { mutableStateListOf<String>() }
    val refresh = {
        pkgList.clear()
        dpm.getKeepUninstalledPackages(receiver)?.forEach { pkgList += it }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.keep_uninstalled_packages), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Text(text = stringResource(R.string.app_list_is))
        SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState()).animateContentSize()) {
            Text(text = if(pkgList.isEmpty()) stringResource(R.string.none) else pkgList.toText())
        }
        Spacer(Modifier.padding(vertical = 5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
            Button(
                onClick = {
                    pkgList.add(pkgName)
                    dpm.setKeepUninstalledPackages(receiver, pkgList)
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.49F)
            ) { 
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = {
                    pkgList.remove(pkgName)
                    dpm.setKeepUninstalledPackages(receiver, pkgList)
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(0.96F)
            ) { 
                Text(stringResource(R.string.remove))
            }
        }
        Button(
            onClick = {
                pkgList.clear()
                dpm.setKeepUninstalledPackages(receiver, pkgList)
                refresh()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.clear_list))
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@Composable
private fun UninstallApp(pkgName: String) { 
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.uninstall_app), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Column(modifier = Modifier.fillMaxWidth()) { 
            Button(
                onClick = {
                    val intent = Intent(context, PackageInstallerReceiver::class.java)
                    val intentSender = PendingIntent.getBroadcast(context, 8, intent, PendingIntent.FLAG_IMMUTABLE).intentSender
                    val pkgInstaller = context.packageManager.packageInstaller
                    pkgInstaller.uninstall(pkgName, intentSender)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.silent_uninstall))
            }
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                    intent.setData(Uri.parse("package:$pkgName"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.request_uninstall))
            }
        }
    }
}

@Composable
private fun InstallApp() { 
    val context = LocalContext.current
    val focusMgr = LocalFocusManager.current
    val selected = fileUriFlow.collectAsState().value != Uri.parse("")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) { 
        Spacer(Modifier.padding(vertical = 10.dp))
        Text(text = stringResource(R.string.install_app), style = typography.headlineLarge)
        Spacer(Modifier.padding(vertical = 5.dp))
        Button(
            onClick = {
                focusMgr.clearFocus()
                val installApkIntent = Intent(Intent.ACTION_GET_CONTENT)
                installApkIntent.setType("application/vnd.android.package-archive")
                installApkIntent.addCategory(Intent.CATEGORY_OPENABLE)
                getFile.launch(installApkIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_apk))
        }
        AnimatedVisibility(selected) {
            Spacer(Modifier.padding(vertical = 3.dp))
            Column(modifier = Modifier.fillMaxWidth()) { 
                Button(
                    onClick = { uriToStream(context, fileUriFlow.value) { stream -> installPackage(context,stream)} },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.silent_install))
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                        intent.setData(fileUriFlow.value)
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.request_install))
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
private fun ClearAppDataDialog(status: MutableState<Boolean>, pkgName: String) {
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    AlertDialog(
        title = { Text(text = stringResource(R.string.clear_app_storage)) },
        text = {
            Text(stringResource(R.string.app_storage_will_be_cleared) + "\n" + pkgName)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val executor = Executors.newCachedThreadPool()
                    val onClear = DevicePolicyManager.OnClearApplicationUserDataListener { pkg: String, succeed: Boolean ->
                        Looper.prepare()
                        val toastText =
                            if(pkg!="") { "$pkg\n" }else{ "" } +
                                    context.getString(R.string.clear_data) +
                                    context.getString(if(succeed) R.string.success else R.string.fail )
                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                        Looper.loop()
                    }
                    dpm.clearApplicationUserData(receiver, pkgName, executor, onClear)
                    status.value = false
                },
                colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)
            ) {
                Text(text = stringResource(R.string.clear))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { status.value = false }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        onDismissRequest = { status.value = false },
        modifier = Modifier.fillMaxWidth()
    )
}

@SuppressLint("NewApi")
@Composable
private fun DefaultDialerAppDialog(status: MutableState<Boolean>, pkgName: String) {
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    AlertDialog(
        title = { Text(stringResource(R.string.set_default_dialer)) },
        text = {
            Text(stringResource(R.string.app_will_be_default_dialer) + "\n" + pkgName)
        },
        onDismissRequest = { status.value = false },
        dismissButton = {
            TextButton(onClick = { status.value = false }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try{
                        dpm.setDefaultDialerApplication(pkgName)
                        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                    }catch(e:IllegalArgumentException) {
                        Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show()
                    }
                    status.value = false
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EnableSystemAppDialog(status: MutableState<Boolean>, pkgName: String) {
    val context = LocalContext.current
    val dpm = context.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val receiver = ComponentName(context,Receiver::class.java)
    AlertDialog(
        title = { Text(stringResource(R.string.enable_system_app)) },
        text = {
            Text(stringResource(R.string.enable_system_app_desc) + "\n" + pkgName)
        },
        onDismissRequest = { status.value = false },
        dismissButton = {
            TextButton(onClick = { status.value = false }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        dpm.enableSystemApp(receiver, pkgName)
                        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                    } catch(e: IllegalArgumentException) {
                        Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show()
                    }
                    status.value = false
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AppControlDialog(status: MutableIntState) {
    val enabled = dialogGetStatus()
    Dialog(
        onDismissRequest = { status.intValue = 0 }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) { 
            Column(
                modifier = Modifier.fillMaxWidth().padding(15.dp)
            ) { 
                Text(
                    text = stringResource(
                        when(status.intValue) { 
                            1 -> R.string.suspend
                            2 -> R.string.hide
                            3 -> R.string.block_uninstall
                            4 -> R.string.always_on_vpn
                            else -> R.string.unknown
                        }
                    ),
                    style = typography.headlineMedium,
                    modifier = Modifier.padding(start = 5.dp)
                )
                Text(
                    text = stringResource(R.string.current_status_is) + stringResource(if(enabled) R.string.enabled else R.string.disabled),
                    modifier = Modifier.padding(start = 5.dp, top = 5.dp, bottom = 5.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) { 
                    TextButton(
                        onClick = { status.intValue = 0 }
                    ) { 
                        Text(text = stringResource(R.string.cancel))
                    }
                    Row{
                        TextButton(
                            onClick = { dialogDismissButtonAction(); status.intValue = 0 }
                        ) { 
                            Text(text = stringResource(R.string.disable))
                        }
                        TextButton(
                            onClick = { dialogConfirmButtonAction(); status.intValue = 0 }
                        ) { 
                            Text(text = stringResource(R.string.enable))
                        }
                    }
                }
            }
        }
    }
}

@Throws(IOException::class)
private fun installPackage(context: Context, inputStream: InputStream) { 
    val packageInstaller = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val sessionId = packageInstaller.createSession(params)
    val session = packageInstaller.openSession(sessionId)
    val out = session.openWrite("COSU", 0, -1)
    val buffer = ByteArray(65536)
    var c: Int
    while(inputStream.read(buffer).also{c = it}!=-1) { out.write(buffer, 0, c) }
    session.fsync(out)
    inputStream.close()
    out.close()
    val intent = Intent(context, PackageInstallerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_IMMUTABLE).intentSender
    session.commit(pendingIntent)
}
