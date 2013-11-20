package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.eilib.EllipseEnvironment;
import com.mincom.batch.RequestDefinition;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf723.*;
import com.mincom.ellipse.edoi.ejb.msf801.*
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf823.*;
import com.mincom.ellipse.edoi.ejb.msf837.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf870.*;
import org.apache.commons.lang.StringUtils;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.Comparable;
import java.text.DecimalFormat

    public class ParamsTrbyed{
        //List of Input Parameters
		String paramPayLocation;
        String paramSortBy;
        String paramWorkGroup1;
		String paramWorkGroup2;
		String paramWorkGroup3;
		String paramWorkGroup4;
		String paramWorkGroup5;
		String paramEmployeeId1;
		String paramEmployeeId2;
		String paramEmployeeId3;
		String paramEmployeeId4;
		String paramEmployeeId5;
    }
    
    public class ProcessTrbyed extends SuperBatch {
        /* 
         * IMPORTANT!
         * Update this Version number EVERY push to GIT 
         */
        private version = 1;
        private ParamsTrbyed batchParams;
		private String [] paramWorkGroups;
		private String [] paramEmployeeIds;
		private ArrayList arrayOfTrbyedReportLine = new ArrayList();
		private def ReportA;
		File newFile = File.createTempFile("TRTYEDA", ".csv");
		FileWriter fstream = new FileWriter(newFile)
		BufferedWriter ReportB = new BufferedWriter(fstream)
		int recordCount823=0;
		int recordCount820=0;
		int recordCount723=0;
		int recordCount837=0;
		private DecimalFormat decFormatter = new DecimalFormat("################0.00");
		
		private class AccessValGrpItem{
			private String accessValGrpItmValue;
			private int noOfNonWildCards;
			private int maxStringLength;
			
			public AccessValGrpItem (String newAccessValGrpItm, int newMaxStringLength){
				maxStringLength = newMaxStringLength;
				setAccessValGrpItmValue(newAccessValGrpItm);
			}

			
			public void setAccessValGrpItmValue(String newAccessValGrpItmValue){
				if(newAccessValGrpItmValue.length()>maxStringLength){
					accessValGrpItmValue = newAccessValGrpItmValue.substring(0,maxStringLength);
				}
				else{
					accessValGrpItmValue = newAccessValGrpItmValue;
				}
				noOfNonWildCards = countNonWildCards(accessValGrpItmValue);
			}

			public String getAccessValGrpItmValue(){
				return accessValGrpItmValue;
			}
			
			public int getNoOfNonWildCards(){
				return noOfNonWildCards;
			}
			
			public void setMaxStringLength(int newMaxStringLength){
				maxStringLength = newMaxStringLength;
			}

			public int getMaxStringLength(){
				return maxStringLength;
			}
						
			public String toString(){
				String outputAccPaygrp;
				if(accessValGrpItmValue.trim().equals("")){
					outputAccPaygrp = "SPACES";
				}
				else{
					outputAccPaygrp = accessValGrpItmValue;
				}
				return("Access Group Value : "+outputAccPaygrp+", Number of Non Wildcards : "+ Integer.toString(noOfNonWildCards)+", Max String Length : "+Integer.toString(maxStringLength));
			}
			
			private int countNonWildCards(String inputString){
				int countResult;
				
				if (inputString.trim().equals("")){
					countResult = -1;
				}
				else{
					countResult = 0;
					while((countResult<inputString.length())&&(!inputString.substring(countResult, countResult+1).equals("*"))){
						countResult++;
					}
					
					if(countResult<maxStringLength){
						if(countResult == inputString.length()){
							countResult = maxStringLength;
						}
					}
					
				}
				return countResult;
			}
		}
		
		private class AccessValGrp{
			private AccessValGrpItem[] accessValGrpItem;
			private int numberOfItem;
			
			public AccessValGrp(String inputString, int newNumberOfItem, int newMaxStringLength){
                int noOfRepeat
				int noOfRemainingChar;
				
                accessValGrpItem = new AccessValGrpItem[newNumberOfItem];
				numberOfItem = newNumberOfItem;
				
				for(int i=0;i<numberOfItem;i++){
					accessValGrpItem[i]=new AccessValGrpItem(" ", newMaxStringLength);
				}
				
				/*
				* Calculate the number of max loop we could have with full newMaxStringLength
				* and then enter the values. The max number of loop is number of item.
				*/
				noOfRepeat = inputString.length()/newMaxStringLength;
				if(noOfRepeat>numberOfItem){
				   noOfRepeat = numberOfItem;
				}
				
				for (int i=0;i<noOfRepeat;i++){
				   accessValGrpItem[i].setAccessValGrpItmValue(inputString.substring((i*newMaxStringLength),((i+1)*newMaxStringLength)));
				}
			   
				/*
				 * Calculate whether we have a remaining character and then enter it to the array
				 * if the array is less than still less than maximum number of item.
			     */
				noOfRemainingChar = inputString.length()%newMaxStringLength;
				if ((noOfRemainingChar>0) && (noOfRepeat<(numberOfItem))){
					accessValGrpItem[noOfRepeat].setAccessValGrpItmValue(inputString.substring((noOfRepeat*newMaxStringLength),(noOfRepeat*newMaxStringLength)+noOfRemainingChar));
				}
				
				
			}
			
			public setAccessValGrpItemByIndex(String itemValue, int arrayIndex){
				accessValGrpItem[arrayIndex].setAccessValGrpItmValue(itemValue);

			}
			
			public AccessValGrpItem getAccessValGrpItemByIndex(int arrayIndex){
				return accessValGrpItem[arrayIndex];
			}
			
			public setNumberOfItem(int newNumberOfItem){
				numberOfItem = newNumberOfItem;
			}
			
			public int getNumberOfItem(){
				return numberOfItem;
			}
			
		}
		
		private class TrbyedaReportLine implements Comparable<TrbyedaReportLine>{
			private String payGroup;
			private String workGroup;
			private String surname;
			private String firstName;
			private String secondName;
			private String employeeId;
			private String deductionEarn;
			private String code;
			private String sortBy;
			private BigDecimal currentFiscalAmount;
			private BigDecimal units;
			
			public TrbyedaReportLine(String newPayGroup,
									 String newWorkGroup,
									 String newEmployeeId,
									 String newDeductionEarn,
									 String newCode,
									 String newSortBy,
									 BigDecimal newCurrentFiscalAmount,
									 BigDecimal newUnits){
					setPayGroup(newPayGroup);
					setWorkGroup(newWorkGroup);
					setEmployeeId(newEmployeeId);
					setDeductionEarn(newDeductionEarn);
					setCode(newCode);
					setSortBy(newSortBy);
					setCurrentFiscalAmount(newCurrentFiscalAmount);
					setUnits(newUnits);
				}
							  			
			public void setPayGroup(String newPayGroup){
				payGroup = newPayGroup; 
			}

			public String getPayGroup(){
				return payGroup;
			}
			
			public void setWorkGroup(String newWorkGroup){
				workGroup = newWorkGroup;
			}
			
			public String getWorkGroup(){
				return workGroup;
			}
	
			public String getSurname(){
				return surname;
			}
			
			public String getFirstName(){
				return firstName;
			}
			
			public String getSecondName(){
				return secondName;
			}
			
			public void setEmployeeId(String newEmployeeId){
				employeeId = newEmployeeId;
				setNameFromMSF810(newEmployeeId);
			}
			
			public String getEmployeeId(){
				return employeeId;
			}
			
			public void setDeductionEarn(String newDeductionEarn){
				deductionEarn = newDeductionEarn;
			}
			
			public String getDeductionEarn(){
				return deductionEarn;
			}
			
			public String getDeductionType(){
				return deductionEarn.substring(0,1);
			}

			public void setCode(String newCode){
				code = newCode;
			}
			
			public String getCode(){
				return code;
			}
			
			public String getDednEarnCodeDesc(){
				String description = " ";
				try{
					if(deductionEarn.equals("Earnings")){
						MSF801_A_801Rec msf801_a_801rec = edoi.findByPrimaryKey(new MSF801_A_801Key("A", "***"+code));
						description=msf801_a_801rec.getTnameA();
					}
					else{
						if(deductionEarn.equals("Deductions")){
							MSF801_D_801Rec msf801_d_801rec = edoi.findByPrimaryKey(new MSF801_D_801Key("D", "***"+code));
							description = msf801_d_801rec.getTnameD();
						}
					}
				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
					
				}
				
				return description;
			}
			
			public void setCurrentFiscalAmount(BigDecimal newCurrentFiscalAmount){
				currentFiscalAmount = newCurrentFiscalAmount;
			}
			
			public BigDecimal getCurrentFiscalAmount(){
				return currentFiscalAmount;
			}

			public void setUnits(BigDecimal newUnits){
				units = newUnits;
			}
			
			public BigDecimal getUnits(){
				return units;
			}

			
			public void setSortBy(String newSortBy){
				sortBy = newSortBy;
			}
					
			private String setNameFromMSF810(String inputEmployeeId){
				try{
					MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(inputEmployeeId));
					surname = msf810Rec.getSurname();
					firstName = msf810Rec.getFirstName();
					secondName = msf810Rec.getSecondName();
				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
					surname = " ";
					firstName = " ";
				}
			}
			
			public getPackedName(){
				return (surname+", "+firstName);
			}
			
			public boolean isHasSameEmpWGPG(TrbyedaReportLine compareValue){
				if(employeeId.equals(compareValue.getEmployeeId())&& payGroup.equals(compareValue.getPayGroup())){
					return true;
				}
				else{
					return false;
				}
			}

			int compareTo(TrbyedaReportLine otherReportLine){
				if(sortBy.equals("E")){
					if (!payGroup.equals(otherReportLine.getPayGroup())){
						return payGroup.compareTo(otherReportLine.getPayGroup())
					}
					if (!employeeId.equals(otherReportLine.getEmployeeId())){
						return employeeId.compareTo(otherReportLine.getEmployeeId())
					}
					if (!workGroup.equals(otherReportLine.getWorkGroup())){
						return workGroup.compareTo(otherReportLine.getWorkGroup())
					}
					if (!surname.equals(otherReportLine.getSurname())){
						return surname.compareTo(otherReportLine.getSurname())
					}
					if (!deductionEarn.equals(otherReportLine.getDeductionEarn())){
						return otherReportLine.getDeductionEarn().compareTo(deductionEarn)
					}
					if (!code.equals(otherReportLine.getCode())){
						return code.compareTo(otherReportLine.getCode())
					}
					return 0;
				}
				else{
					if(sortBy.equals("S")){
						if (!payGroup.equals(otherReportLine.getPayGroup())){
							return payGroup.compareTo(otherReportLine.getPayGroup())
						}
						if (!surname.equals(otherReportLine.getSurname())){
							return surname.compareTo(otherReportLine.getSurname())
						}
						if (!workGroup.equals(otherReportLine.getWorkGroup())){
							return workGroup.compareTo(otherReportLine.getWorkGroup())
						}
						if (!employeeId.equals(otherReportLine.getEmployeeId())){
							return employeeId.compareTo(otherReportLine.getEmployeeId())
						}
						if (!deductionEarn.equals(otherReportLine.getDeductionEarn())){
							return otherReportLine.getDeductionEarn().compareTo(deductionEarn)
						}
						if (!code.equals(otherReportLine.getCode())){
							return code.compareTo(otherReportLine.getCode())
						}
						return 0;
					}
					else{
						if(sortBy.equals("W")){
							if (!payGroup.equals(otherReportLine.getPayGroup())){
								return payGroup.compareTo(otherReportLine.getPayGroup())
							}
							if (!workGroup.equals(otherReportLine.getWorkGroup())){
								return workGroup.compareTo(otherReportLine.getWorkGroup())
							}
							if (!surname.equals(otherReportLine.getSurname())){
								return surname.compareTo(otherReportLine.getSurname())
							}
							if (!employeeId.equals(otherReportLine.getEmployeeId())){
								return employeeId.compareTo(otherReportLine.getEmployeeId())
							}
							if (!deductionEarn.equals(otherReportLine.getDeductionEarn())){
								return otherReportLine.getDeductionEarn().compareTo(deductionEarn)
							}
							if (!code.equals(otherReportLine.getCode())){
								return code.compareTo(otherReportLine.getCode())
							}
							return 0;
						}
					}
				}
			}
			
													
		}


        private AccessValGrp accessValGrp;
		private String accessRule;
		
        public void runBatch(Binding b){            
            
            init(b);
            
            printSuperBatchVersion();
            info("runBatch Version : " + version);
            
            batchParams = params.fill(new ParamsTrbyed())

			paramWorkGroups = new String[5];
			paramWorkGroups[0] = batchParams.paramWorkGroup1;
			paramWorkGroups[1] = batchParams.paramWorkGroup2;
			paramWorkGroups[2] = batchParams.paramWorkGroup3;
			paramWorkGroups[3] = batchParams.paramWorkGroup4;
			paramWorkGroups[4] = batchParams.paramWorkGroup5;

			paramEmployeeIds = new String[5];
			paramEmployeeIds[0] = batchParams.paramEmployeeId1;
			paramEmployeeIds[1] = batchParams.paramEmployeeId2;
			paramEmployeeIds[2] = batchParams.paramEmployeeId3;
			paramEmployeeIds[3] = batchParams.paramEmployeeId4;
			paramEmployeeIds[4] = batchParams.paramEmployeeId5;
			            
            //PrintRequest Parameters
			info("paramPayLocation    : " + batchParams.paramPayLocation);
			info("paramSortBy      : " + batchParams.paramSortBy);
			for(int i = 0; i<5; i++){
				info("paramWorkGroup"+Integer.toString(i+1)+" : "+paramWorkGroups[i]);
			}

			for(int i = 0; i<5; i++){
				info("paramEmployeeId"+Integer.toString(i+1)+": "+paramEmployeeIds[i]);
			}
											
            try {
                    processBatch();
                
            } finally {
                    printBatchReport();
            }
        }

        private void processBatch(){
            info("processBatch");           
            //write process
			
			if(initialise_B000()){
				processMSF820_C050();
				if(!arrayOfTrbyedReportLine.isEmpty()){
					generateTrbyedaReport_E000();
				}
			}
			
        }
        
        //additional method - start from here.
        
		
		private boolean initialise_B000(){
			info("initialise_B000");

			String userIdNew;
			String userId;
			String positionId;

			userId = request.getPropertyString("User")
			info("userId: "+userId)
			
			if (userId.length() <= 5){
				userIdNew = "00000" + userId;
			} else{
				userIdNew = "00000" + userId.substring(1, 6);
			  }
			
			info("userIdNew: "+userIdNew)
			
			Constraint c1 = MSF878Key.employeeId.equalTo(userIdNew);
			Constraint c2 = MSF878Key.primaryPos.equalTo("0");
		
			def query = new QueryImpl(MSF878Rec.class).and(c1.and(c2))
									
			MSF878Rec msf878Rec = (MSF878Rec) edoi.firstRow(query)
			if  (msf878Rec){
				positionId = msf878Rec.getPrimaryKey().getPositionId();
				println("MSF878 Position ID : "+ positionId);
			}else{
				info ("USER ID NOT EXIST")
				return false
				}

			MSF870Rec msf870Rec = edoi.findByPrimaryKey(new MSF870Key(positionId));
					
			accessRule = msf870Rec.getAccessRule();
			info("accessRule: "+accessRule)
			
			if (accessRule.equals("I")||accessRule.equals("E")){
				accessValGrp = new AccessValGrp(msf870Rec.getAccessValGrp(), 20, 3);
			}
			else{
				if(accessRule.equals("W")){
					accessValGrp = new AccessValGrp(msf870Rec.getAccessValGrp(), 10, 7);
				}
				else{
				    if(!accessRule.equals("A")){
						return false;
				    }
				}
			}
			
			ReportA = report.open('TRBYEDA')	
			ReportB.write("Employee ID,Surname,First Name,Second Name,Type,Code,Description,Units,Amount" + "\n")						
			return true;

		}
		
		private void processMSF820_C050(){
			info("processMSF820_C050");
			
			String sPayLocation;
			boolean payLocFound;
			
			int numberOfEmployee = 0;
			
			if((!paramEmployeeIds[0].trim().equals(""))||(!paramEmployeeIds[1].trim().equals(""))||(!paramEmployeeIds[2].trim().equals(""))||
			   (!paramEmployeeIds[3].trim().equals(""))||(!paramEmployeeIds[4].trim().equals(""))){
			
				for(int paramIndex=0; paramIndex<5; paramIndex++){
					if(!paramEmployeeIds[paramIndex].equals(" ")){
						processIndividual_C050(paramIndex);
					}
				}
			}
			else{
				if(batchParams.paramPayLocation.trim().equals("")){
					// Find pay locations from PAYL table
					Constraint c1 = MSF010Key.tableType.equalTo("PAYL")
					def query = new QueryImpl(MSF010Rec.class).and(c1)
																   
					MSF010Key msf010key = new MSF010Key()
					edoi.search(query,{MSF010Rec msf010RecRead ->
							sPayLocation = msf010RecRead.getPrimaryKey().getTableCode()
							try{
								Constraint c2 = MSF820Rec.payLocation.equalTo(sPayLocation)
								query = new QueryImpl(MSF820Rec.class).and(c2);
								edoi.search(query,{MSF820Rec msf820Rec ->
									recordCount820++;
									processWorkGroup_C050(msf820Rec.getPrimaryKey().getEmployeeId(),msf820Rec.getPayGroup());
								})
							}
							catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
								
							}
						})

				}
				else{
					// Validate the pay location
					info("PayLocCheck")
					payLocFound = true;
					try{
						MSF010Key msf010key = new MSF010Key()
						msf010key.setTableType("PAYL")
						msf010key.setTableCode(batchParams.paramPayLocation)
						MSF010Rec msf010RecRead = edoi.findByPrimaryKey(msf010key)
					} catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
						payLocFound = false;
					}
					
					// Pay location is valid
					if (payLocFound) {
						try{
							info("PayLocNotFound")
							Constraint c1 = MSF820Rec.payLocation.equalTo(batchParams.paramPayLocation);
							def query = new QueryImpl(MSF820Rec.class).and(c1)
							edoi.search(query,{MSF820Rec msf820Rec ->
								recordCount820++
								processWorkGroup_C050(msf820Rec.getPrimaryKey().getEmployeeId(),msf820Rec.getPayGroup());
								})
						}
						catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
							info ("Pay Location "+ batchParams.paramPayLocation + " Not Found")
						}
					} else{
					    info("Invalid Pay Location : " + batchParams.paramPayLocation)
						}
				}
			}
			
		}
		
		private void processIndividual_C050(int paramIndex){
			info("processIndividual_c050");

			if(!paramEmployeeIds[paramIndex].trim().equals("")){
				try{
					MSF820Rec msf820Rec = edoi.findByPrimaryKey(new MSF820Key(paramEmployeeIds[paramIndex]));
					recordCount820++;
					processWorkGroup_C050(msf820Rec.getPrimaryKey().getEmployeeId(), msf820Rec.getPayGroup());
				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
					
				}
			}
		}
		
		private void processWorkGroup_C050(String msf820EmployeeId, String msf820PayGroup){
			info("processWorkGroup_C050");
					
			Constraint c1 = MSF723Key.rec_723Type.equalTo("W");
			Constraint c2 = MSF723Key.equipNo.equalTo(" ");
			Constraint c3 = MSF723Key.employeeId.equalTo(msf820EmployeeId);
			Constraint c4 = MSF723Key.effDtRevsd.greaterThanEqualTo("0");
			
			def query = new QueryImpl(MSF723Rec.class).and(c1.and(c2).and(c3).and(c4));
									
			MSF723Rec msf723Rec = (MSF723Rec) edoi.firstRow(query);
			
			if (msf723Rec){
				recordCount723++;
				info("WorkGroup: "+msf723Rec.getPrimaryKey().getWorkGroup())
				if(paramWorkGroups[0].trim().equals("")&&paramWorkGroups[1].trim().equals("")&&paramWorkGroups[2].trim().equals("")&&
				   paramWorkGroups[3].trim().equals("")&&paramWorkGroups[4].trim().equals("")){
					 checkPayGroup_C050(msf820EmployeeId,msf820PayGroup, msf723Rec.getPrimaryKey().getWorkGroup());
				 }
				else{
					int paramIndex = 0;
					boolean isFoundMatch = false;
					
					while((paramIndex < 5) && (!isFoundMatch)){
						if(paramWorkGroups[paramIndex].equals(msf723Rec.getPrimaryKey().getWorkGroup())){					
							checkPayGroup_C050(msf820EmployeeId,msf820PayGroup,msf723Rec.getPrimaryKey().getWorkGroup())
							isFoundMatch = true;
						}
						paramIndex++;
					}
				}
			}

		}
		
		private void checkPayGroup_C050(String msf820EmployeeId, String msf820PayGroup, String msf723WorkGroup){
			info("checkPayGroup_C050");
			
			int paramIndex=0;
			int maxStringLength;
			int noOfNonWildCards;
			String accValGrpItmValue;
			String compareValue;
			boolean isFoundReasonToQuit=false;
			
			if(accessRule.equals("A")){
				processRecords_C050(msf820EmployeeId,msf820PayGroup,msf723WorkGroup)
			}
			else{
				if (accessRule.equals("I")||accessRule.equals("E")){
					compareValue = msf820PayGroup;
				}
				else{
					if(accessRule.equals("W")){
						compareValue = msf723WorkGroup;
					}
				}
				while(paramIndex<accessValGrp.getNumberOfItem()&&!isFoundReasonToQuit){
					
					noOfNonWildCards = accessValGrp.getAccessValGrpItemByIndex(paramIndex).getNoOfNonWildCards();
					accValGrpItmValue = accessValGrp.getAccessValGrpItemByIndex(paramIndex).getAccessValGrpItmValue();
					maxStringLength = accessValGrp.getAccessValGrpItemByIndex(paramIndex).getMaxStringLength();
					
					if(!accValGrpItmValue.trim().equals("")){
						if((noOfNonWildCards<maxStringLength)){
							if(compareValue.substring(0,noOfNonWildCards).equals(accValGrpItmValue.substring(0,noOfNonWildCards))){
								if(accessRule.equals("I")||accessRule.equals("W")){
									processRecords_C050(msf820EmployeeId,msf820PayGroup,msf723WorkGroup);
									isFoundReasonToQuit=true;
								}
								else{
									if(accessRule.equals("E")){
										isFoundReasonToQuit=true;
									}
								}
							}
							else{
								if(accessRule.equals("E")){
									processRecords_C050(msf820EmployeeId,msf820PayGroup,msf723WorkGroup);
									isFoundReasonToQuit=true;
								}
							}
						}
						else{
							if(compareValue.equals(accValGrpItmValue.trim())){
								if(accessRule.equals("I")||accessRule.equals("W")){
									processRecords_C050(msf820EmployeeId,msf820PayGroup,msf723WorkGroup);
									isFoundReasonToQuit=true;
								}
								else{
									if(accessRule.equals("E")){
										isFoundReasonToQuit=true;
									}
								}
							}
							else{
								if(accessRule.equals("E")){
									processRecords_C050(msf820EmployeeId,msf820PayGroup,msf723WorkGroup);
									isFoundReasonToQuit=true;
								}
							}
						}
					}
					
					paramIndex++;
				}
			}
		}
	
		private void processRecords_C050(String msf820EmployeeId, String msf820PayGroup, String msf723WorkGroup){
			info("processRecords_C050");
			try{
				Constraint c1 = MSF823Key.consPayGrp.equalTo(" ");
				Constraint c2 = MSF823Key.employeeId.equalTo(msf820EmployeeId);
				Constraint c3 = MSF823Key.earnCode.greaterThanEqualTo(" ");
				Constraint c4 = MSF823Rec.curFisAmtL.notEqualTo(0);
				def query = new QueryImpl(MSF823Rec.class).and(c1).and(c2).and(c3).and(c4);
				edoi.search(query,{MSF823Rec msf823Rec ->
					if(!msf823Rec.getPrimaryKey().getEarnCode().equals("000")){
						arrayOfTrbyedReportLine.add(new TrbyedaReportLine(msf820PayGroup,
																		  msf723WorkGroup,
																		  msf820EmployeeId,
																		  "Earnings",
																		  msf823Rec.getPrimaryKey().getEarnCode(),
																		  batchParams.paramSortBy,
																		  msf823Rec.getCurFisAmtL(),
																		  msf823Rec.getCurFisUnits()));
						recordCount823++;
					}
				})
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				
			}

			try{
				Constraint c5 = MSF837Key.consPayGrp.equalTo(" ");
				Constraint c6 = MSF837Key.employeeId.equalTo(msf820EmployeeId);
				Constraint c7 = MSF837Key.dednCode.greaterThanEqualTo(" ");
				Constraint c8 = MSF837Rec.curFisAmtL.notEqualTo(0);
				def query = new QueryImpl(MSF837Rec.class).and(c5).and(c6).and(c7).and(c8);
				edoi.search(query,{MSF837Rec msf837Rec ->
					if(!msf837Rec.getPrimaryKey().getDednCode().equals("000")){
										
						arrayOfTrbyedReportLine.add(new TrbyedaReportLine(msf820PayGroup,
																		  msf723WorkGroup,
																		  msf820EmployeeId,
																		  "Deductions",
																		  msf837Rec.getPrimaryKey().getDednCode(),
																		  batchParams.paramSortBy,
																		  msf837Rec.getCurFisAmtL(),
																		  msf837Rec.getCurFisUnits()));
						recordCount837++;
					}
				})
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				
			}
						
		}
		
		private void generateTrbyedaReport_E000(){

			TrbyedaReportLine prevLine, currLine;
			String tempString;
			BigDecimal totAmt;
			BigDecimal totUnits;
			int index;
			
			Collections.sort(arrayOfTrbyedReportLine);
			
			prevLine = arrayOfTrbyedReportLine.get(0);
			writeReportHeader(prevLine);
			
			totAmt = prevLine.getCurrentFiscalAmount();
			totUnits = prevLine.getUnits();
			
			index=1;

			while(index<arrayOfTrbyedReportLine.size()){
				currLine = arrayOfTrbyedReportLine.get(index);

				if(currLine.isHasSameEmpWGPG(prevLine)){
					if(currLine.getDeductionEarn().equals(prevLine.getDeductionEarn())){
						if(currLine.getCode().equals(prevLine.getCode())){
							totAmt = totAmt+currLine.getCurrentFiscalAmount();
							totUnits = totUnits+currLine.getUnits()
						}
						else{
							writeReportDetail(prevLine, totAmt);
							writeCsvFile(prevLine, totAmt, totUnits);
							totAmt=currLine.getCurrentFiscalAmount();
							totUnits = currLine.getUnits();
						}
					}
					else{
					// Report the last line of previous set
						writeReportDetail(prevLine, totAmt);
						writeCsvFile(prevLine, totAmt, totUnits);
						ReportA.write("\n");
						
					// Create header for next set
						ReportA.writeLine(132,"-");
						
						tempString = (currLine.getDeductionEarn()+" Code No").padRight(22);
						tempString = tempString+"Description".padRight(35);
						tempString = tempString+"Amount".padLeft(19);
						ReportA.write("  "+tempString);
						
						ReportA.writeLine(132,"-");
						
						totAmt=currLine.getCurrentFiscalAmount();
					}
				}
				else{
					writeReportDetail(prevLine, totAmt);
					writeCsvFile(prevLine, totAmt, totUnits);
					
					ReportA.write("\f");

					writeReportHeader(currLine);
										
					totAmt = currLine.getCurrentFiscalAmount();
				}
				
				//Write csv file

				prevLine = currLine;
				index++;
			}
			
			//Need to write the last record
			writeReportDetail(currLine, totAmt);


			ReportA.close();
			ReportB.close();
			
		}
			
		private void writeReportHeader(TrbyedaReportLine reportLine){
			String tempString; 
			
			ReportA.write(StringUtils.center("YTD Payroll Earnings & Deductions Report", 132));
			ReportA.write("\n");
			
			try{
				MSF801_PG_801Rec msf801_pg_801rec = edoi.findByPrimaryKey(new MSF801_PG_801Key("PG", reportLine.getPayGroup()));
				tempString = "Payer's Name: "+ msf801_pg_801rec.getGrpTaxNmePg().padRight(25);
				tempString = tempString+"Payer's ABN:  "+ msf801_pg_801rec.getGrpTaxNoPg().padRight(16);
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				tempString = "Payer's Name:".padRight(26);
				tempString = tempString+"Payer's ABN:".padRight(18);
			}
			
			tempString = tempString+"Employee ID: "+reportLine.getEmployeeId().padRight(15);
			tempString = tempString+"Name: "+reportLine.getPackedName();
			
			ReportA.write("  "+tempString);
			ReportA.write("\n");
			ReportA.write("  Please find a list below of the total amounts paid and deducted from your salary for the financial year just ended.  This");
			ReportA.write("  information, where applicable, may assist you when completing your tax return.");
			ReportA.write("\n");
			ReportA.write("  This earnings list includes non-taxable amounts which are not included in the gross salary appearing on your Payment Summary.");
			ReportA.write("\n");
			
			ReportA.writeLine(132,"-");
			
			tempString = (reportLine.getDeductionEarn()+" Code No").padRight(22);
			tempString = tempString+"Description".padRight(35);
			tempString = tempString+"Amount".padLeft(20);
			ReportA.write("  "+tempString);
			
			ReportA.writeLine(132,"-");
			
		}
		
		private void writeReportDetail(TrbyedaReportLine reportLine, BigDecimal totAmt){
			String tempString;
			
			tempString = (reportLine.getCode()).padRight(22);
			tempString = tempString+reportLine.getDednEarnCodeDesc().padRight(35);
			tempString = tempString+(decFormatter.format(totAmt)).padLeft(20)
			ReportA.write("  "+tempString);
		}
		
		private void writeCsvFile(TrbyedaReportLine reportLine, BigDecimal totAmt, BigDecimal totUnits){
			ReportB.write(reportLine.getEmployeeId() + "," +
				reportLine.getSurname() + "," +
				reportLine.getFirstName() + ","+
				reportLine.getSecondName() + "," +
				reportLine.getDeductionType() + "," +
				reportLine.getCode() + "," +
				reportLine.getDednEarnCodeDesc() + "," +
				totUnits.toString() + "," +
				totAmt.toString() + "\n")

		}
		
        private void printBatchReport(){
            info("printBatchReport")
            //print batch report
			println("No of 820 record found : "+recordCount820.toString());
			println("No of 723 record found : "+recordCount723.toString());
			println("No of 823 record found : "+recordCount823.toString());
			println("No of 837 record found : "+recordCount837.toString());
		}
    }
        
/*run script*/  
ProcessTrbyed process = new ProcessTrbyed();
process.runBatch(binding);