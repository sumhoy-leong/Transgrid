/**
 * @author Ventyx 2012
 *
 * OnDisplay, attempt to populate one of the mandatory fields, Costing Flag based on
 * the Equipment Class. We do this here because mandatory fields are validated before
 * the MSO is run and before the onPreSubmit hook is run. So attempt to pre-populate
 * the Costing Flag so that we do not get the error message saying it must be supplied.
 *
 * OnPreSubmit, before an equipment record is created, this hook will attempt to:
 * 1) Apply the PIC Equipment naming convention using the Equipment Class, #EQC and #EQN
 *    tables. I.e. determine the next available equipment number and default that value
 *    into the Equipment No of the screen object
 * 2) Update the #EQN table with the latest Equipment Number allocated
 * 3) Default an Account Code
 * 4) Default a Costing Flag
 * 
 * Similar to core Ellipse's auto-numbering, once a number is allocated, it cannot be
 * unallocated even if the user decides to cancel the creation of an Equipment. This
 * is done to avoid multiple concurrent sessions of Equipment creation picking up the
 * same next available Equipment Number. 
 */

import com.mincom.ellipse.ejra.mso.GenericMsoRecord
import com.mincom.ellipse.ejra.mso.MsoScreenData
import com.mincom.ellipse.ejra.mso.MsoErrorMessage

import com.mincom.ellipse.hook.hooks.MSOHook
import com.mincom.ellipse.hook.hooks.HookTools.*

import com.mincom.ellipse.types.m0000.instances.TableType
import com.mincom.ellipse.types.m0000.instances.TableCode
import com.mincom.ellipse.types.m0000.instances.TableDesc

import com.mincom.ellipse.types.m3001.instances.TableCodeServiceResult
import com.mincom.ellipse.types.m3001.instances.TableCodeDTO

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

public class MSM600B extends MSOHook {

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private String version = "5"
    
    @Override
    public GenericMsoRecord onDisplay(GenericMsoRecord screen){
        log.info("MSM600B onDisplay - version: ${version}")
        
        String sEquipNo = screen.getField("EQUIP_NO2I").getValue()
        
        // Equipment Number must be blank for defaulting to occur
        // I.e. The screen is run for the purpose of creating an Equipment
        if (sEquipNo.equals("") || sEquipNo.equals(null)) {
            // Equipment Class must be supplied if not, skip the hook and let standard
            // validation handle the absence of an Equipment Class
            String sEquipClass = screen.getField("EQUIP_CLASS2I").getValue()
            
            if (!sEquipClass.equals("") && !sEquipClass.equals(null)) {
                // PIC numbering will only apply to equipment using valid classes listed in the #EQC table
                boolean bEquipClassFound = false
                String sAssocRec = ""
                
                // Check #EQC table for the Equipment Class
                (bEquipClassFound, sAssocRec) = checkEquipClass(sEquipClass)
                if (bEquipClassFound) {
                    // Equipment Class is in #EQC so get Costing Flag value
                    if (sAssocRec.size() >= 13) {
                        String sCostingFlag = sAssocRec.substring(12)
                        def sCstFlg = screen.getField("COSTING_FLG2I")
                        sCstFlg.setValue(sCostingFlag)
                    }
                }
            }
        }
        return null
    }
    
