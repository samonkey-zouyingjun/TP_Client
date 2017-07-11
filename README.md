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


##坑
- WifiDirect是区分客户端和服务端的，虽然可以通过设置config建议但是遇到类似机顶盒这样配件较为低的移动端，会默认做客户端，
其次ServerSocket一定要先启动，如Server端做录制端，Client做投屏端，就必须先打开录制端的录制按钮，再打开Client的播放按钮。
