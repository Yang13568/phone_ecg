package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.test.R
import com.example.test.HomeFragment
import com.google.android.material.navigation.NavigationBarView
import com.example.test.StateFragment
import com.example.test.FamilyFragment
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_sign_in.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var bottomNavigationView: BottomNavigationView? = null
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        supportFragmentManager.beginTransaction().replace(R.id.main_container, HomeFragment())
            .commit()
        bottomNavigationView.setSelectedItemId(R.id.nav_home)
        bottomNavigationView.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            var fragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> fragment = HomeFragment()
                R.id.nav_state -> fragment = StateFragment()
                R.id.nav_record -> fragment = RecordFragment()
                R.id.nav_family -> fragment = FamilyFragment()
                R.id.logout ->  logout()
            }
            if (fragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.main_container, fragment!!)
                    .commit()
                bottomNavigationView.menu.findItem(item.itemId).isChecked = true
                return@OnItemSelectedListener true
            }
            false

        })
    }
    fun logout(){
        if(AuthUI.getInstance()!=null) {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    //跳回登入
                    val intent = Intent(this, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
        }else{
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
            finish()
        }
    }
}