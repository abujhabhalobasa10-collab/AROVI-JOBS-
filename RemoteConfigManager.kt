package com.arovi.jobs.firebase

import com.arovi.jobs.models.RemoteAdminConfig
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RemoteConfigManager {
    private val _config = MutableStateFlow(RemoteAdminConfig())
    val config: StateFlow<RemoteAdminConfig> = _config

    fun startListening() {
        try {
            val ref = FirebaseDatabase.getInstance().getReference("arovi_admin_config")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(RemoteAdminConfig::class.java)?.let {
                        _config.value = it
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (_: Exception) {}
    }
}