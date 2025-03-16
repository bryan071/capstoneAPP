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

fun Privacy(
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
                Icon(imageVector = Icons.Default.Info, contentDescription = "Data Privacy")
                Text(text = "REPUBLIC ACT NO. 10173DATA PRIVACY ACT OF 2012", fontSize = 23.sp, fontWeight = FontWeight.Bold)

            }

        },
        text = {
            Column (modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ){
                Text(modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = "AN ACT PROTECTING INDIVIDUAL PERSONAL INFORMATION AND COMMUNICATIONS SYSTEMS IN THE GOVERNMENT AND THE PRIVATE SECTOR, CREATING FOR THIS PURPOSE A NATIONAL PRIVACY COMMISSION, AND FOR OTHER PURPOSES\n" +
                        "\n" +
                        "Section 1. Short Title.\n" +
                        "This Act shall be known as the \"Data Privacy Act of 2012.\"\n" +
                        "\n" +
                        "Section 2. Declaration of Policy.\n" +
                        "It is the policy of the State to protect the fundamental human right of privacy while ensuring free flow of information to promote innovation and growth. The State recognizes the vital role of information and communications technology in nation-building and its obligation to ensure that personal information is secured and protected.\n" +
                        "\n" +
                        "Section 3. Definition of Terms.\n" +
                        "For purposes of this Act:\n" +
                        "(a) \"Personal information\" refers to any information that identifies an individual.\n" +
                        "(b) \"Sensitive personal information\" includes information about race, health, education, genetic or sexual life, proceedings for any offense, government-issued identification numbers, and financial information.\n" +
                        "(c) \"Processing\" refers to any operation performed on personal data including collection, recording, organization, structuring, storage, adaptation, retrieval, consultation, use, disclosure, dissemination, alignment, combination, restriction, erasure, or destruction.\n" +
                        "(d) \"Data subject\" refers to an individual whose personal, sensitive, or privileged information is processed.\n" +
                        "\n" +
                        "Section 4. Scope.\n" +
                        "This Act applies to the processing of personal data by both government and private institutions. However, it does not apply to personal, family, or household activities, nor to information used for journalistic, artistic, literary, or research purposes.\n" +
                        "\n" +
                        "Section 5. National Privacy Commission.\n" +
                        "A National Privacy Commission (NPC) shall be created to monitor and ensure compliance with this Act.\n" +
                        "\n" +
                        "Section 6. Principles of Data Privacy.\n" +
                        "All personal data must be:\n" +
                        "(a) Collected for a specified and legitimate purpose;\n" +
                        "(b) Processed fairly and lawfully;\n" +
                        "(c) Adequate, relevant, and not excessive;\n" +
                        "(d) Retained only as necessary for fulfillment of the purpose;\n" +
                        "(e) Secured against unauthorized access, loss, destruction, or alteration.\n" +
                        "\n" +
                        "Section 7. Rights of Data Subjects.\n" +
                        "Every data subject has the right to:\n" +
                        "(a) Be informed that their personal data is being collected and processed;\n" +
                        "(b) Access their personal data upon request;\n" +
                        "(c) Rectify inaccurate or incomplete data;\n" +
                        "(d) Object to data processing;\n" +
                        "(e) Erasure or blocking of unlawful processing;\n" +
                        "(f) Lodge a complaint for violations of their data privacy rights.\n" +
                        "\n" +
                        "Section 8. Security of Personal Information.\n" +
                        "Data controllers must implement appropriate security measures to protect personal data from unauthorized access, destruction, alteration, or disclosure.\n" +
                        "\n" +
                        "Section 9. Penalties.\n" +
                        "Violations of this Act, including unauthorized processing, improper disposal, and unauthorized access of personal information, shall be punishable with fines and imprisonment, depending on the severity of the offense.\n" +
                        "\n" +
                        "Section 10. Effectivity.\n" +
                        "This Act shall take effect fifteen (15) days after its publication in the Official Gazette or in at least two newspapers of general circulation.\n" +
                        "\n")
            }

        }
    )
}