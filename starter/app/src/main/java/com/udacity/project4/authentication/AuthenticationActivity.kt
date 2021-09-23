package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_authentication
        )
        // register the onResultCallback of the Firebase contract login
        registerOnResult()




        binding.loginButton.setOnClickListener{
            Log.i("Auth Activity", "loginButton setOnClickListener")
            // Create an account and sign in using FirebaseUI, use sign in using email
            // and sign in using Google
            createAccount()

        }
//       A bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

    }

    private fun registerOnResult(){
        signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) {
            val response = it.idpResponse
            if (it.resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                goRemindLocation()
                Log.i(TAG, "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!")
            }else{
                Snackbar.make(
                    binding.authenticationLayout, this.getString(R.string.logging_error),
                    Snackbar.LENGTH_LONG
                ).show()
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.e(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun createAccount() {
        // Give users the option to sign in / register with their email or Google account.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder()
                .build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.NewTheme)
            .setLogo(R.drawable.locationclock)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun goRemindLocation(){
        startActivity(Intent(this, RemindersActivity::class.java))
        finish() // to prevent the user from going back pressing the back button
    }

    companion object {
        const val TAG = "ActivityAuth"
        const val SIGN_IN_REQUEST_CODE = 1001
    }
}
