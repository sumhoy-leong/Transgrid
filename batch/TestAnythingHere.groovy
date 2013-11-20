package com.mincom.ellipse.script.custom

import groovy.lang.Binding

import java.lang.reflect.Field

import nacaLib.varEx.Var

import org.apache.commons.lang.StringUtils

import com.mincom.batch.script.*
import com.mincom.ellipse.client.connection.ConnectionHolder
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf220.MSF220Rec
import com.mincom.ellipse.edoi.ejb.msf221.MSF221Key
import com.mincom.ellipse.edoi.ejb.msf221.MSF221Rec
import com.mincom.ellipse.edoi.ejb.msf600.*
import com.mincom.ellipse.edoi.ejb.msf620.MSF620Rec
import com.mincom.ellipse.ejp.EllipseSessionDataContainer
import com.mincom.ellipse.ejp.area.CommAreaWrapper
import com.mincom.ellipse.script.util.*
import com.mincom.enterpriseservice.ellipse.ConnectionId
import com.mincom.eql.*
import com.mincom.eql.impl.*

public class BatchParams {
	String empty
}

public class TestAnything extends SuperBatch{
	def reportWriter
	def batchParams

	def printCommAreaInformation() {
		Field f = commarea.getClass().getDeclaredField("data"); //NoSuchFieldException
		f.setAccessible(true);
		EllipseSessionDataContainer data = (EllipseSessionDataContainer) f.get(commarea); //IllegalAccessException
		ConnectionId id = ConnectionHolder.getConnectionId();
		char[] area = data.getEllipseSessionData(id).getCOMMAREA();
		CommAreaWrapper wrapper = new CommAreaWrapper(area);

		info("${wrapper.commarea}")
		Class iClazz = wrapper.commarea.getClass()
		Field[] fields = iClazz.getDeclaredFields()
		for(int i = 0; i < fields.length;i++) {
			String fName = fields[i].getName()
			Field field = iClazz.getDeclaredField(fName)
			field.setAccessible(true)
			def fieldGet = field.get(wrapper.commarea)
			String val
			if(fieldGet instanceof Var) {
				val = ((Var)fieldGet).getString()
			} else {
				val = fieldGet.toString()
			}

			info("${fName} - ${val}")
		}
	}

	def printInfoFromAnInstance(Object o){
		info("WX_DSTRCT : ${commarea.district}")
		info("===============================================================")
		info("Object Id : " + Integer.toHexString(System.identityHashCode(o)))
		o.properties.each { info ("${it.key} : ${it.value}") }
		info("===============================================================")
	}

