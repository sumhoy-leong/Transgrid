/**
 * Ventyx 2012
 *
 * This class is intended to provide common methods to utilise the Screen Service.
 *
 */
package com.mincom.ellipse.script.custom

import com.mincom.ellipse.client.connection.ConnectionHolder
import com.mincom.ellipse.ejra.mso.GenericMsoRecord
import com.mincom.ellipse.ejra.mso.MsoErrorMessage
import com.mincom.ellipse.ejra.mso.MsoField
import com.mincom.enterpriseservice.ellipse.ConnectionId
import com.mincom.enterpriseservice.ellipse.EllipseScreenService
import com.mincom.enterpriseservice.ellipse.EllipseScreenServiceLocator

public class ScreenLibrary {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 2

    private EllipseScreenService screenService = EllipseScreenServiceLocator.ellipseScreenService
    private ConnectionId msoCon = ConnectionHolder.connectionId
    private GenericMsoRecord screen
    private static final int MAX_SUBMIT = 10

    private static final String ERR_MESSAGE_MAX_SUBMIT_REACHED = "Cannot continue submitting because the maximum limit (${MAX_SUBMIT}) is reached."

    /**
     * Run an MSO
     * @param progName
     */
    public void executeProgram(String progName){
        screen = screenService.executeByName(msoCon, progName)
    }

    /**
     * Press submit (only once)
     */
    public void ok(){
        screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
        screen = screenService.execute(msoCon, screen)
    }

    /**
     * Press submit (OK) until you get to the next screen
     * @param nextScreen
     * @throws ScreenException - if the nextScreen is never reached
     * @throws MaximumSubmitExceededException - if the maximum submit limit exceeded
     */
    public void okUntilNextScreen(String nextScreen) throws ScreenErrorException{
        boolean isNextScreen = false

        int submitCounter = 0
        while (!isNextScreen) {
            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
            screen = screenService.execute(msoCon, screen)
            submitCounter++

            if (screen.getMapname().equalsIgnoreCase(nextScreen)) {
                isNextScreen = true
            } else {

                if (isError()){
                    throw new ScreenErrorException(getScreenCodeMessage(), getCurrentCursorMap());
                } else if (submitCounter >= MAX_SUBMIT) {
                    throw new MaximumSubmitExceededException("Next Screen ${nextScreen} is never reached. " + ERR_MESSAGE_MAX_SUBMIT_REACHED)
                }
            }
        }
    }

    /**
     * Press submit (OK) until a particular field is no longer populated
     * @param fieldName
     * @throws ScreenException - if the fieldName is never blank or we move to another screen
     * @throws MaximumSubmitExceededException - if the maximum submit limit exceeded
     */
    public void okUntilFieldBlank(String fieldName) throws ScreenErrorException {
        boolean isFieldBlank = false
        String origScreen = screen.getMapname()

        int submitCounter = 0
        while (!isFieldBlank) {
            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
            screen = screenService.execute(msoCon, screen)
            submitCounter++

            if (screen.getMapname().equalsIgnoreCase(origScreen)) {

                MsoField field = getField(fieldName)
                if (field.getValue().trim().equals("")) {
                    isFieldBlank = true
                } else {

                    if (isError()) {
                        throw new ScreenErrorException(getScreenCodeMessage(), getCurrentCursorMap());
                    }

                    if (submitCounter >= MAX_SUBMIT) {
                        throw new MaximumSubmitExceededException("The field ${fieldName} is never blank. " + ERR_MESSAGE_MAX_SUBMIT_REACHED)
                    }
                }
            } else {
                throw new ScreenException("Field ${fieldName} is never blank because we move to another screen (${screen.getMapname()})")
            }
        }
    }

