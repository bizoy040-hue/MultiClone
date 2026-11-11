package com.multiclone.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

data class CloneApp(
    val pkg: String,
    val label: String,
    val icon: Drawable,
    val component: ComponentName,
    val user: UserHandle,
)

class CloneManager(private val ctx: Context) {
    private val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
    private val um = ctx.getSystemService(UserManager::class.java)
    private val launcherApps = ctx.getSystemService(LauncherApps::class.java)
    private val admin = ComponentName(ctx, CloneAdminReceiver::class.java)

    /** True when this copy of the app runs inside the work profile. */
    fun isInsideSpace(): Boolean = dpm.isProfileOwnerApp(ctx.packageName)

    private fun otherProfiles(): List<UserHandle> =
        if (isInsideSpace()) emptyList() else um.userProfiles.filter { it != Process.myUserHandle() }

    fun hasSpace(): Boolean = otherProfiles().isNotEmpty()

    fun canAddExistingApps(): Boolean = Build.VERSION.SDK_INT >= 28

    /** Null if the device does not allow creating a managed profile. */
    fun provisioningIntent(): Intent? {
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) return null
        return Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
    }

    /** Apps present in the clone space (our own app excluded). */
    fun spaceApps(): List<CloneApp> = otherProfiles().flatMap { user ->
        launcherApps.getActivityList(null, user)
            .filter { it.applicationInfo.packageName != ctx.packageName }
            .map {
                CloneApp(it.applicationInfo.packageName, it.label.toString(), it.getBadgedIcon(0), it.componentName, user)
            }
    }.sortedBy { it.label.lowercase() }

    fun open(app: CloneApp): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, null, null); true
    } catch (e: Exception) { false }

    /** Personal side: ask the work-profile copy of this app to install the packages there. */
    fun requestClone(pkgs: Collection<String>): Boolean = try {
        ctx.startActivity(
            Intent(ACTION_CLONE).putExtra(EXTRA_PKGS, pkgs.toTypedArray()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ); true
    } catch (e: Exception) { false }

    /** Work-profile side: install already-on-device packages into this profile. */
    fun installIntoSpace(pkgs: Array<String>): Int {
        if (!isInsideSpace() || Build.VERSION.SDK_INT < 28) return 0
        return pkgs.count { runCatching { dpm.installExistingPackage(admin, it) }.getOrDefault(false) }
    }

    fun removeSpace() { if (isInsideSpace()) dpm.wipeData(0) }

    companion object {
        const val ACTION_CLONE = "com.multiclone.app.action.CLONE"
        const val EXTRA_PKGS = "pkgs"
    }
}
