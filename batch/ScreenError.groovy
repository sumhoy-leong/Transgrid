/**
 * Ventyx 2012
 */
package com.mincom.ellipse.script.custom

import java.util.Map;

public class ScreenError {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 2

    final String errorCode;
    final String errorMsg;
    final String currentCursorField;
    final String currentCursorValue;

    ScreenError(String code, String msg){
        errorCode = code;
        errorMsg  = msg;
    }

    ScreenError(String code, String msg, String currField, String currValue){
        errorCode = code;
        errorMsg  = msg;
        currentCursorField = currField;
        currentCursorValue = currValue;
    }

    ScreenError(Map errorMap){
        errorCode = errorMap.errorCode;
        errorMsg = errorMap.errorMessage;
    }

    ScreenError(Map errorMap, Map cursorMap){
        errorCode = errorMap.errorCode;
        errorMsg  = errorMap.errorMessage;
        currentCursorField = cursorMap.cursorField;
        currentCursorValue = cursorMap.cursorValue;
    }
}
