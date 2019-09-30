package com.cordova.android.toolkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;

/**
 * *******************************************
 *
 * @Project CustomCordovaPlugin
 * @Author zs
 * @Time 2019/9/30 9:52
 * @Description *******************************************
 */
public class ToolkitPlugin extends CordovaPlugin {
    private static final int INSTALL_PERMISSION = 1;     // Constant for request install permission
    private static final int NOTIFY_PERMISSION = 2;     // Constant for request notify permission
    private static final int REQUEST_PERMISSIONS = 3;     // Constant for request permissions
    private static final int APP_SETTING = 4;     // Constant for open APP setting page
    private static final int DEVICE_INFO = 5;     // Constant for get device information

    private static final int UNKNOWN_FAIL_CODE = 101;
    private static final int GET_DEVICEINFO_FAIL_CODE = 11;

    private final PendingRequests pendingRequests = new PendingRequests();
    // 未授权的权限
    private ArrayList<String> _unGrantedPermissions = new ArrayList<>();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) {
            case "requestInstallPermission":
                requestInstallPermission(pendingRequests.createRequest(INSTALL_PERMISSION, null, callbackContext));
                break;
            case "requestNotifyPermission":
                requestNotificationPermission(pendingRequests.createRequest(NOTIFY_PERMISSION, null, callbackContext));
                break;
            case "requestPermissions":
                _unGrantedPermissions.clear();
                PendingRequests.Request req = pendingRequests.createRequest(REQUEST_PERMISSIONS, null, callbackContext);
                JSONArray permissions = args.getJSONArray(0);
                try {
                    for (int i = 0; i < permissions.length(); i++) {
                        _unGrantedPermissions.add(permissions.getString(i));
                    }
                    this.requestPermissions(req);
                } catch (JSONException e) {
                    pendingRequests.resolveWithFailure(req, createErrorObject(UNKNOWN_FAIL_CODE, "Permission has unknow error."));
                    e.printStackTrace();
                }
                break;
            case "openAppSettings":
                this.openAppSettings(pendingRequests.createRequest(APP_SETTING, null, callbackContext));
                break;
            case "getDeviceInfo":
                getDeviceInfo(pendingRequests.createRequest(DEVICE_INFO, null, callbackContext));
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 检查应用安装权限
     */
    @TargetApi(Build.VERSION_CODES.O)
    private void requestInstallPermission(PendingRequests.Request req) {
        boolean installPermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) || this.cordova.getActivity().getPackageManager().canRequestPackageInstalls();
        if (!installPermission) {
            this.switchToAppInstallSettings(req);
        } else {
            pendingRequests.resolveWithSuccess(req);
        }
    }

    /**
     * 手动开启应用安装权限
     */
    @TargetApi(Build.VERSION_CODES.O)
    private void switchToAppInstallSettings(PendingRequests.Request req) {
        //8.0新API
        Uri packageURI = Uri.parse("package:" + this.cordova.getContext().getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        this.cordova.startActivityForResult(this, intent, req.requestCode);
    }

    /**
     * 检查应用通知权限
     */
    private void requestNotificationPermission(PendingRequests.Request req) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(this.cordova.getActivity());
        // areNotificationsEnabled方法的有效性官方只最低支持到API 19，低于19的仍可调用此方法不过只会返回true，即默认为用户已经开启了通知。
        boolean notifyPermission = manager.areNotificationsEnabled();
        if (!notifyPermission) {
            this.switchToNotificationSettings(req);
        } else {
            pendingRequests.resolveWithSuccess(req);
        }
    }

    /**
     * 手动设置应用通知权限
     */
    private void switchToNotificationSettings(PendingRequests.Request req) {
        try {
            // 根据isOpened结果，判断是否需要提醒用户跳转AppInfo页面，去打开App通知权限
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                intent.putExtra(EXTRA_APP_PACKAGE, this.cordova.getContext().getPackageName());
                intent.putExtra(EXTRA_CHANNEL_ID, this.cordova.getContext().getApplicationInfo().uid);
            } else {
                //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
                intent.putExtra("app_package", this.cordova.getContext().getPackageName());
                intent.putExtra("app_uid", this.cordova.getContext().getApplicationInfo().uid);
            }
            this.cordova.startActivityForResult(this, intent, req.requestCode);
        } catch (Exception e) {
            e.printStackTrace();
            // 出现异常则跳转到应用设置界面：锤子坚果3——OC105 API25
            Intent intent = new Intent();

            //下面这种方案是直接跳转到当前应用的设置界面。
            //https://blog.csdn.net/ysy950803/article/details/71910806
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", this.cordova.getContext().getPackageName(), null);
            intent.setData(uri);
            this.cordova.startActivityForResult(this, intent, req.requestCode);
        }
    }

    /**
     * 获取多个权限
     */
    private void requestPermissions(PendingRequests.Request req) {
        if (_unGrantedPermissions.size() > 0) {
            String[] unGrantPermissions = _unGrantedPermissions.toArray(new String[0]);
            PermissionHelper.requestPermissions(this, req.requestCode, unGrantPermissions);
        } else {
            _unGrantedPermissions.clear();
            pendingRequests.resolveWithSuccess(req);
        }
    }

    /**
     * 打开应用设置界面
     */
    private void openAppSettings(PendingRequests.Request req) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + this.cordova.getContext().getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        this.cordova.startActivityForResult(this, intent, req.requestCode);
    }

    /**
     * 打开APP详情页面，引导用户去设置权限
     *
     * @param permissionNames 权限名称（如是多个，使用\n分割）
     * @param requestCode
     */
    private void openAppDetails(String permissionNames, final int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
        builder.setCancelable(false);
        String sb = PermissionUtils.PermissionTip1 + permissionNames + PermissionUtils.PermissionTip2;
        builder.setMessage(sb);
        final ToolkitPlugin plugin = this;
        builder.setPositiveButton(PermissionUtils.PermissionDialogPositiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + cordova.getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                cordova.startActivityForResult(plugin, intent, requestCode);
            }
        });
        builder.show();
    }

    /**
     * 获取设备信息
     */
    @SuppressLint("MissingPermission")
    private void getDeviceInfo(PendingRequests.Request req) {
        boolean hasPhonePermission = PermissionHelper.hasPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (!hasPhonePermission) {
            PermissionHelper.requestPermission(this, req.requestCode, Manifest.permission.READ_PHONE_STATE);
        } else {
            JSONObject r = new JSONObject();
            TelephonyManager tm = (TelephonyManager) this.cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("HardwareIds") String imei = tm.getDeviceId();
            @SuppressLint("HardwareIds") String uuid = Settings.Secure.getString(this.cordova.getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);

            try {
                r.put("uuid", uuid); // 设备唯一码
                r.put("imei", imei); // 卡槽1串号
                r.put("platform", "Android"); // 平台版本
                r.put("sdk", Build.VERSION.SDK_INT); // 系统SDK版本
                r.put("version", Build.VERSION.RELEASE); // android版本
                r.put("brand", Build.BRAND); // 手机品牌
                r.put("model", Build.MODEL); // 手机型号
                r.put("manufacturer", Build.MANUFACTURER); // 手机厂商
                r.put("isVirtual", Build.FINGERPRINT.contains("generic") ||
                        Build.PRODUCT.contains("sdk")); // 是否为虚拟设备

                req.results.put(r);
                pendingRequests.resolveWithSuccess(req);
            } catch (JSONException e) {
                pendingRequests.resolveWithFailure(req, createErrorObject(GET_DEVICEINFO_FAIL_CODE, "can not get device information!"));
                e.printStackTrace();
            }
        }
    }

    /**
     * Called when the activity view exits.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        final PendingRequests.Request req = pendingRequests.get(requestCode);
        // Result received okay
        if (resultCode == Activity.RESULT_OK) {
            if (req.action == DEVICE_INFO || req.action == INSTALL_PERMISSION
                    || req.action == NOTIFY_PERMISSION || req.action == REQUEST_PERMISSIONS) {
                executeRequest(req);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (req.action == DEVICE_INFO || req.action == INSTALL_PERMISSION
                    || req.action == NOTIFY_PERMISSION || req.action == REQUEST_PERMISSIONS) {
                executeRequest(req);
            } else if (req.action == APP_SETTING) {
                pendingRequests.resolveWithSuccess(req);
            }
        } else {
            // If something else
            if (req.results.length() > 0) {
                pendingRequests.resolveWithSuccess(req);
            }
            // something bad happened
            else {
                pendingRequests.resolveWithFailure(req, createErrorObject(UNKNOWN_FAIL_CODE, "unknown failure occur!"));
            }
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        PendingRequests.Request req = pendingRequests.get(requestCode);
        if (req != null) {
            _unGrantedPermissions.clear();

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    _unGrantedPermissions.add(permissions[i]);
                }
            }

            boolean success = (_unGrantedPermissions != null ? _unGrantedPermissions.size() : 0) == 0;

            if (success) {
                if (req.action == DEVICE_INFO) {
                    executeRequest(req);
                } else if (req.action == REQUEST_PERMISSIONS) {
                    pendingRequests.resolveWithSuccess(req);
                }
            } else {
                if (req.action == DEVICE_INFO) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this.cordova.getActivity(), _unGrantedPermissions.get(0))) {
                        String names = PermissionUtils.getInstance().getPermissionNames(_unGrantedPermissions);
                        openAppDetails(names, req.requestCode);
                    } else {
                        executeRequest(req);
                    }
                } else if (req.action == REQUEST_PERMISSIONS) {
                    boolean hasRationale = false;

                    for (String unGrantedPermission : _unGrantedPermissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this.cordova.getActivity(), unGrantedPermission)) {
                            hasRationale = true;
                            break;
                        }
                    }

                    if (hasRationale) {
                        String names = PermissionUtils.getInstance().getPermissionNames(_unGrantedPermissions);
                        openAppDetails(names, req.requestCode);
                    } else {
                        executeRequest(req);
                    }
                }
            }
        }
    }

    private void executeRequest(PendingRequests.Request req) {
        if (req.action == INSTALL_PERMISSION) {
            requestInstallPermission(req);
        } else if (req.action == NOTIFY_PERMISSION) {
            requestNotificationPermission(req);
        } else if (req.action == REQUEST_PERMISSIONS) {
            requestPermissions(req);
        } else if (req.action == DEVICE_INFO) {
            getDeviceInfo(req);
        }
    }

    private JSONObject createErrorObject(int code, String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("code", code);
            obj.put("message", message);
        } catch (JSONException e) {
            // This will never happen
        }
        return obj;
    }

    public Bundle onSaveInstanceState() {
        return pendingRequests.toBundle();
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        pendingRequests.setLastSavedState(state, callbackContext);
    }
}
