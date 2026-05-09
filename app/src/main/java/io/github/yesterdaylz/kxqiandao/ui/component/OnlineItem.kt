package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.yesterdaylz.kxqiandao.data.model.ApiResponse

@Composable
fun OnlineItem(
    user: ApiResponse.MemberData,
    onComplaint: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //学号 + 姓名
        Column(modifier = Modifier.weight(5f)) {
            Text(
                text = user.userId.toString() ?: "",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.userName.toString() ?: "",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        //部门 + 位置
        Column(modifier = Modifier.weight(4f)) {
            Text(
                text = user.userDept ?: "",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.userLocation ?: "",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        //举办
        Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.Center) {
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("举报", fontSize = 12.sp, color = Color.White)
            }
        }
    }
    //举办对话框
    if (showDialog) {
        ComplaintDialog(
            user = user,
            onDismissText = { showDialog = false },
            onConfirm = {
                showDialog = false
                onComplaint()
            }
        )
    }
}
@Composable
fun ComplaintDialog(
    user: ApiResponse.MemberData,
    onDismissText: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissText() }) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "确定要举报Ta么",
                    fontSize = 20.sp,
                    color = Color.Black
                    // 默认就是左对齐，不需要额外设置 textAlign
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${user.userId}\n${user.userName}",
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onConfirm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("确定，它是虚空战神😄", color = Color.White)
                    }

                    // 取消按钮 (在下)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismissText() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEEEEEE)
                        )
                    ) {
                        Text("取消,Ta正在上班🫡", color = Color.Black)
                    }
                }
            }
        }
    }
}
@Preview
@Composable
fun PreviewAlertDialog(){
    val fakeUser = ApiResponse.MemberData(
        userId = 114514,
        userName = "lzz",
        userDept = "摸鱼部",
        userLocation = "5108",
        status = 1
    )

    ComplaintDialog(
        user = fakeUser,
        onDismissText = {},
        onConfirm = {}
    )
}