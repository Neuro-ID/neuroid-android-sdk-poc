package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import com.neuroid.tracker.NeuroID

class Login: LayoutActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_fragment)
        supportFragmentManager.beginTransaction().add(R.id.login_fragment, LoginFragment()).commit()
    }

    fun login(userId: String) {
        ApplicationMain.isLoggedIn = true
        NeuroID.getInstance()?.identify("${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
        NeuroID.getInstance()?.attemptedLogin(userId)
        startActivity(Intent(this, Instructions::class.java))
    }

    fun goBack() {
        startActivity(Intent(this, Splash::class.java))
    }
}