    @Override
    public GenericMsoRecord onPreSubmit(GenericMsoRecord screen){
        log.info("MSM600B onPreSubmit - version: ${version}")
        
        // Only run this custom code if the Submit button was pressed. SUBMIT = 1.
        // A lookup also sends a value of 1. Make sure submit was indeed pressed
        // and a lookup was not called.
        if (screen.getNextAction() == 1 && !checkForQMark(screen)) {
            def sEqpNo = screen.getField("EQUIP_NO2I")
            String sEquipNo = sEqpNo.getValue()
            
            // Equipment Number must be blank for defaulting to occur
            // I.e. The screen is run for the purpose of creating an Equipment
            if (sEquipNo.equals("") || sEquipNo.equals(null)) {
                // Equipment Class must be supplied if not, skip the hook and let standard
                // validation handle the absence of an Equipment Class
                String sEquipClass = screen.getField("EQUIP_CLASS2I").getValue()
                
                if (!sEquipClass.equals("") && !sEquipClass.equals(null)) {
                    // PIC numbering will only apply to equipment using valid classes listed in the #EQC table
                    boolean bEquipClassFound = false
                    String sAssocRec = ""
                    // Check #EQC table for the Equipment Class
                    (bEquipClassFound, sAssocRec) = checkEquipClass(sEquipClass)
                    
                    if (bEquipClassFound) {
                        // Get next available equipment number
                        try {
                            TableServiceReadReplyDTO tableEqnReply = tools.service.get('Table').read({
                                it.tableType = '#EQN'
                                it.tableCode = 'TG'})
                                
                            // Next Equipment Number
                            String sNextNumber = String.format("%06d",
                               (tableEqnReply.getDescription().toInteger() + 1))
                               
                            // Update #EQN with an incremented PIC number
                            try {
                                TableCodeServiceResult tcsReply = tools.service.get("TableCode").update({
                                TableCodeDTO tcDto ->
                                    tcDto.tableType = new TableType('#EQN')
                                    tcDto.tableCode = new TableCode('TG')
                                    tcDto.tableDescription = new TableDesc(sNextNumber)}, false)
                                
                                // Set default values on Equipment request record
                                sEqpNo.setValue('TG' + sNextNumber)
                                if (sAssocRec.size() >= 9) {
                                    def sAcctCode = screen.getField("ACCOUNT_CODE2I")
                                    sAcctCode.setValue(sAssocRec.substring(0,9))
                                }
                                if (sAssocRec.size() >= 13) {
                                    def sCstFlg = screen.getField("COSTING_FLG2I")
                                    sCstFlg.setValue(sAssocRec.substring(12))
                                }
                            } catch (EnterpriseServiceOperationException e){
                                listErrors(e)
                                // Failed to assign a PIC No to the new equipment
                                log.info("Failed to assign a PIC Number. Check #EQN Table.")
                                //throw new RuntimeException("Failed to assign a PIC Number. Check #EQN Table.")
                                screen.setErrorMessage(new MsoErrorMessage("Failed to assign a PIC Number. Check #EQN Table.",
                                    "", "Failed to assign a PIC Number. Check #EQN Table.",
                                    MsoErrorMessage.ERR_TYPE_ERROR,
                                    MsoErrorMessage.ERR_SEVERITY_UNSPECIFIED))
                                screen.setCurrentCursorField(screen.getField("EQUIP_NO2I"))
                                return screen
                            }
                        } catch (EnterpriseServiceOperationException e){
                            listErrors(e)
                            // Failed to locate the last PIC No used
                            log.info("Failed to get the last PIC Number used. Check #EQN Table.")
                            //throw new RuntimeException("Failed to get the last PIC Number used. Check #EQN Table.")
                            screen.setErrorMessage(new MsoErrorMessage("Failed to get the last PIC Number used. Check #EQN Table.",
                                "", "Failed to get the last PIC Number used. Check #EQN Table.",
                                MsoErrorMessage.ERR_TYPE_ERROR,
                                MsoErrorMessage.ERR_SEVERITY_UNSPECIFIED))
                            screen.setCurrentCursorField(screen.getField("EQUIP_NO2I"))
                            return screen
                        }
                    }
                }
            }
        }
        return null
    }

  /**
   * Check if the screen fields have a value of '?'
   * @param screen Screen Object
   * @return Question mark Found TRUE/FALSE
   */
    private boolean checkForQMark (GenericMsoRecord screen) {
        // check if a question mark is in a screen attribute value if it is, assume
        // a lookup has been called and return TRUE
        String screenData = screen.getCurrentScreenDetails().getScreenFields().toString()
        return screenData.contains("'?'")
    }
    
  /**
   * Check if the Equipment Class is in the #EQC table
   * @param sEquipClass Equipment Class
   * @return Equipment Class Found TRUE/FALSE, Associated Record
   */
    private def checkEquipClass (String sEquipClass) {
        Boolean bEquipClassFound = false
        String sAssocRec = ""
      
        try {
            // Check if Equipment Class is in the #EQC table
            TableServiceReadReplyDTO tableEqcReply = tools.service.get('Table').read({
                it.tableType = '#EQC'
                it.tableCode = sEquipClass})
            
            bEquipClassFound = true
            sAssocRec = tableEqcReply.getAssociatedRecord()
        } catch (EnterpriseServiceOperationException e){
            listErrors(e)
        }
        return [bEquipClassFound, sAssocRec]    
    }
    
    private void listErrors (EnterpriseServiceOperationException e) {
        List <ErrorMessageDTO> listError = e.getErrorMessages()
        listError.each{ErrorMessageDTO errorDTO ->
            log.info ("Error Code: " + errorDTO.getCode())
            log.info ("Error Message: " + errorDTO.getMessage())
            log.info ("Error Fields: " + errorDTO.getFieldName())
        }
    }
}