    /**
     * Press submit (OK) until a particular field is no longer blank
     * @param fieldName
     * @throws ScreenException - if the fieldName is never populated or we move to another screen
     * @throws MaximumSubmitExceededException - if the maximum submit limit exceeded
     */
    public void okUntilFieldPopulated(String fieldName) throws ScreenErrorException {
        boolean isFieldPopulated = false
        String origScreen = screen.getMapname()

        int submitCounter = 0
        while (!isFieldPopulated) {

            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
            screen = screenService.execute(msoCon, screen)
            submitCounter++

            if (screen.getMapname().equalsIgnoreCase(origScreen)) {

                MsoField field = getField(fieldName)
                if (!field.getValue().trim().equals("")) {
                    isFieldPopulated = true
                } else {

                    if (isError()) {
                        throw new ScreenErrorException(getScreenCodeMessage(), getCurrentCursorMap());
                    }

                    if (submitCounter >= MAX_SUBMIT) {
                        throw new MaximumSubmitExceededException("Field ${fieldName} is never populated. " + ERR_MESSAGE_MAX_SUBMIT_REACHED)
                    }
                }
            } else {
                throw new ScreenException("Field ${fieldName} is never populated because we move to another screen (${screen.getMapname()})")
            }
        }
    }

    /**
     * Press submit (OK) until a particular field becomes protected
     * @param fieldName
     * @throws ScreenException - if the fieldName is never protected or we move to another screen
     * @throws MaximumSubmitExceededException - if the maximum submit limit exceeded
     */
    public void okUntilFieldProtected(String fieldName) throws ScreenErrorException {
        boolean isFieldProtected = false
        String origScreen = screen.getMapname()

        int submitCounter = 0
        while (!isFieldProtected) {

            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
            screen = screenService.execute(msoCon, screen)
            submitCounter++

            if (screen.getMapname().equalsIgnoreCase(origScreen)) {

                MsoField field = getField(fieldName)
                if (field.isProtected()) {
                    isFieldProtected = true
                } else {

                    if (isError()) {
                        throw new ScreenErrorException(getScreenCodeMessage(), getCurrentCursorMap());
                    }

                    if (submitCounter >= MAX_SUBMIT) {
                        throw new MaximumSubmitExceededException("Field ${fieldName} is never protected. " + ERR_MESSAGE_MAX_SUBMIT_REACHED)
                    }
                }
            } else {
                throw new ScreenException("Field ${fieldName} is never protected because we move to another screen (${screen.getMapname()})")
            }
        }
    }

    /**
     * Submit and bypass warning (if any)
     * @throws ScreenException - if the warning is always occurs or we move to another screen
     * @throws MaximumSubmitExceededException - if the maximum submit limit exceeded
     */
    public void okUntilNoWarning() throws ScreenErrorException {
        boolean isNoWarning = false
        String origScreen = screen.getMapname()

        int submitCounter = 0
        while (!isNoWarning) {

            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY
            screen = screenService.execute(msoCon, screen)
            submitCounter++

            if (screen.getMapname().equalsIgnoreCase(origScreen)) {

                if (!isWarning()) {
                    isNoWarning = true
                } else {

                    if (isError()) {
                        throw new ScreenErrorException(getScreenCodeMessage(), getCurrentCursorMap());
                    }

                    if (submitCounter >= MAX_SUBMIT) {
                        throw new MaximumSubmitExceededException("Warning remains. " + ERR_MESSAGE_MAX_SUBMIT_REACHED )
                    }
                }
            } else {
                throw new ScreenException("Submitting stopped because we move to another screen (${screen.getMapname()})")
            }
        }
    }

