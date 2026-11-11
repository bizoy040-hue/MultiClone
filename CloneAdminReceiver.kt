package com.multiclone.app

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class CloneAdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = getComponentName(context)
        dpm.setProfileName(admin, context.getString(R.string.profile_name))
        // Lets the personal side send "install these apps" requests into the work profile.
        val filter = IntentFilter(CloneManager.ACTION_CLONE).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        dpm.addCrossProfileIntentFilter(admin, filter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS)
        dpm.setProfileEnabled(admin)
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
