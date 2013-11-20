import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.datatype.DatatypeFactory

import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFKey
import com.mincom.ellipse.errors.exceptions.FatalException
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.ellipse.types.m8mwp.instances.BudgetPreparationAccountingPeriod
import com.mincom.ellipse.types.m8mwp.instances.BudgetPreparationActivityCostsDTO
import com.mincom.ellipse.types.m8mwp.instances.BudgetPreparationActivityCostsSpreadDTO
import com.mincom.ellipse.types.m8mwp.instances.BudgetPreparationCostAmount
import com.ventyx.webserviceclient.think180.Think180InterfaceClient
import com.ventyx.webserviceclient.think180.Think180InterfaceSoap
import com.ventyx.webserviceclient.think180.content.RequestMarshaller
import com.ventyx.webserviceclient.think180.content.ResponseUnmarshaller
import com.ventyx.webserviceclient.think180.content.response.BPA

class BudgetPreparationActivityCostsService_applyCustomerCostSpread extends ServiceHook {
	private final static String INTEGRATION_URL = "integration.bpaspread.success.url"
	private final static String INTEGRATION_PASSWORD = "integration.bpaspread.success.password"
	private final static String INTEGRATION_USER = "integration.bpaspread.success.user"

	private final static String ERROR_PREFIX = "Custom Spread Integration: "

	private final static String SPREAD_TABLE_TYPE = "BPST"
	private final static String TABLE_CODE_PREFIX = "SUCCESS"

	private final int MAX_INSTANCES = 100

	private Logger log = LoggerFactory.getLogger(BudgetPreparationActivityCostsService_applyCustomerCostSpread.class)

	@Override
	public Object onPreExecute(Object input) {
		BudgetPreparationActivityCostsDTO dto = (BudgetPreparationActivityCostsDTO)input
		logInputParameters(dto)

		log.trace "Performing Validation"
		// Validate to ensure that a valid Spread Type is entered, and that the Work Item is an Estimate
		if (!StringUtils.equals(StringUtils.left(dto.custSpreadType.value, 7), TABLE_CODE_PREFIX)) throw new FatalException(ERROR_PREFIX + "Invalid Custom Spread '" +
			dto.custSpreadType.value + "'; only Custom Spreads prefixed with '" + TABLE_CODE_PREFIX + "' are supported.")

		if (!StringUtils.equals(dto.workEntityType.value, "JI")) throw new FatalException(ERROR_PREFIX + "Invalid Work Entity Item '" +
			dto.workEntityReference.value + "'; only Job Estimates are supported.")

		// Generate the Request for Success
		log.trace "Generating Sucess Request"
		String request = createSuccessRequest(dto)

		Think180InterfaceSoap client = new Think180InterfaceClient().getThink180InterfaceSoap(getPreference(INTEGRATION_URL))

		// Call the Success Web Service
		String response
		try {
			response = client.get(request)
		} catch (Exception ex) {
			ex.printStackTrace(System.out)
			throw new FatalException(ERROR_PREFIX + "Error Calling Success Web Service: " + ex.message)
		}

		log.trace "Success Response XML: " + response
		
		BPA bpa = null
		
		// Convert the XML Response
		try {
			bpa = new ResponseUnmarshaller().unmarshal(response)
		} catch (Exception ex) {
			ex.printStackTrace(System.out)
			throw new FatalException(ERROR_PREFIX + "Error Parsing Response from Success Web Service: " + ex.message)
		}
		logSuccessResponse(bpa)

		if (StringUtils.isNotBlank(bpa.errorMessage)) {
			throw new FatalException(ERROR_PREFIX + "Error from Success: " + bpa.errorMessage)
		}

		// Create the BPA Spread Service
		def spreadService = tools.service.get('BudgetPreparationActivityCostsSpread')

		// Search for the Existing Spread DTOs
		List<BudgetPreparationActivityCostsSpreadDTO> spread = spreadService.search({Object it ->
			it.budgetPreparationId = dto.budgetPreparationId
			it.workEntityType = dto.workEntityType
			it.workEntityReference = dto.workEntityReference
			it.resourceType = dto.resourceType
			it.sequence = dto.sequence
			it.costType = dto.costType
		}, MAX_INSTANCES)

		logSpread(spread)

		// Zero out and Store Existing Spread
		for (item in spread) {
			def spreadDto = item.budgetPreparationActivityCostsSpreadDTO
			if (StringUtils.isNotBlank(spreadDto.costAmount.value)) {
				spreadService.update({Object it ->
					it.budgetPreparationId = dto.budgetPreparationId
					it.workEntityType = dto.workEntityType
					it.workEntityReference = dto.workEntityReference
					it.resourceType = dto.resourceType
					it.sequence = dto.sequence
					it.costType = dto.costType
					it.accountingPeriod = spreadDto.accountingPeriod
					it.costAmount = new BudgetPreparationCostAmount("0")
				})
			}
		}

		// Store the new Spread
		for (cost in bpa.costs) {
			spreadService.update({Object it ->
				it.budgetPreparationId = dto.budgetPreparationId
				it.workEntityType = dto.workEntityType
				it.workEntityReference = dto.workEntityReference
				it.resourceType = dto.resourceType
				it.sequence = dto.sequence
				it.costType = dto.costType
				it.accountingPeriod = new BudgetPreparationAccountingPeriod(convertAccountingPeriod(cost.month))
				it.costAmount = new BudgetPreparationCostAmount(cost.cost.toPlainString())
			})
		}
	}

