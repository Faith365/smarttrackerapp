package com.smarttracker.app.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ShowBackupCodeDialog(backupCode: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Backup Code", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text("Your backup code is: $backupCode\n\nPlease save this code securely. You’ll need it to recover your account. \n\n 💡HINT* It is your FIRST FIVE digits of your Phone NUmber you provided.")
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
