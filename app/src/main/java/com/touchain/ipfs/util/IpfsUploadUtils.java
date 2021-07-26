package com.touchain.ipfs.util;

import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.tancheng.carbonchain.utils.network.NormalCallBack;
import com.tancheng.carbonchain.utils.network.OkHttpManager;
import com.tancheng.carbonchain.utils.network.SimpleCallBack;
import com.touchain.basic.common.util.DidUtils;
import com.touchain.basic.ui.ipfs.entity.IpfsApiEntity;
import com.touchain.basic.ui.ipfs.entity.IpfsApiFileEntity;

import java.io.File;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

/**
 * ipfs文件上传
 */
public class IpfsUploadUtils {

    public interface Listener {
        void success(IpfsApiEntity result);

        void progress(int progress, long total, long done);

        void fail(String message);
    }


    /**
     * 上传文件到IPFS节点
     *
     * @param file     文件
     * @param listener 回调
     */
    public static void upload(File file, Listener listener) {

        OkHttpManager.getInstance().postUploadSingleFile(IpfsConstant.API_ADD, new NormalCallBack<String>() {
            @Override
            protected void onSuccess(Call call, Response response, String result) {
                LogUtils.e("上传结果为：", result);
                if (result.startsWith("<html>")) {
                    listener.fail("上传失败");
                    return;
                }
                IpfsApiEntity ipfsApiEntity = new Gson().fromJson(result, IpfsApiEntity.class);
                mkDir(ipfsApiEntity.getHash(), file.getName());

                listener.success(ipfsApiEntity);
            }

            @Override
            protected void onEror(Call call, int statusCode, Exception e) {
                listener.fail("请求异常" + statusCode + (e == null ? "" : e.getMessage()));
            }

            @Override
            protected void inProgress(int progress, long total, long done) {
                listener.progress(progress, total, done);
            }
        }, file, "file", null);

    }

    /**
     * 刷新用户did对应的ipfs文件hash，每次上传或者删除都会变化
     */
    public static void refreshUserHash(){
        LogUtils.e("检测是否存在hash");
        getMainHash();
    }


    /**
     * 创建个人文件目录
     */
    private static void mkDir(String hash, String fileName) {
        OkHttpManager.getInstance().postRequest(IpfsConstant.API_FILE_MKDIR + DidUtils.INSTANCE.getDid(), new SimpleCallBack<String>() {
            @Override
            protected void onSuccess(Call call, Response response, String result) {
                LogUtils.d("创建目录成功", result);
                cp(hash, fileName);
            }

            @Override
            protected void onEror(Call call, int statusCode, Exception e) {
                LogUtils.e("创建目录失败", e);
                //重复创建会失败
                cp(hash, fileName);
            }
        }, null);
    }

    /**
     * 移动上传好的文件到指定目录
     */
    private static void cp(String hash, String fileName) {
        String arg1 = "/ipfs/" + hash;
        String arg2 = "/" + DidUtils.INSTANCE.getDid() + "/" + fileName;
        LogUtils.d("移动文件从", arg1, arg2);
        OkHttpManager.getInstance().postRequest(IpfsConstant.API_FILE_CP + "?arg=" + arg1 + "&arg=" + arg2, new SimpleCallBack<String>() {
            @Override
            protected void onSuccess(Call call, Response response, String result) {
                LogUtils.d("移动文件成功", result);
            }

            @Override
            protected void onEror(Call call, int statusCode, Exception e) {
                LogUtils.e("移动文件失败", statusCode, e);
            }
        }, null);
    }

    /**
     * 获取主hash
     * 必须拿到主Hash才能获取到所有文件夹数据
     */
    private static void getMainHash(){
        OkHttpManager.getInstance().postRequest(IpfsConstant.API_MAIN_HASH, new SimpleCallBack<String>() {
            @Override
            protected void onSuccess(Call call, Response response, String result) {
                IpfsApiEntity ipfsApiEntity = new Gson().fromJson(result, IpfsApiEntity.class);
                getUserHash(ipfsApiEntity.getHash());
            }

            @Override
            protected void onEror(Call call, int statusCode, Exception e) {
                LogUtils.e("获取主Hash失败", e);

            }
        }, null);
    }

    /**
     * 获取用户个人文件目录Hash
     * 便于通过此hash获取用户的个人文件列表
     */
    private static void getUserHash(String mainHash){
        OkHttpManager.getInstance().postRequest(IpfsConstant.API_FILE_LIST + mainHash, new SimpleCallBack<String>() {
            @Override
            protected void onSuccess(Call call, Response response, String result) {
                IpfsApiFileEntity ipfsApiFileEntity = JSON.parseObject(result, IpfsApiFileEntity.class);
                List<IpfsApiFileEntity.ObjectsEntity.LinksEntity> linkList = ipfsApiFileEntity.getLinkList();

                String did = DidUtils.INSTANCE.getDid();
                for(IpfsApiFileEntity.ObjectsEntity.LinksEntity entity : linkList){
                    if(entity.getName().equals(did)){
                        LogUtils.d("获取个人hash成功, 存储到本地", entity.getHash());
                        IpfsCacheUtils.INSTANCE.cacheHash(entity.getHash());
                        return;
                    }
                }
            }

            @Override
            protected void onEror(Call call, int statusCode, Exception e) {
                LogUtils.e("获取用户个人文件目录Hash失败", e);

            }
        }, null);
    }
}
