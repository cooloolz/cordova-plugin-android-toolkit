# 使用方法
### 首先在文件中声明插件
declare let ToolkitPlugin: any;

### 获取设备信息

ToolkitPlugin && ToolkitPlugin.getDeviceInfo((pluginResult: any[]) => {
      console.log("成功：" + pluginResult[0])
    }, (error) => {
      console.log("失败：" + error)
});

#### 成功的返回值说明：
deviceInfo: {
    "uuid" : string; // 设备唯一码
    "imei" : string; // 卡槽1串号
    "platform" : string; // "Android" 平台版本
    "sdk" : string; // 系统SDK版本
    "version" : string; // android版本
    "brand" : string; // 手机品牌
    "model" : string; // 手机型号
    "manufacturer" : string; // 手机厂商
    "isVirtual" : string; // 是否为虚拟设备
}

#### 失败的返回值：
无


### 检查应用安装权限

WeichaiServicePlugin && WeichaiServicePlugin.checkInstallPermission(() => {
      console.log("成功：" + canInstall)
    });

  1. 一直打开安装权限授权界面，直到用户授权：


### 检查应用通知权限

WeichaiServicePlugin && WeichaiServicePlugin.checkNotifyPermission(() => {
      console.log("成功：" + canNotify)
    });
    
  1. 一直打开通知权限授权界面，直到用户授权：

### 同时获取多个权限 （要申请的权限需要自行添加到AndroidManifest.xml文件中）
let permissions: string[] = ["android.permission.READ_PHONE_STATE", "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"];
      
WeichaiServicePlugin && WeichaiServicePlugin.requestPermissions(permissions, (pluginResult: any[]) => {
      console.log("成功：")
    });
  
  1. 弹出申请框，用户需要允许权限。用户选择禁止时，一直弹出
  2. 用户选择禁止且选择不再提示后，弹出对话框引导用户到应用设置页手动授权。
