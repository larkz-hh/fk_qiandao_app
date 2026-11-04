# <img src="app/src/main/res/drawable/logo.jpg" width="120" alt="logo" />   

# 三院科协签到 App

## 个人需求

- 帮别人签退时第二天自己签到时眼瞎又帮 Ta 签了n小时
- 上工太沉迷导致过点忘记签退了（已经隐藏了定时签退功能了...）

## 项目描述

这是参考三院科协签到 Android 端的改版 APP，基于 Jetpack Compose 重新构建，保留原 UI 风格和基本功能，增加了一些新功能并修复了如旋转后无法保留页面状态、签到时杀掉APP重新进入后页面显示未签到等若干问题。

悄悄告诉你，在不同页面点击顶部栏的科协 logo 会有不一样的效果~~~

目前已经完成的功能：

- 签到 / 签退，在线成员列表与搜索
- 签到时长排行
- 帮他人签退、举报
- 签到热力图、累计签到天数与时长
- 全部签到记录
- 知识库（科协飞书知识库文档）
- 校园跑（需连校园网）
- 深色模式

当前版本：**4.0.0**

后续：个人需求已解决，暂无后续想法，梦到什么加什么。

## 打包

```bash
./gradlew :app:assembleRelease
```

- release 已开启 
- ABI 拆分：`universal`和 `arm64-v8a`
- 签名信息从 `keystore.properties` 读取

## 如何成为开发者

步骤如下：

1、Fork本仓库

2、向仓库发送 Pull requests

3、提醒合并

4、成为开发者

## Bug反馈

- **已解决**：后端重复签到后名字等为 null——收到 -201，或 code 0 但 data 为空 / 缺少个人信息时，自动查 `/api/record/online/{uid}` 补齐姓名、周序、开始时间；只有这次签到的学号和本地记录的学号相同，才沿用本地记录的姓名，不用再签退重签。
- **已解决**：神人操作之在线成员列表里给自己签退 / 举报——自己那行不显示「签退 / 举报」，`signOutOther` 命中本人时也会重查状态。
- **部分解决**：神人操作之签到后杀进程、切流量、再换回科协网——不会重复签到（-201 兜底），但断网期间会显示「签到」和「状态未确认」，联网后自动纠正，也可双击 LOGO 刷新。
......

未测试功能：

- 举报功能

## 友情链接

 [原科协Android端签到APP](https://github.com/KWJYES/kexie_qiandao_AndroidApp)

 [签到系统后端](https://github.com/sanyuankexie/GCTAAttendanceSystem)