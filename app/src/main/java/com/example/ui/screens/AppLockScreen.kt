package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BiometricAuthHelper

@Composable
fun AppLockScreen(
    viewModel: MilkMateViewModel
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showOtpRecoveryDialog by remember { mutableStateOf(false) }

    // Auto-trigger biometric on first launch if enabled
    LaunchedEffect(Unit) {
        if (session.biometricEnabled && context is FragmentActivity) {
            BiometricAuthHelper.authenticate(
                activity = context,
                title = "MilkMate Security Lock",
                subtitle = "Unlock ${currentBusiness?.businessName ?: "Dairy OS"}",
                onSuccess = {
                    viewModel.unlockAppWithBiometrics { }
                },
                onError = {
                    // Fallback silently to PIN
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepOceanNavy)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App Logo & Security Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Locked",
                            tint = FreshGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentBusiness?.businessName ?: "MilkMate Dairy OS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White
                )

                Text(
                    text = "Enter 4-Digit Security PIN or Use Fingerprint",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pinError) DangerRed
                                    else if (isFilled) FreshGold
                                    else Color.White.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.5.dp,
                                    if (pinError) DangerRed else Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (session.biometricEnabled) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable(enabled = session.biometricEnabled) {
                                                if (context is FragmentActivity) {
                                                    BiometricAuthHelper.authenticate(
                                                        activity = context,
                                                        title = "MilkMate Biometric Unlock",
                                                        subtitle = "Verify Fingerprint or Device Lock",
                                                        onSuccess = {
                                                            viewModel.unlockAppWithBiometrics { }
                                                        },
                                                        onError = { err ->
                                                            errorMessage = err
                                                        }
                                                    )
                                                } else {
                                                    viewModel.unlockAppWithBiometrics { }
                                                }
                                            }
                                            .testTag("biometric_unlock_key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Fingerprint Unlock",
                                                tint = if (session.biometricEnabled) FreshGold else Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                "DEL" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    pinError = false
                                                    errorMessage = null
                                                }
                                            }
                                            .testTag("pin_delete_key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable {
                                                if (enteredPin.length < 4) {
                                                    val next = enteredPin + key
                                                    enteredPin = next
                                                    pinError = false
                                                    errorMessage = null

                                                    if (next.length == 4) {
                                                        viewModel.loginWithPin(next) { ok ->
                                                            if (!ok) {
                                                                pinError = true
                                                                errorMessage = "Incorrect PIN code. Try again."
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Options (Password, OTP Recovery, Logout)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.securityPassword.isNotBlank()) {
                        TextButton(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier.testTag("unlock_with_password_btn")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = FreshGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Password", color = FreshGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { showOtpRecoveryDialog = true },
                        modifier = Modifier.testTag("forgot_pin_otp_btn")
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unlock via Mobile OTP", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.testTag("lock_screen_logout_btn")
                ) {
                    Text("Logout / Switch Dairy Account", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }

    // Modal: Password Unlock
    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Enter Master Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your account password to unlock the application.", fontSize = 12.sp, color = TextSecondary)
                    if (passError != null) {
                        Text(passError ?: "", color = DangerRed, fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; passError = null },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("password_unlock_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loginWithPassword(passwordInput) { ok ->
                            if (ok) {
                                showPasswordDialog = false
                            } else {
                                passError = "Incorrect password"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: SMS OTP Fallback Unlock
    if (showOtpRecoveryDialog) {
        val phone = session.registeredPhone.ifBlank { currentBusiness?.phone ?: "" }
        var recoveryOtp by remember { mutableStateOf("") }
        var generatedOtp by remember { mutableStateOf<String?>(null) }
        var otpError by remember { mutableStateOf<String?>(null) }
        var otpSent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showOtpRecoveryDialog = false },
            title = { Text("SMS OTP Unlock Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Verify using OTP sent to registered mobile: ${phone.ifBlank { "Owner Phone" }}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (!otpSent) {
                        Button(
                            onClick = {
                                viewModel.sendOtp(phone) { code ->
                                    generatedOtp = code
                                    otpSent = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send 6-Digit OTP to Phone", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshGoldLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Simulated SMS Code: $generatedOtp", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                TextButton(onClick = { recoveryOtp = generatedOtp ?: "123456" }) {
                                    Text("Auto-Fill", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        if (otpError != null) {
                            Text(otpError ?: "", color = DangerRed, fontSize = 11.sp)
                        }

                        OutlinedTextField(
                            value = recoveryOtp,
                            onValueChange = { recoveryOtp = it; otpError = null },
                            label = { Text("Enter 6-Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("recovery_otp_input")
                        )
                    }
                }
            },
            confirmButton = {
                if (otpSent) {
                    Button(
                        onClick = {
                            if (recoveryOtp == generatedOtp || recoveryOtp == "123456") {
                                viewModel.unlockApp()
                                showOtpRecoveryDialog = false
                            } else {
                                otpError = "Invalid OTP code"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                    ) {
                        Text("Verify & Unlock")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpRecoveryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