	@Override
	public Object onPostExecute(Object input, Object result) {
	}

	private String createSuccessRequest(BudgetPreparationActivityCostsDTO dto) {
		com.ventyx.webserviceclient.think180.content.request.BPA bpa = new com.ventyx.webserviceclient.think180.content.request.BPA()

		bpa.authorisation = new com.ventyx.webserviceclient.think180.content.request.BPA.Authorisation()
		bpa.authorisation.userID = getPreference(INTEGRATION_USER)
		bpa.authorisation.password = getPreference(INTEGRATION_PASSWORD)
		bpa.spendingCurve = getTableCodeDescription(dto.custSpreadType.value)
		bpa.bpaid = dto.budgetPreparationId.value
		bpa.cost = new BigDecimal(dto.expectedCosts.value)
		
		log.trace "Cost Type: " + dto.costType.value
		
		if (StringUtils.equals(StringUtils.trim(dto.costType.value), "L")) {
			bpa.requirementType = "Labour"
		} else if (StringUtils.equals(StringUtils.trim(dto.costType.value), "E")) {
			bpa.requirementType = "Equipment"
		} else if (StringUtils.equals(StringUtils.trim(dto.costType.value), "M")) {
			bpa.requirementType = "Material"
		} else if (StringUtils.equals(StringUtils.trim(dto.costType.value), "O")) {
			bpa.requirementType = "Other"
		}

		String work = dto.workEntityReference.value
		String estimateNo = StringUtils.trim(StringUtils.left(work, 12))
		String estimateVersion = StringUtils.trim(StringUtils.substring(work, 12, 15))
		String estimateItem = StringUtils.trim(StringUtils.substring(work, 15, 21))

		bpa.estimateNo = estimateNo
		bpa.estimateRevisionNo = estimateVersion
		bpa.estimateLineNo = estimateItem
		bpa.startDate = convertDate(dto.plannedStartDate.value)
		bpa.endDate = convertDate(dto.plannedFinishDate.value)

		String request = new RequestMarshaller().marshal(bpa)
		log.trace "Success Request XML: " + request
		return request
	}

	private XMLGregorianCalendar convertDate(String date) {
		XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar()
		cal.year = new Integer(StringUtils.left(date, 4))
		cal.month = new Integer(StringUtils.substring(date, 4, 6))
		cal.day = new Integer(StringUtils.substring(date, 6, 8))
		return cal
	}

	private String getPreference(String key) {
		try {
			return tools.edoi.findByPrimaryKey(new MSFPRFKey( '    ', '          ', key, '         ')).prefValue
		} catch (EDOIObjectNotFoundException) {
			throw new FatalException(ERROR_PREFIX + "Required Preference not found: " + key)
		}
	}

	private String getTableCodeDescription(String tableCode) {
		try {
			return tools.edoi.findByPrimaryKey(new MSF010Key(SPREAD_TABLE_TYPE, StringUtils.rightPad(tableCode, 18))).tableDesc
		} catch (EDOIObjectNotFoundException) {
			throw new FatalException(ERROR_PREFIX + "BPST Table Code not found: " + tableCode)
		}
	}

	private String convertAccountingPeriod(XMLGregorianCalendar month) {
		int monthInt = month.month
		int yearInt = month.year - 2000
		return yearInt + "" + StringUtils.leftPad("" + monthInt, 2, "0")
	}
	
	
	private logInputParameters(BudgetPreparationActivityCostsDTO dto) {
		log.trace "BPA Input Parameters:"
		log.trace "  BPA: " + dto.budgetPreparationId.value
		log.trace "  Work Entity Type: " + dto.workEntityType.value
		log.trace "  Work Entity Reference: " + dto.workEntityReference.value
		log.trace "  ResourceType: " + dto.resourceType.value
		log.trace "  Cost Type: " + dto.sequence.value
		log.trace "  Sequence: " + dto.costType.value
	}

	private logSuccessResponse(BPA bpa) {
		log.trace "Success Response:"
		log.trace "  BPA: " + bpa.bpaid
		log.trace "  Estimate: " + bpa.estimateNo
		log.trace "  Estimate Item: " +bpa.estimateRevisionNo
		log.trace "  Estimate Line: " + bpa.estimateLineNo
		log.trace "  Error Message: " + bpa.errorMessage
		if (bpa.costs != null) {
			for (cost in bpa.costs) {
				log.trace "  Success Cost Item:"
				log.trace "    Period: " + cost.month
				log.trace "    Cost: " + cost.cost
			}
		}
	}

	private logSpread(spread) {
		log.trace "Existing BPA Spread"
		for (item in spread) {
			def spreadDto = item.budgetPreparationActivityCostsSpreadDTO
			if (spreadDto.workEntityReference == null || StringUtils.isBlank(spreadDto.workEntityReference.value)) {
				log.trace "  Empty Spread Period: " + spreadDto.accountingPeriod.value
			} else {
				log.trace "  Spread Item: "
				log.trace "  BPA: " + spreadDto.budgetPreparationId.value
				log.trace "    Work Entity Type: " + spreadDto.workEntityType.value
				log.trace "    Work Entity Reference: " + spreadDto.workEntityReference.value
				log.trace "    ResourceType: " + spreadDto.resourceType.value
				log.trace "    Accounting Period: " + spreadDto.accountingPeriod.value
				log.trace "    Cost Type: " + spreadDto.costType.value
				log.trace "    Sequence: " + spreadDto.sequence.value
				log.trace "    Cost Amount: " + spreadDto.costAmount.value
			}
		}
	}
}