/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.providers.api.phoenix;

import android.text.TextUtils;

import com.kaltura.netkit.utils.ErrorElement;

/**
 * @hide
 */

public class PhoenixErrorHelper {

    public static final String ERROR_CODE_UNAVILABLE = "Unavailable";
    public static final String ERROR_MESSAGE_UNAVILABLE = "unknown error";

    /**
     * in case specific error codes should be parsed to predefined errors.
     * @param code
     * @param message
     * @return ErrorElement
     */
    public static ErrorElement getErrorElement(String code, String message){
        ErrorElement errorElement = getDefinedErrorElement(code, message);
        if(errorElement == null){
            errorElement = new ErrorElement(code, message);
        }
        return errorElement;
    }

    public static ErrorElement getErrorElement(ErrorElement error){
        ErrorElement errorElement = getDefinedErrorElement(error.getCode(), error.getMessage());
        if(errorElement == null){
            return error;
        }
        return errorElement;
    }

    /**
     * parse phoenix specific errors to playkit errors.
     * errors with text code are messages that my be retrieved from the "getPlaybackContext" API.
     *
     * @param code
     * @param message
     * @return
     */
    private static ErrorElement getDefinedErrorElement(String code, String message) {

        if (code == null) {
            code = ERROR_CODE_UNAVILABLE;
        }

        if (TextUtils.isEmpty(message)) {
            message = ERROR_MESSAGE_UNAVILABLE;
        }
        return new ErrorElement(message, code).setName("OTTError");

/////////ERRORS are now built as OTTError with the xosw ns message given from BE.

//        switch (code){
//            case ERROR_CODE_UNAVILABLE:
//            case "2016":
//                return new ErrorElement(message, code);
//
//            case "500016":
//                return ErrorElement.SessionError.message("session token has been expired");
//
//            case "RecordingPlaybackNotAllowedForNonExistingEpgChannel":
//            case "3039":
//            case "4022":
//            case "3050":
//                return ErrorElement.NotFound.message("requested content was not found");
//
//            case "NoFilesFound":
//            case "3054":
//                return ErrorElement.NotFound.message("no available sources for media");
//
//            case "ServiceNotAllowed":
//            case "DeviceTypeNotAllowed":
//            case "1002":
//            case "3003":
//                return new RestrictionError("requested operation is not allowed", RestrictionError.Restriction.NotAllowed);
//
//            case "RecordingPlaybackNotAllowedForNotEntitledEpgChannel":
//            case "NotEntitled":
//            case "3051":
//            case "3032":
//                return new RestrictionError("content is not entitled", RestrictionError.Restriction.NotEntitled);
//
//            case "ConcurrencyLimitation":
//            case "4001":
//                return new RestrictionError("restricted due to concurrency limitation", RestrictionError.Restriction.ConcurrencyLimitation);
//
//            case "MediaConcurrencyLimitation":
//            case "4000":
//                return new RestrictionError("restricted due to concurrency limitation", RestrictionError.Restriction.MediaConcurrencyLimitation);
//
//            case "2001":
//                return new RestrictionError("restricted due to suspended account", RestrictionError.Restriction.Suspended);
//
//            default:
//                return null;
//        }
    }
}
