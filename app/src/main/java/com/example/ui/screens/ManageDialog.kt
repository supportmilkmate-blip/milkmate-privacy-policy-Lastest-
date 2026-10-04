package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MilkMateViewModel

@Composable
fun ManageDialog(
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit,
    onOpenStaff: () -> Unit = {},
    onOpenPricing: () -> Unit = {},
    onOpenDeliverySettings: () -> Unit = {},
    onOpenExpenseCategories: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ManageHubScreen(
                viewModel = viewModel,
                onBack = onDismiss
            )
        }
    }
}
