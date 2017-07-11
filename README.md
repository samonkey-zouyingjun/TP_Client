# TP_Client

基于wifi direct点对点传输技术的实现局域网内推屏 

## commit记录
- 6-28 实现了客户端和服务端一体的照片推送功能
- 7-4  录制视频到本地mp4
- 7-5  录制视频到本地zyj.264
- 7-5  wifi p2p 推送到另一终端并保存本地zyj.264
- 7-6  使用原生硬件解码播放
- 7-10 用原生的硬件解码和硬件有关，后改为ffmpeg解码方式可以实现较好的解码效果

  ffmpeg解码的NDK编程：http://blog.csdn.net/lidec/article/details/72934405



##注意
 wifi direct 只能是groupClient推送到groupOwner,开发者可以建议设备做GroupOwner;可以在连接时设置wifiP2pConfig.groupOwnerIntent=15 建议设置成GroupOwner。