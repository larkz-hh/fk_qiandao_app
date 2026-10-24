package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme

@Composable
fun OnlineItem(
    user: ApiResponse.MemberData,
    isSelf: Boolean = false,
    onSignOut: () -> Unit,
    onComplaint: () -> Unit
) {
    var showComplaintDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val colors = KxTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //学号 + 姓名
        Column(modifier = Modifier.weight(5f)) {
            Text(
                text = user.userId.toString() ,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.userName.toString(),
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

        if (!isSelf) {
            Column(
                modifier = Modifier.weight(3.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 签退按钮
                Button(
                    onClick = { showSignOutDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).fillMaxWidth(0.9f)
                ) {
                    Text("签退", fontSize = 11.sp, color = colors.onSurface)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 举报按钮
                Button(
                    onClick = { showComplaintDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).fillMaxWidth(0.9f)
                ) {
                    Text("举报", fontSize = 11.sp, color = colors.onSurface)
                }
            }
        }
    }

    //举办对话框
    if (showComplaintDialog) {
        ComplaintDialog(
            user = user,
            onDismissText = { showComplaintDialog = false },
            onConfirm = {
                showComplaintDialog = false
                onComplaint()
            }
        )
    }
    if (showSignOutDialog) {
        SignOutDialog(
            user = user,
            onDismissText = { showSignOutDialog = false },
            onConfirm = {
                showSignOutDialog = false
                onSignOut() // 确定后执行签退回调
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
    val colors = KxTheme.colors
    Dialog(onDismissRequest = { onDismissText() }) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            color = colors.surface,
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
                    color = colors.onSurface
                    // 默认就是左对齐，不需要额外设置 textAlign
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${user.userId}\n${user.userName}",
                    color = colors.muted
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
                            containerColor = colors.danger,
                            contentColor = Color.White
                        )
                    ) {
                        Text("确定，它是虚空战神😄", color = Color.White)
                    }

                    // 取消按钮 (在下)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismissText() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurface
                        )
                    ) {
                        Text("取消,Ta正在上班🫡", color = colors.onSurface)
                    }
                }
            }
        }
    }
}
@Composable
fun SignOutDialog(
    user: ApiResponse.MemberData,
    onDismissText: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = KxTheme.colors
    Dialog(onDismissRequest = { onDismissText() }) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            color = colors.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "你们商量好了吗？确定要帮Ta签退么？未经允许给别人签退的人死全家😄",
                    fontSize = 20.sp,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${user.userId}\n${user.userName}",
                    color = colors.muted
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
                            containerColor = Color(0xFFF57C00)
                        )
                    ) {
                        Text("确定，帮Ta下班👍", color = Color.White)
                    }

                    // 取消按钮
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismissText() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurface
                        )
                    ) {
                        Text("哈哈，我手滑了😄", color = colors.onSurface)
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