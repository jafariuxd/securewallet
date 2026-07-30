package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.ui.theme.SophisticatedDarkBg
import com.example.ui.theme.SophisticatedSurface

data class IranianBank(
    val id: String,
    val name: String,
    val color: Color,
    val logoName: String?
)

val iranianBanks = listOf(
    IranianBank("melli", "بانک ملی ایران", Color(0xFFE2EBF4), "melli"),
    IranianBank("sepah", "بانک سپه", Color(0xFFF2D15C), "sepah"),
    IranianBank("saderat", "بانک صادرات", Color(0xFF0D2561), "saderat"),
    IranianBank("mellat", "بانک ملت", Color(0xFFE31837), "mellat"),
    IranianBank("tejarat", "بانک تجارت", Color(0xFF0F8CBB), "tejarat"),
    IranianBank("pasargad", "بانک پاسارگاد", Color(0xFFE8AB1C), "pasargad"),
    IranianBank("saman", "بانک سامان", Color(0xFF007DC4), "saman"),
    IranianBank("parsian", "بانک پارسیان", Color(0xFF8B0029), "parsian"),
    IranianBank("keshavarzi", "بانک کشاورزی", Color(0xFF338833), "keshavarzi"),
    IranianBank("refah", "بانک رفاه کارگران", Color(0xFF553285), "refahkargaran"),
    IranianBank("maskan", "بانک مسکن", Color(0xFFEA5B0C), "maskan"),
    IranianBank("ayandeh", "بانک آینده", Color(0xFF7E4A3B), "ayande"),
    IranianBank("eghtesad_novin", "بانک اقتصاد نوین", Color(0xFF4C3082), "eghtesad"),
    IranianBank("shahr", "بانک شهر", Color(0xFFD40B32), "shahr"),
    IranianBank("sina", "بانک سینا", Color(0xFF005DAA), "sina"),
    IranianBank("day", "بانک دی", Color(0xFFE12A34), "day"),
    IranianBank("sarmayeh", "بانک سرمایه", Color(0xFF41438D), "sarmaye"),
    IranianBank("karafarin", "بانک کارآفرین", Color(0xFF2A6E3B), "karafarin"),
    IranianBank("gardeshgari", "بانک گردشگری", Color(0xFFE31C23), "gardeshgari"),
    IranianBank("tosee_taavon", "بانک توسعه تعاون", Color(0xFF00539F), "tosetaavon"),
    IranianBank("post_bank", "پست بانک ایران", Color(0xFF007548), "post"),
    IranianBank("sanat_madan", "بانک صنعت و معدن", Color(0xFFE6A425), "sanatmadan"),
    IranianBank("tosee_saderat", "بانک توسعه صادرات", Color(0xFF1E3971), "tosesaderat"),
    IranianBank("khavarmianeh", "بانک خاورمیانه", Color(0xFF711F25), "khavarmianeh"),
    IranianBank("iranzamin", "بانک ایران زمین", Color(0xFFDA2032), "iranzamin"),
    IranianBank("resalat", "بانک قرض الحسنه رسالت", Color(0xFF005B82), "resalat"),
    IranianBank("mehr_iran", "بانک قرض الحسنه مهر ایران", Color(0xFF26733B), "mehriran"),
    IranianBank("blu_bank", "بلوبانک", Color(0xFF0066FF), "blu"),
    IranianBank("tobank", "توبانک", Color(0xFF00BFA5), null),
    IranianBank("viip", "ویپاد (پاسارگاد)", Color(0xFFE8AB1C), "pasargad"),
    IranianBank("abroad", "سایر...", Color(0xFF757575), null)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectorBottomSheet(
    onDismissRequest: () -> Unit,
    onBankSelected: (IranianBank) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredBanks = iranianBanks.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SophisticatedSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "انتخاب بانک",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجوی نام بانک...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(filteredBanks) { bank ->
                    BankItemRow(bank = bank) {
                        onBankSelected(bank)
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
fun BankItemRow(bank: IranianBank, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (bank.logoName != null) {
                val resId = context.resources.getIdentifier(bank.logoName, "raw", context.packageName)
                if (resId != 0) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resId)
                            .decoderFactory(SvgDecoder.Factory())
                            .crossfade(true)
                            .build(),
                        contentDescription = bank.name,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = bank.name,
                        tint = bank.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = bank.name,
                    tint = bank.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = bank.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

fun getBankLogoFromName(name: String): String? {
    return iranianBanks.find { it.name == name }?.logoName
}

fun getBankColorFromName(name: String): Color? {
    return iranianBanks.find { it.name == name }?.color
}
