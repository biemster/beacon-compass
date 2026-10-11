package com.beaconcompass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.beaconcompass.ui.CompassScreen
import com.beaconcompass.ui.theme.MyApplicationTheme
import com.beaconcompass.viewmodel.CompassViewModel
import com.beaconcompass.viewmodel.CompassViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var compassViewModel: CompassViewModel

    // Automated trigger for Location permissions standard system check (essential for compass GPS operations)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // GPS permission results logged here; the ViewModel and SensorManager reactively trigger updates
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            if (::compassViewModel.isInitialized) {
                compassViewModel.startSensorTracking()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialise the ViewModel at activity level so the permission callback can access it
        compassViewModel = ViewModelProvider(
            this,
            CompassViewModelFactory(application)
        )[CompassViewModel::class.java]
        
        // Launch permission scanner immediately upon app start to let hunters lock coordinates fast
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            MyApplicationTheme {
                CompassScreen(viewModel = compassViewModel)
            }
        }
    }
}
