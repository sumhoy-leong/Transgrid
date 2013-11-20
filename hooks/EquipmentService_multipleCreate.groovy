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

public class EquipmentService_multipleCreate extends ServiceHook {
  
  /*
   * IMPORTANT!
   * Update this Version number EVERY push to GIT
   */
  private String version = "2"
  
  @Override
  public Object onPreExecute(Object dto) {
      log.info("EquipmentService_multipleCreate onPreExecute - version: ${version}")
      
      dto.each {EquipmentServiceCreateRequestDTO request ->
          
          String sEquipNo = request.getEquipmentNo()
          String sEquipClass = request.getEquipmentClass()
          
          // Equipment Number must be blank for defaulting to occur
          if (sEquipNo.equals("") || sEquipNo.equals(null)) {
              // Equipment Class must be supplied
              if (!sEquipClass.equals("") && !sEquipClass.equals(null)) {
                  // PIC numbering will only apply to equipment using valid classes listed in the #EQC table
                  String sEqcTableDesc = ""
                  String sEqcAssocRec = ""
                  (sEqcTableDesc, sEqcAssocRec) = readTable('#EQC', sEquipClass)
                  
                  if (!sEqcAssocRec.equals("") && !sEqcAssocRec.equals(null)) {
                      // Get next available PIC number
                      String sEqnTableDesc = ""
                      String sEqnAssocRec = ""
                      (sEqnTableDesc, sEqnAssocRec) = readTable('#EQN', 'TG')
                      
                      if (!sEqnTableDesc.equals("") && !sEqnTableDesc.equals(null)) {
                          String sNextNumber = String.format("%06d",(sEqnTableDesc.toInteger() + 1))
                          
                          // Update #EQN with an incremented PIC number
                          if (updateTable('#EQN', 'TG', sNextNumber)) {
                              // Successful in getting a PIC number for the new equipment
                              // so set default values on Equipment request record
                              request.setEquipmentNo('TG' + sNextNumber)
                              if (sEqcAssocRec.size() >= 9) {
                                  request.setAccountCode(sEqcAssocRec.substring(0,9))
                              }
                              if (sEqcAssocRec.size() >= 10) {
                                  request.setTaxCode(sEqcAssocRec.substring(9,10))
                              }
                              if (sEqcAssocRec.size() >= 12) {
                                  request.setCtaxCode(sEqcAssocRec.substring(10,12))
                              }
                              if (sEqcAssocRec.size() >= 13) {
                                  request.setCostingFlag(sEqcAssocRec.substring(12))
                              }
                          } else {
                              // Failed to assign a PIC No to the new equipment
                              log.info("Failed to assign a PIC Number. Check #EQN Table.")
                              throw new EnterpriseServiceOperationException(
                                  new ErrorMessageDTO("", "Failed to assign a PIC Number. Check #EQN Table.",
                                      "equipmentNo", 0, 0))
                          }
                      } else {
                          // Failed to locate the last PIC No used
                          log.info("Failed to get the last PIC Number used. Check #EQN Table.")
                          throw new EnterpriseServiceOperationException(
                              new ErrorMessageDTO("", "Failed to get the last PIC Number used. Check #EQN Table.",
                                  "equipmentNo", 0, 0))
                      }
                  }
              }
          }
      }
      return null
  }
  
  @Override
  public Object onPostExecute(Object input, Object result) {
      log.info("EquipmentService_multipleCreate onPostExecute - version: ${version}")
      
      // Nothing to do here
      return null
  }
  
  /**
   * Get Table details
   * @param sTableType Table Type
   * @param sTableCode Table Code
   * @return sTableDesc Table Entry's Description
   * @return sAssocRec Table entry's Associated Record
   */
  private def readTable(String sTableType, String sTableCode) {
      log.info("readTable")
      
      String sTableDesc = ""
      String sAssocRec = ""
      
      try {
          TableServiceReadReplyDTO tableReply = tools.service.get('Table').read({
              it.tableType = sTableType
              it.tableCode = sTableCode})
              
          sTableDesc = tableReply.getDescription()
          sAssocRec = tableReply.getAssociatedRecord()
      } catch (EnterpriseServiceOperationException e){
          listErrors(e)
      }
     
      return [sTableDesc, sAssocRec]
  }
  
  /**
   * Update Table
   * @param sTableType
   * @param sTableCode
   * @param sTableDesc
   * @return True/False Indicates if the table update is sucessfully
   */
  private Boolean updateTable(String sTableType, sTableCode, sTableDesc) {
      log.info("updateTable")
      
      Boolean updateOk = false
      
      try {
          TableCodeServiceResult tableReply = tools.service.get("TableCode").update({
             TableCodeDTO tcDto ->
                 tcDto.tableType = new TableType(sTableType)
                 tcDto.tableCode = new TableCode(sTableCode)
                 tcDto.tableDescription = new TableDesc(sTableDesc)}, false)
          
          updateOk = true
      } catch (EnterpriseServiceOperationException e){
          listErrors(e)
      }
      
      return updateOk
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

