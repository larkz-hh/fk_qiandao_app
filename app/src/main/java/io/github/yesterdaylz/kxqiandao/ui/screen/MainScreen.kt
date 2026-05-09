package io.github.yesterdaylz.kxqiandao.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.yesterdaylz.kxqiandao.ui.theme.BackgroundGray
import io.github.yesterdaylz.kxqiandao.ui.theme.DividerGray
import io.github.yesterdaylz.kxqiandao.ui.theme.GreenOnline
import io.github.yesterdaylz.kxqiandao.ui.theme.RedOffline
import io.github.yesterdaylz.kxqiandao.ui.theme.TextWhite
import io.github.yesterdaylz.kxqiandao.viewmodel.MainViewModel
import io.github.yesterdaylz.kxqiandao.R
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse
import io.github.yesterdaylz.kxqiandao.data.model.TopFiveItem
import io.github.yesterdaylz.kxqiandao.ui.component.OnlineItem
import io.github.yesterdaylz.kxqiandao.ui.component.RankingItem
import io.github.yesterdaylz.kxqiandao.ui.theme.Blue1
import kotlin.collections.lastIndex


@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uid by viewModel.uid.collectAsStateWithLifecycle()
    val uname by viewModel.uname.collectAsStateWithLifecycle()
    val time by viewModel.time.collectAsStateWithLifecycle()
    val totalTime by viewModel.totalTime.collectAsStateWithLifecycle()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val filteredUsers by viewModel.filteredOnlineUsers.collectAsStateWithLifecycle()
    val topFiveList by viewModel.topFiveList.collectAsStateWithLifecycle()
    ///收集 Toast 事件
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = R.drawable.logo,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("三院科协", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            NavigationBar (containerColor = Color(0xFFF6F6F6)){
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.HowToReg, contentDescription = "签到") },
                    label = { Text("签到") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.fetchTopFive() //切换到该页面时刷新数据
                    },
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "排名") },
                    label = { Text("排名") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> CheckInTab(
                padding = padding,
                uid = uid,
                uname = uname,
                time = time,
                totalTime = totalTime,
                week = week,
                online = online,
                searchText = searchText,
                filteredUsers = filteredUsers,
                viewModel = viewModel
            )
            1 -> RankingTab(padding = padding, topFiveList = topFiveList)
        }
    }
}

@Composable
private fun CheckInTab(
    padding: PaddingValues,
    uid: String,
    uname: String,
    time: String,
    totalTime: String,
    week: String,
    online: Boolean,
    searchText: String,
    filteredUsers: List<ApiResponse.MemberData>,
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
            value = if (uid == "null") "" else uid,
            onValueChange = { viewModel.onUidChanged(it) },
            label = { Text("学号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnlineStatusCard(
            uid = uid,
            uname = uname,
            time = time,
            totalTime = totalTime,
            week = week,
            online = online
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.signInOrOut() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (online) RedOffline else Blue1
            )
        ) {
            Text(
                text = if (online) "签退" else "签到",
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (online) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "当前在教室的人数：${filteredUsers.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchText,
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
                        filteredUsers.forEach { user ->
                            OnlineItem(
                                user = user,
                                onComplaint = { viewModel.complaint(user) }
                            )
                            HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        }
                    }
                }

//                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
//                        items(filteredUsers, key = { it.userId.toString() ?: "" }) { user ->
//                            OnlineItem(
//                                user = user,
//                                onComplaint = {viewModel.complaint(user) }
//                            )
//                            HorizontalDivider(color = DividerGray, thickness = 1.dp)
//                        }
//                    }
//                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RankingTab(padding: PaddingValues, topFiveList: List<TopFiveItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🏆 本周卷王榜 ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                itemsIndexed(topFiveList) { index, item ->
                    RankingItem(rank = index + 1, item = item)
                    if (index < topFiveList.lastIndex) {
                        HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    }
                }
            }
        }
    }
}


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
            containerColor = if (online) GreenOnline else RedOffline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val textColor = if (online) TextWhite else TextWhite
            InfoRow("学号", uid, textColor)
            Spacer(modifier = Modifier.height(3.dp))
            InfoRow("姓名", uname, textColor)
            Spacer(modifier = Modifier.height(3.dp))
            Text(time, color = textColor,fontSize = 15.sp)//偷个懒，后期再考虑判断逻辑
            Spacer(modifier = Modifier.height(3.dp))
            InfoRow("本周签到总时长" ,totalTime, textColor)
            Spacer(modifier = Modifier.height(3.dp))
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