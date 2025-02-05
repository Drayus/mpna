package com.example.mobileprofessionalnetworkanalyzer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.auth0.android.jwt.JWT
import com.example.mobileprofessionalnetworkanalyzer.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val CLIENT_ID = "866t5tb7ri2vaq"
    private val REDIRECT_URI = "myapp://linkedin-auth"
    private val CLIENT_SECRET = "WPL_AP1.RXp1ysmPeKll9pIc.H/M33Q=="
    private val STATE = "10"  // A random string to prevent CSRF attacks
    private val NONCE = "10"  // A unique value to prevent replay attacks

    private val AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization" +
            "?response_type=code" +
            "&client_id=$CLIENT_ID" +
            "&redirect_uri=$REDIRECT_URI" +
            "&scope=openid%20profile%20email" +
            "&state=$STATE" +
            "&nonce=$NONCE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    fun openLinkedInLogin(view: View) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(this, Uri.parse(AUTH_URL))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            if (uri.toString().startsWith("myapp://linkedin-auth")) {
                val authCode = uri.getQueryParameter("code")
                if (authCode != null) {
                    exchangeCodeForAccessToken(authCode)
                }
            }
        }
    }

    private fun exchangeCodeForAccessToken(authCode: String) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authCode)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url("https://www.linkedin.com/oauth/v2/accessToken")
            .post(requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LinkedInAuth", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()?.string()
                Log.d("LinkedInAuth", "Token Response: $responseBody")

                val json = JSONObject(responseBody ?: "{}")
                val accessToken = json.optString("access_token", "")
                if (accessToken.isNotEmpty()) {
                    getLinkedInProfile(accessToken)
                }

                val idToken = json.optString("id_token", "")
                if (idToken.isNotEmpty()) {
                    validateIDToken(idToken)
                }
            }
        })
    }

    private fun validateIDToken(idToken: String) {
        try {
            val jwt = JWT(idToken)

            val userId = jwt.getClaim("sub").asString()
            val email = jwt.getClaim("email").asString()
            val name = jwt.getClaim("name").asString()

            Log.d("LinkedInIDToken", "User ID: $userId")
            Log.d("LinkedInIDToken", "Email: $email")
            Log.d("LinkedInIDToken", "Name: $name")
        } catch (e: Exception) {
            Log.e("LinkedInIDToken", "Error decoding ID Token: ${e.message}")
        }
    }

    fun getLinkedInProfile(accessToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.linkedin.com/v2/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LinkedInProfile", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("LinkedInProfile", "Profile Data: ${response.body()?.string()}")
            }
        })
    }

    fun logout(view: View) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}