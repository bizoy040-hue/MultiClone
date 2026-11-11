package com.multiclone.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/** Runs in the work profile. Only installs packages that already exist on the device. */
class CloneRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkgs = intent.getStringArrayExtra(CloneManager.EXTRA_PKGS) ?: emptyArray()
        val ok = CloneManager(this).installIntoSpace(pkgs)
        Toast.makeText(this, getString(R.string.clone_result, ok, pkgs.size), Toast.LENGTH_LONG).show()
        finish()
    }
}