	/*
	 * This method used for testing purpose only since there is an issue with the XML database used in MSSDAT routine
	 * This method should be deleted when the code is pushed into the Git
	 */
	String[] validateStartAndEndDate(String startDate, String endDate, String periodYrMn) {
		int[] monthDays = [
			31,
			28,
			31,
			30,
			31,
			30,
			31,
			31,
			30,
			31,
			30,
			31
		]
		int yr = StringUtils.substring(periodYrMn, 0, 2) as int
		int mn = StringUtils.substring(periodYrMn, 2, 4) as int
		if(StringUtils.equals(startDate, "0")) {
			info("validateStartDate")
			info("old startDate ${startDate}")
			info("yr ${yr}")
			info("mn ${mn}")
			startDate = "20"+StringUtils.substring(periodYrMn, 0, 2)+StringUtils.substring(periodYrMn, 2, 4)+"01"
			info("new startDate ${startDate}")
		}
		if(StringUtils.equals(endDate, "0")) {
			info("validateEndDate")
			info("old endDate ${endDate}")
			info("yr ${yr}")
			info("mn ${mn}")
			boolean kabisat = yr % 4 == 0
			int endDays = monthDays[mn-1]
			if(mn == 2 && kabisat) {
				endDays = 29
			}
			endDate = "20"+StringUtils.substring(periodYrMn, 0, 2)+StringUtils.substring(periodYrMn, 2, 4)+endDays.toString()
			info("new endDate ${endDate}")
		}
		return [startDate, endDate]
	}
	public void testEdoiWithRestart() {
		println "testEdoiWithRestart"
		final long startTime = System.nanoTime()
		final long endTime
		int j = 0 //counter
		try{
			int rstCountRec = 0
			boolean eof = false
			boolean restart = false
			// Code Using restart
			String rstTableType = "ER"
			String rstTableCode = " "
			while (!eof){
				Constraint c1 = MSF010Key.tableType.equalTo(rstTableType)
				Constraint c2 = MSF010Key.tableCode.greaterThanEqualTo(rstTableCode)
				def query = new QueryImpl
						(MSF010Rec.class).and(c1).and(c2)
				edoi.search(query).results.each { MSF010Rec msf010Rec->
					if (rstCountRec == 0){
						if (!restart){
							j++
							rstTableType = msf010Rec.getPrimaryKey().getTableType()
							rstTableCode = msf010Rec.getPrimaryKey().getTableCode()
						}
						rstCountRec++
					} else{
						j++
						rstCountRec++
						rstTableType = msf010Rec.getPrimaryKey().getTableType()
						rstTableCode = msf010Rec.getPrimaryKey().getTableCode()
					}

				}
				// Hard coded for test purpose
				if (rstCountRec == 10000){
					rstCountRec = 0
					eof = false
					restart = true
				} else{
					eof = true
				}
			}
		} catch (Exception ex) {
			println "ERROR : ${ex.toString()}"
		} finally {
			endTime = System.nanoTime();
			final long duration = endTime - startTime
			println "EDOI - ER using restart returns ${j} rows; processing time is ${duration} miliseconds"
			reportWriter.write("EDOI - ER using restart returns ${j} rows; processing time is ${duration} miliseconds")
		}
	}

	public void testEdoiWithRestart2() {
		println "testEdoiWithRestart2"
		final long startTime = System.nanoTime()
		final long endTime
		int j = 0 //counter
		try{
			def tableType = "ER"
			def tableCode = " "
			// Processing Restart
			Constraint c1 = MSF010Key.tableType.equalTo(tableType)
			Constraint c2 = MSF010Key.tableCode.greaterThanEqualTo(tableCode)

			def query = new QueryImpl(MSF010Rec.class).and(c1.and(c2)).orderBy(MSF010Rec.aix1)

			//j = edoi.search(query).getResults().size()
			j = edoi.search(query,
					restart.each {MSF010Rec msf010rec ->
						if (msf010rec.getPrimaryKey().getTableType() != tableType || msf010rec.getPrimaryKey().getTableCode() != tableCode){
							println "tableType                                : ${tableType}"
							println "tableCode                                : ${tableCode}"
							println "msf010rec.getPrimaryKey().getTableType() : ${msf010rec.getPrimaryKey().getTableType()}"
							println "msf010rec.getPrimaryKey().getTableCode() : ${msf010rec.getPrimaryKey().getTableCode()}"
							println ""
							tableType = msf010rec.getPrimaryKey().getTableType()  // populate restart value
							tableCode = msf010rec.getPrimaryKey().getTableCode()  // populate restart value
						}
					})
		} catch (Exception ex) {
			ex.printStackTrace()
			println "ERROR : ${ex.toString()}"
		} finally {
			endTime = System.nanoTime();
			final long duration = endTime - startTime
			println "EDOI - ER returns ${j} rows; processing time is ${duration} miliseconds"
			reportWriter.write("EDOI - ER wiht restart2 returns ${j} rows; processing time is ${duration} miliseconds")
		}
	}

