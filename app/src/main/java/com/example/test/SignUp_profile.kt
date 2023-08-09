package com.example.test

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore

import com.example.test.databinding.ActivitySingUpBinding

class SignUp_profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_profile)
        var name = findViewById<EditText>(R.id.editText_Name)
        var phone = findViewById<EditText>(R.id.editText_Phone)
        var address = findViewById<EditText>(R.id.editText_Address)
        var edit_email = findViewById<EditText>(R.id.editText_Email)
        var submit_btn = findViewById<Button>(R.id.button_submit)
        val email = intent.getStringExtra("email")
        edit_email.setText(email.toString())
        submit_btn.setOnClickListener() {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("USER")
            val data = hashMapOf(
                "userAddress" to address.text.toString(),
                //"userEmail" to email.text.toString(),
                "userEmail" to email,
                "userName" to name.text.toString(),
                "userPhone" to phone.text.toString()
            )
            ref.add(data)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                }
        }
    }
}