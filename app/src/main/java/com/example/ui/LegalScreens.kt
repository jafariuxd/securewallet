package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.ui.theme.SophisticatedDarkBg
import com.example.ui.theme.SophisticatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(viewModel: WalletViewModel) {
    BackHandler {
        viewModel.navigateTo(AppScreen.SETTINGS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حریم خصوصی", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "سیاست حفظ حریم خصوصی",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "۱. ذخیره‌سازی آفلاین و محلی\n" +
                                "این اپلیکیشن کاملاً آفلاین کار می‌کند و هیچ‌گونه داده شخصی، مدارک هویتی، رمزها یا اطلاعات بانکی شما را به هیچ سروری ارسال نمی‌کند. تمامی اطلاعات صرفاً در حافظه داخلی دستگاه شما و به صورت رمزنگاری‌شده نگهداری می‌شوند.\n\n" +
                                "۲. امنیت و رمزنگاری\n" +
                                "ما از استانداردهای پیشرفته رمزنگاری برای محافظت از داده‌های شما استفاده می‌کنیم. دسترسی به اپلیکیشن تنها از طریق پین‌کد اختصاصی شما یا احراز هویت بیومتریک (اثر انگشت/تشخیص چهره) دستگاه شما امکان‌پذیر است.\n\n" +
                                "۳. دسترسی‌ها\n" +
                                "این برنامه هیچ دسترسی غیرضروری به امکانات دستگاه شما مانند دوربین، موقعیت مکانی، یا مخاطبین ندارد، مگر مواردی که کاربر صراحتاً برای انتخاب تصویر مدارک از گالری نیاز داشته باشد.\n\n" +
                                "۴. تغییرات در سیاست حریم خصوصی\n" +
                                "در صورت هرگونه تغییر در سیاست حفظ حریم خصوصی، این تغییرات در همین صفحه به اطلاع شما خواهد رسید.\n\n" +
                                "با استفاده از این اپلیکیشن، شما موافقت خود را با این سیاست حفظ حریم خصوصی اعلام می‌کنید.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(viewModel: WalletViewModel) {
    BackHandler {
        viewModel.navigateTo(AppScreen.SETTINGS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قوانین و مقررات", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "قوانین و شرایط استفاده",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "۱. پذیرش شرایط\n" +
                                "کاربر گرامی، استفاده شما از این اپلیکیشن به معنای آگاهی و پذیرش کامل قوانین و مقررات زیر می‌باشد.\n\n" +
                                "۲. مسئولیت حفظ رمز عبور\n" +
                                "شما شخصاً مسئول حفظ امنیت پین‌کد ورود به برنامه هستید. از آنجا که اطلاعات به‌صورت رمزنگاری‌شده و صرفاً در دستگاه شما ذخیره می‌شوند، در صورت فراموشی پین‌کد، امکان بازیابی اطلاعات از سوی ما وجود ندارد.\n\n" +
                                "۳. استفاده قانونی\n" +
                                "شما متعهد می‌شوید که از این برنامه تنها برای مقاصد قانونی استفاده کنید و از ذخیره‌سازی اطلاعات مرتبط با فعالیت‌های غیرمجاز خودداری نمایید.\n\n" +
                                "۴. عدم تضمین ۱۰۰ درصدی سخت‌افزاری\n" +
                                "اگرچه برنامه با بالاترین استانداردهای نرم‌افزاری محافظت می‌شود، اما امنیت فیزیکی دستگاه و عدم نصب برنامه‌های مخرب بر عهده کاربر است. ما هیچ‌گونه مسئولیتی در قبال سرقت دستگاه یا هک شدن گوشی شما توسط بدافزارها نداریم.\n\n" +
                                "۵. به‌روزرسانی‌ها\n" +
                                "ما حق تغییر در این قوانین را در هر زمان برای خود محفوظ می‌دانیم و استفاده مستمر شما از اپلیکیشن به منزله پذیرش تغییرات است.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}