	public void testEdoi() {
		println "testEdoi"
		final long startTime = System.nanoTime()
		final long endTime
		int j = 0 //counter
		try{
			Constraint cTableType = MSF010Key.tableType.equalTo("ER")
			Constraint cTableCode = MSF010Key.tableCode.greaterThanEqualTo(" ")
			def query = new QueryImpl(MSF010Rec.class).and(cTableType).and(cTableCode)
			edoi.search(query) { MSF010Rec msf010Rec-> j++ }
		} catch (Exception ex) {
			println "ERROR : ${ex.toString()}"
		} finally {
			endTime = System.nanoTime();
			final long duration = endTime - startTime
			println "EDOI - ER returns ${j} rows; processing time is ${duration} miliseconds"
			reportWriter.write("EDOI - ER returns ${j} rows; processing time is ${duration} miliseconds")
		}
	}

	public void testServiceCall(int rows) {
		println "testServiceCall"
		final long startTime = System.nanoTime()
		final long endTime
		int j = 0 //counter
		try{

			def serviceGet = service.get("TABLE") //specify the service name to be called
			def restart = "" //restart value

			/*
			 * Since groovy does not support do-while then we need to execute retrieve at the first time
			 * to get the restart value
			 */
			def collectionDTO = serviceGet.retrieve({ Object it ->
				it.setTableType("ER")
				it.setTableCode("")
				it.tableCode = ""
			}, rows, false, restart)

			restart = collectionDTO.getCollectionRestartPoint()
			collectionDTO.replyElements.each { j++ }

			/*
			 * restart value has been set from above service call, then do a loop while restart value is not blank
			 * this loop below will get the rest of the rows
			 */
			while(StringUtils.isNotBlank(restart)) {
				collectionDTO = serviceGet.retrieve({ Object it ->
					it.setTableType("ER")
				}, rows, false, restart)

				restart = collectionDTO.getCollectionRestartPoint()
				collectionDTO.replyElements.each { j++ }
			}
		} catch (Exception ex) {
			println "ERROR : ${ex.toString()}"
		} finally {
			endTime = System.nanoTime();
			final long duration = endTime - startTime
			println "Service.get(TABLE) - ER returns ${j} rows with ROW_COUNT: ${rows}; processing time is ${duration} miliseconds"
			reportWriter.write("Service.get(TABLE) - ER returns ${j} rows with ROW_COUNT: ${rows}; processing time is ${duration} miliseconds")
		}
	}

	void browseMSF221() {
		info("browseMSF221")
		Constraint cPoNo = MSF221Key.poNo.equalTo("P00032")
		Constraint cPoItemNo = MSF221Key.poItemNo.greaterThanEqualTo(" ")
		Constraint cDstrctCode = StringUtils.isNotBlank(batchParams.districtCode) ? MSF220Rec.dstrctCode.equalTo(batchParams.districtCode) : MSF220Rec.dstrctCode.greaterThanEqualTo(" ")
		//"WP" is only available in Transgrid, during testing you could change the priceCode value
		//Constraint cPriceCode = MSF221Rec.priceCode.equalTo("WP")
		Constraint cPriceCode = MSF221Rec.priceCode.greaterThanEqualTo(" ") //--> for testing only
		Constraint cStatus221 = MSF221Rec.status_221.lessThanEqualTo("2")
		def query = new QueryImpl(MSF221Rec.class).and(cPoNo).and(cPoItemNo).and(cStatus221).and(cPriceCode).and(cDstrctCode)
		///Wkwkwkw
		Closure c = {}
		edoi.search(query) { info("Kriuk Kriuk ${it.getClass()} - ${it.toString()}")}
		//		edoi.search(query, { MSF221Rec msf221Rec->
		//			test(msf221Rec, msf220Rec)
		//		})
	}