    /**
     * Submit a function key e.g. F3, F7, F9, etc
     * @param fKey - Integer value 1 to 10
     * @throws ScreenException - If the input key is invalid.
     */
    public void functionKey(int key) {
        switch (key) {

            //Submit F1
            case 1 :
                screen.nextAction = GenericMsoRecord.F1_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F2
            case 2 :
                screen.nextAction = GenericMsoRecord.F2_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F3
            case 3 :
                screen.nextAction = GenericMsoRecord.F3_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F4
            case 4 :
                screen.nextAction = GenericMsoRecord.F4_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F5
            case 5 :
                screen.nextAction = GenericMsoRecord.F5_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F6
            case 6 :
                screen.nextAction = GenericMsoRecord.F6_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F7
            case 7 :
                screen.nextAction = GenericMsoRecord.F7_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F8
            case 8 :
                screen.nextAction = GenericMsoRecord.F8_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F9
            case 9 :
                screen.nextAction = GenericMsoRecord.F9_KEY
                screen = screenService.execute(msoCon, screen)
                break

            //Submit F10
            case 10 :
                screen.nextAction = GenericMsoRecord.F10_KEY
                screen = screenService.execute(msoCon, screen)
                break

            default  :
                throw new ScreenException("Invalid function key value: " + key \
					+ ". The valid value for function key: 1, 2, 3, 4, 5, 6, 7, 8, 9 or 10")
                break
        }
    }

    /**
     * Get the screen map name
     * @param fieldName
     */
    public String getScreenName(){
        String screenName = screen.getMapname()
        return screenName
    }

    /**
     * Get the screen Message (Info / Error / Warning message)
     * @return String - The string returned is the message displayed which can be info / error / warning
     */
    public String getScreenMessage(){
        return screen.getMessage();
    }

    /**
     * Extract error message
     * @return Map - contains error code and message
     */
    public Map getScreenCodeMessage() {
        String[] error = getScreenMessage().split(" - ");
        String errorCode = error[0].indexOf(":") > 0 ? error[0].substring(error[0].indexOf(":") + 1):error[0];
        String errorMessage = error[1];
        return [errorCode: errorCode, errorMessage: errorMessage];
    }

    /**
     * Take all field values on the screen and put them into the Map.
     * @return Map<String, MsoField> - The Map will contain all MsoFields of the screen
     */
    public Map<String, MsoField> getFields(){
        Map<String, String> screenFields = new HashMap<String, String>()
        Map<String, MsoField> mapData = screen.getCurrentScreenDetails().getScreenFields()
        mapData.entrySet().each {mapEntry ->
            screenFields.put(mapEntry.getKey(), mapEntry.getValue())
        }
        return screenFields
    }

    /**
     * Set a particular field to a particular hard coded value
     * @param fieldName - set with Field name (e.g. TABLE_CODE1I)
     * @param value - set with string value
     */
    public void setField(String fieldName, String value){
        if (isValueSet(value)) {
            screen.setFieldValue(fieldName, value)
        }
    }

    /**
     * Check if value is populated and should be used to update the MsoField
     *
     * @param fieldValue
     * @return boolean - true if the field is populated or set to spaces.
     */
    private boolean isValueSet(Object fieldValue){
        return (fieldValue != null)
    }

    /**
     * Get a particular field value
     * @param fieldName - set with Field name (e.g. TABLE_CODE1I)
     * @return MsoField
     */
    public MsoField getField(String fieldName){
        MsoField msoField = screen.getField(fieldName)
        return msoField
    }

    /**
     * Go to Main Menu
     * @throws ScreenException - if the main menu is never presented
     */
    public void goToMainMenu() throws ScreenException{
        functionKey(5)
    }

    /**
     * To check if error occurs
     * @return - True or False
     */
    public boolean isError(){
        return ((char)screen.errorType) == MsoErrorMessage.ERR_TYPE_ERROR
    }

    /**
     * To check if warning occurs
     * @return - True or False
     */
    public boolean isWarning(){
        return ((char)screen.errorType) == MsoErrorMessage.ERR_TYPE_WARNING
    }

    /**
     * Extract current cursor
     * @return Map - contains cursor name and value
     */
    public Map getCurrentCursorMap() {
        String cursorName = screen.getCurrentCursorField().getName()
        String cursorValue = screen.getCurrentCursorField().getValue()
        return [cursorField: cursorName, cursorValue: cursorValue];
    }
}

public class ScreenException extends RuntimeException {
}

public class MaximumSubmitExceededException extends RuntimeException {
}

