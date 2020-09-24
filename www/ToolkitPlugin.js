var exec = require("cordova/exec");

exports.requestInstallPermission = function (success, error) {
    exec(success, error, "ToolkitPlugin", "requestInstallPermission", null);
};

exports.checkNotifyPermission = function (success, error) {
    exec(success, error, "ToolkitPlugin", "checkNotifyPermission", null);
};
exports.requestNotifyPermission = function (success, error) {
    exec(success, error, "ToolkitPlugin", "requestNotifyPermission", null);
};

exports.requestPermissions = function (arg0, success, error) {
    exec(success, error, "ToolkitPlugin", "requestPermissions", [arg0]);
};

exports.openAppSettings = function (success, error) {
    exec(success, error, "ToolkitPlugin", "openAppSettings", null);
};

exports.getDeviceInfo = function (success, error) {
    exec(success, error, "ToolkitPlugin", "getDeviceInfo", null);
};
