/**
 * @author Ventyx 2012
 *
 * Before an equipment record is created, this hook will attempt to:
 * 1) Apply the PIC Equipment naming convention
 * 2) Default an Account Code
 * 3) Default a Tax Code
 * 4) Default a Consumption Tax Code
 * 5) Default a Costing Flag
 */

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.ellipse.hook.hooks.HookTools.*

import com.mincom.ellipse.types.m0000.instances.TableType
import com.mincom.ellipse.types.m0000.instances.TableCode
import com.mincom.ellipse.types.m0000.instances.TableDesc
import com.mincom.ellipse.types.m3001.instances.TableCodeDTO
import com.mincom.ellipse.types.m3001.instances.TableCodeServiceResult

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

public class EquipmentService_create extends ServiceHook {
  
  /*
   * IMPORTANT!
   * Update this Version number EVERY push to GIT
   */
  private String version = "7"
  
  @Override
  public Object onPreExecute(Object dto) {
      log.info("EquipmentService_create onPreExecute - version: ${version}")
      
      EquipmentServiceCreateRequestDTO request = dto
      String sEquipNo = request.getEquipmentNo()
      String sEquipClass = request.getEquipmentClass()
      String sError = "OK" 

      // Equipment Number must be blank for defaulting to occur
      if (sEquipNo.equals("") || sEquipNo.equals(null)) {
          // Equipment Class must be supplied if not, skip the hook and let standard
          // validation handle the absence of an Equipment Class
          if (!sEquipClass.equals("") && !sEquipClass.equals(null)) {
              // PIC numbering will only apply to equipment using valid classes listed in the #EQC table
              try {
                  log.info("validate Equipment Class")
                  TableServiceReadReplyDTO tableEqcReply = tools.service.get('Table').read({
                      it.tableType = '#EQC'
                      it.tableCode = sEquipClass})
                  
                  // Equipment Class is in #EQC so get default values to use from assoc rec
                  String sAssocRec = tableEqcReply.getAssociatedRecord().trim()

                  if (!sAssocRec.equals("") && !sAssocRec.equals(null)) {
                      
                      // Get next available equipment number
                      try {
                          log.info("Last PIC No used from EQN")
                          TableServiceReadReplyDTO tableEqnReply = tools.service.get('Table').read({
                              it.tableType = '#EQN'
                              it.tableCode = 'TG'})
                          
                          // Last Equipment No used from table description
                          String sNextNumber = String.format("%06d",
                              (tableEqnReply.getDescription().toInteger() + 1))
                              
                          // Update #EQN with an incremented PIC number
                          try {
                              log.info("Update EQN")
                              TableCodeServiceResult tcsReply = tools.service.get("TableCode").update({
                                 TableCodeDTO tcDto ->
                                     tcDto.tableType = new TableType('#EQN')
                                     tcDto.tableCode = new TableCode('TG')
                                     tcDto.tableDescription = new TableDesc(sNextNumber)}, false)
                                  
                              // Successful in getting a PIC number for the new equipment
                              // so set default values on Equipment request record
                              request.setEquipmentNo('TG' + sNextNumber)
                              if (sAssocRec.size() >= 9) {
                                  request.setAccountCode(sAssocRec.substring(0,9))
                              }
                              if (sAssocRec.size() >= 10) {
                                  request.setTaxCode(sAssocRec.substring(9,10))
                              }
                              if (sAssocRec.size() >= 12) {
                                  request.setCtaxCode(sAssocRec.substring(10,12))
                              }
                              if (sAssocRec.size() >= 13) {
                                  request.setCostingFlag(sAssocRec.substring(12))
                              }
                          } catch (EnterpriseServiceOperationException e){
                              // Failed to assign a PIC No to the new equipment
                              listErrors(e)
                              log.info("Failed to assign a PIC Number. Check #EQN Table.")
                              sError = "ERR1"
                          }
                      } catch (EnterpriseServiceOperationException e){
                          // Failed to locate the last PIC No used
                          listErrors(e)
                          log.info("Failed to get the last PIC Number used. Check #EQN Table.")
                          sError = "ERR2"
                      }
                  }
              } catch (EnterpriseServiceOperationException e){
                  listErrors(e)
              }

              //Displa
              if (sError != "OK") {
                  if (sError == "ERR1") {
                      throw new EnterpriseServiceOperationException(
                          new ErrorMessageDTO("", "Failed to assign a PIC Number. Check #EQN Table.",
                              "equipmentNo", 0, 0))
                  } else {
                      throw new EnterpriseServiceOperationException(
                          new ErrorMessageDTO("", "Failed to get the last PIC Number used. Check #EQN Table.",
                              "equipmentNo", 0, 0))
                  }
              }
          }
      }
      return null
  }
  
  @Override
  public Object onPostExecute(Object input, Object result) {
      log.info("EquipmentService_create onPostExecute - version: ${version}")
      
      // Nothing to do here
      return null
  }
  
  /**
   * Write errors to log
   * @param e Error Object
   */
  private void listErrors(EnterpriseServiceOperationException e) {
      List <ErrorMessageDTO> listError = e.getErrorMessages()
      listError.each{ErrorMessageDTO errorDTO ->
          log.info ("Error Code: " + errorDTO.getCode())
          log.info ("Error Message: " + errorDTO.getMessage())
          log.info ("Error Fields: " + errorDTO.getFieldName())
      }
  }
}

