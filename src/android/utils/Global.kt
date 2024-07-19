package versionUp.utils

import androidx.appcompat.app.AppCompatActivity
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView

object Global {

    // cordova 插件实例
    lateinit var cordovaPlugin: CordovaPlugin

    // cordova 对象
    lateinit var cordova: CordovaInterface

    // activity 上下文对象
    lateinit var context: AppCompatActivity;

    lateinit var webView: CordovaWebView;

    // 进行初始化数据
    fun init(cordovaPlugin: CordovaPlugin) {
        // 判断变量是否初始化
        if (!this::cordova.isInitialized) {
            this.cordovaPlugin = cordovaPlugin;
            this.cordova = cordovaPlugin.cordova;
            this.webView = cordovaPlugin.webView;
            this.context = cordovaPlugin.cordova.activity;
        }
    }

}