	void browseMSF620() {
		def q = new QueryImpl(MSF620Rec.class)
		int rowCount = 0
		//1
		reportWriter.write("Rows Taken using edoi.search(q)")
		edoi.search(q) {MSF620Rec rec->
			rowCount++
			String s = String.format("Row: %6d; Record: %s %s", rowCount, rec.getPrimaryKey().getDstrctCode(), rec.getPrimaryKey().getWorkOrder())
			reportWriter.write(s)
		}
		reportWriter.write("rowCount ${rowCount}")
		reportWriter.writeLine()
		//2
		rowCount = 0
		reportWriter.write("Rows Taken using edoi.search(q).results.each")
		q = new QueryImpl(MSF620Rec.class)
		edoi.search(q).results.each { MSF620Rec rec->
			rowCount++
			String s = String.format("Row: %6d; Record: %s %s", rowCount, rec.getPrimaryKey().getDstrctCode(), rec.getPrimaryKey().getWorkOrder())
			reportWriter.write(s)
		}
		reportWriter.write("rowCount ${rowCount}")
		reportWriter.writeLine()
		//3
		//		rowCount = 0
		//		reportWriter.write("Rows Taken using service.get(WORKORDER)")
		//		try{
		//
		//			def serviceGet = service.get("WORKORDER") //specify the service name to be called
		//			def restart = "" //restart value
		//
		//			/*
		//			 * Since groovy does not support do-while then we need to execute retrieve at the first time
		//			 * to get the restart value
		//			 */
		//			def collectionDTO = serviceGet.retrieve({Object it -> it.setReturnWorkGroup(true)}, 100, false, restart)
		//			restart = collectionDTO.getCollectionRestartPoint()
		//			collectionDTO.replyElements.each {
		//				rowCount++
		//				String s = String.format("Row: %6d; Record: %s %s", rowCount, it.getDistrictCode(), it.getWorkOrder())
		//				reportWriter.write(s)
		//			}
		//
		//			/*
		//			 * restart value has been set from above service call, then do a loop while restart value is not blank
		//			 * this loop below will get the rest of the rows
		//			 */
		//			while(StringUtils.isNotBlank(restart)) {
		//				collectionDTO = serviceGet.retrieve({Object it -> it.setReturnWorkGroup(true)}, 100, false, restart)
		//				restart = collectionDTO.getCollectionRestartPoint()
		//				collectionDTO.replyElements.each {
		//					rowCount++
		//					String s = String.format("Row: %6d; Record: %s %s", rowCount, it.getDistrictCode(), it.getWorkOrder())
		//					reportWriter.write(s)
		//				}
		//			}
		//
		//			reportWriter.write("rowCount ${rowCount}")
		//			reportWriter.writeLine()
		//		} catch (Exception ex) {
		//			ex.printStackTrace()
		//			info("ERROR : ${ex.toString()}")
		//		}
	}

	public void mainProcess() {
		reportWriter = report.open("REPORT")
		batchParams = params.fill(new BatchParams())
		//browseMSF620()
		//		testEdoiWithRestart2()
		//		testEdoiWithRestart()
		//		testEdoi()
		//		testServiceCall(20)
		//		testServiceCall(40)
		//		testServiceCall(100)
		//		MSF760Key.employeeId
		//		try {
		//			MSF760Rec rec = edoi.findByPrimaryKey(new MSF760Key("AAE02"))
		//			info("rec ${rec}")
		//		} catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e1) {
		//			info("E1")
		//			e1.printStackTrace()
		//		}
		//		catch(Exception e2) {
		//			info("E2")
		//			e2.printStackTrace()
		//		}

		try{
			info("Groovy Version ${groovy.lang.GroovySystem.getVersion()}")
			ScreenAppLibrary sl = new ScreenAppLibrary()
			info("readEquipmentClassif")
			EquipmentDTO eqDTO = new EquipmentDTO()
			eqDTO.setEquipNo("1000")
			EquipmentResultDTO reply =  sl.readEquipmentClassif(eqDTO)
			info("Equip No    : ${reply.getEquipNo()}")
			info("Item Name 1 : ${reply.getItemName1()}")
			info("Equip Clasif: ${reply.getEquipmentClassif0()}")
			reportWriter.write("${reply.getEquipNo()}")
			reportWriter.write("${reply.getEquipmentClassif0()}")
		} catch (Exception  e) {
			e.printStackTrace()
		}

		reportWriter.close()
	}
}

TestAnything test = new TestAnything()
test.init(binding)
test.mainProcess()
