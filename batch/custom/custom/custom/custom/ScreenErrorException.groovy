/**
 * Venty 2012.
 */
package com.mincom.ellipse.script.custom

/**
 * @author c9sg6392
 *	Defines an exception when the screen returns a error message
 */
public class ScreenErrorException extends Exception {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 2

    String code;
    String message;
    String currentCursorField;
    String currentCursorValue;

    public ScreenErrorException(Map params) {
        super();
        this.code = params.errorCode;
        this.message = params.errorMessage;
    }

    public ScreenErrorException(Map errParams, Map cursorParams) {
        super();
        this.code    = errParams.errorCode;
        this.message = errParams.errorMessage;
        this.currentCursorField = cursorParams.cursorField;
        this.currentCursorValue = cursorParams.cursorValue;
    }
}