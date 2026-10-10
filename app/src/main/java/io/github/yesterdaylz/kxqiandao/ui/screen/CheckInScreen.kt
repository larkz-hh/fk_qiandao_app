package io.github.yesterdaylz.kxqiandao.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.ui.component.OnlineItem
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue1
import io.github.yesterdaylz.kxqiandao.ui.theme.DividerGray
import io.github.yesterdaylz.kxqiandao.ui.theme.GrayOffline
import io.github.yesterdaylz.kxqiandao.ui.theme.GreenOnline
import io.github.yesterdaylz.kxqiandao.ui.theme.TextBlack
import io.github.yesterdaylz.kxqiandao.ui.theme.TextWhite
import io.github.yesterdaylz.kxqiandao.viewmodel.MainUiState
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel

/**
 * 签到页
 */
@Composable
fun CheckInTab(
    padding: PaddingValues,
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = if (uiState.uid == "null") "" else uiState.uid,
            onValueChange = { viewModel.onUidChanged(it) },
            label = { Text("学号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnlineStatusCard(
            uid = uiState.uid,
            uname = uiState.uname,
            time = uiState.time,
            totalTime = uiState.totalTime,
            week = uiState.week,
            online = uiState.online
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.signInOrOut() },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.online) Color.White else Blue1
            )
        ) {
            Text(
                text = if (uiState.online) "签退" else "签到",
                fontSize = 18.sp,
                color = if (uiState.online) Color.Red else Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.online) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "当前在教室的人数：${uiState.filteredUsers.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.searchText,
                        onValueChange = { viewModel.onSearchTextChanged(it) },
                        placeholder = { Text("搜索") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        uiState.filteredUsers.forEach { user ->
                            OnlineItem(
                                user = user,
                                isSelf = uiState.selfUid.isNotEmpty() &&
                                        user.userId?.toString() == uiState.selfUid,
                                onSignOut = { viewModel.signOutOther(user.userId.toString()) },
                                onComplaint = { viewModel.complaint(user) }
                            )
                            HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** 在线状态卡片 */
@Composable
private fun OnlineStatusCard(
    uid: String,
    uname: String,
    time: String,
    totalTime: String,
    week: String,
    online: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (online) GreenOnline else GrayOffline
        )
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            val textColor = if (online) TextBlack else TextWhite
            InfoRow("学号", uid, textColor)
            Spacer(modifier = Modifier.height(5.dp))
            InfoRow("姓名", uname, textColor)
            Spacer(modifier = Modifier.height(5.dp))
            Text(time, color = textColor,fontSize = 15.sp)//偷个懒，后期再考虑判断逻辑
            Spacer(modifier = Modifier.height(5.dp))
            InfoRow("本周签到总时长" ,totalTime, textColor)
            Spacer(modifier = Modifier.height(5.dp))
            InfoRow("周序", week, textColor)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color) {
    Row {
        Text("$label: ", color = color)
        Text(value, color = color,fontSize = 16.sp)
    }
}
