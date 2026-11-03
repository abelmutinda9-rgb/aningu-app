package com.music.audify.ui.aningu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.music.audify.data.aningu.AninguApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PlanOption(
    val id: String,
    val name: String,
    val price: String,
    val description: String,
    val badge: String? = null
)

@Composable
fun AninguPaywallDialog(
    onDismiss: () -> Unit,
    onSubscribed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val plans = remember {
        listOf(
            PlanOption("student", "Student", "KSh 105 / mo", "1 account • Verified higher education status"),
            PlanOption("duo", "Duo", "KSh 275 / mo", "2 separate accounts • Best for couples or roommates"),
            PlanOption("family", "Family", "KSh 335 / mo", "Up to 6 separate accounts • Family member management", badge = "Best Value")
        )
    }

    var selectedPlan by remember { mutableStateOf("family") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isPolling by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unlock ANINGU Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your 72-hour free trial has ended. Select a plan to continue streaming without limits.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                plans.forEach { plan ->
                    val isSelected = selectedPlan == plan.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { if (!isLoading) selectedPlan = plan.id }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = plan.price,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = plan.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("M-Pesa Phone (e.g. 0712345678)") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (phoneNumber.trim().length < 9) {
                            statusMessage = "Please enter a valid M-Pesa phone number"
                            return@Button
                        }
                        isLoading = true
                        statusMessage = "Sending STK push to your phone..."
                        scope.launch {
                            val res = AninguApi.initiateStkPush(context, phoneNumber.trim(), selectedPlan)
                            if (!res.success || res.reference == null) {
                                isLoading = false
                                statusMessage = res.message ?: "Failed to initiate M-Pesa push."
                                return@launch
                            }

                            statusMessage = "Enter your M-Pesa PIN on your phone..."
                            isPolling = true
                            val ref = res.reference
                            var attempts = 0
                            var paid = false
                            while (attempts < 20 && !paid) {
                                delay(3000)
                                attempts++
                                val st = AninguApi.checkPaymentStatus(ref)
                                if (st.isTerminal) {
                                    if (st.success) {
                                        paid = true
                                        statusMessage = "Payment confirmed! Welcome to ANINGU Premium."
                                        delay(1500)
                                        isLoading = false
                                        isPolling = false
                                        onSubscribed()
                                        return@launch
                                    } else {
                                        statusMessage = "Payment not completed or cancelled."
                                        isLoading = false
                                        isPolling = false
                                        return@launch
                                    }
                                }
                            }
                            if (!paid) {
                                statusMessage = "Timed out waiting for confirmation. If you entered your PIN, it will activate shortly."
                                isLoading = false
                                isPolling = false
                            }
                        }
                    },
                    enabled = !isLoading && phoneNumber.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = if (isLoading) "Processing..." else "Pay with M-Pesa")
                }
            }
        }
    }
}
