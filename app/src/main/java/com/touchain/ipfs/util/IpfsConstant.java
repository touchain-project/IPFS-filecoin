package com.touchain.ipfs.util;


public class IpfsConstant {

    
    public static final int FILE_SIZE = 30;

    //官方地址
    public static final String OFFICIAL_URL = "https://ipfs.io/ipfs/";
    public static final String OFFICIAL_URI = "ipfs://";
    
    public static final String BASEURL = "http://192.168.0.1";


    //显示地址，后面接hash值
    public static final String SHOW_URL = BASEURL + ":8080/ipfs/";

    public static final String API_ADD = BASEURL + "/api/v0/add";

    public static final String API_GET = BASEURL + "/api/v0/cat";

    public static final String API_STATE = BASEURL + "/api/v0/object/stat";

    public static final String API_FILE_MKDIR = BASEURL + "/api/v0/files/mkdir?arg=/";

    public static final String API_FILE_CP = BASEURL + "/api/v0/files/cp";

    public static final String API_FILE_LIST = BASEURL + "/api/v0/ls?arg=";

    public static final String API_FILE_RM = BASEURL + "/api/v0/files/rm?arg=";

    public static final String API_MAIN_HASH = BASEURL + "/api/v0/files/stat?arg=/";

}
