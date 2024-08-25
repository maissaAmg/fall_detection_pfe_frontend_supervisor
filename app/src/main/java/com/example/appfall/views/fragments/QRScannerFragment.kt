package com.example.appfall.views.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.appfall.R
import com.example.appfall.data.models.TopicSubscription
import com.example.appfall.viewModels.NotificationsViewModel
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.views.activities.ParametersActivity
import com.example.appfall.websockets.WebSocketManager
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.OkHttpClient

class QRScannerFragment : Fragment() {
    private lateinit var codeScanner: CodeScanner
    private lateinit var loadingAnimation: ProgressBar
    private lateinit var successIcon: ImageView
    private lateinit var failureIcon: ImageView
    private lateinit var statusText: TextView
    private var token: String? = null
    private var phone: String? = null
    private var name: String? = null
    private val viewModel: UserViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private lateinit var fcmToken: String
    private lateinit var fcmTopic: String

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                setupScanner()
            } else {
                handlePermissionDenied()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_qr_scanner, container, false)
        loadingAnimation = view.findViewById(R.id.loadingAnimation)
        successIcon = view.findViewById(R.id.successIcon)
        failureIcon = view.findViewById(R.id.failureIcon)
        statusText = view.findViewById(R.id.statusText)

        val settingsIcon: ImageView = view.findViewById(R.id.ic_settings)
        settingsIcon.setOnClickListener {
            val intent = Intent(requireContext(), ParametersActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Code for testing ****************************
//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val fcmToken = task.result
//                Log.d("FCM Token", "Token: $fcmToken")
//                subscribeToTopic("test")
//                subscribeToTopic("news")
//                sendSubscriptionRequest(fcmToken, "test1")
//            } else {
//                Log.e("FCM Token", "Failed to retrieve token: ${task.exception?.message}")
//            }
//        }
        //Code for testing ****************************

        viewModel.getLocalUser()
        viewModel.localUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                phone = user.phone
                token = user.token
                name = user.name
                Log.d("QRScannerFragment", "User data loaded: phone=$phone, token=$token, name=$name")

            } else {
                Log.e("QRScannerFragment", "User data is null")
            }
        }

        WebSocketManager.connectWebSocket()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                setupScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupScanner() {
        val scannerView = view?.findViewById<CodeScannerView>(R.id.scanner_view) ?: return
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {
            activity.runOnUiThread {
                scannerView.visibility = View.INVISIBLE
                loadingAnimation.visibility = View.VISIBLE

                if (token != null && name != null) {
                    Log.d("QRScannerFragment", "Sending WebSocket message with token=$token, name=$name")
                    Log.d("QRScannerFragmentTest", it.text)
                    WebSocketManager.sendMessage("connect:$token:$name:${it.text}")

                    val (beforeDelimiter, afterDelimiter) = splitTextByDelimiter(it.text, ":")
                    println("Before: $beforeDelimiter")
                    println("After: $afterDelimiter")

                    fcmTopic = beforeDelimiter

                    Log.d("QRScannerFragment", "WebSocket message sent")

                } else {
                    Log.e("QRScannerFragment", "Cannot send WebSocket message or API request: token or name is null")
                }
            }
        }

        codeScanner.errorCallback = ErrorCallback {
            activity.runOnUiThread {
                Toast.makeText(activity, "Camera initialization error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        WebSocketManager.receivedMessage.observe(viewLifecycleOwner) { message ->
            Log.d("WebSocket Message", message)
            loadingAnimation.visibility = View.INVISIBLE

            val icon: ImageView
            if (message == "Connected successfully") {
                Log.d("QRScannerFragment", "Connection successful")
                successIcon.visibility = View.VISIBLE
                icon = successIcon
                statusText.text = "Liaison établie avec succès"

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        fcmToken = task.result
                        Log.d("FCM Token", "Token: $fcmToken")
                        //Using firebase built in method
                        subscribeToTopic(fcmTopic)
                        //Using our own method
                        sendSubscriptionRequest(fcmToken, fcmTopic)
                    } else {
                        Log.e("FCM Token", "Failed to retrieve token: ${task.exception?.message}")
                    }
                }
            } else {
                Log.d("QRScannerFragment", "Connection failed: $message")
                failureIcon.visibility = View.VISIBLE
                icon = failureIcon
                statusText.text = "Échec de la connexion"
            }
            statusText.visibility = View.VISIBLE

            Handler().postDelayed({
                icon.visibility = View.INVISIBLE
                statusText.visibility = View.INVISIBLE
                scannerView.visibility = View.VISIBLE
                codeScanner.startPreview()
            }, 3000)
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    private fun handlePermissionDenied() {
        val scannerView = view?.findViewById<CodeScannerView>(R.id.scanner_view)
        scannerView?.visibility = View.INVISIBLE
        Toast.makeText(requireContext(), "La permission de la caméra a été refusée. Le scan des codes QR est désactivée.", Toast.LENGTH_LONG).show()
    }

    private fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Subscribed to topic $topic"
                } else {
                    "Subscription to $topic failed"
                }
                Log.d("FCM Subscription", msg)
            }
    }

    private fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Unsubscribed from topic $topic"
                } else {
                    "Unsubscription from $topic failed"
                }
                Log.d("FCM Unsubscription", msg)
            }
    }

    private fun sendSubscriptionRequest(token: String, topic: String) {
        val subscriptionRequest = TopicSubscription(token, topic)
        notificationsViewModel.subscribeToTopic(subscriptionRequest)
        notificationsViewModel.subscriptionResponse.observe(viewLifecycleOwner) { response ->
            Log.d("FCM Subscription", "Response: $response")
        }
    }

    override fun onResume() {
        super.onResume()
        resetViews()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }

    private fun resetViews() {
        loadingAnimation.visibility = View.INVISIBLE
        successIcon.visibility = View.INVISIBLE
        failureIcon.visibility = View.INVISIBLE
        statusText.visibility = View.INVISIBLE
        view?.findViewById<CodeScannerView>(R.id.scanner_view)?.visibility = View.VISIBLE
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }

    private fun splitTextByDelimiter(text: String, delimiter: String): Pair<String, String> {
        val parts = text.split(delimiter, limit = 2)
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            text to ""  // or handle as needed
        }
    }
}