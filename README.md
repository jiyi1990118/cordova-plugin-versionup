# cordova-plugin-versionup
用kotlin编写的cordova android版本的版本升级插件，（开发借鉴了cordova-hot-code-push-plugin思路）
可配合添加热更新工具 cordova-hot-code-push-cli 使用

## 支持平台
- Android 7.0.0 或更高版本。
- 后续将支持iOS、harmonyOS


### 安装

推荐使用 cordova 7.0+ 当前最新版本12.0.0

### 快速开始指南

1. 创建新的 Cordova 项目使用命令行界面并添加 iOS/Android 平台:

   ```sh
   cordova create TestProject com.example.testproject TestProject
   cd ./TestProject
   cordova platform add android
   cordova platform add ios
   ```
   或者使用现有的项目。
2. 添加插件:

   ```sh
   cordova plugin add cordova-plugin-versionup
   ```
   
3. 前端代码中调用插件方法

    
   ```javascript
    /**
     * 检查是否有更新
     * @param {Object} options
     * @param {Function} options.checkCallback 检查回调
     * @param {Function} options.progressCallback 下载进度回调
     * @param {Function} options.updateCallback 更新回调
     * @param {Function} options.installCallback 安装回调
     * @param {Boolean} options.isForceUpdate 是否强制更新
     * @param {Boolean} options.isForceInstall 是否强制安装
     */
     
    $VersionUp.checkUpdate({
        checkCallback(info){
            console.log(info)
        }, progressCallback(info){
            console.log(info)
        }, updateCallback(info){
            console.log(info)
        }, installCallback(info){
            console.log(info)
        },
        isForceUpdate:false,
        isForceInstall:false,
    })
    
    
    // 获取版本相关信息
    $VersionUp.getVersionInfo(versionInfo=>console.log(versionInfo),e=>console.error(e))
   ```



