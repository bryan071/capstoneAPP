package com.project.webapp.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable

fun Terms(
    onDismiss:()->Unit
){

    androidx.compose.material3.AlertDialog(

        onDismissRequest = onDismiss,
        confirmButton = {}, // No confirm button
        dismissButton = { // Add dismiss button
            Button(modifier = Modifier.height(35.dp), onClick = onDismiss) {
                Text(text = "Close", fontSize = 13.sp)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically){
                Icon(imageVector = Icons.Default.Info, contentDescription = "Terms and Conditions")
                Text(text = "Terms And Conditions", fontSize = 23.sp, fontWeight = FontWeight.Bold)

            }

        },
        text = {
            Column (modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ){
                Text(modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = "1. Introduction\n" +
                            "Welcome to FarmAid. By downloading, accessing, or using our app, you agree to be bound by these Terms and Conditions. If you do not agree, please do not use the app.\n" +
                            "\n" +
                            "2. User Accounts\n" +
                            "\n" +
                            "You must provide accurate and complete information when creating an account.\n" +
                            "\n" +
                            "You are responsible for maintaining the confidentiality of your account credentials.\n" +
                            "\n" +
                            "We reserve the right to suspend or terminate accounts that violate these terms.\n" +
                            "\n" +
                            "3. Privacy Policy\n" +
                            "Your use of the app is also governed by our Privacy Policy, which outlines how we collect, use, and protect your personal data.\n" +
                            "\n" +
                            "4. Acceptable Use\n" +
                            "You agree not to:\n" +
                            "\n" +
                            "Use the app for unlawful or fraudulent activities.\n" +
                            "\n" +
                            "Upload or share harmful, offensive, or inappropriate content.\n" +
                            "\n" +
                            "Attempt to hack, modify, or interfere with the app’s security features.\n" +
                            "\n" +
                            "5. Intellectual Property\n" +
                            "All content, trademarks, and intellectual property in the app belong to [Company Name]. You may not use or reproduce them without our permission.\n" +
                            "\n" +
                            "6. Limitation of Liability\n" +
                            "We are not liable for any indirect, incidental, or consequential damages arising from the use of our app. Your use is at your own risk.\n" +
                            "\n" +
                            "7. Modifications to Terms\n" +
                            "We reserve the right to update these Terms and Conditions at any time. Continued use of the app after changes indicates acceptance.\n" +
                            "\n" +
                            "8. Contact Information\n" +
                            "For any questions about these Terms and Conditions, please contact us at [Contact Information].\n" +
                            "\n" +
                            "By using the app, you acknowledge that you have read, understood, and agreed to these Terms and Conditions.\n" +
                            "\n"
                )
            }

        }




    )
}