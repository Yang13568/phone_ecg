package com.example.test

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivitySignInBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore


class SignInActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        setContentView(binding.root)
        //email sign in
        binding.textView13.setOnClickListener() {
            val intent = Intent(this, SingUpActivity::class.java)
            startActivity(intent)
        }
        val mAuth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = mAuth.getCurrentUser()
        if (user == null) {
            binding.button.setOnClickListener() {
                val email = binding.emailET.text.toString()
                val pass = binding.passET.text.toString()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("email", email);
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                            println(it.exception.toString())
                        }
                    }
                } else {
                    Toast.makeText(this, "Empty Fields Are Not Allowed", Toast.LENGTH_SHORT).show()
                }
            }
            //google sign in
            findViewById<ImageView>(R.id.imageView).setOnClickListener() {
                signIn()
            }
        }
        else{
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", user.email);
            startActivity(intent);
            finish();
        }
    }

    //FirebaseUI
    fun signIn() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "login", Toast.LENGTH_SHORT).show()
            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()
            val email = user?.email
            val usersCollection = db.collection("USER")
            usersCollection.whereEqualTo("userEmail", email).get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("email",email);
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, SignUp_profile::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
        }
    }
}