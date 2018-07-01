package com.google.android.gms.samples.vision.ocrreader.utils;

import android.net.Uri;

/**
 * All Nuance Developers configuration parameters can be set here.
 *
 * Copyright (c) 2015 Nuance Communications. All rights reserved.
 */
public class Configuration {

    //All fields are required.
    //Your credentials can be found in your Nuance Developers portal, under "Manage My Apps".
    public static final String APP_KEY = "4bcdd18768661c5d80f210e3a917426eb09da71f55f86852b86d37ae3d822a894ffb10b8622c15e21e25c5c73c0835eca5dd0bfbd5744d8de1843c1f0666a856";
    public static final String APP_ID = "NMDPPRODUCTION_Mohamed_Ali_Read_Text_For_Blind_20180627065623";
    public static final String SERVER_HOST = "gme.nmdp.nuancemobility.net";
    public static final String SERVER_PORT = "443";

    public static final String LANGUAGE = "!LANGUAGE!";

    public static final Uri SERVER_URI = Uri.parse("nmsps://" + APP_ID + "@" + SERVER_HOST + ":" + SERVER_PORT);

    //Only needed if using NLU
    public static final String CONTEXT_TAG = "!NLU_CONTEXT_TAG!";

    public static final String LANGUAGE_CODE = (Configuration.LANGUAGE.contains("!") ? "ara-XWW" : Configuration.LANGUAGE);

    public static final String LANGUAGE_CODE_ARABIC = "ara-XWW";
    public static final String LANGUAGE_CODE_ENGLISH = "eng-USA";

}



