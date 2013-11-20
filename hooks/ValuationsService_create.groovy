/**
 * @author Ventyx 2013
 * 
 * MSE389 hooks.
 * This program is use for call Ewbimg after the execution MSE389
 */


import com.mincom.ellipse.hook.hooks.ServiceHook;
import com.mincom.enterpriseservice.ellipse.valuations.ValuationsServiceCreateReplyDTO;

class ValuationsService_create extends ServiceHook {

	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */

	private String version = "1" 	
	@Override
	public Object onPostExecute(Object input, Object result) {
		log.info("ValuationsService_create onPostExecute - version: ${version}")

		ValuationsServiceCreateReplyDTO reply = result
		String dstrctValue = reply.getDistrictCode()
		log.info("Hooks onPostExecute dstrctValue from input: ${dstrctValue}")
		String supplierValue = reply.getContractor()
		log.info("Hooks onPostExecute supplierValue from input: ${supplierValue}")
		String invoiceValue = reply.getCntrctrRefNo()
		log.info("Hooks onPostExecute invoiceValue from input: ${invoiceValue}")
		String accountant = reply.getAccountant()
		log.info("Hooks onPostExecute accountant from input: ${accountant}")


		//create request Ewbimg in mse080
		InvoiceImage invoiceImg = new InvoiceImage()
		invoiceImg.submitEwbimg(dstrctValue, supplierValue, invoiceValue, accountant)

		return super.onPostExecute(input, result)
	}
}
