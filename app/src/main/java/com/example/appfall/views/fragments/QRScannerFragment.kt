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
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.views.activities.ParametersActivity
import com.example.appfall.websockets.WebSocketManager

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
        // Initialiser les vues pour l'animation de chargement et les icônes
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

        viewModel.getLocalUser()
        // Observer les données de l'utilisateur local
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
                // Permission already granted
                setupScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show an explanation to the user why we need the permission
                //Toast.makeText(requireContext(), "Camera permission is needed to scan QR codes", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Request the permission
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
                // Hide the scanner view
                scannerView.visibility = View.INVISIBLE

                // Afficher l'animation de chargement
                loadingAnimation.visibility = View.VISIBLE

                if (token != null && name != null) {
                    Log.d("QRScannerFragment", "Sending WebSocket message with token=$token, name=$name")
                    WebSocketManager.sendMessage("connect:$token:$name:${it.text}")
                } else {
                    Log.e("QRScannerFragment", "Cannot send WebSocket message: token or name is null")
                }
            }
        }

        codeScanner.errorCallback = ErrorCallback {
            activity.runOnUiThread {
                Toast.makeText(activity, "Camera initialization error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        WebSocketManager.receivedMessage.observe(viewLifecycleOwner) { message ->
            println("WebSocket View: $message")
            // Masquer l'animation de chargement
            loadingAnimation.visibility = View.INVISIBLE

            val icon: ImageView
            if (message == "Connected successfully") {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                successIcon.visibility = View.VISIBLE
                icon = successIcon
                statusText.text = "Liaison établie avec succès"
                statusText.visibility = View.VISIBLE
                Log.d("received message", message)
            } else {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                failureIcon.visibility = View.VISIBLE
                icon = failureIcon
                statusText.text = "Échec de la connexion"
                statusText.visibility = View.VISIBLE
                Log.d("received message", message)
            }

            // Hide the icon and show the scanner view again after 3 seconds
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
        // Handle the case when the camera permission is denied
        val scannerView = view?.findViewById<CodeScannerView>(R.id.scanner_view)
        scannerView?.visibility = View.INVISIBLE
        Toast.makeText(requireContext(), "La permission de la caméra a été refusée. Le scan des codes QR est désactivée.", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Réinitialiser les vues à chaque reprise
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
}
