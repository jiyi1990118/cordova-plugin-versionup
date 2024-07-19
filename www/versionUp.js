var argscheck = require('cordova/argscheck');
var channel = require('cordova/channel');
var exec = require('cordova/exec');
var cordova = require('cordova');

exports.getVersionInfo = function (success, error) {
    exec(success, error, 'VersionUp', 'getVersionInfo', []);
};

// 检查是否有更新
/**
 * @param {Object} options
 * @param {Function} options.checkCallback 检查回调
 * @param {Function} options.progressCallback 下载进度回调
 * @param {Function} options.updateCallback 更新回调
 * @param {Function} options.installCallback 安装回调
 * @param {Boolean} options.isForceUpdate 是否强制更新
 * @param {Boolean} options.isForceInstall 是否强制安装
 */
exports.checkUpdate = function ({ checkCallback, progressCallback, updateCallback, installCallback, isForceUpdate, isForceInstall } = {}) {
    exec(
        (successInfo) => {
            switch (successInfo.cmd) {
                case 'checkUpdate':
                    typeof checkCallback == 'function' && checkCallback(successInfo);
                    break;
                case 'progress':
                    typeof progressCallback == 'function' && progressCallback(successInfo);
                    break;
                case 'update':
                    typeof updateCallback == 'function' && updateCallback(successInfo);
                    break;
                case 'install':
                    typeof installCallback == 'function' && installCallback(successInfo);
                    break;
                default:
                    console.warn('未知命令', successInfo);
                    break;
            }
        },
        (_error) => { },
        'VersionUp',
        'checkUpdate',
        [isForceUpdate, isForceInstall]
    );
}
